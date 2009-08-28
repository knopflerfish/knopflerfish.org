/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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

package org.osgi.service.cm;

/**
 * An <tt>Exception</tt> class to inform the Configuration Admin service of
 * problems with configuration data.
 * 
 * @version $Revision: 1.1.1.1 $
 * @author Open Services Gateway Initiative
 */
public class ConfigurationException extends Exception {

    private static final long serialVersionUID = 1L;

    String property;

    String reason;

    /**
     * Create a <tt>ConfigurationException</tt> object.
     * 
     * @param property
     *            name of the property that caused the problem, <tt>null</tt>
     *            if no specific property was the cause
     * @param reason
     *            reason for failure
     */
    public ConfigurationException(String property, String reason) {
        super(property + " : " + reason);
        this.property = property;
        this.reason = reason;
    }

    /**
     * Return the property name that caused the failure or null.
     * 
     * @return name of property or null if no specific property caused the
     *         problem
     */
    public String getProperty() {
        return property;
    }

    /**
     * Return the reason for this exception.
     * 
     * @return reason of the failure
     */
    public String getReason() {
        return reason;
    }

}
