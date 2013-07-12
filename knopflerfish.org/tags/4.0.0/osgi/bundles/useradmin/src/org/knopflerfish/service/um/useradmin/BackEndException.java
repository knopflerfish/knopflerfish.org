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

package org.knopflerfish.service.um.useradmin;

/**
 * BackEndException is the base class for exceptions thrown from BackEndControl.
 */
public class BackEndException extends Exception {
    private static final long serialVersionUID = 1L;

    protected Throwable nested;

    /**
     * Public constructor.
     * 
     * @param msg
     *            Message for this exception.
     * @param nested
     *            A Throwable that caused this exception to be thrown.
     */
    public BackEndException(String msg, Throwable nested) {
        super(msg);
        this.nested = nested;
    }

    /**
     * Public constructor.
     * 
     * @param msg
     *            Message for this exception.
     */
    public BackEndException(String msg) {
        this(msg, null);
    }

    /**
     * Returns the nested throwable of this BackEndException.
     * 
     * @return a Throwable object or null.
     */
    public Throwable getNestedException() {
        return nested;
    }

    /**
     * Returns a short description of this throwable object.
     * 
     * @return a string representation of this <code>BackEndException</code>.
     */
    public String toString() {
        return (super.toString())
                + ((nested != null) ? (" , Nested: " + nested.toString()) : "");
    }

}
