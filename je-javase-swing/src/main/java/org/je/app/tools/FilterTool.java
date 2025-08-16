package org.je.app.tools;

import javax.swing.*;
import java.awt.*;

/**
 * Small dialog to toggle a few common X-Render filters quickly.
 * Can be expanded later with more controls.
 */
public class FilterTool extends JDialog {

    public FilterTool(Frame owner) {
        super(owner, "X-Render", false);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; gbc.insets = new Insets(4,8,4,8);

        // Color Mode radio buttons
        panel.add(new JLabel("Color Mode:"), gbc);
        gbc.gridy++;
        ButtonGroup group = new ButtonGroup();
        JRadioButton rbFull = new JRadioButton("Full Color", FilterManager.getColorMode() == FilterManager.ColorMode.FULL_COLOR);
        JRadioButton rbGray = new JRadioButton("Grayscale", FilterManager.getColorMode() == FilterManager.ColorMode.GRAYSCALE);
        JRadioButton rbMono = new JRadioButton("Monochrome", FilterManager.getColorMode() == FilterManager.ColorMode.MONOCHROME);
        group.add(rbFull); group.add(rbGray); group.add(rbMono);
        JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        colorPanel.add(rbFull); colorPanel.add(rbGray); colorPanel.add(rbMono);
        panel.add(colorPanel, gbc);

        // Scanlines toggle
        gbc.gridy++;
        JCheckBox cbScan = new JCheckBox("Scanlines", FilterManager.isScanlines());
        panel.add(cbScan, gbc);

        // Scanline Intensity slider
        gbc.gridy++;
        JPanel scanIntensityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        scanIntensityPanel.add(new JLabel("Scanline Intensity"));
        JSlider scanSlider = new JSlider(0, 100, (int)Math.round(FilterManager.getScanlinesIntensity()*100));
        scanSlider.setMajorTickSpacing(25);
        scanSlider.setPaintTicks(true);
        scanSlider.setPaintLabels(false);
        scanIntensityPanel.add(scanSlider);
        panel.add(scanIntensityPanel, gbc);

        // Vignette toggle
        gbc.gridy++;
        JCheckBox cbVignette = new JCheckBox("Vignette", FilterManager.isVignette());
        panel.add(cbVignette, gbc);

        // Vignette Intensity slider
        gbc.gridy++;
        JPanel vigPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        vigPanel.add(new JLabel("Vignette Intensity"));
        JSlider vigSlider = new JSlider(0, 100, (int)Math.round(FilterManager.getVignetteIntensity()*100));
        vigSlider.setMajorTickSpacing(25);
        vigSlider.setPaintTicks(true);
        vigSlider.setPaintLabels(false);
        vigPanel.add(vigSlider);
        panel.add(vigPanel, gbc);

        // Brightness / Contrast / Gamma / Saturation sliders
        gbc.gridy++;
        panel.add(new JLabel("Brightness / Contrast / Gamma / Saturation"), gbc);

        gbc.gridy++;
        JPanel bcgs = new JPanel(new GridLayout(4, 1, 4, 4));
        JSlider b = slider("Brightness", (int)Math.round(FilterManager.getBrightness()*100));
        JSlider c = slider("Contrast",   (int)Math.round(FilterManager.getContrast()*100));
        JSlider g = slider("Gamma",      (int)Math.round(FilterManager.getGamma()*100));
        JSlider s = slider("Saturation", (int)Math.round(FilterManager.getSaturation()*100));
        bcgs.add(labeled("Brightness", b));
        bcgs.add(labeled("Contrast", c));
        bcgs.add(labeled("Gamma", g));
        bcgs.add(labeled("Saturation", s));
        panel.add(bcgs, gbc);

        // Palette & Dithering
        gbc.gridy++;
        panel.add(new JLabel("Palette & Dithering"), gbc);
        gbc.gridy++;
        JPanel palPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JComboBox<FilterManager.PaletteMode> palBox = new JComboBox<>(FilterManager.PaletteMode.values());
        palBox.setSelectedItem(FilterManager.getPaletteMode());
        JComboBox<FilterManager.DitherMode> dithBox = new JComboBox<>(FilterManager.DitherMode.values());
        dithBox.setSelectedItem(FilterManager.getDitherMode());
        palPanel.add(new JLabel("Palette:")); palPanel.add(palBox);
        palPanel.add(new JLabel("Dither:")); palPanel.add(dithBox);
        panel.add(palPanel, gbc);

        // Bloom controls
        gbc.gridy++;
        panel.add(new JLabel("Bloom"), gbc);
        gbc.gridy++;
        JPanel bloomPanel = new JPanel(new GridLayout(4, 1, 4, 4));
        JCheckBox cbBloom = new JCheckBox("Enable Bloom", FilterManager.isBloom());
        JSlider th = slider("Threshold", (int)Math.round(FilterManager.getBloomThreshold()*100));
        JSlider bi = slider("Intensity", (int)Math.round(FilterManager.getBloomIntensity()*100));
        JSlider br = new JSlider(1, 5, FilterManager.getBloomRadius());
        bloomPanel.add(cbBloom);
        bloomPanel.add(labeled("Threshold", th));
        bloomPanel.add(labeled("Intensity", bi));
        bloomPanel.add(labeled("Radius", br));
        panel.add(bloomPanel, gbc);

        // Apply listeners
        rbFull.addActionListener(e -> FilterManager.setColorMode(FilterManager.ColorMode.FULL_COLOR));
        rbGray.addActionListener(e -> FilterManager.setColorMode(FilterManager.ColorMode.GRAYSCALE));
        rbMono.addActionListener(e -> FilterManager.setColorMode(FilterManager.ColorMode.MONOCHROME));
        cbScan.addActionListener(e -> FilterManager.setScanlines(cbScan.isSelected()));
        scanSlider.addChangeListener(e -> FilterManager.setScanlinesIntensity(scanSlider.getValue()/100f));
        cbVignette.addActionListener(e -> FilterManager.setVignette(cbVignette.isSelected()));
        vigSlider.addChangeListener(e -> FilterManager.setVignetteIntensity(vigSlider.getValue()/100f));

        b.addChangeListener(e -> FilterManager.setBrightness(b.getValue()/100f));
        c.addChangeListener(e -> FilterManager.setContrast(c.getValue()/100f));
        g.addChangeListener(e -> FilterManager.setGamma(g.getValue()/100f));
        s.addChangeListener(e -> FilterManager.setSaturation(s.getValue()/100f));

        palBox.addActionListener(e -> FilterManager.setPaletteMode((FilterManager.PaletteMode) palBox.getSelectedItem()));
        dithBox.addActionListener(e -> FilterManager.setDitherMode((FilterManager.DitherMode) dithBox.getSelectedItem()));
        cbBloom.addActionListener(e -> FilterManager.setBloom(cbBloom.isSelected()));
        th.addChangeListener(e -> FilterManager.setBloomThreshold(th.getValue()/100f));
        bi.addChangeListener(e -> FilterManager.setBloomIntensity(bi.getValue()/100f));
        br.addChangeListener(e -> FilterManager.setBloomRadius(br.getValue()));

        add(panel, BorderLayout.CENTER);

        JButton reset = new JButton("Reset");
        reset.addActionListener(e -> {
            // Reset all filter settings to defaults
            FilterManager.resetToDefaults();
            // Update UI controls to reflect the reset values
            rbFull.setSelected(true);
            cbScan.setSelected(false);
            scanSlider.setValue(12);
            cbVignette.setSelected(false);
            vigSlider.setValue(20);
            b.setValue(100);
            c.setValue(100);
            g.setValue(100);
            s.setValue(100);
            palBox.setSelectedItem(FilterManager.PaletteMode.NONE);
            dithBox.setSelectedItem(FilterManager.DitherMode.NONE);
            cbBloom.setSelected(false);
            th.setValue(70);
            bi.setValue(60);
            br.setValue(2);
        });
        JButton close = new JButton("Close");
        close.addActionListener(e -> setVisible(false));
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(reset);
        south.add(close);
        add(south, BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(480, getHeight()));
        setLocationRelativeTo(owner);
    }

    private static JPanel labeled(String text, JSlider slider) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel(text), BorderLayout.WEST);
        p.add(slider, BorderLayout.CENTER);
        return p;
    }

    private static JSlider slider(String name, int val) {
        JSlider s = new JSlider(0, 200, val);
        s.setMajorTickSpacing(50);
        s.setPaintTicks(true);
        s.setPaintLabels(false);
        return s;
    }
}
