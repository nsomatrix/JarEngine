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
    private final NetworkMeter networkMeter;
    private final JLabel runtimeLabel;
    private javax.swing.Timer restoreTimer;
    private javax.swing.Timer spinnerTimer;
    private javax.swing.Timer runtimeTimer;
    private String persistentText = "Status";
    private boolean includeProxySuffix = true;
    private long midletStartTime = 0;
    private boolean midletRunning = false;
    
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
        
        // Create runtime timer label for far right
        this.runtimeLabel = new JLabel("");
        this.runtimeLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        this.runtimeLabel.setVisible(false);
        
        // Create network meter for right side
        this.networkMeter = new NetworkMeter();

        // Prevent minimum-width issues and ensure consistent height
        try {
            int barHeight = Math.max(1, label.getPreferredSize().height);
            label.setMinimumSize(new Dimension(0, barHeight));
            spinnerLabel.setMinimumSize(new Dimension(20, barHeight));
            runtimeLabel.setMinimumSize(new Dimension(0, barHeight));
            networkMeter.setMinimumSize(new Dimension(0, barHeight));
            this.setMinimumSize(new Dimension(0, barHeight));
            this.setPreferredSize(new Dimension(1, barHeight));
        } catch (Exception ignore) {
        }

        // Layout: runtime timer on far left, spinner next to it, status text in center, network meter on very right
        setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(runtimeLabel, BorderLayout.WEST);
        leftPanel.add(spinnerLabel, BorderLayout.CENTER);
        leftPanel.add(label, BorderLayout.EAST);
        
        add(leftPanel, BorderLayout.WEST);
        add(networkMeter, BorderLayout.EAST);
        
        // Initialize spinner timer (not started yet)
        spinnerTimer = new javax.swing.Timer(80, e -> updateSpinner());
        spinnerTimer.setRepeats(true);
        
        // Initialize runtime timer (updates every second)
        runtimeTimer = new javax.swing.Timer(1000, e -> updateRuntimeDisplay());
        runtimeTimer.setRepeats(true);
    }

    public JComponent getComponent() {
        return this;
    }
    
    /**
     * Get the network meter component for direct access if needed
     */
    public NetworkMeter getNetworkMeter() {
        return networkMeter;
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

    // ========== Menu Action Status Messages ==========
    
    /** Show status message for file loading and start MIDlet timer */
    public void showFileLoaded(String filename) {
        startMidletTimer();
        showTemporaryStatus("Loading JAR: " + filename, 3000);
    }
    
    /** Show status message for URL loading and start MIDlet timer */
    public void showUrlLoaded(String url) {
        startMidletTimer();
        showTemporaryStatus("Loading from URL: " + url, 3000);
    }
    
    /** Show status message for recording start */
    public void showRecordingStarted(String filename) {
        showTemporaryStatus("Recording started: " + filename, 3000);
    }
    
    /** Show status message for recording stop */
    public void showRecordingStopped() {
        showTemporaryStatus("Recording stopped and saved", 3000);
    }
    
    /** Show status message for log console toggle */
    public void showLogConsoleToggled(boolean isVisible) {
        String action = isVisible ? "opened" : "closed";
        showTemporaryStatus("Log console " + action, 2000);
    }
    
    /** Show status message for record store manager toggle */
    public void showRecordStoreManagerToggled(boolean isVisible) {
        String action = isVisible ? "opened" : "closed";
        showTemporaryStatus("Record Store Manager " + action, 2500);
    }
    
    /** Show status message for about dialog */
    public void showAboutDialogOpened() {
        showTemporaryStatus("About dialog opened", 2000);
    }
    
    /** Show status message for theme change */
    public void showThemeApplied(String themeKey) {
        String themeName = themeKey.replace("-", " ");
        if (!themeName.isEmpty()) {
            themeName = themeName.substring(0, 1).toUpperCase() + themeName.substring(1);
        }
        showTemporaryStatus(themeName + " theme applied", 2500);
    }
    
    /** Show status message for network connection toggle */
    public void showNetworkToggled(boolean enabled) {
        String status = enabled ? "enabled" : "disabled";
        showTemporaryStatus("Network connection " + status, 2500);
    }
    
    /** Show status message for device resize */
    public void showDeviceResized(int width, int height) {
        showTemporaryStatus("Device resized to " + width + "x" + height, 2500);
    }
    
    /** Show status message for sleep mode toggle */
    public void showSleepModeToggled(boolean enabled) {
        String status = enabled ? "enabled" : "disabled";
        showTemporaryStatus("Sleep mode " + status, 2000);
    }
    
    /** Show status message for general dialog toggle */
    public void showDialogToggled(String dialogName, boolean isVisible) {
        String action = isVisible ? "opened" : "closed";
        showTemporaryStatus(dialogName + " " + action, 2000);
    }
    
    /** Show status message for menu item selection */
    public void showMenuItemSelected(String itemName) {
        showTemporaryStatus(itemName + " selected", 1500);
    }

    // ========== MIDlet Runtime Timer ==========
    
    /** Start the MIDlet runtime timer */
    public void startMidletTimer() {
        if (!midletRunning) {
            midletRunning = true;
            midletStartTime = System.currentTimeMillis();
            setRuntimeLabelVisibleOnEdt(true);
            if (!runtimeTimer.isRunning()) {
                runtimeTimer.start();
            }
        }
    }
    
    /** Stop the MIDlet runtime timer and hide the display */
    public void stopMidletTimer() {
        if (midletRunning) {
            midletRunning = false;
            if (runtimeTimer.isRunning()) {
                runtimeTimer.stop();
            }
            setRuntimeLabelVisibleOnEdt(false);
            midletStartTime = 0;
        }
    }
    
    /** Update the runtime display with formatted elapsed time */
    private void updateRuntimeDisplay() {
        if (midletRunning && midletStartTime > 0) {
            long elapsedMs = System.currentTimeMillis() - midletStartTime;
            String formattedTime = formatElapsedTime(elapsedMs);
            setRuntimeTextOnEdt(formattedTime);
        }
    }
    
    /** Format elapsed time as 1s, 24min23s, 1h23min23s */
    private String formatElapsedTime(long elapsedMs) {
        long seconds = elapsedMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        seconds = seconds % 60;
        minutes = minutes % 60;
        
        if (hours > 0) {
            return String.format("%dh%02dmin%02ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dmin%02ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    /** Show status message for MIDlet closing and stop the timer */
    public void showMidletClosed() {
        stopMidletTimer();
        showTemporaryStatus("MIDlet closed - returned to launcher", 2500);
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
    
    private void setRuntimeTextOnEdt(String text) {
        if (SwingUtilities.isEventDispatchThread()) {
            runtimeLabel.setText(text);
        } else {
            SwingUtilities.invokeLater(() -> runtimeLabel.setText(text));
        }
    }
    
    private void setRuntimeLabelVisibleOnEdt(boolean visible) {
        if (SwingUtilities.isEventDispatchThread()) {
            runtimeLabel.setVisible(visible);
        } else {
            SwingUtilities.invokeLater(() -> runtimeLabel.setVisible(visible));
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
    
    @Override
    public void removeNotify() {
        // Ensure proper cleanup of timers and resources
        if (restoreTimer != null && restoreTimer.isRunning()) {
            restoreTimer.stop();
        }
        if (spinnerTimer != null && spinnerTimer.isRunning()) {
            spinnerTimer.stop();
        }
        if (runtimeTimer != null && runtimeTimer.isRunning()) {
            runtimeTimer.stop();
        }
        super.removeNotify();
    }
}
