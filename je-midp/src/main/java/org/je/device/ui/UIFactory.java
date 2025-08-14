package org.je.device.ui;

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

public interface UIFactory {
	
	EventDispatcher createEventDispatcher(Display display);
	
	CommandUI createCommandUI(Command command);
	
	/*
	 *  DisplayableUI
	 */
	
	AlertUI createAlertUI(Alert alert);

	CanvasUI createCanvasUI(Canvas canvas);
	
	FormUI createFormUI(Form form);

	ListUI createListUI(List list);
	
	TextBoxUI createTextBoxUI(TextBox textBox);
	
	/*
	 *  ItemUI
	 */

	ChoiceGroupUI createChoiceGroupUI(ChoiceGroup choiceGroup, int choiceType);

	CustomItemUI createCustomItemUI(CustomItemAccess customItemAccess);

	DateFieldUI createDateFieldUI(DateField dateField);

	GaugeUI createGaugeUI(Gauge gauge);
	
	ImageStringItemUI createImageStringItemUI(Item item);

	TextFieldUI createTextFieldUI(TextField textField);

}
