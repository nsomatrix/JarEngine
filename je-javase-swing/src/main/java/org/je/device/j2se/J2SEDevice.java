package org.je.device.j2se;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.DateField;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;

import org.je.CustomItemAccess;
import org.je.device.impl.DeviceImpl;
import org.je.device.impl.ui.CommandImplUI;
import org.je.device.j2se.ui.J2SEAlertUI;
import org.je.device.j2se.ui.J2SECanvasUI;
import org.je.device.j2se.ui.J2SEChoiceGroupUI;
import org.je.device.j2se.ui.J2SECustomItemUI;
import org.je.device.j2se.ui.J2SEDateFieldUI;
import org.je.device.j2se.ui.J2SEFormUI;
import org.je.device.j2se.ui.J2SEGaugeUI;
import org.je.device.j2se.ui.J2SEImageStringItemUI;
import org.je.device.j2se.ui.J2SEListUI;
import org.je.device.j2se.ui.J2SETextBoxUI;
import org.je.device.j2se.ui.J2SETextFieldUI;
import org.je.device.ui.AlertUI;
import org.je.device.ui.CanvasUI;
import org.je.device.ui.ChoiceGroupUI;
import org.je.device.ui.CommandUI;
import org.je.device.ui.CustomItemUI;
import org.je.device.ui.DateFieldUI;
import org.je.device.ui.EventDispatcher;
import org.je.device.ui.FormUI;
import org.je.device.ui.GaugeUI;
import org.je.device.ui.ImageStringItemUI;
import org.je.device.ui.ListUI;
import org.je.device.ui.TextBoxUI;
import org.je.device.ui.TextFieldUI;
import org.je.device.ui.UIFactory;

public class J2SEDevice extends DeviceImpl {

	private UIFactory ui = new UIFactory() {
		
		public EventDispatcher createEventDispatcher(Display display) {
			EventDispatcher eventDispatcher = new EventDispatcher();
			Thread thread = new Thread(eventDispatcher, EventDispatcher.EVENT_DISPATCHER_NAME);
			thread.setDaemon(true);
			thread.start();
			
			return eventDispatcher;
		}

		public CommandUI createCommandUI(Command command) {
			return new CommandImplUI(command);
		}

		public AlertUI createAlertUI(Alert alert) {
			return new J2SEAlertUI(alert);
		}

		public CanvasUI createCanvasUI(Canvas canvas) {
			return new J2SECanvasUI(canvas);
		}
		
		public FormUI createFormUI(Form form) {
			return new J2SEFormUI(form);
		}

		public ListUI createListUI(List list) {
			return new J2SEListUI(list);
		}

		public TextBoxUI createTextBoxUI(TextBox textBox) {
			return new J2SETextBoxUI(textBox);
		}

		public ChoiceGroupUI createChoiceGroupUI(ChoiceGroup choiceGroup, int choiceType) {
			return new J2SEChoiceGroupUI(choiceGroup, choiceType);
		}

		public CustomItemUI createCustomItemUI(CustomItemAccess customItemAccess) {
			return new J2SECustomItemUI(customItemAccess);
		}

		public DateFieldUI createDateFieldUI(DateField dateField) {
			return new J2SEDateFieldUI(dateField);
		}

		public GaugeUI createGaugeUI(Gauge gauge) {
			return new J2SEGaugeUI(gauge);
		}

		public ImageStringItemUI createImageStringItemUI(Item item) {
			return new J2SEImageStringItemUI(item);
		}
		
		public TextFieldUI createTextFieldUI(TextField textField) {
			return new J2SETextFieldUI(textField);
		}

	};

	public UIFactory getUIFactory() {
		return ui;
	}

}
