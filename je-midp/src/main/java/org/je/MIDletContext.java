package org.je;

import javax.microedition.midlet.MIDlet;

import org.je.app.launcher.Launcher;

/**
 * 
 * Represents context for running MIDlet.
 * Enables access to MIDlet, MIDletAccess by threadLocal using MIDletBridge.
 * Created before MIDlet.
 * 
 * Usage: MIDletBridge.getMIDletContext();
 * 
 * @author vlads
 *
 */
public class MIDletContext {

	private MIDletAccess midletAccess;
	
	public MIDletContext() {
		
	}
	
	public MIDletAccess getMIDletAccess() {
		return midletAccess;
	}
	
	protected void setMIDletAccess(MIDletAccess midletAccess) {
		this.midletAccess = midletAccess;	
	}
	
	public MIDlet getMIDlet() {
		if (midletAccess == null) {
			return null;
		}
		return midletAccess.midlet;
	}
	
	public boolean isLauncher() {
		return (getMIDlet() instanceof Launcher);
	}
}
