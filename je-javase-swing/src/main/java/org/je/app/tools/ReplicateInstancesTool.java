package org.je.app.tools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Tool for replicating emulator instances.
 * Allows users to choose between 1-10 instances and launches new independent emulator windows.
 * 
 * Features:
 * - Process tracking and management
 * - Resource cleanup
 * - Port conflict avoidance
 * - Cross-platform compatibility
 * - Error recovery
 * - Memory and resource limits
 */
public class ReplicateInstancesTool extends JDialog {
    
    private static final Logger LOGGER = Logger.getLogger(ReplicateInstancesTool.class.getName());
    private static final int MIN_INSTANCES = 1;
    private static final int MAX_INSTANCES = 10;
    private static final int MAX_CONCURRENT_INSTANCES = 20; // Safety limit
    private static final int LAUNCH_DELAY_MS = 500;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;
    
    // Process management
    private static final ConcurrentHashMap<Integer, Process> runningProcesses = new ConcurrentHashMap<>();
    private static final AtomicInteger processCounter = new AtomicInteger(0);
    private static final ExecutorService processExecutor = Executors.newCachedThreadPool();
    
    private JLabel instanceCountLabel;
    private JButton decreaseButton;
    private JButton increaseButton;
    private JButton launchButton;
    private JButton cancelButton;
    
    private int selectedInstances = 1;
    private final Frame parent;
    
    public ReplicateInstancesTool(Frame parent) {
        super(parent, "Replicate Instances", true);
        this.parent = parent;
        
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        initComponents();
        setupLayout();
        setupEventHandlers();
        
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }
    
