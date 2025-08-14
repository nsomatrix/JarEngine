package org.je.device;


public interface InputMethodListener
{

	public void caretPositionChanged(InputMethodEvent event);

	public void inputMethodTextChanged(InputMethodEvent event);

	public int getCaretPosition();
	
	public String getText();
    
    public int getConstraints();
	
}
