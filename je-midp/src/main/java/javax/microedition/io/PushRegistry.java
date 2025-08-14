package javax.microedition.io;

import java.io.IOException;

import org.je.microedition.ImplFactory;
import org.je.microedition.io.PushRegistryDelegate;

public class PushRegistry {

	private static PushRegistryDelegate impl;

	static {
		impl = (PushRegistryDelegate) ImplFactory.getImplementation(PushRegistry.class, PushRegistryDelegate.class);
	}

	public static void registerConnection(String connection, String midlet, String filter)
			throws ClassNotFoundException, IOException {
		impl.registerConnection(connection, midlet, filter);
	}

	public static boolean unregisterConnection(String connection) {
		return impl.unregisterConnection(connection);
	}

	public static String[] listConnections(boolean available) {
		return impl.listConnections(available);
	}

	public static String getMIDlet(String connection) {
		return impl.getMIDlet(connection);
	}

	public static String getFilter(String connection) {
		return impl.getFilter(connection);
	}

	public static long registerAlarm(String midlet, long time) throws ClassNotFoundException,
			ConnectionNotFoundException {
		return impl.registerAlarm(midlet, time);
	}

}
