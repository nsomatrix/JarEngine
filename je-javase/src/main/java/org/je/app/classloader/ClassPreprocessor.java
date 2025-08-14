package org.je.app.classloader;

import java.io.IOException;
import java.io.InputStream;

import org.je.log.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

/**
 * @author vlads
 *
 */
public class ClassPreprocessor {

	public static byte[] instrument(final InputStream classInputStream, InstrumentationConfig config) {
		try {
			ClassReader cr = new ClassReader(classInputStream);
			ClassWriter cw = new ClassWriter(0);
			ClassVisitor cv = new ChangeCallsClassVisitor(cw, config);
			cr.accept(cv, 0);
			return cw.toByteArray();
		} catch (IOException e) {
			Logger.error("Error loading MIDlet class", e);
			return null;
		} 
    }
	
}
