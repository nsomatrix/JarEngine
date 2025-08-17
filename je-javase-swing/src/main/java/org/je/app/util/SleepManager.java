package org.je.app.util;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.Timer;

/**
 * SleepManager handles putting the emulator to sleep with a dark screen and logo.
 * Optimized for minimal performance impact and thread safety.
 */
public class SleepManager {
    
    private static final int SLEEP_DELAY_MS = 40000; // 40 seconds
    private static final int FADE_INTERVAL_MS = 32; // ~30 FPS for smooth animation
    private static final float FADE_STEP = 0.05f; // Smooth fade step
    private static final String APP_ICON_PATH = "/org/je/icon.png";
    
    private final JFrame parentFrame;
    private final Timer sleepTimer;
    private final JWindow sleepWindow;
    private final Timer fadeTimer;
    private final Timer pulseTimer;
    
    private volatile boolean isSleepEnabled = false;
    private volatile boolean isSleeping = false;
    private volatile float fadeAlpha = 0.0f;
    private volatile float pulseAlpha = 0.0f;
    private volatile boolean isFadingIn = false;
    private volatile boolean isFadingOut = false;
    
    private Image appIcon = null;
    
    public SleepManager(JFrame parentFrame) {
        this.parentFrame = parentFrame;
        this.sleepWindow = createSleepWindow();
        this.sleepTimer = createSleepTimer();
        this.fadeTimer = createFadeTimer();
        this.pulseTimer = createPulseTimer();
        
        loadAppIcon();
    }
    
    /**
     * Create the sleep timer
     */
    private Timer createSleepTimer() {
        Timer timer = new Timer(SLEEP_DELAY_MS, e -> {
            if (isSleepEnabled && !isSleeping) {
                enterSleepMode();
            }
        });
        timer.setRepeats(false);
        return timer;
    }
    
    /**
     * Create the fade animation timer
     */
    private Timer createFadeTimer() {
        return new Timer(FADE_INTERVAL_MS, e -> {
            if (isFadingIn) {
                fadeAlpha += FADE_STEP;
                if (fadeAlpha >= 1.0f) {
                    fadeAlpha = 1.0f;
                    isFadingIn = false;
                    fadeTimer.stop();
                    pulseTimer.start();
                }
                sleepWindow.repaint();
            } else if (isFadingOut) {
                fadeAlpha -= FADE_STEP;
                if (fadeAlpha <= 0.0f) {
                    fadeAlpha = 0.0f;
                    isFadingOut = false;
                    fadeTimer.stop();
                    pulseTimer.stop();
                    sleepWindow.setVisible(false);
                    parentFrame.setEnabled(true);
                    if (isSleepEnabled) {
                        sleepTimer.restart();
                    }
                }
                sleepWindow.repaint();
            }
        });
    }
    
    /**
     * Create the pulse animation timer
     */
    private Timer createPulseTimer() {
        return new Timer(100, e -> { // 10 FPS for subtle pulse
            pulseAlpha += 0.02f;
            if (pulseAlpha >= 0.2f) {
                pulseAlpha = 0.2f;
                pulseTimer.stop();
                // Reverse direction after a delay
                Timer reverseTimer = new Timer(1000, ev -> {
                    pulseTimer.setDelay(100);
                    pulseTimer.start();
                });
                reverseTimer.setRepeats(false);
                reverseTimer.start();
            }
            sleepWindow.repaint();
        });
    }
    
