package org.je.app.tools;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class ProxyTool extends JFrame {
    public ProxyTool() {
        super("Proxy Tool");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        JPanel panel = new JPanel();
        // Add UI components for proxy tool here
        add(panel);
    }
}
