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

import java.net.Socket;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Constructor;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.cm.ConfigurationException;
import org.knopflerfish.service.log.LogRef;


public class TransactionManager extends ThreadGroup {

  // private fields

  private static int managerCount = 0;
  private static int transactionCount = 0;

  private final HttpConfig httpConfig;
  private final LogRef log;

  private final Registrations registrations;

  private final ObjectPool requestPool;
  private final ObjectPool responsePool;

  private ObjectPool transactionPool = null;
  private Boolean isSecure = null;


  // constructors

  public TransactionManager(final HttpConfig httpConfig,
                            final LogRef log,
                            final Registrations registrations,
                            final HttpSessionManager sessionManager) {

    super("HttpServer-TransactionGroup-" + managerCount++);

    this.httpConfig = httpConfig;
    this.log = log;
    this.registrations = registrations;

    requestPool = new ObjectPool() {
      protected PoolableObject createPoolableObject() {
        return new RequestImpl(TransactionManager.this.httpConfig,
                               TransactionManager.this.registrations,
                               sessionManager);
      }
    };
    responsePool = new ObjectPool() {
      protected PoolableObject createPoolableObject() {
        return new ResponseImpl(TransactionManager.this.httpConfig);
      }
    };
  }


  // public methods

  public void startTransaction(final Socket client) {

    final Transaction transaction = (Transaction) transactionPool.get();
    transaction.init(client);
    new Thread(this,
               transaction,
               "HttpServer-Transaction-" + transactionCount++).start();
  }

  public void startTransaction(final InputStream is, final OutputStream os) {

    final Transaction transaction = (Transaction) transactionPool.get();
    transaction.init(is, os);
    new Thread(this, transaction).start();
  }

  public void updated() throws ConfigurationException {

    final Boolean isSecure = new Boolean(httpConfig.isSecure());
    if (!isSecure.equals(this.isSecure)) {
      this.isSecure = isSecure;
/*
 //TODO Those transactions are not really involved in creating sockets... is there a difference in https? 
        try {
          Class clazz = getClass().getClassLoader().loadClass("org.knopflerfish.bundle.http.SecureTransactionPool");
          Constructor constructor = clazz.getConstructor(new Class[] { HttpConfig.class, LogRef.class, Registrations.class, ObjectPool.class, ObjectPool.class });
          transactionPool = (ObjectPool) constructor.newInstance(new Object[] { httpConfig, log, registrations, requestPool, responsePool });
        } catch (ClassNotFoundException cnfe) {
          throw new ConfigurationException(HttpConfig.SECURE_KEY, cnfe.toString());
        } catch (NoSuchMethodException nsme) {
          throw new ConfigurationException(HttpConfig.SECURE_KEY, nsme.toString());
        } catch (InstantiationException ie) {
          throw new ConfigurationException(HttpConfig.SECURE_KEY, ie.toString());
        } catch (IllegalAccessException iae) {
          throw new ConfigurationException(HttpConfig.SECURE_KEY, iae.toString());
        } catch (InvocationTargetException ite) {
          throw new ConfigurationException(HttpConfig.SECURE_KEY, ite.getTargetException().toString());
        }
      } else {
*/      
        transactionPool = new ObjectPool() {
          protected PoolableObject createPoolableObject() {
            return new Transaction(httpConfig,
                                   log,
                                   registrations,
                                   requestPool,
                                   responsePool);
          }
        };
//      }
    }
  }


  // extends ThreadGroup

  public void uncaughtException(Thread thread, Throwable t) {

    if (t instanceof ThreadDeath)
      ;
    else
      if (log.doDebug()) log.debug("Internal error", t);
  }

} // TransactionManager
