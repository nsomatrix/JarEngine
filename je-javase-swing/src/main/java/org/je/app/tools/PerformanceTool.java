package org.je.app.tools;

import javax.swing.Box;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.JOptionPane;

import org.je.app.PerformanceConfig; // <-- Import config

public class PerformanceTool extends JFrame {
    private final JCheckBox hardwareAccelBox = new JCheckBox("Hardware Acceleration");
    private final JCheckBox doubleBufferBox = new JCheckBox("Double Buffering");
    private final JCheckBox frameSkipBox = new JCheckBox("Frame Skipping");
    private final JCheckBox imageCacheBox = new JCheckBox("Image Caching");
    private final JCheckBox vsyncBox = new JCheckBox("VSync");
    private final JButton gcButton = new JButton("Run Garbage Collection");

    public PerformanceTool() {
        super("Performance Tool");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(350, 260);
        setLocationRelativeTo(null);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        hardwareAccelBox.setToolTipText("Toggle Java2D hardware acceleration (requires restart)");
        doubleBufferBox.setToolTipText("Toggle double buffering for smoother rendering");
        frameSkipBox.setToolTipText("Allow emulator to skip frames if lagging");
        imageCacheBox.setToolTipText("Cache images and sprites for faster rendering");
        vsyncBox.setToolTipText("Synchronize rendering with display refresh rate (VSync)");
        gcButton.setToolTipText("Request Java to run garbage collection");

        panel.add(hardwareAccelBox);
        panel.add(doubleBufferBox);
        panel.add(frameSkipBox);
        panel.add(imageCacheBox);
        panel.add(vsyncBox);
        panel.add(Box.createVerticalStrut(10));
        panel.add(gcButton);

        // --- Stub logic for toggles ---
        hardwareAccelBox.addActionListener(e -> {
            // Set system property and prompt for restart
            boolean enabled = hardwareAccelBox.isSelected();
            System.setProperty("sun.java2d.opengl", enabled ? "true" : "false");
            JOptionPane.showMessageDialog(this, "Restart the emulator for hardware acceleration to take effect.");
        });
        doubleBufferBox.addActionListener(e -> {
            boolean enabled = doubleBufferBox.isSelected();
            PerformanceConfig.doubleBufferingEnabled = enabled;
        });
        frameSkipBox.addActionListener(e -> {
            boolean enabled = frameSkipBox.isSelected();
            PerformanceConfig.frameSkippingEnabled = enabled;
        });
        imageCacheBox.addActionListener(e -> {
            boolean enabled = imageCacheBox.isSelected();
            PerformanceConfig.imageCachingEnabled = enabled;
        });
        vsyncBox.addActionListener(e -> {
            boolean enabled = vsyncBox.isSelected();
            System.setProperty("sun.java2d.vsync", enabled ? "true" : "false");
            JOptionPane.showMessageDialog(this, "Restart the emulator for VSync to take effect.");
        });
        gcButton.addActionListener(e -> {
            System.gc();
            JOptionPane.showMessageDialog(this, "Garbage collection requested.");
        });

        add(panel);
    }
}

