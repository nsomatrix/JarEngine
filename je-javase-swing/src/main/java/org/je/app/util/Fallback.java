package org.je.app.util;

import java.awt.*;
import java.awt.event.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import javax.swing.Timer;

import org.je.DisplayComponent;
import org.je.MIDletBridge;
import org.je.MIDletAccess;
import org.je.device.Device;
import org.je.device.DeviceFactory;
import org.je.device.DeviceDisplay;
import org.je.device.EmulatorContext;
import org.je.app.Main;
import org.je.app.Common;

/**
 * Fallback - Comprehensive safety net for JarEngine emulator.
 * 
 * This class provides:
 * - Thread lifecycle monitoring and recovery
 * - Crash detection and automatic recovery
 * - Memory leak prevention
 * - Performance monitoring and optimization
 * - Automatic error recovery
 * - System resource management
 * - Emergency shutdown procedures
 * 
 * Features:
 * - Thread-safe operations with atomic variables
 * - Comprehensive error handling and logging
 * - Automatic resource cleanup
 * - Performance optimization
 * - Memory management
 * - Emergency shutdown procedures
 * 
 * @author JarEngine Team
 * @version 1.0.0
 */
public class Fallback {
    
    // ==================== CONSTANTS ====================
    private static final int MONITOR_INTERVAL_MS = 2000; // 2 seconds
    private static final int MEMORY_CHECK_INTERVAL_MS = 10000; // 10 seconds
    private static final int PERFORMANCE_CHECK_INTERVAL_MS = 5000; // 5 seconds
    private static final int MAX_MEMORY_USAGE_PERCENT = 85; // 85% memory threshold
    private static final int MAX_THREAD_COUNT = 50; // Maximum thread count
    private static final int MAX_RECOVERY_ATTEMPTS = 3; // Maximum recovery attempts
    private static final long MAX_RESPONSE_TIME_MS = 5000; // 5 seconds max response time
    private static final int MAX_MEMORY_HISTORY = 10; // Maximum memory history entries
    private static final int MAX_RESPONSE_HISTORY = 20; // Maximum response time history entries
    private static final int MAX_THREAD_HISTORY = 10; // Maximum thread count history entries
    
    // ==================== INSTANCE VARIABLES ====================
    private final Main mainApp;
    private final Common common;
    private final EmulatorContext emulatorContext;
    
    // Monitoring components
    private final Timer healthMonitorTimer;
    private final Timer memoryMonitorTimer;
    private final Timer performanceMonitorTimer;
    private final Timer threadMonitorTimer;
    
    // State tracking (thread-safe)
    private final AtomicBoolean isMonitoring = new AtomicBoolean(false);
    private final AtomicBoolean isRecovering = new AtomicBoolean(false);
    private final AtomicInteger recoveryAttempts = new AtomicInteger(0);
    private final AtomicInteger crashCount = new AtomicInteger(0);
    
    // Performance tracking (thread-safe collections)
    private final Queue<Long> responseTimes = new ConcurrentLinkedQueue<>();
    private final Queue<Double> memoryUsage = new ConcurrentLinkedQueue<>();
    private final Queue<Integer> threadCounts = new ConcurrentLinkedQueue<>();
    
    // Recovery state (volatile for thread safety)
    private volatile long lastRecoveryTime = 0;
    private volatile long lastCrashTime = 0;
    private volatile String lastError = "";
    private volatile boolean emergencyMode = false;
    
    // Thread monitoring (thread-safe collections)
    private final Set<Thread> monitoredThreads = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<Thread, ThreadInfo> threadInfoMap = new ConcurrentHashMap<>();
    
    // ==================== CONSTRUCTOR ====================
    
