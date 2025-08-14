package org.je;

import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;

import org.je.util.ExtendedRecordListener;
import org.je.util.RecordStoreImpl;

public interface RecordStoreManager {
	
	String getName();

	void deleteRecordStore(String recordStoreName) 
			throws RecordStoreNotFoundException, RecordStoreException;

	RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary) 
			throws RecordStoreException;

	String[] listRecordStores();
	
	void loadRecord(RecordStoreImpl recordStoreImpl, int recordId) 
			throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException;

	void deleteRecord(RecordStoreImpl recordStoreImpl, int recordId) 
			throws RecordStoreNotOpenException, RecordStoreException;

	void saveRecord(RecordStoreImpl recordStoreImpl, int recordId) 
			throws RecordStoreNotOpenException, RecordStoreException;

	int getSizeAvailable(RecordStoreImpl recordStoreImpl);

	/**
	 * Initialize RMS Manager before starting MIDlet 
	 */
	void init(MicroEmulator emulator);

	/**
	 * Delete all record stores.
	 */
	void deleteStores();

	void setRecordListener(ExtendedRecordListener recordListener);
	
	void fireRecordStoreListener(int type, String recordStoreName);

}
