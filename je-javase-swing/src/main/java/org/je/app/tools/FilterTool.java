package org.je.app.tools;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Frame;

public class FilterTool extends JFrame {
    public FilterTool(Frame parent) {
        super("Filter Tool");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel panel = new JPanel();
        // Add UI components for filter tool here
        add(panel);
        pack();
        setLocationRelativeTo(parent);
    }

    public FilterTool() {
        this((Frame) null);
    }
}
