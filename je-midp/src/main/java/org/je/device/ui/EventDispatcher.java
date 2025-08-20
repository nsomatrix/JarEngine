package org.je.device.ui;

import org.je.device.DeviceFactory;
import org.je.performance.PerformanceManager;

public class EventDispatcher implements Runnable {
	
	public static final String EVENT_DISPATCHER_NAME = "event-thread";

	public static int maxFps = -1;

	private volatile boolean cancelled = false;
	
	private Event head = null;

	private Event tail = null;

	private PaintEvent scheduledPaintEvent = null;

	private PointerEvent scheduledPointerDraggedEvent = null;

	private Object serviceRepaintsLock = new Object();
	
	private long lastPaintEventTime = 0;
	
	// High-precision timing for smoother frame pacing
	private static final boolean USE_NANOSECOND_TIMING = true;
	
	// Adaptive frame pacing for smoother performance
	private static volatile boolean adaptiveFramePacing = true;
	private static volatile long frameTimeVariance = 0;
	private static volatile int consecutiveSlowFrames = 0;
	private static final int MAX_CONSECUTIVE_SLOW_FRAMES = 3;

	public EventDispatcher() {
	}
	
	public void run() {

		while (!cancelled) {
			Event event = null;
			synchronized (this) {
				if (head != null) {
					event = head;

									if (maxFps > 0 && event instanceof PaintEvent) {
					long currentTime, targetInterval, difference;
					if (USE_NANOSECOND_TIMING) {
						currentTime = System.nanoTime() / 1000000; // Convert to milliseconds
						targetInterval = 1000 / maxFps;
						difference = currentTime - lastPaintEventTime;
					} else {
						currentTime = System.currentTimeMillis();
						targetInterval = 1000 / maxFps;
						difference = currentTime - lastPaintEventTime;
					}
					
					// Adaptive frame pacing: adjust for frame time variance
					if (adaptiveFramePacing && frameTimeVariance > 0) {
						// Allow some variance tolerance for smoother pacing
						long tolerance = frameTimeVariance / 4;
						targetInterval = Math.max(targetInterval - tolerance, Math.max(targetInterval / 2, 1)); // Ensure minimum 1ms
					}
					
					if (difference < targetInterval) {
						// Track consecutive slow frames
						if (difference < targetInterval / 2) {
							consecutiveSlowFrames++;
							if (consecutiveSlowFrames > MAX_CONSECUTIVE_SLOW_FRAMES) {
								// Skip frame limiting temporarily to catch up
								consecutiveSlowFrames = 0;
							} else {
								event = null;
															long waitTime = targetInterval - difference;
							// Use shorter waits for better responsiveness and prevent negative waits
							waitTime = Math.max(1, Math.min(waitTime, 16)); // Cap at ~60fps equivalent, minimum 1ms
								try {
									wait(waitTime);
								} catch (InterruptedException e) {
									Thread.currentThread().interrupt();
								}
							}
						} else {
							consecutiveSlowFrames = 0;
							event = null;
							try {
								wait(targetInterval - difference);
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							}
						}
					} else {
						consecutiveSlowFrames = 0;
								// Update frame time variance for adaptive pacing
		if (adaptiveFramePacing) {
			// Prevent overflow and ensure reasonable bounds
			long newVariance = (frameTimeVariance + Math.abs(difference - targetInterval)) / 2;
			frameTimeVariance = Math.min(newVariance, 1000); // Cap at 1 second
		}
					}
				}
					
					if (event != null) {
						head = event.next;
						if (head == null) {
							tail = null;
						}
						if (event instanceof PointerEvent && ((PointerEvent) event).type == PointerEvent.POINTER_DRAGGED) {
							scheduledPointerDraggedEvent = null;
						}
					}
				} else {
					try {
						wait();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt(); // Restore interrupt status
						break; // Exit loop if interrupted
					}
				}
			}

			if (event != null) {
				if (event instanceof PaintEvent) {
					// Frame skipping check
					if (PerformanceManager.shouldSkipPaintFrame()) {
						// Skip this frame but release any threads waiting in serviceRepaints()
						lastPaintEventTime = USE_NANOSECOND_TIMING ? 
							System.nanoTime() / 1000000 : System.currentTimeMillis();
						// Clear scheduled paint reference so subsequent paints can enqueue
						synchronized (this) { scheduledPaintEvent = null; }
						// Notify potential waiters that the (skipped) repaint cycle ended
						synchronized (serviceRepaintsLock) { serviceRepaintsLock.notifyAll(); }
						continue;
					}
					synchronized (serviceRepaintsLock) {
						synchronized (this) {
							scheduledPaintEvent = null;
						}
						lastPaintEventTime = USE_NANOSECOND_TIMING ? 
							System.nanoTime() / 1000000 : System.currentTimeMillis();
						post(event);
						serviceRepaintsLock.notifyAll();
					}					
				} else {
					post(event);
				}
			} else {
				// Idle path
				PerformanceManager.onIdleWaitHook();
			}
		}
	}

