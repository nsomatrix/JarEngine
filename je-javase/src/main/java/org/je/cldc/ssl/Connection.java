package org.je.cldc.ssl;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.microedition.io.SecureConnection;
import javax.microedition.io.SecurityInfo;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.je.cldc.CertificateImpl;
import org.je.cldc.ClosedConnection;
import org.je.cldc.SecurityInfoImpl;

public class Connection extends org.je.cldc.socket.SocketConnection implements SecureConnection, ClosedConnection {
	
	private SecurityInfo securityInfo;
	
	public Connection() {
		securityInfo = null;
	}

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
			socket = factory.createSocket(host, port);
		} catch (NoSuchAlgorithmException ex) {
			throw new IOException(ex.toString());
		} catch (KeyManagementException ex) {
			throw new IOException(ex.toString());
		}
		
		return this;
	}

	public void close() throws IOException {
		// TODO fix differences between Java ME and Java SE
		
		socket.close();
	}

	public SecurityInfo getSecurityInfo() throws IOException {
		if (securityInfo == null) {
			SSLSession session = ((SSLSocket) socket).getSession();
			
			Certificate[] certs = session.getPeerCertificates();
			if (certs.length == 0) {
				throw new IOException();
			}
			
			securityInfo = new SecurityInfoImpl(
					session.getCipherSuite(),
					session.getProtocol(),
					new CertificateImpl((X509Certificate) certs[0]));
		}

		return securityInfo;
	}

}
