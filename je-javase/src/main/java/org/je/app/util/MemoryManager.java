package org.je.app.util;

import org.je.log.Logger;
import org.je.performance.PerformanceManager;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Memory management utilities for long-running emulator sessions.
 * Helps prevent memory leaks during extended gameplay periods.
 */
public class MemoryManager {
    
    private static final ScheduledExecutorService scheduler = 
        Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "MemoryManager");
            t.setDaemon(true);
            return t;
        });
    
    private static final List<WeakReference<AutoCloseable>> managedResources = 
        new CopyOnWriteArrayList<>();
    
    private static volatile boolean autoCleanupEnabled = true;
    private static volatile long lastForceGC = 0;
    private static final long FORCE_GC_INTERVAL = 5 * 60 * 1000; // 5 minutes
    
    static {
        // Start periodic cleanup every 30 seconds
        scheduler.scheduleWithFixedDelay(MemoryManager::performCleanup, 30, 30, TimeUnit.SECONDS);
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdown();
            cleanupAllResources();
        }, "MemoryManager-Shutdown"));
    }
    
    /**
     * Register a resource for automatic cleanup
     */
    public static void registerResource(AutoCloseable resource) {
        if (resource != null) {
            managedResources.add(new WeakReference<>(resource));
        }
    }
    
    /**
     * Perform periodic memory cleanup
     */
    private static void performCleanup() {
        try {
            // Clean up dead weak references
            managedResources.removeIf(ref -> ref.get() == null);
            
            // Clear performance manager sprite cache if getting large
            long heapUsage = PerformanceManager.getEmulatedUsageBytes();
            long heapLimit = PerformanceManager.getEmulatedHeapLimitBytes();
            
            if (heapUsage > heapLimit * 0.8) { // 80% threshold
                Logger.info("Memory usage high (" + (heapUsage / 1024 / 1024) + "MB), clearing sprite cache");
                PerformanceManager.clearSpriteCache();
            }
            
            // Force GC periodically for long-running sessions
            long now = System.currentTimeMillis();
            if (autoCleanupEnabled && (now - lastForceGC) > FORCE_GC_INTERVAL) {
                Logger.debug("Performing periodic garbage collection for long-running session");
                System.gc();
                lastForceGC = now;
            }
            
        } catch (Exception e) {
            Logger.error("Error during memory cleanup", e);
        }
    }
    
    /**
     * Force immediate cleanup of all managed resources
     */
    public static void cleanupAllResources() {
        for (WeakReference<AutoCloseable> ref : managedResources) {
            AutoCloseable resource = ref.get();
            if (resource != null) {
                try {
                    resource.close();
                } catch (Exception e) {
                    Logger.debug("Error closing resource: " + e.getMessage());
                }
            }
        }
        managedResources.clear();
    }
    
    /**
     * Enable/disable automatic cleanup features
     */
    public static void setAutoCleanupEnabled(boolean enabled) {
        autoCleanupEnabled = enabled;
        Logger.info("Memory auto-cleanup " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Get memory usage statistics
     */
    public static String getMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        long emulatedUsage = PerformanceManager.getEmulatedUsageBytes();
        
        return String.format(
            "JVM: %dMB used / %dMB total / %dMB max | Emulated: %dMB | Managed Resources: %d",
            usedMemory / 1024 / 1024,
            totalMemory / 1024 / 1024, 
            maxMemory / 1024 / 1024,
            emulatedUsage / 1024 / 1024,
            managedResources.size()
        );
    }
}
