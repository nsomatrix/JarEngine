package org.je.microedition.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.InputConnection;
import javax.microedition.io.OutputConnection;

/**
 * @author vlads 
 * 
 * Default Connector
 */
public abstract class ConnectorAdapter implements ConnectorDelegate {

	public abstract Connection open(String name, int mode, boolean timeouts) throws IOException;

	public Connection open(String name) throws IOException {
		return open(name, Connector.READ_WRITE, false);
	}

	public Connection open(String name, int mode) throws IOException {
		return open(name, mode, false);
	}

	public DataInputStream openDataInputStream(String name) throws IOException {
		return ((InputConnection) open(name)).openDataInputStream();
	}

	public DataOutputStream openDataOutputStream(String name) throws IOException {
		return ((OutputConnection) open(name)).openDataOutputStream();
	}

	public InputStream openInputStream(String name) throws IOException {
		return ((InputConnection) open(name)).openInputStream();
	}

	public OutputStream openOutputStream(String name) throws IOException {
		return ((OutputConnection) open(name)).openOutputStream();
	}

}
