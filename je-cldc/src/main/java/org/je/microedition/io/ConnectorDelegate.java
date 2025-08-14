package org.je.microedition.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connection;

import org.je.microedition.Implementation;

/**
 * Delegate for javax.microedition.Connector
 * 
 * @author vlads
 */
public interface ConnectorDelegate extends Implementation {

	public Connection open(String name) throws IOException;
	
	public Connection open(String name, int mode) throws IOException;

	public Connection open(String name, int mode, boolean timeouts) throws IOException;

	public DataInputStream openDataInputStream(String name) throws IOException;

	public DataOutputStream openDataOutputStream(String name) throws IOException;

	public InputStream openInputStream(String name) throws IOException;
  
	public OutputStream openOutputStream(String name) throws IOException;
	
}
