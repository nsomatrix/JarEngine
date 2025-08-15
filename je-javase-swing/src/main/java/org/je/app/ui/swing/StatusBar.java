package org.je.app.ui.swing;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Swing status bar component with centralized behavior.
 * - Receives direct app status updates
 * - Appends proxy status when enabled
 * - Supports temporary messages (auto-restore after delay)
 * - Features animated braille spinner for configuration operations
 */
public class StatusBar extends JPanel {

    private final JLabel spinnerLabel;
    private final JLabel label;
    private javax.swing.Timer restoreTimer;
    private javax.swing.Timer spinnerTimer;
    private String persistentText = "Status";
    private boolean includeProxySuffix = true;
    
    // Braille spinner animation frames
    private static final String[] BRAILLE_FRAMES = {
        "⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"
    };
    private int currentFrame = 0;
    private boolean spinnerActive = false;

    public StatusBar() {
        super(new BorderLayout());
        
        // Create spinner label for left side
        this.spinnerLabel = new JLabel(" ");
        this.spinnerLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        this.spinnerLabel.setVisible(false);
        
        // Create main status label
        this.label = new JLabel(persistentText);

        // Prevent minimum-width issues and ensure consistent height
        try {
            int barHeight = Math.max(1, label.getPreferredSize().height);
            label.setMinimumSize(new Dimension(0, barHeight));
            spinnerLabel.setMinimumSize(new Dimension(20, barHeight));
            this.setMinimumSize(new Dimension(0, barHeight));
            this.setPreferredSize(new Dimension(1, barHeight));
        } catch (Exception ignore) {
        }

        // Layout: spinner on far left, status text in center-left
        setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(spinnerLabel, BorderLayout.WEST);
        leftPanel.add(label, BorderLayout.CENTER);
        
        add(leftPanel, BorderLayout.WEST);
        
        // Initialize spinner timer (not started yet)
        spinnerTimer = new javax.swing.Timer(80, e -> updateSpinner());
        spinnerTimer.setRepeats(true);
    }

    public JComponent getComponent() {
        return this;
    }

    /** Toggle whether proxy info is appended to messages. */
    public void setIncludeProxySuffix(boolean enabled) {
        this.includeProxySuffix = enabled;
    }

    /** Set the persistent status text (will be shown unless a temporary message is active). */
    public void setPersistentStatus(String text) {
        if (text == null) {
            text = "";
        }
        final String fullText = buildWithProxySuffix(text);
        this.persistentText = fullText;
        // Only update the label if no temporary message is currently displayed
        if (restoreTimer == null || !restoreTimer.isRunning()) {
            setLabelTextOnEdt(fullText);
        }
    }

    /** Show a temporary message for durationMs, then restore persistent text. */
    public void showTemporaryStatus(String text, int durationMs) {
        if (text == null) {
            text = "";
        }
        final String tempText = text;
        setLabelTextOnEdt(tempText);

        if (restoreTimer != null && restoreTimer.isRunning()) {
            restoreTimer.stop();
        }
        restoreTimer = new javax.swing.Timer(Math.max(0, durationMs), e -> {
            setLabelTextOnEdt(persistentText);
            restoreTimer.stop();
        });
        restoreTimer.setRepeats(false);
        restoreTimer.start();
    }

    /** Update status text - main entry point for app-wide status updates */
    public void statusBarChanged(String text) {
        // Central entry point for app-wide status updates
        setPersistentStatus(text);
    }

    /** Start the configuration saving spinner */
    public void startConfigSpinner() {
        if (!spinnerActive) {
            spinnerActive = true;
            currentFrame = 0;
            setSpinnerTextOnEdt(BRAILLE_FRAMES[currentFrame]);
            spinnerLabel.setVisible(true);
            if (!spinnerTimer.isRunning()) {
                spinnerTimer.start();
            }
        }
    }

    /** Stop the configuration saving spinner */
    public void stopConfigSpinner() {
        if (spinnerActive) {
            spinnerActive = false;
            if (spinnerTimer.isRunning()) {
                spinnerTimer.stop();
            }
            spinnerLabel.setVisible(false);
        }
    }

    /** Show spinner for a specified duration then auto-hide */
    public void showConfigSpinner(int durationMs) {
        startConfigSpinner();
        
        // Auto-stop after duration
        javax.swing.Timer autoStop = new javax.swing.Timer(durationMs, e -> stopConfigSpinner());
        autoStop.setRepeats(false);
        autoStop.start();
    }

    /** Update the spinner animation frame */
    private void updateSpinner() {
        if (spinnerActive) {
            currentFrame = (currentFrame + 1) % BRAILLE_FRAMES.length;
            setSpinnerTextOnEdt(BRAILLE_FRAMES[currentFrame]);
        }
    }

    private void setLabelTextOnEdt(String text) {
        if (SwingUtilities.isEventDispatchThread()) {
            label.setText(text);
        } else {
            SwingUtilities.invokeLater(() -> label.setText(text));
        }
    }

    private void setSpinnerTextOnEdt(String text) {
        if (SwingUtilities.isEventDispatchThread()) {
            spinnerLabel.setText(text);
        } else {
            SwingUtilities.invokeLater(() -> spinnerLabel.setText(text));
        }
    }

    private String buildWithProxySuffix(String base) {
        if (!includeProxySuffix) {
            return base;
        }
        try {
            org.je.util.ProxyConfig proxyConfig = org.je.util.ProxyConfig.getInstance();
            if (proxyConfig.isEnabled()) {
                return base + " [PROXY: " + proxyConfig.getProxyHost() + ":" + proxyConfig.getProxyPort() + "]";
            }
        } catch (Exception ignored) {
        }
        return base;
    }
}
