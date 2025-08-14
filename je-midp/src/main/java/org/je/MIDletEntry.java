package org.je;

import javax.microedition.midlet.MIDlet;


public class MIDletEntry 
{

  private String name;
  private Class midletClass;
  
  
  public MIDletEntry(String name, Class midletClass)
  {
    this.name = name;
    this.midletClass = midletClass;
  }
  
  
  public String getName()
  {
    return name;
  }
  
  
  public Class getMIDletClass()
  {
    return midletClass;
  }

}
