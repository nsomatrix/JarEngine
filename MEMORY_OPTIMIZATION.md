# Memory Optimization Guide for Long-Running J2ME Games

This guide addresses memory leak issues when running J2ME games for extended periods (hours/days).

## 🔍 Root Causes of Memory Leaks

### 1. **Thread Accumulation**
- Background threads from games not properly terminated
- Short grace period for thread cleanup (previously 500ms)

### 2. **Graphics Memory Growth**
- BufferedImages and sprite caches accumulating
- No automatic cleanup of graphics resources

### 3. **Timer/Event Listener Buildup**
- Timers not properly disposed between MIDlet restarts
- Event listeners remaining in memory

### 4. **Incomplete Resource Cleanup**
- Network connections not always properly closed
- File handles and streams left open

## ✅ Implemented Solutions

### **Enhanced Thread Management**
- Increased thread termination grace period to 2000ms
- Better logging of thread cleanup process
- Improved thread interruption handling

### **Automatic Memory Management**
- New `MemoryManager` class with periodic cleanup (every 30 seconds)
- Automatic sprite cache clearing when memory usage > 80%
- Forced garbage collection every 5 minutes for long sessions
- Resource tracking with weak references

### **Improved MIDlet Lifecycle**
- Enhanced cleanup during MIDlet context destruction
- Automatic clearing of sprite caches and thread-local buffers
- Better resource disposal coordination

### **Memory Monitoring Tools**
- New `MemoryMonitorTool` for real-time memory tracking
- Manual cleanup controls
- Auto-cleanup toggle

## 🚀 JVM Configuration Recommendations

### **For Long-Running Sessions (Hours/Days):**

```bash
# Recommended JVM arguments for production VPS:
-Xmx2G                    # Max heap 2GB (adjust based on VPS RAM)
-Xms512M                  # Initial heap 512MB
-XX:+UseG1GC              # G1 garbage collector (better for long-running)
-XX:MaxGCPauseMillis=200  # Target GC pause time
-XX:+UseStringDeduplication # Reduce string memory usage
-XX:+DisableExplicitGC    # Prevent manual System.gc() calls from apps
```

### **For Development/Testing:**

```bash
# With memory monitoring enabled:
-Xmx1G -Xms256M -XX:+UseG1GC -verbose:gc -XX:+PrintGCDetails
```

### **Memory-Constrained Environments:**

```bash
# For limited RAM (< 2GB):
-Xmx512M -Xms128M -XX:+UseSerialGC -XX:NewRatio=3
```

## 🛠️ Configuration Settings

### **Performance Manager Settings**
- Set emulated heap limit appropriately (default 64MB)
- Enable sprite caching for better performance
- Configure frame skipping for smoother gameplay

### **Memory Manager Settings**
- Auto-cleanup enabled by default
- Periodic cleanup every 30 seconds
- Force GC every 5 minutes

## 📊 Monitoring Memory Usage

### **Built-in Tools:**
1. Use the new Memory Monitor tool (Tools → Memory Monitor)
2. Watch for warnings in logs about thread/timer cleanup
3. Monitor emulated vs JVM heap usage

### **External Monitoring:**
```bash
# Monitor Java process memory:
ps aux | grep java
top -p <java_pid>

# JVM memory details:
jstat -gc <java_pid> 1s
```

## ⚡ Best Practices for Long Sessions

1. **Restart MIDlets Periodically**: Even with fixes, restart games every few hours
2. **Monitor Memory**: Use the built-in memory monitor tool
3. **Adjust Heap Limits**: Configure emulated heap based on game requirements
4. **Enable Logging**: Monitor cleanup warnings in logs
5. **Use G1GC**: Better garbage collector for long-running applications

## 🔧 Troubleshooting

### **Still Running Out of Memory?**

1. **Check Thread Warnings**: Look for "still running" thread warnings in logs
2. **Monitor Sprite Cache**: Large games may need higher emulated heap limits
3. **JVM Heap Size**: Increase `-Xmx` if total memory usage is high
4. **Game-Specific Issues**: Some games may have their own memory leaks

### **Performance Issues?**

1. **Disable Auto-Cleanup**: If causing stutters, disable in Memory Monitor
2. **Adjust Cleanup Frequency**: Modify `FORCE_GC_INTERVAL` in MemoryManager
3. **Use Different GC**: Try `-XX:+UseParallelGC` for better throughput

## 📈 Expected Improvements

With these fixes, you should see:
- ✅ Stable memory usage over long periods
- ✅ Better cleanup between MIDlet restarts  
- ✅ Reduced thread/timer accumulation
- ✅ More predictable garbage collection
- ✅ Real-time monitoring capabilities

The emulator should now handle 24+ hour gaming sessions without memory issues.
