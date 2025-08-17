package org.je.app.ui.swing;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import org.je.util.NetEventBus;

/**
 * Swing status bar component with centralized behavior.
 * - Receives direct app status updates
 * - Appends proxy status when enabled
 * - Supports temporary messages (auto-restore after delay)
 * - Features animated braille spinner for configuration operations
 * - Includes integrated network activity meter with auto-show/hide
 * - Displays MIDlet runtime timer
 */
public class StatusBar extends JPanel {

    private final JLabel spinnerLabel;
    private final JLabel label;
    private final JLabel networkMeterLabel;
    private final JLabel runtimeLabel;
    private javax.swing.Timer restoreTimer;
    private javax.swing.Timer spinnerTimer;
    private javax.swing.Timer runtimeTimer;
    private javax.swing.Timer networkUpdateTimer;
    private javax.swing.Timer networkHideTimer;
    private String persistentText = "";
    private boolean includeProxySuffix = true;
    private long midletStartTime = 0;
    private boolean midletRunning = false;
    
    // Status indicator fields
    private JLabel statusIndicator;
    private Timer blinkTimer;
    private boolean blinkState = false;
    private float alpha = 1.0f; // For fade effect
    private boolean fadeDirection = false; // true = fade out, false = fade in
    private static final Color GREEN_COLOR = new Color(0, 200, 0);
    private static final Color RED_COLOR = new Color(200, 0, 0);
    
    // Network meter fields
    private static final int NETWORK_UPDATE_INTERVAL_MS = 1000; // Update every second
    private static final int NETWORK_HIDE_DELAY_MS = 3000; // Hide after 3 seconds of inactivity
    private static final int NETWORK_ACTIVITY_WINDOW_MS = 2000; // Consider events from last 2 seconds
    private final AtomicLong lastEventCount = new AtomicLong(0);
    private final AtomicLong lastNetworkUpdateTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicReference<String> currentSpeedText = new AtomicReference<>("");
    private volatile boolean networkMeterVisible = false;
    
    // UI control flags
    private volatile boolean updatesEnabled = true; // Controls status message updates
    private volatile boolean timerEnabled = true;   // Controls runtime timer display
    private volatile boolean networkMeterEnabled = true; // Controls network meter display
    
    // Braille spinner animation frames
    private static final String[] BRAILLE_FRAMES = {
        "⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"
    };
    private int currentFrame = 0;
    private boolean spinnerActive = false;

