package org.je.device;


public class DeviceFactory 
{
  private static Device device;
  
  
  public static Device getDevice()
  {
    return device;
  }
  
  
  public static void setDevice(Device device)
  {
	  if (DeviceFactory.device != null) {
		  DeviceFactory.device.destroy();
	  }
	  device.init();
	  DeviceFactory.device = device;
  }
  
}
