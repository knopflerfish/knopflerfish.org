/*
 * Copyright (c) 2003-2005, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 *
 * - Neither the name of the KNOPFLERFISH project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.knopflerfish.bundle.cm;

import java.util.Vector;

import org.osgi.service.cm.ConfigurationException;

/**
 * * This class is responsible for dispatching configurations * to
 * ManagedService(Factories). * * It is also responsible for calling
 * <code>ConfigurationPlugins</code>. * *
 * 
 * @author Per Gustafson *
 * @version 1.0
 */

final class UpdateQueue implements Runnable {
    /**
     * * The PluginManager to use.
     */

    private PluginManager pm;

    /**
     * * The thread running this object.
     */

    private Thread thread;

    private final Object threadLock = new Object();

    /**
     * * The queue of updates.
     */

    private Vector queue = new Vector();

    /**
     * * Construct an UpdateQueue given a * BundleContext. * *
     * 
     * @param tracker
     *            The BundleContext to use.
     */

    UpdateQueue(PluginManager pm) {
        this.pm = pm;
    }

    /**
     * * Overide of Thread.run().
     */

    public void run() {
        while (true) {
            Update update = dequeue();
            if (update == null) {
                return;
            }
            try {
                update.doUpdate(pm);
            } catch (ConfigurationException ce) {
                Activator.log.error("[CM] Error in configuration for "
                        + update.pid, ce);
            } catch (Throwable t) {
                Activator.log.error("[CM] Error while updating " + update.pid,
                        t);
            }
        }
    }

    /**
     * * Add an entry to the end of the queue. * *
     * 
     * @param update
     *            The Update to add to the queue. * *
     * @throws java.lang.Exception
     *             If given a null argument.
     */

    public synchronized void enqueue(Update update) {
        if (update == null) {
            return;
        }
        queue.addElement(update);
        attachNewThreadIfNeccesary();
        notifyAll();
    }

    /**
     * * Get and remove the next entry from the queue. * * If the queue is empty
     * this method waits until an * entry is available. * *
     * 
     * @return The Hashtable entry removed from the queue.
     */

    private synchronized Update dequeue() {
        if (queue.isEmpty()) {
            try {
                wait(5000);
            } catch (InterruptedException ignored) {
            }
        }
        if (queue.isEmpty()) {
            detachCurrentThread();
            return null;
        }
        Update u = (Update) queue.elementAt(0);
        queue.removeElementAt(0);
        return u;
    }

    void attachNewThreadIfNeccesary() {
        synchronized (threadLock) {
            if (thread == null) {
                thread = new Thread(this);
                thread.setDaemon(true);
                thread.start();
            }
        }
    }

    void detachCurrentThread() {
        synchronized (threadLock) {
            thread = null;
        }
    }
}
