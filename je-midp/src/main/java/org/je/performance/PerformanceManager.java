package org.je.performance;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.microedition.lcdui.Image;

/**
 * Central performance feature toggle hub. Lives in je-midp so lower level code (EventDispatcher)
 * and higher Swing layers can both reference it without creating circular dependencies.
 *
 * Features are intentionally lightweight and optional – if a flag is off existing behavior is unchanged.
 */
public final class PerformanceManager {

    private PerformanceManager() {}

    // ========= Configuration Save Callback Interface =========
    public interface ConfigSaveCallback {
        void onConfigSave(String configType, int spinnerDurationMs);
    }
    
    private static volatile ConfigSaveCallback configSaveCallback;
    
    /** Set the callback to trigger when configuration is saved */
    public static void setConfigSaveCallback(ConfigSaveCallback callback) {
        configSaveCallback = callback;
    }

    // ========= Toggles =========
    private static volatile boolean hardwareAcceleration; // advisory only
    private static volatile boolean antiAliasing;
    private static volatile boolean doubleBuffering; // user can enable for smoother rendering
    private static volatile boolean powerSavingMode;
    private static volatile boolean idleSkipping;
    private static volatile boolean frameSkipping;
    private static volatile boolean threadPriorityBoost;
    private static volatile boolean inputThrottling;
    private static volatile boolean spriteCaching;
    private static volatile boolean textureFiltering; // user can enable for better scaling quality
    private static volatile boolean vSync;

    // ========= Heap emulation =========
    private static volatile long emulatedHeapLimitBytes = 64L * 1024 * 1024; // 64 MB soft cap
    private static final long HEAP_STEP = 16L * 1024 * 1024; // 16 MB increments
    // Track non-cached image bytes separately from sprite cache bytes
    private static volatile long baseImageBytes; // sum of registered images not in sprite cache

    // ========= Sprite cache =========
    private static final Map<String, Image> spriteCache = new ConcurrentHashMap<>();
    private static volatile long spriteCacheBytes; // approximate

    // ========= Frame skipping helpers =========
    private static volatile int frameSkipModulo = 2; // process 1 of every 2 frames when enabled
    private static int frameCounter;

    // ========= Input throttling helpers =========
    private static volatile long lastPointerDragTime;
    private static volatile int pointerDragMinIntervalMs = 16; // ~60Hz when throttling enabled

    // ========= FPS handling references =========
    private static volatile int previousFpsForPowerSave = -1;
    private static volatile int previousFpsForVSync = -1;

    // ======= Public API =======
    public static boolean isHardwareAcceleration() { return hardwareAcceleration; }
    public static void setHardwareAcceleration(boolean v) {
        hardwareAcceleration = v;
        System.setProperty("sun.java2d.opengl", v ? "true" : "false");
    savePreferencesAsync();
    }

    public static boolean isAntiAliasing() { return antiAliasing; }
    public static void setAntiAliasing(boolean v) { antiAliasing = v; }
    public static void setAntiAliasingPersist(boolean v) { antiAliasing = v; savePreferencesAsync(); }

    public static boolean isDoubleBuffering() { return doubleBuffering; }
    public static void setDoubleBuffering(boolean v) { doubleBuffering = v; }
    public static void setDoubleBufferingPersist(boolean v) { doubleBuffering = v; savePreferencesAsync(); }

    public static boolean isPowerSavingMode() { return powerSavingMode; }
    public static void setPowerSavingMode(boolean v, int currentMaxFps) {
        if (v && !powerSavingMode) {
            previousFpsForPowerSave = currentMaxFps;
        } else if (!v && powerSavingMode) {
            // Restore previous FPS (including unlimited values <=0)
            if (!vSync) {
                org.je.device.ui.EventDispatcher.maxFps = previousFpsForPowerSave;
            }
        }
        powerSavingMode = v;
        if (powerSavingMode) {
            org.je.device.ui.EventDispatcher.maxFps = 15; // throttle
        }
    savePreferencesAsync();
    }

