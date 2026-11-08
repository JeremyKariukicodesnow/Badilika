"""
NewLife Desktop Blocker - Modern Nature-Themed GUI
Faithfully implements the clean NewLife design mockup with scenic misty forest
background, clean sidebar navigation, crisp custom icons (no emojis), and
responsive 5-hour cool-down safeguard screens.
"""

import datetime
import os
import sys
import threading
import time
import tkinter as tk
from tkinter import ttk
from PIL import Image, ImageOps, ImageTk

from blocker_core import BlockerCore
from safeguard import Safeguard, SafeguardState

# Color Palette matching the mockup
COLOR_TEAL_PRIMARY = "#184838"
COLOR_TEAL_HOVER = "#123b2e"
COLOR_SLATE_BTN = "#4e6c61"
COLOR_SLATE_BTN_HOVER = "#3e5b51"
COLOR_ACCENT_BLUE = "#184838"
COLOR_ACCENT_BLUE_HOVER = "#123b2e"

COLOR_CARD_BG = "#ffffff"
COLOR_SIDEBAR_BG = "#f5f7ef"
COLOR_SIDEBAR_BORDER = "#dfe7da"
COLOR_TEXT_MAIN = "#1d3128"
COLOR_TEXT_MUTED = "#60736a"
COLOR_TEXT_LIGHT = "#899a92"

COLOR_GREEN_BG = "#f0fdf4"
COLOR_GREEN_BORDER = "#bbf7d0"
COLOR_GREEN_TEXT = "#15803d"

COLOR_RED_BG = "#fef2f2"
COLOR_RED_BORDER = "#fecaca"
COLOR_RED_TEXT = "#dc2626"

COLOR_AMBER_BG = "#fffbeb"
COLOR_AMBER_BORDER = "#fde68a"
COLOR_AMBER_TEXT = "#b45309"

COLOR_ORANGE_BG = "#fff7ed"
COLOR_ORANGE_BORDER = "#fed7aa"
COLOR_ORANGE_TEXT = "#c2410c"

COOLDOWN_PRESETS = {
    "30 seconds": 30,
    "5 minutes": 5 * 60,
    "30 minutes": 30 * 60,
    "1 hour": 60 * 60,
    "5 hours": 5 * 60 * 60,
    "12 hours": 12 * 60 * 60,
    "24 hours": 24 * 60 * 60,
}


