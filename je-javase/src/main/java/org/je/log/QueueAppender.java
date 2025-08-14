package org.je.log;

import java.util.LinkedList;
import java.util.List;

public class QueueAppender implements LoggerAppender {

	private int buferSize;

	private List queue = new LinkedList();

	public QueueAppender(int buferSize) {
		this.buferSize = buferSize;
	}

	public void append(LoggingEvent event) {
		queue.add(event);
		if (queue.size() > buferSize) {
			queue.remove(0);
		}
	}

	public LoggingEvent poll() {
		if (queue.size() == 0) {
			return null;
		}
		return (LoggingEvent) queue.remove(0);
	}

}
