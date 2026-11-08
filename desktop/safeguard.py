"""
NewLife Desktop Blocker - Safeguard Engine
Manages the 5-Hour Cool-Down delay, persistent state,
tamper detection, and reflection safeguards.
"""

import json
import os
import time
from typing import Dict, Any, Tuple

DEFAULT_COOLDOWN_SECONDS = 5 * 3600  # 5 hours
MIN_COOLDOWN_SECONDS = 30
MAX_COOLDOWN_SECONDS = 24 * 3600

class SafeguardState:
    LOCKED = "LOCKED"                      # Protection is active and locked
    COOLDOWN = "COOLDOWN"                  # User requested unblock; 5-hr timer running; content still blocked
    READY_TO_UNLOCK = "READY_TO_UNLOCK"    # 5 hours elapsed; user may confirm unblock
    UNLOCKED = "UNLOCKED"                  # Protection is currently disabled


class Safeguard:
    def __init__(self, state_dir: str = None, cooldown_seconds: int = DEFAULT_COOLDOWN_SECONDS):
        if state_dir:
            self.state_dir = state_dir
        else:
            appdata = os.environ.get("APPDATA", os.path.expanduser("~"))
            self.state_dir = os.path.join(appdata, "Badilika")
            # Legacy migration
            legacy_dir = os.path.join(appdata, "NewLifeBlocker")
            legacy_file = os.path.join(legacy_dir, "safeguard_state.json")
            new_file = os.path.join(self.state_dir, "safeguard_state.json")
            if os.path.exists(legacy_file) and not os.path.exists(new_file):
                try:
                    os.makedirs(self.state_dir, exist_ok=True)
                    import shutil
                    shutil.copy2(legacy_file, new_file)
                except Exception:
                    pass

        os.makedirs(self.state_dir, exist_ok=True)
        self.state_file = os.path.join(self.state_dir, "safeguard_state.json")
        self.cooldown_seconds = cooldown_seconds
        self._load_state()

    def _load_state(self) -> None:
        """Load state from persistent storage or initialize defaults."""
        defaults = {
            "status": SafeguardState.UNLOCKED,
            "requested_at": 0.0,
            "unlock_eligible_at": 0.0,
            "last_verified_time": time.time(),
            "protection_streak_start": time.time(),
            "total_unblock_requests": 0,
            "cooldown_seconds": self.cooldown_seconds,
        }

        if os.path.exists(self.state_file):
            try:
                with open(self.state_file, "r", encoding="utf-8") as f:
                    self.data = json.load(f)
                    # Fill any missing keys
                    for k, v in defaults.items():
                        if k not in self.data:
                            self.data[k] = v
            except Exception:
                self.data = defaults
        else:
            self.data = defaults
            self._save_state()

        # The saved choice is the source of truth after the first launch.
        self.cooldown_seconds = int(self.data.get("cooldown_seconds", self.cooldown_seconds))

    def _save_state(self) -> None:
        """Save current state to JSON file."""
        try:
            with open(self.state_file, "w", encoding="utf-8") as f:
                json.dump(self.data, f, indent=2)
        except Exception:
            pass

    def get_status(self) -> Dict[str, Any]:
        """
        Evaluate current state and return status dictionary with remaining time.
        Detects clock tampering if system time went backwards.
        """
        now = time.time()
        last_verified = self.data.get("last_verified_time", now)

        # Basic clock tamper detection: if time went backwards by more than 60 seconds
        clock_tampered = now < (last_verified - 60)
        if not clock_tampered:
            self.data["last_verified_time"] = now
            self._save_state()

        status = self.data.get("status", SafeguardState.UNLOCKED)
        unlock_at = self.data.get("unlock_eligible_at", 0.0)

        remaining_seconds = 0
        if status == SafeguardState.COOLDOWN:
            remaining_seconds = max(0, int(unlock_at - now))
            if remaining_seconds <= 0 and not clock_tampered:
                status = SafeguardState.READY_TO_UNLOCK
                self.data["status"] = status
                self._save_state()

        streak_hours = max(0, int((now - self.data.get("protection_streak_start", now)) / 3600))

        return {
            "status": status,
            "remaining_seconds": remaining_seconds,
            "unlock_eligible_at": unlock_at,
            "clock_tampered": clock_tampered,
            "streak_hours": streak_hours,
        }

    def lock(self) -> None:
        """Immediately lock and activate protection."""
        now = time.time()
        self.data["status"] = SafeguardState.LOCKED
        self.data["requested_at"] = 0.0
        self.data["unlock_eligible_at"] = 0.0
        self.data["last_verified_time"] = now
        if not self.data.get("protection_streak_start"):
            self.data["protection_streak_start"] = now
        self._save_state()

    def request_unblock(self) -> Tuple[bool, str]:
        """Initiate the 5-hour cool-down delay."""
        now = time.time()
        current_status = self.data.get("status")

        if current_status == SafeguardState.UNLOCKED:
            return False, "Protection is already inactive."

        if current_status == SafeguardState.COOLDOWN:
            status = self.get_status()
            rem = status["remaining_seconds"]
            return False, f"Cool-down is already active. {self.format_remaining(rem)} remaining."

        # Start 5-hour cooldown
        self.data["status"] = SafeguardState.COOLDOWN
        self.data["requested_at"] = now
        self.data["unlock_eligible_at"] = now + self.cooldown_seconds
        self.data["last_verified_time"] = now
        self.data["total_unblock_requests"] = self.data.get("total_unblock_requests", 0) + 1
        self._save_state()

        return True, f"{self.format_remaining(self.cooldown_seconds)} cool-down initiated. Protection remains fully active."

    def get_cooldown_seconds(self) -> int:
        """Return the duration chosen for the next unblock request."""
        return self.cooldown_seconds

    def set_cooldown_seconds(self, seconds: int) -> Tuple[bool, str]:
        """Save a new cooldown duration, but never alter an active countdown."""
        try:
            seconds = int(seconds)
        except (TypeError, ValueError):
            return False, "Choose a valid cool-down duration."
        if not MIN_COOLDOWN_SECONDS <= seconds <= MAX_COOLDOWN_SECONDS:
            return False, "Choose a duration from 30 seconds to 24 hours."

        status = self.get_status()["status"]
        if status == SafeguardState.COOLDOWN:
            return False, "A cool-down is active. Its duration cannot be changed until it finishes or is cancelled."

        self.cooldown_seconds = seconds
        self.data["cooldown_seconds"] = seconds
        self._save_state()
        return True, f"Cool-down set to {self.format_remaining(seconds)}. This applies to your next request."

    def cancel_unblock_request(self) -> Tuple[bool, str]:
        """User changed their mind during cool-down. Re-lock immediately."""
        self.lock()
        return True, "Unblock request canceled! Shield is locked and secure."

    def confirm_unblock(self) -> Tuple[bool, str]:
        """Confirm unblock after 5-hour delay has elapsed."""
        status = self.get_status()
        if status["clock_tampered"]:
            return False, "Clock tampering detected. Please synchronize system time."

        if status["status"] != SafeguardState.READY_TO_UNLOCK and status["status"] != SafeguardState.UNLOCKED:
            rem = status["remaining_seconds"]
            return False, f"Cannot unblock yet. Cool-down is still in progress ({self.format_remaining(rem)} remaining)."

        now = time.time()
        self.data["status"] = SafeguardState.UNLOCKED
        self.data["requested_at"] = 0.0
        self.data["unlock_eligible_at"] = 0.0
        self.data["last_verified_time"] = now
        self.data["protection_streak_start"] = 0.0
        self._save_state()

        return True, "Protection unlocked successfully."

    @staticmethod
    def format_remaining(seconds: int) -> str:
        """Format seconds into HH:MM:SS format."""
        hours = seconds // 3600
        minutes = (seconds % 3600) // 60
        secs = seconds % 60
        return f"{hours:02d}h {minutes:02d}m {secs:02d}s"