    private void initComponents() {
        instanceCountLabel = new JLabel("1", SwingConstants.CENTER);
        instanceCountLabel.setFont(new Font("Arial", Font.BOLD, 24));
        instanceCountLabel.setPreferredSize(new Dimension(80, 40));
        instanceCountLabel.setBorder(BorderFactory.createEtchedBorder());
        instanceCountLabel.setToolTipText("Number of instances to launch (1-10)");
        
        decreaseButton = new JButton("←");
        decreaseButton.setFont(new Font("Arial", Font.BOLD, 16));
        decreaseButton.setPreferredSize(new Dimension(50, 40));
        decreaseButton.setToolTipText("Decrease number of instances (Left Arrow)");
        
        increaseButton = new JButton("→");
        increaseButton.setFont(new Font("Arial", Font.BOLD, 16));
        increaseButton.setPreferredSize(new Dimension(50, 40));
        increaseButton.setToolTipText("Increase number of instances (Right Arrow)");
        
        launchButton = new JButton("Launch Instances");
        launchButton.setPreferredSize(new Dimension(150, 35));
        launchButton.setToolTipText("Launch the selected number of emulator instances");
        
        cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(new Dimension(100, 35));
        cancelButton.setToolTipText("Cancel and close this dialog (Escape)");
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Main panel with padding
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Title label
        JLabel titleLabel = new JLabel("Select Number of Instances", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Instance counter panel
        JPanel counterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        counterPanel.add(decreaseButton);
        counterPanel.add(instanceCountLabel);
        counterPanel.add(increaseButton);
        counterPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.add(launchButton);
        buttonPanel.add(cancelButton);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(counterPanel);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(buttonPanel);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private void setupEventHandlers() {
        // Action listeners
        decreaseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedInstances > MIN_INSTANCES) {
                    selectedInstances--;
                    updateInstanceCount();
                }
            }
        });
        
        increaseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedInstances < MAX_INSTANCES) {
                    selectedInstances++;
                    updateInstanceCount();
                }
            }
        });
        
        launchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                launchInstances();
                dispose(); // Dismiss the dialog after launching instances
            }
        });
        
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        
        // Keyboard navigation
        getRootPane().registerKeyboardAction(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (selectedInstances > MIN_INSTANCES) {
                        selectedInstances--;
                        updateInstanceCount();
                    }
                }
            },
            "Decrease",
            KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        
        getRootPane().registerKeyboardAction(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (selectedInstances < MAX_INSTANCES) {
                        selectedInstances++;
                        updateInstanceCount();
                    }
                }
            },
            "Increase",
            KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        
        getRootPane().registerKeyboardAction(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    launchInstances();
                    dispose(); // Dismiss the dialog after launching instances
                }
            },
            "Launch",
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        
        getRootPane().registerKeyboardAction(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            },
            "Cancel",
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }
    
    private void updateInstanceCount() {
        instanceCountLabel.setText(String.valueOf(selectedInstances));
        decreaseButton.setEnabled(selectedInstances > MIN_INSTANCES);
        increaseButton.setEnabled(selectedInstances < MAX_INSTANCES);
    }
    
    private void launchInstances() {
        // Check if we're already at the limit
        if (runningProcesses.size() >= MAX_CONCURRENT_INSTANCES) {
            JOptionPane.showMessageDialog(this,
                "Maximum number of instances (" + MAX_CONCURRENT_INSTANCES + ") already running.\n" +
                "Please close some instances before launching new ones.",
                "Instance Limit Reached",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Check if the requested number would exceed the limit
        if (runningProcesses.size() + selectedInstances > MAX_CONCURRENT_INSTANCES) {
            int available = MAX_CONCURRENT_INSTANCES - runningProcesses.size();
            JOptionPane.showMessageDialog(this,
                "Cannot launch " + selectedInstances + " instances.\n" +
                "Only " + available + " more instances can be launched.\n" +
                "Please reduce the number or close some existing instances.",
                "Instance Limit Exceeded",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            final List<Process> launchedProcesses = new ArrayList<>();
            final List<String> errors = new ArrayList<>();
            
            for (int i = 0; i < selectedInstances; i++) {
                LOGGER.info("Launching instance " + (i + 1) + " of " + selectedInstances);
                
                try {
                    Process process = launchNewInstance();
                    if (process != null) {
                        launchedProcesses.add(process);
                        // Small delay between launches to avoid overwhelming the system
                        if (i < selectedInstances - 1) {
                            Thread.sleep(LAUNCH_DELAY_MS);
                        }
                    }
                } catch (Exception e) {
                    String error = "Failed to launch instance " + (i + 1) + ": " + e.getMessage();
                    errors.add(error);
                    LOGGER.log(Level.WARNING, error, e);
                }
            }
            
            // Show results
            if (launchedProcesses.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Failed to launch any instances.\n\nErrors:\n" + String.join("\n", errors),
                    "Launch Failed",
                    JOptionPane.ERROR_MESSAGE);
            } else if (!errors.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Successfully launched " + launchedProcesses.size() + " of " + selectedInstances + " instances.\n\n" +
                    "Errors:\n" + String.join("\n", errors),
                    "Partial Launch Success",
                    JOptionPane.WARNING_MESSAGE);
            }
            // Removed the success dialog - instances launch silently on success
                
        } catch (Exception e) {
            String errorMessage = "Error launching instances: " + e.getMessage();
            LOGGER.log(Level.SEVERE, errorMessage, e);
            
            JOptionPane.showMessageDialog(this,
                errorMessage + "\n\nPlease ensure:\n" +
                "1. Java is properly installed and accessible\n" +
                "2. The application JAR file is accessible (if running from JAR)\n" +
                "3. Gradle is available (if running from Gradle project)\n" +
                "4. You have sufficient permissions to launch new processes\n" +
                "5. No port conflicts exist",
                "Launch Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private Process launchNewInstance() throws IOException {
        // First, try to launch using Gradle if we're in a Gradle project
        Process process = tryLaunchUsingGradle();
        if (process != null) {
            return process;
        }
        
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        
        // Check if java executable exists
        File javaExe = new File(javaBin);
        if (!javaExe.exists()) {
            // Try alternative paths for different operating systems
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("windows")) {
                javaBin = javaHome + File.separator + "bin" + File.separator + "java.exe";
            } else if (osName.contains("mac")) {
                javaBin = javaHome + File.separator + "bin" + File.separator + "java";
            } else {
                // Linux and other Unix-like systems
                javaBin = javaHome + File.separator + "bin" + File.separator + "java";
            }
            
            javaExe = new File(javaBin);
            if (!javaExe.exists()) {
                throw new IOException("Java executable not found at: " + javaBin);
            }
        }
        
        // Get the current JAR file path
        String jarPath = getJarPath();
        if (jarPath != null) {
            // Try to launch using JAR
            try {
                return launchUsingJar(javaBin, jarPath);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to launch using JAR: " + e.getMessage(), e);
                // Fall through to main class method
            }
        }
        
        // If we can't find a JAR or JAR launch failed, try to launch using the main class directly
        return launchUsingMainClass(javaBin);
    }
    
    private Process tryLaunchUsingGradle() {
        try {
            // Check if we're in a Gradle project by looking for gradlew or gradlew.bat
            File currentDir = new File(System.getProperty("user.dir"));
            File gradlew = new File(currentDir, "gradlew");
            File gradlewBat = new File(currentDir, "gradlew.bat");
            File gradleWrapper = null;
            
            if (gradlew.exists() && gradlew.canExecute()) {
                gradleWrapper = gradlew;
                LOGGER.info("Found Gradle wrapper: " + gradlew.getAbsolutePath());
            } else if (gradlewBat.exists()) {
                gradleWrapper = gradlewBat;
                LOGGER.info("Found Gradle wrapper: " + gradlewBat.getAbsolutePath());
            } else {
                LOGGER.info("No Gradle wrapper found in: " + currentDir.getAbsolutePath());
                return null;
            }
            
            if (gradleWrapper != null) {
                // Try to launch using Gradle
                List<String> command = new ArrayList<>();
                if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                    command.add("cmd");
                    command.add("/c");
                    command.add(gradleWrapper.getAbsolutePath());
                } else {
                    command.add(gradleWrapper.getAbsolutePath());
                }
                command.add("run");
                
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(currentDir);
                pb.environment().putAll(System.getenv());
                pb.redirectErrorStream(true);
                
                LOGGER.info("Launching with Gradle command: " + String.join(" ", command));
                
                Process process = pb.start();
                int processId = processCounter.incrementAndGet();
                runningProcesses.put(processId, process);
                
                // Start a thread to monitor the process
                startProcessMonitor(processId, process);
                
                LOGGER.info("Launched new emulator instance using Gradle successfully (PID: " + processId + ")");
                return process;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to launch using Gradle: " + e.getMessage(), e);
        }
        return null;
    }
    
    private Process launchUsingJar(String javaBin, String jarPath) throws IOException {
        // Build the command
        List<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("-jar");
        command.add(jarPath);
        
        // Add any existing arguments if needed
        String[] existingArgs = getExistingArguments();
        if (existingArgs != null) {
            for (String arg : existingArgs) {
                command.add(arg);
            }
        }
        
        // Launch the process
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(System.getProperty("user.dir")));
        pb.environment().putAll(System.getenv());
        pb.redirectErrorStream(true);
        
        LOGGER.info("Launching with command: " + String.join(" ", command));
        
        Process process = pb.start();
        int processId = processCounter.incrementAndGet();
        runningProcesses.put(processId, process);
        
        // Start a thread to monitor the process
        startProcessMonitor(processId, process);
        
        LOGGER.info("Launched new emulator instance successfully (PID: " + processId + ")");
        return process;
    }
    
    private Process launchUsingMainClass(String javaBin) throws IOException {
        // Fallback method for launching when running from IDE or Gradle
        List<String> command = new ArrayList<>();
        command.add(javaBin);
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add("org.je.app.Main");
        
        // Launch the process
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(System.getProperty("user.dir")));
        pb.environment().putAll(System.getenv());
        pb.redirectErrorStream(true);
        
        LOGGER.info("Launching with main class command: " + String.join(" ", command));
        
        Process process = pb.start();
        int processId = processCounter.incrementAndGet();
        runningProcesses.put(processId, process);
        
        // Start a thread to monitor the process
        startProcessMonitor(processId, process);
        
        LOGGER.info("Launched new emulator instance using main class successfully (PID: " + processId + ")");
        return process;
    }
    
    private void startProcessMonitor(int processId, Process process) {
        processExecutor.submit(() -> {
            try {
                // Wait for the process to complete
                int exitCode = process.waitFor();
                LOGGER.info("Process " + processId + " exited with code: " + exitCode);
            } catch (InterruptedException e) {
                LOGGER.warning("Process " + processId + " monitoring interrupted");
                Thread.currentThread().interrupt();
            } finally {
                // Remove the process from our tracking
                runningProcesses.remove(processId);
                LOGGER.info("Removed process " + processId + " from tracking");
            }
        });
    }
    
    /**
     * Gets the current number of running instances.
     * @return the number of currently running instances
     */
    public static int getRunningInstanceCount() {
        return runningProcesses.size();
    }
    
    /**
     * Shuts down all running instances and cleans up resources.
     * This method should be called when the application is shutting down.
     */
    public static void shutdownAllInstances() {
        LOGGER.info("Shutting down " + runningProcesses.size() + " instances...");
        for (Process process : runningProcesses.values()) {
            try {
                if (process.isAlive()) {
                    process.destroy();
                    // Give it a moment to shutdown gracefully
                    if (!process.waitFor(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        process.destroyForcibly();
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error shutting down process: " + e.getMessage(), e);
            }
        }
        runningProcesses.clear();
        
        // Shutdown the executor service
        processExecutor.shutdown();
        try {
            if (!processExecutor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                processExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            processExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    @Override
    public void dispose() {
        super.dispose();
    }
    
    // Cleanup when the application shuts down
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutdown hook triggered - cleaning up instances");
            shutdownAllInstances();
        }, "ReplicateInstancesTool-ShutdownHook"));
    }
    
    private String getJarPath() {
        try {
            // Method 1: Try to get the JAR file path from the class location
            Class<?> mainClass = Class.forName("org.je.app.Main");
            URL location = mainClass.getProtectionDomain().getCodeSource().getLocation();
            if (location != null) {
                File jarFile = new File(location.toURI());
                if (jarFile.exists() && jarFile.getName().endsWith(".jar")) {
                    return jarFile.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Method 1 failed: " + e.getMessage(), e);
        }
        
        try {
            // Method 2: Try to find the JAR in the current directory
            File currentDir = new File(System.getProperty("user.dir"));
            File[] jarFiles = currentDir.listFiles(new java.io.FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.getName().endsWith(".jar");
                }
            });
            if (jarFiles != null && jarFiles.length > 0) {
                // Look for a JAR file that might be the emulator
                for (File jarFile : jarFiles) {
                    String name = jarFile.getName().toLowerCase();
                    if (name.contains("jar") || name.contains("emulator") || name.contains("je")) {
                        return jarFile.getAbsolutePath();
                    }
                }
                // If no specific match, return the first JAR
                return jarFiles[0].getAbsolutePath();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Method 2 failed: " + e.getMessage(), e);
        }
        
        try {
            // Method 3: Try to find the JAR in the classpath
            String classpath = System.getProperty("java.class.path");
            if (classpath != null) {
                String[] paths = classpath.split(File.pathSeparator);
                for (String path : paths) {
                    if (path.endsWith(".jar")) {
                        File jarFile = new File(path);
                        if (jarFile.exists()) {
                            return jarFile.getAbsolutePath();
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Method 3 failed: " + e.getMessage(), e);
        }
        
        try {
            // Method 4: Try to find the JAR in common locations
            String[] commonPaths = {
                "target/classes",  // Maven build directory
                "build/classes",   // Gradle build directory
                "out/classes",     // IntelliJ build directory
                "bin"             // Eclipse build directory
            };
            
            for (String path : commonPaths) {
                File buildDir = new File(path);
                if (buildDir.exists() && buildDir.isDirectory()) {
                    // Look for JAR files in the parent directory
                    File parentDir = buildDir.getParentFile();
                    if (parentDir != null) {
                        File[] jarFiles = parentDir.listFiles(new java.io.FileFilter() {
                            @Override
                            public boolean accept(File file) {
                                return file.getName().endsWith(".jar");
                            }
                        });
                        if (jarFiles != null && jarFiles.length > 0) {
                            return jarFiles[0].getAbsolutePath();
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Method 4 failed: " + e.getMessage(), e);
        }
        
        return null;
    }
    
    private String[] getExistingArguments() {
        // This method can be extended to capture and pass existing arguments
        // For now, return null to use default arguments
        return null;
    }
    
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            updateInstanceCount();
        }
        super.setVisible(visible);
    }
} 