    public static boolean isIdleSkipping() { return idleSkipping; }
    public static void setIdleSkipping(boolean v) { idleSkipping = v; }
    public static void setIdleSkippingPersist(boolean v) { idleSkipping = v; savePreferencesAsync(); }

    public static boolean isFrameSkipping() { return frameSkipping; }
    public static void setFrameSkipping(boolean v) { frameSkipping = v; frameCounter = 0; }
    public static void setFrameSkippingPersist(boolean v) { frameSkipping = v; frameCounter = 0; savePreferencesAsync(); }
    public static void setFrameSkipModulo(int modulo) { if (modulo >= 2) frameSkipModulo = modulo; }

    public static boolean isThreadPriorityBoost() { return threadPriorityBoost; }
    public static void setThreadPriorityBoost(boolean v, Thread eventThread) {
        threadPriorityBoost = v;
        if (eventThread != null) {
            try {
                eventThread.setPriority(v ? Thread.MAX_PRIORITY : Thread.NORM_PRIORITY);
            } catch (SecurityException ignored) {}
        }
    savePreferencesAsync();
    }

    public static boolean isInputThrottling() { return inputThrottling; }
    public static void setInputThrottling(boolean v) { inputThrottling = v; }
    public static void setInputThrottlingPersist(boolean v) { inputThrottling = v; savePreferencesAsync(); }
    public static void setPointerDragMinIntervalMs(int ms) { if (ms >= 1) pointerDragMinIntervalMs = ms; }

    public static boolean isSpriteCaching() { return spriteCaching; }
    public static void setSpriteCaching(boolean v) { spriteCaching = v; if (!v) clearSpriteCache(); }
    public static void setSpriteCachingPersist(boolean v) { spriteCaching = v; if (!v) clearSpriteCache(); savePreferencesAsync(); }

    public static boolean isTextureFiltering() { return textureFiltering; }
    public static void setTextureFiltering(boolean v) { textureFiltering = v; }
    public static void setTextureFilteringPersist(boolean v) { textureFiltering = v; savePreferencesAsync(); }

    public static boolean isVSync() { return vSync; }
    public static void setVSync(boolean v, int currentMaxFps) {
        if (v && !vSync) {
            previousFpsForVSync = currentMaxFps;
            if (currentMaxFps <= 0 || currentMaxFps > 60) {
                org.je.device.ui.EventDispatcher.maxFps = 60;
            }
        } else if (!v && vSync) {
            // Restore prior FPS cap (including unlimited -1) if power saving not active
            if (!powerSavingMode) {
                org.je.device.ui.EventDispatcher.maxFps = previousFpsForVSync;
            }
        }
        vSync = v;
        savePreferencesAsync();
    }

    public static long getEmulatedHeapLimitBytes() { return emulatedHeapLimitBytes; }
    public static void increaseHeap() { emulatedHeapLimitBytes += HEAP_STEP; savePreferencesAsync(); }
    public static void decreaseHeap() { emulatedHeapLimitBytes = Math.max(HEAP_STEP, emulatedHeapLimitBytes - HEAP_STEP); trimSpriteCacheIfNeeded(); savePreferencesAsync(); }
    public static void setEmulatedHeapLimitBytes(long bytes) {
        if (bytes < HEAP_STEP) {
            bytes = HEAP_STEP; // minimum 16MB
        }
        long max = 4096L * 1024 * 1024; // 4 GB ceiling
        if (bytes > max) {
            bytes = max;
        }
        emulatedHeapLimitBytes = bytes;
        trimSpriteCacheIfNeeded();
        savePreferencesAsync();
    }

    public static Image getCachedSprite(String key) {
        return spriteCaching ? spriteCache.get(key) : null;
    }
    public static synchronized void putCachedSprite(String key, Image img) {
        if (!spriteCaching || img == null) return;
        try {
            long sz = (long) img.getWidth() * (long) img.getHeight() * 4L;
            if (spriteCacheBytes + sz > emulatedHeapLimitBytes) {
                trimSpriteCacheIfNeeded();
                if (spriteCacheBytes + sz > emulatedHeapLimitBytes) {
                    return;
                }
            }
            spriteCache.put(key, img);
            spriteCacheBytes += sz;
        } catch (Throwable ignored) {}
    }

