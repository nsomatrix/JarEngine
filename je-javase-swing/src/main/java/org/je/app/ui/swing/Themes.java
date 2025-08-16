package org.je.app.ui.swing;

import java.awt.Window;
import java.awt.Dimension;

import javax.swing.UIManager;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatLaf;
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
            // Step 1: Set the Look & Feel first
            setLookAndFeelForTheme(theme);

            // Step 2: Extract theme colors immediately after L&F is set
            ThemeColors themeColors = extractThemeColors();
            
            // Step 3: Update Common with new theme info before UI updates
            Config.setCurrentTheme(theme);
            if (common != null) {
                String themeMode = isDarkTheme(theme) ? "dark" : "light";
                common.setCurrentTheme(themeMode);
                common.setThemeColors(themeColors.backgroundRGB, themeColors.foregroundRGB, themeColors.secondaryRGB);
            }

            // Step 4: Schedule UI updates with enhanced slider handling
            SwingUtilities.invokeLater(() -> {
                try {
                    // Capture slider states before UI update to prevent shrinking
                    java.util.Map<javax.swing.JSlider, SliderState> sliderStates = captureSliderStates();
                    
                    // Apply slider normalization BEFORE updating UI components
                    normalizeSliderSizes();
                    
                    // Update all UI components
                    updateAllUIComponents(windows);
                    
                    // Restore slider states and ensure proper sizing
                    SwingUtilities.invokeLater(() -> {
                        restoreAndFixSliderStates(sliderStates);
                        
                        // Step 5: Update device display theme after UI is refreshed
                        SwingUtilities.invokeLater(() -> {
                            updateDeviceDisplayTheme(isDarkTheme(theme) ? "dark" : "light");
                            
                            // Step 6: Final launcher canvas refresh with minimal delay
                            SwingUtilities.invokeLater(() -> {
                                forceLauncherCanvasRefresh(common);
                            });
                        });
                    });
                    
                } catch (Throwable t) {
                    Logger.error("Error during theme UI update", t);
                }
            });
            
        } catch (Exception ex) {
            Logger.error("Failed to apply theme", ex);
        }
    }

    private static class SliderState {
        final int value;
        final int min;
        final int max;
        final boolean enabled;
        final java.awt.Dimension preferredSize;
        final java.awt.Dimension minimumSize;
        
        SliderState(javax.swing.JSlider slider) {
            this.value = slider.getValue();
            this.min = slider.getMinimum();
            this.max = slider.getMaximum();
            this.enabled = slider.isEnabled();
            this.preferredSize = slider.getPreferredSize();
            this.minimumSize = slider.getMinimumSize();
        }
    }

    private static java.util.Map<javax.swing.JSlider, SliderState> captureSliderStates() {
        java.util.Map<javax.swing.JSlider, SliderState> states = new java.util.HashMap<>();
        try {
            for (Window window : Window.getWindows()) {
                captureSliderStatesRecursive(window, states);
            }
        } catch (Throwable ignored) {}
        return states;
    }

    private static void captureSliderStatesRecursive(java.awt.Container container, 
                                                   java.util.Map<javax.swing.JSlider, SliderState> states) {
        try {
            for (java.awt.Component comp : container.getComponents()) {
                if (comp instanceof javax.swing.JSlider) {
                    states.put((javax.swing.JSlider) comp, new SliderState((javax.swing.JSlider) comp));
                } else if (comp instanceof java.awt.Container) {
                    captureSliderStatesRecursive((java.awt.Container) comp, states);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void normalizeSliderSizes() {
        try {
            // Enhanced slider size normalization with more robust sizing
            if (UIManager.get("Slider.trackWidth") == null || (UIManager.getInt("Slider.trackWidth") < 6))
                UIManager.put("Slider.trackWidth", 10); // Slightly larger for better visibility
            
            Object ts = UIManager.get("Slider.thumbSize");
            if (!(ts instanceof Dimension) || ((Dimension) ts).width < 14 || ((Dimension) ts).height < 14)
                UIManager.put("Slider.thumbSize", new Dimension(16, 16)); // Slightly larger
            
            // Additional slider properties to prevent shrinking
            UIManager.put("Slider.minimumHorizontalSize", new Dimension(36, 21));
            UIManager.put("Slider.minimumVerticalSize", new Dimension(21, 36));
            UIManager.put("Slider.preferredHorizontalSize", new Dimension(200, 21));
            UIManager.put("Slider.preferredVerticalSize", new Dimension(21, 200));
            
        } catch (Throwable ignored) {}
    }

    private static void restoreAndFixSliderStates(java.util.Map<javax.swing.JSlider, SliderState> states) {
        try {
            for (java.util.Map.Entry<javax.swing.JSlider, SliderState> entry : states.entrySet()) {
                javax.swing.JSlider slider = entry.getKey();
                SliderState state = entry.getValue();
                
                // Restore values
                slider.setMinimum(state.min);
                slider.setMaximum(state.max);
                slider.setValue(state.value);
                slider.setEnabled(state.enabled);
                
                // Fix sizing issues by ensuring minimum sizes
                java.awt.Dimension prefSize = state.preferredSize;
                java.awt.Dimension minSize = state.minimumSize;
                
                if (prefSize != null) {
                    // Ensure minimum acceptable slider size
                    int width = Math.max(prefSize.width, 100);
                    int height = Math.max(prefSize.height, 21);
                    slider.setPreferredSize(new java.awt.Dimension(width, height));
                }
                
                if (minSize != null) {
                    int width = Math.max(minSize.width, 50);
                    int height = Math.max(minSize.height, 21);
                    slider.setMinimumSize(new java.awt.Dimension(width, height));
                }
                
                // Force revalidation
                slider.revalidate();
                slider.repaint();
                
                // Revalidate parent containers
                java.awt.Container parent = slider.getParent();
                while (parent != null) {
                    parent.revalidate();
                    parent = parent.getParent();
                }
            }
        } catch (Throwable ignored) {}
    }

    private static class ThemeColors {
        final int backgroundRGB;
        final int foregroundRGB; 
        final int secondaryRGB;
        
        ThemeColors(int bg, int fg, int sec) {
            this.backgroundRGB = bg;
            this.foregroundRGB = fg;
            this.secondaryRGB = sec;
        }
    }

    private static ThemeColors extractThemeColors() {
        try {
            java.awt.Color bg = UIManager.getColor("Panel.background");
            if (bg == null) bg = UIManager.getColor("control");
            java.awt.Color fg = UIManager.getColor("Label.foreground");
            if (fg == null) fg = UIManager.getColor("textText");
            java.awt.Color sec = UIManager.getColor("Label.disabledForeground");
            if (sec == null) sec = UIManager.getColor("Component.infoForeground");
            
            // If secondary still null, derive it by blending fg toward bg
            if (sec == null && bg != null && fg != null) sec = blend(fg, bg, 0.5f);
            
            if (bg != null && fg != null) {
                int bgRGB = (bg.getRed() << 16) | (bg.getGreen() << 8) | bg.getBlue();
                int fgRGB = (fg.getRed() << 16) | (fg.getGreen() << 8) | fg.getBlue();
                int secRGB = (sec != null ? ((sec.getRed() << 16) | (sec.getGreen() << 8) | sec.getBlue()) : fgRGB);
                return new ThemeColors(bgRGB, fgRGB, secRGB);
            }
        } catch (Throwable ignore) {}
        
        // Fallback colors
        return new ThemeColors(-1, -1, -1);
    }

    private static void updateAllUIComponents(Window... windows) {
        try {
            // FlatLaf global update
            FlatLaf.updateUI();
        } catch (Throwable ignored) {
            // Fallback: manual update
            for (Window w : Window.getWindows()) {
                try { 
                    javax.swing.SwingUtilities.updateComponentTreeUI(w); 
                    w.repaint();
                } catch (Throwable __) {}
            }
        }
        
        // Update explicitly provided windows
        if (windows != null) {
            for (Window w : windows) {
                if (w != null) {
                    try { 
                        javax.swing.SwingUtilities.updateComponentTreeUI(w);
                        w.repaint();
                    } catch (Throwable __) {}
                }
            }
        }
    }

    private static void updateDeviceDisplayTheme(String themeMode) {
        try {
            if (DeviceFactory.getDevice() != null &&
                    DeviceFactory.getDevice().getDeviceDisplay() instanceof org.je.device.j2se.J2SEDeviceDisplay) {
                ((org.je.device.j2se.J2SEDeviceDisplay) DeviceFactory.getDevice().getDeviceDisplay())
                    .updateThemeColors(themeMode);
            }
        } catch (Throwable t) {
            Logger.error("Failed to update device display theme colors", t);
        }
    }

    private static void forceLauncherCanvasRefresh(Common common) {
        try {
            if (common != null) {
                // Approach 1: Re-set theme colors to force refresh (most reliable)
                ThemeColors colors = extractThemeColors();
                common.setThemeColors(colors.backgroundRGB, colors.foregroundRGB, colors.secondaryRGB);
                
                // Approach 2: Trigger a launcher refresh if method exists (safe reflection)
                try {
                    java.lang.reflect.Method refreshMethod = common.getClass().getMethod("refreshLauncher");
                    refreshMethod.invoke(common);
                } catch (Throwable ignored) {
                    // Method might not exist, that's OK
                }
                
                // Approach 3: Gentle repaint - only repaint visible windows to prevent freezing
                SwingUtilities.invokeLater(() -> {
                    try {
                        for (Window window : Window.getWindows()) {
                            if (window.isVisible() && window.isDisplayable()) {
                                window.repaint();
                                // Don't do deep traversal to prevent excessive repainting
                            }
                        }
                    } catch (Throwable ignored) {}
                });
            }
        } catch (Throwable ignored) {
            // Best effort - don't fail theme switching if launcher refresh fails
        }
    }

    // Removed aggressive repaintComponentTree method to prevent freezing

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
