package org.je.app.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.je.log.Logger;

/**
 * UI Manager configuration persistence for JarEngine.
 * Manages status bar component visibility and UI update settings.
 * Follows the same pattern as PerformanceManager, FilterManager, and NetConfig.
 */
public final class UIManagerConfig {

    private UIManagerConfig() {}

    // ========= UI Manager Settings =========
    private static volatile boolean updatesEnabled = true;  // Status updates enabled by default
    private static volatile boolean timerEnabled = true;    // Runtime timer enabled by default
    private static volatile boolean networkMeterEnabled = true; // Network meter enabled by default
    
    // Future extensibility - add new UI components here
    // private static volatile boolean memoryMonitorEnabled = true;
    // private static volatile boolean fpsCounterEnabled = false;
    // private static volatile boolean deviceInfoEnabled = true;

    // ========= Persistence =========
    private static final String FILE_NAME = "uimanager.properties";
    private static volatile boolean preferencesLoaded = false;
    private static volatile boolean pendingSave = false;
    private static long lastSaveTime = 0L;
    private static final long SAVE_DEBOUNCE_MS = 750; // batch rapid changes

    static {
        loadPreferences();
    }

    // ========= Public Getters =========
    public static boolean isUpdatesEnabled() {
        return updatesEnabled;
    }

    public static boolean isTimerEnabled() {
        return timerEnabled;
    }

    public static boolean isNetworkMeterEnabled() {
        return networkMeterEnabled;
    }

    // ========= Public Setters with Persistence =========
    public static void setUpdatesEnabled(boolean enabled) {
        if (updatesEnabled != enabled) {
            updatesEnabled = enabled;
            savePreferencesAsync();
        }
    }

    public static void setTimerEnabled(boolean enabled) {
        if (timerEnabled != enabled) {
            timerEnabled = enabled;
            savePreferencesAsync();
        }
    }

    public static void setNetworkMeterEnabled(boolean enabled) {
        if (networkMeterEnabled != enabled) {
            networkMeterEnabled = enabled;
            savePreferencesAsync();
        }
    }

    // ========= Reset to Defaults =========
    public static synchronized void resetToDefaults() {
        updatesEnabled = true;
        timerEnabled = true;
        networkMeterEnabled = true;
        savePreferencesAsync();
    }

    // ========= Persistence Implementation =========
    private static File getPreferencesFile() {
        try {
            File jeHome = new File(System.getProperty("user.home"), ".je");
            jeHome.mkdirs();
            return new File(jeHome, FILE_NAME);
        } catch (Exception e) {
            Logger.error("Failed to get UI Manager preferences file path", e);
            return new File(FILE_NAME); // fallback to current directory
        }
    }

    public static synchronized void loadPreferences() {
        if (preferencesLoaded) return;
        
        File prefsFile = getPreferencesFile();
        if (prefsFile.exists()) {
            Properties p = new Properties();
            try (FileInputStream fis = new FileInputStream(prefsFile)) {
                p.load(fis);
                
                // Load settings with defaults
                updatesEnabled = Boolean.parseBoolean(p.getProperty("updatesEnabled", Boolean.toString(updatesEnabled)));
                timerEnabled = Boolean.parseBoolean(p.getProperty("timerEnabled", Boolean.toString(timerEnabled)));
                networkMeterEnabled = Boolean.parseBoolean(p.getProperty("networkMeterEnabled", Boolean.toString(networkMeterEnabled)));
                
                // Future settings can be loaded here:
                // memoryMonitorEnabled = Boolean.parseBoolean(p.getProperty("memoryMonitorEnabled", Boolean.toString(memoryMonitorEnabled)));
                // fpsCounterEnabled = Boolean.parseBoolean(p.getProperty("fpsCounterEnabled", Boolean.toString(fpsCounterEnabled)));
                // deviceInfoEnabled = Boolean.parseBoolean(p.getProperty("deviceInfoEnabled", Boolean.toString(deviceInfoEnabled)));
                
            } catch (IOException e) {
                Logger.debug("Could not load UI Manager preferences: " + e.getMessage());
                // Use defaults
            }
        }
        preferencesLoaded = true;
    }

    public static void savePreferencesAsync() {
        if (!preferencesLoaded) return; // don't save during static initialization until load completes
        
        synchronized (UIManagerConfig.class) {
            pendingSave = true;
            long now = System.currentTimeMillis();
            
            // Simple debounce: only save if sufficient time passed or force after delay
            if (now - lastSaveTime >= SAVE_DEBOUNCE_MS) {
                savePreferences();
            } else {
                // Schedule a delayed save via a daemon thread
                Thread t = new Thread(() -> {
                    try { 
                        Thread.sleep(SAVE_DEBOUNCE_MS); 
                    } catch (InterruptedException ignored) {}
                    
                    synchronized (UIManagerConfig.class) {
                        if (pendingSave && System.currentTimeMillis() - lastSaveTime >= SAVE_DEBOUNCE_MS) {
                            savePreferences();
                        }
                    }
                }, "UIManagerPrefsSaver");
                t.setDaemon(true);
                try {
                    t.start();
                } catch (IllegalThreadStateException ignored) {}
            }
        }
    }

    private static synchronized void savePreferences() {
        pendingSave = false;
        lastSaveTime = System.currentTimeMillis();
        
        Properties p = new Properties();
        p.setProperty("updatesEnabled", Boolean.toString(updatesEnabled));
        p.setProperty("timerEnabled", Boolean.toString(timerEnabled));
        p.setProperty("networkMeterEnabled", Boolean.toString(networkMeterEnabled));
        
        // Future settings can be saved here:
        // p.setProperty("memoryMonitorEnabled", Boolean.toString(memoryMonitorEnabled));
        // p.setProperty("fpsCounterEnabled", Boolean.toString(fpsCounterEnabled));
        // p.setProperty("deviceInfoEnabled", Boolean.toString(deviceInfoEnabled));

        File prefsFile = getPreferencesFile();
        try (FileOutputStream fos = new FileOutputStream(prefsFile)) {
            p.store(fos, "UI Manager Configuration for JarEngine\n# Generated automatically - do not edit manually");
            Logger.debug("UI Manager preferences saved to: " + prefsFile.getAbsolutePath());
        } catch (IOException e) {
            Logger.error("Failed to save UI Manager preferences", e);
        }
        
        // Trigger config save spinner if Common is available
        try {
            org.je.app.Common.showConfigSpinner(600);
        } catch (Throwable ignored) {
            // Common might not be available during shutdown or early initialization
        }
    }
}