    public static Map<String, Image> snapshotCache() { return Collections.unmodifiableMap(spriteCache); }

    private static synchronized void trimSpriteCacheIfNeeded() {
        if (spriteCacheBytes <= emulatedHeapLimitBytes) return;
        clearSpriteCache();
    }

    private static synchronized void clearSpriteCache() {
        spriteCache.clear();
        spriteCacheBytes = 0;
    }

    public static boolean shouldSkipPaintFrame() {
        if (!frameSkipping) return false;
        int c = ++frameCounter;
        return c % frameSkipModulo != 0;
    }

    public static boolean shouldThrottlePointerDrag() {
        if (!inputThrottling) return false;
        long now = System.currentTimeMillis();
        long dt = now - lastPointerDragTime;
        if (dt < pointerDragMinIntervalMs) {
            return true;
        }
        lastPointerDragTime = now;
        return false;
    }

    public static void onIdleWaitHook() {
        if (idleSkipping) {
            try { Thread.sleep(4); } catch (InterruptedException ignored) {}
        }
    }

    // ======= Image / Heap tracking =======
    public static long getEmulatedUsageBytes() { return baseImageBytes + spriteCacheBytes; }
    public static synchronized boolean registerImage(int width, int height) {
        long sz = (long) width * (long) height * 4L;
        long newUsage = baseImageBytes + spriteCacheBytes + sz;
        if (newUsage > emulatedHeapLimitBytes) {
            // Reject registration; caller may choose to proceed but we signal exceeded limit
            return false;
        }
        baseImageBytes += sz;
        return true;
    }
    public static synchronized void noteImageFreed(int width, int height) {
        long sz = (long) width * (long) height * 4L;
        baseImageBytes = Math.max(0, baseImageBytes - sz);
    }

    // ======= Persistence =======
    private static final String FILE_NAME = "performance.properties";
    private static volatile boolean preferencesLoaded;
    private static volatile boolean pendingSave;
    private static long lastSaveTime;
    private static final long SAVE_DEBOUNCE_MS = 750; // batch rapid changes

    static {
        loadPreferences();
    }

    private static File getPreferencesFile() {
        String userHome = System.getProperty("user.home", ".");
        // Mirror Config path logic: ~/.je[/<emulatorID>]
        String emulatorId = System.getProperty("je.emulatorID"); // allow injection via system property
        File base = new File(userHome, ".je");
        if (emulatorId != null && !emulatorId.isEmpty()) {
            base = new File(base, emulatorId);
        }
        base.mkdirs();
        return new File(base, FILE_NAME);
    }

    public static synchronized void loadPreferences() {
        if (preferencesLoaded) return;
        File f = getPreferencesFile();
        if (f.isFile()) {
            Properties p = new Properties();
            try (FileInputStream in = new FileInputStream(f)) {
                p.load(in);
                hardwareAcceleration = Boolean.parseBoolean(p.getProperty("hardwareAcceleration", Boolean.toString(hardwareAcceleration)));
                antiAliasing = Boolean.parseBoolean(p.getProperty("antiAliasing", Boolean.toString(antiAliasing)));
                doubleBuffering = Boolean.parseBoolean(p.getProperty("doubleBuffering", Boolean.toString(doubleBuffering)));
                powerSavingMode = Boolean.parseBoolean(p.getProperty("powerSavingMode", Boolean.toString(powerSavingMode)));
                idleSkipping = Boolean.parseBoolean(p.getProperty("idleSkipping", Boolean.toString(idleSkipping)));
                frameSkipping = Boolean.parseBoolean(p.getProperty("frameSkipping", Boolean.toString(frameSkipping)));
                threadPriorityBoost = Boolean.parseBoolean(p.getProperty("threadPriorityBoost", Boolean.toString(threadPriorityBoost)));
                inputThrottling = Boolean.parseBoolean(p.getProperty("inputThrottling", Boolean.toString(inputThrottling)));
                spriteCaching = Boolean.parseBoolean(p.getProperty("spriteCaching", Boolean.toString(spriteCaching)));
                textureFiltering = Boolean.parseBoolean(p.getProperty("textureFiltering", Boolean.toString(textureFiltering)));
                vSync = Boolean.parseBoolean(p.getProperty("vSync", Boolean.toString(vSync)));
                try {
                    long heap = Long.parseLong(p.getProperty("emulatedHeapLimitBytes", Long.toString(emulatedHeapLimitBytes)));
                    setEmulatedHeapLimitBytes(heap);
                } catch (NumberFormatException ignored) {}
            } catch (IOException ignored) {}
        }
        // Re-apply any required system properties from loaded state without triggering a save
        try {
            System.setProperty("sun.java2d.opengl", hardwareAcceleration ? "true" : "false");
        } catch (Throwable ignored) {}
        preferencesLoaded = true;
    }

