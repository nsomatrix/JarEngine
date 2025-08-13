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
import java.awt.event.InputEvent;
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
import javax.swing.Box;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;

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
import org.je.app.ui.swing.UpdateDialog;
import org.je.app.ui.swing.StatusDialog;
import org.je.app.ui.swing.SwingAboutDialog;
import org.je.app.ui.swing.SwingDeviceComponent;
import org.je.app.util.SleepManager;
import org.je.app.util.SelfDestructManager;
import org.je.app.ui.swing.SwingDialogWindow;
import org.je.app.ui.swing.SwingDisplayComponent;
import org.je.app.ui.swing.SwingErrorMessageDialogPanel;
import org.je.app.ui.swing.SwingLogConsoleDialog;


import org.je.app.util.DeviceEntry;
import org.je.app.util.IOUtils;
import org.je.app.util.MidletURLReference;
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
import org.je.app.tools.ReplicateInstancesTool;
import org.je.performance.PerformanceManager;


public class Main extends JFrame {
	// Preserve original launch args to allow restart with same params
	private static String[] launchArgs = new String[0];

	private static final long serialVersionUID = 1L;

	protected Common common;



    private org.je.app.tools.FPSTool fpsToolDialog = null;
    private MIDletUrlPanel midletUrlPanel = null;



	private JFileChooser fileChooser = null;



	private JMenuItem menuOpenMIDletFile;

	private JMenuItem menuOpenMIDletURL;





	private JRadioButtonMenuItem menuStartRecord;
	private JRadioButtonMenuItem menuStopRecord;
	private ButtonGroup recordButtonGroup;

	private JCheckBoxMenuItem menuMIDletNetworkConnection;

	private JCheckBoxMenuItem menuLogConsole;

	private JCheckBoxMenuItem menuRecordStoreManager;

	private JMenuItem menuResize;

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

    private org.je.app.ui.swing.StatusBar statusBar;

