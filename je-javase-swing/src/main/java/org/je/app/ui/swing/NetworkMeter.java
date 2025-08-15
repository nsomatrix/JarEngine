package org.je.app.ui.swing;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Font;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import org.je.util.NetEventBus;

/**
 * Network activity meter that displays bandwidth usage in the status bar.
 * Shows and hides automatically based on network activity.
 * Displays speed in appropriate units (B/s, KB/s, MB/s).
 */
public class NetworkMeter extends JLabel {
    
    private static final int UPDATE_INTERVAL_MS = 1000; // Update every second
    private static final int HIDE_DELAY_MS = 3000; // Hide after 3 seconds of inactivity
    private static final int ACTIVITY_WINDOW_MS = 2000; // Consider events from last 2 seconds
    
    private final AtomicLong lastEventCount = new AtomicLong(0);
    private final AtomicLong lastUpdateTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicReference<String> currentSpeedText = new AtomicReference<>("");
    
    private Timer updateTimer;
    private Timer hideTimer;
    private volatile boolean meterVisible = false;
    
    public NetworkMeter() {
        super();
        setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        setVisible(false);
        
        // Initialize update timer
        updateTimer = new Timer(UPDATE_INTERVAL_MS, e -> updateNetworkStats());
        updateTimer.setRepeats(true);
        updateTimer.start();
        
        // Initialize hide timer (not started initially)
        hideTimer = new Timer(HIDE_DELAY_MS, e -> hideMeter());
        hideTimer.setRepeats(false);
    }
    
    /**
     * Update network statistics by analyzing recent network events
     */
    private void updateNetworkStats() {
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
            long cutoffTime = currentTime - ACTIVITY_WINDOW_MS;
            
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
            long timeDelta = currentTime - lastUpdateTime.get();
            if (timeDelta > 0) {
                double inSpeed = (bytesInThisPeriod * 1000.0) / timeDelta;
                double outSpeed = (bytesOutThisPeriod * 1000.0) / timeDelta;
                double totalSpeed = inSpeed + outSpeed;
                
                // Show meter if there's activity or new events
                if (hasNewEvents || totalSpeed > 0) {
                    showMeter();
                    updateSpeedDisplay(totalSpeed, inSpeed, outSpeed);
                    
                    // Reset hide timer
                    if (hideTimer.isRunning()) {
                        hideTimer.restart();
                    } else {
                        hideTimer.start();
                    }
                }
            }
            
            lastUpdateTime.set(currentTime);
            
        } catch (Exception ex) {
            // Silently handle any errors to avoid interfering with the UI
        }
    }
    
    /**
     * Estimate bytes from network event information
     */
    private long estimateBytesFromEvent(NetEventBus.NetEvent event) {
        // This is a rough estimation since NetEventBus doesn't track actual byte counts
        // We'll estimate based on event type and info
        
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
     * Update the speed display with formatted text
     */
    /**
     * Update the speed display text with formatted values
     * @param totalSpeed Combined speed in bytes per second
     * @param inSpeed Download speed in bytes per second
     * @param outSpeed Upload speed in bytes per second
     */
    private void updateSpeedDisplay(double totalSpeed, double inSpeed, double outSpeed) {
        String speedText = formatSpeed(totalSpeed);
        
        // Add direction indicators if there's significant directional traffic
        if (inSpeed > outSpeed * 2) {
            speedText = "↓ " + speedText;
        } else if (outSpeed > inSpeed * 2) {
            speedText = "↑ " + speedText;
        } else if (inSpeed > 0 && outSpeed > 0) {
            speedText = "↕ " + speedText;
        }
        
        final String finalSpeedText = speedText; // Make it effectively final
        currentSpeedText.set(finalSpeedText);
        
        SwingUtilities.invokeLater(() -> {
            setText(finalSpeedText);
            repaint();
        });
    }
    
    /**
     * Format speed in human-readable units
     */
    private String formatSpeed(double bytesPerSecond) {
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
    private void showMeter() {
        if (!meterVisible) {
            meterVisible = true;
            SwingUtilities.invokeLater(() -> {
                setVisible(true);
                if (getParent() != null) {
                    getParent().revalidate();
                    getParent().repaint();
                }
            });
        }
    }
    
    /**
     * Hide the network meter
     */
    private void hideMeter() {
        if (meterVisible) {
            meterVisible = false;
            SwingUtilities.invokeLater(() -> {
                setVisible(false);
                if (getParent() != null) {
                    getParent().revalidate();
                    getParent().repaint();
                }
            });
        }
    }
    
    /**
     * Cleanup resources when the component is removed
     */
    @Override
    public void removeNotify() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
        if (hideTimer != null) {
            hideTimer.stop();
        }
        super.removeNotify();
    }
    
    /**
     * Get current speed text for testing/debugging
     */
    public String getCurrentSpeedText() {
        return currentSpeedText.get();
    }
    
    /**
     * Check if meter is currently visible
     */
    public boolean isMeterVisible() {
        return meterVisible;
    }
}
