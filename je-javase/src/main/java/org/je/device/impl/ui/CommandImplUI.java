package org.je.device.impl.ui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Image;

import org.je.device.ui.CommandUI;

public class CommandImplUI implements CommandUI {
	
	private Command command;

	public CommandImplUI(Command command) {
		this.command = command;
	}

	public Command getCommand() {
		return command;
	}

	public void setImage(Image image) {
		// TODO Auto-generated method stub		
	}

}