    private ResizeDeviceDisplayDialog resizeDeviceDisplayDialog = null;
    private SleepManager sleepManager;
    private SelfDestructManager selfDestructManager;
    // Prevent auto-resize handler from overriding explicit menu-based resize
    private volatile boolean suppressAutoResize = false;

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
			// Update resize menu state after closing MIDlet
			updateResizeMenuState();
		}
	};



	private ActionListener menuStartRecordListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			// Get Pictures folder path
			File picturesFolder = getPicturesFolder();
			if (!picturesFolder.exists()) {
				picturesFolder.mkdirs();
			}
			
			// Generate auto filename with timestamp
			String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
			String filename = "jar_engine_recording_" + timestamp + ".gif";
			File captureFile = new File(picturesFolder, filename);
			
			// Handle file override automatically
			int counter = 1;
			while (captureFile.exists()) {
				filename = "jar_engine_recording_" + timestamp + "_" + counter + ".gif";
				captureFile = new File(picturesFolder, filename);
				counter++;
			}

			encoder = new AnimatedGifEncoder();
			encoder.start(captureFile.getAbsolutePath());

			menuStartRecord.setSelected(true);
			menuStopRecord.setSelected(false);

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
			
			Message.info("Recording started: " + captureFile.getName());
		}
	};

	private ActionListener menuStopRecordListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			synchronized (Main.this) {
				if (encoder != null) {
					encoder.finish();
					encoder = null;
					Message.info("Recording stopped and saved");
				}
			}

			menuStartRecord.setSelected(false);
			menuStopRecord.setSelected(true);
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
					UIManager.setLookAndFeel(new FlatMacLightLaf());
					// Set specific FlatLaf properties to prevent menu bar integration on Windows
					UIManager.put("TitlePane.useWindowDecorations", false);
					UIManager.put("TitlePane.menuBarEmbedded", false);
				} else {
					// On other platforms, use FlatLaf
					UIManager.setLookAndFeel(new FlatMacLightLaf());
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
					UIManager.setLookAndFeel(new FlatMacDarkLaf());
					// Set specific FlatLaf properties to prevent menu bar integration on Windows
					UIManager.put("TitlePane.useWindowDecorations", false);
					UIManager.put("TitlePane.menuBarEmbedded", false);
				} else {
					// On other platforms, use FlatLaf
					UIManager.setLookAndFeel(new FlatMacDarkLaf());
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
				if (DeviceFactory.getDevice() != null && DeviceFactory.getDevice().getDeviceDisplay() instanceof org.je.device.j2se.J2SEDeviceDisplay) {
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



	private ActionListener menuStatusListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			StatusDialog statusDialog = new StatusDialog(Main.this);
			SwingDialogWindow.show(Main.this, "Status", statusDialog, false);
		}
	};

	private ActionListener menuUpdateEmulatorListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			UpdateDialog updateDialog = new UpdateDialog(Main.this);
			updateDialog.setVisible(true);
		}
	};

	private ActionListener menuReplicateInstancesListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			ReplicateInstancesTool replicateTool = new ReplicateInstancesTool(Main.this);
			replicateTool.setVisible(true);
		}
	};

    private StatusBarListener statusBarListener = text -> {
        if (statusBar != null) {
            statusBar.statusBarChanged(text);
        }
    };

	private ResponseInterfaceListener responseInterfaceListener = new ResponseInterfaceListener() {
		public void stateChanged(boolean state) {
			menuOpenMIDletFile.setEnabled(state);
			menuOpenMIDletURL.setEnabled(state);
		}
	};

    private ComponentListener componentListener = new ComponentAdapter() {
        Timer timer;
        final Object resizeTimerLock = new Object();

		int count = 0;

        public void componentResized(ComponentEvent e) {
            if (suppressAutoResize) {
                return;
            }
			count++;
			// Check if we're in launcher mode (Launcher MIDlet) or user MIDlet mode
			javax.microedition.midlet.MIDlet currentMIDlet = MIDletBridge.getCurrentMIDlet();
			boolean isLauncherMode = (currentMIDlet == null || currentMIDlet instanceof org.je.app.launcher.Launcher);
			
            // Show live dimensions in status bar (temporary for 2 seconds)
            Dimension size = devicePanel.getSize();
            if (statusBar != null) {
                statusBar.showTemporaryStatus(size.width + "x" + size.height, 2000);
            }
			
            if (isLauncherMode) {
                // In launcher mode: resize device to match content area for pre-MIDlet workflows
                int width = getContentPane().getWidth();
                int height = getContentPane().getHeight() - statusBar.getHeight();
                if (width > 0 && height > 0) {
                    DeviceDisplayImpl deviceDisplay = (DeviceDisplayImpl) DeviceFactory.getDevice().getDeviceDisplay();
                    if (deviceDisplay.isResizable()) {
                        setDeviceSize(deviceDisplay, width, height);
                    }
                }
                devicePanel.revalidate();
                devicePanel.repaint();
            } else {
                // User MIDlet is running: only stretch/fill behavior
                Device deviceSafe = DeviceFactory.getDevice();
                DeviceDisplayImpl deviceDisplay = deviceSafe != null ? (DeviceDisplayImpl) deviceSafe.getDeviceDisplay() : null;
                if (deviceDisplay != null && deviceDisplay.isResizable()) {
                    devicePanel.revalidate();
                    synchronized (resizeTimerLock) {
                        if (timer == null) {
                            timer = new Timer();
                        }
                        timer.schedule(new CountTimerTask(count) {
                            public void run() {
                                if (counter == count) {
                                    Config.setDeviceEntryDisplaySize(deviceEntry, new Rectangle(0, 0, devicePanel
                                            .getWidth(), devicePanel.getHeight()));
                                    synchronized (resizeTimerLock) {
                                        timer.cancel();
                                        timer = null;
                                    }
                                }
                            }
                        }, 2000);
                    }
                }
			}
		}
	};

	private WindowAdapter windowListener = new WindowAdapter() {
		public void windowClosing(WindowEvent ev) {
			// Cleanup sleep manager
			if (sleepManager != null) {
				sleepManager.cleanup();
			}
            // Cleanup self-destruct manager
            if (selfDestructManager != null) {
                selfDestructManager.cleanup();
            }
			menuExitListener.actionPerformed(null);
		}

		public void windowIconified(WindowEvent ev) {
			try {
				javax.microedition.midlet.MIDlet m = MIDletBridge.getCurrentMIDlet();
				MIDletAccess access = m != null ? MIDletBridge.getMIDletAccess(m) : null;
				if (access != null) access.pauseApp();
			} catch (Throwable ignored) {}
		}

		public void windowDeiconified(WindowEvent ev) {
			try {
				javax.microedition.midlet.MIDlet m = MIDletBridge.getCurrentMIDlet();
				MIDletAccess access = m != null ? MIDletBridge.getMIDletAccess(m) : null;
				if (access != null) access.startApp();
			} catch (Throwable ex) {
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
        // Allow shrinking the window freely; do not let the menu bar enforce a minimum width
        menuBar.setMinimumSize(new Dimension(0, 0));

		JMenu menuFile = new JMenu("Load");
		try {
			ImageIcon loadIcon = new ImageIcon(Main.class.getResource("/org/je/load.png"));
			menuFile.setIcon(loadIcon);
		} catch (Exception e) {
			System.err.println("Warning: Could not load Load menu icon: " + e.getMessage());
		}

		menuOpenMIDletFile = new JMenuItem("Load JAR");
		menuOpenMIDletFile.addActionListener(menuOpenMIDletFileListener);
		menuFile.add(menuOpenMIDletFile);

		menuOpenMIDletURL = new JMenuItem("Fetch via URL");
		menuOpenMIDletURL.addActionListener(menuOpenMIDletURLListener);
		menuFile.add(menuOpenMIDletURL);

		JMenuItem menuItemTmp = new JMenuItem("Terminate");
	menuItemTmp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK));
		menuItemTmp.addActionListener(menuCloseMidletListener);
		menuFile.add(menuItemTmp);

		JMRUMenu urlsMRU = new JMRUMenu("Recents Library");
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

	JMenuItem menuItem = new JMenuItem("Exit");
	menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
		menuItem.addActionListener(menuExitListener);
		menuFile.add(menuItem);

		JMenu menuOptions = new JMenu("Tune");
		try {
			ImageIcon tuneIcon = new ImageIcon(Main.class.getResource("/org/je/tune.png"));
			menuOptions.setIcon(tuneIcon);
		} catch (Exception e) {
			System.err.println("Warning: Could not load Tune menu icon: " + e.getMessage());
		}



		JMenu menuScaleLCD = new JMenu("Scaled display");
		menuOptions.add(menuScaleLCD);
		zoomLevels = new JCheckBoxMenuItem[3];
		for (int i = 0; i < zoomLevels.length; ++i) {
			zoomLevels[i] = new JCheckBoxMenuItem("x " + (i + 2));
			zoomLevels[i].setActionCommand("" + (i + 2));
			zoomLevels[i].addActionListener(menuScaledDisplayListener);
			menuScaleLCD.add(zoomLevels[i]);
		}



		menuMIDletNetworkConnection = new JCheckBoxMenuItem("MIDlet Network access");
		menuMIDletNetworkConnection.setState(true);
		menuMIDletNetworkConnection.addActionListener(menuMIDletNetworkConnectionListener);
		menuOptions.add(menuMIDletNetworkConnection);

		menuRecordStoreManager = new JCheckBoxMenuItem("Record Store Manager");
		menuRecordStoreManager.setState(false);
		menuRecordStoreManager.addActionListener(menuRecordStoreManagerListener);

		menuLogConsole = new JCheckBoxMenuItem("Log console");
		menuLogConsole.setState(false);
		menuLogConsole.addActionListener(menuLogConsoleListener);

		menuOptions.addSeparator();

		menuResize = new JMenuItem("Resize Device");
		menuResize.setEnabled(true); // Initially enabled, will be disabled when MIDlet is running
		menuResize.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				if (resizeDeviceDisplayDialog == null) {
					resizeDeviceDisplayDialog = new ResizeDeviceDisplayDialog();
				}
				DeviceDisplayImpl deviceDisplay = (DeviceDisplayImpl) DeviceFactory.getDevice().getDeviceDisplay();
				resizeDeviceDisplayDialog.setDeviceDisplaySize(deviceDisplay.getFullWidth(), deviceDisplay
						.getFullHeight());
                if (SwingDialogWindow.show(Main.this, "Enter new size...", resizeDeviceDisplayDialog, true)) {
                    int newW = resizeDeviceDisplayDialog.getDeviceDisplayWidth();
                    int newH = resizeDeviceDisplayDialog.getDeviceDisplayHeight();
                    try {
                        suppressAutoResize = true;
                        setDeviceSize(deviceDisplay, newW, newH);
                        // Explicitly size the frame so content area matches desired device size
                        java.awt.Insets insets = getInsets();
                        int statusH = statusBar.getHeight();
                        if (statusH <= 0) {
                            statusH = statusBar.getPreferredSize() != null ? statusBar.getPreferredSize().height : 0;
                        }
                        JMenuBar mb = getJMenuBar();
                        int menuH = (mb != null && mb.getPreferredSize() != null) ? mb.getPreferredSize().height : 0;
                        int frameW = newW + insets.left + insets.right;
                        int frameH = newH + statusH + menuH + insets.top + insets.bottom;
                        setSize(frameW, frameH);
                        validate();
                    } finally {
                        suppressAutoResize = false;
                    }
                    devicePanel.requestFocus();
                }
			}
		});
		menuOptions.add(menuResize);

		// Sleep mode menu item
		JCheckBoxMenuItem menuSleep = new JCheckBoxMenuItem("Sleep Mode");
		menuSleep.setToolTipText("Enable sleep mode - emulator will sleep after 10 seconds of inactivity");
		menuSleep.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				if (sleepManager == null) {
					sleepManager = new SleepManager(Main.this);
				}
				sleepManager.setSleepEnabled(menuSleep.isSelected());
			}
		});
		menuOptions.add(menuSleep);

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

		JMenuItem menuReplicateInstances = new JMenuItem("Replicate Instances");
		menuReplicateInstances.addActionListener(menuReplicateInstancesListener);
		menuOptions.add(menuReplicateInstances);

		// Self-Destruct submenu
		JMenu menuSelfDestructSubmenu = new JMenu("Self-Destruct");
		ButtonGroup selfDestructButtonGroup = new ButtonGroup();

		JRadioButtonMenuItem menuSelfDestructActivate = new JRadioButtonMenuItem("Activate");
		menuSelfDestructActivate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (selfDestructManager == null) {
					selfDestructManager = new SelfDestructManager(Main.this);
				}
				selfDestructManager.showConfigDialog();
			}
		});
		selfDestructButtonGroup.add(menuSelfDestructActivate);
		menuSelfDestructSubmenu.add(menuSelfDestructActivate);

		JRadioButtonMenuItem menuSelfDestructDeactivate = new JRadioButtonMenuItem("Deactivate");
		menuSelfDestructDeactivate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (selfDestructManager == null) {
					selfDestructManager = new SelfDestructManager(Main.this);
				}
				selfDestructManager.deactivateSelfDestruct();
			}
		});
		selfDestructButtonGroup.add(menuSelfDestructDeactivate);
		menuSelfDestructSubmenu.add(menuSelfDestructDeactivate);

		menuOptions.add(menuSelfDestructSubmenu);

		JMenu menuTools = new JMenu("Tools");
		try {
			ImageIcon toolsIcon = new ImageIcon(Main.class.getResource("/org/je/tools.png"));
			menuTools.setIcon(toolsIcon);
		} catch (Exception e) {
			System.err.println("Warning: Could not load Tools menu icon: " + e.getMessage());
		}
		// Add tools menu items here as needed

