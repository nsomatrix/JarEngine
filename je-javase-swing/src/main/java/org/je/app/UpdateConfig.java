package org.je.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.je.log.Logger;

/**
 * Configuration manager for automatic updates in JarEngine.
 * Manages automatic update checking settings, reminder intervals, and preferences.
 * Follows the same pattern as UIManagerConfig, PerformanceManager, and NetConfig.
 */
public final class UpdateConfig {

    private UpdateConfig() {}

    // ========= Update Configuration Settings =========
    private static volatile boolean autoCheckEnabled = true;        // Auto check enabled by default
    private static volatile boolean remindMeLater = false;          // Don't remind by default
    private static volatile long lastCheckTime = 0L;               // Last time we checked for updates
    private static volatile long reminderTime = 0L;                // When to remind again (if remind me is set)
    private static volatile int checkIntervalHours = 24;           // Check every 24 hours by default
    private static volatile int reminderIntervalHours = 24;        // Remind after 24 hours by default
    private static volatile String lastKnownVersion = "";          // Last version we found available
    private static volatile boolean updateNotificationShown = false; // Whether we already showed notification for current version

    // ========= Persistence =========
    private static final String FILE_NAME = "updates.properties";
    private static volatile boolean preferencesLoaded = false;
    private static volatile boolean pendingSave = false;
    private static long lastSaveTime = 0L;
    private static final long SAVE_DEBOUNCE_MS = 750; // batch rapid changes
    
