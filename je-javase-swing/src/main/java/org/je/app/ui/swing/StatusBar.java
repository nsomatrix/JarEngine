package org.je.app.ui.swing;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * Swing status bar component with centralized behavior.
 * - Receives direct app status updates
 * - Appends proxy status when enabled
 * - Supports temporary messages (auto-restore after delay)
 */
public class StatusBar extends JPanel {

    private final JLabel label;
    private javax.swing.Timer restoreTimer;
    private String persistentText = "Status";
    private boolean includeProxySuffix = true;

    public StatusBar() {
        super(new BorderLayout());
        this.label = new JLabel(persistentText);

        // Prevent minimum-width issues and ensure consistent height
        try {
            int barHeight = Math.max(1, label.getPreferredSize().height);
            label.setMinimumSize(new Dimension(0, barHeight));
            this.setMinimumSize(new Dimension(0, barHeight));
            this.setPreferredSize(new Dimension(1, barHeight));
        } catch (Exception ignore) {
        }

        setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        add(label, BorderLayout.WEST);
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

    private void setLabelTextOnEdt(String text) {
        if (SwingUtilities.isEventDispatchThread()) {
            label.setText(text);
        } else {
            SwingUtilities.invokeLater(() -> label.setText(text));
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
