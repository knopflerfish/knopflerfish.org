/*
 * Copyright (c) 2006, KNOPFLERFISH project
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
 
/*
  ConnectionFactories should close all currently open 
  connections when they are stopped*. This class enables
  one to easily keep track of Connections that a factory
  has created.
  
  Whenever a connection is created the Connection should 
  register itself using the registerConnection and whenever 
  a Connection is closed it should call unregisterConnection.
  When a factory is unloaded from the system the function 
  closeAll will be called, then all registered Connection
  will be closed and unregistered.
   
  *: according to r4-cmpn section 109.4.1, p. 224
  */

package org.knopflerfish.bundle.connectors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.microedition.io.Connection;

import org.osgi.framework.BundleContext;
import org.osgi.service.io.ConnectionFactory;

public abstract class BaseConnectionFactory implements ConnectionFactory
{
  private final List list = new ArrayList();

  // something like new String[]{"datagram", ..}
  public abstract String[] getSupportedSchemes();

  public void registerFactory(BundleContext bc) {
    Hashtable properties = new Hashtable();
    properties.put(ConnectionFactory.IO_SCHEME, getSupportedSchemes());
    bc.registerService(ConnectionFactory.class.getName(), this, properties);
  }

  public void registerConnection(Connection con) {
    synchronized (list) {
      list.add(con);
    }
  }

  public void unregisterConnection(Connection con) {
    synchronized (list) {
      list.remove(con);
    }
  }

  public void unregisterFactory(BundleContext bc) {
    Iterator copyIterator;
    synchronized (list) {
      // Copy list to avoid race condition
      copyIterator = new ArrayList(list).iterator();

      // Just a precaution, if this factory is used again.
      list.clear();
    }

    while (copyIterator.hasNext()) {
      try {
        ((Connection) copyIterator.next()).close();
      } catch (IOException e) { /* ignore */ }
    }
  }
}
