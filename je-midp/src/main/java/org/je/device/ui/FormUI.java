package org.je.device.ui;

import javax.microedition.lcdui.ItemStateListener;

public interface FormUI extends DisplayableUI {

	int append(ItemUI item);
	 
	void delete(int itemNum);
	 
	void deleteAll();
	 
	void insert(int itemNum, ItemUI item);

	void set(int itemNum, ItemUI item);

	void setItemStateListener(ItemStateListener itemStateListener);
	
	// TODO remove when Swing UI is completely rewritten
	ItemStateListener getItemStateListener();
	
}
