package org.je.device.j2se.ui;

import javax.microedition.lcdui.TextBox;

import org.je.device.impl.ui.DisplayableImplUI;
import org.je.device.ui.TextBoxUI;

public class J2SETextBoxUI extends DisplayableImplUI implements TextBoxUI {

	private String text;

	public J2SETextBoxUI(TextBox textBox) {
		super(textBox);
	}

	public int getCaretPosition() {
		// TODO not yet used
		return -1;
	}

	public String getString() {
		// TODO not yet used
		return text;
	}

	public void setString(String text) {
		// TODO not yet used
		this.text = text;
	}
	
	public void insert(String text, int caret) {
		// TODO not yet used
	}

	public void delete(int offset, int length) {
		// TODO not yet used
	} 

}
