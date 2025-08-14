package org.je.app.ui.swing;

import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * Bazowa klasa panelu wyswietlanego w oknie dialogowym
 */

public class SwingDialogPanel extends JPanel
{

  public JButton btOk = new JButton("OK");
  public JButton btCancel = new JButton("Cancel");

  boolean state;
  
  boolean extra;

  /**
   * Walidacja panelu
   *
   * @param state czy wyswietlac komunikaty bledow
   * @return true jesli wszysko jest ok
   */
  public boolean check(boolean state)
  {
    return true;
  }


  protected void hideNotify()
  {
  }

  
  protected void showNotify()
  {
  }
  
  protected JButton getExtraButton()
  {
	return null;  
  }
  
  public boolean isExtraButtonPressed() {
	  return extra;
  }

}