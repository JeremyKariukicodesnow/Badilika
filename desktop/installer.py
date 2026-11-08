"""
Badilika Windows Setup & Installer
Installs or Updates Badilika, places a Desktop shortcut with the custom leaf icon,
and adds Badilika to the Windows Start Menu.
"""

import os
import sys
import shutil
import subprocess
import tkinter as tk
from tkinter import ttk, messagebox
from PIL import Image, ImageTk

# Constants
APP_NAME = "Badilika"
DEFAULT_INSTALL_DIR = os.path.join(
    os.environ.get("LOCALAPPDATA", os.path.expanduser("~")),
    "Programs",
    APP_NAME
)

COLOR_TEAL = "#2e7d7a"
COLOR_TEAL_HOVER = "#246663"
COLOR_BG = "#ffffff"
COLOR_TEXT_MAIN = "#0f172a"
COLOR_TEXT_MUTED = "#64748b"


def create_shortcut(target_exe: str, shortcut_path: str, icon_path: str = None, description: str = ""):
    """Create a Windows .lnk shortcut using Windows Script Host."""
    try:
        ps_script = f'''
        $ws = New-Object -ComObject WScript.Shell
        $s = $ws.CreateShortcut("{shortcut_path}")
        $s.TargetPath = "{target_exe}"
        $s.WorkingDirectory = "{os.path.dirname(target_exe)}"
        $s.Description = "{description}"
        if ("{icon_path}" -ne "" -and (Test-Path "{icon_path}")) {{
            $s.IconLocation = "{icon_path}, 0"
        }}
        $s.Save()
        '''
        subprocess.run(
            ["powershell", "-NoProfile", "-Command", ps_script],
            capture_output=True,
            check=False,
            creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0
        )
        return True
    except Exception as e:
        print(f"Failed to create shortcut: {e}")
        return False


