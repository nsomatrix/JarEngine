package javax.microedition.rms;

import org.je.MIDletBridge;


public class RecordStore 
{  
	
	public static final int AUTHMODE_PRIVATE = 0;
	
	public static final int AUTHMODE_ANY = 1;
	
//	TODO there should be a public constructors
//	private RecordStore() {
//	    
//	}
	
	public static void deleteRecordStore(String recordStoreName)
			throws RecordStoreException, RecordStoreNotFoundException
	{
		MIDletBridge.getRecordStoreManager().deleteRecordStore(recordStoreName);
	}
	
	
    public static String[] listRecordStores()
    {
    	return MIDletBridge.getRecordStoreManager().listRecordStores();
    }
	
	
    public static RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary) 
    		throws RecordStoreException, RecordStoreFullException, RecordStoreNotFoundException
    {
        return MIDletBridge.getRecordStoreManager().openRecordStore(recordStoreName, createIfNecessary);
    }
    
    
    public static RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary, int authmode, boolean writable)
    		throws RecordStoreException, RecordStoreFullException, RecordStoreNotFoundException
    {
		// TODO Not yet implemented
    	return openRecordStore(recordStoreName, createIfNecessary);
    }
	
    
    public static RecordStore openRecordStore(String recordStoreName, String vendorName, String suiteName)
    		throws RecordStoreException, RecordStoreNotFoundException
	{
		// TODO Not yet implemented
    	return openRecordStore(recordStoreName, false);
	}
	
    public void closeRecordStore() 
    		throws RecordStoreNotOpenException, RecordStoreException
    {
    	// Must be overriden
    }
	
	
	public String getName()
			throws RecordStoreNotOpenException
	{
    	// Must be overriden
		
		return null;
	}
	
	
    public int getVersion() 
    		throws RecordStoreNotOpenException
    {
    	// Must be overriden
		
		return -1;
    }


    public int getNumRecords() 
    		throws RecordStoreNotOpenException
    {
    	// Must be overriden
		
		return -1;
    }


    public int getSize()
    		throws RecordStoreNotOpenException
    {
    	// Must be overriden
		
		return -1;
    }

  
    public int getSizeAvailable()
    		throws RecordStoreNotOpenException
    {
    	// Must be overriden
		
		return -1;
    }

  
    public long getLastModified() 
    		throws RecordStoreNotOpenException
    {
    	// Must be overriden
		
		return -1;
    }


    public void addRecordListener(RecordListener listener)
    {
    	// Must be overriden
    }
	
	
    public void removeRecordListener(RecordListener listener)
    {
    	// Must be overriden
    }
	
	
    public int getNextRecordID() 
    		throws RecordStoreNotOpenException, RecordStoreException
    {
    	// Must be overriden
		
		return -1;
    }
	
	
    public int addRecord(byte[] data, int offset, int numBytes)
            throws RecordStoreNotOpenException, RecordStoreException, RecordStoreFullException
    {
    	// Must be overriden
		
		return -1;
    }
	
	
    public void deleteRecord(int recordId) 
    		throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException
    {
    	// Must be overriden
    }
	

    public int getRecordSize(int recordId) 
    		throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException
    {
    	// Must be overriden
		
		return -1;
    }
	

    public int getRecord(int recordId, byte[] buffer, int offset)
            throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException
    {
    	// Must be overriden
		
		return -1;
    }
	
	
    public byte[] getRecord(int recordId) 
    		throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException
    {
    	// Must be overriden
		
		return null;
    }
	
    
    public void setMode(int authmode, boolean writable)
    		throws RecordStoreException
	{
		// TODO Not yet implemented
	}
	
    public void setRecord(int recordId, byte[] newData, int offset, int numBytes)
            throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException, RecordStoreFullException
    {
    	// Must be overriden
    }

  	
    public RecordEnumeration enumerateRecords(RecordFilter filter, RecordComparator comparator, boolean keepUpdated)
            throws RecordStoreNotOpenException
    {
  		// Must be overriden
    	
    	return null;
    }
    
}

