package org.je;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Item;

import org.je.device.ui.DisplayableUI;
import org.je.device.ui.ItemUI;


public interface DisplayAccess
{
	void commandAction(Command c, Displayable d);
	
	void commandAction(Command c, Item i);

	Display getDisplay();

	void keyPressed(int keyCode);

	void keyRepeated(int keyCode);

	void keyReleased(int keyCode);

	void pointerPressed(int x, int y);

	void pointerReleased(int x, int y);

	void pointerDragged(int x, int y);

	void paint(Graphics g);
	
	boolean isFullScreenMode();
	
	void hideNotify();
	   
    ItemUI getItemUI(Item item);
	
	Displayable getCurrent();

	DisplayableUI getDisplayableUI(Displayable displayable);

	void setCurrent(Displayable d);
	
	void sizeChanged();
  
	void repaint();

	void clean();

}
