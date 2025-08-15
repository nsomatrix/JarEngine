package org.je.app.launcher;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;

import org.je.app.CommonInterface;

public class LauncherCanvas extends Canvas {
    private Image logo;
    private String message;
    private MIDlet midlet;
    private CommonInterface common;

    public LauncherCanvas(MIDlet midlet, String message, CommonInterface common) {
        this.midlet = midlet;
        this.message = message;
        this.common = common;
        try {
            // Adjust the path if your icon is elsewhere
            logo = Image.createImage("/org/je/icon.png");
        } catch (Exception e) {
            logo = null;
        }
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        String theme = null;
        try { theme = (common != null) ? common.getCurrentTheme() : null; } catch (Throwable ignored) {}
        boolean dark = "dark".equals(theme);
    // Prefer bridged colors from Common (Swing UIManager-derived)
    int bgColor = 0;
    int textColor = 0;
    int secondary = 0;
        boolean haveBg = false, haveFg = false, haveSec = false;
        if (common != null) {
            try {
                int bg = common.getThemeBgColor();
                int fg = common.getThemeFgColor();
                int sec = common.getThemeSecondaryColor();
                // Treat negative values as "unset"; otherwise mask to 24-bit
                if (bg >= 0) { bgColor = bg & 0xFFFFFF; haveBg = true; }
                if (fg >= 0) { textColor = fg & 0xFFFFFF; haveFg = true; }
                if (sec >= 0) { secondary = sec & 0xFFFFFF; haveSec = true; }
            } catch (Throwable ignored) {}
        }
        // Fallbacks only if any of the bridged colors are actually unset
        if (!haveBg) bgColor = dark ? 0x202225 : 0xFFFFFF;
        if (!haveFg) textColor = dark ? 0xEAEAEA : 0x111111;
        if (!haveSec) secondary = dark ? 0xA0A0A0 : 0x666666;
        g.setColor(bgColor); // themed background
        g.fillRect(0, 0, w, h);

        // Draw logo centered
        if (logo != null) {
            int imgX = (w - logo.getWidth()) / 2;
            int imgY = h / 4 - logo.getHeight() / 2;
            g.drawImage(logo, imgX, imgY, Graphics.TOP | Graphics.LEFT);
        }

        // Draw main title
    g.setColor(textColor);
        g.setFont(Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_LARGE));
        String title = "JarEngine";
        int titleX = w / 2;
        int titleY = h / 2;
        g.drawString(title, titleX, titleY, Graphics.TOP | Graphics.HCENTER);

        // Draw helpful instructions
    g.setColor(secondary); // Subtle secondary text
        g.setFont(Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        String instruction1 = "Drop a .jar file here";
        String instruction2 = "or use File → Open";
        
        int instructionY = h * 3 / 4;
        g.drawString(instruction1, w / 2, instructionY, Graphics.TOP | Graphics.HCENTER);
        g.drawString(instruction2, w / 2, instructionY + 20, Graphics.TOP | Graphics.HCENTER);
    }
} 