    /**
     * Create the sleep window
     */
    private JWindow createSleepWindow() {
        JWindow window = new JWindow(parentFrame);
        window.setLayout(new BorderLayout());
        window.setBackground(new Color(0, 0, 0, 0));
        window.setAlwaysOnTop(false);
        
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                
                try {
                    // Enable anti-aliasing
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    
                    // Draw dark gradient background
                    int width = getWidth();
                    int height = getHeight();
                    
                    GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(0, 0, 0, (int)(fadeAlpha * 200)),
                        width, height, new Color(0, 0, 0, (int)(fadeAlpha * 220))
                    );
                    g2d.setPaint(gradient);
                    g2d.fillRect(0, 0, width, height);
                    
                    // Draw app icon with pulse effect
                    if (appIcon != null) {
                        int iconSize = 80;
                        int x = (width - iconSize) / 2;
                        int y = (height - iconSize) / 2 - 20;
                        
                        // Apply subtle pulse effect
                        float pulseScale = 1.0f + (pulseAlpha * 0.05f);
                        int scaledSize = (int) (iconSize * pulseScale);
                        int scaledX = x - (scaledSize - iconSize) / 2;
                        int scaledY = y - (scaledSize - iconSize) / 2;
                        
                        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f + (pulseAlpha * 0.1f)));
                        g2d.drawImage(appIcon, scaledX, scaledY, scaledSize, scaledSize, null);
                    }
                    
                    // Draw "Click to Wake" hint
                    if (fadeAlpha > 0.5f) {
                        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));
                        g2d.setColor(new Color(180, 180, 180)); // Professional grey
                        // Use relative font sizing based on system defaults
                        Font baseFont = UIManager.getFont("Label.font");
                        g2d.setFont(baseFont.deriveFont(Font.PLAIN, baseFont.getSize() * 1.3f));
                        
                        String hintText = "Click to Wake";
                        FontMetrics fm = g2d.getFontMetrics();
                        int textWidth = fm.stringWidth(hintText);
                        int textX = (width - textWidth) / 2;
                        int textY = height / 2 + 60;
                        
                        g2d.drawString(hintText, textX, textY);
                    }
                } finally {
                    g2d.dispose(); // Ensure Graphics2D is disposed
                }
            }
        };
        
        panel.setOpaque(true);
        panel.setFocusable(true);
        
        // Single mouse listener for wake functionality
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isSleeping) {
                    e.consume();
                    exitSleepMode();
                }
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                if (isSleeping) {
                    e.consume();
                    exitSleepMode();
                }
            }
        });
        
        // Single key listener for wake functionality
        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (isSleeping) {
                    e.consume();
                    exitSleepMode();
                }
            }
        });
        
        window.add(panel, BorderLayout.CENTER);
        return window;
    }
    
    /**
     * Load the app icon
     */
    private void loadAppIcon() {
        try {
            appIcon = Toolkit.getDefaultToolkit().getImage(getClass().getResource(APP_ICON_PATH));
        } catch (Exception e) {
            appIcon = null;
        }
    }
    
    /**
     * Enable or disable sleep mode
     */
    public void setSleepEnabled(boolean enabled) {
        this.isSleepEnabled = enabled;
        
        if (enabled) {
            sleepTimer.restart();
        } else {
            sleepTimer.stop();
            if (isSleeping) {
                exitSleepMode();
            }
        }
    }
    
    /**
     * Enter sleep mode
     */
    private void enterSleepMode() {
        if (isSleeping) return;
        
        isSleeping = true;
        
        SwingUtilities.invokeLater(() -> {
            try {
                Rectangle bounds = parentFrame.getBounds();
                sleepWindow.setBounds(bounds);
                sleepWindow.setLocation(bounds.x, bounds.y);
                sleepWindow.setVisible(true);
                sleepWindow.requestFocus();
                
                parentFrame.setEnabled(false);
                sleepTimer.stop();
                
                fadeAlpha = 0.0f;
                isFadingIn = true;
                isFadingOut = false;
                fadeTimer.start();
            } catch (Exception e) {
                isSleeping = false;
            }
        });
    }
    
    /**
     * Exit sleep mode
     */
    private void exitSleepMode() {
        if (!isSleeping) return;
        
        isSleeping = false;
        
        SwingUtilities.invokeLater(() -> {
            try {
                isFadingOut = true;
                isFadingIn = false;
                fadeTimer.start();
            } catch (Exception e) {
                // Silent error handling
            }
        });
    }
    
    /**
     * Check if currently sleeping
     */
    public boolean isSleeping() {
        return isSleeping;
    }
    
    /**
     * Check if sleep mode is enabled
     */
    public boolean isSleepEnabled() {
        return isSleepEnabled;
    }
    
    /**
     * Reset the sleep timer
     */
    public void resetSleepTimer() {
        if (isSleepEnabled) {
            // Always restart the timer when user is active, regardless of sleep state
            sleepTimer.restart();
        }
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        sleepTimer.stop();
        fadeTimer.stop();
        pulseTimer.stop();
        
        if (sleepWindow != null) {
            sleepWindow.setVisible(false);
            sleepWindow.dispose();
        }
    }
} 