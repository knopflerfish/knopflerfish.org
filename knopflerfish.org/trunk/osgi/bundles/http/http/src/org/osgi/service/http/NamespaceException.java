/*
 * $Header: /home/wistrand/cvs/knopflerfish.org/osgi/bundles/http/http/src/org/osgi/service/http/NamespaceException.java,v 1.1.1.1 2004/03/05 20:35:10 wistrand Exp $
 *
 * Copyright (c) The Open Services Gateway Initiative (2000).
 * All Rights Reserved.
 *
 * Implementation of certain elements of the Open Services Gateway Initiative
 * (OSGI) Specification may be subject to third party intellectual property
 * rights, including without limitation, patent rights (such a third party may
 * or may not be a member of OSGi). OSGi is not responsible and shall not be
 * held responsible in any manner for identifying or failing to identify any or
 * all such third party intellectual property rights.
 *
 * This document and the information contained herein are provided on an "AS
 * IS" basis and OSGI DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO ANY WARRANTY THAT THE USE OF THE INFORMATION HEREIN WILL
 * NOT INFRINGE ANY RIGHTS AND ANY IMPLIED WARRANTIES OF MERCHANTABILITY OR
 * FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT WILL OSGI BE LIABLE FOR ANY
 * LOSS OF PROFITS, LOSS OF BUSINESS, LOSS OF USE OF DATA, INTERRUPTION OF
 * BUSINESS, OR FOR DIRECT, INDIRECT, SPECIAL OR EXEMPLARY, INCIDENTIAL,
 * PUNITIVE OR CONSEQUENTIAL DAMAGES OF ANY KIND IN CONNECTION WITH THIS
 * DOCUMENT OR THE INFORMATION CONTAINED HEREIN, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH LOSS OR DAMAGE.
 *
 * All Company, brand and product names may be trademarks that are the sole
 * property of their respective owners. All rights reserved.
 */

package org.osgi.service.http;

/**
 * A NamespaceException is thrown to indicate an error with the caller's request
 * to register a servlet or resources into the URI namespace of the Http
 * Service. This exception indicates that the requested alias already is in use.
 * 
 * @version $Revision: 1.1.1.1 $
 * @author Open Services Gateway Initiative
 */
public class NamespaceException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Nested exception
     */
    private transient Throwable exception;

    /**
     * Construct a <tt>NamespaceException</tt> object with a detail message.
     * 
     * @param message
     *            the detail message
     */
    public NamespaceException(String message) {
        super(message);
        exception = null;
    }

    /**
     * Construct a <tt>NamespaceException</tt> object with a detail message
     * and a nested exception.
     * 
     * @param message
     *            the detail message
     * @param exception
     *            the nested exception
     */
    public NamespaceException(String message, Throwable exception) {
        super(message);
        this.exception = exception;
    }

    /**
     * Returns the nested exception.
     * 
     * @return the nested exception or <code>null</code> if there is no nested
     *         exception.
     */
    public Throwable getException() {
        return (exception);
    }
}
