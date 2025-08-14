package org.je.cldc;

import java.security.cert.X509Certificate;

import javax.microedition.pki.Certificate;

public class CertificateImpl implements Certificate {
	
	private X509Certificate cert;

	public CertificateImpl(X509Certificate cert) {
		this.cert = cert;
	}

	public String getIssuer() {
		return cert.getIssuerDN().getName();
	}

	public long getNotAfter() {
		return cert.getNotAfter().getTime();
	}

	public long getNotBefore() {
		return cert.getNotBefore().getTime();
	}

	public String getSerialNumber() {
		return cert.getSerialNumber().toString();
	}

	public String getSigAlgName() {
		return cert.getSigAlgName();
	}

	public String getSubject() {
		return cert.getSubjectDN().getName();
	}

	public String getType() {
		return cert.getType();
	}

	public String getVersion() {
		return Integer.toString(cert.getVersion());
	}

}
