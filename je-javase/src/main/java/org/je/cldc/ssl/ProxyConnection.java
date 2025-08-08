package org.je.cldc.ssl;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.je.util.ProxyConfig;

/**
 * Proxy-enabled SSL connection wrapper
 * Routes SSL connections through configured proxy
 */
public class ProxyConnection extends Connection {
    
    private ProxyConfig proxyConfig;
    
    public ProxyConnection() {
        super();
        this.proxyConfig = ProxyConfig.getInstance();
    }
    
    @Override
    public javax.microedition.io.Connection open(String name) throws IOException {
        if (!org.je.cldc.http.Connection.isAllowNetworkConnection()) {
            throw new IOException("No network");
        }
        
        int portSepIndex = name.lastIndexOf(':');
        int port = Integer.parseInt(name.substring(portSepIndex + 1));
        String host = name.substring("ssl://".length(), portSepIndex);
        
        // TODO validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(
                    X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(
                    X509Certificate[] certs, String authType) {
                }
            }
        };
        
        try {
            SSLContext sc = SSLContext.getInstance("SSL");			
            sc.init(null, trustAllCerts, new SecureRandom());
            SSLSocketFactory factory = sc.getSocketFactory();
            
            if (proxyConfig.isEnabled() && proxyConfig.isSocketEnabled()) {
                // Use SOCKS proxy for SSL connections
                Socket proxySocket = new Socket(proxyConfig.getSocketProxy());
                proxySocket.connect(new java.net.InetSocketAddress(host, port));
                socket = factory.createSocket(proxySocket, host, port, true);
            } else {
                // Direct SSL connection
                socket = factory.createSocket(host, port);
            }
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException(ex.toString());
        } catch (KeyManagementException ex) {
            throw new IOException(ex.toString());
        }
        
        return this;
    }
} 