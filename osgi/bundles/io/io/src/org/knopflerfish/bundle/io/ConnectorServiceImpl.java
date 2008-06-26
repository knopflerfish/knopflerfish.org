/*
 * Copyright (c) 2006-2008, KNOPFLERFISH project
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

package org.knopflerfish.bundle.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.ArrayList;

import javax.microedition.io.Connector;
import javax.microedition.io.Connection;
import javax.microedition.io.InputConnection;
import javax.microedition.io.OutputConnection;
import javax.microedition.io.ConnectionNotFoundException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.InvalidSyntaxException;

import org.osgi.service.io.ConnectorService;
import org.osgi.service.io.ConnectionFactory;

class ConnectorServiceImpl implements ConnectorService {

  private static BundleContext bc = null;

  public ConnectorServiceImpl(BundleContext bc) {
    this.bc = bc;
  }

  public Connection open(String uri) throws IOException {
    return open(uri, ConnectorService.READ_WRITE);
  }

  public Connection open(String uri, int mode) throws IOException {
    return open(uri, mode, false);
  }

  public Connection open(String uri, int mode, boolean timeouts)
    throws IOException
  {
    if (mode != Connector.READ &&
        mode != Connector.WRITE &&
        mode != Connector.READ_WRITE)
      throw new IllegalArgumentException("Variable mode has an invalid value");

    if (null==uri || uri.length()==0)
      throw new IllegalArgumentException("URI must not be null or empty");

    int schemeSeparator = uri.indexOf(':');

    if (schemeSeparator < 0) {
      throw new IllegalArgumentException("Not a valid URI");
    }

    String scheme = uri.substring(0, schemeSeparator);

    ConnectionFactory factory = getFactory(scheme);
    Connection retval = null;

    if (factory != null) {
      retval = factory.createConnection(uri, mode, timeouts);

    } else {

      // fall back to Connector.
      try {
        retval = Connector.open(uri, mode, timeouts);
      } catch (Exception e) {
        throw new ConnectionNotFoundException();
      }

    }

    if (retval == null)
      throw new ConnectionNotFoundException();
    else
      return retval;
  }


  private ConnectionFactory getFactory(String scheme) {
    ServiceReference[] refs = null;

    try {
      refs =
        bc.getServiceReferences(ConnectionFactory.class.getName(), null);
    } catch (InvalidSyntaxException e) {
      // this should not be happening
    }

    if (refs == null) {
      return null;
    }

    ArrayList list = new ArrayList(); // matching services

    for (int i = 0; i < refs.length; i++) {
      String[] tmp = (String[])refs[i].getProperty(ConnectionFactory.IO_SCHEME);

      for (int o = 0; o < tmp.length; o++) {

        if (scheme.equalsIgnoreCase(tmp[o])) {
          list.add(refs[i]);
          break;
        }
      }
    }

    if (list.isEmpty()) {
      return null;
    }

    ServiceReference bestRef = (ServiceReference)list.get(0);

    if (list.size() == 1)
      return (ConnectionFactory)bc.getService(bestRef);

    int bestRanking = getRanking(bestRef);
    int rank;
    ServiceReference ref;

    for (int i = 1; i < list.size(); i++) {
      ref = (ServiceReference)list.get(i);
      rank = getRanking(ref);

      if (rank > bestRanking) { // by highest rank, then lowest service id.
        bestRanking = rank;
        bestRef = ref;

      } else {
        if (rank == bestRanking) {

          Long l1 = (Long)ref.getProperty(Constants.SERVICE_ID);
          Long l2 = (Long)bestRef.getProperty(Constants.SERVICE_ID);

          if (l1.compareTo(l2) < 0) {
            bestRef = ref;
          }
        }
      }
    }
    return (ConnectionFactory)bc.getService(bestRef);
  }

  /* Returns 0 (default value) if there is not rank for
     the specificed service or if it has an invalid rank.
  */
  private int getRanking(ServiceReference ref) {

    Object rank =
      (Object)ref.getProperty(Constants.SERVICE_RANKING);

    if (rank == null)
      return 0;

    if (rank instanceof Integer)
      return ((Integer)rank).intValue();
    else
      return 0;

  }

  public DataInputStream openDataInputStream(String name) throws IOException {
    Connection con = open(name, ConnectorService.READ, false);

    if (con instanceof InputConnection) {
      DataInputStream stream = ((InputConnection)con).openDataInputStream();
      con.close();
      return stream;
    }
    con.close();
    throw new IOException("Given scheme does not support input");
  }


  public DataOutputStream openDataOutputStream(String name) throws IOException {
    Connection con = open(name, ConnectorService.WRITE, false);

    if (con instanceof OutputConnection) {
      DataOutputStream stream = ((OutputConnection)con).openDataOutputStream();
      con.close();
      return stream;
    }

    con.close();
    throw new IOException("Given scheme does not support output");
  }


  public InputStream openInputStream(String name) throws IOException {

    Connection con = open(name, ConnectorService.READ, false);

    if (con instanceof InputConnection) {
      InputStream stream = ((InputConnection)con).openInputStream();
      con.close();
      return stream;
    }

    con.close();
    throw new IOException("Given scheme does not support input");
  }


  public OutputStream openOutputStream(String name) throws IOException {
    Connection con = open(name, ConnectorService.WRITE, false);

    if (con instanceof OutputConnection) {
      OutputStream stream = ((OutputConnection)con).openOutputStream();
      con.close();
      return stream;
    }

    con.close();
    throw new IOException("Given scheme does not support output");
  }
}
