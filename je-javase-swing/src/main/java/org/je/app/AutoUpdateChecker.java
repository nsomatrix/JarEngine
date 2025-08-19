package org.je.app;

import javax.swing.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.je.log.Logger;
import org.je.app.util.BuildVersion;

/**
 * Automatic update checker service for JarEngine.
 * Runs background checks for updates based on user preferences and shows
 * non-intrusive notifications when updates are available.
 * 
 * Features:
 * - Automatic periodic checking (configurable interval)
 * - "Remind me later" functionality
 * - Non-blocking notifications
 * - Respects user preferences (can be disabled)
 * - Integrates with existing UpdateChecker and UpdateDialog
 * 
 * Threading: Uses shared executor to prevent thread leaks
 */
public class AutoUpdateChecker {

    private static AutoUpdateChecker instance;
    private static final ScheduledExecutorService SHARED_EXECUTOR = 
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AutoUpdateChecker-Shared");
            t.setDaemon(true);
            return t;
        });
    
    static {
        // Add shutdown hook to cleanly shutdown the executor
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                SHARED_EXECUTOR.shutdown();
                if (!SHARED_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                    SHARED_EXECUTOR.shutdownNow();
                }
            } catch (InterruptedException e) {
                SHARED_EXECUTOR.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }, "AutoUpdateChecker-Shutdown"));
    }
    
    private ScheduledFuture<?> periodicTask;
    private ScheduledFuture<?> initialTask;
    private JFrame parentFrame;
    private volatile boolean running = false;

    private AutoUpdateChecker() {
        // Private constructor for singleton
    }

    /**
     * Get the singleton instance
     */
    public static synchronized AutoUpdateChecker getInstance() {
        if (instance == null) {
            instance = new AutoUpdateChecker();
        }
        return instance;
    }

    /**
     * Initialize the auto update checker with the parent frame
     * @param parent The main application frame for showing dialogs
     */
    public void initialize(JFrame parent) {
        this.parentFrame = parent;
        if (UpdateConfig.isAutoCheckEnabled()) {
            start();
        }
    }

    /**
     * Start the automatic update checking service
     */
    public synchronized void start() {
        if (running) {
            return; // Already running
        }

        if (!UpdateConfig.isAutoCheckEnabled()) {
            return;
        }
        
        // Cancel any existing tasks
        cancelTasks();
        
        // Schedule the first check after a short delay (30 seconds)
        // to allow the application to fully start up
        initialTask = SHARED_EXECUTOR.schedule(this::performUpdateCheck, 30, TimeUnit.SECONDS);

        // Then schedule regular checks based on the configured interval
        long intervalMinutes = UpdateConfig.getCheckIntervalHours() * 60;
        periodicTask = SHARED_EXECUTOR.scheduleAtFixedRate(this::performUpdateCheck, 
            intervalMinutes, intervalMinutes, TimeUnit.MINUTES);

        running = true;
    }

    /**
     * Stop the automatic update checking service
     */
    public synchronized void stop() {
        if (!running) {
            return;
        }
        
        cancelTasks();
        running = false;
    }
    
    /**
     * Cancel all scheduled tasks
     */
    private void cancelTasks() {
        if (initialTask != null && !initialTask.isDone()) {
            initialTask.cancel(false);
            initialTask = null;
        }
        if (periodicTask != null && !periodicTask.isDone()) {
            periodicTask.cancel(false);
            periodicTask = null;
        }
    }

    /**
     * Restart the service with new settings (thread-safe)
     */
    public void restart() {
        // Use shared executor instead of creating new threads
        SHARED_EXECUTOR.execute(() -> {
            synchronized (AutoUpdateChecker.this) {
                stop();
                if (UpdateConfig.isAutoCheckEnabled()) {
                    start();
                }
            }
        });
    }

    /**
     * Check if the service is running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Perform the actual update check
     */
    private void performUpdateCheck() {
        try {
            // Check if we should actually perform the check
            if (!UpdateConfig.isAutoCheckEnabled()) {
                return;
            }

            // Check if it's time to check for updates
            if (!UpdateConfig.shouldCheckForUpdates() && !UpdateConfig.shouldShowReminder()) {
                return;
            }

            // Get current and latest versions
            String currentVersion = BuildVersion.getVersion();
            String latestVersion = UpdateChecker.getLatestVersion();

            // Mark that we completed the check
            UpdateConfig.markUpdateCheckCompleted();
            UpdateConfig.setLastKnownVersion(latestVersion);

            // Check if an update is available
            boolean updateAvailable = UpdateChecker.isUpdateAvailable(currentVersion, latestVersion);

            if (updateAvailable) {
                // Show notification if we haven't already shown it for this version
                if (!UpdateConfig.isUpdateNotificationShown()) {
                    SwingUtilities.invokeLater(() -> showUpdateNotification(latestVersion, currentVersion));
                    UpdateConfig.setUpdateNotificationShown(true);
                }
            } else if (UpdateConfig.shouldShowReminder()) {
                // Check if there's a reminder to show
                SwingUtilities.invokeLater(() -> showReminderNotification());
            }

        } catch (Exception e) {
            // Silently handle errors in background update checks
            // No logging to avoid terminal spam
        }
    }

    /**
     * Show a non-intrusive update notification
     */
    private void showUpdateNotification(String latestVersion, String currentVersion) {
        if (parentFrame == null) {
            return; // Can't show notification without parent frame
        }

        String message = String.format(
            "A new version of JarEngine is available!\n\n" +
            "Current version: %s\n" +
            "Latest version: %s\n\n" +
            "Would you like to update now?", 
            currentVersion, latestVersion
        );

        String[] options = {"Update Now", "Remind Me Later", "Don't Ask Again"};
        
        int choice = JOptionPane.showOptionDialog(
            parentFrame,
            message,
            "Update Available - JarEngine",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            options,
            options[0] // Default to "Update Now"
        );

        switch (choice) {
            case 0: // Update Now
                showUpdateDialog();
                UpdateConfig.clearReminder();
                break;
            case 1: // Remind Me Later
                UpdateConfig.snoozeReminder();
                break;
            case 2: // Don't Ask Again
                UpdateConfig.setAutoCheckEnabled(false);
                UpdateConfig.clearReminder();
                break;
            default:
                // User closed dialog - treat as "Remind Me Later"
                UpdateConfig.snoozeReminder();
                break;
        }
    }

    /**
     * Show a reminder notification
     */
    private void showReminderNotification() {
        if (parentFrame == null) {
            return;
        }

        String lastKnownVersion = UpdateConfig.getLastKnownVersion();
        if (lastKnownVersion.isEmpty()) {
            return; // No known update to remind about
        }

        String currentVersion = BuildVersion.getVersion();
        boolean updateStillAvailable = UpdateChecker.isUpdateAvailable(currentVersion, lastKnownVersion);
        
        if (!updateStillAvailable) {
            // Update is no longer available, clear the reminder
            UpdateConfig.clearReminder();
            return;
        }

        String message = String.format(
            "Reminder: Update to JarEngine %s is still available.\n\n" +
            "Current version: %s\n" +
            "Available version: %s\n\n" +
            "Would you like to update now?", 
            lastKnownVersion, currentVersion, lastKnownVersion
        );

        String[] options = {"Update Now", "Remind Me Later", "Stop Reminding"};
        
        int choice = JOptionPane.showOptionDialog(
            parentFrame,
            message,
            "Update Reminder - JarEngine",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            options,
            options[0]
        );

        switch (choice) {
            case 0: // Update Now
                showUpdateDialog();
                UpdateConfig.clearReminder();
                break;
            case 1: // Remind Me Later
                UpdateConfig.snoozeReminder();
                break;
            case 2: // Stop Reminding
                UpdateConfig.clearReminder();
                break;
            default:
                // User closed dialog - treat as "Remind Me Later"
                UpdateConfig.snoozeReminder();
                break;
        }
    }

    /**
     * Show the update dialog
     */
    private void showUpdateDialog() {
        if (parentFrame != null) {
            try {
                // Import the UpdateDialog class
                org.je.app.ui.swing.UpdateDialog dialog = 
                    new org.je.app.ui.swing.UpdateDialog(parentFrame);
                dialog.setVisible(true);
            } catch (Exception e) {
                Logger.error("Error showing update dialog", e);
                // Fallback: show simple message
                JOptionPane.showMessageDialog(parentFrame,
                    "Please check for updates manually from the Help menu.",
                    "Update Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Force a manual check for updates (called from UI)
     */
    public void checkNow() {
        // Always use shared executor for consistency
        SHARED_EXECUTOR.execute(this::performUpdateCheck);
    }

    /**
     * Get status information about the auto-update service
     */
    public String getStatusInfo() {
        if (!UpdateConfig.isAutoCheckEnabled()) {
            return "Automatic updates: Disabled";
        }

        if (!running) {
            return "Automatic updates: Enabled (Service not running)";
        }

        long lastCheck = UpdateConfig.getLastCheckTime();
        long nextCheck = lastCheck + (UpdateConfig.getCheckIntervalHours() * 60 * 60 * 1000L);
        long timeUntilNext = nextCheck - System.currentTimeMillis();

        if (timeUntilNext <= 0) {
            return "Automatic updates: Enabled (Check due now)";
        }

        long hoursUntilNext = timeUntilNext / (60 * 60 * 1000L);
        return String.format("Automatic updates: Enabled (Next check in %d hours)", hoursUntilNext);
    }
}