// Screenshot menu item
JMenuItem menuScreenshot = new JMenuItem("Screenshot");
menuScreenshot.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
menuScreenshot.addActionListener(e -> {
    String path = ScreenshotUtil.captureAndSaveScreenshot(devicePanel, "jar_engine_screenshot_");
    if (path != null) {
        JOptionPane.showMessageDialog(this, "Screenshot saved to: " + path, "Screenshot", JOptionPane.INFORMATION_MESSAGE);
    } else {
        JOptionPane.showMessageDialog(this, "Failed to save screenshot", "Screenshot Error", JOptionPane.ERROR_MESSAGE);
    }
});
menuTools.add(menuScreenshot);



JMenuItem menuFPS = new JMenuItem("FPS");
menuFPS.addActionListener(e -> {
    if (fpsToolDialog == null || !fpsToolDialog.isDisplayable()) {
        fpsToolDialog = new org.je.app.tools.FPSTool(this);
        fpsToolDialog.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent e) {
                fpsToolDialog = null;
            }
        });
    }
    if (!fpsToolDialog.isVisible()) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            fpsToolDialog.pack();
            fpsToolDialog.revalidate();
            fpsToolDialog.repaint();
            fpsToolDialog.setVisible(true);
        });
    } else {
        fpsToolDialog.toFront();
        fpsToolDialog.requestFocus();
    }
});
menuTools.add(menuFPS);

