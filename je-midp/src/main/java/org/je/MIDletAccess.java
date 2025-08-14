package org.je;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import org.je.DisplayAccess;

/**
 * 
 * Enables access to MIDlet protected methods.
 *
 */
public abstract class MIDletAccess {
	
	public MIDlet midlet;

	private DisplayAccess displayAccess;

	public MIDletAccess(MIDlet amidlet) {
		midlet = amidlet;
	}

	public DisplayAccess getDisplayAccess() {
		return displayAccess;
	}

	public void setDisplayAccess(DisplayAccess adisplayAccess) {
		displayAccess = adisplayAccess;
	}

	public abstract void startApp() throws MIDletStateChangeException;

	public abstract void pauseApp();

	public abstract void destroyApp(boolean unconditional) throws MIDletStateChangeException;

}