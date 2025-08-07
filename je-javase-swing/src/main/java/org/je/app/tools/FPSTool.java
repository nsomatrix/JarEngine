package org.je.app.tools;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class FPSTool extends JFrame {
    public FPSTool() {
        super("FPS Tool");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        JPanel panel = new JPanel();
        // Add UI components for FPS tool here
        add(panel);
    }
}
