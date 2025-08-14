package org.je.app.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Special InputStream wrapper for loading resources is needed to change
 * behavior of read(byte[] b) under Java SE. All existing Java ME
 * implementations always read all bytes available in one read call.
 */
public class MIDletResourceInputStream extends InputStream {

	private InputStream is;

	public MIDletResourceInputStream(InputStream is) {
		this.is = is;
	}

	public int available() throws IOException {
		return is.available();
	}

	public int read() throws IOException {
		return is.read();
	}

	public int read(byte[] b) throws IOException {
		int result = 0;
		int count = 0;
		do {
			count = is.read(b, result, b.length - result);
			if (count != -1) {
				result += count;
				if (result == b.length) {
					return result;
				}
			} else if (result != 0) {
				return result;
			}
		} while (count != -1);

		return -1;
	}

}
