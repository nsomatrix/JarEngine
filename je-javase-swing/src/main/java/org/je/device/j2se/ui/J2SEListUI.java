package org.je.device.j2se.ui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

import org.je.device.impl.ui.DisplayableImplUI;
import org.je.device.ui.ListUI;

public class J2SEListUI extends DisplayableImplUI implements ListUI {

	public J2SEListUI(List list) {
		super(list);
	}

	public int append(String stringPart, Image imagePart) {
		// TODO not yet used
		return -1;
	}

	public void setSelectCommand(Command command) {
		// TODO not yet used
	}

	public int getSelectedIndex() {
		// TODO not yet used
		return 0;
	}

	public void setSelectedIndex(int elementNum, boolean selected) {
		// TODO not yet used
	}

	public String getString(int elementNum) {
		// TODO not yet used
		return null;
	}

	public void delete(int elementNum) {
		// TODO Auto-generated method stub		
	}

	public void deleteAll() {
		// TODO Auto-generated method stub		
	}

	public void insert(int elementNum, String stringPart, Image imagePart) {
		// TODO not yet used
	}

	public void set(int elementNum, String stringPart, Image imagePart) {
		// TODO not yet used
	}

	public int size() {
		// TODO not yet used
		return 0;
	}

}
