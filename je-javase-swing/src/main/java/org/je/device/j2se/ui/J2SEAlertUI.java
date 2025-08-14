package org.je.device.j2se.ui;

import javax.microedition.lcdui.Alert;

import org.je.device.impl.ui.DisplayableImplUI;
import org.je.device.ui.AlertUI;

public class J2SEAlertUI extends DisplayableImplUI implements AlertUI {

	public J2SEAlertUI(Alert alert) {
		super(alert);
	}

	public void setString(String str) {
		// TODO not yet used
	}

}
