package org.je.cldc.socket;

import java.io.IOException;
import java.net.Socket;

import org.je.util.ProxyConfig;

/**
 * Proxy-enabled Socket connection wrapper
 * Routes Socket connections through configured proxy
 */
public class ProxyConnection extends Connection {
    
    private ProxyConfig proxyConfig;
    
    public ProxyConnection() {
        this.proxyConfig = ProxyConfig.getInstance();
    }
    
    @Override
    public javax.microedition.io.Connection open(String name) throws IOException {
        if (!org.je.cldc.http.Connection.isAllowNetworkConnection()) {
            throw new IOException("No network");
        }

        int port = -1;
        int portSepIndex = name.lastIndexOf(':');
        if (portSepIndex == -1) {
            throw new IllegalArgumentException("Port missing");
        }
        String portToParse = name.substring(portSepIndex + 1);
        if (portToParse.length() > 0) {
            port = Integer.parseInt(portToParse);
        }
        String host = name.substring("socket://".length(), portSepIndex);

        if (host.length() > 0) {
            if (port == -1) {
                throw new IllegalArgumentException("Port missing");
            }
            return new ProxySocketConnection(host, port);
        } else {
            if (port == -1) {
                return new ServerSocketConnection();
            } else {
                return new ServerSocketConnection(port);
            }
        }
    }
    
    /**
     * Proxy-enabled SocketConnection implementation
     */
    private class ProxySocketConnection extends SocketConnection {
        
        public ProxySocketConnection(String host, int port) throws IOException {
            super();
            
            if (proxyConfig.isEnabled() && proxyConfig.isSocketEnabled()) {
                // Use SOCKS proxy
                socket = new Socket(proxyConfig.getSocketProxy());
                socket.connect(new java.net.InetSocketAddress(host, port));
            } else {
                // Direct connection
                socket = new Socket(host, port);
            }
        }
    }
} 