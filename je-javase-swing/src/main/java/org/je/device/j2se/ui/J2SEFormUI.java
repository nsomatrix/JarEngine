package org.je.device.j2se.ui;

import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.ItemStateListener;

import org.je.device.impl.ui.DisplayableImplUI;
import org.je.device.ui.FormUI;
import org.je.device.ui.ItemUI;

public class J2SEFormUI extends DisplayableImplUI implements FormUI {

	private ItemStateListener itemStateListener;
	
	public J2SEFormUI(Form form) {
		super(form);
	}

	public int append(ItemUI item) {
		// TODO not yet used
		return 0;
	}

	public void delete(int itemNum) {
		// TODO not yet used		
	}

	public void deleteAll() {
		// TODO not yet used		
	}

	public void insert(int itemNum, ItemUI item) {
		// TODO not yet used		
	}

	public void set(int itemNum, ItemUI item) {
		// TODO Auto-generated method stub		
	}

	public void setItemStateListener(ItemStateListener itemStateListener) {
		this.itemStateListener = itemStateListener;
	}
	
	public ItemStateListener getItemStateListener() {
		return itemStateListener;
	}

}
