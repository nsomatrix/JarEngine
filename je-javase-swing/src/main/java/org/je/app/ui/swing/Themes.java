package org.je.app.ui.swing;

import java.awt.Window;

import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
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
            javax.swing.SwingUtilities.updateComponentTreeUI(w);
                    }
                }
            }
            // Persist and publish to Common
            Config.setCurrentTheme(theme);
            if (common != null) {
                // For MIDP/launcher and device UI, only light/dark matters
                common.setCurrentTheme(isDarkTheme(theme) ? "dark" : "light");
                // Publish UI-derived palette for launcher (no hardcoded color fallbacks)
                try {
                    java.awt.Color bg = UIManager.getColor("Panel.background");
                    if (bg == null) bg = UIManager.getColor("control");
                    java.awt.Color fg = UIManager.getColor("Label.foreground");
                    if (fg == null) fg = UIManager.getColor("textText");
                    java.awt.Color sec = UIManager.getColor("Label.disabledForeground");
                    if (sec == null) sec = UIManager.getColor("Component.infoForeground");
                    // If secondary still null, derive it by blending fg toward bg (less prominent)
                    if (sec == null && bg != null && fg != null) sec = blend(fg, bg, 0.5f);
                    if (bg != null && fg != null) {
                        int bgRGB = (bg.getRed() << 16) | (bg.getGreen() << 8) | bg.getBlue();
                        int fgRGB = (fg.getRed() << 16) | (fg.getGreen() << 8) | fg.getBlue();
                        int secRGB = (sec != null ? ((sec.getRed() << 16) | (sec.getGreen() << 8) | sec.getBlue()) : fgRGB);
                        common.setThemeColors(bgRGB, fgRGB, secRGB);
                    } else {
                        // Explicitly clear palette so Launcher uses safe fallbacks
                        common.setThemeColors(-1, -1, -1);
                    }
                } catch (Throwable ignore) {}
            }
            // Update emulator device display theme colors if present
            try {
                if (DeviceFactory.getDevice() != null &&
                        DeviceFactory.getDevice().getDeviceDisplay() instanceof org.je.device.j2se.J2SEDeviceDisplay) {
                    ((org.je.device.j2se.J2SEDeviceDisplay) DeviceFactory.getDevice().getDeviceDisplay())
                .updateThemeColors(isDarkTheme(theme) ? "dark" : "light");
                }
            } catch (Throwable t) {
                Logger.error("Failed to update device display theme colors", t);
            }
            // Launcher refresh is intentionally not handled here to avoid protected access to Common.
        } catch (Exception ex) {
            Logger.error("Failed to apply theme", ex);
        }
    }

    private static java.awt.Color blend(java.awt.Color c1, java.awt.Color c2, float ratio) {
        float r = Math.max(0f, Math.min(1f, ratio));
        int rr = Math.round(c1.getRed() * (1 - r) + c2.getRed() * r);
        int gg = Math.round(c1.getGreen() * (1 - r) + c2.getGreen() * r);
        int bb = Math.round(c1.getBlue() * (1 - r) + c2.getBlue() * r);
        return new java.awt.Color(rr, gg, bb);
    }

    /**
     * Set FlatLaf Look & Feel for the theme with per-platform tuning.
     * @param theme "light" or "dark"
     */
    private static void setLookAndFeelForTheme(String theme) throws Exception {
        String osName = System.getProperty("os.name", "").toLowerCase();
        boolean isWindows = osName.contains("windows");
        String t = theme == null ? "light" : theme.trim().toLowerCase();
        switch (t) {
            case "dark": // legacy
            case "macdark":
                UIManager.setLookAndFeel(new FlatMacDarkLaf());
                break;
            case "light": // legacy
            case "maclight":
                UIManager.setLookAndFeel(new FlatMacLightLaf());
                break;
            case "flatdark":
                UIManager.setLookAndFeel(new FlatDarkLaf());
                break;
            case "flatlight":
                UIManager.setLookAndFeel(new FlatLightLaf());
                break;
            case "intellij":
                UIManager.setLookAndFeel(new FlatIntelliJLaf());
                break;
            case "darcula":
                UIManager.setLookAndFeel(new FlatDarculaLaf());
                break;
            // Extra themes from flatlaf-intellij-themes
            case "onedark":
                setIJTheme("com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme");
                break;
            case "github-light":
                setIJTheme("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatGitHubIJTheme");
                break;
            case "github-dark":
                setIJTheme("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatGitHubDarkIJTheme");
                break;
            case "github-dark-contrast":
                setIJTheme("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatGitHubDarkContrastIJTheme");
                break;
            case "dracula": // alias for specific Dracula IJ theme
                setIJTheme("com.formdev.flatlaf.intellijthemes.FlatDraculaIJTheme");
                break;
            case "nord":
                setIJTheme("com.formdev.flatlaf.intellijthemes.FlatNordIJTheme");
                break;
            case "nord-dark":
                setIJTheme("com.formdev.flatlaf.intellijthemes.FlatNordDarkIJTheme");
                break;
            case "monokai-pro":
                setIJTheme("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMonokaiProIJTheme");
                break;
            case "solarized-light":
                setIJTheme("com.formdev.flatlaf.intellijthemes.FlatSolarizedLightIJTheme");
                break;
            case "solarized-dark":
                setIJTheme("com.formdev.flatlaf.intellijthemes.FlatSolarizedDarkIJTheme");
                break;
            case "arc":
                setIJTheme("com.formdev.flatlaf.intellijthemes.FlatArcIJTheme");
                break;
            case "arc-dark":
                setIJTheme("com.formdev.flatlaf.intellijthemes.FlatArcDarkIJTheme");
                break;
            case "arc-orange":
                setIJTheme("com.formdev.flatlaf.intellijthemes.FlatArcOrangeIJTheme");
                break;
            case "arc-dark-orange":
                setIJTheme("com.formdev.flatlaf.intellijthemes.FlatArcDarkOrangeIJTheme");
                break;
            case "material-light":
                setIJTheme("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialLighterIJTheme");
                break;
            case "material-dark":
                setIJTheme("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDarkerIJTheme");
                break;
            case "material-lighter":
                setIJTheme("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialLighterIJTheme");
                break;
            case "material-darker":
                setIJTheme("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDarkerIJTheme");
                break;
            case "material-palenight":
                setIJTheme("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialPalenightIJTheme");
                break;
            case "cobalt2":
                setIJTheme("com.formdev.flatlaf.intellijthemes.FlatCobalt2IJTheme");
                break;
            case "carbon":
                setIJTheme("com.formdev.flatlaf.intellijthemes.FlatCarbonIJTheme");
                break;
            case "gray":
                setIJTheme("com.formdev.flatlaf.intellijthemes.FlatGrayIJTheme");
                break;
            case "hiberbee-dark":
                setIJTheme("com.formdev.flatlaf.intellijthemes.FlatHiberbeeDarkIJTheme");
                break;
            case "high-contrast":
                setIJTheme("com.formdev.flatlaf.intellijthemes.FlatHighContrastIJTheme");
                break;
            default:
                // Fallback to Mac Light/Dark based on darkness
                if (isDarkTheme(t)) {
                    UIManager.setLookAndFeel(new FlatMacDarkLaf());
                } else {
                    UIManager.setLookAndFeel(new FlatMacLightLaf());
                }
        }
        if (isWindows) {
            // Avoid menu bar integration/title pane issues on Windows
            UIManager.put("TitlePane.useWindowDecorations", Boolean.FALSE);
            UIManager.put("TitlePane.menuBarEmbedded", Boolean.FALSE);
        }
    }

    public static boolean isDarkTheme(String theme) {
        if (theme == null) return false;
        String t = theme.trim().toLowerCase();
    return t.equals("dark")
        || t.equals("macdark")
        || t.equals("flatdark")
        || t.equals("darcula")
        || t.equals("dracula")
        || t.equals("onedark")
        || t.equals("github-dark")
        || t.equals("github-dark-contrast")
                || t.equals("nord")
                || t.equals("nord-dark")
        || t.equals("solarized-dark")
        || t.equals("monokai-pro")
        || t.equals("arc-dark")
        || t.equals("arc-dark-orange")
        || t.equals("material-dark")
        || t.equals("material-darker")
        || t.equals("material-palenight")
        || t.equals("cobalt2")
        || t.equals("carbon")
        || t.equals("hiberbee-dark")
        || t.equals("high-contrast");
    }

    // Helper to load IJ themes by class name with graceful fallback
    private static void setIJTheme(String className) throws Exception {
        try {
            Class<?> cls = Class.forName(className);
            Object laf = cls.getDeclaredConstructor().newInstance();
            if (laf instanceof javax.swing.LookAndFeel) {
                UIManager.setLookAndFeel((javax.swing.LookAndFeel) laf);
            } else {
                // Unexpected; fallback
                UIManager.setLookAndFeel(new FlatIntelliJLaf());
            }
        } catch (Throwable t) {
            // If theme class not present, fallback to IntelliJ
            UIManager.setLookAndFeel(new FlatIntelliJLaf());
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
