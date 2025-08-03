/**
 *  MicroEmulator
 *  Copyright (C) 2001 Bartek Teodorczyk <barteo@barteo.net>
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
 */

package org.je.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;                                                            
import java.util.List;                                                                
import java.util.NoSuchElementException;                                              
import java.util.Timer;                                                               
import java.util.TimerTask; 

import javax.microedition.midlet.MIDletStateChangeException;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;

import org.je.DisplayAccess;
import org.je.DisplayComponent;
import org.je.MIDletAccess;
import org.je.MIDletBridge;
import org.je.MIDletContext;
import org.je.app.capture.AnimatedGifEncoder;
import org.je.app.classloader.MIDletClassLoader;
import org.je.app.ui.DisplayRepaintListener;
import org.je.app.ui.Message;
import org.je.app.ui.ResponseInterfaceListener;
import org.je.app.ui.StatusBarListener;
import org.je.app.ui.swing.DropTransferHandler;
import org.je.app.ui.swing.ExtensionFileFilter;
import org.je.app.ui.swing.JMRUMenu;
import org.je.app.ui.swing.MIDletUrlPanel;
import org.je.app.ui.swing.RecordStoreManagerDialog;
import org.je.app.ui.swing.ResizeDeviceDisplayDialog;
import org.je.app.ui.swing.SwingAboutDialog;
import org.je.app.ui.swing.SwingDeviceComponent;
import org.je.app.ui.swing.SwingDialogWindow;
import org.je.app.ui.swing.SwingDisplayComponent;
import org.je.app.ui.swing.SwingErrorMessageDialogPanel;
import org.je.app.ui.swing.SwingLogConsoleDialog;

import org.je.app.util.AppletProducer;
import org.je.app.util.DeviceEntry;
import org.je.app.util.IOUtils;
import org.je.app.util.MidletURLReference;
import org.je.device.Device;
import org.je.device.DeviceDisplay;
import org.je.device.DeviceFactory;
import org.je.device.Device;
import org.je.device.EmulatorContext;
import org.je.device.FontManager;
import org.je.device.InputMethod;
import org.je.device.impl.DeviceDisplayImpl;                                    
import org.je.device.impl.DeviceImpl;                                           
import org.je.device.impl.Rectangle; 
import org.je.device.impl.SoftButton;
import org.je.device.j2se.J2SEDevice;
import org.je.device.j2se.J2SEDeviceDisplay;
import org.je.device.j2se.J2SEFontManager;
import org.je.device.j2se.J2SEGraphicsSurface;
import org.je.device.j2se.J2SEInputMethod;
import org.je.log.Logger;
import org.je.log.QueueAppender;
import org.je.util.JadMidletEntry;


public class Main extends JFrame {

	private static final long serialVersionUID = 1L;

	protected Common common;



	private MIDletUrlPanel midletUrlPanel = null;

	private JFileChooser saveForWebChooser;

	private JFileChooser fileChooser = null;

	private JFileChooser captureFileChooser = null;

	private JMenuItem menuOpenMIDletFile;

	private JMenuItem menuOpenMIDletURL;



	private JMenuItem menuSaveForWeb;

	private JMenuItem menuStartCapture;

	private JMenuItem menuStopCapture;

	private JCheckBoxMenuItem menuMIDletNetworkConnection;

	private JCheckBoxMenuItem menuLogConsole;

	private JCheckBoxMenuItem menuRecordStoreManager;

	private JFrame scaledDisplayFrame;
	
	// Theme-related fields
	private JRadioButtonMenuItem menuLightTheme;
	private JRadioButtonMenuItem menuDarkTheme;
	private ButtonGroup themeButtonGroup;

	private JCheckBoxMenuItem[] zoomLevels;

	private SwingDeviceComponent devicePanel;

	private SwingLogConsoleDialog logConsoleDialog;

	private RecordStoreManagerDialog recordStoreManagerDialog;

	private QueueAppender logQueueAppender;

	private DeviceEntry deviceEntry;

	private AnimatedGifEncoder encoder;

	private JLabel statusBar = new JLabel("Status");

	private JButton resizeButton = new JButton("Resize");

	private ResizeDeviceDisplayDialog resizeDeviceDisplayDialog = null;

	protected EmulatorContext emulatorContext = new EmulatorContext() {

		private InputMethod inputMethod = new J2SEInputMethod();

		private DeviceDisplay deviceDisplay = new J2SEDeviceDisplay(this);

		private FontManager fontManager = new J2SEFontManager();

		public DisplayComponent getDisplayComponent() {
			return devicePanel.getDisplayComponent();
		}

		public InputMethod getDeviceInputMethod() {
			return inputMethod;
		}

		public DeviceDisplay getDeviceDisplay() {
			return deviceDisplay;
		}

		public FontManager getDeviceFontManager() {
			return fontManager;
		}

		public InputStream getResourceAsStream(Class origClass, String name) {
            return MIDletBridge.getCurrentMIDlet().getClass().getResourceAsStream(name);
		}
		
		public boolean platformRequest(final String URL) {
			new Thread(new Runnable() {
				public void run() {
					Message.info("MIDlet requests that the device handle the following URL: " + URL);
				}
			}).start();

			return false;
		}
	};

