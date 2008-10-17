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
 * An object with a timeout.
 * 
 * <p>
 * A CachedObject has a timeout period - during that period the <code>get</code>
 * method will return the stored object, afterwards <code>get</code> will
 * return <code>null</code>
 * </p>
 * 
 * @see CacheMap
 */
public class CachedObject {

    /**
     * Default timeout period in milliseconds. Value is 60 seconds.
     */
    public final static long DEFAULT_TIMEOUT = 60 * 1000;

    // Timeout in milliseconds for this cached object
    private long timeout;

    // Creation time of this cached object.
    private long creationTime;

    // Actual object stored in cache
    private Object object;

    /**
     * Equivalent to <code>CachedObject(null)</code>.
     * 
     * @see CacheMap
     */
    public CachedObject() {
        this(null);
    }

    /**
     * Equivalent to
     * <code>CachedObject(object, CachedObject.DEFAULT_TIMEOUT)</code>
     * 
     * @see #DEFAULT_TIMEOUT
     */
    public CachedObject(Object object) {
        this(object, DEFAULT_TIMEOUT);
    }

    /**
     * Create a cached object from an object and a specified timeout.
     * 
     * @param object
     *            Object to cache
     * @param timeout
     *            period in milliseconds.
     * @see #DEFAULT_TIMEOUT
     */
    public CachedObject(Object object, long timeout) {
        this.timeout = timeout;
        set(object);
    }

    /**
     * Set the cache's object and restore its creation time.
     * 
     * @param object
     *            Object to cache
     */
    public void set(Object object) {
        this.object = object;
        creationTime = System.currentTimeMillis();
    }

    /**
     * Get the cached object.
     * 
     * @return The cached object before its timeout period, <code>null</code>
     *         afterwards.
     */
    public Object get() {

        long now = System.currentTimeMillis();
        if (now - creationTime > timeout) {
            flush();
        }

        return object;
    }

    /**
     * Clear the stored object. After this call, <code>get</code> will
     * 
     */
    public void flush() {
        object = null;
    }

    /**
     * Print the cached object as "&lt;object's string value&gt;:&lt;remaining
     * time in milliseconds&gt;"
     */
    public String toString() {
        return object.toString() + ":"
                + (System.currentTimeMillis() - creationTime);
    }

} // CachedObject