// Performance submenu with functional toggles
JMenu menuPerformance = new JMenu("Performance");

// (Removed legacy "Heap Size" emulated cap menu – JVM Heap (Restart) retained)

// JVM Heap Size – relaunch app with selected -Xmx
JMenuItem menuJvmHeap = new JMenuItem("JVM Heap Size");
menuJvmHeap.addActionListener(e -> {
	long currentJvmMb;
	try {
		long maxBytes = Runtime.getRuntime().maxMemory();
		currentJvmMb = maxBytes > 0 && maxBytes < Long.MAX_VALUE ? Math.max(1, maxBytes / (1024 * 1024)) : 1024;
	} catch (Throwable ignored) { currentJvmMb = 1024; }

	java.util.List<String> labels = new java.util.ArrayList<>();
	int[] steps = new int[]{256,384,512,640,768,896,1024,1280,1536,1792,2048,2304,2560,2816,3072,3328,3584,3840,4096};
	for (int mb : steps) labels.add(Integer.toString(mb));
	labels.add("Custom");

	String currentStr = Long.toString(currentJvmMb);
	Object selection = javax.swing.JOptionPane.showInputDialog(
			this,
			"Set JVM heap (Xmx) in MB. App will restart:",
			"JVM Heap Size",
			javax.swing.JOptionPane.PLAIN_MESSAGE,
			null,
			labels.toArray(),
			labels.contains(currentStr) ? currentStr : "1024");
	if (selection == null) return; // canceled
	String chosen = selection.toString();
	long chosenMb = currentJvmMb;
	if ("Custom".equals(chosen)) {
		String input = javax.swing.JOptionPane.showInputDialog(this, "Enter JVM heap in MB (min 256, max 4096):", currentJvmMb);
		if (input == null) return; // canceled
		try { chosenMb = Long.parseLong(input.trim()); } catch (NumberFormatException ex) {
			javax.swing.JOptionPane.showMessageDialog(this, "Invalid number", "JVM Heap", javax.swing.JOptionPane.ERROR_MESSAGE);
			return;
		}
	} else {
		try { chosenMb = Long.parseLong(chosen); } catch (NumberFormatException ignored) {}
	}
	if (chosenMb < 256) {
		javax.swing.JOptionPane.showMessageDialog(this, "Minimum JVM heap is 256 MB", "JVM Heap", javax.swing.JOptionPane.WARNING_MESSAGE);
		return;
	}
	if (chosenMb > 4096) {
		javax.swing.JOptionPane.showMessageDialog(this, "Maximum JVM heap is 4096 MB", "JVM Heap", javax.swing.JOptionPane.WARNING_MESSAGE);
		chosenMb = 4096;
	}

	javax.swing.JCheckBox syncEmu = new javax.swing.JCheckBox("Also set Emulated (MIDP) Heap to this size", true);
	javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout());
	panel.add(new javax.swing.JLabel("Apply -Xmx" + chosenMb + "m and restart now?"), java.awt.BorderLayout.NORTH);
	panel.add(syncEmu, java.awt.BorderLayout.CENTER);
	int res = javax.swing.JOptionPane.showConfirmDialog(this, panel, "Restart Required", javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE);
	if (res != javax.swing.JOptionPane.OK_OPTION) return;

	if (syncEmu.isSelected()) {
		long emuTargetMb = Math.min(chosenMb, 4096);
		try {
			org.je.performance.PerformanceManager.setEmulatedHeapLimitBytes(emuTargetMb * 1024L * 1024L);
			if (statusBar != null) statusBar.showTemporaryStatus("Emulated heap set: " + emuTargetMb + " MB", 2500);
		} catch (Throwable ignored) {}
	}

	restartWithJvmHeap(chosenMb);
});
menuPerformance.add(menuJvmHeap);
// (separator removed; toggles follow immediately)

