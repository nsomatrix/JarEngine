package org.je.app.ui.swing;

import javax.swing.*;
import java.awt.*;

public class HeapSizeTool extends JDialog {
    private static final String[] OPTIONS = {"64 MB", "128 MB", "256 MB", "512 MB", "1024 MB", "2048 MB", "4096 MB"};
    private static final String[] VALUES = {"64m", "128m", "256m", "512m", "1024m", "2048m", "4096m"};
    private JComboBox<String> heapCombo;
    private String selectedValue = null;

    public HeapSizeTool(Frame parent, String currentValue) {
        super(parent, "Configure Heap Size", true);
        setLayout(new BorderLayout(10, 10));

        JLabel label = new JLabel("Select JVM Heap Size (will apply on next restart):");
        add(label, BorderLayout.NORTH);

        heapCombo = new JComboBox<>(OPTIONS);
        int idx = 1;
        if (currentValue != null) {
            for (int i = 0; i < VALUES.length; i++) {
                if (currentValue.equals(VALUES[i])) {
                    idx = i;
                    break;
                }
            }
        }
        heapCombo.setSelectedIndex(idx);
        add(heapCombo, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        okButton.addActionListener(e -> {
            selectedValue = VALUES[heapCombo.getSelectedIndex()];
            setVisible(false);
        });
        cancelButton.addActionListener(e -> {
            selectedValue = null;
            setVisible(false);
        });

        pack();
        setLocationRelativeTo(parent);
    }

    public String getSelectedHeapSize() {
        return selectedValue;
    }
}