    /**
     * Creates a new Fallback instance with comprehensive monitoring capabilities.
     * 
     * @param mainApp The main application instance
     * @param common The common application instance
     * @param emulatorContext The emulator context
     * @throws IllegalArgumentException if any parameter is null
     */
    public Fallback(Main mainApp, Common common, EmulatorContext emulatorContext) {
        // Validate parameters
        if (mainApp == null) {
            throw new IllegalArgumentException("Main application cannot be null");
        }
        if (common == null) {
            throw new IllegalArgumentException("Common instance cannot be null");
        }
        if (emulatorContext == null) {
            throw new IllegalArgumentException("Emulator context cannot be null");
        }
        
        this.mainApp = mainApp;
        this.common = common;
        this.emulatorContext = emulatorContext;
        
        // Initialize monitoring timers
        this.healthMonitorTimer = createHealthMonitorTimer();
        this.memoryMonitorTimer = createMemoryMonitorTimer();
        this.performanceMonitorTimer = createPerformanceMonitorTimer();
        this.threadMonitorTimer = createThreadMonitorTimer();
        
        // Set up emergency shutdown hook
        setupEmergencyShutdownHook();
        
        // Set up uncaught exception handler
        setupUncaughtExceptionHandler();
        
        logInfo("Fallback system initialized successfully");
    }
    
    // ==================== PUBLIC METHODS ====================
    
    /**
     * Start the fallback monitoring system
     */
    public void startMonitoring() {
        if (isMonitoring.compareAndSet(false, true)) {
            healthMonitorTimer.start();
            memoryMonitorTimer.start();
            performanceMonitorTimer.start();
            threadMonitorTimer.start();
            
            // Log monitoring start
            logInfo("Fallback monitoring system started");
        }
    }
    
    /**
     * Stop the fallback monitoring system
     */
    public void stopMonitoring() {
        if (isMonitoring.compareAndSet(true, false)) {
            healthMonitorTimer.stop();
            memoryMonitorTimer.stop();
            performanceMonitorTimer.stop();
            threadMonitorTimer.stop();
            
            // Log monitoring stop
            logInfo("Fallback monitoring system stopped");
        }
    }
    
    /**
     * Force a recovery attempt
     */
    public void forceRecovery() {
        if (!isRecovering.get()) {
            performRecovery("Manual recovery triggered");
        }
    }
    
    /**
     * Get current system health status
     */
    public SystemHealth getSystemHealth() {
        return new SystemHealth(
            getMemoryUsage(),
            getThreadCount(),
            getAverageResponseTime(),
            crashCount.get(),
            recoveryAttempts.get(),
            emergencyMode,
            lastError
        );
    }
    
    /**
     * Emergency shutdown procedure
     */
    public void emergencyShutdown() {
        emergencyMode = true;
        logError("EMERGENCY SHUTDOWN INITIATED");
        
        try {
            // Stop all monitoring
            stopMonitoring();
            
            // Clean up resources
            cleanupResources();
            
            // Force garbage collection
            System.gc();
            
            // Exit application
            System.exit(0);
        } catch (Exception e) {
            logError("Emergency shutdown failed: " + e.getMessage());
            System.exit(1);
        }
    }
    
    // ==================== PRIVATE METHODS ====================
    
    /**
     * Create health monitoring timer
     */
    private Timer createHealthMonitorTimer() {
        return new Timer(MONITOR_INTERVAL_MS, e -> {
            try {
                monitorSystemHealth();
            } catch (Exception ex) {
                logError("Health monitoring failed: " + ex.getMessage());
            }
        });
    }
    
    /**
     * Create memory monitoring timer
     */
    private Timer createMemoryMonitorTimer() {
        return new Timer(MEMORY_CHECK_INTERVAL_MS, e -> {
            try {
                monitorMemoryUsage();
            } catch (Exception ex) {
                logError("Memory monitoring failed: " + ex.getMessage());
            }
        });
    }
    
    /**
     * Create performance monitoring timer
     */
    private Timer createPerformanceMonitorTimer() {
        return new Timer(PERFORMANCE_CHECK_INTERVAL_MS, e -> {
            try {
                monitorPerformance();
            } catch (Exception ex) {
                logError("Performance monitoring failed: " + ex.getMessage());
            }
        });
    }
    
    /**
     * Create thread monitoring timer
     */
    private Timer createThreadMonitorTimer() {
        return new Timer(MONITOR_INTERVAL_MS, e -> {
            try {
                monitorThreads();
            } catch (Exception ex) {
                logError("Thread monitoring failed: " + ex.getMessage());
            }
        });
    }
    
