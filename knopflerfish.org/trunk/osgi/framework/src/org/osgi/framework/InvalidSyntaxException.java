/*
 * Copyright (c) The Open Services Gateway Initiative (2000-2001).
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

package org.osgi.framework;

/**
 * A Framework exception.
 *
 * <p>An <tt>InvalidSyntaxException</tt> object indicates that a filter
 * string parameter has an invalid syntax and cannot be parsed.
 *
 * <p> See {@link Filter} for a description of the filter string syntax.
 *
 * @version $Revision: 1.1.1.1 $
 * @author Open Services Gateway Initiative
 */

public class InvalidSyntaxException extends Exception
{
    /**
     * The invalid filter string.
     */
    private transient String filter;

    /**
     * Creates an exception of type <tt>InvalidSyntaxException</tt>. 
     *
     * <p>This method creates an <tt>InvalidSyntaxException</tt> object with 
     * the specified message and the filter string which generated the exception.
     *
     * @param msg The message.
     * @param filter The invalid filter string.
     */
    public InvalidSyntaxException(String msg, String filter)
    {
        super(msg);
        this.filter = filter;
    }

    /**
     * Returns the filter string that generated the <tt>InvalidSyntaxException</tt> object.
     *
     * @return The invalid filter string.
     * @see BundleContext#getServiceReferences
     * @see BundleContext#addServiceListener
     */
    public String getFilter()
    {
        return(filter);
    }

  public String toString() {
    return super.toString() + ", filter=" + filter;
  }
}