    private static synchronized void savePreferencesAsync() {
        if (!preferencesLoaded) return; // don't save during static initialization until load completes
        long now = System.currentTimeMillis();
        pendingSave = true;
        // Simple debounce: only save if sufficient time passed or force after delay
        if (now - lastSaveTime >= SAVE_DEBOUNCE_MS) {
            savePreferences();
        } else {
            // schedule a delayed save via a daemon thread
            Thread t = new Thread(() -> {
                try { Thread.sleep(SAVE_DEBOUNCE_MS); } catch (InterruptedException ignored) {}
                synchronized (PerformanceManager.class) {
                    if (pendingSave && System.currentTimeMillis() - lastSaveTime >= SAVE_DEBOUNCE_MS) {
                        savePreferences();
                    }
                }
            }, "PerfPrefsSaver");
            t.setDaemon(true);
            t.start();
        }
    }

    private static synchronized void savePreferences() {
        // Trigger config save callback if available
        if (configSaveCallback != null) {
            try {
                configSaveCallback.onConfigSave("performance", 700);
            } catch (Exception ignored) {} // Don't fail if callback fails
        }
        
        pendingSave = false;
        lastSaveTime = System.currentTimeMillis();
        Properties p = new Properties();
        p.setProperty("hardwareAcceleration", Boolean.toString(hardwareAcceleration));
        p.setProperty("antiAliasing", Boolean.toString(antiAliasing));
        p.setProperty("doubleBuffering", Boolean.toString(doubleBuffering));
        p.setProperty("powerSavingMode", Boolean.toString(powerSavingMode));
        p.setProperty("idleSkipping", Boolean.toString(idleSkipping));
        p.setProperty("frameSkipping", Boolean.toString(frameSkipping));
        p.setProperty("threadPriorityBoost", Boolean.toString(threadPriorityBoost));
        p.setProperty("inputThrottling", Boolean.toString(inputThrottling));
        p.setProperty("spriteCaching", Boolean.toString(spriteCaching));
        p.setProperty("textureFiltering", Boolean.toString(textureFiltering));
        p.setProperty("vSync", Boolean.toString(vSync));
        p.setProperty("emulatedHeapLimitBytes", Long.toString(emulatedHeapLimitBytes));
        File f = getPreferencesFile();
        try (FileOutputStream out = new FileOutputStream(f)) {
            p.store(out, "JarEngine Performance Preferences");
        } catch (IOException ignored) {}
    }

    // ======= Defaults reset =======
    public static synchronized void resetToDefaults() {
        // Toggle defaults
        hardwareAcceleration = false;
        try { System.setProperty("sun.java2d.opengl", "false"); } catch (Throwable ignored) {}
        antiAliasing = false;
        doubleBuffering = false;
        powerSavingMode = false;
        idleSkipping = false;
        frameSkipping = false;
        threadPriorityBoost = false;
        inputThrottling = false;
        spriteCaching = false;
        textureFiltering = false;
        vSync = false;
        // Emulated heap default
        emulatedHeapLimitBytes = 64L * 1024 * 1024;
        // Clear runtime counters/caches
        baseImageBytes = 0L;
        clearSpriteCache();
        // Persist
        savePreferences();
    }
}
