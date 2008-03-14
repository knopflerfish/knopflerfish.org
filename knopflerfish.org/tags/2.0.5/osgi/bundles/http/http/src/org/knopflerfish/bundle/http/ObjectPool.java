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

public abstract class ObjectPool {

    // private fields

    private final PoolableObject[] pooledObjects;

    private int count = 0;

    // constructors

    public ObjectPool() {
        this(10);
    }

    public ObjectPool(final int maxInstances) {
        this(maxInstances, 0);
    }

    public ObjectPool(final int maxInstances, final int createInstances) {

        pooledObjects = new PoolableObject[maxInstances];

        count = 0;
        while (count < createInstances)
            put(createPoolableObject());
    }

    // protected abstract methods

    protected abstract PoolableObject createPoolableObject();

    // public methods

    public PoolableObject get() {

        PoolableObject object;
        synchronized (pooledObjects) {
            if (count == 0) {
                object = createPoolableObject();
            } else {
                object = pooledObjects[--count];
            }
        }
        object.init();

        return object;
    }

    public void put(PoolableObject object) {

        object.destroy();
        synchronized (pooledObjects) {
            if (pooledObjects.length > count)
                pooledObjects[count++] = object;
        }
    }

} // ObjectPool
