/**
 *  MicroEmulator
 *  Copyright (C) 2001,2002 Bartek Teodorczyk <barteo@barteo.net>
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

package org.je.cldc.http;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.InetAddress;

import javax.microedition.io.HttpConnection;

import org.je.microedition.io.ConnectionImplementation;
import org.je.util.NetEventBus;
import org.je.util.net.NetConfig;

public class Connection implements HttpConnection, ConnectionImplementation {

	protected URLConnection cn;

	protected boolean connected = false;

	protected static boolean allowNetworkConnection = true;

	// Preserve original URL components even if we internally rewrite the target (e.g., DNS override)
	protected URL originalUrl;

	public javax.microedition.io.Connection openConnection(String name, int mode, boolean timeouts) throws IOException {
		if (!isAllowNetworkConnection() || NetConfig.Policy.offline) {
			throw new IOException("No network");
		}
	URL url;
		try {
			url = new URL(name);
		} catch (MalformedURLException ex) {
			throw new IOException(ex.toString());
		}
	this.originalUrl = url;
		// Captive portal simulation: return a 302 redirect without network
		if (NetConfig.Policy.captivePortal && ("http".equalsIgnoreCase(url.getProtocol()) || "https".equalsIgnoreCase(url.getProtocol()))) {
			cn = new CaptiveHttpURLConnection(url);
			try { NetEventBus.publish("HTTP", "OUT", url.toString(), "captive-302"); } catch (Throwable ignore) {}
			return this;
		}

		// HTTP DNS override: for http (not https), resolve host override and connect to IP, setting Host header
		if ("http".equalsIgnoreCase(url.getProtocol())) {
			try {
				String originalHost = url.getHost();
				InetAddress resolved = NetConfig.Dns.resolveHost(originalHost);
				String ip = resolved.getHostAddress();
				int port = url.getPort();
				if (port == -1) port = 80;
				URL rewritten = new URL(url.getProtocol(), ip, port, url.getFile());
				cn = rewritten.openConnection();
				// Preserve original Host header (with port if non-default)
				String hostHeader = originalHost + (port != 80 ? (":" + port) : "");
				cn.setRequestProperty("Host", hostHeader);
			} catch (Exception e) {
				// Fallback to default behavior if resolution fails
				cn = url.openConnection();
			}
		} else {
			cn = url.openConnection();
		}
		cn.setDoOutput(true);
		// Apply TLS trust-all if enabled
		NetConfig.TLS.applyToHttpsIfNeeded(cn);
		// J2ME do not follow redirects. Test this url
		// http://www.je.org/test/r/
		if (cn instanceof HttpURLConnection) {
			((HttpURLConnection) cn).setInstanceFollowRedirects(false);
		}
		// Publish open event
		try {
			NetEventBus.publish("HTTP", "OUT", url.toString(), "open");
		} catch (Throwable ignore) {}
		return this;
	}

	public void close() throws IOException {
		if (cn == null) {
			return;
		}

		if (cn instanceof HttpURLConnection) {
			((HttpURLConnection) cn).disconnect();
		}

		cn = null;
	}

	public String getURL() {
	if (originalUrl == null) {
			return null;
		}

	return originalUrl.toString();
	}

	public String getProtocol() {
		return "http";
	}

	public String getHost() {
	if (originalUrl == null) {
			return null;
		}

	return originalUrl.getHost();
	}

	public String getFile() {
	if (originalUrl == null) {
			return null;
		}

	return originalUrl.getFile();
	}

	public String getRef() {
	if (originalUrl == null) {
			return null;
		}

	return originalUrl.getRef();
	}

	public String getQuery() {
		if (cn == null) {
			return null;
		}

		// return cn.getURL().getQuery();
		return null;
	}

	public int getPort() {
	if (originalUrl == null) {
			return -1;
		}

	int port = originalUrl.getPort();
		if (port == -1) {
			return 80;
		}
		return port;
	}

	public String getRequestMethod() {
		if (cn == null) {
			return null;
		}

		if (cn instanceof HttpURLConnection) {
			return ((HttpURLConnection) cn).getRequestMethod();
		} else {
			return null;
		}
	}

	public void setRequestMethod(String method) throws IOException {
		if (cn == null) {
			throw new IOException();
		}

		if (method.equals(HttpConnection.POST)) {
			cn.setDoOutput(true);
		}

		if (cn instanceof HttpURLConnection) {
			((HttpURLConnection) cn).setRequestMethod(method);
		}
	}

	public String getRequestProperty(String key) {
		if (cn == null) {
			return null;
		}

		return cn.getRequestProperty(key);
	}

	public void setRequestProperty(String key, String value) throws IOException {
		if (cn == null || connected) {
			throw new IOException();
		}

		cn.setRequestProperty(key, value);
	}

	public int getResponseCode() throws IOException {
		if (cn == null) {
			throw new IOException();
		}
		if (!connected) {
			cn.connect();
			connected = true;
		}

		if (cn instanceof HttpURLConnection) {
			int code = ((HttpURLConnection) cn).getResponseCode();
			try { NetEventBus.publish("HTTP", "IN", getURL(), "code="+code); } catch (Throwable ignore) {}
			return code;
		} else {
			return -1;
		}
	}

	public String getResponseMessage() throws IOException {
		if (cn == null) {
			throw new IOException();
		}
		if (!connected) {
			cn.connect();
			connected = true;
		}

		if (cn instanceof HttpURLConnection) {
			return ((HttpURLConnection) cn).getResponseMessage();
		} else {
			return null;
		}
	}

	public long getExpiration() throws IOException {
		if (cn == null) {
			throw new IOException();
		}
		if (!connected) {
			cn.connect();
			connected = true;
		}

		return cn.getExpiration();
	}

	public long getDate() throws IOException {
		if (cn == null) {
			throw new IOException();
		}
		if (!connected) {
			cn.connect();
			connected = true;
		}

		return cn.getDate();
	}

	public long getLastModified() throws IOException {
		if (cn == null) {
			throw new IOException();
		}
		if (!connected) {
			cn.connect();
			connected = true;
		}

		return cn.getLastModified();
	}

	public String getHeaderField(String name) throws IOException {
		if (cn == null) {
			throw new IOException();
		}
		if (!connected) {
			cn.connect();
			connected = true;
		}

		return cn.getHeaderField(name);
	}

	public int getHeaderFieldInt(String name, int def) throws IOException {
		if (cn == null) {
			throw new IOException();
		}
		if (!connected) {
			cn.connect();
			connected = true;
		}

		return cn.getHeaderFieldInt(name, def);
	}

	public long getHeaderFieldDate(String name, long def) throws IOException {
		if (cn == null) {
			throw new IOException();
		}
		if (!connected) {
			cn.connect();
			connected = true;
		}

		return cn.getHeaderFieldDate(name, def);
	}

	public String getHeaderField(int n) throws IOException {
		if (cn == null) {
			throw new IOException();
		}
		if (!connected) {
			cn.connect();
			connected = true;
		}

		return cn.getHeaderField(getImplIndex(n));
	}

	public String getHeaderFieldKey(int n) throws IOException {
		if (cn == null) {
			throw new IOException();
		}
		if (!connected) {
			cn.connect();
			connected = true;
		}

		return cn.getHeaderFieldKey(getImplIndex(n));
	}

	private int getImplIndex(int index){
		if (cn.getHeaderFieldKey(0) == null && cn.getHeaderField(0) != null){
			index++;
		}
		return index;
	}

	public InputStream openInputStream() throws IOException {
		if (cn == null) {
			throw new IOException();
		}

		connected = true;

		InputStream in = cn.getInputStream();
		// Wrap for traffic shaping
		in = NetConfig.Traffic.wrapInput(in);
		return in;
	}

	public DataInputStream openDataInputStream() throws IOException {
		return new DataInputStream(openInputStream());
	}

	public OutputStream openOutputStream() throws IOException {
		if (cn == null) {
			throw new IOException();
		}

		connected = true;

		OutputStream out = cn.getOutputStream();
		// Wrap for traffic shaping
		out = NetConfig.Traffic.wrapOutput(out);
		return out;
	}

	public DataOutputStream openDataOutputStream() throws IOException {
		return new DataOutputStream(openOutputStream());
	}

	public String getType() {
		try {
			return getHeaderField("content-type");
		} catch (IOException ex) {
			return null;
		}
	}

	public String getEncoding() {
		try {
			return getHeaderField("content-encoding");
		} catch (IOException ex) {
			return null;
		}
	}

	public long getLength() {
		try {
			return getHeaderFieldInt("content-length", -1);
		} catch (IOException ex) {
			return -1;
		}
	}

	public static boolean isAllowNetworkConnection() {
		return allowNetworkConnection;
	}

	public static void setAllowNetworkConnection(boolean allowNetworkConnection) {
		Connection.allowNetworkConnection = allowNetworkConnection;
	}

}

// Minimal captive portal 302 response wrapper
class CaptiveHttpURLConnection extends HttpURLConnection {
	private boolean prepared = false;
	private int code = 302;
	private java.util.Map<String,String> headers = new java.util.LinkedHashMap<>();
	private java.io.ByteArrayInputStream body;

	protected CaptiveHttpURLConnection(URL u) {
		super(u);
	}

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