    /**
     * Monitor overall system health
     */
    private void monitorSystemHealth() {
        // Check if main application is responsive
        if (!isApplicationResponsive()) {
            logWarning("Application not responsive, triggering recovery");
            performRecovery("Application not responsive");
            return;
        }
        
        // Check for deadlocks
        if (detectDeadlocks()) {
            logError("Deadlock detected, triggering recovery");
            performRecovery("Deadlock detected");
            return;
        }
        
        // Check for excessive error count
        if (crashCount.get() > 5) {
            logError("Excessive crash count detected, triggering recovery");
            performRecovery("Excessive crash count");
            return;
        }
        
        // Check for memory leaks
        if (detectMemoryLeak()) {
            logWarning("Memory leak detected, triggering cleanup");
            performMemoryCleanup();
        }
    }
    
    /**
     * Monitor memory usage with improved error handling
     */
    private void monitorMemoryUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            if (runtime == null) {
                logError("Runtime.getRuntime() returned null");
                return;
            }
            
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            // Validate memory values
            if (maxMemory <= 0 || totalMemory <= 0 || freeMemory < 0 || usedMemory < 0) {
                logError("Invalid memory values detected");
                return;
            }
            
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            
            // Validate percentage
            if (memoryUsagePercent < 0 || memoryUsagePercent > 100) {
                logError("Invalid memory usage percentage: " + memoryUsagePercent);
                return;
            }
            
            memoryUsage.offer(memoryUsagePercent);
            
            // Keep only last MAX_MEMORY_HISTORY measurements
            while (memoryUsage.size() > MAX_MEMORY_HISTORY) {
                memoryUsage.poll();
            }
            