	/**
	 * Do not service any more events
	 */
	public final void cancel() {
		cancelled = true;
		synchronized (this) {
			notify();
		}
	}
	
	/**
	 * Enable/disable adaptive frame pacing for smoother performance
	 */
	public static void setAdaptiveFramePacing(boolean enabled) {
		adaptiveFramePacing = enabled;
		if (!enabled) {
			frameTimeVariance = 0;
			consecutiveSlowFrames = 0;
		}
	}
	
	/**
	 * Get current adaptive frame pacing status
	 */
	public static boolean isAdaptiveFramePacing() {
		return adaptiveFramePacing;
	}

	public void put(Event event) {
		synchronized (this) {
			if (event instanceof PaintEvent && scheduledPaintEvent != null) {
				scheduledPaintEvent.merge((PaintEvent) event);
			} else if (event instanceof PointerEvent && scheduledPointerDraggedEvent != null
					&& ((PointerEvent) event).type == PointerEvent.POINTER_DRAGGED) {
				scheduledPointerDraggedEvent.x = ((PointerEvent) event).x;
				scheduledPointerDraggedEvent.y = ((PointerEvent) event).y;
			} else {
				if (event instanceof PaintEvent) {
					scheduledPaintEvent = (PaintEvent) event;
				}
				if (event instanceof PointerEvent && ((PointerEvent) event).type == PointerEvent.POINTER_DRAGGED) {
					scheduledPointerDraggedEvent = (PointerEvent) event;
				}
				if (tail != null) {
					tail.next = event;
				}
				tail = event;
				if (head == null) {
					head = event;
				}
				notify();
			}
		}
	}

	public void put(Runnable runnable) {
		put(new RunnableEvent(runnable));
	}

	public void serviceRepaints() {
		synchronized (serviceRepaintsLock) {
			synchronized (this) {
				if (scheduledPaintEvent == null) {
					return;
				}

				// TODO move scheduledPaintEvent to head
			}

			try {
				serviceRepaintsLock.wait();
			} catch (InterruptedException e) {
			}
		}
	}

	protected void post(Event event) {
		event.run();
	}

	public abstract class Event implements Runnable {

		Event next = null;

	}

	public final class PaintEvent extends Event {

		private int x = -1, y = -1, width = -1, height = -1;

		public PaintEvent(int x, int y, int width, int height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}

		public void run() {
			DeviceFactory.getDevice().getDeviceDisplay().repaint(x, y, width, height);
		}

		/**
		 * Do a 2-D merge of the paint areas
		 * 
		 * @param event
		 */
		public final void merge(PaintEvent event) {
			int xMax = x + width;
			int yMax = y + height;

			this.x = Math.min(this.x, event.x);
			xMax = Math.max(xMax, event.x + event.width);

			this.y = Math.min(this.y, event.y);
			yMax = Math.max(yMax, event.y + event.height);

			this.width = xMax - x;
			this.height = yMax - y;
		}

	}

	public final class PointerEvent extends EventDispatcher.Event {

		public static final short POINTER_PRESSED = 0;

		public static final short POINTER_RELEASED = 1;

		public static final short POINTER_DRAGGED = 2;

		private Runnable runnable;

		private short type;

		private int x;

		private int y;

		public PointerEvent(Runnable runnable, short type, int x, int y) {
			this.runnable = runnable;
			this.type = type;
			this.x = x;
			this.y = y;
		}

		public void run() {
			runnable.run();
		}
	}
	
	public class RunnableEvent extends Event {

		private Runnable runnable;

		public RunnableEvent(Runnable runnable) {
			this.runnable = runnable;
		}

		public void run() {
			runnable.run();
		}

	}
	
}
