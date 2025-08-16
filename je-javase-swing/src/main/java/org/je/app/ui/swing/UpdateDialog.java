package org.je.app.ui.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

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
    private JButton settingsButton;
    
    // Auto-update settings components
    private JCheckBox autoCheckBox;
    private JComboBox<String> intervalComboBox;
    private JLabel autoUpdateStatusLabel;
    
    private String currentVersion;
    private String latestVersion;
    private boolean updateAvailable = false;
    private boolean settingsExpanded = false;

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
        settingsButton = new JButton("Settings");
        
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

        // Version info panel
        JPanel versionPanel = new JPanel(new GridLayout(2, 2));
        versionPanel.add(new JLabel("Current Version:"));
        versionPanel.add(new JLabel(currentVersion));
        versionPanel.add(new JLabel("Latest Version:"));
        versionPanel.add(new JLabel("Unknown"));

        contentPanel.add(versionPanel);
        contentPanel.add(Box.createVerticalStrut(5));

        // Status and progress panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(statusLabel, BorderLayout.CENTER);
        statusPanel.add(progressBar, BorderLayout.SOUTH);
        contentPanel.add(statusPanel);

        // Auto-update settings panel (initially hidden)
        createSettingsPanel();

        add(contentPanel, BorderLayout.CENTER);

        // Button panel - use natural Swing spacing
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(checkButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(settingsButton);
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void createSettingsPanel() {
        // This will be added to the main content panel when settings are expanded
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
        
        settingsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleSettings();
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
        JPanel contentPanel = (JPanel) getContentPane().getComponent(0);
        JPanel versionPanel = (JPanel) contentPanel.getComponent(0);
        JLabel latestVersionLabel = (JLabel) versionPanel.getComponent(3);
        latestVersionLabel.setText(latestVersion != null ? latestVersion : "Unknown");
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
        
        // Run download in background
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    // Create temporary file for download
                    File tempFile = File.createTempFile("JarEngine-update-", ".jar");
                    tempFile.deleteOnExit();
                    
                    // Download the update
                    UpdateChecker.downloadUpdate(latestVersion, tempFile);
                    
                    // Show success message
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            statusLabel.setText("Update downloaded successfully. Installing...");
                            progressBar.setIndeterminate(false);
                            progressBar.setValue(100);
                        }
                    });
                    
                    // Wait a moment for user to see the message, then apply update
                    Thread.sleep(2000);
                    
                    // Apply the update (this will restart the application)
                    UpdateChecker.applyUpdateAndRestart(tempFile, latestVersion);
                    
                } catch (Exception e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            handleUpdateError(e);
                        }
                    });
                }
            }
        });
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

    private void toggleSettings() {
        settingsExpanded = !settingsExpanded;
        
        JPanel contentPanel = (JPanel) getContentPane().getComponent(0);
        
        if (settingsExpanded) {
            // Add settings panel
            JPanel settingsPanel = createExpandedSettingsPanel();
            contentPanel.add(Box.createVerticalStrut(10));
            contentPanel.add(new JSeparator());
            contentPanel.add(Box.createVerticalStrut(10));
            contentPanel.add(settingsPanel);
            settingsButton.setText("Hide Settings");
        } else {
            // Remove settings panel
            int componentCount = contentPanel.getComponentCount();
            if (componentCount >= 7) { // version panel, strut, status panel, strut, separator, strut, settings panel
                contentPanel.remove(componentCount - 1); // settings panel
                contentPanel.remove(componentCount - 2); // strut
                contentPanel.remove(componentCount - 3); // separator
                contentPanel.remove(componentCount - 4); // strut
            }
            settingsButton.setText("Settings");
        }
        
        pack();
        setLocationRelativeTo(getParent());
    }

    private JPanel createExpandedSettingsPanel() {
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Automatic Updates"));

        // Auto-check setting
        JPanel autoCheckPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        autoCheckPanel.add(autoCheckBox);
        settingsPanel.add(autoCheckPanel);

        // Interval setting
        JPanel intervalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        intervalPanel.add(new JLabel("Check frequency:"));
        intervalPanel.add(intervalComboBox);
        intervalComboBox.setEnabled(autoCheckBox.isSelected());
        settingsPanel.add(intervalPanel);

        // Status display
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(autoUpdateStatusLabel);
        settingsPanel.add(statusPanel);

        // "Remind me later" info and controls
        if (UpdateConfig.isRemindMeLater()) {
            JPanel reminderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            reminderPanel.add(new JLabel("Reminder set for next update check"));
            JButton clearReminderButton = new JButton("Clear Reminder");
            clearReminderButton.addActionListener(e -> {
                UpdateConfig.clearReminder();
                toggleSettings(); // Refresh the panel
                toggleSettings();
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
}