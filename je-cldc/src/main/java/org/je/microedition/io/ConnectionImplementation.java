package org.je.microedition.io;

import java.io.IOException;

import javax.microedition.io.Connection;

/**
 * 
 * This proper name for original <code>ClosedConnection</code> interface.
 * 
 * @author vlads
 *
 */
public interface ConnectionImplementation {
	
	public Connection openConnection(String name, int mode, boolean timeouts) throws IOException;
	
}
