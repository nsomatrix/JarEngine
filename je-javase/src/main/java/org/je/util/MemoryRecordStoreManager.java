package org.je.util;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotFoundException;

import org.je.MicroEmulator;
import org.je.RecordStoreManager;

public class MemoryRecordStoreManager implements RecordStoreManager {
	private Hashtable recordStores = new Hashtable();

	private ExtendedRecordListener recordListener = null;

	public void init(MicroEmulator emulator) {
	}

	public String getName() {
		return "Memory record store";
	}

	public void deleteRecordStore(String recordStoreName) throws RecordStoreNotFoundException, RecordStoreException {
		RecordStoreImpl recordStoreImpl = (RecordStoreImpl) recordStores.get(recordStoreName);
		if (recordStoreImpl == null) {
			throw new RecordStoreNotFoundException(recordStoreName);
		}
		if (recordStoreImpl.isOpen()) {
			throw new RecordStoreException();
		}
		recordStores.remove(recordStoreName);

		fireRecordStoreListener(ExtendedRecordListener.RECORDSTORE_DELETE, recordStoreName);
	}

	public RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary)
			throws RecordStoreNotFoundException {
		RecordStoreImpl recordStoreImpl = (RecordStoreImpl) recordStores.get(recordStoreName);
		if (recordStoreImpl == null) {
			if (!createIfNecessary) {
				throw new RecordStoreNotFoundException(recordStoreName);
			}
			recordStoreImpl = new RecordStoreImpl(this, recordStoreName);
			recordStores.put(recordStoreName, recordStoreImpl);
		}
		recordStoreImpl.setOpen(true);
		if (recordListener != null) {
			recordStoreImpl.addRecordListener(recordListener);
		}

		fireRecordStoreListener(ExtendedRecordListener.RECORDSTORE_OPEN, recordStoreName);

		return recordStoreImpl;
	}

	public String[] listRecordStores() {
		String[] result = null;

		int i = 0;
		for (Enumeration e = recordStores.keys(); e.hasMoreElements();) {
			if (result == null) {
				result = new String[recordStores.size()];
			}
			result[i] = (String) e.nextElement();
			i++;
		}

		return result;
	}

	public void deleteRecord(RecordStoreImpl recordStoreImpl, int recordId) {
	}
	
	public void loadRecord(RecordStoreImpl recordStoreImpl, int recordId) {
	}

	public void saveRecord(RecordStoreImpl recordStoreImpl, int recordId) {
	}

	public void init() {
		deleteStores();
	}

	public void deleteStores() {
		if (recordStores != null)
			recordStores.clear();
	}

	public int getSizeAvailable(RecordStoreImpl recordStoreImpl) {
		// FIXME returns too much
		return (int) Runtime.getRuntime().freeMemory();
	}

	public void setRecordListener(ExtendedRecordListener recordListener) {
		this.recordListener = recordListener;
	}

	public void fireRecordStoreListener(int type, String recordStoreName) {
		if (recordListener != null) {
			recordListener.recordStoreEvent(type, System.currentTimeMillis(), recordStoreName);
		}
	}
}
