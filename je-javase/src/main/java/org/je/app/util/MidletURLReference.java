package org.je.app.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;
import java.util.Vector;

import nanoxml.XMLElement;

import org.je.log.Logger;

/**
 * @author vlads
 *
 */
public class MidletURLReference implements XMLItem {


	private String name;
	
	private String url;

	public MidletURLReference() {
		super();
	}
	
	/**
	 * @param name MIDlet name
	 * @param url  URL to locate this URL
	 */
	public MidletURLReference(String name, String url) {
		super();
		this.name = name;
		this.url = url;
	}

	public boolean equals(Object obj) {
		 if (!(obj instanceof MidletURLReference)) {
			 return false;
		 }
		 return ((MidletURLReference)obj).url.equals(url); 
	}
	 
	public String toString() {
		// make the text presentation shorter.
		URL u;
		try {
			u = new URL(url);
		} catch (MalformedURLException e) {
			Logger.error(e);
			return url;
		}
		StringBuffer b = new StringBuffer();
		
		String scheme = u.getProtocol();
		if (scheme.equals("file") || scheme.startsWith("http")) {
			b.append(scheme).append("://");
			if (u.getHost() != null) {
				b.append(u.getHost());
			}
			Vector pathComponents = new Vector();
			final String pathSeparator = "/";
			StringTokenizer st = new StringTokenizer(u.getPath(), pathSeparator);
			while (st.hasMoreTokens()) {
				pathComponents.add(st.nextToken());
			}
			if (pathComponents.size() > 3) {
				b.append(pathSeparator);
				b.append(pathComponents.get(0));
				b.append(pathSeparator).append("...").append(pathSeparator);
				b.append(pathComponents.get(pathComponents.size()-2));
				b.append(pathSeparator);
				b.append(pathComponents.get(pathComponents.size()-1));
			} else {
				b.append(u.getPath());
			}
			
		} else {
			b.append(url);
		}
		if (name != null) {
			b.append(" - ");
			b.append(name);
		}
		return b.toString();
	}
	
	public void read(XMLElement xml) {
		name = xml.getChildString("name", "");
		url = xml.getChildString("url", "");
	}

	public void save(XMLElement xml) {
		xml.removeChildren();
		xml.addChild("name", name);
		xml.addChild("url", url);
	}

	public String getName() {
		return this.name;
	}

	public String getUrl() {
		return this.url;
	}
	
}
