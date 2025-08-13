package org.je.app.util;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 * SelfDestructManager handles the self-destruct functionality for the emulator.
 * Provides timer-based graceful termination with configurable countdown.
 *
 * This class is designed to be completely isolated from core emulator functionality
 * and will not interfere with normal emulator operation.
 */
public class SelfDestructManager {

    private static final String DEFAULT_TIMER = "00:00:05:00"; // 5 minutes default
    private static final long MINUTE_MS = 60 * 1000L;
    private static final long HOUR_MS = 60 * MINUTE_MS;
    private static final long DAY_MS = 24 * HOUR_MS;

    private final JFrame parentFrame;
    private final Timer countdownTimer;
    private final AtomicBoolean isActive = new AtomicBoolean(false);
    private final AtomicBoolean isConfigured = new AtomicBoolean(false);

    private long countdownEndTime = 0;
    private long countdownDuration = 0;
    private JDialog configDialog = null;
    private JDialog deactivationDialog = null;
    private Timer liveUpdateTimer = null;

    /**
     * Creates a new SelfDestructManager instance.
     *
     * @param parentFrame The parent frame for dialogs
     */
    public SelfDestructManager(JFrame parentFrame) {
        if (parentFrame == null) {
            throw new IllegalArgumentException("Parent frame cannot be null");
        }
        this.parentFrame = parentFrame;
        this.countdownTimer = new Timer("SelfDestructTimer", true);
    }

    /**
     * Shows the self-destruct configuration dialog
     */
    public void showConfigDialog() {
        if (configDialog != null && configDialog.isVisible()) {
            configDialog.toFront();
            return;
        }

        configDialog = new JDialog(parentFrame, "Self-Destruct Configuration", true);
        configDialog.setLayout(new BorderLayout());
        configDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel titleLabel = new JLabel("Set Self-Destruct Timer");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);

        // Timer format explanation
        JLabel formatLabel = new JLabel("Format: DD:HH:MM:SS (Days:Hours:Minutes:Seconds)");
        formatLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        gbc.gridy = 1;
        gbc.insets = new Insets(2, 5, 10, 5);
        mainPanel.add(formatLabel, gbc);

        // Timer input
        JLabel timerLabel = new JLabel("Timer:");
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        mainPanel.add(timerLabel, gbc);

        JTextField timerField = new JTextField(DEFAULT_TIMER, 15);
        timerField.setToolTipText("Enter timer in DD:HH:MM:SS format");
        gbc.gridx = 1;
        gbc.gridy = 2;
        mainPanel.add(timerField, gbc);

