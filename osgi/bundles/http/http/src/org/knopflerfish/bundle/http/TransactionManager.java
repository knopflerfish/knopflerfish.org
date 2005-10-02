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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.knopflerfish.service.log.LogRef;

public class TransactionManager extends ThreadGroup {

    // private fields

    private static int managerCount = 0;

    private static int transactionCount = 0;

    private final LogRef log;

    final Registrations registrations;

    final ObjectPool requestPool;

    final ObjectPool responsePool;

    private ObjectPool transactionPool = null;

    // constructors

    public TransactionManager(final LogRef log,
            final Registrations registrations,
            final HttpSessionManager sessionManager) {

        super("HttpServer-TransactionGroup-" + managerCount++);

        this.log = log;
        this.registrations = registrations;

        requestPool = new ObjectPool() {
            protected PoolableObject createPoolableObject() {
                return new RequestImpl(TransactionManager.this.registrations,
                        sessionManager);
            }
        };
        responsePool = new ObjectPool() {
            protected PoolableObject createPoolableObject() {
                return new ResponseImpl();
            }
        };
        transactionPool = new ObjectPool() {
            protected PoolableObject createPoolableObject() {
                return new Transaction(log, registrations, requestPool,
                        responsePool);
            }
        };
    }

    // public methods

    public void startTransaction(final Socket client,
            final HttpConfigWrapper httpConfig) {
        final Transaction transaction = (Transaction) transactionPool.get();
        transaction.init(client, httpConfig);
        new Thread(this, transaction, "HttpServer-Transaction-"
                + transactionCount++).start();
    }

    public void startTransaction(final InputStream is, final OutputStream os,
            final HttpConfigWrapper httpConfig) {
        final Transaction transaction = (Transaction) transactionPool.get();
        transaction.init(is, os, httpConfig);
        new Thread(this, transaction).start();
    }

    // extends ThreadGroup

    public void uncaughtException(Thread thread, Throwable t) {

        if (t instanceof ThreadDeath) {

        } else if (log.doDebug()) {
            log.debug("Internal error", t);
        }
    }

} // TransactionManager
