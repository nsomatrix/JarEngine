package org.je.cldc.https;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;

import org.je.util.ProxyConfig;

/**
 * Proxy-enabled HTTPS connection wrapper
 * Routes HTTPS connections through configured proxy
 */
public class ProxyConnection extends Connection {
    
    private ProxyConfig proxyConfig;
    
    public ProxyConnection() {
        super();
        this.proxyConfig = ProxyConfig.getInstance();
    }
    
    @Override
    public javax.microedition.io.Connection openConnection(String name, int mode, boolean timeouts) throws IOException {
        if (!isAllowNetworkConnection()) {
            throw new IOException("No network");
        }
        
        URL url;
        try {
            url = new URL(name);
        } catch (Exception ex) {
            throw new IOException(ex.toString());
        }
        
        // Use proxy if enabled
        if (proxyConfig.isEnabled() && proxyConfig.isHttpsEnabled()) {
            cn = url.openConnection(proxyConfig.getHttpsProxy());
        } else {
            cn = url.openConnection();
        }
        
        cn.setDoOutput(true);
        
        // Set authentication if required
        if (proxyConfig.isEnabled() && proxyConfig.isUseAuthentication() && 
            !proxyConfig.getProxyUsername().isEmpty()) {
            String auth = proxyConfig.getProxyUsername() + ":" + proxyConfig.getProxyPassword();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            cn.setRequestProperty("Proxy-Authorization", "Basic " + encodedAuth);
        }
        
        // J2ME do not follow redirects
        if (cn instanceof HttpURLConnection) {
            ((HttpURLConnection) cn).setInstanceFollowRedirects(false);
        }
        
        return this;
    }
} 