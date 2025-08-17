package org.je.app.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.UIManager;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.je.util.ProxyConfig;

/**
 * Proxy configuration tool for JarEngine emulator
 * Provides a user interface for configuring proxy settings
 */
public class ProxyTool extends JFrame {
    
    private static final long serialVersionUID = 1L;
    
    private ProxyConfig proxyConfig;
    
    // UI Components
    private JCheckBox enableProxyCheckBox;
    private JTextField hostField;
    private JSpinner portSpinner;
    private JCheckBox useAuthCheckBox;
    private JTextField usernameField;
    private JPasswordField passwordField;
    
    // Protocol checkboxes
    private JCheckBox httpCheckBox;
    private JCheckBox httpsCheckBox;
    private JCheckBox socketCheckBox;
    
    // Buttons
    private JButton testButton;
    private JButton applyButton;
    private JButton cancelButton;
    
    public ProxyTool(Frame parent) {
        super("Proxy Configuration");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        this.proxyConfig = ProxyConfig.getInstance();
        
        initComponents();
        loadCurrentSettings();
        layoutComponents();
        
        setLocationRelativeTo(parent);
        setResizable(false);
    }
    
    public ProxyTool() {
        this((Frame) null);
    }
    
    private void initComponents() {
        // Main proxy settings
        enableProxyCheckBox = new JCheckBox("Enable Proxy");
        hostField = new JTextField(20);
        portSpinner = new JSpinner(new SpinnerNumberModel(8080, 1, 65535, 1));
        
        // Configure spinner to not use thousands separators
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(portSpinner, "#");
        portSpinner.setEditor(editor);
        
        useAuthCheckBox = new JCheckBox("Use Authentication");
        usernameField = new JTextField(15);
        passwordField = new JPasswordField(15);
        
        // Protocol settings
        httpCheckBox = new JCheckBox("HTTP");
        httpsCheckBox = new JCheckBox("HTTPS");
        socketCheckBox = new JCheckBox("Socket/SOCKS");
        
        // Buttons
        testButton = new JButton("Test Connection");
        applyButton = new JButton("Apply");
        cancelButton = new JButton("Cancel");
        
        // Add listeners
        setupListeners();
    }
    
