/*
 * Copyright (c) 2004, KNOPFLERFISH project
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

package org.knopflerfish.bundle.framework_test;

import org.osgi.framework.*;
import org.knopflerfish.service.framework_test.*;
import java.util.*;
import java.io.*;

public class RegListenThread implements Runnable, org.osgi.framework.ServiceListener {
  BundleContext       bc;
  PrintStream         out;
  Thread              t1;
  ServiceRegistration sr;
  Hashtable           props;
  int                 ID     = 2;
  boolean             status = false;

  public RegListenThread(BundleContext bc, PrintStream out) throws Exception {
    this.bc  = bc;
    this.out = out;
    bc.addServiceListener(this);
  }

  public synchronized void stop() {
    // out.println("Shutdow of thread " + String.valueOf(ID));
    bc.removeServiceListener(this);
  }

  public boolean getStatus() {
    return status;
  }

  public void run() {
    serviceChanged (new ServiceEvent(org.osgi.framework.ServiceEvent.MODIFIED,
          bc.getServiceReference(RegServThread.class.getName())));

    // out.println("Exiting run() of thread " + String.valueOf(ID));
    status = true;
  }

  public synchronized void serviceChanged(ServiceEvent se) {
    ServiceReference srlocal = se.getServiceReference();
    try {
      Thread.sleep(500); // sleep for longer time that the registering thread sleeps
                         // as we need to be hanging here to create 
                         // a lock for the registering thread
    }
    catch (Exception ex) {
      out.println("### Frame test bundle :FRAME210A exception, in RegListenThread");
      ex.printStackTrace(out);
      status = false;
    }

    bc.getService(srlocal);
  }
}
