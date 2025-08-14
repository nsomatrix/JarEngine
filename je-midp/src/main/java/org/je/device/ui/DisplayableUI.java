package org.je.device.ui;

import java.util.Vector;

import javax.microedition.lcdui.CommandListener;

public interface DisplayableUI {
	
	void addCommandUI(CommandUI cmd);
	
	void removeCommandUI(CommandUI cmd);
	
	CommandListener getCommandListener();
	
	void setCommandListener(CommandListener l);

	void hideNotify();

	void showNotify();

	void invalidate();

	// TODO remove method
	Vector getCommandsUI();
	
}
