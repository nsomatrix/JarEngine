package org.je.app.ui.swing;

import javax.swing.JTextField;

import org.je.app.ui.swing.SwingDialogPanel;

public class MIDletUrlPanel extends SwingDialogPanel {
	
    private static final long serialVersionUID = 1L;
    
    private JTextField jadUrlField = new JTextField(50);

	public MIDletUrlPanel() {		
		add(jadUrlField);
	}
	
	public String getText() {
		return jadUrlField.getText();
	}

	protected void showNotify() {
		jadUrlField.setText("");
	}
	
}
