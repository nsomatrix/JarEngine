package org.je.app.util;

import nanoxml.XMLElement;

public interface XMLItem {

	public void save(XMLElement xml);
	
	public void read(XMLElement xml);
}
