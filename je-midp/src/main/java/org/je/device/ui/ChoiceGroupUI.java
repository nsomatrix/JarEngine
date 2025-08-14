package org.je.device.ui;

import javax.microedition.lcdui.Image;

public interface ChoiceGroupUI extends ItemUI {

	void delete(int elementNum);
	
	void deleteAll();

	void setSelectedIndex(int elementNum, boolean selected);

	int getSelectedIndex();

	void insert(int elementNum, String stringPart, Image imagePart);
	
	boolean isSelected(int elementNum);

	void setSelectedFlags(boolean[] selectedArray);

	int getSelectedFlags(boolean[] selectedArray);
	
	String getString(int elementNum);

	void set(int elementNum, String stringPart, Image imagePart);
	
	int size();

}
