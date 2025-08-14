package org.je.app.ui.swing;

import java.io.File;
import java.util.Hashtable;

import javax.swing.filechooser.FileFilter;


public class ExtensionFileFilter extends FileFilter 
{
  
  String description;
  
  Hashtable extensions = new Hashtable(); 
  

  public ExtensionFileFilter(String description)
  {
    this.description = description;
  }
  
  
  public boolean accept(File file) 
  {
    if(file != null) {
	    if(file.isDirectory()) {
        return true;
	    }
      String ext = getExtension(file);
      if(ext != null && extensions.get(ext) != null) {
        return true;
      }
    }
    
  	return false;
  }
  
  
  public void addExtension(String extension)
  {
    extensions.put(extension.toLowerCase(), this);
  }

  
  public String getDescription() 
  {
    return description;
  }

  
  String getExtension(File file) 
  {
    if (file != null) {
	    String filename = file.getName();
	    int i = filename.lastIndexOf('.');
	    if (i > 0 && i < filename.length() - 1) {
        return filename.substring(i + 1).toLowerCase();
	    }
    }
  
    return null;
  }
  
}
