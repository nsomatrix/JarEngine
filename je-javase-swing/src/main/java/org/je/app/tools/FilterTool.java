package org.je.app.tools;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class FilterTool extends JFrame {
    public FilterTool() {
        super("Filter Tool");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        JPanel panel = new JPanel();
        // Add UI components for filter tool here
        add(panel);
    }
}
