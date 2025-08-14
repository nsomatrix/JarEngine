package org.je.cldc;

import javax.microedition.io.SecurityInfo;
import javax.microedition.pki.Certificate;
import org.je.log.Logger;

public class SecurityInfoImpl implements SecurityInfo {

	private String cipherSuite;
	private String protocolName;
	private Certificate certificate;

	public SecurityInfoImpl(String cipherSuite, String protocolName, Certificate certificate) {
		this.cipherSuite = cipherSuite;
		this.protocolName = protocolName;
		this.certificate = certificate;
	}

	public String getCipherSuite() {
		return cipherSuite;
	}

	public String getProtocolName() {
		if (protocolName.startsWith("TLS")) {
			return "TLS";
		} else if (protocolName.startsWith("SSL")) {
			return "SSL";
		} else {
			// TODO Auto-generated method stub
			try {
				throw new RuntimeException();
			} catch (RuntimeException ex) {
				Logger.error(ex);
				throw ex;
			}
		}
	}

	public String getProtocolVersion() {
		if (protocolName.startsWith("TLS")) {
			return "3.1";
		} else if (getProtocolName().equals("SSL")) {
			return "3.0";
		} else {
			// TODO Auto-generated method stub
			try {
				throw new RuntimeException();
			} catch (RuntimeException ex) {
				Logger.error(ex);
				throw ex;
			}
		}
	}

	public Certificate getServerCertificate() {
		return certificate;
	}

}
