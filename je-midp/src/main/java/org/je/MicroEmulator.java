package org.je;

import java.io.InputStream;

import javax.microedition.io.ConnectionNotFoundException;

import org.je.app.launcher.Launcher;

public interface MicroEmulator {

	RecordStoreManager getRecordStoreManager();
	
	Launcher getLauncher();

	String getAppProperty(String key);
	
	InputStream getResourceAsStream(Class origClass, String name);

	void notifyDestroyed(MIDletContext midletContext);

	void destroyMIDletContext(MIDletContext midletContext);
	
	int checkPermission(String permission);
	
	boolean platformRequest(String URL) throws ConnectionNotFoundException;

	String getSuiteName();

}
