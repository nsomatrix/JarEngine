package org.je.device.impl;

import javax.microedition.lcdui.Command;


/**
 * A SoftButton can have an associated Command.
 */
public interface SoftButton 
{
	int TYPE_COMMAND = 1;
	int TYPE_ICON = 2;
	
	String getName();
	
	int getType();

  Command getCommand();
  
  void setCommand(Command cmd);
  
  boolean isVisible();
  
  void setVisible(boolean state);
  
  boolean isPressed();
  
  void setPressed(boolean state);
  
  Rectangle getPaintable();
  
  /**
   * Check if the command is of a type usually associated with this SoftButton.
   * E.g. "BACK" commands are normally placed only on a particular button.
   */
  boolean preferredCommandType(Command cmd);

}
