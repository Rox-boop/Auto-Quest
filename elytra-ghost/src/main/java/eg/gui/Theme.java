package eg.gui;

import java.awt.Color;
import java.awt.Font;

final class Theme {
    private Theme() {}
    static final Color BG = new Color(0x0E0F14);
    static final Color PANEL = new Color(0x161821);
    static final Color CARD = new Color(0x1C1F2B);
    static final Color CARD_HOVER = new Color(0x23273A);
    static final Color TEXT = new Color(0xE8EAF2);
    static final Color MUTED = new Color(0x8A90A6);
    static final Color ACCENT_A = new Color(0x7C5CFF);
    static final Color ACCENT_B = new Color(0x2EE6D6);
    static final Color OFF = new Color(0x3A3F55);
    static final Color DANGER = new Color(0xFF5470);
    static final Font BASE = new Font("Segoe UI", Font.PLAIN, 13);
    static final Font BOLD = BASE.deriveFont(Font.BOLD, 14f);
    static final Font TITLE = BASE.deriveFont(Font.BOLD, 18f);
    static final Font SMALL = BASE.deriveFont(11.5f);
}
