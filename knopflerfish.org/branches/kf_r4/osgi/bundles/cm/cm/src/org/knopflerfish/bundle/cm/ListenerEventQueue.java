package org.knopflerfish.bundle.cm;

import java.util.Vector;

import org.osgi.framework.BundleContext;

/**
 * @author js
 */
final public class ListenerEventQueue implements Runnable {

	/**
	 ** The thread running this object.
	 **/

	private Thread thread;

	/**
	 ** The thread running this object.
	 **/

	private final Object threadLock = new Object();

	/**
	 ** The queue of events.
	 **/

	private Vector queue = new Vector();

	/**
	 * The bundle context
	 */
	private BundleContext bc;

	/**
	 ** Construct an UpdateQueue given a  
	 ** BundleContext.
	 **
	 ** @param bc The BundleContext to use.
	 **/
	ListenerEventQueue(BundleContext bc) {
		this.bc = bc;
	}

	/**
	 ** Overide of Thread.run().
	 **/

	public void run() {
		while (true) {
			if (doListenerUpdateQueueLogging()) {
				Activator.log
						.debug("[ListenerEventQueue] Getting next ListenerEvent from queue");
			}
			ListenerEvent update = dequeue();
			if (update == null) {
				if (doListenerUpdateQueueLogging()) {
					Activator.log
							.debug("[ListenerEventQueue] Got null ListenerEvent from queue");
				}
				return;
			} else {
				if (doListenerUpdateQueueLogging()) {
					Activator.log
							.debug("[ListenerEventQueue] Got an ListenerEvent from queue");
				}
				try {
					if (doListenerUpdateQueueLogging()) {
						Activator.log
								.debug("[ListenerEventQueue] Calling ListnerUpdate.sendEvent");
					}
					update.sendEvent(bc);
					if (doListenerUpdateQueueLogging()) {
						Activator.log
								.debug("[ListenerEventQueue] ListenerEvent.sendEvent returned");
					}
				} catch (Throwable t) {
					Activator.log.error("[CM] Error while sending event", t);
				}
			}
		}
	}

	/**
	 ** Add an entry to the end of the queue.
	 **
	 ** @param update The Update to add to the queue.
	 **
	 ** @throws java.lang.IllegalArgumentException If given a null argument.
	 **/
	public synchronized void enqueue(ListenerEvent update) {
		if (update == null) {
			throw new IllegalArgumentException(
					"ListenerEventQueue.enqueue(ListenerEvent) needs a non-null argument.");
		}
		if (doListenerUpdateQueueLogging()) {
			Activator.log.debug("[ListenerEventQueue] Adding update to queue");
		}
		queue.addElement(update);
		attachNewThreadIfNeccesary();
		notifyAll();
	}

	/**
	 ** Get and remove the next entry from the queue.
	 ** 
	 ** If the queue is empty this method waits until an
	 ** entry is available.
	 **
	 ** @return The Hashtable entry removed from the queue.
	 **/
	private synchronized ListenerEvent dequeue() {
		if (queue.isEmpty()) {
			try {
				if (doListenerUpdateQueueLogging()) {
					Activator.log
							.debug("[ListenerEventQueue] Queue is empty. Waiting 5000 ms");
				}
				wait(5000);
			} catch (InterruptedException ignored) {
			}
		}
		if (queue.isEmpty()) {
			if (doListenerUpdateQueueLogging()) {
				Activator.log
						.debug("[ListenerEventQueue] Queue is still empty. Detaching thread.");
			}
			detachCurrentThread();
			return null;
		} else {
			ListenerEvent u = (ListenerEvent) queue.elementAt(0);
			queue.removeElementAt(0);
			return u;
		}
	}

	void attachNewThreadIfNeccesary() {
		synchronized (threadLock) {
			if (thread == null) {
				if (doListenerUpdateQueueLogging()) {
					Activator.log
							.debug("[ListenerEventQueue] Attaching new thread.");
				}
				thread = new Thread(this);
				thread.setDaemon(true);
				thread.start();
			}
		}
	}

	void detachCurrentThread() {
		synchronized (threadLock) {
			if (doListenerUpdateQueueLogging()) {
				Activator.log
						.debug("[ListenerEventQueue] Detaching thread because queue is empty.");
			}
			thread = null;
		}
	}

	boolean doListenerUpdateQueueLogging() {
		//return Activator.log.doDebug()
		return true;
	}
}
