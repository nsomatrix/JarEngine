package org.je.app.tools;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.je.device.ui.EventDispatcher;

public class FPSTool extends JFrame {
    private JSlider fpsSlider;
    private JLabel fpsValueLabel;
    private JCheckBox overlayCheckBox;
    private Timer fpsUpdateTimer;
    
    // FPS overlay state - made public static for overlay access
    public static volatile boolean fpsOverlayEnabled = false;
    public static volatile int targetFps = 30; // Default FPS
    public static volatile double currentFps = 0.0; // Current FPS for overlay
    private static volatile int frameCount = 0; // Frame counter for FPS calculation
    
    public FPSTool() {
        super("FPS Tool");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(400, 260);
        setLocationRelativeTo(null);
        setResizable(false);
        
        initComponents();
        setupLayout();
        setupEventHandlers();
        startFpsMonitoring();
        revalidate();
        repaint();
    }
    
    private void initComponents() {
        // FPS Slider (1-60 FPS, default 60)
        fpsSlider = new JSlider(JSlider.HORIZONTAL, 1, 30, targetFps);
        fpsSlider.setMajorTickSpacing(10);
        fpsSlider.setMinorTickSpacing(5);
        fpsSlider.setPaintTicks(true);
        fpsSlider.setPaintLabels(true);
        
        // Label for target FPS
        fpsValueLabel = new JLabel("Target FPS: " + targetFps);
        
        // Checkbox for overlay
        overlayCheckBox = new JCheckBox("Show FPS Overlay", fpsOverlayEnabled);
        
        // Apply initial FPS setting
        EventDispatcher.maxFps = targetFps;
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Main panel with padding
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // FPS Control Section
        JPanel fpsPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        fpsPanel.setBorder(BorderFactory.createTitledBorder("FPS Control"));
        

        fpsPanel.add(fpsSlider);
        fpsPanel.add(fpsValueLabel);
        
        // Overlay Section
        JPanel overlayPanel = new JPanel(new BorderLayout());
        overlayPanel.setBorder(BorderFactory.createTitledBorder("Toggle"));
        overlayPanel.add(overlayCheckBox, BorderLayout.CENTER);
        
        mainPanel.add(fpsPanel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(overlayPanel);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private void setupEventHandlers() {
        // FPS Slider - automatically applies FPS limiting
        fpsSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (!fpsSlider.getValueIsAdjusting()) {
                    targetFps = fpsSlider.getValue();
                    fpsValueLabel.setText("Target FPS: " + targetFps);
                    EventDispatcher.maxFps = targetFps; // Automatically apply FPS limiting
                }
            }
        });
        
        // FPS Overlay Checkbox
        overlayCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fpsOverlayEnabled = overlayCheckBox.isSelected();
                // Force repaint of the display component to show/hide overlay immediately
                try {
                    org.je.device.DeviceFactory.getDevice().getDeviceDisplay().repaint(0, 0, 1, 1);
                } catch (Exception ex) {
                    // Ignore any errors if device is not available
                }
            }
        });
    }
    
    private void startFpsMonitoring() {
        // Update FPS calculation every second (but don't display in dialog)
        fpsUpdateTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateCurrentFps();
            }
        });
        fpsUpdateTimer.start();
    }
    
    // Method to be called from display component to count frames
    public static void incrementFrameCount() {
        frameCount++;
    }
    
    private void updateCurrentFps() {
        currentFps = frameCount;
        frameCount = 0;
        // Don't update any label - current FPS only shows on canvas overlay
    }
    
    @Override
    public void dispose() {
        if (fpsUpdateTimer != null) {
            fpsUpdateTimer.stop();
        }
        super.dispose();
    }
}
