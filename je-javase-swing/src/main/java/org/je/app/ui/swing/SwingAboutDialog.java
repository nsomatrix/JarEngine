package org.je.app.ui.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.je.app.Main;
import org.je.app.util.BuildVersion;

/**
 * Enhanced About dialog with multiple tabs for different information
 * @author vlads
 * 
 */
public class SwingAboutDialog extends SwingDialogPanel {

	private static final long serialVersionUID = 1L;

	public SwingAboutDialog() {
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(500, 400));

		// Create tabbed pane
		JTabbedPane tabbedPane = new JTabbedPane();
		
		// About tab
		tabbedPane.addTab("About", createAboutPanel());
		
		// System Info tab
		tabbedPane.addTab("System Info", createSystemInfoPanel());
		
		// License tab
		tabbedPane.addTab("License", createLicensePanel());
		
		add(tabbedPane, BorderLayout.CENTER);
		
		// Buttons panel
		add(createButtonsPanel(), BorderLayout.SOUTH);
	}
	
	private JPanel createAboutPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		// Icon and title section
		c.insets = new Insets(20, 20, 10, 20);
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.CENTER;

		// Icon
		JLabel iconLabel = new JLabel();
		try {
			java.awt.Image iconImage = Toolkit.getDefaultToolkit().getImage(
					Main.class.getResource("/org/je/icon.png"));
			if (iconImage != null) {
				// Scale the icon to a reasonable size
				java.awt.Image scaledImage = iconImage.getScaledInstance(64, 64, java.awt.Image.SCALE_SMOOTH);
				iconLabel.setIcon(new ImageIcon(scaledImage));
			}
		} catch (Exception e) {
			System.err.println("Warning: Could not load about dialog icon: " + e.getMessage());
		}
		panel.add(iconLabel, c);
		
		// Title
		c.gridy = 1;
		c.insets = new Insets(10, 20, 5, 20);
		JLabel titleLabel = new JLabel("JarEngine™", SwingConstants.CENTER);
		Font baseFont = UIManager.getFont("Label.font");
		titleLabel.setFont(baseFont.deriveFont(Font.BOLD, baseFont.getSize() * 2.0f));
		panel.add(titleLabel, c);
		
		// Subtitle
		c.gridy = 2;
		c.insets = new Insets(0, 20, 20, 20);
		JLabel subtitleLabel = new JLabel("A Standalone Cross Platform J2ME Emulator", SwingConstants.CENTER);
		subtitleLabel.setFont(baseFont.deriveFont(Font.ITALIC, baseFont.getSize() * 1.1f));
		panel.add(subtitleLabel, c);
		
		// Version info
		c.gridy = 3;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(10, 40, 5, 40);
		JLabel versionLabel = new JLabel("Version: " + BuildVersion.getVersion());
		versionLabel.setFont(baseFont.deriveFont(Font.BOLD));
		panel.add(versionLabel, c);
		
		// Build info
		c.gridy = 4;
		c.insets = new Insets(5, 40, 5, 40);
		String javaVersion = System.getProperty("java.version", "Unknown");
		JLabel buildLabel = new JLabel("Built with Java " + javaVersion);
		panel.add(buildLabel, c);
		
		// Copyright
		c.gridy = 5;
		c.insets = new Insets(20, 40, 10, 40);
		JLabel copyrightLabel = new JLabel("(C) 2025 NSOMatrix™");
		panel.add(copyrightLabel, c);
		
		// Description
		c.gridy = 6;
		c.insets = new Insets(10, 40, 20, 40);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		JTextArea descriptionArea = new JTextArea(
			"JarEngine™ is a J2ME (Java 2 Platform, Micro Edition) emulator which is cross platform capable. " +
			"Seamlessly run Java apps on MacOSX / Windows / Linux Devices."
		);
		descriptionArea.setEditable(false);
		descriptionArea.setOpaque(false);
		descriptionArea.setWrapStyleWord(true);
		descriptionArea.setLineWrap(true);
		descriptionArea.setFont(baseFont);
		panel.add(descriptionArea, c);
		
		return panel;
	}
	
	private JPanel createSystemInfoPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		c.insets = new Insets(5, 10, 5, 10);
		c.anchor = GridBagConstraints.WEST;
		
		int row = 0;
		
		// Java Information
		addInfoRow(panel, c, row++, "Java Version:", System.getProperty("java.version", "Unknown"));
		addInfoRow(panel, c, row++, "Java Vendor:", System.getProperty("java.vendor", "Unknown"));
		addInfoRow(panel, c, row++, "Java Home:", System.getProperty("java.home", "Unknown"));
		
		// Add separator
		c.gridy = row++;
		c.gridx = 0;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(10, 10, 10, 10);
		panel.add(new javax.swing.JSeparator(), c);
		
		// Reset constraints
		c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(5, 10, 5, 10);
		
		// System Information
		addInfoRow(panel, c, row++, "Operating System:", 
			System.getProperty("os.name", "Unknown") + " " + 
			System.getProperty("os.version", "Unknown"));
		addInfoRow(panel, c, row++, "Architecture:", System.getProperty("os.arch", "Unknown"));
		addInfoRow(panel, c, row++, "User Name:", System.getProperty("user.name", "Unknown"));
		addInfoRow(panel, c, row++, "User Home:", System.getProperty("user.home", "Unknown"));
		
		// Add separator
		c.gridy = row++;
		c.gridx = 0;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(10, 10, 10, 10);
		panel.add(new javax.swing.JSeparator(), c);
		
		// Reset constraints
		c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(5, 10, 5, 10);
		
		// Memory Information
		Runtime runtime = Runtime.getRuntime();
		long totalMemory = runtime.totalMemory() / (1024 * 1024);
		long freeMemory = runtime.freeMemory() / (1024 * 1024);
		long maxMemory = runtime.maxMemory() / (1024 * 1024);
		long usedMemory = totalMemory - freeMemory;
		
		addInfoRow(panel, c, row++, "Total Memory:", totalMemory + " MB");
		addInfoRow(panel, c, row++, "Used Memory:", usedMemory + " MB");
		addInfoRow(panel, c, row++, "Free Memory:", freeMemory + " MB");
		addInfoRow(panel, c, row++, "Max Memory:", maxMemory + " MB");
		
		// Screen Information
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		addInfoRow(panel, c, row++, "Screen Resolution:", 
			(int)screenSize.getWidth() + " x " + (int)screenSize.getHeight());
		
		// Add filler to push content to top
		c.gridy = row;
		c.gridx = 0;
		c.gridwidth = 2;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.VERTICAL;
		panel.add(Box.createVerticalGlue(), c);
		
		return panel;
	}
	
	private void addInfoRow(JPanel panel, GridBagConstraints c, int row, String label, String value) {
		c.gridy = row;
		c.gridx = 0;
		c.weightx = 0.0;
		JLabel labelComponent = new JLabel(label);
		labelComponent.setFont(labelComponent.getFont().deriveFont(Font.BOLD));
		panel.add(labelComponent, c);
		
		c.gridx = 1;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		JLabel valueComponent = new JLabel(value);
		panel.add(valueComponent, c);
		
		// Reset fill for next iteration
		c.fill = GridBagConstraints.NONE;
	}
	
	private JPanel createLicensePanel() {
		JPanel panel = new JPanel(new BorderLayout());
		
		JTextArea licenseArea = new JTextArea();
		licenseArea.setEditable(false);
		licenseArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		
		// Try to load license from file, or provide default text
		String licenseText = getLicenseText();
		licenseArea.setText(licenseText);
		licenseArea.setCaretPosition(0);
		
		JScrollPane scrollPane = new JScrollPane(licenseArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		panel.add(scrollPane, BorderLayout.CENTER);
		
		return panel;
	}
	
	private String getLicenseText() {
		try {
			java.io.InputStream licenseStream = Main.class.getResourceAsStream("/LICENSE");
			if (licenseStream == null) {
				// Try alternative paths
				licenseStream = Main.class.getResourceAsStream("/org/je/LICENSE");
			}
			
			if (licenseStream != null) {
				java.io.BufferedReader reader = new java.io.BufferedReader(
					new java.io.InputStreamReader(licenseStream));
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					sb.append(line).append("\n");
				}
				reader.close();
				return sb.toString();
			}
		} catch (Exception e) {
			System.err.println("Warning: Could not load license file: " + e.getMessage());
		}
		
		// Default license text if file not found
		return "MIT License\n\n" +
			   "Copyright (c) 2025 NSOMatrix™\n\n" +
			   "Permission is hereby granted, free of charge, to any person obtaining a copy\n" +
			   "of this software and associated documentation files (the \"Software\"), to deal\n" +
			   "in the Software without restriction, including without limitation the rights\n" +
			   "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell\n" +
			   "copies of the Software, and to permit persons to whom the Software is\n" +
			   "furnished to do so, subject to the following conditions:\n\n" +
			   "The above copyright notice and this permission notice shall be included in all\n" +
			   "copies or substantial portions of the Software.\n\n" +
			   "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR\n" +
			   "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,\n" +
			   "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE\n" +
			   "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER\n" +
			   "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,\n" +
			   "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE\n" +
			   "SOFTWARE.";
	}
	
	private JPanel createButtonsPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		// Website button
		JButton websiteButton = new JButton("Project Website");
		websiteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				openWebsite("https://jarengine-emu.github.io/app/");
			}
		});
		
		// Documentation button
		JButton docsButton = new JButton("Documentation");
		docsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				openWebsite("https://jarengine-emu.github.io/app/docs");
			}
		});
		
		panel.add(Box.createHorizontalGlue());
		panel.add(websiteButton);
		panel.add(Box.createHorizontalStrut(10));
		panel.add(docsButton);
		panel.add(Box.createHorizontalGlue());
		
		return panel;
	}
	
	private void openWebsite(String url) {
		// Execute URL opening in background thread to avoid EDT blocking
		new Thread(() -> {
			try {
				// First try using Java Desktop API (works on Windows, macOS, Linux with GUI)
				if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
					Desktop.getDesktop().browse(new URI(url));
					return;
				}
				
				// Fallback: Try platform-specific commands
				String osName = System.getProperty("os.name", "").toLowerCase();
				String[] command = null;
				
				if (osName.contains("win")) {
					// Windows
					command = new String[]{"rundll32", "url.dll,FileProtocolHandler", url};
				} else if (osName.contains("mac")) {
					// macOS
					command = new String[]{"open", url};
				} else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
					// Linux/Unix - try common browsers with non-blocking check
					String[] browsers = {"xdg-open", "gnome-open", "kde-open", "firefox", "mozilla", "chromium", "google-chrome"};
					for (String browser : browsers) {
						try {
							// Check if browser exists (with timeout to prevent hanging)
							Process checkProcess = Runtime.getRuntime().exec(new String[]{"which", browser});
							boolean finished = checkProcess.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
							if (finished && checkProcess.exitValue() == 0) {
								command = new String[]{browser, url};
								break;
							}
						} catch (Exception ignore) {
							// Continue to next browser
						}
					}
				}
				
				if (command != null) {
					Runtime.getRuntime().exec(command);
				} else {
					// Show URL in dialog if all else fails (on EDT)
					javax.swing.SwingUtilities.invokeLater(() -> showUrlDialog(url));
				}
				
			} catch (Exception e) {
				System.err.println("Could not open website: " + e.getMessage());
				// Show URL in dialog as final fallback (on EDT)
				javax.swing.SwingUtilities.invokeLater(() -> showUrlDialog(url));
			}
		}, "BrowserOpener-" + System.currentTimeMillis()).start();
	}
	
	private void showUrlDialog(String url) {
		// Copy URL to clipboard and show dialog
		try {
			java.awt.datatransfer.StringSelection stringSelection = new java.awt.datatransfer.StringSelection(url);
			java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(stringSelection, null);
			
			javax.swing.JOptionPane.showMessageDialog(this,
				"Unable to open browser automatically.\n\n" +
				"The URL has been copied to your clipboard:\n" + url + "\n\n" +
				"Please paste it into your web browser.",
				"Open URL",
				javax.swing.JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception clipboardError) {
			// If clipboard also fails, just show the URL
			javax.swing.JOptionPane.showMessageDialog(this,
				"Unable to open browser automatically.\n\n" +
				"Please visit: " + url,
				"Open URL",
				javax.swing.JOptionPane.INFORMATION_MESSAGE);
		}
	}
}
