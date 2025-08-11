/**
 *  MicroEmulator
 *  Copyright (C) 2001-2024 Bartek Teodorczyk <barteo@barteo.net>
 *
 *  It is licensed under the following two licenses as alternatives:
 *    1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 *    2. Apache License (the "AL") Version 2.0
 *
 *  You may not use this file except in compliance with at least one of
 *  the above two licenses.
 *
 *  You may obtain a copy of the LGPL at
 *      http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 *
 *  You may obtain a copy of the AL at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the LGPL or the AL for the specific language governing permissions and
 *  limitations.
 */
package org.je.app.ui.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.je.app.Main;
import org.je.app.Config;
import org.je.app.util.BuildVersion;
import org.je.log.Logger;

/**
 * Status dialog displaying comprehensive system information, performance metrics,
 * and emulator state in real-time. Designed for cross-platform compatibility
 * and minimal interference with the emulator core.
 */
public class StatusDialog extends SwingDialogPanel {
    private static final long serialVersionUID = 1L;
    
    private JTabbedPane tabbedPane;
    private Main mainInstance;
    private Timer updateTimer;
    private final AtomicBoolean isUpdating = new AtomicBoolean(false);
    
    // System panel labels
    private JLabel javaVersionLabel;
    private JLabel osLabel;
    private JLabel emulatorVersionLabel;
    private JLabel configDirLabel;
    private JLabel screenResLabel;
    private JLabel networkInterfacesLabel;
    
    // Performance panel labels (JVM stats)
    private JLabel heapMemoryLabel;
    private JLabel nonHeapMemoryLabel;
    private JLabel activeThreadsLabel;
    private JLabel peakThreadsLabel;
    private JLabel uptimeLabel;
    
    // Host panel labels (System stats)
    private JLabel cpuUsageLabel;
    private JLabel systemMemoryLabel;
    private JLabel diskSpaceLabel;
    private JLabel cpuCoresLabel;
    private JLabel systemUptimeLabel;
    private JLabel batteryLabel;
    
    // Graph panels
    private StatusGraphPanel cpuUsageGraph;
    private StatusGraphPanel systemMemoryGraph;
    private StatusGraphPanel heapMemoryGraph;
    private StatusGraphPanel threadCountGraph;
    
    // Add fields for CPU calculation
    private long lastCpuTime = 0;
    private long lastCpuTimeNanos = 0;
    
