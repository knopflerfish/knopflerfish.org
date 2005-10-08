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
package org.knopflerfish.bundle.soap.remotefw.client;

import org.osgi.framework.*;
import org.osgi.util.tracker.*;

import java.util.*;
import org.knopflerfish.service.log.LogRef;
import org.osgi.service.startlevel.*;

import org.knopflerfish.service.soap.remotefw.*;

import java.io.*;
import java.net.*;

import org.knopflerfish.bundle.soap.remotefw.*;
import org.knopflerfish.service.remotefw.*;


public class RemoteFrameworkImpl implements RemoteFramework {

  Map clients = new HashMap();

  public RemoteFrameworkImpl() {
  }

  public BundleContext connect(String host) {
    try {
      RemoteFWClient remoteClient = new RemoteFWClient();
      remoteClient.open(host);

      BundleContext bc = remoteClient.getBundleContext();
      clients.put(bc, remoteClient);
      return bc;
    } catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Failed to connect to host=" + host);
    }
  }

  public void disconnect(BundleContext bc) {
    RemoteFWClient remoteClient = (RemoteFWClient)clients.get(bc);
    if(remoteClient != null) {
      remoteClient.close();
      clients.remove(bc);
    } else {
      System.err.println("No remote bc=" + bc);
    }
  }

  public Map getSystemProperties(BundleContext bc) {
    RemoteFWClient remoteClient = (RemoteFWClient)clients.get(bc);
    if(remoteClient != null) {
      return remoteClient.vectorToMap(remoteClient.getSystemProperties());
    } else {
      throw new IllegalArgumentException("No client connected to " + bc);
    }
  }
}
