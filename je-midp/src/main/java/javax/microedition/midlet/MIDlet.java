package javax.microedition.midlet;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.lcdui.Display;

import org.je.DisplayAccess;
import org.je.MIDletAccess;
import org.je.MIDletBridge;

public abstract class MIDlet {

	private boolean destroyed;

	class MIDletAccessor extends MIDletAccess {

		public MIDletAccessor() {
			super(MIDlet.this);
			destroyed = false;
		}

		public void startApp() throws MIDletStateChangeException {
			MIDletBridge.setCurrentMIDlet(midlet);
			midlet.startApp();
		}

		public void pauseApp() {
			midlet.pauseApp();
		}

		public void destroyApp(boolean unconditional) throws MIDletStateChangeException {
			if (!midlet.destroyed) {
				midlet.destroyApp(unconditional);
			}
			DisplayAccess da = getDisplayAccess();
			if (da != null) {
				da.clean();
				setDisplayAccess(null);
			}
			MIDletBridge.destroyMIDletContext(MIDletBridge.getMIDletContext(midlet));
		}
	}

	protected MIDlet() {
		MIDletBridge.registerMIDletAccess(new MIDletAccessor());

		// Initialize Display
		Display.getDisplay(this);
	}

	protected abstract void startApp() throws MIDletStateChangeException;

	protected abstract void pauseApp();

	protected abstract void destroyApp(boolean unconditional) throws MIDletStateChangeException;

	public final int checkPermission(String permission) {
		return MIDletBridge.checkPermission(permission);
	}

	public final String getAppProperty(String key) {
		return MIDletBridge.getAppProperty(key);
	}

	public final void notifyDestroyed() {
		destroyed = true;
		MIDletBridge.notifyDestroyed();
	}

	public final void notifyPaused() {
	}

	public final boolean platformRequest(String URL) throws ConnectionNotFoundException {
		return MIDletBridge.platformRequest(URL);
	}

	public final void resumeRequest() {
		// TODO implement
	}

}