// Generic toggle factory (no 'Enable' prefix; checked state implies enabled)
java.util.function.Function<String, JCheckBoxMenuItem> addToggle = (label) -> {
	JCheckBoxMenuItem item = new JCheckBoxMenuItem(label);
	menuPerformance.add(item);
	return item;
};

JCheckBoxMenuItem tHardware = addToggle.apply("Hardware Acceleration");
JCheckBoxMenuItem tAA = addToggle.apply("Anti Aliasing");
JCheckBoxMenuItem tDB = addToggle.apply("Double Buffering");
JCheckBoxMenuItem tPS = addToggle.apply("Power Saving Mode");
JCheckBoxMenuItem tIdle = addToggle.apply("Idle Skipping");
JCheckBoxMenuItem tFS = addToggle.apply("Frame Skipping");
JCheckBoxMenuItem tTPB = addToggle.apply("Thread Priority Boost");
JCheckBoxMenuItem tInput = addToggle.apply("Input Throttling");
JCheckBoxMenuItem tSprite = addToggle.apply("Sprite Caching");
JCheckBoxMenuItem tTex = addToggle.apply("Texture Filtering");
JCheckBoxMenuItem tVSync = addToggle.apply("VSync");

// Initialize states from PerformanceManager
tHardware.setSelected(PerformanceManager.isHardwareAcceleration());
tAA.setSelected(PerformanceManager.isAntiAliasing());
tDB.setSelected(PerformanceManager.isDoubleBuffering());
tPS.setSelected(PerformanceManager.isPowerSavingMode());
tIdle.setSelected(PerformanceManager.isIdleSkipping());
tFS.setSelected(PerformanceManager.isFrameSkipping());
tTPB.setSelected(PerformanceManager.isThreadPriorityBoost());
tInput.setSelected(PerformanceManager.isInputThrottling());
tSprite.setSelected(PerformanceManager.isSpriteCaching());
tTex.setSelected(PerformanceManager.isTextureFiltering());
tVSync.setSelected(PerformanceManager.isVSync());

// Wire listeners
tHardware.addActionListener(ev -> PerformanceManager.setHardwareAcceleration(tHardware.isSelected()));
tAA.addActionListener(ev -> PerformanceManager.setAntiAliasingPersist(tAA.isSelected()));
tDB.addActionListener(ev -> PerformanceManager.setDoubleBufferingPersist(tDB.isSelected()));
tPS.addActionListener(ev -> PerformanceManager.setPowerSavingMode(tPS.isSelected(), org.je.device.ui.EventDispatcher.maxFps));
tIdle.addActionListener(ev -> PerformanceManager.setIdleSkippingPersist(tIdle.isSelected()));
tFS.addActionListener(ev -> PerformanceManager.setFrameSkippingPersist(tFS.isSelected()));
tTPB.addActionListener(ev -> PerformanceManager.setThreadPriorityBoost(tTPB.isSelected(), findEventThread()));
tInput.addActionListener(ev -> PerformanceManager.setInputThrottlingPersist(tInput.isSelected()));
tSprite.addActionListener(ev -> PerformanceManager.setSpriteCachingPersist(tSprite.isSelected()));
tTex.addActionListener(ev -> PerformanceManager.setTextureFilteringPersist(tTex.isSelected()));
tVSync.addActionListener(ev -> PerformanceManager.setVSync(tVSync.isSelected(), org.je.device.ui.EventDispatcher.maxFps));

// Add Reset Performance Settings at the bottom so toggle references are in scope
menuPerformance.addSeparator();
JMenuItem menuPerfReset = new JMenuItem("Reset Performance Settings");
menuPerfReset.addActionListener(e -> {
	int res = JOptionPane.showConfirmDialog(this,
			"Reset all performance toggles and emulated heap to defaults?",
			"Reset Performance Settings", JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE);
	if (res != JOptionPane.OK_OPTION) return;
	try {
		PerformanceManager.resetToDefaults();
		// Reflect in menu checkboxes
		tHardware.setSelected(PerformanceManager.isHardwareAcceleration());
		tAA.setSelected(PerformanceManager.isAntiAliasing());
		tDB.setSelected(PerformanceManager.isDoubleBuffering());
		tPS.setSelected(PerformanceManager.isPowerSavingMode());
		tIdle.setSelected(PerformanceManager.isIdleSkipping());
		tFS.setSelected(PerformanceManager.isFrameSkipping());
		tTPB.setSelected(PerformanceManager.isThreadPriorityBoost());
		tInput.setSelected(PerformanceManager.isInputThrottling());
		tSprite.setSelected(PerformanceManager.isSpriteCaching());
		tTex.setSelected(PerformanceManager.isTextureFiltering());
		tVSync.setSelected(PerformanceManager.isVSync());
		if (statusBar != null) statusBar.showTemporaryStatus("Performance settings reset", 2500);
		JOptionPane.showMessageDialog(this, "Performance settings were reset.", "Reset",
				JOptionPane.INFORMATION_MESSAGE);
	} catch (Throwable ex) {
		Logger.error("Failed to reset performance settings", ex);
		JOptionPane.showMessageDialog(this, "Failed to reset settings: " + ex.getMessage(),
				"Reset Error", JOptionPane.ERROR_MESSAGE);
	}
});
menuPerformance.add(menuPerfReset);

