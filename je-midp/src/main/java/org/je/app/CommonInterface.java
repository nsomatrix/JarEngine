package org.je.app;

import javax.microedition.midlet.MIDlet;

import org.je.MIDletEntry;

public interface CommonInterface {

	MIDlet initMIDlet(boolean startMidlet, MIDletEntry entry);
	
	String getCurrentTheme();

}