class BadilikaInstallerApp(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("Badilika Setup")
        self.geometry("520x420")
        self.resizable(False, False)
        self.configure(bg=COLOR_BG)

        # Base dir for resources
        if getattr(sys, "frozen", False):
            self.base_dir = getattr(sys, "_MEIPASS", os.path.dirname(os.path.abspath(__file__)))
        else:
            self.base_dir = os.path.dirname(os.path.abspath(__file__))

        self.assets_dir = os.path.join(self.base_dir, "assets")
        self.install_dir = DEFAULT_INSTALL_DIR

        # Set window icon
        ico_file = os.path.join(self.assets_dir, "badilika.ico")
        if os.path.exists(ico_file):
            try:
                self.iconbitmap(ico_file)
            except Exception:
                pass

        self.is_update = os.path.exists(os.path.join(self.install_dir, "Badilika.exe"))
        self._build_ui()

    def _build_ui(self):
        # Header banner
        header = tk.Frame(self, bg="#f8fafc", bd=0, padx=24, pady=20)
        header.pack(fill=tk.X)

        # Logo: the source artwork is a 256px illustration. Showing it at its
        # native size makes the setup window taller than its fixed geometry,
        # hiding the installation buttons (especially at 125–150% DPI).
        logo_path = os.path.join(self.assets_dir, "icons", "logo_leaf.png")
        if os.path.exists(logo_path):
            try:
                with Image.open(logo_path) as img:
                    img.thumbnail((52, 52), Image.Resampling.LANCZOS)
                    self.logo_img = ImageTk.PhotoImage(img.copy())
                lbl_ico = tk.Label(header, image=self.logo_img, bg="#f8fafc")
                lbl_ico.pack(side=tk.LEFT, padx=(0, 14))
            except Exception:
                # Setup must remain usable even if optional branding artwork is
                # unavailable or damaged.
                self.logo_img = None

        head_text_f = tk.Frame(header, bg="#f8fafc")
        head_text_f.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        t_text = "Update Badilika" if self.is_update else "Install Badilika"
        lbl_t = tk.Label(head_text_f, text=t_text, font=("Segoe UI", 16, "bold"), fg=COLOR_TEXT_MAIN, bg="#f8fafc")
        lbl_t.pack(anchor="w")

        sub_text = "An existing installation was found. Ready to upgrade." if self.is_update else "Focused web protection for a safer digital life."
        lbl_s = tk.Label(head_text_f, text=sub_text, font=("Segoe UI", 9), fg=COLOR_TEXT_MUTED, bg="#f8fafc")
        lbl_s.pack(anchor="w")

        # Divider line
        div = tk.Frame(self, bg="#e2e8f0", height=1)
        div.pack(fill=tk.X)

        # Body
        body = tk.Frame(self, bg=COLOR_BG, padx=28, pady=20)
        body.pack(fill=tk.BOTH, expand=True)

        if self.is_update:
            desc = (
                "An existing installation of Badilika was detected on this computer.\n\n"
                "Setup will upgrade Badilika to the latest version with the newest "
                "adult & escort blocklists, SafeSearch rules, and UI enhancements.\n\n"
                "✓ Your existing protection status and 5-hour cool-down will be preserved.\n"
                "✓ A desktop shortcut will be verified and updated."
            )
        else:
            desc = (
                "Welcome to Badilika Setup.\n\n"
                "This installer will set up Badilika on your PC and provide:\n\n"
                "• Dedicated adult and escort website blocking\n"
                "• Search engine SafeSearch enforcement\n"
                "• Anti-relapse 5-hour cool-down delay safeguard\n"
                "• Desktop shortcut and Start Menu integration"
            )

        lbl_desc = tk.Label(body, text=desc, font=("Segoe UI", 9), fg=COLOR_TEXT_MAIN, bg=COLOR_BG, justify="left", wraplength=460)
        lbl_desc.pack(anchor="w", pady=(0, 14))

        # Destination preview
        dest_box = tk.Frame(body, bg="#f8fafc", bd=1, relief="solid", highlightbackground="#e2e8f0", padx=10, pady=8)
        dest_box.pack(fill=tk.X, pady=(0, 14))
        tk.Label(dest_box, text=f"Install location: {self.install_dir}", font=("Segoe UI", 8), fg=COLOR_TEXT_MUTED, bg="#f8fafc").pack(anchor="w")

        # Bottom Action Bar
        bottom_bar = tk.Frame(self, bg="#f8fafc", bd=1, relief="solid", highlightbackground="#e2e8f0", padx=20, pady=14)
        bottom_bar.pack(fill=tk.X, side=tk.BOTTOM)

        btn_action_text = "Update Badilika Now" if self.is_update else "Install Badilika"
        self.btn_install = tk.Button(
            bottom_bar,
            text=btn_action_text,
            font=("Segoe UI", 10, "bold"),
            bg=COLOR_TEAL,
            fg="#ffffff",
            activebackground=COLOR_TEAL_HOVER,
            activeforeground="#ffffff",
            relief="flat",
            cursor="hand2",
            padx=16,
            pady=6,
            command=self._do_install
        )
        self.btn_install.pack(side=tk.RIGHT, padx=(10, 0))

        btn_cancel = tk.Button(
            bottom_bar,
            text="Cancel",
            font=("Segoe UI", 9),
            bg="#ffffff",
            fg=COLOR_TEXT_MAIN,
            relief="solid",
            bd=1,
            highlightbackground="#cbd5e1",
            cursor="hand2",
            padx=12,
            pady=5,
            command=self.destroy
        )
        btn_cancel.pack(side=tk.RIGHT)

    def _do_install(self):
        self.btn_install.config(state=tk.DISABLED, text="Installing...")
        self.update()

        try:
            os.makedirs(self.install_dir, exist_ok=True)

            # Copy Badilika.exe
            src_exe = os.path.join(self.base_dir, "Badilika.exe")
            target_exe = os.path.join(self.install_dir, "Badilika.exe")

            if os.path.exists(src_exe):
                pass
            else:
                # Fallback: if running from dev directory
                dev_exe = os.path.join(os.path.dirname(os.path.abspath(__file__)), "Badilika.exe")
                if os.path.exists(dev_exe):
                    src_exe = dev_exe
                else:
                    raise FileNotFoundError("Badilika.exe was not included with this setup program.")

            # Copy to a temporary name first so a failed/blocked update never
            # leaves the installed shortcut pointing at a partial executable.
            staged_exe = target_exe + ".new"
            shutil.copy2(src_exe, staged_exe)
            os.replace(staged_exe, target_exe)

            # Copy adult_domains.txt
            src_domains = os.path.join(self.base_dir, "adult_domains.txt")
            if os.path.exists(src_domains):
                shutil.copy2(src_domains, os.path.join(self.install_dir, "adult_domains.txt"))

            # Copy assets directory
            target_assets = os.path.join(self.install_dir, "assets")
            if os.path.exists(self.assets_dir):
                if os.path.exists(target_assets):
                    shutil.rmtree(target_assets, ignore_errors=True)
                shutil.copytree(self.assets_dir, target_assets)

            target_ico = os.path.join(target_assets, "badilika.ico")

            # 1. Create Desktop Shortcut
            desktop_dir = os.path.join(os.environ.get("USERPROFILE", os.path.expanduser("~")), "Desktop")
            desktop_shortcut = os.path.join(desktop_dir, "Badilika.lnk")
            create_shortcut(
                target_exe=target_exe,
                shortcut_path=desktop_shortcut,
                icon_path=target_ico,
                description="Badilika - Focused Web Protection"
            )

            # 2. Create Start Menu Shortcut
            start_menu_dir = os.path.join(
                os.environ.get("APPDATA", os.path.expanduser("~")),
                r"Microsoft\Windows\Start Menu\Programs"
            )
            start_shortcut = os.path.join(start_menu_dir, "Badilika.lnk")
            create_shortcut(
                target_exe=target_exe,
                shortcut_path=start_shortcut,
                icon_path=target_ico,
                description="Badilika - Focused Web Protection"
            )

            # Success message
            msg = (
                "Badilika has been successfully updated!\n\n" if self.is_update else "Badilika has been successfully installed!\n\n"
            )
            msg += "A shortcut with the Badilika leaf icon has been created on your Desktop.\n\nWould you like to launch Badilika now?"

            if messagebox.askyesno("Setup Complete", msg):
                subprocess.Popen([target_exe], creationflags=subprocess.DETACHED_PROCESS if os.name == "nt" else 0)

            self.destroy()

        except Exception as e:
            messagebox.showerror("Setup Error", f"An error occurred during installation:\n{e}")
            self.btn_install.config(state=tk.NORMAL, text="Retry")


if __name__ == "__main__":
    app = BadilikaInstallerApp()
    app.mainloop()
