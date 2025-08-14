package org.je.microedition;

import java.util.Map;

/**
 * @author vlads
 * 
 * Optional JSR implementation can be plugged to Emulator using this interfaces.
 * See module je-jsr-75 as example
 * 
 * Relevant JarEngine command line option
 * 
 * <pre>
 *  --impl JSR_implementation_class_name Initialize and register optional JSR implementation class.
 * </pre>
 * 
 */
public interface ImplementationInitialization {

	/**
	 * See "--id EmulatorID" command line option
	 */
	public static final String PARAM_EMULATOR_ID = "emulatorID";

	/**
	 * 
	 * Call implementation initialization inside secure context.
	 * 
	 * @param parameters
	 *            Map of configuration options and emulatorID property.
	 */
	public void registerImplementation(Map parameters);

	/**
	 * Called when MIDlet started
	 */
	public void notifyMIDletStart();

	/**
	 * Called when MIDlet exits or destroyed
	 */
	public void notifyMIDletDestroyed();
}
