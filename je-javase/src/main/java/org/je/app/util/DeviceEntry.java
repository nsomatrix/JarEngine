package org.je.app.util;

import org.je.device.EmulatorContext;

public class DeviceEntry {
	
	private String name;

	private String fileName;

	private String descriptorLocation;

	private boolean defaultDevice;

	private boolean canRemove;

	/**
	 * @deprecated
	 */
	private String className;

	/**
	 * @deprecated
	 */
	private EmulatorContext emulatorContext;

	public DeviceEntry(String name, String fileName, String descriptorLocation, boolean defaultDevice) {
		this(name, fileName, descriptorLocation, defaultDevice, true);
	}

	public DeviceEntry(String name, String fileName, String descriptorLocation, boolean defaultDevice, boolean canRemove) {
		this.name = name;
		this.fileName = fileName;
		this.descriptorLocation = descriptorLocation;
		this.defaultDevice = defaultDevice;
		this.canRemove = canRemove;
	}

	/**
	 * @deprecated use new DeviceEntry(String name, String fileName, String descriptorLocation, boolean defaultDevice);
	 */
	public DeviceEntry(String name, String fileName, boolean defaultDevice, String className,
			EmulatorContext emulatorContext) {
		this(name, fileName, null, defaultDevice, true);

		this.className = className;
		this.emulatorContext = emulatorContext;
	}

	public boolean canRemove() {
		return canRemove;
	}

	public String getDescriptorLocation() {
		return descriptorLocation;
	}

	public String getFileName() {
		return fileName;
	}

	/**
	 * @deprecated
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getName() {
		return name;
	}

	public boolean isDefaultDevice() {
		return defaultDevice;
	}

	public void setDefaultDevice(boolean b) {
		defaultDevice = b;
	}

	public boolean equals(DeviceEntry test) {
		if (test == null) {
			return false;
		}
		if (test.getDescriptorLocation().equals(getDescriptorLocation())) {
			return true;
		}

		return false;
	}

	public String toString() {
		if (defaultDevice) {
			return name + " (default)";
		} else {
			return name;
		}
	}

}