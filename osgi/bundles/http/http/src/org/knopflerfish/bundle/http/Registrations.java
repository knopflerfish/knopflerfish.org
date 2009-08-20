/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Vector;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

public class Registrations {

    // private fields

    private final Dictionary registrations = new Hashtable();

    private final Vector servlets = new Vector();

    // private methods

    private String fixAlias(String alias) {

        if (alias.equals("/"))
            return "";
        return alias;
    }

    // public methods

    public void addServlet(Servlet servlet)
        throws ServletException
    {

        if (servlets.contains(servlet))
            throw new ServletException("Servlet already registered");

        servlets.addElement(servlet);
    }

    public void removeServlet(Servlet servlet) {
        servlets.removeElement(servlet);
    }

    public void put(String alias, Registration registration) {
        registrations.put(fixAlias(alias), registration);
    }

    public Registration remove(String alias) {
        return (Registration) registrations.remove(fixAlias(alias));
    }

    public Registration get(String alias) {
        return (Registration) registrations.get(fixAlias(alias));
    }

    public RequestDispatcherImpl getRequestDispatcher(String uri)
    {
        String alias = uri;
        while (true) {
            Registration registration = (Registration) registrations.get(alias);
            if (registration != null) {
                RequestDispatcherImpl dispatcher = registration
                        .getRequestDispatcher(uri);
                if (dispatcher != null)
                    return dispatcher;
            }
            int index = alias.lastIndexOf('/');
            if (index == -1) {
                return null;
            }
            alias = alias.substring(0, index);
        }
    }

} // Registrations
