/*
 *  MicroEmulator
 *  Copyright (C) 2006 Bartek Teodorczyk <barteo@barteo.net>
 *
 *  It is licensed under the following two licenses as alternatives:
 *    1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 *    2. Apache License (the "AL") Version 2.0
 *
 *  You may not use this file except in compliance with at least one of
 *  the above two licenses.
 *
 *  You may obtain a copy of the LGPL at
 *      http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 *
 *  You may obtain a copy of the AL at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the LGPL or the AL for the specific language governing permissions and
 *  limitations.
 */

package org.je.cldc.https;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.microedition.io.HttpsConnection;
import javax.microedition.io.SecurityInfo;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLContext;

import org.je.cldc.CertificateImpl;
import org.je.cldc.SecurityInfoImpl;
import org.je.log.Logger;
import org.je.util.net.NetConfig;
import org.je.util.NetEventBus;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.InetAddress;
import java.net.URLConnection;
import java.net.HttpURLConnection;

public class Connection extends org.je.cldc.http.Connection implements HttpsConnection {

	private SSLContext sslContext;

	private SecurityInfo securityInfo;

	public Connection() {
	    try {
			sslContext = SSLContext.getInstance("TLS");
		} catch (NoSuchAlgorithmException ex) {
			Logger.error(ex);
		}

		securityInfo = null;
	}

	public SecurityInfo getSecurityInfo() throws IOException {
		if (securityInfo == null) {
		    if (cn == null) {
				throw new IOException();
			}
			if (!connected) {
				cn.connect();
				connected = true;
			}
			HttpsURLConnection https = (HttpsURLConnection) cn;

			Certificate[] certs = https.getServerCertificates();
			if (certs.length == 0) {
				throw new IOException();
			}
			securityInfo = new SecurityInfoImpl(
					https.getCipherSuite(),
					sslContext.getProtocol(),
					new CertificateImpl((X509Certificate) certs[0]));
		}

		return securityInfo;
	}

	public String getProtocol() {
		return "https";
	}

	@Override
	public javax.microedition.io.Connection openConnection(String name, int mode, boolean timeouts) throws IOException {
		if (!isAllowNetworkConnection() || NetConfig.Policy.offline) {
			throw new IOException("No network");
		}
		URL url;
		try { url = new URL(name); } catch (MalformedURLException ex) { throw new IOException(ex.toString()); }
		this.originalUrl = url;
		// Captive portal: simulate redirect without network
		if (NetConfig.Policy.captivePortal) {
			cn = new CaptiveHttpsURLConnection(url);
			try { NetEventBus.publish("HTTPS", "OUT", url.toString(), "captive-302"); } catch (Throwable ignore) {}
			return this;
		}

		// Create connection
		URLConnection ucn = url.openConnection();
		if (ucn instanceof HttpsURLConnection) {
			HttpsURLConnection https = (HttpsURLConnection) ucn;
			// Apply DNS override with SNI if override exists
			try {
				String host = url.getHost();
				InetAddress resolved = NetConfig.Dns.resolveHost(host);
				// If resolved differs from default resolution, use SNI+IP wrapper; we can't detect default easily, so always wrap
				SSLSocketFactory base = NetConfig.TLS.getBaseFactory();
				SSLSocketFactory sni = NetConfig.TLS.wrapWithSniAndIp(base, host, resolved);
				https.setSSLSocketFactory(sni);
			} catch (Exception ignored) {}
			// Apply trust-all hostname verifier if enabled
			if (NetConfig.TLS.trustAll) {
				NetConfig.TLS.applyDefaultContextIfNeeded();
				https.setHostnameVerifier((h,s) -> true);
			}
			https.setInstanceFollowRedirects(false);
		}
		cn = ucn;
		cn.setDoOutput(true);
		try { NetEventBus.publish("HTTPS", "OUT", url.toString(), "open"); } catch (Throwable ignore) {}
		return this;
	}


    /**
     * Returns the network port number of the URL for this HttpsConnection
     *
     * @return  the network port number of the URL for this HttpsConnection. The default HTTPS port number (443) is returned if there was no port number in the string passed to Connector.open.
     */
	public int getPort() {
		if (cn == null) {
			return -1;
		}
		int port = cn.getURL().getPort();
		if (port == -1) {
			return 443;
		}
		return port;
	}

}

// Minimal captive portal 302 response wrapper for HTTPS path (shares behavior with HTTP variant)
class CaptiveHttpsURLConnection extends HttpURLConnection {
	private boolean prepared = false;
	private int code = 302;
	private java.util.Map<String,String> headers = new java.util.LinkedHashMap<>();
	private java.io.ByteArrayInputStream body;

	protected CaptiveHttpsURLConnection(URL u) { super(u); }

	@Override public void disconnect() { /* no-op */ }
	@Override public boolean usingProxy() { return false; }
	@Override public void connect() throws IOException { ensurePrepared(); connected = true; }

	private void ensurePrepared() {
		if (prepared) return;
		String loc = "http://127.0.0.1:" + NetConfig.Policy.captivePort + "/";
		headers.put("Location", loc);
		headers.put("Content-Type", "text/plain; charset=utf-8");
		byte[] msg = ("Redirecting to captive portal: " + loc + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
		body = new java.io.ByteArrayInputStream(msg);
		prepared = true;
	}

	@Override public int getResponseCode() throws IOException { ensurePrepared(); return code; }
	@Override public String getResponseMessage() throws IOException { return "Found"; }
	@Override public String getHeaderField(String name) { ensurePrepared(); return headers.get(name); }
	@Override public String getHeaderFieldKey(int n) { ensurePrepared(); if (n==0) return "Location"; if (n==1) return "Content-Type"; return null; }
	@Override public String getHeaderField(int n) { ensurePrepared(); if (n==0) return headers.get("Location"); if (n==1) return headers.get("Content-Type"); return null; }
	@Override public java.io.InputStream getInputStream() throws IOException { ensurePrepared(); return body; }
	@Override public java.io.OutputStream getOutputStream() throws IOException { return new java.io.ByteArrayOutputStream(); }
}