class BadilikaApp(tk.Tk):
    def __init__(self, blocker: BlockerCore, safeguard: Safeguard):
        super().__init__()
        self.blocker = blocker
        self.safeguard = safeguard

        self.title("Badilika - Focused Web Protection")
        self.geometry("1280x820")
        self.minsize(1080, 740)
        self.configure(bg="#1a202c")

        # Resolve assets path for both dev and PyInstaller bundle
        if getattr(sys, "frozen", False):
            self.base_dir = getattr(sys, "_MEIPASS", os.path.dirname(os.path.abspath(__file__)))
        else:
            self.base_dir = os.path.dirname(os.path.abspath(__file__))

        self.assets_dir = os.path.join(self.base_dir, "assets")
        self.icons_dir = os.path.join(self.assets_dir, "icons")

        ico_file = os.path.join(self.assets_dir, "badilika.ico")
        if os.path.exists(ico_file):
            try:
                self.iconbitmap(ico_file)
            except Exception:
                pass

        # Startup synchronization
        if self.blocker.is_blocking_active() and self.safeguard.get_status()["status"] == SafeguardState.UNLOCKED:
            self.safeguard.lock()
        elif not self.blocker.is_blocking_active() and self.safeguard.get_status()["status"] == SafeguardState.LOCKED:
            self.blocker.enable_blocking()

        self.current_nav = "home"
        self._home_state_signature = None
        self._countdown_label = None
        self._countdown_deadline_label = None
        self._load_images_and_icons()
        self._build_scenic_ui()
        self._start_timer_loop()

    def _load_images_and_icons(self):
        """Preload all icons and the background image."""
        self.icon_cache = {}
        icon_names = [
            "logo_leaf", "nav_home", "nav_home_active", "nav_rules",
            "nav_settings", "nav_about", "badge_unlocked", "badge_shield_active",
            "badge_hourglass", "badge_alert", "diag_globe", "diag_search",
            "diag_list", "diag_whitelist", "action_plus", "action_globe"
        ]
        image_sizes = {
            "logo_leaf": (58, 58),
            "badge_unlocked": (54, 54), "badge_shield_active": (54, 54),
            "badge_hourglass": (54, 54), "badge_alert": (54, 54),
        }
        for name in icon_names:
            p = os.path.join(self.icons_dir, f"{name}.png")
            if os.path.exists(p):
                try:
                    with Image.open(p) as img:
                        img.thumbnail(image_sizes.get(name, (22, 22)), Image.Resampling.LANCZOS)
                        self.icon_cache[name] = ImageTk.PhotoImage(img.copy())
                except Exception:
                    # Missing/corrupt optional artwork must never prevent the
                    # protection controls from opening.
                    continue

        # Background landscape photo
        bg_path = os.path.join(self.assets_dir, "background.jpg")
        if os.path.exists(bg_path):
            try:
                with Image.open(bg_path) as image:
                    self.orig_bg = image.copy()
            except Exception:
                self.orig_bg = None
        else:
            self.orig_bg = None
        self.bg_photo = None
    def _build_scenic_ui(self):
        """Construct the UI overlaying the scenic misty forest background."""
        self.canvas = tk.Canvas(self, highlightthickness=0, bd=0, bg="#e5edf0")
        self.canvas.pack(fill=tk.BOTH, expand=True)

        # Central Floating Card Window (as shown in the mockup)
        self.card_window = tk.Frame(self.canvas, bg=COLOR_CARD_BG, bd=1, relief="solid", highlightbackground="#cbd5e1")
        self.card_window_id = self.canvas.create_window(0, 0, window=self.card_window, anchor="center")

        self._build_card_inner()
        # Bind only the canvas. Binding the toplevel also receives Configure
        # notifications from descendants on some Tk builds, which resized the
        # whole dashboard using a child widget's tiny dimensions.
        self.canvas.bind("<Configure>", self._on_window_resize)
        self.after_idle(self._layout_scenic_ui)

    def _layout_scenic_ui(self):
        self._on_window_resize(None)

    def _on_window_resize(self, event=None):
        """Resize background smoothly and center the application card."""
        w = self.canvas.winfo_width()
        h = self.canvas.winfo_height()
        if w < 100 or h < 100:
            return

        if self.orig_bg:
            resized = ImageOps.fit(self.orig_bg, (w, h), Image.Resampling.LANCZOS, centering=(0.5, 0.45))
            self.bg_photo = ImageTk.PhotoImage(resized)
            self.canvas.delete("bg_img")
            self.canvas.create_image(0, 0, image=self.bg_photo, anchor="nw", tags="bg_img")
            self.canvas.tag_lower("bg_img")

        card_w = min(1120, w - 110)
        card_h = min(720, h - 90)
        self.canvas.coords(self.card_window_id, w // 2, h // 2)
        self.canvas.itemconfigure(self.card_window_id, width=card_w, height=card_h)

    def _build_card_inner(self):
        # 1. Left Sidebar
        self.sidebar = tk.Frame(self.card_window, bg=COLOR_SIDEBAR_BG, width=210, bd=0)
        self.sidebar.pack(side=tk.LEFT, fill=tk.Y)
        self.sidebar.pack_propagate(False)

        sep = tk.Frame(self.card_window, bg=COLOR_SIDEBAR_BORDER, width=1)
        sep.pack(side=tk.LEFT, fill=tk.Y)

        # Logo at top of sidebar
        logo_frame = tk.Frame(self.sidebar, bg=COLOR_SIDEBAR_BG)
        logo_frame.pack(fill=tk.X, padx=22, pady=(28, 32))
        if "logo_leaf" in self.icon_cache:
            lbl_logo_icon = tk.Label(logo_frame, image=self.icon_cache["logo_leaf"], bg=COLOR_SIDEBAR_BG)
            lbl_logo_icon.pack(side=tk.LEFT, padx=(0, 6))
        lbl_logo_text = tk.Label(logo_frame, text="Badilika", font=("Segoe UI", 17, "bold"), fg=COLOR_TEXT_MAIN, bg=COLOR_SIDEBAR_BG)
        lbl_logo_text.pack(side=tk.LEFT)

        self.nav_btns = {}
        nav_items = [
            ("home", "Home", "nav_home", "nav_home_active"),
            ("rules", "Rules", "nav_rules", "nav_rules"),
            ("settings", "Settings", "nav_settings", "nav_settings"),
            ("about", "About", "nav_about", "nav_about"),
        ]
        for key, text, icon_default, icon_active in nav_items:
            f = tk.Frame(self.sidebar, bg=COLOR_SIDEBAR_BG, cursor="hand2", padx=14, pady=11)
            f.pack(fill=tk.X, padx=16, pady=4)
            
            icon_lbl = tk.Label(f, image=self.icon_cache.get(icon_default), bg=COLOR_SIDEBAR_BG)
            icon_lbl.pack(side=tk.LEFT, padx=(4, 8))
            
            txt_lbl = tk.Label(f, text=text, font=("Segoe UI", 10, "bold" if key == "home" else "normal"),
                               fg=COLOR_TEAL_PRIMARY if key == "home" else COLOR_TEXT_MUTED, bg=COLOR_SIDEBAR_BG)
            txt_lbl.pack(side=tk.LEFT)

            def make_handler(k):
                return lambda e: self._switch_nav(k)

            f.bind("<Button-1>", make_handler(key))
            icon_lbl.bind("<Button-1>", make_handler(key))
            txt_lbl.bind("<Button-1>", make_handler(key))

            self.nav_btns[key] = (f, icon_lbl, txt_lbl, icon_default, icon_active)

        bottom_tag = tk.Label(
            self.sidebar,
            text="A safer\ndigital life\nstarts here.",
            font=("Segoe UI", 10),
            fg=COLOR_TEXT_LIGHT,
            bg=COLOR_SIDEBAR_BG,
            justify="left"
        )
        bottom_tag.pack(side=tk.BOTTOM, anchor="w", padx=16, pady=16)

        # 2. Main Content Area
        self.content_area = tk.Frame(self.card_window, bg=COLOR_CARD_BG, padx=42, pady=34)
        self.content_area.pack(side=tk.RIGHT, fill=tk.BOTH, expand=True)

        self._render_home_tab()

    def _switch_nav(self, tab_key: str):
        self.current_nav = tab_key
        for k, (f, icon_lbl, txt_lbl, icon_default, icon_active) in self.nav_btns.items():
            if k == tab_key:
                f.config(bg="#f1f5f9")
                icon_lbl.config(image=self.icon_cache.get(icon_active, self.icon_cache.get(icon_default)), bg="#f1f5f9")
                txt_lbl.config(fg=COLOR_TEAL_PRIMARY, font=("Segoe UI", 9, "bold"), bg="#f1f5f9")
            else:
                f.config(bg=COLOR_SIDEBAR_BG)
                icon_lbl.config(image=self.icon_cache.get(icon_default), bg=COLOR_SIDEBAR_BG)
                txt_lbl.config(fg=COLOR_TEXT_MUTED, font=("Segoe UI", 9, "normal"), bg=COLOR_SIDEBAR_BG)

        for child in self.content_area.winfo_children():
            child.destroy()

        if tab_key == "home":
            self._render_home_tab()
        elif tab_key == "rules":
            self._render_rules_tab()
        elif tab_key == "settings":
            self._render_settings_tab()
        elif tab_key == "about":
            self._render_about_tab()
    def _render_home_tab(self):
        status_info = self.safeguard.get_status()
        status = status_info["status"]
        rem_sec = status_info["remaining_seconds"]
        unlock_at = status_info["unlock_eligible_at"]
        self._home_state_signature = (status, unlock_at)
        self._countdown_label = None
        self._countdown_deadline_label = None

        header_frame = tk.Frame(self.content_area, bg=COLOR_CARD_BG)
        header_frame.pack(fill=tk.X, pady=(0, 12))

        if status == SafeguardState.UNLOCKED:
            h_title = "Good to see you"
            h_sub = "Take control of your browsing environment."
        elif status == SafeguardState.LOCKED:
            h_title = "You're protected"
            h_sub = "Badilika is keeping you safer online."
        elif status == SafeguardState.COOLDOWN:
            h_title = "Stay strong"
            h_sub = "You're doing great. Keep going."
        else:
            h_title = "Final step"
            h_sub = "You're in control of what happens next."

        lbl_head = tk.Label(header_frame, text=h_title, font=("Segoe UI", 22, "bold"), fg=COLOR_TEXT_MAIN, bg=COLOR_CARD_BG)
        lbl_head.pack(anchor="w")
        lbl_sub = tk.Label(header_frame, text=h_sub, font=("Segoe UI", 11), fg=COLOR_TEXT_MUTED, bg=COLOR_CARD_BG)
        lbl_sub.pack(anchor="w")

        if status == SafeguardState.UNLOCKED:
            card_bg, card_border, badge_icon, badge_title, badge_sub, title_color = (
                COLOR_RED_BG, COLOR_RED_BORDER, "badge_unlocked",
                "Shield Inactive", "Protection is currently disabled. Adult sites and escorts are accessible.", COLOR_RED_TEXT
            )
        elif status == SafeguardState.LOCKED:
            card_bg, card_border, badge_icon, badge_title, badge_sub, title_color = (
                COLOR_GREEN_BG, COLOR_GREEN_BORDER, "badge_shield_active",
                "Shield Active & Locked", "Adult domains and escort sites are blocked.", COLOR_GREEN_TEXT
            )
        elif status == SafeguardState.COOLDOWN:
            card_bg, card_border, badge_icon, badge_title, badge_sub, title_color = (
                COLOR_AMBER_BG, COLOR_AMBER_BORDER, "badge_hourglass",
                "Cool-down Delay in Progress", "A short break helps you stay in control.", COLOR_AMBER_TEXT
            )
        else:
            card_bg, card_border, badge_icon, badge_title, badge_sub, title_color = (
                COLOR_ORANGE_BG, COLOR_ORANGE_BORDER, "badge_alert",
                "Cool-down Finished", "The 5-hour reflection period has completed.", COLOR_ORANGE_TEXT
            )

        status_card = tk.Frame(self.content_area, bg=card_bg, bd=1, relief="solid", highlightbackground=card_border)
        status_card.pack(fill=tk.X, pady=(0, 18), ipady=14, ipadx=16)

        if badge_icon in self.icon_cache:
            icon_box = tk.Label(status_card, image=self.icon_cache[badge_icon], bg=card_bg)
            icon_box.pack(side=tk.LEFT, padx=(10, 14), pady=4)

        text_box = tk.Frame(status_card, bg=card_bg)
        text_box.pack(side=tk.LEFT, fill=tk.BOTH, expand=True, pady=4)

        lbl_card_title = tk.Label(text_box, text=badge_title, font=("Segoe UI", 15, "bold"), fg=title_color, bg=card_bg)
        lbl_card_title.pack(anchor="w")

        lbl_card_desc = tk.Label(text_box, text=badge_sub, font=("Segoe UI", 10), fg=COLOR_TEXT_MUTED, bg=card_bg, wraplength=660, justify="left")
        lbl_card_desc.pack(anchor="w", pady=(2, 0))

        if status in (SafeguardState.UNLOCKED, SafeguardState.LOCKED):
            self._render_diagnostics_table(is_locked=(status == SafeguardState.LOCKED))
        elif status == SafeguardState.COOLDOWN:
            self._render_cooldown_box(rem_sec, unlock_at)
        elif status == SafeguardState.READY_TO_UNLOCK:
            self._render_ready_to_unlock_box()

        self._render_action_buttons(status, rem_sec)

    def _render_diagnostics_table(self, is_locked: bool):
        diag_frame = tk.Frame(self.content_area, bg=COLOR_CARD_BG)
        diag_frame.pack(fill=tk.X, pady=(0, 14))

        lbl_title = tk.Label(diag_frame, text="Shield Diagnostics & Rules", font=("Segoe UI", 13, "bold"), fg=COLOR_TEXT_MAIN, bg=COLOR_CARD_BG)
        lbl_title.pack(anchor="w", pady=(0, 6))

        stats = self.blocker.protection_stats()
        metric_row = tk.Frame(diag_frame, bg=COLOR_CARD_BG)
        metric_row.pack(fill=tk.X, pady=(0, 10))
        metrics = [
            ("Protected domains", f"{stats['domains']:,}", "Bundled, updated & custom"),
            ("DNS bypass endpoints", str(stats['encrypted_dns_endpoints']), "Common public resolvers"),
            ("Custom rules", str(stats['custom_domains']), "Domains you added"),
        ]
        for index, (label, value, detail) in enumerate(metrics):
            card = tk.Frame(metric_row, bg="#f1f6ee", bd=1, relief="solid", highlightbackground="#dce8d7", padx=10, pady=9)
            card.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=(0 if index == 0 else 4, 0 if index == len(metrics)-1 else 4))
            tk.Label(card, text=label.upper(), font=("Segoe UI", 7, "bold"), fg=COLOR_TEXT_MUTED, bg="#f1f6ee").pack(anchor="w")
            tk.Label(card, text=value, font=("Segoe UI", 18, "bold"), fg=COLOR_TEAL_PRIMARY, bg="#f1f6ee").pack(anchor="w")
            tk.Label(card, text=detail, font=("Segoe UI", 7), fg=COLOR_TEXT_MUTED, bg="#f1f6ee").pack(anchor="w")

        rows = [
            ("diag_globe", "Blocked Domains", f"{stats['domains']:,} local rules active", COLOR_TEXT_MUTED),
            ("diag_search", "SafeSearch", "Enforced (Google, Bing, DuckDuckGo)" if is_locked else "Inactive", COLOR_GREEN_TEXT if is_locked else COLOR_RED_TEXT),
            ("diag_list", "Escorts & Adult Classifieds", "Filtered & Blocked" if is_locked else "Not filtered", COLOR_GREEN_TEXT if is_locked else COLOR_RED_TEXT),
            ("diag_globe", "Encrypted DNS guard", "Common DoH/DoT endpoints blocked" if is_locked else "Inactive", COLOR_GREEN_TEXT if is_locked else COLOR_RED_TEXT),
            ("diag_whitelist", "Whitelist", "Movies & Social services (Netflix, Prime, Reddit, X)", COLOR_TEXT_MUTED),
        ]

        for icon_key, label_t, val_t, val_col in rows:
            row_f = tk.Frame(diag_frame, bg="#f8fafc", padx=10, pady=8)
            row_f.pack(fill=tk.X, pady=3)

            if icon_key in self.icon_cache:
                ic = tk.Label(row_f, image=self.icon_cache[icon_key], bg="#f8fafc")
                ic.pack(side=tk.LEFT, padx=(2, 8))

            lbl_name = tk.Label(row_f, text=label_t, font=("Segoe UI", 10), fg=COLOR_TEXT_MAIN, bg="#f8fafc")
            lbl_name.pack(side=tk.LEFT)

            lbl_val = tk.Label(row_f, text=val_t, font=("Segoe UI", 9, "bold" if "Enforced" in val_t or "Filtered" in val_t else "normal"),
                               fg=val_col, bg="#f8fafc", justify="right")
            lbl_val.pack(side=tk.RIGHT)

    def _render_cooldown_box(self, rem_sec: int, unlock_at: float):
        cool_frame = tk.Frame(self.content_area, bg="#f8fafc", bd=1, relief="solid", highlightbackground="#e2e8f0", padx=14, pady=12)
        cool_frame.pack(fill=tk.X, pady=(0, 10))

        self._countdown_label = tk.Label(
            cool_frame,
            text=self.safeguard.format_remaining(rem_sec),
            font=("Segoe UI", 32, "bold"),
            fg=COLOR_TEXT_MAIN,
            bg="#f8fafc"
        )
        self._countdown_label.pack()

        dt_str = datetime.datetime.fromtimestamp(unlock_at).strftime("%I:%M %p")
        self._countdown_deadline_label = tk.Label(
            cool_frame,
            text=f"Deadline: Protection can be disabled after {dt_str}",
            font=("Segoe UI", 10),
            fg=COLOR_TEXT_LIGHT,
            bg="#f8fafc"
        )
        self._countdown_deadline_label.pack(pady=(2, 8))

        advice_f = tk.Frame(cool_frame, bg="#ffffff", bd=1, relief="solid", highlightbackground="#e2e8f0", padx=10, pady=8)
        advice_f.pack(fill=tk.X, pady=(0, 10))

        lbl_adv = tk.Label(
            advice_f,
            text="Protection remains 100% active. Step away from the screen, take a walk, or drink water. You've got this.",
            font=("Segoe UI", 8),
            fg=COLOR_TEXT_MUTED,
            bg="#ffffff",
            wraplength=640,
            justify="center"
        )
        lbl_adv.pack()

        btn_keep = tk.Button(
            cool_frame,
            text="I Changed My Mind (Keep Me Safe)",
            font=("Segoe UI", 9, "bold"),
            bg="#e0f2fe",
            fg="#0369a1",
            activebackground="#bae6fd",
            activeforeground="#0284c7",
            relief="flat",
            cursor="hand2",
            padx=12,
            pady=6,
            command=self._on_cancel_unblock
        )
        btn_keep.pack()

    def _render_ready_to_unlock_box(self):
        ready_frame = tk.Frame(self.content_area, bg="#ffffff", bd=1, relief="solid", highlightbackground="#e2e8f0", padx=12, pady=10)
        ready_frame.pack(fill=tk.X, pady=(0, 10))

        lbl_q = tk.Label(
            ready_frame,
            text="Are you certain you want to turn off your shield?\nStaying protected helps you avoid risky content and maintain a healthier, more focused life.",
            font=("Segoe UI", 8),
            fg=COLOR_TEXT_MUTED,
            bg="#ffffff",
            justify="center"
        )
        lbl_q.pack(pady=(0, 8))

        btn_keep_locked = tk.Button(
            ready_frame,
            text="Keep Shield Locked (Recommended)",
            font=("Segoe UI", 9, "bold"),
            bg=COLOR_TEAL_PRIMARY,
            fg="#ffffff",
            activebackground=COLOR_TEAL_HOVER,
            activeforeground="#ffffff",
            relief="flat",
            cursor="hand2",
            pady=7,
            command=self._on_cancel_unblock
        )
        btn_keep_locked.pack(fill=tk.X, pady=(0, 6))

        btn_confirm_off = tk.Button(
            ready_frame,
            text="Confirm Disable Protection",
            font=("Segoe UI", 9),
            bg="#f1f5f9",
            fg=COLOR_RED_TEXT,
            activebackground="#fee2e2",
            activeforeground=COLOR_RED_TEXT,
            relief="flat",
            cursor="hand2",
            pady=6,
            command=self._on_confirm_disable
        )
        btn_confirm_off.pack(fill=tk.X)
    def _render_action_buttons(self, status: str, rem_sec: int):
        btn_frame = tk.Frame(self.content_area, bg=COLOR_CARD_BG)
        btn_frame.pack(fill=tk.X, side=tk.BOTTOM)

        if status == SafeguardState.UNLOCKED:
            btn_main = tk.Button(
                btn_frame,
                text="Activate Shield Now",
                font=("Segoe UI", 10, "bold"),
                bg=COLOR_TEAL_PRIMARY,
                fg="#ffffff",
                activebackground=COLOR_TEAL_HOVER,
                activeforeground="#ffffff",
                relief="flat",
                cursor="hand2",
                pady=9,
                command=self._on_activate_shield
            )
            btn_main.pack(fill=tk.X, pady=(0, 8))

        elif status == SafeguardState.LOCKED:
            btn_main = tk.Button(
                btn_frame,
                text=f"Request Unblock ({self.safeguard.format_remaining(self.safeguard.get_cooldown_seconds())} Delay)",
                font=("Segoe UI", 12, "bold"),
                bg=COLOR_SLATE_BTN,
                fg="#ffffff",
                activebackground=COLOR_SLATE_BTN_HOVER,
                activeforeground="#ffffff",
                relief="flat",
                cursor="hand2",
                pady=12,
                command=self._show_request_unblock_modal
            )
            btn_main.pack(fill=tk.X, pady=(0, 8))

        elif status == SafeguardState.COOLDOWN:
            btn_disabled = tk.Button(
                btn_frame,
                text=f"Locked in Cool-down ({self.safeguard.format_remaining(rem_sec)})",
                font=("Segoe UI", 9, "bold"),
                bg="#cbd5e1",
                fg="#64748b",
                relief="flat",
                state=tk.DISABLED,
                pady=9
            )
            btn_disabled.pack(fill=tk.X, pady=(0, 8))

        sub_bar = tk.Frame(btn_frame, bg=COLOR_CARD_BG)
        sub_bar.pack(fill=tk.X)

        btn_upd = tk.Button(
            sub_bar,
            text=" Update from Web",
            image=self.icon_cache.get("action_globe"),
            compound=tk.LEFT,
            font=("Segoe UI", 8),
            bg="#f8fafc",
            fg=COLOR_TEXT_MAIN,
            bd=1,
            relief="solid",
            highlightbackground="#e2e8f0",
            cursor="hand2",
            padx=10,
            pady=5,
            command=self._on_update_web_list
        )
        btn_upd.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=(0, 4))

        btn_cust = tk.Button(
            sub_bar,
            text=" Block Custom Domain",
            image=self.icon_cache.get("action_plus"),
            compound=tk.LEFT,
            font=("Segoe UI", 8),
            bg="#f8fafc",
            fg=COLOR_TEXT_MAIN,
            bd=1,
            relief="solid",
            highlightbackground="#e2e8f0",
            cursor="hand2",
            padx=10,
            pady=5,
            command=self._show_custom_domain_modal
        )
        btn_cust.pack(side=tk.RIGHT, fill=tk.X, expand=True, padx=(4, 0))

    def _render_rules_tab(self):
        f = tk.Frame(self.content_area, bg=COLOR_CARD_BG)
        f.pack(fill=tk.BOTH, expand=True)

        tk.Label(f, text="Protection Rules", font=("Segoe UI", 16, "bold"), fg=COLOR_TEXT_MAIN, bg=COLOR_CARD_BG).pack(anchor="w")
        tk.Label(f, text="Detailed overview of active filters and guarantees.", font=("Segoe UI", 9), fg=COLOR_TEXT_MUTED, bg=COLOR_CARD_BG).pack(anchor="w", pady=(0, 14))

        rules = [
            ("Dedicated Adult Sites", "Uses the bundled adult and cam list, optional community list updates, and your own custom domains."),
            ("Escort & Classifieds Portals", "Blocks commercial escort platforms, erotic massage locators, and adult hookup directories (e.g. Skipthegames, Megapersonals, Tryst, Eros)."),
            ("SafeSearch & encrypted DNS", "Forces SafeSearch on Google, Bing and DuckDuckGo. While locked, Windows Firewall blocks common direct DNS, DNS-over-TLS and DNS-over-HTTPS resolver endpoints."),
            ("Whitelist Guarantee", "Maintains zero false positives for entertainment and work: Netflix, Amazon Prime, Disney+, Reddit, X/Twitter, Instagram, YouTube, and Twitch remain completely open."),
            ("5-Hour Safeguard", "Prevents impulsive bypasses by enforcing a mandatory 5-hour cool-down delay before protection can ever be disabled.")
        ]
        for title, desc in rules:
            rf = tk.Frame(f, bg="#f8fafc", bd=1, relief="solid", highlightbackground="#e2e8f0", padx=10, pady=8)
            rf.pack(fill=tk.X, pady=4)
            tk.Label(rf, text=title, font=("Segoe UI", 9, "bold"), fg=COLOR_TEXT_MAIN, bg="#f8fafc").pack(anchor="w")
            tk.Label(rf, text=desc, font=("Segoe UI", 8), fg=COLOR_TEXT_MUTED, bg="#f8fafc", wraplength=420, justify="left").pack(anchor="w")

    def _render_settings_tab(self):
        f = tk.Frame(self.content_area, bg=COLOR_CARD_BG)
        f.pack(fill=tk.BOTH, expand=True)

        tk.Label(f, text="Settings & safeguards", font=("Segoe UI", 22, "bold"), fg=COLOR_TEXT_MAIN, bg=COLOR_CARD_BG).pack(anchor="w")
        tk.Label(f, text="Set the reflection delay that applies when you request to turn protection off.", font=("Segoe UI", 11), fg=COLOR_TEXT_MUTED, bg=COLOR_CARD_BG).pack(anchor="w", pady=(0, 20))

        duration_card = tk.Frame(f, bg="#f8fafc", bd=1, relief="solid", highlightbackground="#dbe4ea", padx=20, pady=18)
        duration_card.pack(fill=tk.X, pady=(0, 12))
        tk.Label(duration_card, text="Cool-down duration", font=("Segoe UI", 13, "bold"), fg=COLOR_TEXT_MAIN, bg="#f8fafc").pack(anchor="w")
        tk.Label(duration_card, text="Choose from 30 seconds to 24 hours. A running countdown cannot be changed.", font=("Segoe UI", 10), fg=COLOR_TEXT_MUTED, bg="#f8fafc").pack(anchor="w", pady=(3, 12))

        current_seconds = self.safeguard.get_cooldown_seconds()
        current_label = next((label for label, seconds in COOLDOWN_PRESETS.items() if seconds == current_seconds), "5 hours")
        selected_duration = tk.StringVar(value=current_label)
        status = self.safeguard.get_status()["status"]
        is_active = status == SafeguardState.COOLDOWN

        controls = tk.Frame(duration_card, bg="#f8fafc")
        controls.pack(fill=tk.X)
        selector = ttk.Combobox(controls, textvariable=selected_duration, values=list(COOLDOWN_PRESETS), state="disabled" if is_active else "readonly", font=("Segoe UI", 10), width=18)
        selector.pack(side=tk.LEFT)

        def save_duration():
            success, message = self.safeguard.set_cooldown_seconds(COOLDOWN_PRESETS[selected_duration.get()])
            self._show_toast("Cool-down updated" if success else "Could not update", message, is_error=not success)
            if success:
                self._switch_nav("settings")

        save_button = tk.Button(controls, text="Save duration", font=("Segoe UI", 10, "bold"), bg=COLOR_TEAL_PRIMARY, fg="#ffffff", activebackground=COLOR_TEAL_HOVER, activeforeground="#ffffff", relief="flat", cursor="hand2", padx=16, pady=7, command=save_duration, state=tk.DISABLED if is_active else tk.NORMAL)
        save_button.pack(side=tk.LEFT, padx=(10, 0))
        if is_active:
            tk.Label(duration_card, text="Locked until the current cool-down finishes or you choose Keep Me Safe.", font=("Segoe UI", 10, "bold"), fg=COLOR_AMBER_TEXT, bg="#f8fafc").pack(anchor="w", pady=(12, 0))

        settings = [
            ("Anti-tamper verification", "Active — system clock rollback is detected."),
            ("DNS resolver refresh", "Automatic whenever protection rules change."),
            ("SafeSearch redirection", "Google, Bing and DuckDuckGo are protected while the shield is active."),
        ]
        for title, detail in settings:
            rf = tk.Frame(f, bg="#ffffff", bd=1, relief="solid", highlightbackground="#e2e8f0", padx=16, pady=12)
            rf.pack(fill=tk.X, pady=5)
            tk.Label(rf, text=title, font=("Segoe UI", 11, "bold"), fg=COLOR_TEXT_MAIN, bg="#ffffff").pack(anchor="w")
            tk.Label(rf, text=detail, font=("Segoe UI", 10), fg=COLOR_TEXT_MUTED, bg="#ffffff", wraplength=720, justify="left").pack(anchor="w", pady=(3, 0))

    def _render_about_tab(self):
        f = tk.Frame(self.content_area, bg=COLOR_CARD_BG)
        f.pack(fill=tk.BOTH, expand=True)

        tk.Label(f, text="About Badilika", font=("Segoe UI", 16, "bold"), fg=COLOR_TEXT_MAIN, bg=COLOR_CARD_BG).pack(anchor="w")
        tk.Label(f, text="Version 1.0.0 • Lightweight Windows Content Shield", font=("Segoe UI", 9), fg=COLOR_TEXT_MUTED, bg=COLOR_CARD_BG).pack(anchor="w", pady=(0, 14))

        box = tk.Frame(f, bg="#f8fafc", bd=1, relief="solid", highlightbackground="#e2e8f0", padx=14, pady=14)
        box.pack(fill=tk.X, pady=4)

        tagline = tk.Label(box, text="LESS DISTRACTIONS. A BRIGHTER TOMORROW.", font=("Segoe UI", 10, "bold"), fg=COLOR_TEAL_PRIMARY, bg="#f8fafc")
        tagline.pack(anchor="w", pady=(0, 6))

        p1 = tk.Label(
            box,
            text="Badilika was created to empower you to reclaim focus, build healthier digital habits, and eliminate the pull of compulsive pornography and escort browsing.\n\nEverything runs 100% locally on your machine with zero remote tracking and zero overhead.",
            font=("Segoe UI", 8),
            fg=COLOR_TEXT_MUTED,
            bg="#f8fafc",
            wraplength=420,
            justify="left"
        )
        p1.pack(anchor="w")
    def _start_timer_loop(self):
        if self.current_nav == "home" and self.winfo_exists():
            status_info = self.safeguard.get_status()
            signature = (status_info["status"], status_info["unlock_eligible_at"])
            # Rebuild only when the actual application state changes. Recreating
            # the full dashboard once per second caused visible blank/frozen UI
            # intervals on slower packaged builds.
            if signature != self._home_state_signature:
                for child in self.content_area.winfo_children():
                    child.destroy()
                self._render_home_tab()
            elif self._countdown_label is not None and self._countdown_label.winfo_exists():
                self._countdown_label.config(text=self.safeguard.format_remaining(status_info["remaining_seconds"]))
        self.after(1000, self._start_timer_loop)

    def _on_activate_shield(self):
        success, msg = self.blocker.enable_blocking()
        if success:
            self.safeguard.lock()
            self._show_toast("Shield Activated", "Adult content and escort sites are now completely blocked.")
            self._switch_nav("home")
        else:
            self._show_toast("Error", msg, is_error=True)

    def _on_cancel_unblock(self):
        self.safeguard.cancel_unblock_request()
        self._show_toast("Shield Re-Locked", "Great decision! The unblock request was canceled. Your shield is secure.")
        self._switch_nav("home")

    def _on_confirm_disable(self):
        success, msg = self.blocker.disable_blocking()
        if not success:
            self._show_toast("Error", msg, is_error=True)
            return
        success, msg = self.safeguard.confirm_unblock()
        if not success:
            # Do not claim protection is disabled if the safeguard rejects it.
            self.blocker.enable_blocking()
            self._show_toast("Error", msg, is_error=True)
            return
        self._show_toast("Shield Inactive", "Protection has been disabled.")
        self._switch_nav("home")

    def _on_update_web_list(self):
        def _worker():
            success, msg = self.blocker.update_community_blocklist()
            self.after(0, lambda: self._show_toast("Blocklist Updated", msg if success else "Failed to update blocklist.", is_error=not success))
            self.after(0, lambda: self._switch_nav("home"))

        threading.Thread(target=_worker, daemon=True).start()

    def _show_request_unblock_modal(self):
        modal = tk.Toplevel(self)
        modal.title("Request Unblock")
        modal.geometry("520x280")
        modal.resizable(False, False)
        modal.configure(bg="#ffffff")
        modal.transient(self)
        modal.grab_set()

        x = self.winfo_x() + (self.winfo_width() // 2) - 260
        y = self.winfo_y() + (self.winfo_height() // 2) - 140
        modal.geometry(f"+{x}+{y}")

        content = tk.Frame(modal, bg="#ffffff", padx=20, pady=18)
        content.pack(fill=tk.BOTH, expand=True)

        top_f = tk.Frame(content, bg="#ffffff")
        top_f.pack(fill=tk.X, pady=(0, 10))

        info_icon = tk.Label(top_f, text="i", font=("Segoe UI", 11, "bold"), fg="#ffffff", bg=COLOR_ACCENT_BLUE, width=3, height=1)
        info_icon.pack(side=tk.LEFT, padx=(0, 10))

        title_lbl = tk.Label(top_f, text="This will not immediately disable protection.", font=("Segoe UI", 10, "bold"), fg=COLOR_TEXT_MAIN, bg="#ffffff", wraplength=340, justify="left")
        title_lbl.pack(side=tk.LEFT)

        body_lbl = tk.Label(
            content,
            text=f"Instead, a {self.safeguard.format_remaining(self.safeguard.get_cooldown_seconds())} cool-down will start. During this time, your shield remains fully active. This delay helps you make a more thoughtful decision.",
            font=("Segoe UI", 10),
            fg=COLOR_TEXT_MUTED,
            bg="#ffffff",
            wraplength=460,
            justify="left"
        )
        body_lbl.pack(anchor="w", pady=(0, 20))

        btn_row = tk.Frame(content, bg="#ffffff")
        btn_row.pack(side=tk.BOTTOM, fill=tk.X)

        def _start_delay():
            self.safeguard.request_unblock()
            modal.destroy()
            self._switch_nav("home")

        btn_start = tk.Button(
            btn_row,
            text=f"Start {self.safeguard.format_remaining(self.safeguard.get_cooldown_seconds())} Delay",
            font=("Segoe UI", 10, "bold"),
            bg=COLOR_ACCENT_BLUE,
            fg="#ffffff",
            activebackground=COLOR_ACCENT_BLUE_HOVER,
            activeforeground="#ffffff",
            relief="flat",
            cursor="hand2",
            padx=16,
            pady=6,
            command=_start_delay
        )
        btn_start.pack(side=tk.RIGHT, padx=(8, 0))

        btn_cancel = tk.Button(
            btn_row,
            text="Cancel",
            font=("Segoe UI", 9),
            bg="#f1f5f9",
            fg=COLOR_TEXT_MAIN,
            activebackground="#e2e8f0",
            relief="flat",
            cursor="hand2",
            padx=14,
            pady=6,
            command=modal.destroy
        )
        btn_cancel.pack(side=tk.RIGHT)

    def _show_custom_domain_modal(self):
        modal = tk.Toplevel(self)
        modal.title("Block Custom Domain")
        modal.geometry("440x240")
        modal.resizable(False, False)
        modal.configure(bg="#ffffff")
        modal.transient(self)
        modal.grab_set()

        x = self.winfo_x() + (self.winfo_width() // 2) - 220
        y = self.winfo_y() + (self.winfo_height() // 2) - 120
        modal.geometry(f"+{x}+{y}")

        content = tk.Frame(modal, bg="#ffffff", padx=20, pady=18)
        content.pack(fill=tk.BOTH, expand=True)

        top_f = tk.Frame(content, bg="#ffffff")
        top_f.pack(fill=tk.X, pady=(0, 10))

        lbl_title = tk.Label(top_f, text="Enter the domain to block:", font=("Segoe UI", 10, "bold"), fg=COLOR_TEXT_MAIN, bg="#ffffff")
        lbl_title.pack(side=tk.LEFT)

        entry_domain = tk.Entry(content, font=("Segoe UI", 10), bd=1, relief="solid", highlightbackground="#cbd5e1", bg="#f8fafc")
        entry_domain.pack(fill=tk.X, ipady=6, pady=(0, 4))
        entry_domain.focus_set()

        lbl_ex = tk.Label(content, text="Example: specificsite.com", font=("Segoe UI", 8), fg=COLOR_TEXT_LIGHT, bg="#ffffff")
        lbl_ex.pack(anchor="w", pady=(0, 20))

        btn_row = tk.Frame(content, bg="#ffffff")
        btn_row.pack(side=tk.BOTTOM, fill=tk.X)

        def _do_add():
            domain = entry_domain.get().strip().lower()
            if domain.startswith("http://") or domain.startswith("https://"):
                domain = domain.split("//")[1].split("/")[0]
            success, message = self.blocker.add_custom_domain(domain)
            if not success:
                self._show_toast("Could Not Add Domain", message, is_error=True)
                return

            if self.safeguard.get_status()["status"] in (SafeguardState.LOCKED, SafeguardState.COOLDOWN):
                self.blocker.enable_blocking()

            modal.destroy()
            self._show_toast("Domain Added", message)
            self._switch_nav("home")

        btn_add = tk.Button(
            btn_row,
            text="Block Domain",
            font=("Segoe UI", 9, "bold"),
            bg=COLOR_ACCENT_BLUE,
            fg="#ffffff",
            activebackground=COLOR_ACCENT_BLUE_HOVER,
            activeforeground="#ffffff",
            relief="flat",
            cursor="hand2",
            padx=16,
            pady=6,
            command=_do_add
        )
        btn_add.pack(side=tk.RIGHT, padx=(8, 0))

        btn_cancel = tk.Button(
            btn_row,
            text="Cancel",
            font=("Segoe UI", 9),
            bg="#f1f5f9",
            fg=COLOR_TEXT_MAIN,
            activebackground="#e2e8f0",
            relief="flat",
            cursor="hand2",
            padx=14,
            pady=6,
            command=modal.destroy
        )
        btn_cancel.pack(side=tk.RIGHT)

    def _show_toast(self, title: str, message: str, is_error: bool = False):
        toast = tk.Toplevel(self)
        toast.overrideredirect(True)
        toast.attributes("-topmost", True)
        toast.configure(bg="#ffffff")

        px = self.winfo_x() + self.winfo_width() - 360
        py = self.winfo_y() + 40
        toast.geometry(f"340x68+{px}+{py}")

        border_col = COLOR_RED_BORDER if is_error else COLOR_GREEN_BORDER
        f = tk.Frame(toast, bg="#ffffff", bd=1, relief="solid", highlightbackground=border_col, padx=12, pady=10)
        f.pack(fill=tk.BOTH, expand=True)

        ic_text = "!" if is_error else "+"
        ic_col = COLOR_RED_TEXT if is_error else COLOR_GREEN_TEXT
        ic_bg = COLOR_RED_BG if is_error else COLOR_GREEN_BG

        ic = tk.Label(f, text=ic_text, font=("Segoe UI", 9, "bold"), fg=ic_col, bg=ic_bg, width=2, height=1)
        ic.pack(side=tk.LEFT, padx=(0, 10))

        tf = tk.Frame(f, bg="#ffffff")
        tf.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)

        t_lbl = tk.Label(tf, text=title, font=("Segoe UI", 9, "bold"), fg=COLOR_TEXT_MAIN, bg="#ffffff")
        t_lbl.pack(anchor="w")
        m_lbl = tk.Label(tf, text=message, font=("Segoe UI", 8), fg=COLOR_TEXT_MUTED, bg="#ffffff", wraplength=250, justify="left")
        m_lbl.pack(anchor="w")

        self.after(4000, lambda: toast.destroy() if toast.winfo_exists() else None)
