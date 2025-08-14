package org.je.app.ui.swing;

import java.awt.Window;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

import org.je.app.Config;
import org.je.app.Common;
import org.je.device.DeviceFactory;
import org.je.log.Logger;
// no MIDlet bridging here; launcher restart should be handled by callers in the same package as Common

/**
 * Centralized theming utilities for JarEngine.
 * Handles Swing Look & Feel, persistence, and propagating theme changes
 * to the emulator device display and launcher.
 */
public final class Themes {

    private Themes() {}

    /**
     * Initialize the Swing Look & Feel at startup based on saved config.
     * Falls back to system LAF if FlatLaf init fails.
     */
    public static void initializeLookAndFeelFromConfig() {
        String theme = safeCurrentTheme();
        try {
            setLookAndFeelForTheme(theme);
        } catch (Exception ex) {
            Logger.error(ex);
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex2) {
                Logger.error(ex2);
            }
        }
    }

    /**
     * Apply the given theme, update provided windows, persist setting,
     * update device display theme colors, and refresh launcher if active.
     * @param theme "light" or "dark"
     * @param common instance used to update current theme and (optionally) restart launcher
     * @param windows top-level windows to re-style via updateComponentTreeUI
     */
    public static void applyTheme(String theme, Common common, Window... windows) {
        try {
            setLookAndFeelForTheme(theme);
            // Update any provided windows/components
            if (windows != null) {
                for (Window w : windows) {
                    if (w != null) {
                        SwingUtilities.updateComponentTreeUI(w);
                    }
                }
            }
            // Persist and publish to Common
            Config.setCurrentTheme(theme);
            if (common != null) {
                common.setCurrentTheme(theme);
            }
            // Update emulator device display theme colors if present
            try {
                if (DeviceFactory.getDevice() != null &&
                        DeviceFactory.getDevice().getDeviceDisplay() instanceof org.je.device.j2se.J2SEDeviceDisplay) {
                    ((org.je.device.j2se.J2SEDeviceDisplay) DeviceFactory.getDevice().getDeviceDisplay())
                            .updateThemeColors(theme);
                }
            } catch (Throwable t) {
                Logger.error("Failed to update device display theme colors", t);
            }
            // Launcher refresh is intentionally not handled here to avoid protected access to Common.
        } catch (Exception ex) {
            Logger.error("Failed to apply theme", ex);
        }
    }

    /**
     * Set FlatLaf Look & Feel for the theme with per-platform tuning.
     * @param theme "light" or "dark"
     */
    private static void setLookAndFeelForTheme(String theme) throws Exception {
        String osName = System.getProperty("os.name", "").toLowerCase();
        boolean isWindows = osName.contains("windows");
        boolean dark = "dark".equalsIgnoreCase(theme);

        if (dark) {
            UIManager.setLookAndFeel(new FlatMacDarkLaf());
        } else {
            UIManager.setLookAndFeel(new FlatMacLightLaf());
        }
        if (isWindows) {
            // Avoid menu bar integration/title pane issues on Windows
            UIManager.put("TitlePane.useWindowDecorations", Boolean.FALSE);
            UIManager.put("TitlePane.menuBarEmbedded", Boolean.FALSE);
        }
    }

    private static String safeCurrentTheme() {
        try {
            String t = Config.getCurrentTheme();
            return (t == null || t.trim().isEmpty()) ? "light" : t;
        } catch (Throwable ignored) {
            return "light";
        }
    }
}
