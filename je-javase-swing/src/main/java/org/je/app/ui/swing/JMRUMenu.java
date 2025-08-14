package org.je.app.ui.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.ImageIcon;

import org.je.app.util.MRUListListener;
import org.je.app.util.MidletURLReference;

/**
 * @author vlads
 * 
 */
public class JMRUMenu extends JMenu implements MRUListListener {

	private static final long serialVersionUID = 1L;

	public static class MRUActionEvent extends ActionEvent {

		private static final long serialVersionUID = 1L;

		Object sourceMRU;

		public MRUActionEvent(Object sourceMRU, ActionEvent orig) {
			super(orig.getSource(), orig.getID(), orig.getActionCommand(), orig.getWhen(), orig.getModifiers());
			this.sourceMRU = sourceMRU;
		}

		public Object getSourceMRU() {
			return sourceMRU;
		}

	}

	public JMRUMenu(String s) {
		super(s);
	}

	public void listItemChanged(final Object item) {
		// Convert to MidletURLReference if possible, so we can show name and icon
		final MidletURLReference ref = (item instanceof MidletURLReference) ? (MidletURLReference) item : null;
		final String label = (ref != null && ref.getName() != null && !ref.getName().isEmpty())
				? ref.getName() : item.toString();

		// De-duplicate by label (suite name) or fallback to string
		for (int i = 0; i < getItemCount(); i++) {
			if (getItem(i).getText().equals(label)) {
				remove(i);
				break;
			}
		}

		AbstractAction a = new AbstractAction(label) {

			private static final long serialVersionUID = 1L;

			Object sourceMRU = item;

			public void actionPerformed(ActionEvent e) {
				JMRUMenu.this.fireActionPerformed(new MRUActionEvent(sourceMRU, e));
			}
		};

		JMenuItem menu = new JMenuItem(a);
		// Attach icon when we know the MIDlet reference
		try {
			if (ref != null) {
				ImageIcon ic = MidletIconLoader.resolveIcon(ref);
				if (ic != null) menu.setIcon(ic);
			}
		} catch (Throwable ignore) {}
		this.insert(menu, 0);
	}

	/**
	 * Do not create new Event
	 */
	protected void fireActionPerformed(ActionEvent event) {
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ActionListener.class) {
				((ActionListener) listeners[i + 1]).actionPerformed(event);
			}
		}
	}

}