        // Examples
        JLabel exampleLabel = new JLabel("Examples: 00:01:30:00 = 1h 30m, 01:00:00:00 = 1 day");
        exampleLabel.setFont(new Font("Arial", Font.PLAIN, 9));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(2, 5, 15, 5);
        mainPanel.add(exampleLabel, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton okButton = new JButton("Activate");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String timerText = timerField.getText().trim();
                if (parseAndValidateTimer(timerText)) {
                    activateSelfDestruct(countdownDuration);
                    configDialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(configDialog,
                        "Invalid timer format. Please use DD:HH:MM:SS format.\n" +
                        "Example: 00:01:30:00 for 1 hour 30 minutes",
                        "Invalid Format",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> configDialog.dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 5, 5, 5);
        mainPanel.add(buttonPanel, gbc);

        configDialog.add(mainPanel, BorderLayout.CENTER);
        configDialog.pack();
        configDialog.setLocationRelativeTo(parentFrame);
        configDialog.setResizable(false);
        configDialog.setVisible(true);
    }

    /**
     * Activates the self-destruct timer (background process)
     *
     * @param durationMs Duration in milliseconds
     */
    public void activateSelfDestruct(long durationMs) {
        if (durationMs <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }

        if (isActive.get()) {
            deactivateSelfDestruct();
        }

        this.countdownDuration = durationMs;
        this.countdownEndTime = System.currentTimeMillis() + durationMs;
        this.isActive.set(true);
        this.isConfigured.set(true);

        // Start the countdown timer (background process)
        startCountdown();
    }

    /**
     * Deactivates the self-destruct timer
     */
    public void deactivateSelfDestruct() {
        if (!isActive.get()) {
            // If not active, show message that no timer is running
            JOptionPane.showMessageDialog(parentFrame,
                "No self-destruct timer is currently active.",
                "Self-Destruct Status",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // If active, show the deactivation dialog with timer info
        showDeactivationDialog();
    }

    /**
     * Shows deactivation dialog with current timer information
     */
    private void showDeactivationDialog() {
        if (deactivationDialog != null && deactivationDialog.isVisible()) {
            deactivationDialog.toFront();
            return;
        }

        deactivationDialog = new JDialog(parentFrame, "Self-Destruct Status", true);
        deactivationDialog.setLayout(new BorderLayout());
        deactivationDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);

        // Status icon
        JLabel statusIcon = new JLabel("⚠️");
        statusIcon.setFont(new Font("Dialog", Font.BOLD, 24));
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(statusIcon, gbc);

        // Title
        JLabel titleLabel = new JLabel("Self-Destruct Timer Active");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 1;
        gbc.gridy = 0;
        mainPanel.add(titleLabel, gbc);

        // Timer info
        JLabel timerInfoLabel = new JLabel("Time Remaining:");
        timerInfoLabel.setFont(new Font("Arial", Font.BOLD, 12));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 15, 5, 15);
        mainPanel.add(timerInfoLabel, gbc);

        // Countdown label (will be updated live)
        JLabel countdownLabel = new JLabel(getFormattedRemainingTime());
        countdownLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        countdownLabel.setForeground(Color.RED);
        gbc.gridy = 2;
        gbc.insets = new Insets(5, 15, 15, 15);
        mainPanel.add(countdownLabel, gbc);

        // Deactivate button
        JButton deactivateButton = new JButton("Deactivate Self-Destruct");
        deactivateButton.addActionListener(e -> {
            if (isActive.get()) {
                if (countdownTimer != null) {
                    countdownTimer.cancel();
                }
                this.countdownEndTime = 0;
                this.countdownDuration = 0;
                this.isActive.set(false);
            }
            deactivationDialog.dispose();
        });

        gbc.gridy = 3;
        gbc.insets = new Insets(10, 15, 10, 15);
        mainPanel.add(deactivateButton, gbc);

        deactivationDialog.add(mainPanel, BorderLayout.CENTER);
        deactivationDialog.pack();
        deactivationDialog.setLocationRelativeTo(parentFrame);
        deactivationDialog.setResizable(false);

        // Start live timer updates for this dialog
        startLiveTimerUpdates(countdownLabel);

        deactivationDialog.setVisible(true);
    }

    /**
     * Starts live timer updates for the deactivation dialog
     */
    private void startLiveTimerUpdates(JLabel countdownLabel) {
        // Cancel any existing live update timer
        if (liveUpdateTimer != null) {
            liveUpdateTimer.cancel();
        }

        liveUpdateTimer = new Timer("LiveUpdateTimer", true);
        liveUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    if (deactivationDialog != null && deactivationDialog.isVisible() && isActive.get()) {
                        String remainingTime = getFormattedRemainingTime();
                        countdownLabel.setText(remainingTime);

                        // Add blinking effect when time is running low (less than 1 minute)
                        long remainingMs = getRemainingTime();
                        if (remainingMs < MINUTE_MS) { // Less than 1 minute
                            countdownLabel.setForeground(countdownLabel.getForeground() == Color.RED ? Color.BLACK : Color.RED);
                        } else {
                            countdownLabel.setForeground(Color.RED);
                        }
                    } else {
                        // Stop updating if dialog is closed or timer is inactive
                        cancel();
                    }
                });
            }
        }, 0, 500); // Update every 500ms for smoother visual feedback

        // Store the timer reference for cleanup
        deactivationDialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                if (liveUpdateTimer != null) {
                    liveUpdateTimer.cancel();
                    liveUpdateTimer = null;
                }
            }
        });
    }

    /**
     * Checks if self-destruct is currently active
     *
     * @return true if active, false otherwise
     */
    public boolean isActive() {
        return isActive.get();
    }

    /**
     * Checks if self-destruct is configured
     *
     * @return true if configured, false otherwise
     */
    public boolean isConfigured() {
        return isConfigured.get();
    }

    /**
     * Gets the remaining time in milliseconds
     *
     * @return Remaining time in milliseconds, or 0 if not active
     */
    public long getRemainingTime() {
        if (!isActive.get()) {
            return 0;
        }
        return Math.max(0, countdownEndTime - System.currentTimeMillis());
    }

    /**
     * Gets the formatted remaining time string
     *
     * @return Formatted time string in DD:HH:MM:SS format
     */
    public String getFormattedRemainingTime() {
        long remaining = getRemainingTime();
        if (remaining <= 0) {
            return "00:00:00:00";
        }

        long days = remaining / DAY_MS;
        remaining %= DAY_MS;
        long hours = remaining / HOUR_MS;
        remaining %= HOUR_MS;
        long minutes = remaining / MINUTE_MS;
        remaining %= MINUTE_MS;
        long seconds = remaining / 1000;

        return String.format("%02d:%02d:%02d:%02d", days, hours, minutes, seconds);
    }

    /**
     * Parses and validates timer string in DD:HH:MM:SS format
     *
     * @param timerText Timer string to parse
     * @return true if valid, false otherwise
     */
    private boolean parseAndValidateTimer(String timerText) {
        if (timerText == null || timerText.trim().isEmpty()) {
            return false;
        }

        String[] parts = timerText.trim().split(":");
        if (parts.length != 4) {
            return false;
        }

        try {
            int days = Integer.parseInt(parts[0]);
            int hours = Integer.parseInt(parts[1]);
            int minutes = Integer.parseInt(parts[2]);
            int seconds = Integer.parseInt(parts[3]);

            if (days < 0 || hours < 0 || minutes < 0 || seconds < 0 ||
                hours > 23 || minutes > 59 || seconds > 59) {
                return false;
            }

            countdownDuration = (days * DAY_MS) + (hours * HOUR_MS) + (minutes * MINUTE_MS) + (seconds * 1000L);

            if (countdownDuration <= 0) {
                return false;
            }

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Starts the countdown timer (background process)
     */
    private void startCountdown() {
        countdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isActive.get()) {
                    cancel();
                    return;
                }

                long remaining = getRemainingTime();
                if (remaining <= 0) {
                    // Timer expired - terminate immediately without warning
                    SwingUtilities.invokeLater(() -> gracefullyTerminate());
                    cancel();
                }
            }
        }, 0, 1000); // Check every second
    }

    /**
     * Gracefully terminates the emulator (background process)
     */
    private void gracefullyTerminate() {
        // Close dialogs if open
        if (configDialog != null && configDialog.isVisible()) {
            configDialog.dispose();
        }
        if (deactivationDialog != null && deactivationDialog.isVisible()) {
            deactivationDialog.dispose();
        }

        // Exit the application immediately
        System.exit(0);
    }

    /**
     * Cleanup resources. This method should be called when the manager is no longer needed.
     */
    public void cleanup() {
        deactivateSelfDestruct();

        if (countdownTimer != null) {
            countdownTimer.cancel();
        }

        if (liveUpdateTimer != null) {
            liveUpdateTimer.cancel();
            liveUpdateTimer = null;
        }

        if (configDialog != null && configDialog.isVisible()) {
            configDialog.dispose();
        }

        if (deactivationDialog != null && deactivationDialog.isVisible()) {
            deactivationDialog.dispose();
        }
    }
}
