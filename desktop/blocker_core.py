"""
Badilika Desktop Blocker - Core Engine
Handles Windows hosts file modifications, SafeSearch redirects,
and domain blocklist management.
"""

import os
import re
import sys
import subprocess
import urllib.request
from typing import List, Set, Tuple

MARKER_START = "# === BEGIN BADILIKA PORN BLOCKER ==="
MARKER_END = "# === END BADILIKA PORN BLOCKER ==="

LEGACY_MARKER_START = "# === BEGIN NEWLIFE PORN BLOCKER ==="
LEGACY_MARKER_END = "# === END NEWLIFE PORN BLOCKER ==="

DEFAULT_HOSTS_PATH = os.path.join(
    os.environ.get("SystemRoot", r"C:\Windows"),
    r"System32\drivers\etc\hosts"
)

# SafeSearch VIP IP addresses
SAFESEARCH_RULES = [
    # Google SafeSearch (forcesafesearch.google.com -> 216.239.38.120)
    ("216.239.38.120", "www.google.com"),
    ("216.239.38.120", "google.com"),
    # Bing SafeSearch (strict.bing.com -> 204.79.197.220)
    ("204.79.197.220", "www.bing.com"),
    ("204.79.197.220", "bing.com"),
    # DuckDuckGo SafeSearch (safe.duckduckgo.com -> 52.142.124.215)
    ("52.142.124.215", "duckduckgo.com"),
    ("52.142.124.215", "www.duckduckgo.com"),
]

# Explicit exclusions to ensure general streaming and social media are NEVER blocked
WHITELIST_SAFETY = {
    "netflix.com", "www.netflix.com",
    "disneyplus.com", "www.disneyplus.com",
    "amazon.com", "primevideo.com", "www.primevideo.com",
    "hulu.com", "www.hulu.com",
    "max.com", "hbomax.com",
    "youtube.com", "www.youtube.com",
    "twitch.tv", "www.twitch.tv",
    "reddit.com", "www.reddit.com",
    "twitter.com", "x.com", "www.twitter.com", "www.x.com",
    "instagram.com", "www.instagram.com",
    "facebook.com", "www.facebook.com",
    "linkedin.com", "www.linkedin.com",
    "tiktok.com", "www.tiktok.com",
    "github.com", "www.github.com",
    "wikipedia.org", "en.wikipedia.org",
}

# Common public resolvers.  Hosts-file rules cannot see encrypted DNS traffic,
# so Windows Firewall also blocks these endpoints while the shield is active.
# The app already requires elevation before it can change hosts or firewall rules.
ENCRYPTED_DNS_ENDPOINTS = [
    "1.1.1.1", "1.0.0.1", "8.8.8.8", "8.8.4.4", "9.9.9.9", "149.112.112.112",
    "94.140.14.14", "94.140.15.15", "208.67.222.222", "208.67.220.220",
]
FIREWALL_RULE_PREFIX = "Badilika encrypted DNS protection"


