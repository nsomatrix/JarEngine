package org.je.app.tools;

import org.je.app.util.MemoryManager;
import org.je.performance.PerformanceManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Memory monitoring and management tool for long-running emulator sessions.
 */
public class MemoryMonitorTool extends JDialog {
    
    private static final long serialVersionUID = 1L;
    
    private JLabel memoryLabel;
    private JButton gcButton;
    private JButton clearCacheButton;
    private JCheckBox autoCleanupCheckbox;
    private Timer updateTimer;
    
    public MemoryMonitorTool(Frame parent) {
        super(parent, "Memory Monitor", false); // Non-modal
        
        initComponents();
        setupLayout();
        setupEventHandlers();
        
        pack();
        setLocationRelativeTo(parent);
        
        // Set window properties for better user experience
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(true);
        
        // Start updating memory stats every 2 seconds
        updateTimer = new Timer(2000, e -> updateMemoryStats());
        updateTimer.start();
        
        // Update initial stats
        updateMemoryStats();
    }
    
    private void initComponents() {
        memoryLabel = new JLabel("Loading memory stats...");
        memoryLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        memoryLabel.setVerticalAlignment(SwingConstants.TOP);
        
        gcButton = new JButton("Force GC");
        gcButton.setToolTipText("Force garbage collection (may cause brief pause)");
        gcButton.setMnemonic('G'); // Alt+G keyboard shortcut
        
        clearCacheButton = new JButton("Clear Cache");
        clearCacheButton.setToolTipText("Clear sprite cache and managed resources");
        clearCacheButton.setMnemonic('C'); // Alt+C keyboard shortcut
        
        autoCleanupCheckbox = new JCheckBox("Auto Cleanup", true);
        autoCleanupCheckbox.setToolTipText("Enable automatic memory cleanup for long sessions");
        autoCleanupCheckbox.setMnemonic('A'); // Alt+A keyboard shortcut
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Memory stats panel with proper spacing
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Memory Usage"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5) // Inner padding
        ));
        topPanel.add(memoryLabel, BorderLayout.CENTER);
        
        // Button panel with proper spacing
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        buttonPanel.add(gcButton);
        buttonPanel.add(clearCacheButton);
        buttonPanel.add(autoCleanupCheckbox);
        
        add(topPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Set minimum size based on content, not hardcoded
        setMinimumSize(getPreferredSize());
    }
    
    private void setupEventHandlers() {
        gcButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gcButton.setEnabled(false);
                gcButton.setText("Running...");
                
                // Run GC on background thread to avoid blocking EDT
                new Thread(() -> {
                    System.gc();
                    System.runFinalization();
                    
                    // Update UI back on EDT
                    SwingUtilities.invokeLater(() -> {
                        gcButton.setEnabled(true);
                        gcButton.setText("Force GC");
                        updateMemoryStats();
                    });
                }, "GC-Thread").start();
            }
        });
        
        clearCacheButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PerformanceManager.clearSpriteCache();
                MemoryManager.cleanupAllResources();
                updateMemoryStats();
                JOptionPane.showMessageDialog(MemoryMonitorTool.this, 
                    "Cache cleared and resources cleaned up.", 
                    "Memory Cleanup", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        autoCleanupCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MemoryManager.setAutoCleanupEnabled(autoCleanupCheckbox.isSelected());
            }
        });
        
        // Stop timer when window is closed
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (updateTimer != null) {
                    updateTimer.stop();
                }
            }
        });
    }
    
    private void updateMemoryStats() {
        if (memoryLabel != null) {
            String stats = MemoryManager.getMemoryStats();
            memoryLabel.setText("<html>" + stats.replace(" | ", "<br>") + "</html>");
        }
    }
    
    @Override
    public void dispose() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
        super.dispose();
    }
}