	private ActionListener menuOpenMIDletFileListener = new ActionListener() {
		public void actionPerformed(ActionEvent ev) {
			if (fileChooser == null) {
				ExtensionFileFilter fileFilter = new ExtensionFileFilter("MIDlet files");
				fileFilter.addExtension("jad");
				fileFilter.addExtension("jar");
				fileChooser = new JFileChooser();
				fileChooser.setFileFilter(fileFilter);
				fileChooser.setDialogTitle("Open MIDlet File...");
				fileChooser.setCurrentDirectory(new File(Config.getRecentDirectory("recentJadDirectory")));
			}

			int returnVal = fileChooser.showOpenDialog(Main.this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				Config.setRecentDirectory("recentJadDirectory", fileChooser.getCurrentDirectory().getAbsolutePath());
				String url = IOUtils.getCanonicalFileURL(fileChooser.getSelectedFile());
				Common.openMIDletUrlSafe(url);
				if (recordStoreManagerDialog != null) {
					recordStoreManagerDialog.refresh();
				}
			}
		}
	};

	private ActionListener menuOpenMIDletURLListener = new ActionListener() {
		public void actionPerformed(ActionEvent ev) {
			if (midletUrlPanel == null) {
				midletUrlPanel = new MIDletUrlPanel();
			}
			if (SwingDialogWindow.show(Main.this, "Enter MIDlet URL:", midletUrlPanel, true)) {
				Common.openMIDletUrlSafe(midletUrlPanel.getText());
				if (recordStoreManagerDialog != null) {
					recordStoreManagerDialog.refresh();
				}
			}
		}
	};

