package org.je.app.ui.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.je.app.UpdateChecker;
import org.je.app.UpdateConfig;
import org.je.app.AutoUpdateChecker;
import org.je.app.util.BuildVersion;

/**
 * Standard dialog for checking and applying updates
 */
public class UpdateDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JButton checkButton;
    private JButton updateButton;
    private JButton closeButton;
    
    // Auto-update settings components
    private JCheckBox autoCheckBox;
    private JComboBox<String> intervalComboBox;
    private JLabel autoUpdateStatusLabel;
    
    // Version display components
    private JLabel latestVersionLabel;
    
    private String currentVersion;
    private String latestVersion;
    private boolean updateAvailable = false;

    public UpdateDialog(JFrame parent) {
        super(parent, "Check for Updates", true);
        this.currentVersion = BuildVersion.getVersion();

        // Use industry standard approach - let Swing handle everything naturally
        initComponents();
        layoutComponents();
        setupEventHandlers();

        pack(); // Size the dialog naturally based on content
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initComponents() {
        statusLabel = new JLabel("Click 'Check for Updates' to check for available updates.");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        
        checkButton = new JButton("Check");
        updateButton = new JButton("Update");
        updateButton.setEnabled(false);
        closeButton = new JButton("Close");
        
        // Auto-update settings components
        autoCheckBox = new JCheckBox("Automatically check for updates", UpdateConfig.isAutoCheckEnabled());
        
        String[] intervals = {"Every 6 hours", "Every 12 hours", "Daily", "Every 2 days", "Weekly"};
        intervalComboBox = new JComboBox<>(intervals);
        
        // Set current interval selection
        int currentInterval = UpdateConfig.getCheckIntervalHours();
        switch (currentInterval) {
            case 6: intervalComboBox.setSelectedIndex(0); break;
            case 12: intervalComboBox.setSelectedIndex(1); break;
            case 24: intervalComboBox.setSelectedIndex(2); break;
            case 48: intervalComboBox.setSelectedIndex(3); break;
            case 168: intervalComboBox.setSelectedIndex(4); break;
            default: intervalComboBox.setSelectedIndex(2); break; // Default to daily
        }
        
        autoUpdateStatusLabel = new JLabel();
        updateAutoUpdateStatus();
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());

        // Main content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Version info panel with better styling
        JPanel versionPanel = new JPanel(new GridLayout(2, 2, 10, 5));
        versionPanel.setBorder(BorderFactory.createTitledBorder("Version Information"));
        
        JLabel currentVersionLabel = new JLabel("Current Version:");
        currentVersionLabel.setFont(currentVersionLabel.getFont().deriveFont(Font.BOLD));
        versionPanel.add(currentVersionLabel);
        
        JLabel currentVersionValue = new JLabel(currentVersion);
        currentVersionValue.setFont(currentVersionValue.getFont().deriveFont(Font.PLAIN));
        versionPanel.add(currentVersionValue);
        
        JLabel latestVersionLabel = new JLabel("Latest Version:");
        latestVersionLabel.setFont(latestVersionLabel.getFont().deriveFont(Font.BOLD));
        versionPanel.add(latestVersionLabel);
        
        this.latestVersionLabel = new JLabel("Unknown");
        this.latestVersionLabel.setFont(this.latestVersionLabel.getFont().deriveFont(Font.PLAIN));
        versionPanel.add(this.latestVersionLabel);

        contentPanel.add(versionPanel);
        contentPanel.add(Box.createVerticalStrut(15));

        // Status and progress panel
        JPanel statusPanel = new JPanel(new BorderLayout(0, 10));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Update Status"));
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        statusPanel.add(progressBar, BorderLayout.SOUTH);
        contentPanel.add(statusPanel);

        // Auto-update settings panel (always visible)
        contentPanel.add(Box.createVerticalStrut(15));
        contentPanel.add(createExpandedSettingsPanel());

        add(contentPanel, BorderLayout.CENTER);

        // Button panel with better spacing and styling
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        // Style buttons for better appearance
        styleButton(checkButton, "Check for updates");
        styleButton(updateButton, "Download and install update");
        styleButton(closeButton, "Close dialog");
        
        buttonPanel.add(checkButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Apply consistent styling to buttons
     */
    private void styleButton(JButton button, String tooltip) {
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(120, 30));
    }



    private void setupEventHandlers() {
        checkButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checkForUpdates();
            }
        });
        
        updateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                downloadAndInstallUpdate();
            }
        });
        
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        // Auto-update settings event handlers
        autoCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean enabled = autoCheckBox.isSelected();
                UpdateConfig.setAutoCheckEnabled(enabled);
                intervalComboBox.setEnabled(enabled);
                updateAutoUpdateStatus();
                
                // Restart auto-update service with new settings
                AutoUpdateChecker.getInstance().restart();
            }
        });

        intervalComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!intervalComboBox.isEnabled()) return;
                
                int selectedIndex = intervalComboBox.getSelectedIndex();
                int hours;
                switch (selectedIndex) {
                    case 0: hours = 6; break;    // Every 6 hours
                    case 1: hours = 12; break;   // Every 12 hours
                    case 2: hours = 24; break;   // Daily
                    case 3: hours = 48; break;   // Every 2 days
                    case 4: hours = 168; break;  // Weekly
                    default: hours = 24; break;  // Default to daily
                }
                
                UpdateConfig.setCheckIntervalHours(hours);
                updateAutoUpdateStatus();
                
                // Restart auto-update service with new interval
                AutoUpdateChecker.getInstance().restart();
            }
        });
    }

    private void checkForUpdates() {
        checkButton.setEnabled(false);
        progressBar.setVisible(true);
        statusLabel.setText("Checking for updates...");
        
        // Run in background thread to avoid blocking UI
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    latestVersion = UpdateChecker.getLatestVersion();
                    updateAvailable = UpdateChecker.isUpdateAvailable(currentVersion, latestVersion);
                    
                    // Update the config with the latest version info
                    UpdateConfig.markUpdateCheckCompleted();
                    UpdateConfig.setLastKnownVersion(latestVersion);
                    if (updateAvailable) {
                        UpdateConfig.setUpdateNotificationShown(false); // Reset for new version
                    }
                    
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            updateUIAfterCheck();
                        }
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            handleCheckError(e);
                        }
                    });
                }
            }
        });
    }

    private void updateUIAfterCheck() {
        progressBar.setVisible(false);
        checkButton.setEnabled(true);
        
        if (updateAvailable) {
            statusLabel.setText("Update available! Version " + latestVersion + " is ready to download.");
            updateButton.setEnabled(true);
        } else {
            statusLabel.setText("You have the latest version (" + currentVersion + ").");
            updateButton.setEnabled(false);
        }
        
        // Update the latest version display
        updateLatestVersionDisplay(latestVersion != null ? latestVersion : "Unknown");
    }

    private void handleCheckError(Exception e) {
        progressBar.setVisible(false);
        checkButton.setEnabled(true);
        statusLabel.setText("Error checking for updates: " + e.getMessage());
        updateButton.setEnabled(false);
    }

    private void downloadAndInstallUpdate() {
        if (!updateAvailable || latestVersion == null) {
            return;
        }
        
        String message = "This will download and install version " + latestVersion + ".\n" +
                        "The application will restart automatically.\n\n" +
                        "Do you want to continue?";
        
        String[] options = {"Update Now", "Remind Me Later", "Cancel"};
        
        int result = JOptionPane.showOptionDialog(
            this,
            message,
            "Confirm Update",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0] // Default to "Update Now"
        );
        
        if (result == 1) { // Remind Me Later
            UpdateConfig.snoozeReminder();
            JOptionPane.showMessageDialog(this,
                "You will be reminded about this update in " + 
                UpdateConfig.getReminderIntervalHours() + " hours.",
                "Reminder Set",
                JOptionPane.INFORMATION_MESSAGE);
            dispose();
            return;
        } else if (result != 0) { // Cancel or closed dialog
            return;
        }
        
        // Clear any existing reminder since user chose to update now
        UpdateConfig.clearReminder();
        
        // Disable buttons during download
        checkButton.setEnabled(false);
        updateButton.setEnabled(false);
        closeButton.setEnabled(false);
        
        progressBar.setVisible(true);
        statusLabel.setText("Downloading update...");
        
        // Run download in background with progress tracking
        new Thread(() -> {
            try {
                // Create temporary file for download
                File tempFile = File.createTempFile("JarEngine-update-", ".jar");
                tempFile.deleteOnExit();
                
                // Show download progress
                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(0);
                    statusLabel.setText("Preparing download...");
                });
                
                // Download the update with progress updates
                downloadWithProgress(latestVersion, tempFile);
                
                // Show success message
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Update downloaded successfully. Installing...");
                    progressBar.setValue(100);
                });
                
                // Wait a moment for user to see the message, then apply update
                Thread.sleep(2000);
                
                // Apply the update (this will restart the application)
                UpdateChecker.applyUpdateAndRestart(tempFile, latestVersion);
                
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    handleUpdateError(e);
                });
            }
        }, "UpdateDownloader").start();
    }

    private void handleUpdateError(Exception e) {
        progressBar.setVisible(false);
        checkButton.setEnabled(true);
        updateButton.setEnabled(updateAvailable);
        closeButton.setEnabled(true);
        statusLabel.setText("Error during update: " + e.getMessage());
        
        JOptionPane.showMessageDialog(
            this,
            "Failed to download or install the update:\n" + e.getMessage(),
            "Update Error",
            JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Download update with progress tracking
     */
    private void downloadWithProgress(String version, File destinationFile) throws IOException {
        String downloadUrl = "https://github.com/nsomatrix/JarEngine/releases/download/v" + version + "/JarEngine-" + version + ".jar";
        
        URL url = new URL(downloadUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("User-Agent", "JarEngine-Updater/1.0");

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to download update. HTTP error code: " + responseCode + 
                               " for URL: " + downloadUrl);
        }

        int contentLength = connection.getContentLength();
        if (contentLength <= 0) {
            // If content length unknown, use indeterminate progress
            SwingUtilities.invokeLater(() -> {
                progressBar.setIndeterminate(true);
                statusLabel.setText("Downloading update...");
            });
        }

        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(destinationFile)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            int totalBytesRead = 0;
            
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                
                if (contentLength > 0) {
                    final int progress = (int) ((totalBytesRead * 100.0) / contentLength);
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(progress);
                        statusLabel.setText(String.format("Downloading update... %d%%", progress));
                    });
                }
            }
        }
        
        // Verify the downloaded file
        if (!destinationFile.exists() || destinationFile.length() == 0) {
            throw new IOException("Downloaded file is invalid or empty");
        }
    }


    

    


    private JPanel createExpandedSettingsPanel() {
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Automatic Updates"));

        // Auto-check setting
        JPanel autoCheckPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        autoCheckPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        autoCheckPanel.add(autoCheckBox);
        settingsPanel.add(autoCheckPanel);

        // Interval setting
        JPanel intervalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        intervalPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        intervalPanel.add(new JLabel("Check frequency:"));
        intervalPanel.add(Box.createHorizontalStrut(10));
        intervalPanel.add(intervalComboBox);
        intervalComboBox.setEnabled(autoCheckBox.isSelected());
        settingsPanel.add(intervalPanel);

        // Status display
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        statusPanel.add(autoUpdateStatusLabel);
        settingsPanel.add(statusPanel);

        // "Remind me later" info and controls
        if (UpdateConfig.isRemindMeLater()) {
            JPanel reminderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            reminderPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            reminderPanel.add(new JLabel("Reminder set for next update check"));
            reminderPanel.add(Box.createHorizontalStrut(15));
            JButton clearReminderButton = new JButton("Clear Reminder");
            styleButton(clearReminderButton, "Remove the reminder for this update");
            clearReminderButton.addActionListener(e -> {
                UpdateConfig.clearReminder();
                // Refresh the status display
                updateAutoUpdateStatus();
            });
            reminderPanel.add(clearReminderButton);
            settingsPanel.add(reminderPanel);
        }

        return settingsPanel;
    }

    private void updateAutoUpdateStatus() {
        if (autoUpdateStatusLabel != null) {
            autoUpdateStatusLabel.setText(AutoUpdateChecker.getInstance().getStatusInfo());
        }
    }
    
    /**
     * Update the latest version display
     */
    private void updateLatestVersionDisplay(String version) {
        if (latestVersionLabel != null) {
            latestVersionLabel.setText(version);
        }
    }
}