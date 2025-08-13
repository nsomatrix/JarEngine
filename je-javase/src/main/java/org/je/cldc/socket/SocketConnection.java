/*
 *  MicroEmulator
 *  Copyright (C) 2001-2003 Bartek Teodorczyk <barteo@barteo.net>
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

package org.je.cldc.socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.je.util.NetEventBus;
import org.je.util.net.NetConfig;

public class SocketConnection implements javax.microedition.io.SocketConnection {

	protected Socket socket;
	
	public SocketConnection() {		
	}

	public SocketConnection(String host, int port) throws IOException {
		if (NetConfig.Policy.offline) throw new IOException("No network");
		String targetHost = host; int targetPort = port;
		InetAddress addr;
		if (NetConfig.Policy.captivePortal) {
			addr = InetAddress.getByName("127.0.0.1");
			targetPort = NetConfig.Policy.captivePort;
		} else {
			addr = NetConfig.Dns.resolveHost(host);
		}
		this.socket = new Socket();
		// initial latency
		try { Thread.sleep(Math.max(0, NetConfig.Traffic.latencyMs)); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
		this.socket.connect(new InetSocketAddress(addr, targetPort));
		try { NetEventBus.publish("TCP", "OUT", targetHost+":"+targetPort, "connect"); } catch (Throwable ignore) {}
	}
	
	public SocketConnection(Socket socket) {
		this.socket = socket;
	}

	public String getAddress() throws IOException {
		if (socket == null || socket.isClosed()) {
			throw new IOException();
		}

		return socket.getInetAddress().toString();
	}

	public String getLocalAddress() throws IOException {
		if (socket == null || socket.isClosed()) {
			throw new IOException();
		}

		return socket.getLocalAddress().toString();
	}

	public int getLocalPort() throws IOException {
		if (socket == null || socket.isClosed()) {
			throw new IOException();
		}

		return socket.getLocalPort();
	}

	public int getPort() throws IOException {
		if (socket == null || socket.isClosed()) {
			throw new IOException();
		}

		return socket.getPort();
	}

	public int getSocketOption(byte option) throws IllegalArgumentException,
			IOException {
		if (socket != null && socket.isClosed()) {
			throw new IOException();
		}
		switch (option) {
		case DELAY:
			if (socket.getTcpNoDelay()) {
				return 1;
			} else {
				return 0;
			}
		case LINGER:
			int value = socket.getSoLinger();
			if (value == -1) {
				return 0;
			} else {
				return value;
			}
		case KEEPALIVE:
			if (socket.getKeepAlive()) {
				return 1;
			} else {
				return 0;
			}
		case RCVBUF:
			return socket.getReceiveBufferSize();
		case SNDBUF:
			return socket.getSendBufferSize();
		default:
			throw new IllegalArgumentException();
		}
	}

	public void setSocketOption(byte option, int value)
			throws IllegalArgumentException, IOException {
		if (socket.isClosed()) {
			throw new IOException();
		}
		switch (option) {
		case DELAY:
			int delay;
			if (value == 0) {
				delay = 0;
			} else {
				delay = 1;
			}
			socket.setTcpNoDelay(delay == 0 ? false : true);
			break;
		case LINGER:
			if (value < 0) {
				throw new IllegalArgumentException();
			}
			socket.setSoLinger(value == 0 ? false : true, value);
			break;
		case KEEPALIVE:
			int keepalive;
			if (value == 0) {
				keepalive = 0;
			} else {
				keepalive = 1;
			}
			socket.setKeepAlive(keepalive == 0 ? false : true);
			break;
		case RCVBUF:
			if (value <= 0) {
				throw new IllegalArgumentException();
			}
			socket.setReceiveBufferSize(value);
			break;
		case SNDBUF:
			if (value <= 0) {
				throw new IllegalArgumentException();
			}
			socket.setSendBufferSize(value);
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	public void close() throws IOException {
		// TODO fix differences between Java ME and Java SE
		try {
			String target;
			try { target = getAddress()+":"+getPort(); } catch (Throwable t) { target = "?"; }
			NetEventBus.publish("TCP", "IN", target, "close");
		} catch (Throwable ignore) {}
		socket.close();
	}

	public InputStream openInputStream() throws IOException {
		InputStream in = socket.getInputStream();
		// Wrap for traffic shaping
		in = NetConfig.Traffic.wrapInput(in);
		try { NetEventBus.publish("TCP", "IN", getAddress()+":"+getPort(), "openIn"); } catch (Throwable ignore) {}
		return in;
	}

	public DataInputStream openDataInputStream() throws IOException {
		return new DataInputStream(openInputStream());
	}

	public OutputStream openOutputStream() throws IOException {
		OutputStream out = socket.getOutputStream();
		// Wrap for traffic shaping
		out = NetConfig.Traffic.wrapOutput(out);
		try { NetEventBus.publish("TCP", "OUT", socket.getInetAddress().getHostAddress()+":"+socket.getPort(), "openOut"); } catch (Throwable ignore) {}
		return out;
	}

	public DataOutputStream openDataOutputStream() throws IOException {
		return new DataOutputStream(openOutputStream());
	}

}
