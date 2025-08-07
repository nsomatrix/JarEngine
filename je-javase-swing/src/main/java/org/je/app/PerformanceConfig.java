package org.je.app;

/**
 * Centralized config for runtime performance toggles.
 */
public class PerformanceConfig {
    // Double buffering toggle
    public static volatile boolean doubleBufferingEnabled = true;
    // Frame skipping toggle
    public static volatile boolean frameSkippingEnabled = false;
    // Image caching toggle
    public static volatile boolean imageCachingEnabled = true;
}