    private void setupListeners() {
        enableProxyCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateComponentStates();
            }
        });
        
        useAuthCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateComponentStates();
            }
        });
        
        testButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                testProxyConnection();
            }
        });
        
        applyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                applySettings();
            }
        });
        
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }
    
    private void loadCurrentSettings() {
        enableProxyCheckBox.setSelected(proxyConfig.isEnabled());
        hostField.setText(proxyConfig.getProxyHost());
        portSpinner.setValue(proxyConfig.getProxyPort());
        useAuthCheckBox.setSelected(proxyConfig.isUseAuthentication());
        usernameField.setText(proxyConfig.getProxyUsername());
        passwordField.setText(proxyConfig.getProxyPassword());
        
        httpCheckBox.setSelected(proxyConfig.isHttpEnabled());
        httpsCheckBox.setSelected(proxyConfig.isHttpsEnabled());
        socketCheckBox.setSelected(proxyConfig.isSocketEnabled());
        
        updateComponentStates();
    }
    
    private void updateComponentStates() {
        boolean proxyEnabled = enableProxyCheckBox.isSelected();
        boolean authEnabled = useAuthCheckBox.isSelected();
        
        hostField.setEnabled(proxyEnabled);
        portSpinner.setEnabled(proxyEnabled);
        useAuthCheckBox.setEnabled(proxyEnabled);
        usernameField.setEnabled(proxyEnabled && authEnabled);
        passwordField.setEnabled(proxyEnabled && authEnabled);
        
        httpCheckBox.setEnabled(proxyEnabled);
        httpsCheckBox.setEnabled(proxyEnabled);
        socketCheckBox.setEnabled(proxyEnabled);
        
        testButton.setEnabled(proxyEnabled);
    }
    
    private java.util.concurrent.ExecutorService testExecutor = 
        java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ProxyTest");
            t.setDaemon(true);
            return t;
        });
    
    private void testProxyConnection() {
        testButton.setEnabled(false);
        testButton.setText("Testing...");
        
        // Run test in managed thread pool
        testExecutor.submit(new Runnable() {
            public void run() {
                final boolean success = proxyConfig.testProxy();
                
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        testButton.setEnabled(true);
                        testButton.setText("Test Connection");
                        
                        if (success) {
                            JOptionPane.showMessageDialog(ProxyTool.this,
                                "Proxy connection test successful!",
                                "Test Result",
                                JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(ProxyTool.this,
                                "Proxy connection test failed!\nPlease check your proxy settings.",
                                "Test Result",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
            }
        });
    }
    
    private void applySettings() {
        try {
            proxyConfig.setEnabled(enableProxyCheckBox.isSelected());
            proxyConfig.setProxyHost(hostField.getText().trim());
            proxyConfig.setProxyPort((Integer) portSpinner.getValue());
            proxyConfig.setUseAuthentication(useAuthCheckBox.isSelected());
            proxyConfig.setProxyUsername(usernameField.getText());
            proxyConfig.setProxyPassword(new String(passwordField.getPassword()));
            
            proxyConfig.setHttpEnabled(httpCheckBox.isSelected());
            proxyConfig.setHttpsEnabled(httpsCheckBox.isSelected());
            proxyConfig.setSocketEnabled(socketCheckBox.isSelected());
            
            JOptionPane.showMessageDialog(this,
                "Proxy settings applied successfully!\n" +
                "Note: Some changes may require restarting the emulator to take full effect.",
                "Settings Applied",
                JOptionPane.INFORMATION_MESSAGE);
                
            dispose();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error applying settings: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout());
        
        // Main settings panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        GridBagConstraints gbc = new GridBagConstraints();
        // Use consistent spacing relative to system defaults
        int pad = Math.max(4, UIManager.getInt("Panel.margin"));
        gbc.insets = new Insets(pad, pad, pad, pad);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Proxy settings section
        JPanel proxyPanel = new JPanel(new GridBagLayout());
        proxyPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Proxy Settings", 
            TitledBorder.LEFT, TitledBorder.TOP));
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        proxyPanel.add(enableProxyCheckBox, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        proxyPanel.add(new JLabel("Host:"), gbc);
        gbc.gridx = 1;
        proxyPanel.add(hostField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        proxyPanel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1;
        proxyPanel.add(portSpinner, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        proxyPanel.add(useAuthCheckBox, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1;
        proxyPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        proxyPanel.add(usernameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5;
        proxyPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        proxyPanel.add(passwordField, gbc);
        
        // Protocol settings section
        JPanel protocolPanel = new JPanel(new GridBagLayout());
        protocolPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Protocols", 
            TitledBorder.LEFT, TitledBorder.TOP));
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        protocolPanel.add(httpCheckBox, gbc);
        gbc.gridx = 1;
        protocolPanel.add(httpsCheckBox, gbc);
        gbc.gridx = 2;
        protocolPanel.add(socketCheckBox, gbc);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(testButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);
        
        // Layout main panel
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        mainPanel.add(proxyPanel, gbc);
        
        gbc.gridy = 1;
        mainPanel.add(protocolPanel, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        pack();
        // Set minimum size relative to screen size instead of hardcoded values
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int minWidth = Math.max(400, screenSize.width / 4);
        int minHeight = Math.max(350, screenSize.height / 4);
        setMinimumSize(new Dimension(minWidth, minHeight));
    }
    
    @Override
    public void dispose() {
        if (testExecutor != null && !testExecutor.isShutdown()) {
            testExecutor.shutdown();
            try {
                if (!testExecutor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    testExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                testExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        super.dispose();
    }
}
