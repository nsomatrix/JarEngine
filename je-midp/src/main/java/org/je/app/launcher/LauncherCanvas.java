package org.je.app.launcher;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;

public class LauncherCanvas extends Canvas {
    private Image logo;
    private String message;
    private MIDlet midlet;
    private String theme;

    public LauncherCanvas(MIDlet midlet, String message, String theme) {
        this.midlet = midlet;
        this.message = message;
        this.theme = theme;
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
        int bgColor = 0xFFFFFF;
        int textColor = 0x000000;
        if ("dark".equals(theme)) {
            bgColor = 0x222222;
            textColor = 0xFFFFFF;
        }
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
        g.setColor(0x666666); // Subtle gray for secondary text
        if ("dark".equals(theme)) {
            g.setColor(0xBBBBBB); // Light gray for dark mode
        }
        g.setFont(Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        String instruction1 = "Drop a .jar file here";
        String instruction2 = "or use File → Open";
        
        int instructionY = h * 3 / 4;
        g.drawString(instruction1, w / 2, instructionY, Graphics.TOP | Graphics.HCENTER);
        g.drawString(instruction2, w / 2, instructionY + 20, Graphics.TOP | Graphics.HCENTER);
    }
} 