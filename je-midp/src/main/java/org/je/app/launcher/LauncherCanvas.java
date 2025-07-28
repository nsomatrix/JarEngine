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

        // Draw message centered below logo
        g.setColor(textColor); // themed text
        g.setFont(Font.getDefaultFont());
        int msgY = h / 2;
        g.drawString(message, w / 2, msgY, Graphics.TOP | Graphics.HCENTER);
    }
} 