    public StatusBar() {
        super(new BorderLayout());
        
        // Ensure proper background matching
        setOpaque(true);
        
        // Create spinner label with gradient styling (purple to blue)
        this.spinnerLabel = new JLabel(" ") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Create gradient from purple to blue (your logo colors)
                Color purple = new Color(155, 89, 182); // Purple
                Color blue = new Color(52, 152, 219);   // Blue
                
                GradientPaint gradient = new GradientPaint(
                    0, 0, purple,
                    getWidth(), getHeight(), blue
                );
                
                g2d.setPaint(gradient);
                g2d.setFont(getFont());
                
                // Draw the text with gradient
                String text = getText();
                if (text != null && !text.trim().isEmpty()) {
                    FontMetrics fm = g2d.getFontMetrics();
                    int x = (getWidth() - fm.stringWidth(text)) / 2;
                    int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                    g2d.drawString(text, x, y);
                }
                
                g2d.dispose();
            }
        };
        // Use relative font sizing based on system defaults
        Font baseFont = UIManager.getFont("Label.font");
        this.spinnerLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, Math.max(10, baseFont.getSize())));
        this.spinnerLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        this.spinnerLabel.setToolTipText("Processing Activity");
        
        // Create main status label with professional styling
        this.label = new JLabel(persistentText);
        this.label.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        
        // Create runtime timer label with professional styling
        this.runtimeLabel = new JLabel("");
        this.runtimeLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(9, baseFont.getSize() - 1)));
        this.runtimeLabel.setForeground(new Color(41, 128, 185)); // Professional blue
        this.runtimeLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        this.runtimeLabel.setVisible(false);
        this.runtimeLabel.setToolTipText("MIDlet Runtime");
        
        // Create integrated network meter label with professional styling
        this.networkMeterLabel = new JLabel("");
        this.networkMeterLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(9, baseFont.getSize() - 1)));
        this.networkMeterLabel.setForeground(new Color(39, 174, 96)); // Professional green
        this.networkMeterLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        this.networkMeterLabel.setVisible(false);
        this.networkMeterLabel.setToolTipText("Network Activity");

        // Set professional minimum sizes and prevent overlap
        try {
            int barHeight = Math.max(20, label.getPreferredSize().height + 4);
            
            // Status indicator section (fixed width)
            statusIndicator.setPreferredSize(new Dimension(20, barHeight));
            statusIndicator.setMinimumSize(new Dimension(20, barHeight));
            
            // Runtime timer (dynamic width with minimum)
            runtimeLabel.setMinimumSize(new Dimension(60, barHeight));
            runtimeLabel.setPreferredSize(new Dimension(80, barHeight));
            
            // Spinner (fixed small width)
            spinnerLabel.setMinimumSize(new Dimension(20, barHeight));
            spinnerLabel.setPreferredSize(new Dimension(25, barHeight));
            
            // Main status label (flexible)
            label.setMinimumSize(new Dimension(100, barHeight));
            
            // Network meter (dynamic width with minimum)
            networkMeterLabel.setMinimumSize(new Dimension(60, barHeight));
            networkMeterLabel.setPreferredSize(new Dimension(80, barHeight));
            
            // Overall status bar
            this.setMinimumSize(new Dimension(300, barHeight));
            this.setPreferredSize(new Dimension(500, barHeight));
        } catch (Exception ignore) {
        }

        // Clean professional layout without visual artifacts
        setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        
        // Initialize status indicator (circle)
        statusIndicator = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int size = Math.min(getWidth(), getHeight()) - 4;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                
                if (midletRunning) {
                    // Professional green with smooth fade effect
                    Color baseGreen = new Color(46, 204, 113); // Professional green
                    Color fadeGreen = new Color(baseGreen.getRed(), baseGreen.getGreen(), baseGreen.getBlue(), 
                                               Math.round(255 * alpha));
                    g2d.setColor(fadeGreen);
                    g2d.fillOval(x, y, size, size);
                    
                    // Add subtle inner highlight for depth
                    Color glowColor = new Color(255, 255, 255, Math.round(80 * alpha));
                    g2d.setColor(glowColor);
                    g2d.fillOval(x + size/3, y + size/3, size/3, size/3);
                    
                    // Professional border
                    g2d.setColor(new Color(39, 174, 96, Math.round(255 * alpha)));
                    g2d.setStroke(new BasicStroke(1.0f));
                    g2d.drawOval(x, y, size, size);
                } else {
                    // Professional muted red for inactive state
                    g2d.setColor(new Color(231, 76, 60, 180)); // Semi-transparent professional red
                    g2d.fillOval(x, y, size, size);
                    
                    // Subtle border for inactive state
                    g2d.setColor(new Color(192, 57, 43, 120));
                    g2d.setStroke(new BasicStroke(1.0f));
                    g2d.drawOval(x, y, size, size);
                }
                
                g2d.dispose();
            }
            
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(16, 16);
            }
        };
        statusIndicator.setToolTipText("MIDlet Status");
        
        // Initialize smooth fade blink timer for green indicator
        blinkTimer = new Timer(50, e -> {
            if (midletRunning) {
                // Smooth fade animation
                if (fadeDirection) {
                    // Fading out
                    alpha -= 0.08f;
                    if (alpha <= 0.3f) {
                        alpha = 0.3f;
                        fadeDirection = false; // Start fading in
                    }
                } else {
                    // Fading in
                    alpha += 0.08f;
                    if (alpha >= 1.0f) {
                        alpha = 1.0f;
                        fadeDirection = true; // Start fading out
                    }
                }
                statusIndicator.repaint();
            }
        });
        
        // Create a professional multi-section layout
        JPanel leftSection = new JPanel(new BorderLayout(4, 0));
        leftSection.add(statusIndicator, BorderLayout.WEST);
        leftSection.add(runtimeLabel, BorderLayout.CENTER);
        
        JPanel centerSection = new JPanel(new BorderLayout(4, 0));
        centerSection.add(spinnerLabel, BorderLayout.WEST);
        centerSection.add(label, BorderLayout.CENTER);
        
        JPanel rightSection = new JPanel(new BorderLayout());
        rightSection.add(networkMeterLabel, BorderLayout.CENTER);
        
        // Add subtle separators between sections - use natural height
        JPanel separatorLeft = new JPanel();
        separatorLeft.setPreferredSize(new Dimension(1, 0)); // Let height be natural
        separatorLeft.setBackground(Color.LIGHT_GRAY);
        separatorLeft.setOpaque(false);
        
        JPanel separatorRight = new JPanel();
        separatorRight.setPreferredSize(new Dimension(1, 0)); // Let height be natural
        separatorRight.setBackground(Color.LIGHT_GRAY);
        separatorRight.setOpaque(false);
        
        // Assemble the professional layout
        add(leftSection, BorderLayout.WEST);
        add(centerSection, BorderLayout.CENTER);
        add(rightSection, BorderLayout.EAST);
        
        // Initialize spinner timer (not started yet)
        spinnerTimer = new javax.swing.Timer(80, e -> updateSpinner());
        spinnerTimer.setRepeats(true);
        
        // Initialize runtime timer (updates every second)
        runtimeTimer = new javax.swing.Timer(1000, e -> updateRuntimeDisplay());
        runtimeTimer.setRepeats(true);
        
        // Initialize network meter timers
        networkUpdateTimer = new javax.swing.Timer(NETWORK_UPDATE_INTERVAL_MS, e -> updateNetworkStats());
        networkUpdateTimer.setRepeats(true);
        networkUpdateTimer.start();
        
        networkHideTimer = new javax.swing.Timer(NETWORK_HIDE_DELAY_MS, e -> hideNetworkMeter());
        networkHideTimer.setRepeats(false);
    }

    public JComponent getComponent() {
        return this;
    }
    
        /**
     * Get the network meter label (deprecated - network meter is now integrated)
     * @deprecated Network meter is now integrated into StatusBar
     */
    @Deprecated
    public JLabel getNetworkMeter() {
        return networkMeterLabel;
    }
    
    /** Toggle whether proxy info is appended to messages. */
    public void setIncludeProxySuffix(boolean enabled) {
        this.includeProxySuffix = enabled;
    }

    /** Set the persistent status text (will be shown unless a temporary message is active). */
    public void setPersistentStatus(String text) {
        if (!updatesEnabled) return; // Respect updates enabled flag
        
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
        if (!updatesEnabled) return; // Respect updates enabled flag
        
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
    
    /** Show status message for screenshot success */
    public void showScreenshotSaved(String path) {
        // Extract just the filename from the full path for a cleaner display
        String filename = path.substring(path.lastIndexOf(System.getProperty("file.separator")) + 1);
        showTemporaryStatus("Screenshot saved: " + filename, 4000);
    }
    
    /** Show status message for screenshot error */
    public void showScreenshotError() {
        showTemporaryStatus("Failed to save screenshot", 3000);
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
    
    /** Start the MIDlet runtime timer and show it */
    public void startMidletTimer() {
        synchronized (this) {
            // Always reset and restart the timer for fresh starts
            midletStartTime = System.currentTimeMillis();
            midletRunning = true; // Set the running flag
            
            // Clear initial placeholder text when MIDlet starts
            if ("Status".equals(persistentText)) {
                persistentText = "";
                setLabelTextOnEdt("");
            }
            
            // Reset and start professional fade animation
            alpha = 1.0f;
            fadeDirection = true; // Start by fading out
            if (!blinkTimer.isRunning()) {
                blinkTimer.start();
            }
            statusIndicator.repaint();
            
            // Only show the timer if it's enabled
            if (timerEnabled) {
                setRuntimeLabelVisibleOnEdt(true);
            }
            
            // Always restart the timer to ensure it's running
            if (runtimeTimer.isRunning()) {
                runtimeTimer.restart();
            } else {
                runtimeTimer.start();
            }
        }
    }

    /** Stop the MIDlet runtime timer and hide it */
    public void stopMidletTimer() {
        synchronized (this) {
            midletRunning = false; // Clear the running flag
            midletStartTime = 0;   // Reset start time
            
            // Stop fade animation and reset to solid red
            if (blinkTimer.isRunning()) {
                blinkTimer.stop();
            }
            alpha = 1.0f; // Reset to full opacity for red state
            statusIndicator.repaint();
            
            if (runtimeTimer.isRunning()) {
                runtimeTimer.stop();
            }
            
            // Always hide the timer when stopping
            setRuntimeLabelVisibleOnEdt(false);
        }
    }    /** Update the runtime display with formatted elapsed time */
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

    // ========== Integrated Network Meter ==========
    
    /**
     * Update network statistics by analyzing recent network events
     */
    private void updateNetworkStats() {
        if (!networkMeterEnabled) return; // Respect network meter enabled flag
        
        try {
            List<NetEventBus.NetEvent> events = NetEventBus.snapshot();
            long currentTime = System.currentTimeMillis();
            long eventCount = events.size();
            
            // Check if there are new events
            boolean hasNewEvents = eventCount > lastEventCount.get();
            lastEventCount.set(eventCount);
            
            // Calculate bandwidth from recent events using actual byte counts
            long bytesInThisPeriod = 0;
            long bytesOutThisPeriod = 0;
            long cutoffTime = currentTime - NETWORK_ACTIVITY_WINDOW_MS;
            
            // Only analyze recent events to avoid processing entire history
            // Start from a reasonable point to avoid scanning thousands of old events
            int startIndex = Math.max(0, (int)(eventCount - 100)); // Only check last 100 events max
            
            // Analyze recent events for bandwidth calculation
            for (int i = startIndex; i < eventCount; i++) {
                NetEventBus.NetEvent event = events.get(i);
                if (event.ts >= cutoffTime) {
                    long eventBytes = event.bytes > 0 ? event.bytes : estimateBytesFromEvent(event);
                    
                    if ("IN".equals(event.direction)) {
                        bytesInThisPeriod += eventBytes;
                    } else if ("OUT".equals(event.direction)) {
                        bytesOutThisPeriod += eventBytes;
                    }
                }
            }
            
            // Calculate speed (bytes per second)
            long timeDelta = currentTime - lastNetworkUpdateTime.get();
            if (timeDelta > 0) {
                double inSpeed = (bytesInThisPeriod * 1000.0) / timeDelta;
                double outSpeed = (bytesOutThisPeriod * 1000.0) / timeDelta;
                double totalSpeed = inSpeed + outSpeed;
                
                // Show meter if there's activity or new events
                if (hasNewEvents || totalSpeed > 0) {
                    showNetworkMeter();
                    updateNetworkSpeedDisplay(totalSpeed, inSpeed, outSpeed);
                    
                    // Reset hide timer
                    if (networkHideTimer.isRunning()) {
                        networkHideTimer.restart();
                    } else {
                        networkHideTimer.start();
                    }
                }
            }
            
            lastNetworkUpdateTime.set(currentTime);
            
        } catch (Exception ex) {
            // Silently handle any errors to avoid interfering with the UI
        }
    }
    
    /**
     * Estimate bytes from network event information
     */
    private long estimateBytesFromEvent(NetEventBus.NetEvent event) {
        if (event.info != null) {
            String info = event.info.toLowerCase();
            
            // Look for size indicators in the event info
            if (info.contains("kb") || info.contains("kilobyte")) {
                return 1024; // Assume 1KB
            } else if (info.contains("mb") || info.contains("megabyte")) {
                return 1024 * 1024; // Assume 1MB
            } else if (info.contains("byte")) {
                return 100; // Small transfer
            }
        }
        
        // Default estimates based on connection type
        switch (event.type) {
            case "HTTP":
            case "HTTPS":
                return 2048; // Assume 2KB for HTTP requests
            case "TCP":
                return 512;  // Assume 512B for TCP connections
            case "UDP":
                return 256;  // Assume 256B for UDP packets
            default:
                return 100;  // Small default
        }
    }
    
    /**
     * Update the network speed display with formatted text
     */
    private void updateNetworkSpeedDisplay(double totalSpeed, double inSpeed, double outSpeed) {
        String speedText = formatNetworkSpeed(totalSpeed);
        
        // Add direction indicators if there's significant directional traffic
        if (inSpeed > outSpeed * 2) {
            speedText = "↓ " + speedText;
        } else if (outSpeed > inSpeed * 2) {
            speedText = "↑ " + speedText;
        } else if (inSpeed > 0 && outSpeed > 0) {
            speedText = "↕ " + speedText;
        }
        
        final String finalSpeedText = speedText;
        currentSpeedText.set(finalSpeedText);
        
        SwingUtilities.invokeLater(() -> {
            networkMeterLabel.setText(finalSpeedText);
            networkMeterLabel.repaint();
        });
    }
    
    /**
     * Format speed in human-readable units
     */
    private String formatNetworkSpeed(double bytesPerSecond) {
        if (bytesPerSecond < 1024) {
            return String.format("%.0f B/s", bytesPerSecond);
        } else if (bytesPerSecond < 1024 * 1024) {
            return String.format("%.1f KB/s", bytesPerSecond / 1024.0);
        } else {
            return String.format("%.1f MB/s", bytesPerSecond / (1024.0 * 1024.0));
        }
    }
    
    /**
     * Show the network meter
     */
    private void showNetworkMeter() {
        if (!networkMeterEnabled) return; // Respect network meter enabled flag
        
        if (!networkMeterVisible) {
            networkMeterVisible = true;
            SwingUtilities.invokeLater(() -> {
                networkMeterLabel.setVisible(true);
                revalidate();
                repaint();
            });
        }
    }
    
    /**
     * Hide the network meter
     */
    private void hideNetworkMeter() {
        if (networkMeterVisible) {
            networkMeterVisible = false;
            SwingUtilities.invokeLater(() -> {
                networkMeterLabel.setVisible(false);
                revalidate();
                repaint();
            });
        }
    }
    
    /**
     * Get current network speed text for testing/debugging
     */
    public String getCurrentNetworkSpeedText() {
        return currentSpeedText.get();
    }
    
    /**
     * Check if network meter is currently visible
     */
    public boolean isNetworkMeterVisible() {
        return networkMeterVisible;
    }
    
    // ========== UI Manager Control Methods ==========
    
    /**
     * Enable or disable status updates display
     */
    public void setUpdatesEnabled(boolean enabled) {
        // For now, this controls whether temporary status messages are shown
        // You could extend this to control other update behaviors
        this.updatesEnabled = enabled;
    }
    
    /**
     * Enable or disable the runtime timer display
     */
    public void setTimerEnabled(boolean enabled) {
        this.timerEnabled = enabled;
        if (!enabled && runtimeTimer != null && runtimeTimer.isRunning()) {
            // Hide the timer if currently visible and running
            SwingUtilities.invokeLater(() -> {
                runtimeLabel.setVisible(false);
                revalidate();
                repaint();
            });
        } else if (enabled && runtimeTimer != null && runtimeTimer.isRunning()) {
            // Show the timer if it should be visible and is running
            SwingUtilities.invokeLater(() -> {
                runtimeLabel.setVisible(true);
                revalidate();
                repaint();
            });
        }
    }
    
    /**
     * Enable or disable the network meter display
     */
    public void setNetworkMeterEnabled(boolean enabled) {
        this.networkMeterEnabled = enabled;
        if (!enabled) {
            // Hide the network meter if currently visible
            hideNetworkMeter();
            if (networkUpdateTimer != null && networkUpdateTimer.isRunning()) {
                networkUpdateTimer.stop();
            }
        } else {
            // Restart network monitoring if it was stopped
            if (networkUpdateTimer != null && !networkUpdateTimer.isRunning()) {
                networkUpdateTimer.start();
            }
        }
    }

    /** Start the configuration saving spinner */
    public void startConfigSpinner() {
        if (!spinnerActive) {
            spinnerActive = true;
            currentFrame = 0;
            setSpinnerTextOnEdt(BRAILLE_FRAMES[currentFrame]);
            // Don't change visibility - spinner is always "visible" but shows space when inactive
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
            // Set to space character instead of hiding to prevent layout changes
            setSpinnerTextOnEdt(" ");
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
            // Professional text truncation to prevent overflow
            String displayText = truncateText(text, label.getWidth() - 20);
            label.setText(displayText);
            label.setToolTipText(text.length() > displayText.length() ? text : null);
        } else {
            SwingUtilities.invokeLater(() -> {
                String displayText = truncateText(text, label.getWidth() - 20);
                label.setText(displayText);
                label.setToolTipText(text.length() > displayText.length() ? text : null);
            });
        }
    }
    
    /** Truncate text professionally with ellipsis */
    private String truncateText(String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return text;
        }
        
        FontMetrics fm = label.getFontMetrics(label.getFont());
        if (fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        
        String ellipsis = "...";
        int ellipsisWidth = fm.stringWidth(ellipsis);
        
        for (int i = text.length() - 1; i > 0; i--) {
            String truncated = text.substring(0, i);
            if (fm.stringWidth(truncated) + ellipsisWidth <= maxWidth) {
                return truncated + ellipsis;
            }
        }
        
        return ellipsis;
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
        if (networkUpdateTimer != null && networkUpdateTimer.isRunning()) {
            networkUpdateTimer.stop();
        }
        if (networkHideTimer != null && networkHideTimer.isRunning()) {
            networkHideTimer.stop();
        }
        if (blinkTimer != null && blinkTimer.isRunning()) {
            blinkTimer.stop();
        }
        super.removeNotify();
    }
}