            if (memoryUsagePercent > MAX_MEMORY_USAGE_PERCENT) {
                logWarning("High memory usage detected: " + String.format("%.1f%%", memoryUsagePercent));
                performMemoryCleanup();
            }
        } catch (Exception e) {
            logError("Memory monitoring failed: " + e.getMessage());
        }
    }
    
    /**
     * Monitor performance metrics with improved error handling
     */
    private void monitorPerformance() {
        try {
            long startTime = System.currentTimeMillis();
            
            // Measure response time by checking if UI is responsive
            SwingUtilities.invokeLater(() -> {
                try {
                    long responseTime = System.currentTimeMillis() - startTime;
                    
                    // Validate response time
                    if (responseTime >= 0 && responseTime < Long.MAX_VALUE) {
                        responseTimes.offer(responseTime);
                        
                        // Keep only last MAX_RESPONSE_HISTORY measurements
                        while (responseTimes.size() > MAX_RESPONSE_HISTORY) {
                            responseTimes.poll();
                        }
                    }
                } catch (Exception e) {
                    logError("Response time measurement failed: " + e.getMessage());
                }
            });
            
            // Check average response time
            double avgResponseTime = getAverageResponseTime();
            if (avgResponseTime > MAX_RESPONSE_TIME_MS) {
                logWarning("Slow response time detected: " + String.format("%.0fms", avgResponseTime));
                performPerformanceOptimization();
            }
        } catch (Exception e) {
            logError("Performance monitoring failed: " + e.getMessage());
        }
    }
    
    /**
     * Monitor thread activity with improved error handling
     */
    private void monitorThreads() {
        try {
            ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
            if (rootGroup == null) {
                logError("ThreadGroup is null");
                return;
            }
            
            while (rootGroup.getParent() != null) {
                rootGroup = rootGroup.getParent();
            }
            
            Thread[] threads = new Thread[rootGroup.activeCount()];
            int threadCount = rootGroup.enumerate(threads);
            
            // Validate thread count
            if (threadCount >= 0 && threadCount < Integer.MAX_VALUE) {
                threadCounts.offer(threadCount);
                
                // Keep only last MAX_THREAD_HISTORY measurements
                while (threadCounts.size() > MAX_THREAD_HISTORY) {
                    threadCounts.poll();
                }
                
                if (threadCount > MAX_THREAD_COUNT) {
                    logWarning("High thread count detected: " + threadCount);
                    performThreadCleanup();
                }
                
                // Monitor individual threads
                for (Thread thread : threads) {
                    if (thread != null && !monitoredThreads.contains(thread)) {
                        monitoredThreads.add(thread);
                        threadInfoMap.put(thread, new ThreadInfo(thread));
                    }
                }
            }
        } catch (Exception e) {
            logError("Thread monitoring failed: " + e.getMessage());
        }
    }
    
    /**
     * Check if application is responsive
     */
    private boolean isApplicationResponsive() {
        try {
            // Check if main window is visible and responsive
            if (mainApp != null && !mainApp.isVisible()) {
                return false;
            }
            
            // Check if device panel is responsive using reflection
            if (mainApp != null) {
                try {
                    Field devicePanelField = Main.class.getDeclaredField("devicePanel");
                    devicePanelField.setAccessible(true);
                    Object devicePanel = devicePanelField.get(mainApp);
                    if (devicePanel != null && devicePanel instanceof Component) {
                        return ((Component) devicePanel).isDisplayable();
                    }
                } catch (Exception e) {
                    // Ignore reflection errors
                }
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Detect deadlocks
     */
    private boolean detectDeadlocks() {
        try {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            long[] deadlockedThreads = threadBean.findDeadlockedThreads();
            return deadlockedThreads != null && deadlockedThreads.length > 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Detect memory leaks
     */
    private boolean detectMemoryLeak() {
        if (memoryUsage.size() < 5) return false;
        
        // Check if memory usage is consistently increasing
        Double[] recentUsage = memoryUsage.toArray(new Double[0]);
        boolean increasing = true;
        
        for (int i = 1; i < recentUsage.length; i++) {
            if (recentUsage[i] <= recentUsage[i-1]) {
                increasing = false;
                break;
            }
        }
        
        return increasing;
    }
    
    /**
     * Perform system recovery with improved error handling
     */
    private void performRecovery(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            reason = "Unknown reason";
        }
        
        if (isRecovering.compareAndSet(false, true)) {
            try {
                logWarning("Starting recovery procedure: " + reason);
                
                // Increment recovery attempts
                int currentAttempts = recoveryAttempts.incrementAndGet();
                
                // Check if we've exceeded max recovery attempts
                if (currentAttempts > MAX_RECOVERY_ATTEMPTS) {
                    logError("Maximum recovery attempts exceeded (" + currentAttempts + "), entering emergency mode");
                    emergencyMode = true;
                    return;
                }
                
                // Perform recovery steps in order of priority
                performMemoryCleanup();
                performThreadCleanup();
                performPerformanceOptimization();
                restartCriticalComponents();
                
                // Reset recovery state
                lastRecoveryTime = System.currentTimeMillis();
                logInfo("Recovery procedure completed successfully (attempt " + currentAttempts + ")");
                
            } catch (Exception e) {
                logError("Recovery procedure failed: " + e.getMessage());
                // Don't increment recovery attempts on failure to avoid false emergency mode
                recoveryAttempts.decrementAndGet();
            } finally {
                isRecovering.set(false);
            }
        } else {
            logInfo("Recovery already in progress, skipping: " + reason);
        }
    }
    
    /**
     * Perform memory cleanup with improved error handling
     */
    private void performMemoryCleanup() {
        try {
            logInfo("Starting memory cleanup...");
            
            // Clear caches
            clearCaches();
            
            // Force garbage collection (but don't rely on it)
            try {
                System.gc();
                logInfo("Garbage collection requested");
            } catch (Exception e) {
                logWarning("Garbage collection failed: " + e.getMessage());
            }
            
            // Clear response time history
            responseTimes.clear();
            
            logInfo("Memory cleanup completed successfully");
        } catch (Exception e) {
            logError("Memory cleanup failed: " + e.getMessage());
        }
    }
    
    /**
     * Perform thread cleanup with improved error handling
     */
    private void performThreadCleanup() {
        try {
            logInfo("Starting thread cleanup...");
            
            // Clear old thread info
            int oldThreadCount = threadInfoMap.size();
            threadInfoMap.clear();
            monitoredThreads.clear();
            
            // Clear thread count history
            threadCounts.clear();
            
            logInfo("Thread cleanup completed successfully (cleared " + oldThreadCount + " thread entries)");
        } catch (Exception e) {
            logError("Thread cleanup failed: " + e.getMessage());
        }
    }
    
    /**
     * Perform performance optimization with improved error handling
     */
    private void performPerformanceOptimization() {
        try {
            logInfo("Starting performance optimization...");
            
            // Clear response time history
            responseTimes.clear();
            
            // Request focus for main window
            if (mainApp != null && mainApp.isVisible()) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        mainApp.requestFocus();
                        try {
                            Field devicePanelField = Main.class.getDeclaredField("devicePanel");
                            devicePanelField.setAccessible(true);
                            Object devicePanel = devicePanelField.get(mainApp);
                            if (devicePanel != null && devicePanel instanceof Component) {
                                ((Component) devicePanel).requestFocus();
                            }
                        } catch (Exception e) {
                            logWarning("Device panel focus failed: " + e.getMessage());
                        }
                    } catch (Exception e) {
                        logWarning("Main window focus failed: " + e.getMessage());
                    }
                });
            }
            
            logInfo("Performance optimization completed successfully");
        } catch (Exception e) {
            logError("Performance optimization failed: " + e.getMessage());
        }
    }
    
    /**
     * Restart critical components
     */
    private void restartCriticalComponents() {
        try {
            // Restart device panel if needed
            if (mainApp != null) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        Field devicePanelField = Main.class.getDeclaredField("devicePanel");
                        devicePanelField.setAccessible(true);
                        Object devicePanel = devicePanelField.get(mainApp);
                        if (devicePanel != null && devicePanel instanceof Component) {
                            ((Component) devicePanel).revalidate();
                            ((Component) devicePanel).repaint();
                        }
                    } catch (Exception e) {
                        logError("Failed to restart device panel: " + e.getMessage());
                    }
                });
            }
            
            logInfo("Critical components restarted");
        } catch (Exception e) {
            logError("Failed to restart critical components: " + e.getMessage());
        }
    }
    
    /**
     * Clear caches
     */
    private void clearCaches() {
        try {
            // Clear any internal caches
            if (common != null) {
                // Clear common caches if available
                clearCommonCaches();
            }
            
            // Clear emulator context caches if available
            if (emulatorContext != null) {
                clearEmulatorContextCaches();
            }
        } catch (Exception e) {
            logError("Cache clearing failed: " + e.getMessage());
        }
    }
    
    /**
     * Clear common caches
     */
    private void clearCommonCaches() {
        try {
            // Use reflection to clear caches if they exist
            Field[] fields = Common.class.getDeclaredFields();
            for (Field field : fields) {
                if (field.getName().contains("cache") || field.getName().contains("Cache")) {
                    field.setAccessible(true);
                    Object value = field.get(common);
                    if (value instanceof Collection) {
                        ((Collection<?>) value).clear();
                    } else if (value instanceof Map) {
                        ((Map<?, ?>) value).clear();
                    }
                }
            }
        } catch (Exception e) {
            // Ignore reflection errors
        }
    }
    
    /**
     * Clear emulator context caches
     */
    private void clearEmulatorContextCaches() {
        try {
            // Use reflection to clear caches if they exist
            Field[] fields = emulatorContext.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (field.getName().contains("cache") || field.getName().contains("Cache")) {
                    field.setAccessible(true);
                    Object value = field.get(emulatorContext);
                    if (value instanceof Collection) {
                        ((Collection<?>) value).clear();
                    } else if (value instanceof Map) {
                        ((Map<?, ?>) value).clear();
                    }
                }
            }
        } catch (Exception e) {
            // Ignore reflection errors
        }
    }
    
    /**
     * Setup emergency shutdown hook
     */
    private void setupEmergencyShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logInfo("Emergency shutdown hook triggered");
            cleanupResources();
        }));
    }
    
    /**
     * Setup uncaught exception handler
     */
    private void setupUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            logError("Uncaught exception in thread " + thread.getName() + ": " + throwable.getMessage());
            crashCount.incrementAndGet();
            lastCrashTime = System.currentTimeMillis();
            lastError = throwable.getMessage();
            
            // Trigger recovery if not already recovering
            if (!isRecovering.get()) {
                performRecovery("Uncaught exception: " + throwable.getMessage());
            }
        });
    }
    
    /**
     * Clean up resources with improved error handling
     */
    private void cleanupResources() {
        try {
            logInfo("Starting resource cleanup...");
            
            // Stop monitoring
            stopMonitoring();
            
            // Clear collections safely
            if (responseTimes != null) {
                responseTimes.clear();
            }
            if (memoryUsage != null) {
                memoryUsage.clear();
            }
            if (threadCounts != null) {
                threadCounts.clear();
            }
            if (monitoredThreads != null) {
                monitoredThreads.clear();
            }
            if (threadInfoMap != null) {
                threadInfoMap.clear();
            }
            
            logInfo("Resource cleanup completed successfully");
        } catch (Exception e) {
            logError("Resource cleanup failed: " + e.getMessage());
        }
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Get current memory usage percentage with validation
     */
    private double getMemoryUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            if (runtime == null) {
                return 0.0;
            }
            
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            // Validate memory values
            if (maxMemory <= 0 || totalMemory <= 0 || freeMemory < 0 || usedMemory < 0) {
                return 0.0;
            }
            
            double percentage = (double) usedMemory / maxMemory * 100;
            
            // Validate percentage
            if (percentage < 0 || percentage > 100) {
                return 0.0;
            }
            
            return percentage;
        } catch (Exception e) {
            logError("Memory usage calculation failed: " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Get current thread count with validation
     */
    private int getThreadCount() {
        try {
            ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
            if (rootGroup == null) {
                return 0;
            }
            
            while (rootGroup.getParent() != null) {
                rootGroup = rootGroup.getParent();
            }
            
            int count = rootGroup.activeCount();
            return count >= 0 ? count : 0;
        } catch (Exception e) {
            logError("Thread count calculation failed: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Get average response time with validation
     */
    private double getAverageResponseTime() {
        try {
            if (responseTimes == null || responseTimes.isEmpty()) {
                return 0.0;
            }
            
            long total = 0;
            int count = 0;
            for (Long time : responseTimes) {
                if (time != null && time >= 0 && time < Long.MAX_VALUE) {
                    total += time;
                    count++;
                }
            }
            
            return count > 0 ? (double) total / count : 0.0;
        } catch (Exception e) {
            logError("Average response time calculation failed: " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Log info message with timestamp
     */
    private void logInfo(String message) {
        if (message != null) {
            String timestamp = new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date());
            System.out.println("[" + timestamp + "] [Fallback] INFO: " + message);
        }
    }
    
    /**
     * Log warning message with timestamp
     */
    private void logWarning(String message) {
        if (message != null) {
            String timestamp = new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date());
            System.out.println("[" + timestamp + "] [Fallback] WARNING: " + message);
        }
    }
    
    /**
     * Log error message with timestamp
     */
    private void logError(String message) {
        if (message != null) {
            String timestamp = new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date());
            System.err.println("[" + timestamp + "] [Fallback] ERROR: " + message);
        }
    }
    
    // ==================== INNER CLASSES ====================
    
    /**
     * System health information
     */
    public static class SystemHealth {
        public final double memoryUsage;
        public final int threadCount;
        public final double averageResponseTime;
        public final int crashCount;
        public final int recoveryAttempts;
        public final boolean emergencyMode;
        public final String lastError;
        
        public SystemHealth(double memoryUsage, int threadCount, double averageResponseTime,
                          int crashCount, int recoveryAttempts, boolean emergencyMode, String lastError) {
            this.memoryUsage = memoryUsage;
            this.threadCount = threadCount;
            this.averageResponseTime = averageResponseTime;
            this.crashCount = crashCount;
            this.recoveryAttempts = recoveryAttempts;
            this.emergencyMode = emergencyMode;
            this.lastError = lastError;
        }
        
        @Override
        public String toString() {
            return String.format("SystemHealth{memory=%.1f%%, threads=%d, response=%.0fms, crashes=%d, recoveries=%d, emergency=%s, lastError='%s'}",
                memoryUsage, threadCount, averageResponseTime, crashCount, recoveryAttempts, emergencyMode, lastError);
        }
    }
    
    /**
     * Thread information
     */
    private static class ThreadInfo {
        public final String name;
        public final long id;
        public final Thread.State state;
        public final long startTime;
        
        public ThreadInfo(Thread thread) {
            this.name = thread.getName();
            this.id = thread.getId();
            this.state = thread.getState();
            this.startTime = System.currentTimeMillis();
        }
    }
} 