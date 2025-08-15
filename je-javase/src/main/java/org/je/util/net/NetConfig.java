package org.je.util.net;

import javax.net.ssl.*;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URLConnection;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.je.app.Common;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Global network configuration toggles and helpers used by CLDC I/O and UI tools.
 */
public final class NetConfig {
    private NetConfig() {}

    public static final class Policy {
        public static volatile boolean offline = false;
        public static volatile boolean captivePortal = false;
        public static volatile int captivePort = 8081;
    }

    public static final class Dns {
        private static final Map<String, String> overrides = new LinkedHashMap<>();
        public static synchronized void put(String host, String ip) {
            overrides.put(host.toLowerCase(Locale.ROOT), ip);
        }
        public static synchronized void remove(String host) {
            overrides.remove(host.toLowerCase(Locale.ROOT));
        }
        public static synchronized void clear() {
            overrides.clear();
        }
        public static synchronized Map<String, String> snapshot() {
            return new LinkedHashMap<>(overrides);
        }
        public static InetAddress resolveHost(String host) throws UnknownHostException {
            String key = host.toLowerCase(Locale.ROOT);
            String ip;
            synchronized (Dns.class) { ip = overrides.get(key); }
            if (ip != null && !ip.isEmpty()) {
                return InetAddress.getByName(ip);
            }
            return InetAddress.getByName(host);
        }
    }

    public static final class Traffic {
        public static volatile int bandwidthKbps = 0; // 0 = unlimited
        public static volatile int latencyMs = 0;
        public static volatile int jitterMs = 0;
        public static volatile int packetLossPct = 0; // for UDP only

        private static int computeSleepMs(int bytes) {
            int bw = bandwidthKbps;
            if (bw <= 0) return 0;
            // time in ms to send 'bytes' at bw kbps = ceil((bytes*8 bits)/(bw*1000 bits/s) * 1000)
            double ms = Math.ceil((bytes * 8.0) / (bw * 1000.0) * 1000.0);
            return (int) ms;
        }

        private static int computeLatencyOnce(Random rnd) {
            int base = Math.max(0, latencyMs);
            int jit = Math.max(0, jitterMs);
            if (jit == 0) return base;
            // jitter +/- up to jitterMs/2
            int delta = jit == 0 ? 0 : (rnd.nextInt(jit + 1) - jit / 2);
            int total = base + delta;
            return Math.max(0, total);
        }

        public static InputStream wrapInput(InputStream in) {
            return new ThrottledInputStream(in);
        }

        public static OutputStream wrapOutput(OutputStream out) {
            return new ThrottledOutputStream(out);
        }

        public static boolean udpPermitAndDelay(int bytes, Random rnd) {
            if (rnd == null) rnd = new Random();
            int loss = Math.max(0, Math.min(100, packetLossPct));
            if (loss > 0 && rnd.nextInt(100) < loss) {
                return false; // drop
            }
            // apply latency+jitter and bandwidth delay
            sleepQuiet(computeLatencyOnce(rnd));
            int d = computeSleepMs(bytes);
            if (d > 0) sleepQuiet(d);
            return true;
        }

        private static void sleepQuiet(int ms) {
            if (ms <= 0) return;
            try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }

        private static final class ThrottledInputStream extends FilterInputStream {
            private boolean first = true;
            private final Random rnd = new Random();
            protected ThrottledInputStream(InputStream in) { super(in); }
            @Override public int read() throws IOException {
                int r = super.read();
                if (r >= 0) {
                    applyDelay(1);
                    // Publish byte count for network monitoring
                    publishByteActivity("IN", 1);
                }
                return r;
            }
            @Override public int read(byte[] b, int off, int len) throws IOException {
                int n = super.read(b, off, len);
                if (n > 0) {
                    applyDelay(n);
                    // Publish byte count for network monitoring
                    publishByteActivity("IN", n);
                }
                return n;
            }
            private void applyDelay(int bytes) {
                if (first) { first = false; sleepQuiet(computeLatencyOnce(rnd)); }
                int d = computeSleepMs(bytes);
                if (d > 0) sleepQuiet(d);
            }
            private void publishByteActivity(String direction, long bytes) {
                try {
                    org.je.util.NetEventBus.publish("DATA", direction, "throttled-stream", 
                        "Stream activity", bytes);
                } catch (Exception ignored) {
                    // Don't let network monitoring break actual I/O
                }
            }
        }

