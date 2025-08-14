package org.je.cldc;

import java.io.IOException;

import javax.microedition.io.Connection;

/**
 * 
 * @deprecated use <code>ConnectionImplementation</code> or <code>ConnectorAdapter</code> and <code>ImplFactory</code> to registed GCF protocol .
 *
 */
public interface ClosedConnection {

	Connection open(String name) throws IOException;

}