    public StatusDialog(Main mainInstance) {
        this.mainInstance = mainInstance;
        initComponents();
        startLiveUpdates();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        // Create tabbed pane
        tabbedPane = new JTabbedPane();
        
        // System Information tab
        tabbedPane.addTab("System", createSystemPanel());
        
        // Performance tab (JVM stats)
        tabbedPane.addTab("Performance", createPerformancePanel());
        
        // Host tab (System stats)
        tabbedPane.addTab("Host", createHostPanel());
        
        // Graphs tab (Real-time charts)
        tabbedPane.addTab("Graphs", createGraphsPanel());
        
        // Add tabbed pane to main panel
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    private void startLiveUpdates() {
        // Update every 500ms for better real-time monitoring
        updateTimer = new Timer(500, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!isUpdating.get()) {
                    updateAllPanels();
                }
            }
        });
        updateTimer.start();
        
        // Initial update
        updateAllPanels();
    }
    
    private void updateAllPanels() {
        if (isUpdating.compareAndSet(false, true)) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        updateSystemPanel();
                        updatePerformancePanel();
                        updateHostPanel();
                        updateGraphs();
                    } catch (Exception e) {
                        Logger.error("Error updating status panels", e);
                    } finally {
                        isUpdating.set(false);
                    }
                }
            });
        }
    }
    
    // Helper method to create consistent panel layouts
    private JPanel createPanelWithBorder(int top, int left, int bottom, int right) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        return panel;
    }
    
    // Helper method to create consistent GridBagConstraints
    private GridBagConstraints createGridBagConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new java.awt.Insets(2, 5, 2, 5);
        return c;
    }
    
    // Helper method to add a label pair to a panel
    private void addLabelPair(JPanel panel, GridBagConstraints c, int row, String labelText, JLabel valueLabel) {
        c.gridx = 0; c.gridy = row;
        panel.add(new JLabel(labelText), c);
        c.gridx = 1; c.gridy = row;
        panel.add(valueLabel, c);
    }
    
    private JPanel createSystemPanel() {
        JPanel panel = createPanelWithBorder(10, 10, 10, 10);
        GridBagConstraints c = createGridBagConstraints();
        
        javaVersionLabel = new JLabel();
        osLabel = new JLabel();
        emulatorVersionLabel = new JLabel();
        configDirLabel = new JLabel();
        screenResLabel = new JLabel();
        networkInterfacesLabel = new JLabel();
        
        addLabelPair(panel, c, 0, "Java Version:", javaVersionLabel);
        addLabelPair(panel, c, 1, "Operating System:", osLabel);
        addLabelPair(panel, c, 2, "Emulator Version:", emulatorVersionLabel);
        addLabelPair(panel, c, 3, "Config Directory:", configDirLabel);
        addLabelPair(panel, c, 4, "Screen Resolution:", screenResLabel);
        addLabelPair(panel, c, 5, "Network Interfaces:", networkInterfacesLabel);
        
        return panel;
    }
    
    private JPanel createGraphsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Create graph panels with smaller sizes for compact display
        cpuUsageGraph = new StatusGraphPanel("CPU Usage", new Color(255, 100, 100), new Color(255, 200, 200, 100), 30, 100, true);
        systemMemoryGraph = new StatusGraphPanel("System Memory", new Color(100, 150, 255), new Color(200, 220, 255, 100), 30, 0, true);
        heapMemoryGraph = new StatusGraphPanel("Heap Memory", new Color(100, 255, 100), new Color(200, 255, 200, 100), 30, 0, true);
        threadCountGraph = new StatusGraphPanel("Thread Count", new Color(255, 150, 100), new Color(255, 220, 200, 100), 30, 0, true);
        
        // Set smaller preferred sizes for compact graphs
        Dimension graphSize = new Dimension(200, 150);
        cpuUsageGraph.setPreferredSize(graphSize);
        systemMemoryGraph.setPreferredSize(graphSize);
        heapMemoryGraph.setPreferredSize(graphSize);
        threadCountGraph.setPreferredSize(graphSize);
        
        // Add graphs to panel
        panel.add(cpuUsageGraph);
        panel.add(systemMemoryGraph);
        panel.add(heapMemoryGraph);
        panel.add(threadCountGraph);
        
        return panel;
    }
    
    private JPanel createHostPanel() {
        JPanel panel = createPanelWithBorder(10, 10, 10, 10);
        GridBagConstraints c = createGridBagConstraints();
        
        cpuUsageLabel = new JLabel();
        cpuCoresLabel = new JLabel();
        systemMemoryLabel = new JLabel();
        diskSpaceLabel = new JLabel();
        systemUptimeLabel = new JLabel();
        batteryLabel = new JLabel();
        
        addLabelPair(panel, c, 0, "CPU Usage:", cpuUsageLabel);
        addLabelPair(panel, c, 1, "CPU Cores:", cpuCoresLabel);
        addLabelPair(panel, c, 2, "System Memory:", systemMemoryLabel);
        addLabelPair(panel, c, 3, "Disk Space:", diskSpaceLabel);
        addLabelPair(panel, c, 4, "System Uptime:", systemUptimeLabel);
        addLabelPair(panel, c, 5, "Battery:", batteryLabel);
        
        return panel;
    }
    
    private JPanel createPerformancePanel() {
        JPanel panel = createPanelWithBorder(10, 10, 10, 10);
        GridBagConstraints c = createGridBagConstraints();
        
        heapMemoryLabel = new JLabel();
        nonHeapMemoryLabel = new JLabel();
        activeThreadsLabel = new JLabel();
        peakThreadsLabel = new JLabel();
        uptimeLabel = new JLabel();
        
        addLabelPair(panel, c, 0, "Heap Memory:", heapMemoryLabel);
        addLabelPair(panel, c, 1, "Non-Heap Memory:", nonHeapMemoryLabel);
        addLabelPair(panel, c, 2, "Active Threads:", activeThreadsLabel);
        addLabelPair(panel, c, 3, "Peak Thread Count:", peakThreadsLabel);
        addLabelPair(panel, c, 4, "JVM Uptime:", uptimeLabel);
        
        return panel;
    }
    
    private void updateSystemPanel() {
        if (javaVersionLabel != null) {
            try {
                // Java Version - with fallback for missing properties
                String javaVersion = System.getProperty("java.version", "Unknown");
                String javaVendor = System.getProperty("java.vendor", "Unknown");
                javaVersionLabel.setText(javaVersion + " (" + javaVendor + ")");
                
                // Operating System - with fallback for missing properties
                String osName = System.getProperty("os.name", "Unknown");
                String osVersion = System.getProperty("os.version", "Unknown");
                String osArch = System.getProperty("os.arch", "Unknown");
                osLabel.setText(osName + " " + osVersion + " (" + osArch + ")");
                
                // Emulator Version - using BuildVersion safely
                try {
                    emulatorVersionLabel.setText("JarEngine " + BuildVersion.getVersion());
                } catch (Exception e) {
                    emulatorVersionLabel.setText("JarEngine (version unknown)");
                }
                
                // Config Directory - with proper error handling
                try {
                    File configPath = Config.getConfigPath();
                    if (configPath != null && configPath.exists()) {
                        configDirLabel.setText(configPath.getAbsolutePath());
                    } else {
                        configDirLabel.setText("Not available");
                    }
                } catch (Exception e) {
                    configDirLabel.setText("Error: " + e.getMessage());
                }
                
                // Screen Resolution - with fallback
                try {
                    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    screenResLabel.setText(screenSize.width + "x" + screenSize.height);
                } catch (Exception e) {
                    screenResLabel.setText("Unknown");
                }
                
                // Network Interfaces - with improved error handling
                networkInterfacesLabel.setText(getNetworkInterfaces());
                
            } catch (Exception e) {
                Logger.error("Error updating system panel", e);
            }
        }
    }
    
    private void updatePerformancePanel() {
        if (heapMemoryLabel != null) {
            try {
                MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
                MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
                MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
                
                // Handle heap memory
                long heapMax = heapUsage.getMax();
                String heapMaxStr = heapMax > 0 ? formatBytes(heapMax) : "Unlimited";
                int heapPercent = heapMax > 0 ? (int)(heapUsage.getUsed() * 100 / heapMax) : 0;
                heapMemoryLabel.setText(formatBytes(heapUsage.getUsed()) + " / " + heapMaxStr + " (" + heapPercent + "%)");
                
                // Handle non-heap memory
                long nonHeapMax = nonHeapUsage.getMax();
                String nonHeapMaxStr;
                int nonHeapPercent;
                
                if (nonHeapMax == -1) {
                    nonHeapMaxStr = "No Limit";
                    nonHeapPercent = 0;
                } else if (nonHeapMax == 0) {
                    nonHeapMaxStr = "Unknown";
                    nonHeapPercent = 0;
                } else {
                    nonHeapMaxStr = formatBytes(nonHeapMax);
                    nonHeapPercent = (int)(nonHeapUsage.getUsed() * 100 / nonHeapMax);
                }
                
                nonHeapMemoryLabel.setText(formatBytes(nonHeapUsage.getUsed()) + " / " + nonHeapMaxStr + " (" + nonHeapPercent + "%)");
                
                // Thread information
                ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
                activeThreadsLabel.setText(String.valueOf(threadBean.getThreadCount()));
                peakThreadsLabel.setText(String.valueOf(threadBean.getPeakThreadCount()));
                
                // JVM Uptime
                long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
                uptimeLabel.setText(formatDuration(uptime));
                
            } catch (Exception e) {
                Logger.error("Error updating performance panel", e);
            }
        }
    }
    
    private void updateGraphs() {
        if (cpuUsageGraph != null) {
            try {
                // Update CPU usage graph with REAL data only
                double cpuUsage = getCpuUsage();
                if (cpuUsage >= 0) {
                    cpuUsageGraph.addDataPoint(cpuUsage);
                }
                
                // Update system memory graph with REAL data only
                long totalMemory = getTotalPhysicalMemory();
                long freeMemory = getFreePhysicalMemory();
                if (totalMemory > 0 && freeMemory >= 0) {
                    long usedMemory = totalMemory - freeMemory;
                    double memoryPercentage = (usedMemory * 100.0) / totalMemory;
                    systemMemoryGraph.addDataPoint(memoryPercentage);
                }
                
                // Update heap memory graph with REAL data only
                MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
                MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
                long heapMax = heapUsage.getMax();
                if (heapMax > 0) {
                    // Heap has a defined maximum - calculate percentage
                    double heapPercentage = (heapUsage.getUsed() * 100.0) / heapMax;
                    heapMemoryGraph.addDataPoint(heapPercentage);
                } else {
                    // Unlimited heap - show used memory as absolute value (normalized)
                    long usedMemory = heapUsage.getUsed();
                    // Normalize to a reasonable scale (e.g., 0-100 based on typical usage)
                    double normalizedUsage = Math.min((usedMemory / (1024.0 * 1024.0 * 1024.0)) * 10, 100.0); // Scale: 1GB = 10%
                    heapMemoryGraph.addDataPoint(normalizedUsage);
                }
                
                // Update thread count graph with REAL data only
                ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
                int threadCount = threadBean.getThreadCount();
                threadCountGraph.addDataPoint(threadCount);
                
            } catch (Exception e) {
                Logger.error("Error updating graphs", e);
            }
        }
    }
    
    private void updateHostPanel() {
        if (cpuUsageLabel != null) {
            try {
                // CPU Usage
                double cpuUsage = getCpuUsage();
                if (cpuUsage >= 0) {
                    cpuUsageLabel.setText(String.format("%.1f%%", cpuUsage));
                } else {
                    cpuUsageLabel.setText("N/A");
                }
                
                // CPU Cores
                cpuCoresLabel.setText(String.valueOf(Runtime.getRuntime().availableProcessors()));
                
                // System Memory
                long totalMemory = getTotalPhysicalMemory();
                long freeMemory = getFreePhysicalMemory();
                if (totalMemory > 0 && freeMemory >= 0) {
                    long usedMemory = totalMemory - freeMemory;
                    int memoryPercent = (int)Math.round((usedMemory * 100.0) / totalMemory);
                    systemMemoryLabel.setText(formatBytes(usedMemory) + " / " + formatBytes(totalMemory) + " (" + memoryPercent + "%)");
                } else {
                    systemMemoryLabel.setText("N/A");
                }
                
                // Disk Space - Handle cross-platform compatibility
                File root = null;
                String os = System.getProperty("os.name", "").toLowerCase();
                if (os.contains("windows")) {
                    root = new File("C:\\");
                } else {
                    root = new File("/");
                }
                
                long totalSpace = root.getTotalSpace();
                long freeSpace = root.getFreeSpace();
                if (totalSpace > 0 && freeSpace >= 0) {
                    long usedSpace = totalSpace - freeSpace;
                    int diskPercent = (int)Math.round((usedSpace * 100.0) / totalSpace);
                    diskSpaceLabel.setText(formatBytes(usedSpace) + " / " + formatBytes(totalSpace) + " (" + diskPercent + "%)");
                } else {
                    diskSpaceLabel.setText("N/A");
                }
                
                // System Uptime
                long uptime = getSystemUptime();
                systemUptimeLabel.setText(formatDuration(uptime));
                
                // Battery Status
                batteryLabel.setText(getBatteryStatus());
                
            } catch (Exception e) {
                Logger.error("Error updating host panel", e);
            }
        }
    }
    
    private double getCpuUsage() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            
            // Try to get system-wide CPU load (most accurate)
            try {
                Class<?> sunOsBeanClass = getSunOsBeanClass();
                if (sunOsBeanClass != null && sunOsBeanClass.isInstance(osBean)) {
                    Method getSystemCpuLoadMethod = sunOsBeanClass.getMethod("getSystemCpuLoad");
                    Object result = getSystemCpuLoadMethod.invoke(osBean);
                    if (result instanceof Double) {
                        Double systemCpuLoad = (Double) result;
                        if (systemCpuLoad >= 0) {
                            double cpuUsage = Math.min(systemCpuLoad * 100.0, 100.0);
                            return cpuUsage;
                        }
                    }
                }
            } catch (NoSuchMethodException | SecurityException e) {
                // Method not available
            } catch (Exception e) {
                // Reflection failed, continue with alternative
            }
            
            // Use system load average (accurate for Unix-like systems)
            double loadAverage = osBean.getSystemLoadAverage();
            if (loadAverage >= 0) {
                int cores = Runtime.getRuntime().availableProcessors();
                double percentage = (loadAverage / cores) * 100;
                double cpuUsage = Math.min(percentage, 100.0);
                return cpuUsage;
            }
            
            // Fallback to process CPU time (least accurate but always available)
            try {
                Class<?> sunOsBeanClass = getSunOsBeanClass();
                if (sunOsBeanClass != null && sunOsBeanClass.isInstance(osBean)) {
                    Method getProcessCpuTimeMethod = sunOsBeanClass.getMethod("getProcessCpuTime");
                    Object result = getProcessCpuTimeMethod.invoke(osBean);
                    if (result instanceof Long) {
                        long currentCpuTime = (Long) result;
                        long currentTime = System.nanoTime();
                        
                        if (lastCpuTime == 0) {
                            lastCpuTime = currentCpuTime;
                            lastCpuTimeNanos = currentTime;
                            return 0.0;
                        }
                        
                        long cpuTimeDelta = currentCpuTime - lastCpuTime;
                        long timeDelta = currentTime - lastCpuTimeNanos;
                        
                        if (timeDelta > 0) {
                            double cpuUsage = (cpuTimeDelta * 100.0) / timeDelta;
                            
                            lastCpuTime = currentCpuTime;
                            lastCpuTimeNanos = currentTime;
                            
                            return Math.min(Math.max(cpuUsage, 0.0), 100.0);
                        }
                    }
                }
            } catch (Exception e) {
                // Process CPU time method failed
            }
            
        } catch (Exception e) {
            Logger.error("Error getting CPU usage", e);
        }
        return -1;
    }
    
    private Class<?> getSunOsBeanClass() {
        try {
            return Class.forName("com.sun.management.OperatingSystemMXBean");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
    
    private long getTotalPhysicalMemory() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            
            try {
                Class<?> sunOsBeanClass = getSunOsBeanClass();
                if (sunOsBeanClass != null && sunOsBeanClass.isInstance(osBean)) {
                    Method getTotalPhysicalMemorySizeMethod = sunOsBeanClass.getMethod("getTotalPhysicalMemorySize");
                    Object result = getTotalPhysicalMemorySizeMethod.invoke(osBean);
                    if (result instanceof Long) {
                        return (Long) result;
                    }
                }
            } catch (NoSuchMethodException | SecurityException e) {
                // Method not available
            } catch (Exception e) {
                // Reflection failed
            }
            
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            if (maxMemory != Long.MAX_VALUE) {
                return maxMemory * 4;
            }
            
        } catch (Exception e) {
            Logger.error("Error getting total physical memory", e);
        }
        return -1;
    }
    
    private long getFreePhysicalMemory() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            
            try {
                Class<?> sunOsBeanClass = getSunOsBeanClass();
                if (sunOsBeanClass != null && sunOsBeanClass.isInstance(osBean)) {
                    Method getFreePhysicalMemorySizeMethod = sunOsBeanClass.getMethod("getFreePhysicalMemorySize");
                    Object result = getFreePhysicalMemorySizeMethod.invoke(osBean);
                    if (result instanceof Long) {
                        return (Long) result;
                    }
                }
            } catch (NoSuchMethodException | SecurityException e) {
                // Method not available
            } catch (Exception e) {
                // Reflection failed
            }
            
            Runtime runtime = Runtime.getRuntime();
            long freeMemory = runtime.freeMemory();
            long totalMemory = runtime.totalMemory();
            long usedMemory = totalMemory - freeMemory;
            
            if (usedMemory > 0) {
                long estimatedFreePhysical = (freeMemory * 4);
                return Math.max(estimatedFreePhysical, 0);
            }
            
        } catch (Exception e) {
            Logger.error("Error getting free physical memory", e);
        }
        return -1;
    }
    
    private long getSystemUptime() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            
            try {
                Class<?> sunOsBeanClass = getSunOsBeanClass();
                if (sunOsBeanClass != null && sunOsBeanClass.isInstance(osBean)) {
                    Method getSystemUptimeMethod = sunOsBeanClass.getMethod("getSystemUptime");
                    Object result = getSystemUptimeMethod.invoke(osBean);
                    if (result instanceof Long) {
                        return (Long) result;
                    }
                }
            } catch (NoSuchMethodException | SecurityException e) {
                // Method not available
            } catch (Exception e) {
                // Reflection failed
            }
            
        } catch (Exception e) {
            Logger.error("Error getting system uptime", e);
        }
        return ManagementFactory.getRuntimeMXBean().getUptime();
    }
    
    private String getBatteryStatus() {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("linux")) {
                return getLinuxBatteryStatus();
            } else if (os.contains("windows")) {
                return "N/A (Windows)";
            } else if (os.contains("mac")) {
                return "N/A (macOS)";
            } else {
                return "N/A";
            }
        } catch (Exception e) {
            Logger.error("Error getting battery status", e);
        }
        return "N/A";
    }
    
    private String getLinuxBatteryStatus() {
        try {
            Path batteryDir = Paths.get("/sys/class/power_supply");
            if (!Files.exists(batteryDir)) {
                return "N/A (No power supply info)";
            }
            
            List<String> batteryInfo = new ArrayList<>();
            Path batteryPath = null;
            
            // Find the first battery directory
            java.util.stream.Stream<Path> paths = Files.list(batteryDir);
            try {
                batteryPath = paths.filter(Files::isDirectory)
                                  .filter(path -> path.getFileName().toString().contains("BAT"))
                                  .findFirst()
                                  .orElse(null);
            } finally {
                paths.close();
            }
            
            if (batteryPath == null) {
                return "N/A (No battery found)";
            }
            
            // Read battery status
            Path statusFile = batteryPath.resolve("status");
            if (Files.exists(statusFile) && Files.isReadable(statusFile)) {
                java.util.stream.Stream<String> lines = Files.lines(statusFile);
                try {
                    String status = lines.findFirst().orElse("Unknown");
                    batteryInfo.add(status);
                } finally {
                    lines.close();
                }
            }
            
            // Read battery capacity
            Path capacityFile = batteryPath.resolve("capacity");
            if (Files.exists(capacityFile) && Files.isReadable(capacityFile)) {
                java.util.stream.Stream<String> lines = Files.lines(capacityFile);
                try {
                    String capacity = lines.findFirst().orElse("");
                    if (!capacity.isEmpty()) {
                        batteryInfo.add(capacity + "%");
                    }
                } finally {
                    lines.close();
                }
            }
            
            return batteryInfo.isEmpty() ? "N/A (No battery info)" : String.join(" ", batteryInfo);
            
        } catch (Exception e) {
            return "N/A (Error)";
        }
    }
    
    private String getNetworkInterfaces() {
        StringBuilder sb = new StringBuilder();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            List<String> activeInterfaces = new ArrayList<>();
            
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isUp() && !ni.isLoopback() && !ni.isVirtual()) {
                    String displayName = ni.getDisplayName();
                    if (displayName != null && !displayName.isEmpty()) {
                        activeInterfaces.add(displayName);
                    }
                }
            }
            
            if (activeInterfaces.isEmpty()) {
                sb.append("No active interfaces");
            } else {
                sb.append(String.join(", ", activeInterfaces));
            }
            
        } catch (SocketException e) {
            sb.append("Error retrieving network interfaces");
        }
        return sb.toString();
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 0) return "0 B";
        if (bytes == 0) return "0 B";
        
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB", "PB" };
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        digitGroups = Math.min(digitGroups, units.length - 1);
        digitGroups = Math.max(digitGroups, 0); // Ensure non-negative
        
        double value = bytes / Math.pow(1024, digitGroups);
        // Handle very small values that might round to 0
        if (value < 0.1 && digitGroups > 0) {
            value = bytes / Math.pow(1024, digitGroups - 1);
            digitGroups--;
        }
        return String.format("%.1f %s", value, units[digitGroups]);
    }
    
    private String formatDuration(long millis) {
        if (millis < 0) return "0s";
        
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m " + (seconds % 60) + "s";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
    
    @Override
    public void removeNotify() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
        super.removeNotify();
    }
}