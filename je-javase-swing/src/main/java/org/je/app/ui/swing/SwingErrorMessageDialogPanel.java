package org.je.app.ui.swing;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import org.je.app.ui.Message;
import org.je.app.ui.MessageListener;

/**
 * @author vlads
 *
 */
public class SwingErrorMessageDialogPanel extends SwingDialogPanel implements MessageListener {

	private static final long serialVersionUID = 1L;

	private Frame parent;
	
	private JLabel iconLabel;
	
	private JLabel textLabel;
	
	private JTextArea stackTraceArea;
	
	private JScrollPane stackTracePane;

	/**
	 * @param parent
	 */
	public SwingErrorMessageDialogPanel(Frame parent) {
		this.parent = parent;
		
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		c.ipadx = 10;
		c.ipady = 10;
		c.gridx = 0;
		c.gridy = 0;
		iconLabel = new JLabel();
		add(iconLabel, c);
		
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 1;
		textLabel = new JLabel();
		add(textLabel, c);
		
		stackTraceArea = new JTextArea();
		stackTraceArea.setEditable(false);
		stackTracePane = new JScrollPane(stackTraceArea);
		// Use natural sizing with reasonable constraints instead of hardcoded size
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int maxWidth = Math.min(600, screenSize.width / 2);
		int maxHeight = Math.min(400, screenSize.height / 2);
		stackTracePane.setMaximumSize(new Dimension(maxWidth, maxHeight));
		// Set minimum rows/columns to provide a reasonable starting size
		stackTraceArea.setRows(15);
		stackTraceArea.setColumns(50);
		
	}
	
	/* (non-Javadoc)
	 * @see org.je.app.ui.MessageListener#showMessage(int, java.lang.String, java.lang.String, java.lang.Throwable)
	 */
	public void showMessage(int level, String title, String text, Throwable throwable) {
		switch (level) {
		case Message.ERROR:
			iconLabel.setIcon(UIManager.getIcon("OptionPane.errorIcon"));
			break;
		case Message.WARN:
			iconLabel.setIcon(UIManager.getIcon("OptionPane.warningIcon"));
			break;
		default:
			iconLabel.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
		}

		textLabel.setText(text);
		
		if (throwable != null) {
			StringWriter writer = new StringWriter();
			throwable.printStackTrace(new PrintWriter(writer));
			stackTraceArea.setText(writer.toString());
			stackTraceArea.setCaretPosition(0);
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.gridx = 0;
			c.gridy = 1;
			c.gridwidth = 2;
			c.weightx = 1;
			c.weighty = 1;
			add(stackTracePane, c);
		}
		
		SwingDialogWindow.show(parent, title, this, false);
		
		if (throwable != null) {
			remove(stackTracePane);
		}
	}

}
