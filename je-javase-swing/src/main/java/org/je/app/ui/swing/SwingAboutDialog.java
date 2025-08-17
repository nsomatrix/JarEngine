package org.je.app.ui.swing;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.UIManager;

import org.je.app.Main;
import org.je.app.util.BuildVersion;

/**
 * @author vlads
 * 
 */
public class SwingAboutDialog extends SwingDialogPanel {

	private static final long serialVersionUID = 1L;

	private JLabel iconLabel;

	private JLabel textLabel;

	public SwingAboutDialog() {

		setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();

		c.ipadx = 10;
		c.ipady = 10;
		c.gridx = 0;
		c.gridy = 0;
		iconLabel = new JLabel();
		add(iconLabel, c);

		// Try to load the icon, but don't fail if it's not found
		try {
			java.awt.Image iconImage = Toolkit.getDefaultToolkit().getImage(
					Main.class.getResource("/org/je/icon.png"));
			if (iconImage != null) {
				iconLabel.setIcon(new ImageIcon(iconImage));
			}
		} catch (Exception e) {
			// Icon not found, continue without it
			System.err.println("Warning: Could not load about dialog icon: " + e.getMessage());
		}

		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		textLabel = new JLabel("JarEngine");
		// Use relative font sizing based on system defaults
		Font baseFont = UIManager.getFont("Label.font");
		textLabel.setFont(baseFont.deriveFont(Font.BOLD, baseFont.getSize() * 1.5f));
		add(textLabel, c);

		c.gridy = 1;
		c.weightx = 0.0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.WEST;
		add(new JLabel("version: " + BuildVersion.getVersion()), c);

		c.gridy = 2;
		c.weightx = 0.0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		add(new JLabel("Copyright (C) 2001-2008 Bartek Teodorczyk & co"), c);

	}
}