        private static final class ThrottledOutputStream extends FilterOutputStream {
            private boolean first = true;
            private final Random rnd = new Random();
            protected ThrottledOutputStream(OutputStream out) { super(out); }
            @Override public void write(int b) throws IOException {
                applyDelay(1);
                super.write(b);
                // Publish byte count for network monitoring
                publishByteActivity("OUT", 1);
            }
            @Override public void write(byte[] b, int off, int len) throws IOException {
                applyDelay(len);
                super.write(b, off, len);
                // Publish byte count for network monitoring
                publishByteActivity("OUT", len);
            }
            private void applyDelay(int bytes) {
                if (first) { first = false; sleepQuiet(computeLatencyOnce(rnd)); }
                int d = computeSleepMs(bytes);
                if (d > 0) sleepQuiet(d);
            }
            private void publishByteActivity(String direction, long bytes) {
                try {
                    org.je.util.NetEventBus.publish("DATA", direction, "throttled-stream", 
                        "Stream activity", bytes);
                } catch (Exception ignored) {
                    // Don't let network monitoring break actual I/O
                }
            }
        }
    }

    public static final class TLS {
        public static volatile boolean trustAll = false;
        // Capture original JVM defaults so we can restore them
        private static final HostnameVerifier ORIGINAL_HOSTNAME_VERIFIER = HttpsURLConnection.getDefaultHostnameVerifier();
        private static final SSLSocketFactory ORIGINAL_SSL_SOCKET_FACTORY = (SSLSocketFactory) HttpsURLConnection.getDefaultSSLSocketFactory();

