package org.je.cldc.socket;

import java.io.IOException;

import org.je.cldc.ClosedConnection;

public class Connection implements ClosedConnection {

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
			return new SocketConnection(host, port);
		} else {
			if (port == -1) {
				return new ServerSocketConnection();
			} else {
				return new ServerSocketConnection(port);
			}
		}
	}

	public void close() throws IOException {
		// Implemented in SocketConnection or ServerSocketConnection
	}

}
