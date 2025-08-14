package javax.microedition.lcdui;

import org.je.device.DeviceFactory;
import org.je.device.ui.CommandUI;

public class Command {

	public static final int SCREEN = 1;

	public static final int BACK = 2;

	public static final int CANCEL = 3;

	public static final int OK = 4;

	public static final int HELP = 5;

	public static final int STOP = 6;

	public static final int EXIT = 7;

	public static final int ITEM = 8;

	// this is needed to allow for item commands without 
	// breaking the existing interfaces
	// (yeah i know it's ugly) 
	// Andres Navarro
	private Command originalCommand;

	private Item focusedItem;

	private Command itemCommand;

	String label;

	int commandType;

	int priority;
	
	CommandUI ui;

	public Command(String label, int commandType, int priority) {
		this.label = label;
		this.commandType = commandType;
		this.priority = priority;
		
		this.ui = DeviceFactory.getDevice().getUIFactory().createCommandUI(this);
	}

	public Command(String shortLabel, String longLabel, int commandType, int priority) {
		// TODO implement
		this(shortLabel, commandType, priority);
	}

	public int getCommandType() {
		return commandType;
	}

	public String getLabel() {
		return label;
	}

	public String getLongLabel() {
		// TODO implement;
		return label;
	}

	public int getPriority() {
		return priority;
	}	
	
	/**
	 * @since MIDP 3.0
	 */
	public void setImage(Image image) {
		ui.setImage(image);
	}

	Item getFocusedItem() {
		if (isRegularCommand()) {
			throw new IllegalStateException();
		}
		// can't be null!
		return focusedItem;
	}

	Command getItemCommand(Item item) {
		if (!isRegularCommand()) {
			throw new IllegalStateException();
		}
		if (item == null) {
			throw new NullPointerException();
		}
		// we are allowed by the spec to threat all commands containes
		// in an Item as of the type ITEM
		if (itemCommand == null) {
			itemCommand = new Command(getLabel(), Command.ITEM, getPriority());
			itemCommand.originalCommand = this;
		}
		itemCommand.focusedItem = item;
		return itemCommand;
	}

	Command getOriginalCommand() {
		if (isRegularCommand()) {
			throw new IllegalStateException();
		}
		// can't be null!
		return originalCommand;
	}

	boolean isRegularCommand() {
		return originalCommand == null;
	}
}
