package org.je.app;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenshotUtil {
    /**
     * Captures a screenshot of the given component and saves it to the user's Pictures folder.
     * Returns the file path, or null on failure.
     */
    public static String captureAndSaveScreenshot(Component component, String prefix) {
        try {
            Rectangle bounds = component.getBounds();
            BufferedImage image = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
            component.paint(image.getGraphics());

            File picturesFolder = getPicturesFolder();
            if (!picturesFolder.exists()) {
                picturesFolder.mkdirs();
            }
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            String filename = prefix + timestamp + ".png";
            File screenshotFile = new File(picturesFolder, filename);
            int counter = 1;
            while (screenshotFile.exists()) {
                filename = prefix + timestamp + "_" + counter + ".png";
                screenshotFile = new File(picturesFolder, filename);
                counter++;
            }
            ImageIO.write(image, "png", screenshotFile);
            return screenshotFile.getAbsolutePath();
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the cross-platform Pictures folder location.
     */
    public static File getPicturesFolder() {
        String userHome = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new File(userHome, "Pictures");
        } else if (os.contains("mac")) {
            return new File(userHome, "Pictures");
        } else {
            // Linux and others: XDG_PICTURES_DIR or fallback
            String xdg = System.getenv("XDG_PICTURES_DIR");
            if (xdg != null && !xdg.isEmpty()) {
                return new File(xdg.replace("$HOME", userHome));
            }
            return new File(userHome, "Pictures");
        }
    }
}