	private ActionListener menuCloseMidletListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			common.startLauncher(MIDletBridge.getMIDletContext());
		}
	};

	private ActionListener menuSaveForWebListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if (saveForWebChooser == null) {
				ExtensionFileFilter fileFilter = new ExtensionFileFilter("HTML files");
				fileFilter.addExtension("html");
				saveForWebChooser = new JFileChooser();
				saveForWebChooser.setFileFilter(fileFilter);
				saveForWebChooser.setDialogTitle("Save for Web...");
				saveForWebChooser.setCurrentDirectory(new File(Config.getRecentDirectory("recentSaveForWebDirectory")));
			}
			if (saveForWebChooser.showSaveDialog(Main.this) == JFileChooser.APPROVE_OPTION) {
				Config.setRecentDirectory("recentSaveForWebDirectory", saveForWebChooser.getCurrentDirectory()
						.getAbsolutePath());
				File pathFile = saveForWebChooser.getSelectedFile().getParentFile();

				String name = saveForWebChooser.getSelectedFile().getName();
				if (!name.toLowerCase().endsWith(".html") && name.indexOf('.') == -1) {
					name = name + ".html";
				}

				// try to get from distribution home location
				String resource = MIDletClassLoader.getClassResourceName(this.getClass().getName());
				URL url = this.getClass().getClassLoader().getResource(resource);
				String path = url.getPath();
				int prefix = path.indexOf(':');
				String mainJarFileName = path.substring(prefix + 1, path.length() - resource.length());
				File appletJarDir = new File(new File(mainJarFileName).getParent(), "lib");
				File appletJarFile = new File(appletJarDir, "je-javase-applet.jar");
				if (!appletJarFile.exists()) {
					appletJarFile = null;
				}

				if (appletJarFile == null) {
					// try to get from maven2 repository
					/*
					 * loc/org/je/je/2.0.1-SNAPSHOT/je-2.0.1-20070227.080140-1.jar String
					 * version = doRegExpr(mainJarFileName, ); String basePath = "loc/org/je/" appletJarFile = new
					 * File(basePath + "je-javase-applet/" + version + "/je-javase-applet" + version +
					 * ".jar"); if (!appletJarFile.exists()) { appletJarFile = null; }
					 */
				}

				if (appletJarFile == null) {
					ExtensionFileFilter fileFilter = new ExtensionFileFilter("JAR packages");
					fileFilter.addExtension("jar");
					JFileChooser appletChooser = new JFileChooser();
					appletChooser.setFileFilter(fileFilter);
					appletChooser.setDialogTitle("Select JE applet jar package...");
					appletChooser.setCurrentDirectory(new File(Config.getRecentDirectory("recentAppletJarDirectory")));
					if (appletChooser.showOpenDialog(Main.this) == JFileChooser.APPROVE_OPTION) {
						Config.setRecentDirectory("recentAppletJarDirectory", appletChooser.getCurrentDirectory()
								.getAbsolutePath());
						appletJarFile = appletChooser.getSelectedFile();
					} else {
						return;
					}
				}

				JadMidletEntry jadMidletEntry;
				Iterator it = common.jad.getMidletEntries().iterator();
				if (it.hasNext()) {
					jadMidletEntry = (JadMidletEntry) it.next();
				} else {
					Message.error("MIDlet Suite has no entries");
					return;
				}

				String midletInput = common.jad.getJarURL();
				DeviceEntry deviceInput = null;

				File htmlOutputFile = new File(pathFile, name);
				if (!allowOverride(htmlOutputFile)) {
					return;
				}
				File appletPackageOutputFile = new File(pathFile, "je-javase-applet.jar");
				if (!allowOverride(appletPackageOutputFile)) {
					return;
				}
				File midletOutputFile = new File(pathFile, midletInput.substring(midletInput.lastIndexOf("/") + 1));
				if (!allowOverride(midletOutputFile)) {
					return;
				}
				File deviceOutputFile = null;
				if (deviceInput != null && deviceInput.getFileName() != null) {
					deviceOutputFile = new File(pathFile, deviceInput.getFileName());
					if (!allowOverride(deviceOutputFile)) {
						return;
					}
				}

				try {
					AppletProducer.createHtml(htmlOutputFile, (DeviceImpl) DeviceFactory.getDevice(), jadMidletEntry
							.getClassName(), midletOutputFile, appletPackageOutputFile, deviceOutputFile);
					AppletProducer.createMidlet(new URL(midletInput), midletOutputFile);
					IOUtils.copyFile(appletJarFile, appletPackageOutputFile);
					if (deviceInput != null && deviceInput.getFileName() != null) {
						IOUtils.copyFile(new File(Config.getConfigPath(), deviceInput.getFileName()), deviceOutputFile);
					}
				} catch (IOException ex) {
					Logger.error(ex);
				}
			}
		}

		private boolean allowOverride(File file) {
			if (file.exists()) {
				int answer = JOptionPane.showConfirmDialog(Main.this, "Override the file:" + file + "?", "Question?",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if (answer == 1 /* no */) {
					return false;
				}
			}

			return true;
		}
	};

	private ActionListener menuStartCaptureListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if (captureFileChooser == null) {
				ExtensionFileFilter fileFilter = new ExtensionFileFilter("GIF files");
				fileFilter.addExtension("gif");
				captureFileChooser = new JFileChooser();
				captureFileChooser.setFileFilter(fileFilter);
				captureFileChooser.setDialogTitle("Capture to GIF File...");
				captureFileChooser.setCurrentDirectory(new File(Config.getRecentDirectory("recentCaptureDirectory")));
			}

			if (captureFileChooser.showSaveDialog(Main.this) == JFileChooser.APPROVE_OPTION) {
				Config.setRecentDirectory("recentCaptureDirectory", captureFileChooser.getCurrentDirectory()
						.getAbsolutePath());
				String name = captureFileChooser.getSelectedFile().getName();
				if (!name.toLowerCase().endsWith(".gif") && name.indexOf('.') == -1) {
					name = name + ".gif";
				}
				File captureFile = new File(captureFileChooser.getSelectedFile().getParentFile(), name);
				if (!allowOverride(captureFile)) {
					return;
				}

				encoder = new AnimatedGifEncoder();
				encoder.start(captureFile.getAbsolutePath());

				menuStartCapture.setEnabled(false);
				menuStopCapture.setEnabled(true);

				((SwingDisplayComponent) emulatorContext.getDisplayComponent())
						.addDisplayRepaintListener(new DisplayRepaintListener() {
					long start = 0;

					public void repaintInvoked(Object repaintObject) {
						synchronized (Main.this) {
							if (encoder != null) {
								if (start == 0) {
									start = System.currentTimeMillis();
								} else {
									long current = System.currentTimeMillis();
									encoder.setDelay((int) (current - start));
									start = current;
								}

								encoder.addFrame(((J2SEGraphicsSurface) repaintObject).getImage());
							}
						}
					}
				});
			}
		}

		private boolean allowOverride(File file) {
			if (file.exists()) {
				int answer = JOptionPane.showConfirmDialog(Main.this, "Override the file:" + file + "?", "Question?",
						JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
				if (answer == 1 /* no */) {
					return false;
				}
			}

			return true;
		}
	};

	private ActionListener menuStopCaptureListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			menuStopCapture.setEnabled(false);

			synchronized (Main.this) {
				encoder.finish();
				encoder = null;
			}

			menuStartCapture.setEnabled(true);
		}
	};

	private ActionListener menuMIDletNetworkConnectionListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			org.je.cldc.http.Connection.setAllowNetworkConnection(menuMIDletNetworkConnection.getState());
		}

	};

	private ActionListener menuRecordStoreManagerListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if (recordStoreManagerDialog == null) {
				recordStoreManagerDialog = new RecordStoreManagerDialog(Main.this, common);
				recordStoreManagerDialog.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
						menuRecordStoreManager.setState(false);
					}
				});
				recordStoreManagerDialog.pack();
				Rectangle window = Config.getWindow("recordStoreManager", new Rectangle(0, 0, 640, 320));
				recordStoreManagerDialog.setBounds(window.x, window.y, window.width, window.height);
			}
			recordStoreManagerDialog.setVisible(!recordStoreManagerDialog.isVisible());
		}
	};

	private ActionListener menuLogConsoleListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if (logConsoleDialog == null) {
				logConsoleDialog = new SwingLogConsoleDialog(Main.this, Main.this.logQueueAppender);
				logConsoleDialog.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
						menuLogConsole.setState(false);
					}
				});
				logConsoleDialog.pack();
				// To avoid NPE on MacOS setFocusableWindowState(false) have to be called after pack()
				logConsoleDialog.setFocusableWindowState(false);
				Rectangle window = Config.getWindow("logConsole", new Rectangle(0, 0, 640, 320));
				logConsoleDialog.setBounds(window.x, window.y, window.width, window.height);
			}
			logConsoleDialog.setVisible(!logConsoleDialog.isVisible());
		}
	};

	private ActionListener menuAboutListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			SwingDialogWindow.show(Main.this, "About", new SwingAboutDialog(), false);
		}
	};

	private ActionListener menuLightThemeListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			try {
				// Check if running on Windows
				String osName = System.getProperty("os.name").toLowerCase();
				if (osName.contains("windows")) {
					// On Windows, use FlatLaf with specific settings to avoid menu bar integration
					UIManager.setLookAndFeel(new FlatLightLaf());
					// Set specific FlatLaf properties to prevent menu bar integration on Windows
					UIManager.put("TitlePane.useWindowDecorations", false);
					UIManager.put("TitlePane.menuBarEmbedded", false);
				} else {
					// On other platforms, use FlatLaf
					UIManager.setLookAndFeel(new FlatLightLaf());
				}
				SwingUtilities.updateComponentTreeUI(Main.this);
				// Update child windows if they exist
				if (logConsoleDialog != null) {
					SwingUtilities.updateComponentTreeUI(logConsoleDialog);
				}
				if (recordStoreManagerDialog != null) {
					SwingUtilities.updateComponentTreeUI(recordStoreManagerDialog);
				}
				if (scaledDisplayFrame != null) {
					SwingUtilities.updateComponentTreeUI(scaledDisplayFrame);
				}
				if (resizeDeviceDisplayDialog != null) {
					SwingUtilities.updateComponentTreeUI(resizeDeviceDisplayDialog);
				}
				// Save theme preference
				Config.setCurrentTheme("light");
				common.setCurrentTheme("light");
				// Update device display colors
				if (DeviceFactory.getDevice() != null && DeviceFactory.getDevice().getDeviceDisplay() instanceof org.je.device.j2se.J2SEDeviceDisplay) {
					((org.je.device.j2se.J2SEDeviceDisplay) DeviceFactory.getDevice().getDeviceDisplay()).updateThemeColors("light");
				}
				// Restart launcher if we're in launcher mode (to update LauncherCanvas theme)
				if (MIDletBridge.getMIDletContext() != null && MIDletBridge.getMIDletContext().isLauncher()) {
					// Add small delay to prevent rapid switching
					try {
						Thread.sleep(100);
					} catch (InterruptedException ie) {
						// Ignore
					}
					common.startLauncher(MIDletBridge.getMIDletContext());
				}
			} catch (Exception ex) {
				Logger.error("Failed to set light theme", ex);
			}
		}
	};

	private ActionListener menuDarkThemeListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			try {
				// Check if running on Windows
				String osName = System.getProperty("os.name").toLowerCase();
				if (osName.contains("windows")) {
					// On Windows, use FlatLaf with specific settings to avoid menu bar integration
					UIManager.setLookAndFeel(new FlatDarkLaf());
					// Set specific FlatLaf properties to prevent menu bar integration on Windows
					UIManager.put("TitlePane.useWindowDecorations", false);
					UIManager.put("TitlePane.menuBarEmbedded", false);
				} else {
					// On other platforms, use FlatLaf
					UIManager.setLookAndFeel(new FlatDarkLaf());
				}
				SwingUtilities.updateComponentTreeUI(Main.this);
				// Update child windows if they exist
				if (logConsoleDialog != null) {
					SwingUtilities.updateComponentTreeUI(logConsoleDialog);
				}
				if (recordStoreManagerDialog != null) {
					SwingUtilities.updateComponentTreeUI(recordStoreManagerDialog);
				}
				if (scaledDisplayFrame != null) {
					SwingUtilities.updateComponentTreeUI(scaledDisplayFrame);
				}
				if (resizeDeviceDisplayDialog != null) {
					SwingUtilities.updateComponentTreeUI(resizeDeviceDisplayDialog);
				}
				// Save theme preference
				Config.setCurrentTheme("dark");
				common.setCurrentTheme("dark");
				// Update device display colors
				if (DeviceFactory.getDevice().getDeviceDisplay() instanceof org.je.device.j2se.J2SEDeviceDisplay) {
					((org.je.device.j2se.J2SEDeviceDisplay) DeviceFactory.getDevice().getDeviceDisplay()).updateThemeColors("dark");
				}
				// Restart launcher if we're in launcher mode (to update LauncherCanvas theme)
				if (MIDletBridge.getMIDletContext() != null && MIDletBridge.getMIDletContext().isLauncher()) {
					// Add small delay to prevent rapid switching
					try {
						Thread.sleep(100);
					} catch (InterruptedException ie) {
						// Ignore
					}
					common.startLauncher(MIDletBridge.getMIDletContext());
				}
			} catch (Exception ex) {
				Logger.error("Failed to set dark theme", ex);
			}
		}
	};

	private ActionListener menuExitListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			synchronized (Main.this) {
				if (encoder != null) {
					encoder.finish();
					encoder = null;
				}
			}

			// Clean up MIDlet context
			if (common != null) {
				MIDletContext context = MIDletBridge.getMIDletContext();
				if (context != null) {
					common.notifyDestroyed(context);
				}
			}

			if (logConsoleDialog != null) {
				Config.setWindow("logConsole", new Rectangle(logConsoleDialog.getX(), logConsoleDialog.getY(),
						logConsoleDialog.getWidth(), logConsoleDialog.getHeight()), logConsoleDialog.isVisible());
			}
			if (recordStoreManagerDialog != null) {
				Config.setWindow("recordStoreManager", new Rectangle(recordStoreManagerDialog.getX(),
						recordStoreManagerDialog.getY(), recordStoreManagerDialog.getWidth(), recordStoreManagerDialog
								.getHeight()), recordStoreManagerDialog.isVisible());
			}
			if (scaledDisplayFrame != null) {
				Config.setWindow("scaledDisplay", new Rectangle(scaledDisplayFrame.getX(), scaledDisplayFrame.getY(),
						0, 0), false);
			}
			Config.setWindow("main", new Rectangle(Main.this.getX(), Main.this.getY(), Main.this.getWidth(), Main.this
					.getHeight()), true);

			System.exit(0);
		}
	};



	private ActionListener menuScaledDisplayListener = new ActionListener() {
		private DisplayRepaintListener updateScaledImageListener;

		public void actionPerformed(ActionEvent e) {
			final JCheckBoxMenuItem selectedZoomLevelMenuItem = (JCheckBoxMenuItem) e.getSource();
			if (selectedZoomLevelMenuItem.isSelected()) {
				for (int i = 0; i < zoomLevels.length; ++i) {
					if (zoomLevels[i] != e.getSource()) {
						zoomLevels[i].setSelected(false);
					}
				}
				final int scale = Integer.parseInt(e.getActionCommand());
				if (scaledDisplayFrame != null) {
					((SwingDisplayComponent) emulatorContext.getDisplayComponent())
							.removeDisplayRepaintListener(updateScaledImageListener);
					scaledDisplayFrame.dispose();
				}
				scaledDisplayFrame = new JFrame(getTitle());
				scaledDisplayFrame.setContentPane(new JLabel(new ImageIcon()));
				updateScaledImageListener = new DisplayRepaintListener() {
					public void repaintInvoked(Object repaintObject) {
						updateScaledImage(scale, scaledDisplayFrame);
						scaledDisplayFrame.validate();
					}
				};
				scaledDisplayFrame.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent event) {
						selectedZoomLevelMenuItem.setSelected(false);
					}
				});
				scaledDisplayFrame.getContentPane().addMouseListener(new MouseListener() {
					private MouseListener receiver = ((SwingDisplayComponent) emulatorContext.getDisplayComponent())
							.getMouseListener();

					public void mouseClicked(MouseEvent e) {
						receiver.mouseClicked(createAdaptedMouseEvent(e, scale));
					}

					public void mousePressed(MouseEvent e) {
						receiver.mousePressed(createAdaptedMouseEvent(e, scale));
					}

					public void mouseReleased(MouseEvent e) {
						receiver.mouseReleased(createAdaptedMouseEvent(e, scale));
					}

					public void mouseEntered(MouseEvent e) {
						receiver.mouseEntered(createAdaptedMouseEvent(e, scale));
					}

					public void mouseExited(MouseEvent e) {
						receiver.mouseExited(createAdaptedMouseEvent(e, scale));
					}
				});
				scaledDisplayFrame.getContentPane().addMouseMotionListener(new MouseMotionListener() {
					private MouseMotionListener receiver = ((SwingDisplayComponent) emulatorContext
							.getDisplayComponent()).getMouseMotionListener();

					public void mouseDragged(MouseEvent e) {
						receiver.mouseDragged(createAdaptedMouseEvent(e, scale));
					}

					public void mouseMoved(MouseEvent e) {
						receiver.mouseMoved(createAdaptedMouseEvent(e, scale));
					}
				});
				scaledDisplayFrame.getContentPane().addMouseWheelListener(new MouseWheelListener() {
					private MouseWheelListener receiver = ((SwingDisplayComponent) emulatorContext
							.getDisplayComponent()).getMouseWheelListener();

					public void mouseWheelMoved(MouseWheelEvent e) {
						MouseWheelEvent adaptedEvent = createAdaptedMouseWheelEvent(e, scale);
						receiver.mouseWheelMoved(adaptedEvent);
					}
				});
				scaledDisplayFrame.addKeyListener(devicePanel);

				updateScaledImage(scale, scaledDisplayFrame);
				((SwingDisplayComponent) emulatorContext.getDisplayComponent())
						.addDisplayRepaintListener(updateScaledImageListener);
				scaledDisplayFrame.setIconImage(getIconImage());
				scaledDisplayFrame.setResizable(false);
				Point location = getLocation();
				Dimension size = getSize();
				Rectangle window = Config.getWindow("scaledDisplay", new Rectangle(location.x + size.width, location.y,
						0, 0));
				scaledDisplayFrame.setLocation(window.x, window.y);
				Config.setWindow("scaledDisplay", new Rectangle(scaledDisplayFrame.getX(), scaledDisplayFrame.getY(),
						0, 0), false);
				scaledDisplayFrame.pack();
				scaledDisplayFrame.setVisible(true);
			} else {
				((SwingDisplayComponent) emulatorContext.getDisplayComponent())
						.removeDisplayRepaintListener(updateScaledImageListener);
				scaledDisplayFrame.dispose();
			}
		}

		private MouseEvent createAdaptedMouseEvent(MouseEvent e, int scale) {
			return new MouseEvent(e.getComponent(), e.getID(), e.getWhen(), e.getModifiers(), e.getX() / scale, e
					.getY()
					/ scale, e.getClickCount(), e.isPopupTrigger(), e.getButton());
		}

		private MouseWheelEvent createAdaptedMouseWheelEvent(MouseWheelEvent e, int scale) {
			return new MouseWheelEvent(e.getComponent(), e.getID(), e.getWhen(), e.getModifiers(), e.getX() / scale, e
					.getY()
					/ scale, e.getClickCount(), e.isPopupTrigger(), e.getScrollType(), e.getScrollAmount(), e
					.getWheelRotation());
		}

		private void updateScaledImage(int scale, JFrame scaledLCDFrame) {
			J2SEGraphicsSurface graphicsSurface = 
					((SwingDisplayComponent) emulatorContext.getDisplayComponent()).getGraphicsSurface();
			
			BufferedImage img = graphicsSurface.getImage();
			BufferedImage scaledImg = new BufferedImage(img.getWidth() * scale, img.getHeight() * scale, img.getType());
			Graphics2D imgGraphics = scaledImg.createGraphics();
			imgGraphics.scale(scale, scale);
			imgGraphics.drawImage(img, 0, 0, null);
			
			((ImageIcon) (((JLabel) scaledLCDFrame.getContentPane()).getIcon())).setImage(scaledImg);
			((JLabel) scaledLCDFrame.getContentPane()).repaint();
		}
	};

	private StatusBarListener statusBarListener = new StatusBarListener() {
		public void statusBarChanged(String text) {
			FontMetrics metrics = statusBar.getFontMetrics(statusBar.getFont());
			statusBar.setPreferredSize(new Dimension(metrics.stringWidth(text), metrics.getHeight()));
			statusBar.setText(text);
		}
	};

	private ResponseInterfaceListener responseInterfaceListener = new ResponseInterfaceListener() {
		public void stateChanged(boolean state) {
			menuOpenMIDletFile.setEnabled(state);
			menuOpenMIDletURL.setEnabled(state);

			if (common.jad.getJarURL() != null) {
				menuSaveForWeb.setEnabled(state);
			} else {
				menuSaveForWeb.setEnabled(false);
			}
		}
	};

	private ComponentListener componentListener = new ComponentAdapter() {
		Timer timer;

		int count = 0;

		public void componentResized(ComponentEvent e) {
			count++;
			DeviceDisplayImpl deviceDisplay = (DeviceDisplayImpl) DeviceFactory.getDevice().getDeviceDisplay();
			if (deviceDisplay.isResizable()) {
				// Disable device resizing to allow proper scaling
				// setDeviceSize(deviceDisplay, devicePanel.getWidth(), devicePanel.getHeight());
				devicePanel.revalidate();
				statusBarListener.statusBarChanged("New size: " + deviceDisplay.getFullWidth() + "x"
						+ deviceDisplay.getFullHeight());
				synchronized (statusBarListener) {
					if (timer == null) {
						timer = new Timer();
					}
					timer.schedule(new CountTimerTask(count) {
						public void run() {
							if (counter == count) {
								Config.setDeviceEntryDisplaySize(deviceEntry, new Rectangle(0, 0, devicePanel
										.getWidth(), devicePanel.getHeight()));
								statusBarListener.statusBarChanged("");
								timer.cancel();
								timer = null;
							}
						}
					}, 2000);
				}
			}
		}
	};

	private WindowAdapter windowListener = new WindowAdapter() {
		public void windowClosing(WindowEvent ev) {
			menuExitListener.actionPerformed(null);
		}

		public void windowIconified(WindowEvent ev) {
			MIDletBridge.getMIDletAccess(MIDletBridge.getCurrentMIDlet()).pauseApp();
		}

		public void windowDeiconified(WindowEvent ev) {
			try {
				MIDletBridge.getMIDletAccess(MIDletBridge.getCurrentMIDlet()).startApp();
			} catch (MIDletStateChangeException ex) {
				System.err.println(ex);
			}
		}
	};

	public Main() {
		this(null);
	}

	public Main(DeviceEntry defaultDevice) {

		this.logQueueAppender = new QueueAppender(1024);
		Logger.addAppender(logQueueAppender);

		JMenuBar menuBar = new JMenuBar();

		JMenu menuFile = new JMenu("File");

		menuOpenMIDletFile = new JMenuItem("Open MIDlet File...");
		menuOpenMIDletFile.addActionListener(menuOpenMIDletFileListener);
		menuFile.add(menuOpenMIDletFile);

		menuOpenMIDletURL = new JMenuItem("Open MIDlet URL...");
		menuOpenMIDletURL.addActionListener(menuOpenMIDletURLListener);
		menuFile.add(menuOpenMIDletURL);

		JMenuItem menuItemTmp = new JMenuItem("Close MIDlet");
		menuItemTmp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK));
		menuItemTmp.addActionListener(menuCloseMidletListener);
		menuFile.add(menuItemTmp);

		menuFile.addSeparator();

		JMRUMenu urlsMRU = new JMRUMenu("Recent MIDlets...");
		urlsMRU.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if (event instanceof JMRUMenu.MRUActionEvent) {
					Common.openMIDletUrlSafe(((MidletURLReference) ((JMRUMenu.MRUActionEvent) event).getSourceMRU())
							.getUrl());
					if (recordStoreManagerDialog != null) {
						recordStoreManagerDialog.refresh();
					}
				}
			}
		});

		Config.getUrlsMRU().setListener(urlsMRU);
		menuFile.add(urlsMRU);

		menuFile.addSeparator();

		menuSaveForWeb = new JMenuItem("Save for Web...");
		menuSaveForWeb.addActionListener(menuSaveForWebListener);
		menuFile.add(menuSaveForWeb);

		menuFile.addSeparator();

		JMenuItem menuItem = new JMenuItem("Exit");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
		menuItem.addActionListener(menuExitListener);
		menuFile.add(menuItem);

		JMenu menuOptions = new JMenu("Options");



		JMenu menuScaleLCD = new JMenu("Scaled display");
		menuOptions.add(menuScaleLCD);
		zoomLevels = new JCheckBoxMenuItem[3];
		for (int i = 0; i < zoomLevels.length; ++i) {
			zoomLevels[i] = new JCheckBoxMenuItem("x " + (i + 2));
			zoomLevels[i].setActionCommand("" + (i + 2));
			zoomLevels[i].addActionListener(menuScaledDisplayListener);
			menuScaleLCD.add(zoomLevels[i]);
		}

		menuStartCapture = new JMenuItem("Start capture to GIF...");
		menuStartCapture.addActionListener(menuStartCaptureListener);
		menuOptions.add(menuStartCapture);

		menuStopCapture = new JMenuItem("Stop capture");
		menuStopCapture.setEnabled(false);
		menuStopCapture.addActionListener(menuStopCaptureListener);
		menuOptions.add(menuStopCapture);

		menuMIDletNetworkConnection = new JCheckBoxMenuItem("MIDlet Network access");
		menuMIDletNetworkConnection.setState(true);
		menuMIDletNetworkConnection.addActionListener(menuMIDletNetworkConnectionListener);
		menuOptions.add(menuMIDletNetworkConnection);

		menuRecordStoreManager = new JCheckBoxMenuItem("Record Store Manager");
		menuRecordStoreManager.setState(false);
		menuRecordStoreManager.addActionListener(menuRecordStoreManagerListener);
		menuOptions.add(menuRecordStoreManager);

		menuLogConsole = new JCheckBoxMenuItem("Log console");
		menuLogConsole.setState(false);
		menuLogConsole.addActionListener(menuLogConsoleListener);
		menuOptions.add(menuLogConsole);

		// Theme selection menu
		JMenu menuTheme = new JMenu("Theme");
		themeButtonGroup = new ButtonGroup();
		
		menuLightTheme = new JRadioButtonMenuItem("Light");
		menuLightTheme.addActionListener(menuLightThemeListener);
		themeButtonGroup.add(menuLightTheme);
		menuTheme.add(menuLightTheme);
		
		menuDarkTheme = new JRadioButtonMenuItem("Dark");
		menuDarkTheme.addActionListener(menuDarkThemeListener);
		themeButtonGroup.add(menuDarkTheme);
		menuTheme.add(menuDarkTheme);
		
		// Set the current theme selection based on saved preference
		String currentTheme = Config.getCurrentTheme();
		if ("dark".equals(currentTheme)) {
			menuDarkTheme.setSelected(true);
		} else {
			menuLightTheme.setSelected(true);
		}
		
		menuOptions.add(menuTheme);

		menuOptions.addSeparator();
		JCheckBoxMenuItem menuShowMouseCoordinates = new JCheckBoxMenuItem("Mouse coordinates");
		menuShowMouseCoordinates.setState(false);
		menuShowMouseCoordinates.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				devicePanel.switchShowMouseCoordinates();
			}
		});
		menuOptions.add(menuShowMouseCoordinates);

		JMenu menuHelp = new JMenu("Help");
		JMenuItem menuAbout = new JMenuItem("About");
		menuAbout.addActionListener(menuAboutListener);
		menuHelp.add(menuAbout);

		menuBar.add(menuFile);
		menuBar.add(menuOptions);
		menuBar.add(menuHelp);
		setJMenuBar(menuBar);

		setTitle("JarEngine");

		// Try to load the icon, but don't fail if it's not found
		try {
			java.awt.Image iconImage = Toolkit.getDefaultToolkit().getImage(Main.class.getResource("/org/je/icon.png"));
			if (iconImage != null) {
				this.setIconImage(iconImage);
			}
		} catch (Exception e) {
			// Icon not found, continue without it
			System.err.println("Warning: Could not load application icon: " + e.getMessage());
		}

		addWindowListener(windowListener);

		Config.loadConfig(null, emulatorContext);
		Logger.setLocationEnabled(Config.isLogConsoleLocationEnabled());

		Rectangle window = Config.getWindow("main", new Rectangle(0, 0, 160, 120));
		this.setLocation(window.x, window.y);

		getContentPane().add(createContents(getContentPane()), "Center");



		this.common = new Common(emulatorContext);
		this.common.setStatusBarListener(statusBarListener);
		this.common.setResponseInterfaceListener(responseInterfaceListener);
		this.common.loadImplementationsFromConfig();

		this.resizeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				if (resizeDeviceDisplayDialog == null) {
					resizeDeviceDisplayDialog = new ResizeDeviceDisplayDialog();
				}
				DeviceDisplayImpl deviceDisplay = (DeviceDisplayImpl) DeviceFactory.getDevice().getDeviceDisplay();
				resizeDeviceDisplayDialog.setDeviceDisplaySize(deviceDisplay.getFullWidth(), deviceDisplay
						.getFullHeight());
				if (SwingDialogWindow.show(Main.this, "Enter new size...", resizeDeviceDisplayDialog, true)) {
				    setDeviceSize(deviceDisplay, resizeDeviceDisplayDialog.getDeviceDisplayWidth(), resizeDeviceDisplayDialog.getDeviceDisplayHeight());
					pack();
					devicePanel.requestFocus();
				}
			}
		});

		JPanel statusPanel = new JPanel();
		statusPanel.setLayout(new BorderLayout());
		statusPanel.add(statusBar, "West");
		statusPanel.add(this.resizeButton, "East");

		getContentPane().add(statusPanel, "South");

		Message.addListener(new SwingErrorMessageDialogPanel(this));

		devicePanel.setTransferHandler(new DropTransferHandler());
	}

	protected Component createContents(Container parent) {
		devicePanel = new SwingDeviceComponent();
		devicePanel.addKeyListener(devicePanel);
		addKeyListener(devicePanel);

		return devicePanel;
	}

	public boolean setDevice(DeviceEntry entry) {
		if (DeviceFactory.getDevice() != null) {
			// ((J2SEDevice) DeviceFactory.getDevice()).dispose();
		}
		final String errorTitle = "Error creating device";
		try {
			ClassLoader classLoader = getClass().getClassLoader();
			if (entry.getFileName() != null) {
				URL[] urls = new URL[1];
				urls[0] = new File(Config.getConfigPath(), entry.getFileName()).toURI().toURL();
				classLoader = Common.createExtensionsClassLoader(urls);
			}

			// TODO font manager have to be moved from emulatorContext into
			// device
			emulatorContext.getDeviceFontManager().init();

			Device device = DeviceImpl.create(emulatorContext, classLoader, entry.getDescriptorLocation(),
					J2SEDevice.class);
			this.deviceEntry = entry;

			DeviceDisplayImpl deviceDisplay = (DeviceDisplayImpl) device.getDeviceDisplay();
			if (deviceDisplay.isResizable()) {
				Rectangle size = Config.getDeviceEntryDisplaySize(entry);
				if (size != null) {
				    setDeviceSize(deviceDisplay, size.width, size.height);
				}
			}
			common.setDevice(device);
			updateDevice();
			return true;
		} catch (MalformedURLException e) {
			Message.error(errorTitle, errorTitle + ", " + Message.getCauseMessage(e), e);
		} catch (IOException e) {
			Message.error(errorTitle, errorTitle + ", " + Message.getCauseMessage(e), e);
		} catch (Throwable e) {
			Message.error(errorTitle, errorTitle + ", " + Message.getCauseMessage(e), e);
		}
		return false;
	}
	
	protected void setDeviceSize(DeviceDisplayImpl deviceDisplay, int width, int height) {
	    // move the soft buttons
	    int menuh = 0;
	    Enumeration en = DeviceFactory.getDevice().getSoftButtons().elements();
        while (en.hasMoreElements()) {
            SoftButton button = (SoftButton) en.nextElement();
            Rectangle paintable = button.getPaintable();
            paintable.y = height - paintable.height;
            menuh = paintable.height;
        }
        // resize the display area
        deviceDisplay.setDisplayPaintable(new Rectangle(0, 0, width, height - menuh));
        deviceDisplay.setDisplayRectangle(new Rectangle(0, 0, width, height));
        ((SwingDisplayComponent) devicePanel.getDisplayComponent()).init();
        // update display
        MIDletAccess ma = MIDletBridge.getMIDletAccess();
        if (ma == null) {
            return;
        }
        DisplayAccess da = ma.getDisplayAccess();
        if (da != null) {
            da.sizeChanged();
            deviceDisplay.repaint(0, 0, deviceDisplay.getFullWidth(), deviceDisplay.getFullHeight());
        }
	}

	protected void updateDevice() {
		devicePanel.init();
		if (((DeviceDisplayImpl) DeviceFactory.getDevice().getDeviceDisplay()).isResizable()) {
			setResizable(true);
			resizeButton.setVisible(true);
		} else {
			setResizable(false);
			resizeButton.setVisible(false);
		}

		pack();

		devicePanel.requestFocus();
		
		// Force a repaint after device update
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				repaint();
			}
		});
	}

	public static void main(String args[]) {
		List params = new ArrayList();
		StringBuffer debugArgs = new StringBuffer();
		for (int i = 0; i < args.length; i++) {
			params.add(args[i]);
			if (debugArgs.length() != 0) {
				debugArgs.append(", ");
			}
			debugArgs.append("[").append(args[i]).append("]");
		}
		if (params.contains("--headless")) {
			Headless.main(args);
			return;
		}

		try {
			// Check if running on Windows
			String osName = System.getProperty("os.name").toLowerCase();
			if (osName.contains("windows")) {
				// On Windows, use FlatLaf but with specific settings to avoid menu bar integration
				String savedTheme = Config.getCurrentTheme();
				if ("dark".equals(savedTheme)) {
					UIManager.setLookAndFeel(new FlatDarkLaf());
				} else {
					UIManager.setLookAndFeel(new FlatLightLaf());
				}
				// Set specific FlatLaf properties to prevent menu bar integration on Windows
				UIManager.put("TitlePane.useWindowDecorations", false);
				UIManager.put("TitlePane.menuBarEmbedded", false);
			} else {
				// On other platforms, use FlatLaf themes normally
				String savedTheme = Config.getCurrentTheme();
				if ("dark".equals(savedTheme)) {
					UIManager.setLookAndFeel(new FlatDarkLaf());
				} else {
					UIManager.setLookAndFeel(new FlatLightLaf());
				}
			}
		} catch (Exception ex) {
			Logger.error(ex);
			// Fallback to system look and feel if FlatLaf fails
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception ex2) {
				Logger.error(ex2);
			}
		}

		final Main app = new Main();
		if (args.length > 0) {
			Logger.debug("arguments", debugArgs.toString());
		}
		
		// Remove shutdown hook as it's causing issues with repeated notifyDestroyed calls
		
		app.common.initParams(params, null, J2SEDevice.class);
		app.updateDevice();

		// Ensure device is properly initialized before showing the window
		Device device = DeviceFactory.getDevice();
		if (device != null && device.getDeviceDisplay() != null) {
			// Force device initialization
			device.getDeviceDisplay().getFullWidth();
			device.getDeviceDisplay().getFullHeight();
		}

		// Ensure device is properly initialized
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				app.validate();
				app.setVisible(true);
				
				// Force multiple repaints to ensure graphics surface is created
				app.repaint();
				
				// Schedule additional repaints to ensure initialization
				javax.swing.Timer timer = new javax.swing.Timer(100, new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						app.repaint();
					}
				});
				timer.setRepeats(false);
				timer.start();
			}
		});

		if (Config.isWindowOnStart("logConsole")) {
			app.menuLogConsoleListener.actionPerformed(null);
			app.menuLogConsole.setSelected(true);
		}
		if (Config.isWindowOnStart("recordStoreManager")) {
			app.menuRecordStoreManagerListener.actionPerformed(null);
			app.menuRecordStoreManager.setSelected(true);
		}

		String midletString;
		try {
			midletString = (String) params.iterator().next();
		} catch (NoSuchElementException ex) {
			midletString = null;
		}
		app.common.initMIDlet(true);

		app.addComponentListener(app.componentListener);

		app.responseInterfaceListener.stateChanged(true);
	}

	private abstract class CountTimerTask extends TimerTask {

		protected int counter;

		public CountTimerTask(int counter) {
			this.counter = counter;
		}

	}

}
