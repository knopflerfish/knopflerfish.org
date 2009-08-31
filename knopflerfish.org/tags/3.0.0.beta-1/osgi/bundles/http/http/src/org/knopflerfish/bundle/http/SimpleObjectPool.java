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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

public class SimpleObjectPool extends ObjectPool {

    // private fields

    private Constructor constructor;

    // constructors

    public SimpleObjectPool(final Class clazz) {

        super();

        init(clazz);
    }

    public SimpleObjectPool(final Class clazz, final int maxInstances) {

        super(maxInstances);

        init(clazz);
    }

    public SimpleObjectPool(final Class clazz, final int maxInstances,
            final int createInstances) {

        super(maxInstances, createInstances);

        init(clazz);
    }

    // private methods

    private void init(final Class clazz) {

        if (!clazz.isAssignableFrom(PoolableObject.class))
            throw new IllegalArgumentException("Class must implement "
                    + PoolableObject.class.getName());
        try {
            constructor = clazz.getConstructor(null);
        } catch (NoSuchMethodException nsme) {
            throw new IllegalArgumentException(
                    "Class must have public default constructor");
        }
        if (!Modifier.isPublic(constructor.getModifiers()))
            throw new IllegalArgumentException(
                    "Class must have public default constructor");
    }

    // extends ObjectPool

    protected PoolableObject createPoolableObject() {

        try {
            return (PoolableObject) constructor.newInstance(null);
        } catch (InstantiationException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        } catch (InvocationTargetException e) {
            return null;
        }
    }

} // SimpleObjectPool
