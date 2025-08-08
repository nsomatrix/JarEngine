package org.je.app.tools;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Frame;

public class ProxyTool extends JFrame {
    public ProxyTool(Frame parent) {
        super("Proxy Tool");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel panel = new JPanel();
        // Add UI components for proxy tool here
        add(panel);
        pack();
        setLocationRelativeTo(parent);
    }

    public ProxyTool() {
        this((Frame) null);
    }
}
