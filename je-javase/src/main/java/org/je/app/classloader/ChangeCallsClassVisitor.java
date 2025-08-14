package org.je.app.classloader;

import java.util.HashMap;
import java.util.Map;

import org.je.app.util.MIDletThread;
import org.je.app.util.MIDletTimer;
import org.je.app.util.MIDletTimerTask;
import org.je.log.Logger;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * @author vlads
 * 
 */
public class ChangeCallsClassVisitor extends ClassAdapter {

	InstrumentationConfig config;

	static final Map javaVersion = new HashMap();

	static {
		javaVersion.put(new Integer(0x3002D), "1.1");
		javaVersion.put(new Integer(0x3002E), "1.2");
		javaVersion.put(new Integer(47), "1.3");
		javaVersion.put(new Integer(48), "1.4");
		javaVersion.put(new Integer(49), "1.5");
		javaVersion.put(new Integer(50), "1.6");
	}

	public ChangeCallsClassVisitor(ClassVisitor cv, InstrumentationConfig config) {
		super(cv);
		this.config = config;
	}

	public void visit(final int version, final int access, final String name, final String signature, String superName,
			final String[] interfaces) {
		if ((0xFF & version) >= 49) {
			String v = (String) javaVersion.get(new Integer(version));
			Logger.warn("Loading MIDlet class " + name + " of version " + version + ((v == null) ? "" : (" " + v)));
		}
		if (config.isEnhanceThreadCreation()) {
			if (superName.equals("java/lang/Thread")) {
				superName = ChangeCallsMethodVisitor.codeName(MIDletThread.class);
			} else if (superName.equals("java/util/Timer")) {
				superName = ChangeCallsMethodVisitor.codeName(MIDletTimer.class);
			} else if (superName.equals("java/util/TimerTask")) {
				superName = ChangeCallsMethodVisitor.codeName(MIDletTimerTask.class);
			}
		}
		super.visit(version, access, name, signature, superName, interfaces);
	}

	public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
			final String[] exceptions) {
		return new ChangeCallsMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions), config);
	}

}
