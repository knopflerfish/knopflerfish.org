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

import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

/**
 * Utility class for handling common class loading cases, like wrapping external
 * libraries in the correct context class loader.
 */
public class ClassLoaderUtil {

    /**
     * Run the specified action in a specified ContextClassLoader.
     * 
     * <p>
     * The <tt>doContextClassLoader</tt> sets the current thread's context
     * class loader to the specified <tt>classloader</tt>, calls the action's
     * <tt>run</tt> method, resets the thread's context class loader and
     * finally returns the resulting value.
     * </p>
     * 
     * <p>
     * Example:
     * 
     * <pre>
     *    Object result = ClassLoaderUtil
     *      .doContextClassLoader(Activator.getClass().getClassLoader(),
     *                            new PrivilegedAction() {
     *                              public Object run() {
     *                                // Use external library
     *                                // ...
     *                                return someresult;
     *                              }
     *                            };
     * </pre>
     * 
     * where <tt>Activator</tt> is the bundle activator, or any other class
     * loaded by the bundle class loader.
     * </p>
     * 
     * @param classloader
     *            Class loader to be used as the thread's context class loader.
     * @param action
     *            Action code to run in the specified class loader.<br>
     *            The usage of the <tt>PrivilegedAction</tt> interface is to
     *            avoid creating a new interface with exactly the same methods.
     *            It does <b>not</b> imply that the code is run using
     *            <tt>AccessController.doPrivileged</tt>
     * @return Value returned from the <tt>action.run</tt>
     */
    public static Object doContextClassLoader(ClassLoader classloader,
            PrivilegedAction action) {
        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(classloader);
            Object obj = action.run();
            return obj;
        } finally {
            Thread.currentThread().setContextClassLoader(oldLoader);
        }
    }

    /**
     * Run the specified action in a specified ContextClassLoader.
     * 
     * <p>
     * As above, but accepts and rethrows exceptions from the action.
     * </p>
     * 
     * @param classloader
     *            Class loader to be used as the thread's context class loader.
     * @param action
     *            Action code to run in the specified class loader.<br>
     *            The usage of the <tt>PrivilegedAction</tt> interface is to
     *            avoid creating a new interface with exactly the same methods.
     *            It does <b>not</b> imply that the code is run using
     *            <tt>AccessController.doPrivileged</tt>
     * @return Value returned from the <tt>action.run</tt>
     * 
     * @throws Exception
     *             if <tt>action.run</tt> throws an exception, pass this
     *             upwards
     */
    public static Object doContextClassLoader(ClassLoader classloader,
            PrivilegedExceptionAction action) throws Exception {

        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(classloader);
            Object obj = action.run();
            return obj;
        } finally {
            Thread.currentThread().setContextClassLoader(oldLoader);
        }
    }
}
