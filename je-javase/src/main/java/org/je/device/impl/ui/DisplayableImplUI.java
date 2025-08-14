package org.je.device.impl.ui;

import java.util.Vector;

import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

import org.je.DisplayAccess;
import org.je.MIDletAccess;
import org.je.MIDletBridge;
import org.je.device.ui.CommandUI;
import org.je.device.ui.DisplayableUI;

public class DisplayableImplUI implements DisplayableUI {
	
	protected Displayable displayable;
	
	private Vector commands = new Vector();
	
	protected DisplayableImplUI(Displayable displayable) {
		this.displayable = displayable;
	}

	public void addCommandUI(CommandUI cmd) {
		// Check that its not the same command
		for (int i = 0; i < commands.size(); i++) {
			if (cmd == (CommandUI) commands.elementAt(i)) {
				// Its the same just return
				return;
			}
		}

		// Now insert it in order
		boolean inserted = false;
		for (int i = 0; i < commands.size(); i++) {
			if (cmd.getCommand().getPriority() < ((CommandUI) commands.elementAt(i)).getCommand().getPriority()) {
				commands.insertElementAt(cmd, i);
				inserted = true;
				break;
			}
		}
		if (inserted == false) {
			// Not inserted just place it at the end
			commands.addElement(cmd);
		}		

		if (displayable.isShown()) {
			updateCommands();
		}
	}

	public void removeCommandUI(CommandUI cmd) {
		commands.removeElement(cmd);
		
		if (displayable.isShown()) {
			updateCommands();
		}
	}

	public void setCommandListener(CommandListener l) {
		// TODO Auto-generated method stub

	}
	
	public CommandListener getCommandListener() {
		// TODO Auto-generated method stub

		return null;
	}

	public void hideNotify() {
		// TODO Auto-generated method stub

	}

	public void showNotify() {
		updateCommands();
	}

	public void invalidate() {
		// TODO implement invalidate
	}
	
	public Vector getCommandsUI()
	{
		return commands;
	}

	private void updateCommands() {
		CommandManager.getInstance().updateCommands(getCommandsUI());
		MIDletAccess ma = MIDletBridge.getMIDletAccess();
		if (ma == null) {
			return;
		}
		DisplayAccess da = ma.getDisplayAccess();
		if (da == null) {
			return;
		}
		da.repaint();
	}

}
