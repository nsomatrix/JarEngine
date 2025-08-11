package org.je.app.ui.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import org.je.app.UpdateChecker;
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

        add(contentPanel, BorderLayout.CENTER);

        // Button panel - use natural Swing spacing
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(checkButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
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
        
        int result = JOptionPane.showConfirmDialog(
            this,
            "This will download and install version " + latestVersion + ".\n" +
            "The application will restart automatically.\n\n" +
            "Do you want to continue?",
            "Confirm Update",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        
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
}