menuTools.add(menuPerformance);

// Networking submenu
JMenu menuNetworking = new JMenu("Networking");
JMenuItem menuProxy = new JMenuItem("Proxy Settings");
menuProxy.addActionListener(e -> new org.je.app.tools.ProxyTool(this).setVisible(true));
menuNetworking.add(menuProxy);
menuNetworking.addSeparator();

// Individual tools as radio items that open their own small dialogs
JRadioButtonMenuItem netMonitor = new JRadioButtonMenuItem("Network Monitor");
netMonitor.addActionListener(e -> org.je.app.tools.NetworkTools.openMonitorDialog(this).setVisible(true));
menuNetworking.add(netMonitor);

JRadioButtonMenuItem netTraffic = new JRadioButtonMenuItem("Traffic Shaping");
netTraffic.addActionListener(e -> org.je.app.tools.NetworkTools.openTrafficDialog(this).setVisible(true));
menuNetworking.add(netTraffic);

JRadioButtonMenuItem netTester = new JRadioButtonMenuItem("Connection Tester");
netTester.addActionListener(e -> org.je.app.tools.NetworkTools.openTesterDialog(this).setVisible(true));
menuNetworking.add(netTester);

JRadioButtonMenuItem netDns = new JRadioButtonMenuItem("DNS Overrides");
netDns.addActionListener(e -> org.je.app.tools.NetworkTools.openDnsDialog(this).setVisible(true));
menuNetworking.add(netDns);

JRadioButtonMenuItem netMock = new JRadioButtonMenuItem("Mock Server");
netMock.addActionListener(e -> org.je.app.tools.NetworkTools.openMockDialog(this).setVisible(true));
menuNetworking.add(netMock);

 JRadioButtonMenuItem netTls = new JRadioButtonMenuItem("TLS Settings" + (org.je.util.net.NetConfig.TLS.trustAll ? " (trust-all ON)" : " (trust-all OFF)"));
netTls.addActionListener(e -> org.je.app.tools.NetworkTools.openTlsDialog(this).setVisible(true));
menuNetworking.add(netTls);

JRadioButtonMenuItem netCapture = new JRadioButtonMenuItem("Packet Capture");
netCapture.addActionListener(e -> org.je.app.tools.NetworkTools.openCaptureDialog(this).setVisible(true));
menuNetworking.add(netCapture);

 JRadioButtonMenuItem netOffline = new JRadioButtonMenuItem("Offline / Captive Portal" + (org.je.util.net.NetConfig.Policy.offline ? " (offline ON)" : (org.je.util.net.NetConfig.Policy.captivePortal ? " (captive ON)" : "")));
netOffline.addActionListener(e -> org.je.app.tools.NetworkTools.openOfflineDialog(this).setVisible(true));
menuNetworking.add(netOffline);

JRadioButtonMenuItem netMetrics = new JRadioButtonMenuItem("Network Metrics");
netMetrics.addActionListener(e -> org.je.app.tools.NetworkTools.openMetricsDialog(this).setVisible(true));
menuNetworking.add(netMetrics);

JRadioButtonMenuItem netUdp = new JRadioButtonMenuItem("UDP Tester");
netUdp.addActionListener(e -> org.je.app.tools.NetworkTools.openUdpDialog(this).setVisible(true));
menuNetworking.add(netUdp);

menuTools.add(menuNetworking);

// Record submenu
JMenu menuRecord = new JMenu("Record");
recordButtonGroup = new ButtonGroup();

menuStartRecord = new JRadioButtonMenuItem("Start Capture");
menuStartRecord.addActionListener(menuStartRecordListener);
recordButtonGroup.add(menuStartRecord);
menuRecord.add(menuStartRecord);

menuStopRecord = new JRadioButtonMenuItem("Stop Capture");
menuStopRecord.addActionListener(menuStopRecordListener);
recordButtonGroup.add(menuStopRecord);
menuRecord.add(menuStopRecord);

// Initially set Stop Capture as selected (not recording)
menuStopRecord.setSelected(true);

menuTools.add(menuRecord);

