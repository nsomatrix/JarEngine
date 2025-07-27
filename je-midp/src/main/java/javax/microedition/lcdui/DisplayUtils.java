package javax.microedition.lcdui;

import org.je.device.ui.DisplayableUI;

public class DisplayUtils
{

    public static DisplayableUI getDisplayableUI(Displayable displayable) {
        return displayable.ui;
    }
}
