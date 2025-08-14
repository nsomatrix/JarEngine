package org.je.cldc.socket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import javax.microedition.io.StreamConnection;

public class ServerSocketConnection implements
		javax.microedition.io.ServerSocketConnection {
	
	private ServerSocket serverSocket;
	
	public ServerSocketConnection() throws IOException {
		serverSocket = new ServerSocket();
	}

	public ServerSocketConnection(int port) throws IOException {
		serverSocket = new ServerSocket(port);
	}

	public String getLocalAddress() throws IOException {
		InetAddress localHost = InetAddress.getLocalHost();
		return localHost.getHostAddress();
	}

	public int getLocalPort() throws IOException {
		return serverSocket.getLocalPort();
	}

	public StreamConnection acceptAndOpen() throws IOException {
		return new SocketConnection(serverSocket.accept());
	}

	public void close() throws IOException {
		serverSocket.close();
	}

}
