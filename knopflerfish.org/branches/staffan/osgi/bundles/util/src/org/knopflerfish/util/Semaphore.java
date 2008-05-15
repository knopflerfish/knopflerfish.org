/*
 * Copyright (c) 2003, KNOPFLERFISH project
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

package org.knopflerfish.util;

/**
 * * The <code>Semaphore</code> class handles synchronization and waiting for
 * values. *
 * 
 * @author Johan Agat and Anders Rimen
 */
public class Semaphore {
    private Object value = null;

    private boolean closed = false;

    /**
     * Waits up to <code>timeout</code> milliseconds for this Semaphore to
     * receive a value.
     * 
     * @return The value of the Semaphore or null if this Semaphore has been
     *         closed or if the specified timeout has expired.
     */
    public synchronized Object get(long timeout) {
        long until = System.currentTimeMillis() + timeout;
        while (!closed && value == null) {
            try {
                long t = until - System.currentTimeMillis();
                if (t >= 0)
                    wait(t);
                else
                    return null;
            } catch (InterruptedException ignore) {
            }
        }
        return value;
    }

    /**
     * Sets the value of this Semaphore. This will cause all blocked calls to
     * get() to return the value. If set() is called several times with a short
     * or no delay between the calls, the exact value returned by a given
     * blocked call to get() is not deterministic.
     * 
     */
    public synchronized void set(Object v) {
        if (closed)
            return;
        value = v;
        // setCount++;
        notifyAll();
    }

    public synchronized void reset() {
        value = null;
    }

    public synchronized void close() {
        value = null;
        closed = true;
        notifyAll();
    }

}
