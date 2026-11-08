"""
Unit tests for BlockerCore
"""

import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
from blocker_core import BlockerCore, MARKER_START, MARKER_END

class TestBlockerCore(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.hosts_path = os.path.join(self.temp_dir.name, "test_hosts")
        self.seed_path = os.path.join(self.temp_dir.name, "test_seed.txt")
        self.cache_dir = os.path.join(self.temp_dir.name, "cache")

        # Create dummy initial hosts file
        with open(self.hosts_path, "w", encoding="utf-8") as f:
            f.write("127.0.0.1 localhost\n::1 localhost\n# Custom user mapping\n192.168.1.50 myserver.local\n")

        # Create dummy seed file
        with open(self.seed_path, "w", encoding="utf-8") as f:
            f.write("pornhub.com\nxvideos.com\nskipthegames.com\nnetflix.com\n# comment\nreddit.com\n")

        self.core = BlockerCore(
            hosts_path=self.hosts_path,
            seed_file=self.seed_path,
            cache_dir=self.cache_dir
        )
        # Hosts-file behaviour is unit-tested without altering this computer's
        # real Windows Firewall. Firewall commands require elevation and are
        # covered separately by the runtime error handling.
        self.core._set_encrypted_dns_firewall = lambda enabled: (True, "Test firewall guard")

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_whitelist_safety_enforced(self):
        """Ensure netflix and reddit are filtered out of blocklist despite being in raw list."""
        domains = self.core.load_domains()
        self.assertIn("pornhub.com", domains)
        self.assertIn("xvideos.com", domains)
        self.assertIn("skipthegames.com", domains)
        self.assertNotIn("netflix.com", domains)
        self.assertNotIn("reddit.com", domains)

    def test_enable_and_disable_blocking(self):
        """Test enabling blocking and clean removal from hosts."""
        self.assertFalse(self.core.is_blocking_active())

        # Enable
        success, msg = self.core.enable_blocking()
        self.assertTrue(success, msg)
        self.assertTrue(self.core.is_blocking_active())

        # Verify content
        with open(self.hosts_path, "r", encoding="utf-8") as f:
            content = f.read()

        self.assertIn("192.168.1.50 myserver.local", content)  # Preserved existing
        self.assertIn(MARKER_START, content)
        self.assertIn(MARKER_END, content)
        self.assertIn("0.0.0.0 pornhub.com", content)
        self.assertIn("0.0.0.0 skipthegames.com", content)
        self.assertIn("216.239.38.120 www.google.com", content)  # SafeSearch check

        # Disable
        success, msg = self.core.disable_blocking()
        self.assertTrue(success, msg)
        self.assertFalse(self.core.is_blocking_active())

        # Verify markers removed and original preserved
        with open(self.hosts_path, "r", encoding="utf-8") as f:
            content_after = f.read()

        self.assertNotIn(MARKER_START, content_after)
        self.assertNotIn(MARKER_END, content_after)
        self.assertNotIn("0.0.0.0 pornhub.com", content_after)
        self.assertIn("192.168.1.50 myserver.local", content_after)

    def test_protection_stats_count_local_sources(self):
        stats = self.core.protection_stats()
        self.assertGreater(stats["domains"], 0)
        self.assertGreater(stats["seed_domains"], 0)
        self.assertEqual(stats["custom_domains"], 0)
        self.assertFalse(stats["active"])
        self.assertGreater(stats["encrypted_dns_endpoints"], 0)

    def test_custom_domain_is_persisted_outside_the_bundled_seed(self):
        success, message = self.core.add_custom_domain("https://exampleadult.test/path")
        self.assertFalse(success, message)  # URLs are deliberately not valid domains

        success, message = self.core.add_custom_domain("exampleadult.test")
        self.assertTrue(success, message)
        self.assertIn("exampleadult.test", self.core.load_domains())
        self.assertTrue(os.path.exists(self.core.custom_file))

if __name__ == "__main__":
    unittest.main()
