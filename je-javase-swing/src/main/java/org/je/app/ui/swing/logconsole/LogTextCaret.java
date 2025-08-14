package org.je.app.ui.swing.logconsole;

import java.awt.Rectangle;

import javax.swing.text.DefaultCaret;

/**
 * @author Michael Lifshits
 *
 */
public class LogTextCaret extends DefaultCaret{

	private static final long serialVersionUID = 1L;
	
	private boolean visibilityAdjustmentEnabled = true;
	
    protected void adjustVisibility(Rectangle nloc) {
    	if (visibilityAdjustmentEnabled) {
    		super.adjustVisibility(nloc);
    	}
    }

	public void setVisibilityAdjustment(boolean flag) {
		visibilityAdjustmentEnabled = flag;
	}
    
}
