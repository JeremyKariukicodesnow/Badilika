"""
Generate crisp, anti-aliased UI icons matching the NewLife design mockup.
"""

import os
from PIL import Image, ImageDraw

def create_icons(output_dir: str):
    os.makedirs(output_dir, exist_ok=True)
    scale = 4  # Super-sampling for razor-sharp edges

    def get_draw(size):
        img = Image.new("RGBA", (size * scale, size * scale), (0, 0, 0, 0))
        return img, ImageDraw.Draw(img), size * scale

    def save_icon(img, name, target_size):
        res = img.resize((target_size, target_size), Image.Resampling.LANCZOS)
        res.save(os.path.join(output_dir, f"{name}.png"), "PNG")

    # 1. Leaf Logo (Teal)
    img, draw, S = get_draw(32)
    c_teal = (49, 120, 115, 255)
    draw.polygon([(S*0.8, S*0.15), (S*0.25, S*0.2), (S*0.15, S*0.75), (S*0.45, S*0.9), (S*0.85, S*0.5)], fill=c_teal)
    draw.line([(S*0.15, S*0.85), (S*0.7, S*0.3)], fill=(255, 255, 255, 230), width=int(2*scale))
    save_icon(img, "logo_leaf", 32)

    # 2. Home Icon
    img, draw, S = get_draw(20)
    col = (75, 85, 99, 255)
    draw.polygon([(S*0.5, S*0.2), (S*0.15, S*0.5), (S*0.85, S*0.5)], fill=None, outline=col, width=int(1.8*scale))
    draw.rectangle([S*0.25, S*0.48, S*0.75, S*0.85], outline=col, width=int(1.8*scale))
    draw.rectangle([S*0.4, S*0.6, S*0.6, S*0.85], outline=col, width=int(1.5*scale))
    save_icon(img, "nav_home", 20)

    # Home Active (Teal)
    img, draw, S = get_draw(20)
    col_active = (49, 120, 115, 255)
    draw.polygon([(S*0.5, S*0.2), (S*0.15, S*0.5), (S*0.85, S*0.5)], fill=None, outline=col_active, width=int(2*scale))
    draw.rectangle([S*0.25, S*0.48, S*0.75, S*0.85], outline=col_active, width=int(2*scale))
    draw.rectangle([S*0.4, S*0.6, S*0.6, S*0.85], outline=col_active, width=int(1.8*scale))
    save_icon(img, "nav_home_active", 20)

    # 3. Rules / Document Icon
    img, draw, S = get_draw(20)
    col = (148, 163, 184, 255)
    draw.rounded_rectangle([S*0.2, S*0.15, S*0.8, S*0.85], radius=int(2*scale), outline=col, width=int(1.8*scale))
    draw.line([(S*0.35, S*0.35), (S*0.65, S*0.35)], fill=col, width=int(1.5*scale))
    draw.line([(S*0.35, S*0.5), (S*0.65, S*0.5)], fill=col, width=int(1.5*scale))
    draw.line([(S*0.35, S*0.65), (S*0.55, S*0.65)], fill=col, width=int(1.5*scale))
    save_icon(img, "nav_rules", 20)

    # 4. Settings Gear Icon
    img, draw, S = get_draw(20)
    draw.ellipse([S*0.3, S*0.3, S*0.7, S*0.7], outline=col, width=int(1.8*scale))
    draw.ellipse([S*0.42, S*0.42, S*0.58, S*0.58], outline=col, width=int(1.5*scale))
    for angle in [0, 45, 90, 135]:
        draw.line([(S*0.18, S*0.5), (S*0.82, S*0.5)], fill=col, width=int(1.6*scale))
    draw.ellipse([S*0.32, S*0.32, S*0.68, S*0.68], fill=(255, 255, 255, 255), outline=col, width=int(1.8*scale))
    draw.ellipse([S*0.42, S*0.42, S*0.58, S*0.58], outline=col, width=int(1.5*scale))
    save_icon(img, "nav_settings", 20)

    # 5. About Info Icon
    img, draw, S = get_draw(20)
    draw.ellipse([S*0.15, S*0.15, S*0.85, S*0.85], outline=col, width=int(1.8*scale))
    draw.ellipse([S*0.47, S*0.32, S*0.53, S*0.38], fill=col)
    draw.line([(S*0.5, S*0.45), (S*0.5, S*0.68)], fill=col, width=int(1.8*scale))
    save_icon(img, "nav_about", 20)

    # 6. Shield Inactive: Padlock Open (Red)
    img, draw, S = get_draw(52)
    c_red = (220, 38, 38, 255)
    # Shackle open
    draw.arc([S*0.35, S*0.15, S*0.65, S*0.5], start=180, end=360, fill=c_red, width=int(3.2*scale))
    draw.line([(S*0.35, S*0.32), (S*0.35, S*0.52)], fill=c_red, width=int(3.2*scale))
    # Body
    draw.rounded_rectangle([S*0.25, S*0.48, S*0.75, S*0.85], radius=int(6*scale), fill=(254, 226, 226, 255), outline=c_red, width=int(3*scale))
    draw.ellipse([S*0.46, S*0.6, S*0.54, S*0.68], fill=c_red)
    draw.line([(S*0.5, S*0.68), (S*0.5, S*0.76)], fill=c_red, width=int(2.5*scale))
    save_icon(img, "badge_unlocked", 52)

    # 7. Shield Active: Shield Check (Green)
    img, draw, S = get_draw(52)
    c_green = (22, 101, 52, 255)
    # Shield shape
    pts = [(S*0.5, S*0.15), (S*0.8, S*0.25), (S*0.8, S*0.55), (S*0.5, S*0.85), (S*0.2, S*0.55), (S*0.2, S*0.25)]
    draw.polygon(pts, fill=(220, 252, 231, 255), outline=c_green)
    draw.line([(S*0.5, S*0.15), (S*0.8, S*0.25), (S*0.8, S*0.55), (S*0.5, S*0.85), (S*0.2, S*0.55), (S*0.2, S*0.25), (S*0.5, S*0.15)], fill=c_green, width=int(3*scale))
    # Checkmark inside
    draw.line([(S*0.36, S*0.5), (S*0.46, S*0.62), (S*0.64, S*0.38)], fill=c_green, width=int(3.5*scale))
    save_icon(img, "badge_shield_active", 52)

    # 8. Cooldown: Hourglass (Amber)
    img, draw, S = get_draw(52)
    c_amber = (180, 83, 9, 255)
    draw.rounded_rectangle([S*0.22, S*0.15, S*0.78, S*0.25], radius=int(3*scale), fill=c_amber)
    draw.rounded_rectangle([S*0.22, S*0.75, S*0.78, S*0.85], radius=int(3*scale), fill=c_amber)
    draw.polygon([(S*0.3, S*0.25), (S*0.7, S*0.25), (S*0.55, S*0.5), (S*0.7, S*0.75), (S*0.3, S*0.75), (S*0.45, S*0.5)], fill=(254, 243, 199, 255), outline=c_amber)
    draw.line([(S*0.3, S*0.25), (S*0.7, S*0.25), (S*0.55, S*0.5), (S*0.7, S*0.75), (S*0.3, S*0.75), (S*0.45, S*0.5), (S*0.3, S*0.25)], fill=c_amber, width=int(3*scale))
    # Sand
    draw.polygon([(S*0.38, S*0.68), (S*0.62, S*0.68), (S*0.5, S*0.58)], fill=c_amber)
    save_icon(img, "badge_hourglass", 52)

    # 9. Cooldown Finished: Alert Triangle (Orange/Brown)
    img, draw, S = get_draw(52)
    c_warn = (194, 65, 12, 255)
    draw.polygon([(S*0.5, S*0.15), (S*0.85, S*0.8), (S*0.15, S*0.8)], fill=(255, 237, 213, 255), outline=c_warn)
    draw.line([(S*0.5, S*0.15), (S*0.85, S*0.8), (S*0.15, S*0.8), (S*0.5, S*0.15)], fill=c_warn, width=int(3*scale))
    draw.line([(S*0.5, S*0.38), (S*0.5, S*0.58)], fill=c_warn, width=int(3.2*scale))
    draw.ellipse([S*0.46, S*0.66, S*0.54, S*0.74], fill=c_warn)
    save_icon(img, "badge_alert", 52)

    # 10. Diagnostics Row Icons: Globe (Domains)
    img, draw, S = get_draw(18)
    col_d = (100, 116, 139, 255)
    draw.ellipse([S*0.15, S*0.15, S*0.85, S*0.85], outline=col_d, width=int(1.5*scale))
    draw.ellipse([S*0.32, S*0.15, S*0.68, S*0.85], outline=col_d, width=int(1.3*scale))
    draw.line([(S*0.15, S*0.5), (S*0.85, S*0.5)], fill=col_d, width=int(1.3*scale))
    save_icon(img, "diag_globe", 18)

    # 11. Search Glass (SafeSearch)
    img, draw, S = get_draw(18)
    draw.ellipse([S*0.15, S*0.15, S*0.65, S*0.65], outline=col_d, width=int(1.6*scale))
    draw.line([(S*0.53, S*0.53), (S*0.85, S*0.85)], fill=col_d, width=int(2*scale))
    save_icon(img, "diag_search", 18)

    # 12. Escorts List / Bars
    img, draw, S = get_draw(18)
    draw.line([(S*0.15, S*0.25), (S*0.3, S*0.25)], fill=col_d, width=int(1.8*scale))
    draw.line([(S*0.4, S*0.25), (S*0.85, S*0.25)], fill=col_d, width=int(1.5*scale))
    draw.line([(S*0.15, S*0.5), (S*0.3, S*0.5)], fill=col_d, width=int(1.8*scale))
    draw.line([(S*0.4, S*0.5), (S*0.85, S*0.5)], fill=col_d, width=int(1.5*scale))
    draw.line([(S*0.15, S*0.75), (S*0.3, S*0.75)], fill=col_d, width=int(1.8*scale))
    draw.line([(S*0.4, S*0.75), (S*0.85, S*0.75)], fill=col_d, width=int(1.5*scale))
    save_icon(img, "diag_list", 18)

    # 13. Whitelist Check Shield
    img, draw, S = get_draw(18)
    draw.polygon([(S*0.5, S*0.15), (S*0.85, S*0.25), (S*0.85, S*0.55), (S*0.5, S*0.85), (S*0.15, S*0.55), (S*0.15, S*0.25)], outline=col_d, width=int(1.5*scale))
    draw.line([(S*0.35, S*0.48), (S*0.45, S*0.6), (S*0.65, S*0.38)], fill=col_d, width=int(1.6*scale))
    save_icon(img, "diag_whitelist", 18)

    # 14. Plus Icon
    img, draw, S = get_draw(16)
    draw.line([(S*0.2, S*0.5), (S*0.8, S*0.5)], fill=col_d, width=int(2*scale))
    draw.line([(S*0.5, S*0.2), (S*0.5, S*0.8)], fill=col_d, width=int(2*scale))
    save_icon(img, "action_plus", 16)

    # 15. Globe Small for Update button
    img, draw, S = get_draw(16)
    draw.ellipse([S*0.15, S*0.15, S*0.85, S*0.85], outline=col_d, width=int(1.4*scale))
    draw.ellipse([S*0.33, S*0.15, S*0.67, S*0.85], outline=col_d, width=int(1.2*scale))
    draw.line([(S*0.15, S*0.5), (S*0.85, S*0.5)], fill=col_d, width=int(1.2*scale))
    save_icon(img, "action_globe", 16)

    print("Successfully generated all clean UI icons!")

if __name__ == "__main__":
    create_icons("desktop/assets/icons")
