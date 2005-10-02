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

package org.knopflerfish.bundle.http;

import java.util.Enumeration;
import java.util.Vector;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionContext;

public class HttpSessionImpl implements HttpSession {

    // private fields

    private boolean invalid = true;

    private String id = null;

    private long creationTime = -1;

    private long accessedTime = -1;

    private long lastAccessedTime = -1;

    private int maxInactiveInterval = -1;

    private final Attributes attributes = new Attributes();

    // public methods

    public void init(int count) {

        creationTime = System.currentTimeMillis();
        accessedTime = creationTime;

        id = "session." + count + "." + creationTime;

        invalid = false;
    }

    public void join() {

        lastAccessedTime = accessedTime;
        accessedTime = System.currentTimeMillis();
    }

    public boolean isExpired() {
        return invalid
                || accessedTime + maxInactiveInterval * 1000 < System
                        .currentTimeMillis();
    }

    public void destroy() {

        invalid = true;

        id = null;
        creationTime = -1;
        accessedTime = -1;
        lastAccessedTime = -1;
        maxInactiveInterval = -1;

        attributes.removeAll();
    }

    // implements HttpSession

    public String getId() {
        return id;
    }

    public boolean isNew() {

        if (invalid)
            throw new IllegalStateException("Invalid session");

        return lastAccessedTime == -1;
    }

    public long getCreationTime() {

        if (invalid)
            throw new IllegalStateException("Invalid session");

        return creationTime;
    }

    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    public void setMaxInactiveInterval(int maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
    }

    public void invalidate() {

        if (invalid)
            throw new IllegalStateException("Invalid session");

        Enumeration names = getAttributeNames();
        while (names.hasMoreElements())
            removeAttribute((String) names.nextElement());

        invalid = true;
    }

    public Object getAttribute(String name) {

        if (invalid)
            throw new IllegalStateException("Invalid session");

        return attributes.getAttribute(name);
    }

    public synchronized Enumeration getAttributeNames() {

        if (invalid)
            throw new IllegalStateException("Invalid session");

        return attributes.getAttributeNames();
    }

    public synchronized void setAttribute(String name, Object value) {

        if (invalid)
            throw new IllegalStateException("Invalid session");

        attributes.setAttribute(name, value);

        if (value instanceof HttpSessionBindingListener) {
            HttpSessionBindingListener listener = (HttpSessionBindingListener) value;
            listener.valueBound(new HttpSessionBindingEvent(this, name));
        }
    }

    public synchronized void removeAttribute(String name) {

        if (invalid)
            throw new IllegalStateException("Invalid session");

        Object value = attributes.removeAttribute(name);

        if (value != null && value instanceof HttpSessionBindingListener) {
            HttpSessionBindingListener listener = (HttpSessionBindingListener) value;
            listener.valueUnbound(new HttpSessionBindingEvent(this, name));
        }
    }

    public synchronized Object getValue(String name) {
        return getAttribute(name); // deprecated
    }

    public synchronized String[] getValueNames() { // deprecated

        Vector v = new Vector();

        Enumeration e = getAttributeNames();
        while (e.hasMoreElements())
            v.addElement(e.nextElement());
        String[] names = new String[v.size()];
        v.copyInto(names);

        return names;
    }

    public void putValue(String name, Object value) {
        setAttribute(name, value); // deprecated
    }

    public void removeValue(String name) {
        removeAttribute(name); // deprecated
    }

    public HttpSessionContext getSessionContext() {
        return null; // deprecated
    }

} // HttpSessionImpl
