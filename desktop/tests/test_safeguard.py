"""
Unit tests for Safeguard
"""

import os
import sys
import tempfile
import time
import unittest

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
from safeguard import Safeguard, SafeguardState

class TestSafeguard(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        # Use a short cooldown of 3 seconds for testing purposes
        self.safeguard = Safeguard(
            state_dir=self.temp_dir.name,
            cooldown_seconds=3
        )

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_initial_state(self):
        status = self.safeguard.get_status()
        self.assertEqual(status["status"], SafeguardState.UNLOCKED)
        self.assertEqual(status["remaining_seconds"], 0)

    def test_lock_and_request_unblock(self):
        self.safeguard.lock()
        status = self.safeguard.get_status()
        self.assertEqual(status["status"], SafeguardState.LOCKED)

        # Request unblock triggers cooldown
        success, msg = self.safeguard.request_unblock()
        self.assertTrue(success, msg)

        status = self.safeguard.get_status()
        self.assertEqual(status["status"], SafeguardState.COOLDOWN)
        self.assertGreater(status["remaining_seconds"], 0)

        # Early confirm should fail
        success_early, msg_early = self.safeguard.confirm_unblock()
        self.assertFalse(success_early)

        # Cancel unblock restores lock
        success_cancel, msg_cancel = self.safeguard.cancel_unblock_request()
        self.assertTrue(success_cancel)
        status = self.safeguard.get_status()
        self.assertEqual(status["status"], SafeguardState.LOCKED)

    def test_cooldown_expiration(self):
        self.safeguard.lock()
        self.safeguard.request_unblock()

        # Wait for the 3-second cooldown to elapse
        time.sleep(3.2)

        status = self.safeguard.get_status()
        self.assertEqual(status["status"], SafeguardState.READY_TO_UNLOCK)
        self.assertEqual(status["remaining_seconds"], 0)

        # Confirm unblock now succeeds
        success, msg = self.safeguard.confirm_unblock()
        self.assertTrue(success)
        self.assertEqual(self.safeguard.get_status()["status"], SafeguardState.UNLOCKED)

    def test_cooldown_setting_persists_and_cannot_change_while_active(self):
        success, message = self.safeguard.set_cooldown_seconds(30)
        self.assertTrue(success, message)
        self.assertEqual(self.safeguard.get_cooldown_seconds(), 30)

        self.safeguard.lock()
        self.safeguard.request_unblock()
        success, message = self.safeguard.set_cooldown_seconds(60)
        self.assertFalse(success, message)
        self.assertEqual(self.safeguard.get_cooldown_seconds(), 30)

if __name__ == "__main__":
    unittest.main()