        public static void applyDefaultContextIfNeeded() {
            if (!trustAll) return;
            try {
                TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] xcs, String string) {}
                    public void checkServerTrusted(X509Certificate[] xcs, String string) {}
                }};
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier((h, s) -> true);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                // ignore
            }
        }

        public static void applyToHttpsIfNeeded(URLConnection cn) {
            if (!trustAll) return;
            if (cn instanceof HttpsURLConnection) {
                applyDefaultContextIfNeeded();
                ((HttpsURLConnection) cn).setHostnameVerifier((h, s) -> true);
            }
        }

        // Provide a base factory according to trustAll
        public static SSLSocketFactory getBaseFactory() {
            if (!trustAll) return (SSLSocketFactory) SSLSocketFactory.getDefault();
            try {
                TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] xcs, String string) {}
                    public void checkServerTrusted(X509Certificate[] xcs, String string) {}
                }};
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                return sc.getSocketFactory();
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                return (SSLSocketFactory) SSLSocketFactory.getDefault();
            }
        }

        // Wrap base SSLSocketFactory so that it connects to connectAddr while presenting SNI for sniHost
        public static SSLSocketFactory wrapWithSniAndIp(final SSLSocketFactory base, final String sniHost, final InetAddress connectAddr) {
            return new SSLSocketFactory() {
                private void applySni(SSLSocket sock) {
                    try {
                        SSLParameters p = sock.getSSLParameters();
                        java.util.List<SNIServerName> names = java.util.Collections.singletonList(new SNIHostName(sniHost));
                        p.setServerNames(names);
                        sock.setSSLParameters(p);
                    } catch (Throwable ignored) {}
                }
                private SSLSocket connectAndSni(int port) throws IOException {
                    SSLSocket s = (SSLSocket) base.createSocket(connectAddr, port);
                    applySni(s);
                    return s;
                }
                @Override public String[] getDefaultCipherSuites() { return base.getDefaultCipherSuites(); }
                @Override public String[] getSupportedCipherSuites() { return base.getSupportedCipherSuites(); }
                @Override public Socket createSocket(String host, int port) throws IOException { return connectAndSni(port); }
                @Override public Socket createSocket(InetAddress host, int port) throws IOException { return connectAndSni(port); }
                @Override public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
                    SSLSocket s = (SSLSocket) base.createSocket(connectAddr, port, localHost, localPort);
                    applySni(s); return s; }
                @Override public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
                    SSLSocket s = (SSLSocket) base.createSocket(connectAddr, port, localAddress, localPort);
                    applySni(s); return s; }
                @Override public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
                    // Ignore provided host; rely on already-connected socket or connect explicitly
                    Socket baseSock = s;
                    if (baseSock == null || !baseSock.isConnected()) {
                        baseSock = new Socket();
                        baseSock.connect(new java.net.InetSocketAddress(connectAddr, port));
                    }
                    SSLSocket ssl = (SSLSocket) base.createSocket(baseSock, sniHost, port, autoClose);
                    applySni(ssl);
                    return ssl;
                }
            };
        }

        // Restore JVM HTTPS defaults (undo trust-all changes)
        public static void resetDefaultContext() {
            try {
                HttpsURLConnection.setDefaultSSLSocketFactory(ORIGINAL_SSL_SOCKET_FACTORY);
                HttpsURLConnection.setDefaultHostnameVerifier(ORIGINAL_HOSTNAME_VERIFIER);
            } catch (Throwable ignored) {}
        }
    }

    // ======= Persistence (network.properties) =======
    private static final String FILE_NAME = "network.properties";
    private static volatile boolean preferencesLoaded;
    private static volatile boolean pendingSave;
    private static long lastSaveTime;
    private static final long SAVE_DEBOUNCE_MS = 750;

    static { loadPreferences(); }

    private static java.io.File getPreferencesFile() {
        String userHome = System.getProperty("user.home", ".");
        String emulatorId = System.getProperty("je.emulatorID");
        java.io.File base = new java.io.File(userHome, ".je");
        if (emulatorId != null && !emulatorId.isEmpty()) base = new java.io.File(base, emulatorId);
        base.mkdirs();
        return new java.io.File(base, FILE_NAME);
    }

    public static synchronized void loadPreferences() {
        if (preferencesLoaded) return;
        java.io.File f = getPreferencesFile();
        if (f.isFile()) {
            java.util.Properties p = new java.util.Properties();
            try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
                p.load(in);
                Policy.offline = Boolean.parseBoolean(p.getProperty("offline", Boolean.toString(Policy.offline)));
                Policy.captivePortal = Boolean.parseBoolean(p.getProperty("captivePortal", Boolean.toString(Policy.captivePortal)));
                try { Policy.captivePort = Integer.parseInt(p.getProperty("captivePort", Integer.toString(Policy.captivePort))); } catch (NumberFormatException ignored) {}
                TLS.trustAll = Boolean.parseBoolean(p.getProperty("trustAll", Boolean.toString(TLS.trustAll)));
                try {
                    Traffic.bandwidthKbps = Integer.parseInt(p.getProperty("bandwidthKbps", Integer.toString(Traffic.bandwidthKbps)));
                    Traffic.latencyMs = Integer.parseInt(p.getProperty("latencyMs", Integer.toString(Traffic.latencyMs)));
                    Traffic.jitterMs = Integer.parseInt(p.getProperty("jitterMs", Integer.toString(Traffic.jitterMs)));
                    Traffic.packetLossPct = Integer.parseInt(p.getProperty("packetLossPct", Integer.toString(Traffic.packetLossPct)));
                } catch (NumberFormatException ignored) {}
                synchronized (Dns.class) {
                    Dns.clear();
                    for (String key : p.stringPropertyNames()) {
                        if (key.startsWith("dns.")) {
                            String host = key.substring(4);
                            String ip = p.getProperty(key);
                            if (host != null && !host.isEmpty() && ip != null && !ip.isEmpty()) {
                                Dns.put(host, ip);
                            }
                        }
                    }
                }
            } catch (java.io.IOException ignored) {}
        }
        preferencesLoaded = true;
        // Apply trustAll defaults to JVM if set
        try { TLS.applyDefaultContextIfNeeded(); } catch (Throwable ignored) {}
    }

    public static void savePreferencesAsync() {
        synchronized (NetConfig.class) {
            if (!preferencesLoaded) return;
            pendingSave = true;
            long now = System.currentTimeMillis();
            if (now - lastSaveTime >= SAVE_DEBOUNCE_MS) {
                savePreferences();
            } else {
                Thread t = new Thread(() -> {
                    try { Thread.sleep(SAVE_DEBOUNCE_MS); } catch (InterruptedException ignored) {}
                    synchronized (NetConfig.class) {
                        if (pendingSave && System.currentTimeMillis() - lastSaveTime >= SAVE_DEBOUNCE_MS) savePreferences();
                    }
                }, "NetPrefsSaver");
                t.setDaemon(true); t.start();
            }
        }
    }

    public static synchronized void savePreferences() {
        pendingSave = false; lastSaveTime = System.currentTimeMillis();
        java.util.Properties p = new java.util.Properties();
        p.setProperty("offline", Boolean.toString(Policy.offline));
        p.setProperty("captivePortal", Boolean.toString(Policy.captivePortal));
        p.setProperty("captivePort", Integer.toString(Policy.captivePort));
        p.setProperty("trustAll", Boolean.toString(TLS.trustAll));
        p.setProperty("bandwidthKbps", Integer.toString(Traffic.bandwidthKbps));
        p.setProperty("latencyMs", Integer.toString(Traffic.latencyMs));
        p.setProperty("jitterMs", Integer.toString(Traffic.jitterMs));
        p.setProperty("packetLossPct", Integer.toString(Traffic.packetLossPct));
        for (Map.Entry<String,String> e : Dns.snapshot().entrySet()) {
            p.setProperty("dns."+e.getKey(), e.getValue());
        }
        java.io.File f = getPreferencesFile();
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(f)) {
            p.store(out, "JarEngine Network Preferences");
        } catch (java.io.IOException ignored) {}
        
        // Show spinner after save completion
        Common.showConfigSpinner(800);
    }

    // Reset all network-related preferences and runtime toggles to defaults
    public static synchronized void resetToDefaults() {
        // Policy
        Policy.offline = false;
        Policy.captivePortal = false;
        Policy.captivePort = 8081;
        // TLS
        TLS.trustAll = false;
        TLS.resetDefaultContext();
        // Traffic shaping
        Traffic.bandwidthKbps = 0;
        Traffic.latencyMs = 0;
        Traffic.jitterMs = 0;
        Traffic.packetLossPct = 0;
        // DNS overrides
        Dns.clear();
        // Persist
        savePreferencesAsync();
    }
}
