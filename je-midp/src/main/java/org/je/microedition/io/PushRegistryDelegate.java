package org.je.microedition.io;

import java.io.IOException;

import javax.microedition.io.ConnectionNotFoundException;

public interface PushRegistryDelegate {

	public void registerConnection(String connection, String midlet, String filter) throws ClassNotFoundException,
			IOException;

	public boolean unregisterConnection(String connection);

	public String[] listConnections(boolean available);

	public String getMIDlet(String connection);

	public String getFilter(String connection);

	public long registerAlarm(String midlet, long time)

	throws ClassNotFoundException, ConnectionNotFoundException;

}