    // ========= Thread Management =========
    private static final ScheduledExecutorService SAVE_EXECUTOR = 
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "UpdateConfigSaver");
            t.setDaemon(true);
            return t;
        });
    private static volatile ScheduledFuture<?> pendingSaveTask;
    
    // ========= Constants =========
    private static final long HOUR_IN_MS = 60 * 60 * 1000L;

    static {
        // Load preferences first
        loadPreferences();
        
        // Add shutdown hook to cleanly save any pending changes and shutdown executor
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                // Save any pending changes immediately
                if (pendingSave) {
                    savePreferences();
                }
                // Shutdown executor
                SAVE_EXECUTOR.shutdown();
                if (!SAVE_EXECUTOR.awaitTermination(2, TimeUnit.SECONDS)) {
                    SAVE_EXECUTOR.shutdownNow();
                }
            } catch (Exception e) {
                // Ignore errors during shutdown
            }
        }, "UpdateConfig-Shutdown"));
    }

    // ========= Public Getters =========
    public static boolean isAutoCheckEnabled() {
        return autoCheckEnabled;
    }

    public static boolean isRemindMeLater() {
        return remindMeLater;
    }

    public static long getLastCheckTime() {
        return lastCheckTime;
    }

    public static long getReminderTime() {
        return reminderTime;
    }

    public static int getCheckIntervalHours() {
        return checkIntervalHours;
    }

    public static int getReminderIntervalHours() {
        return reminderIntervalHours;
    }

    public static String getLastKnownVersion() {
        return lastKnownVersion;
    }

    public static boolean isUpdateNotificationShown() {
        return updateNotificationShown;
    }

    // ========= Public Setters with Persistence =========
    public static void setAutoCheckEnabled(boolean enabled) {
        if (autoCheckEnabled != enabled) {
            autoCheckEnabled = enabled;
            savePreferencesAsync();
        }
    }

    public static void setRemindMeLater(boolean remind) {
        if (remindMeLater != remind) {
            remindMeLater = remind;
            if (remind) {
                // Set reminder time to current time + reminder interval
                reminderTime = System.currentTimeMillis() + (reminderIntervalHours * HOUR_IN_MS);
            } else {
                reminderTime = 0L;
            }
            savePreferencesAsync();
        }
    }

    public static void setLastCheckTime(long time) {
        if (lastCheckTime != time) {
            lastCheckTime = time;
            savePreferencesAsync();
        }
    }

    public static void setCheckIntervalHours(int hours) {
        if (checkIntervalHours != hours && hours > 0) {
            checkIntervalHours = hours;
            savePreferencesAsync();
        }
    }

    public static void setReminderIntervalHours(int hours) {
        if (reminderIntervalHours != hours && hours > 0) {
            reminderIntervalHours = hours;
            savePreferencesAsync();
        }
    }

    public static void setLastKnownVersion(String version) {
        if (!lastKnownVersion.equals(version)) {
            lastKnownVersion = version != null ? version : "";
            // Reset notification flag when version changes
            updateNotificationShown = false;
            savePreferencesAsync();
        }
    }

    public static void setUpdateNotificationShown(boolean shown) {
        if (updateNotificationShown != shown) {
            updateNotificationShown = shown;
            savePreferencesAsync();
        }
    }

    // ========= Helper Methods =========
    
    /**
     * Check if it's time to automatically check for updates
     */
    public static boolean shouldCheckForUpdates() {
        if (!autoCheckEnabled) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long timeSinceLastCheck = currentTime - lastCheckTime;
        return timeSinceLastCheck >= (checkIntervalHours * HOUR_IN_MS);
    }

    /**
     * Check if it's time to show the reminder
     */
    public static boolean shouldShowReminder() {
        if (!remindMeLater || reminderTime == 0L) {
            return false;
        }
        
        return System.currentTimeMillis() >= reminderTime;
    }

    /**
     * Mark that we've completed an update check
     */
    public static void markUpdateCheckCompleted() {
        setLastCheckTime(System.currentTimeMillis());
    }

    /**
     * Clear the reminder (user chose to update or dismiss permanently)
     */
    public static void clearReminder() {
        setRemindMeLater(false);
    }

    /**
     * Set a new reminder for later
     */
    public static void snoozeReminder() {
        setRemindMeLater(true);
    }

    // ========= Reset to Defaults =========
    public static synchronized void resetToDefaults() {
        autoCheckEnabled = true;
        remindMeLater = false;
        lastCheckTime = 0L;
        reminderTime = 0L;
        checkIntervalHours = 24;
        reminderIntervalHours = 24;
        lastKnownVersion = "";
        updateNotificationShown = false;
        savePreferencesAsync();
    }

    // ========= Persistence Implementation =========
    private static File getPreferencesFile() {
        try {
            File jeHome = new File(System.getProperty("user.home"), ".je");
            jeHome.mkdirs();
            return new File(jeHome, FILE_NAME);
        } catch (Exception e) {
            Logger.error("Failed to get Update Config preferences file path", e);
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
                autoCheckEnabled = Boolean.parseBoolean(p.getProperty("autoCheckEnabled", Boolean.toString(autoCheckEnabled)));
                remindMeLater = Boolean.parseBoolean(p.getProperty("remindMeLater", Boolean.toString(remindMeLater)));
                lastCheckTime = Long.parseLong(p.getProperty("lastCheckTime", Long.toString(lastCheckTime)));
                reminderTime = Long.parseLong(p.getProperty("reminderTime", Long.toString(reminderTime)));
                checkIntervalHours = Integer.parseInt(p.getProperty("checkIntervalHours", Integer.toString(checkIntervalHours)));
                reminderIntervalHours = Integer.parseInt(p.getProperty("reminderIntervalHours", Integer.toString(reminderIntervalHours)));
                lastKnownVersion = p.getProperty("lastKnownVersion", lastKnownVersion);
                updateNotificationShown = Boolean.parseBoolean(p.getProperty("updateNotificationShown", Boolean.toString(updateNotificationShown)));
                
            } catch (IOException | NumberFormatException e) {
                // Could not load preferences, use defaults
            }
        }
        preferencesLoaded = true;
    }

    public static void savePreferencesAsync() {
        if (!preferencesLoaded) return; // don't save during static initialization until load completes
        
        synchronized (UpdateConfig.class) {
            pendingSave = true;
            long now = System.currentTimeMillis();
            
            // Cancel any pending save task
            if (pendingSaveTask != null && !pendingSaveTask.isDone()) {
                pendingSaveTask.cancel(false);
            }
            
            // Simple debounce: only save if sufficient time passed or schedule after delay
            if (now - lastSaveTime >= SAVE_DEBOUNCE_MS) {
                savePreferences();
            } else {
                // Schedule a delayed save using managed executor
                pendingSaveTask = SAVE_EXECUTOR.schedule(() -> {
                    synchronized (UpdateConfig.class) {
                        if (pendingSave && System.currentTimeMillis() - lastSaveTime >= SAVE_DEBOUNCE_MS) {
                            savePreferences();
                        }
                    }
                }, SAVE_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
            }
        }
    }

    private static synchronized void savePreferences() {
        pendingSave = false;
        lastSaveTime = System.currentTimeMillis();
        
        Properties p = new Properties();
        p.setProperty("autoCheckEnabled", Boolean.toString(autoCheckEnabled));
        p.setProperty("remindMeLater", Boolean.toString(remindMeLater));
        p.setProperty("lastCheckTime", Long.toString(lastCheckTime));
        p.setProperty("reminderTime", Long.toString(reminderTime));
        p.setProperty("checkIntervalHours", Integer.toString(checkIntervalHours));
        p.setProperty("reminderIntervalHours", Integer.toString(reminderIntervalHours));
        p.setProperty("lastKnownVersion", lastKnownVersion);
        p.setProperty("updateNotificationShown", Boolean.toString(updateNotificationShown));

        File prefsFile = getPreferencesFile();
        try (FileOutputStream fos = new FileOutputStream(prefsFile)) {
            p.store(fos, "Update Configuration for JarEngine\n# Generated automatically - do not edit manually");
            // Preferences saved silently
        } catch (IOException e) {
            Logger.error("Failed to save Update Config preferences", e);
        }
        
        // Trigger config save spinner if Common is available
        try {
            org.je.app.Common.showConfigSpinner(600);
        } catch (Throwable ignored) {
            // Common might not be available during shutdown or early initialization
        }
    }
}
