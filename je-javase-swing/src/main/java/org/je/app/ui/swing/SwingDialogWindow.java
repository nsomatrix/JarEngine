package org.je.app.ui.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

/**
 * Uniwersalna klasa sluzaca do wyswietlania okienek dialogowych
 */

public class SwingDialogWindow
{

  /**
   * Open modal dialog window
   *
   * @param title dialog title
   * @param panel content
   * @param hasCancel has Cancel button 
   * @return true if user pressed OK button
   */
  public static boolean show(Frame parent, String title, final SwingDialogPanel panel, boolean hasCancel)
  {
    final JDialog dialog = new JDialog(parent, title, true);
    dialog.getContentPane().setLayout(new BorderLayout());
    dialog.getContentPane().add(panel, BorderLayout.CENTER);

    JPanel actionPanel = new JPanel();
    actionPanel.add(panel.btOk);
    if (hasCancel) {
    	actionPanel.add(panel.btCancel);
    }
    final JButton extraButton = panel.getExtraButton();
    if (extraButton != null) {
    	actionPanel.add(extraButton);
    }
    dialog.getContentPane().add(actionPanel, BorderLayout.SOUTH);
    dialog.pack();
    dialog.setLocationRelativeTo(parent);

    ActionListener closeListener = new ActionListener() {
		public void actionPerformed(ActionEvent event) {
			Object source = event.getSource();
			panel.extra = false;
			if (source == panel.btOk || source == extraButton) {
				if (panel.check(true)) {
					if (source == extraButton) {
						panel.extra = true;
					}
					panel.state = true;
					dialog.setVisible(false);
					panel.hideNotify();
				}
			} else {
				panel.state = false;
				dialog.setVisible(false);
				panel.hideNotify();
			}
		}
	};
    
    WindowAdapter windowAdapter = new WindowAdapter()
    {
      public void windowClosing(WindowEvent e)
      {
        panel.state = false;
        panel.hideNotify();
      }
    };

    dialog.addWindowListener(windowAdapter);
    panel.btOk.addActionListener(closeListener);
    panel.btCancel.addActionListener(closeListener);
    if (extraButton != null) {
    	extraButton.addActionListener(closeListener);
    }
    panel.showNotify();
    dialog.setVisible(true);
    panel.btOk.removeActionListener(closeListener);
    panel.btCancel.removeActionListener(closeListener);
    if (extraButton != null) {
    	extraButton.removeActionListener(closeListener);
    }

    return panel.state;
  }

}

