"""
Badilika Desktop Blocker - Main Entrypoint
Checks Windows Administrator privileges, handles UAC elevation,
initializes the core blocker engine, and starts the GUI.
"""

import ctypes
import os
import subprocess
import sys
import traceback

from blocker_core import BlockerCore
from safeguard import Safeguard
from gui import BadilikaApp


def is_admin() -> bool:
    """Check if the script is running with administrative privileges."""
    try:
        return ctypes.windll.shell32.IsUserAnAdmin() != 0
    except Exception:
        return False


def run_as_admin() -> bool:
    """Request UAC elevation and re-launch the script."""
    # A frozen application must elevate the executable itself.  Passing the
    # executable as an argument makes one-file builds relaunch incorrectly.
    if getattr(sys, "frozen", False):
        executable = sys.executable
        params = subprocess.list2cmdline(sys.argv[1:])
    else:
        executable = sys.executable
        params = subprocess.list2cmdline([os.path.abspath(sys.argv[0]), *sys.argv[1:]])
    try:
        # Runas verb requests Windows UAC prompt
        ret = ctypes.windll.shell32.ShellExecuteW(
            None,
            "runas",
            executable,
            params,
            None,
            1  # SW_SHOWNORMAL
        )
        if ret > 32:
            # Elevation request succeeded, terminate current un-elevated process
            return True
    except Exception as e:
        print(f"Elevation request failed: {e}")
    return False


def main():
    # Enforce Administrator privileges on Windows so hosts file can be updated
    if os.name == "nt" and not is_admin():
        print("Administrator privileges required. Requesting elevation...")
        if run_as_admin():
            return
        # If the user denies UAC, the original process must not continue.
        print("Administrator privileges were not granted. Exiting.")
        sys.exit(1)

    blocker = BlockerCore()
    safeguard = Safeguard()

    app = BadilikaApp(blocker=blocker, safeguard=safeguard)
    app.mainloop()


if __name__ == "__main__":
    try:
        main()
    except Exception:
        # Windowed PyInstaller builds otherwise hide startup exceptions.  Keep
        # a local diagnostic that can be supplied with a support request.
        log_dir = os.path.join(os.environ.get("APPDATA", os.path.expanduser("~")), "Badilika")
        os.makedirs(log_dir, exist_ok=True)
        with open(os.path.join(log_dir, "startup-error.log"), "w", encoding="utf-8") as log_file:
            traceback.print_exc(file=log_file)
        raise
