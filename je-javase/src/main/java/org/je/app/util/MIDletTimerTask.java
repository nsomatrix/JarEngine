package org.je.app.util;

import java.util.TimerTask;

public abstract class MIDletTimerTask extends TimerTask {
	
	MIDletTimer timer;

	long time = -1;
	
	long period;
	
	boolean oneTimeTaskExcecuted = false;
	
	public boolean cancel() {
		if (timer == null) {
			return false;
		}
		
		synchronized (timer.tasks) {
			// task was never scheduled
			if (time == -1) {
				return false;
			}		
			// task was scheduled for one-time execution and has already run
			if (oneTimeTaskExcecuted) {
				return false;
			}
			timer.tasks.remove(this);
		}

		return true;
	}

	public long scheduledExecutionTime() {
		return time;
	}

}
