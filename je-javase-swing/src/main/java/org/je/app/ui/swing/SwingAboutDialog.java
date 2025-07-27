/**
 *  MicroEmulator
 *  Copyright (C) 2006-2008 Bartek Teodorczyk <barteo@barteo.net>
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
 *
 *  It is licensed under the following two licenses as alternatives:
 *    1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 *    2. Apache License (the "AL") Version 2.0
 *
 *  You may not use this file except in compliance with at least one of
 *  the above two licenses.
 *
 *  You may obtain a copy of the LGPL at
 *      http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 *
 *  You may obtain a copy of the AL at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the LGPL or the AL for the specific language governing permissions and
 *  limitations.
 *
 *  @version $Id$
 */
package org.je.app.ui.swing;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

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
		textLabel.setFont(new Font("Default", Font.BOLD, 18));
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