menuTools.add(menuRecordStoreManager);
menuTools.add(menuLogConsole);

		JMenu menuHelp = new JMenu("Settings");
		try {
			ImageIcon settingsIcon = new ImageIcon(Main.class.getResource("/org/je/settings.png"));
			menuHelp.setIcon(settingsIcon);
		} catch (Exception e) {
			System.err.println("Warning: Could not load Settings menu icon: " + e.getMessage());
		}
		
		JMenuItem menuSysInfo = new JMenuItem("Status");
		menuSysInfo.addActionListener(menuStatusListener);
		menuHelp.add(menuSysInfo);
		
		JMenuItem menuUpdateEmulator = new JMenuItem("Update Emulator");
		menuUpdateEmulator.addActionListener(menuUpdateEmulatorListener);
		menuHelp.add(menuUpdateEmulator);
		
		menuHelp.addSeparator();
		
        JMenuItem menuAbout = new JMenuItem("About");
		menuAbout.addActionListener(menuAboutListener);
		menuHelp.add(menuAbout);

		// Center the menu items on the menu bar
		menuBar.add(Box.createHorizontalGlue()); // Left glue
		menuBar.add(menuFile);
        menuBar.add(menuOptions);
		menuBar.add(menuTools);
		menuBar.add(menuHelp);
		menuBar.add(Box.createHorizontalGlue()); // Right glue
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



        statusBar = new org.je.app.ui.swing.StatusBar();
        getContentPane().add(statusBar.getComponent(), "South");
        statusBar.setVisible(true);

        // Ensure the frame itself doesn't keep an artificial minimum size
        setMinimumSize(new Dimension(0, 0));

		Message.addListener(new SwingErrorMessageDialogPanel(this));

		devicePanel.setTransferHandler(new DropTransferHandler());
		
		// Start timer to monitor MIDlet state
		midletStateTimer.start();
	}

    protected Component createContents(Container parent) {
        devicePanel = new SwingDeviceComponent();
        devicePanel.addKeyListener(devicePanel);
        addKeyListener(devicePanel);

        // Add sleep timer reset listeners
        addSleepTimerResetListeners();

        // Return the device panel directly so it stretches to fill the center
        return devicePanel;
    }
	
	/**
	 * Add listeners to reset sleep timer on user interaction
	 */
	private void addSleepTimerResetListeners() {
		// Add mouse listener to reset sleep timer (only on click, not movement)
		addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				resetSleepTimer();
			}
			
			@Override
			public void mousePressed(java.awt.event.MouseEvent e) {
				resetSleepTimer();
			}
		});
		
		// Add key listener to reset sleep timer
		addKeyListener(new java.awt.event.KeyAdapter() {
			@Override
			public void keyPressed(java.awt.event.KeyEvent e) {
				resetSleepTimer();
			}
		});
		
		// Add focus listener to reset sleep timer when window gains focus
		addWindowFocusListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowGainedFocus(java.awt.event.WindowEvent e) {
				resetSleepTimer();
			}
		});
		
		// Add listeners to device panel (where most user interaction happens)
		if (devicePanel != null) {
			devicePanel.addMouseListener(new java.awt.event.MouseAdapter() {
				@Override
				public void mouseClicked(java.awt.event.MouseEvent e) {
					resetSleepTimer();
				}
				
				@Override
				public void mousePressed(java.awt.event.MouseEvent e) {
					resetSleepTimer();
				}
			});
			
			devicePanel.addKeyListener(new java.awt.event.KeyAdapter() {
				@Override
				public void keyPressed(java.awt.event.KeyEvent e) {
					resetSleepTimer();
				}
			});
			
			// Also add listeners to the display component inside device panel
			try {
				org.je.DisplayComponent displayComponent = devicePanel.getDisplayComponent();
				if (displayComponent instanceof java.awt.Component) {
					java.awt.Component awtComponent = (java.awt.Component) displayComponent;
					awtComponent.addMouseListener(new java.awt.event.MouseAdapter() {
						@Override
						public void mouseClicked(java.awt.event.MouseEvent e) {
							resetSleepTimer();
						}
						
						@Override
						public void mousePressed(java.awt.event.MouseEvent e) {
							resetSleepTimer();
						}
					});
					
					awtComponent.addKeyListener(new java.awt.event.KeyAdapter() {
						@Override
						public void keyPressed(java.awt.event.KeyEvent e) {
							resetSleepTimer();
						}
					});
				}
			} catch (Exception e) {
				// Ignore if display component is not available yet
			}
		}
	}
	
	/**
	 * Reset the sleep timer if sleep manager exists
	 */
	private void resetSleepTimer() {
		if (sleepManager != null) {
			sleepManager.resetSleepTimer();
		}
	}
	
	/**
	 * Refresh sleep timer listeners for the current device
	 */
	private void refreshSleepTimerListeners() {
		if (devicePanel != null && sleepManager != null) {
			try {
				org.je.DisplayComponent displayComponent = devicePanel.getDisplayComponent();
				if (displayComponent instanceof java.awt.Component) {
					java.awt.Component awtComponent = (java.awt.Component) displayComponent;
					
					// Remove existing listeners first
					for (java.awt.event.MouseListener ml : awtComponent.getMouseListeners()) {
						if (ml.getClass().getName().contains("MouseAdapter")) {
							awtComponent.removeMouseListener(ml);
						}
					}
					
					// Add new sleep timer reset listeners
					awtComponent.addMouseListener(new java.awt.event.MouseAdapter() {
						@Override
						public void mouseClicked(java.awt.event.MouseEvent e) {
							resetSleepTimer();
						}
						
						@Override
						public void mousePressed(java.awt.event.MouseEvent e) {
							resetSleepTimer();
						}
					});
					
					awtComponent.addKeyListener(new java.awt.event.KeyAdapter() {
						@Override
						public void keyPressed(java.awt.event.KeyEvent e) {
							resetSleepTimer();
						}
					});
				}
			} catch (Exception e) {
				// Ignore if display component is not available
			}
		}
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
			
			// Refresh sleep timer listeners for the new device
			refreshSleepTimerListeners();
			
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
        // Reset the graphics surface so it is recreated at the new size
        ((SwingDisplayComponent) devicePanel.getDisplayComponent()).resetGraphicsSurface();
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
		} else {
			setResizable(false);
		}

		pack();

		devicePanel.requestFocus();
		
		// Update resize menu state based on MIDlet status
		updateResizeMenuState();
		
		// Force a repaint after device update
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				repaint();
			}
		});
	}
	
	/**
	 * Updates the resize menu item state based on current MIDlet status.
	 * Resize is enabled when Launcher is running or no MIDlet is running.
	 * Resize is disabled when a user MIDlet is running.
	 */
	private void updateResizeMenuState() {
		if (menuResize != null) {
			Object currentMIDlet = MIDletBridge.getCurrentMIDlet();
			boolean midletRunning = false;
			
			if (currentMIDlet != null) {
				// Allow resizing when Launcher is running, disable for other MIDlets
				String midletClassName = currentMIDlet.getClass().getName();
				midletRunning = !midletClassName.contains("Launcher");
			}
			
			menuResize.setEnabled(!midletRunning);
		}
	}
	
	// Timer to periodically check MIDlet state and update resize menu availability
	private javax.swing.Timer midletStateTimer = new javax.swing.Timer(500, new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			updateResizeMenuState();
		}
	});

	// Relaunch the current JVM with -Xmx set to mb, preserving classpath and original args
	private void restartWithJvmHeap(long mb) {
		try {
			String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
			String classpath = System.getProperty("java.class.path", "");
			java.util.List<String> cmd = new java.util.ArrayList<>();
			cmd.add(javaBin);
			// Preserve important system properties
			cmd.add("-Djava.awt.headless=false");
			// Apply requested Xmx
			cmd.add("-Xmx" + mb + "m");
			// Pass through je.emulatorID if set
			String emuId = System.getProperty("je.emulatorID");
			if (emuId != null && !emuId.isEmpty()) cmd.add("-Dje.emulatorID=" + emuId);
			// Classpath and main class
			cmd.add("-cp");
			cmd.add(classpath);
			cmd.add(Main.class.getName());
			// Original args
			if (launchArgs != null) for (String a : launchArgs) cmd.add(a);

			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.inheritIO();
			pb.start();
		} catch (Exception ex) {
			Logger.error("Failed to restart with new JVM heap", ex);
			javax.swing.JOptionPane.showMessageDialog(this, "Failed to restart: " + ex.getMessage(), "JVM Heap", javax.swing.JOptionPane.ERROR_MESSAGE);
			return;
		}
		// Exit current process after spawn
		System.exit(0);
	}

	// Attempts to locate the event dispatcher thread by its constant name.
	private Thread findEventThread() {
		ThreadGroup root = Thread.currentThread().getThreadGroup();
		while (root.getParent() != null) {
			root = root.getParent();
		}
		Thread[] threads = new Thread[Thread.activeCount() * 2];
		int n = root.enumerate(threads, true);
		for (int i = 0; i < n; i++) {
			Thread t = threads[i];
			if (t != null && org.je.device.ui.EventDispatcher.EVENT_DISPATCHER_NAME.equals(t.getName())) {
				return t;
			}
		}
		return null;
	}

	public static void main(String args[]) {
	// Keep a copy of the original args for restart
	launchArgs = args != null ? args.clone() : new String[0];
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
					UIManager.setLookAndFeel(new FlatMacDarkLaf());
				} else {
					UIManager.setLookAndFeel(new FlatMacLightLaf());
				}
				// Set specific FlatLaf properties to prevent menu bar integration on Windows
				UIManager.put("TitlePane.useWindowDecorations", false);
				UIManager.put("TitlePane.menuBarEmbedded", false);
			} else {
				// On other platforms, use FlatLaf themes normally
				String savedTheme = Config.getCurrentTheme();
				if ("dark".equals(savedTheme)) {
					UIManager.setLookAndFeel(new FlatMacDarkLaf());
				} else {
					UIManager.setLookAndFeel(new FlatMacLightLaf());
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

	private File getPicturesFolder() {
		String osName = System.getProperty("os.name").toLowerCase();
		String userHome = System.getProperty("user.home");
		
		if (osName.contains("windows")) {
			// Windows: %USERPROFILE%\Pictures
			return new File(userHome, "Pictures");
		} else if (osName.contains("mac")) {
			// macOS: ~/Pictures
			return new File(userHome, "Pictures");
		} else {
			// Linux: ~/Pictures (common) or fallback to user home
			File pictures = new File(userHome, "Pictures");
			if (pictures.exists() || pictures.mkdirs()) {
				return pictures;
			} else {
				// Fallback to user home directory
				return new File(userHome);
			}
		}
	}

	private abstract class CountTimerTask extends TimerTask {

		protected int counter;

		public CountTimerTask(int counter) {
			this.counter = counter;
		}

	}
}
