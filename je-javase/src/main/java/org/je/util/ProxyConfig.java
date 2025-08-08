package org.je.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Proxy configuration manager for JarEngine emulator
 * Handles proxy settings and provides proxy instances for different protocols
 */
public class ProxyConfig {
    
    private static final Logger logger = Logger.getLogger(ProxyConfig.class.getName());
    
    private static ProxyConfig instance;
    
    private boolean enabled = false;
    private String proxyHost = "127.0.0.1";
    private int proxyPort = 8080;
    private String proxyUsername = "";
    private String proxyPassword = "";
    private boolean useAuthentication = false;
    
    // Protocol-specific settings
    private boolean httpEnabled = true;
    private boolean httpsEnabled = true;
    private boolean socketEnabled = true;
    
    private ProxyConfig() {
        loadConfig();
    }
    
    public static synchronized ProxyConfig getInstance() {
        if (instance == null) {
            instance = new ProxyConfig();
        }
        return instance;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        saveConfig();
    }
    
    public String getProxyHost() {
        return proxyHost;
    }
    
    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
        saveConfig();
    }
    
    public int getProxyPort() {
        return proxyPort;
    }
    
    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
        saveConfig();
    }
    
    public String getProxyUsername() {
        return proxyUsername;
    }
    
    public void setProxyUsername(String proxyUsername) {
        this.proxyUsername = proxyUsername;
        saveConfig();
    }
    
    public String getProxyPassword() {
        return proxyPassword;
    }
    
    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
        saveConfig();
    }
    
    public boolean isUseAuthentication() {
        return useAuthentication;
    }
    
    public void setUseAuthentication(boolean useAuthentication) {
        this.useAuthentication = useAuthentication;
        saveConfig();
    }
    
    public boolean isHttpEnabled() {
        return httpEnabled;
    }
    
    public void setHttpEnabled(boolean httpEnabled) {
        this.httpEnabled = httpEnabled;
        saveConfig();
    }
    
    public boolean isHttpsEnabled() {
        return httpsEnabled;
    }
    
    public void setHttpsEnabled(boolean httpsEnabled) {
        this.httpsEnabled = httpsEnabled;
        saveConfig();
    }
    
    public boolean isSocketEnabled() {
        return socketEnabled;
    }
    
    public void setSocketEnabled(boolean socketEnabled) {
        this.socketEnabled = socketEnabled;
        saveConfig();
    }
    
    /**
     * Get a proxy instance for HTTP connections
     */
    public Proxy getHttpProxy() {
        if (!enabled || !httpEnabled) {
            return Proxy.NO_PROXY;
        }
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
    }
    
    /**
     * Get a proxy instance for HTTPS connections
     */
    public Proxy getHttpsProxy() {
        if (!enabled || !httpsEnabled) {
            return Proxy.NO_PROXY;
        }
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
    }
    
    /**
     * Get a proxy instance for Socket connections
     */
    public Proxy getSocketProxy() {
        if (!enabled || !socketEnabled) {
            return Proxy.NO_PROXY;
        }
        return new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, proxyPort));
    }
    
    /**
     * Test the proxy connection
     */
    public boolean testProxy() {
        if (!enabled) {
            return false;
        }
        
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(proxyHost, proxyPort), 5000);
            socket.close();
            return true;
        } catch (IOException e) {
            logger.warning("Proxy test failed: " + e.getMessage());
            return false;
        }
    }
    
    private void loadConfig() {
        try {
            Properties props = new Properties();
            
            // Try to load from user directory first
            java.io.File configDir = new java.io.File(System.getProperty("user.home"), ".jarengine");
            java.io.File configFile = new java.io.File(configDir, "proxy.properties");
            
            if (configFile.exists()) {
                props.load(new java.io.FileInputStream(configFile));
            } else {
                // Try to load from classpath as fallback
                try {
                    props.load(getClass().getResourceAsStream("/proxy.properties"));
                } catch (Exception e) {
                    // Use defaults if no config file found
                }
            }
            
            enabled = Boolean.parseBoolean(props.getProperty("proxy.enabled", "false"));
            proxyHost = props.getProperty("proxy.host", "127.0.0.1");
            proxyPort = Integer.parseInt(props.getProperty("proxy.port", "8080"));
            proxyUsername = props.getProperty("proxy.username", "");
            proxyPassword = props.getProperty("proxy.password", "");
            useAuthentication = Boolean.parseBoolean(props.getProperty("proxy.auth", "false"));
            httpEnabled = Boolean.parseBoolean(props.getProperty("proxy.http.enabled", "true"));
            httpsEnabled = Boolean.parseBoolean(props.getProperty("proxy.https.enabled", "true"));
            socketEnabled = Boolean.parseBoolean(props.getProperty("proxy.socket.enabled", "true"));
            
        } catch (Exception e) {
            logger.warning("Could not load proxy configuration: " + e.getMessage());
        }
    }
    
    private void saveConfig() {
        try {
            Properties props = new Properties();
            props.setProperty("proxy.enabled", String.valueOf(enabled));
            props.setProperty("proxy.host", proxyHost);
            props.setProperty("proxy.port", String.valueOf(proxyPort));
            props.setProperty("proxy.username", proxyUsername);
            props.setProperty("proxy.password", proxyPassword);
            props.setProperty("proxy.auth", String.valueOf(useAuthentication));
            props.setProperty("proxy.http.enabled", String.valueOf(httpEnabled));
            props.setProperty("proxy.https.enabled", String.valueOf(httpsEnabled));
            props.setProperty("proxy.socket.enabled", String.valueOf(socketEnabled));
            
            // Save to user directory
            java.io.File configDir = new java.io.File(System.getProperty("user.home"), ".jarengine");
            configDir.mkdirs();
            java.io.File configFile = new java.io.File(configDir, "proxy.properties");
            props.store(new java.io.FileOutputStream(configFile), "JarEngine Proxy Configuration");
            
        } catch (Exception e) {
            logger.warning("Could not save proxy configuration: " + e.getMessage());
        }
    }
} 