class BlockerCore:
    def __init__(self, hosts_path: str = None, seed_file: str = None, cache_dir: str = None):
        self.hosts_path = hosts_path or DEFAULT_HOSTS_PATH
        
        # Local base directory (supports both raw python and PyInstaller standalone exe)
        if getattr(sys, "frozen", False):
            base_dir = getattr(sys, "_MEIPASS", os.path.dirname(os.path.abspath(__file__)))
        else:
            base_dir = os.path.dirname(os.path.abspath(__file__))
        self.seed_file = seed_file or os.path.join(base_dir, "adult_domains.txt")
        
        if cache_dir:
            self.cache_dir = cache_dir
        else:
            appdata = os.environ.get("APPDATA", os.path.expanduser("~"))
            self.cache_dir = os.path.join(appdata, "Badilika")
            
        os.makedirs(self.cache_dir, exist_ok=True)
        self.community_file = os.path.join(self.cache_dir, "community_adult_hosts.txt")
        # The seed list is bundled read-only in a PyInstaller one-file build.
        # Store user-added rules in AppData so they survive updates/restarts.
        self.custom_file = os.path.join(self.cache_dir, "custom_domains.txt")

    def is_blocking_active(self) -> bool:
        """Check if Badilika markers currently exist in the hosts file."""
        if not os.path.exists(self.hosts_path):
            return False
        try:
            with open(self.hosts_path, "r", encoding="utf-8", errors="ignore") as f:
                content = f.read()
                return (MARKER_START in content and MARKER_END in content) or (LEGACY_MARKER_START in content and LEGACY_MARKER_END in content)
        except Exception:
            return False

    def load_domains(self) -> Set[str]:
        """Load unique adult domains from seed file and community cache."""
        domains: Set[str] = set()

        # 1. Read seed file
        if os.path.exists(self.seed_file):
            with open(self.seed_file, "r", encoding="utf-8", errors="ignore") as f:
                for line in f:
                    line = line.strip()
                    if line and not line.startswith("#"):
                        domain = line.lower()
                        if domain not in WHITELIST_SAFETY:
                            domains.add(domain)

        # 2. Read downloaded and user-created entries from writable AppData.
        for domain_file in (self.community_file, self.custom_file):
            if not os.path.exists(domain_file):
                continue
            with open(domain_file, "r", encoding="utf-8", errors="ignore") as f:
                for line in f:
                    line = line.strip()
                    if not line or line.startswith("#"):
                        continue
                    # Parse hosts file format: 0.0.0.0 domain.com
                    parts = line.split()
                    if len(parts) >= 2:
                        domain = parts[1].lower()
                        if domain not in WHITELIST_SAFETY and "." in domain:
                            domains.add(domain)
                    elif len(parts) == 1 and "." in parts[0]:
                        domain = parts[0].lower()
                        if domain not in WHITELIST_SAFETY:
                            domains.add(domain)

        return domains

    def add_custom_domain(self, domain: str) -> Tuple[bool, str]:
        """Persist a validated user domain outside the read-only app bundle."""
        domain = domain.strip().lower().removeprefix("www.")
        if not re.fullmatch(r"(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,63}", domain):
            return False, "Enter a valid domain, for example: example.com"
        if domain in WHITELIST_SAFETY:
            return False, "This domain is protected by the safety whitelist and cannot be blocked."
        try:
            existing = self.load_domains()
            if domain in existing:
                return True, f"{domain} is already in your blocklist."
            with open(self.custom_file, "a", encoding="utf-8") as custom_file:
                custom_file.write(f"{domain}\nwww.{domain}\n")
            return True, f"Added {domain} to your blocklist."
        except Exception as exc:
            return False, f"Could not save the domain: {exc}"

    def get_blocked_count(self) -> int:
        """Return total unique domains configured to be blocked."""
        return len(self.load_domains())

    def protection_stats(self) -> dict:
        """Return local, verifiable protection information for the dashboard."""
        domains = self.load_domains()
        def count_entries(path: str) -> int:
            if not os.path.exists(path):
                return 0
            try:
                with open(path, "r", encoding="utf-8", errors="ignore") as f:
                    return sum(1 for line in f if line.strip() and not line.lstrip().startswith("#"))
            except OSError:
                return 0
        return {
            "domains": len(domains),
            "seed_domains": count_entries(self.seed_file),
            "community_domains": count_entries(self.community_file),
            "custom_domains": count_entries(self.custom_file),
            "active": self.is_blocking_active(),
            "encrypted_dns_endpoints": len(ENCRYPTED_DNS_ENDPOINTS),
            "community_updated_at": os.path.getmtime(self.community_file) if os.path.exists(self.community_file) else 0,
        }

    def _set_encrypted_dns_firewall(self, enabled: bool) -> Tuple[bool, str]:
        """Block common direct DNS/DoH endpoints without affecting ordinary websites."""
        if os.name != "nt":
            return True, "Firewall rules are only needed on Windows."
        try:
            for suffix in ("TCP", "UDP"):
                name = f"{FIREWALL_RULE_PREFIX} {suffix}"
                subprocess.run(["netsh", "advfirewall", "firewall", "delete", "rule", f"name={name}"], capture_output=True, check=False, creationflags=subprocess.CREATE_NO_WINDOW)
                if enabled:
                    ports = "53,853,443" if suffix == "TCP" else "53,853"
                    result = subprocess.run([
                        "netsh", "advfirewall", "firewall", "add", "rule", f"name={name}",
                        "dir=out", "action=block", f"protocol={suffix}",
                        f"remoteip={','.join(ENCRYPTED_DNS_ENDPOINTS)}", f"remoteport={ports}",
                        "profile=any", "enable=yes",
                    ], capture_output=True, text=True, check=False, creationflags=subprocess.CREATE_NO_WINDOW)
                    if result.returncode != 0:
                        return False, (result.stderr or result.stdout or "Could not set encrypted-DNS firewall rules.").strip()
            return True, "Encrypted DNS endpoints protected."
        except OSError as exc:
            return False, f"Could not configure encrypted-DNS protection: {exc}"

    def build_block_section(self) -> str:
        """Generate the hosts section containing SafeSearch and adult blocks."""
        lines = [MARKER_START]
        lines.append("# SafeSearch Enforcement")
        for ip, host in SAFESEARCH_RULES:
            lines.append(f"{ip} {host}")

        lines.append("\n# Adult Content Domain Blocks (Targeted Pornography Blocklist)")
        domains = sorted(self.load_domains())
        for d in domains:
            lines.append(f"0.0.0.0 {d}")

        lines.append(MARKER_END)
        return "\n".join(lines)

    def enable_blocking(self) -> Tuple[bool, str]:
        """Inject blocker section into Windows hosts file."""
        try:
            current_content = ""
            if os.path.exists(self.hosts_path):
                with open(self.hosts_path, "r", encoding="utf-8", errors="ignore") as f:
                    current_content = f.read()

            # Clean out any previous section first
            clean_content = self._strip_existing_section(current_content)
            
            block_section = self.build_block_section()
            
            new_content = clean_content.rstrip() + "\n\n" + block_section + "\n"
            
            with open(self.hosts_path, "w", encoding="utf-8") as f:
                f.write(new_content)

            self.flush_dns()
            firewall_ok, firewall_message = self._set_encrypted_dns_firewall(True)
            if not firewall_ok:
                return False, f"Hosts protection was written but {firewall_message}"
            return True, f"Successfully activated protection ({len(self.load_domains())} domains blocked). {firewall_message}"
        except PermissionError:
            return False, "Permission denied. Please run NewLife as Administrator."
        except Exception as e:
            return False, f"Failed to activate blocker: {str(e)}"

    def disable_blocking(self) -> Tuple[bool, str]:
        """Remove blocker section from Windows hosts file cleanly."""
        try:
            if not os.path.exists(self.hosts_path):
                firewall_ok, firewall_message = self._set_encrypted_dns_firewall(False)
                if not firewall_ok:
                    return False, firewall_message
                return True, "Hosts file does not exist. " + firewall_message

            with open(self.hosts_path, "r", encoding="utf-8", errors="ignore") as f:
                content = f.read()

            clean_content = self._strip_existing_section(content)

            with open(self.hosts_path, "w", encoding="utf-8") as f:
                f.write(clean_content)

            self.flush_dns()
            firewall_ok, firewall_message = self._set_encrypted_dns_firewall(False)
            if not firewall_ok:
                return False, f"Hosts file was restored but {firewall_message}"
            return True, "Protection disabled and hosts file restored."
        except PermissionError:
            return False, "Permission denied. Please run Badilika as Administrator."
        except Exception as e:
            return False, f"Failed to disable blocker: {str(e)}"

    def _strip_existing_section(self, content: str) -> str:
        """Remove everything between markers inclusive (including legacy markers)."""
        pattern = re.compile(
            re.escape(MARKER_START) + r".*?" + re.escape(MARKER_END),
            re.DOTALL
        )
        cleaned = pattern.sub("", content)

        legacy_pattern = re.compile(
            re.escape(LEGACY_MARKER_START) + r".*?" + re.escape(LEGACY_MARKER_END),
            re.DOTALL
        )
        cleaned = legacy_pattern.sub("", cleaned)

        # Normalize double blank lines
        cleaned = re.sub(r"\n{3,}", "\n\n", cleaned)
        return cleaned.strip() + "\n"

    def flush_dns(self) -> bool:
        """Flush the Windows DNS resolver cache."""
        try:
            subprocess.run(
                ["ipconfig", "/flushdns"],
                capture_output=True,
                check=False,
                creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0
            )
            return True
        except Exception:
            return False

    def update_community_blocklist(self, url: str = None) -> Tuple[bool, str]:
        """
        Download curated community porn-only blocklist (StevenBlack porn variant).
        Filters out any false positives before caching.
        """
        if not url:
            url = "https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/porn/hosts"

        try:
            req = urllib.request.Request(url, headers={"User-Agent": "NewLifeBlocker/1.0"})
            with urllib.request.urlopen(req, timeout=15) as resp:
                data = resp.read().decode("utf-8", errors="ignore")

            # Extract domains and enforce whitelist
            valid_entries = []
            for line in data.splitlines():
                line = line.strip()
                if not line or line.startswith("#"):
                    continue
                parts = line.split()
                if len(parts) >= 2:
                    domain = parts[1].lower()
                    if domain not in WHITELIST_SAFETY and domain != "localhost":
                        valid_entries.append(f"0.0.0.0 {domain}")

            with open(self.community_file, "w", encoding="utf-8") as f:
                f.write("\n".join(valid_entries))

            # If blocking is currently active, re-apply with new domains
            if self.is_blocking_active():
                self.enable_blocking()

            return True, f"Successfully updated blocklist! Total: {len(valid_entries)} domains."
        except Exception as e:
            return False, f"Could not update community list: {str(e)}"
