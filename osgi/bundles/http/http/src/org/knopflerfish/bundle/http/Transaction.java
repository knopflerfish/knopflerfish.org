/*
 * Copyright (c) 2003-2011,2015 KNOPFLERFISH project
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.knopflerfish.service.log.LogRef;

public class Transaction
  implements Runnable
{
  // private fields

  private HttpConfigWrapper httpConfig;

  private final Registrations registrations;

  private InputStream is = null;

  private OutputStream os = null;

  // protected fields

  protected LogRef log;

  protected Socket client = null;
  
  private TransactionManager transManager;
  
  private RequestImpl requestImpl;
  
  private ResponseImpl responseImpl;
  
  private int requestCount = 0;
  
  String name = null;

  // constructors

  public Transaction(final TransactionManager transManager,
                     int count,
                     final LogRef log, 
                     final Registrations registrations)
  {
    this.transManager = transManager;
    this.log = log;
    this.registrations = registrations;
    this.name = "Transaction-" + count;
  }

  // public methods

  public void init(final Socket client, final HttpConfigWrapper httpConfig)
  {
    this.httpConfig = httpConfig;
    this.client = client;
  }

 
  // implements Runnable

  public void run()
  {
    InetAddress remoteAddress = null;
    InetAddress localAddress = null;

    int localPort = 0;
    int remotePort = 0;
    boolean reservedKeepAlive = false;
    
    long startTime = System.currentTimeMillis();
    
    try {
      if (client != null) {
        client.setSoTimeout(1000 * httpConfig.getConnectionTimeout());
        is = client.getInputStream();
        os = client.getOutputStream();
        localAddress = client.getLocalAddress();
        localPort = client.getLocalPort();
        remoteAddress = client.getInetAddress();
        remotePort = client.getPort();
      }
      requestImpl.init(is, localAddress, localPort, remoteAddress, remotePort, httpConfig);
      responseImpl.init(os, requestImpl, httpConfig);
      
      while (true) {
        try {
          requestImpl.handle();
          responseImpl.handle();
          requestCount++;
          
          // Determine if keep-alive is requested and if request can be granted,
          // given the thread keep alive policy
          if (requestImpl.getKeepAlive()) {
            if (!reservedKeepAlive)
              responseImpl.setKeepAlive(reservedKeepAlive = transManager.reserveKeepAlive());
          } 
          else if (reservedKeepAlive) {
            transManager.releaseKeepAlive();
            reservedKeepAlive = false;
          }          

          final String method = requestImpl.getMethod();
          final String uri = requestImpl.getRequestURI();
          if (Activator.log.doDebug())
            Activator.log.debug(Thread.currentThread().getName() + " - requesting: " + uri);
          
          final RequestDispatcherImpl dispatcher =
            registrations.getRequestDispatcher(uri);

          if ("TRACE".equals(method) && !httpConfig.isTraceEnabled()) {
            responseImpl.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
          } else if (dispatcher == null) {
            responseImpl.setKeepAlive(false);
            responseImpl.sendError(HttpServletResponse.SC_NOT_FOUND);
          } else {
            // HACK SMA Expect: 100-Continue
            final String expect = requestImpl.getHeader(HeaderBase.EXPECT_HEADER_KEY);
            if (expect != null) {
              // case-insensitive
              if (HeaderBase.EXPECT_100_CONTINUE_VALUE
                  .compareToIgnoreCase(expect) == 0) {
                responseImpl.sendContinue();
                // create a new response
                responseImpl.init(os, requestImpl, httpConfig);
              } else {
                responseImpl.sendError(HttpServletResponse.SC_EXPECTATION_FAILED);
              }
            }
            // TODO to be fully compliant the header should
            // be forwarded in case of http proxy

            // END HACK
            try {
              requestImpl.setServletPath(dispatcher.getServletPath());
              dispatcher.forward(requestImpl, responseImpl);
            } catch (final ServletException se) {
              responseImpl.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
          }

        } catch (final HttpException he) {
          responseImpl.setKeepAlive(false);
          responseImpl.sendError(he.getCode(), he.getMessage());
          
        }

        responseImpl.commit();
        // Activator.log.debug(Thread.currentThread().getName() + " - response.KeepAlive: " + responseImpl.getKeepAlive());
        
        if (responseImpl.getKeepAlive()) {
          // Activator.log.info(Thread.currentThread().getName() + " - we are trying to keep-alive");
          
          final InputStream is = requestImpl.getRawInputStream();
          // Activator.log.info(Thread.currentThread().getName() + " markSupported()=" + is.markSupported()); 
          if (is != null && is.markSupported()) {
            is.mark(4);
            int i = is.read();
            if (i == -1) {
              // Activator.log.info(Thread.currentThread().getName() + " - EOF on keep alive connection, giving up");
              break;
            }
            //if (is.read() != '\r' || is.read() != '\n') {
            if (i != '\r' || i != '\n') {
              is.reset();
            }
            else {
            }
          }
          requestImpl.reset(true);
          responseImpl.resetHard();
        } else {
          break;
        }

      }

    } catch (final SocketException se) {
      // ignore: client closed socket
      if (log.doDebug())
        Activator.log.debug(Thread.currentThread().getName() + "SocketException=" + se);
    } catch (final InterruptedIOException iioe) {
      // ignore: keep alive socket timeout
      if (log.doDebug())  
        Activator.log.debug(Thread.currentThread().getName() + "InterrupterIOException=" + iioe);
    } catch (final IOException ioe) {
      // ignore: broken pipe
      if (log.doDebug())
        Activator.log.error(Thread.currentThread().getName() + " - IOException:" + ioe, ioe);
    } catch (final ThreadDeath td) {
      Activator.log.info(Thread.currentThread().getName() + "ThreadDeath=" + td);
      throw td;
    } catch (final Throwable t) {
      if (log.doError()) {
        log.error("Internal error: " + t, t);
      }
      try {
        responseImpl.init(os, requestImpl, httpConfig);
        responseImpl.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                           "Internal error: " + t);
      } catch (final IOException ignore) {
      }
    } finally {
      long duration = System.currentTimeMillis() - startTime;
      if (Activator.log.doDebug())
        Activator.log.debug(Thread.currentThread().getName() + " - finally(), count=" + requestCount + " duration=" + duration);
      
      if (reservedKeepAlive)
        transManager.releaseKeepAlive();
      
      if (is != null) {
        try {
          is.close();
        } catch (final Exception ignore) {
        }
      }
      if (os != null) {
        try {
          os.close();
        } catch (final Exception ignore) {
        }
      }
      if (client != null) {
        try {
          client.close();
        } catch (final Exception ignore) {
        }
      }
    }
  }

  public void init(RequestImpl requestImpl, ResponseImpl responseImpl)
  {
    this.requestImpl = requestImpl;
    this.responseImpl = responseImpl;
    
  }

  public int getRequestCount()
  {
    return requestCount;
  }
  
  public void closeConnection() {
    if (client != null)
      try {
        client.close();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

} // Transaction
