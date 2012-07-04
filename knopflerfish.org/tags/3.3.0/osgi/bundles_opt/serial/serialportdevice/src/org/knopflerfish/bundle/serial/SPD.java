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

package org.knopflerfish.bundle.serial;

import java.io.*;
import java.util.*;
import javax.comm.*;
import org.osgi.framework.*;
import org.osgi.service.device.*;
import org.knopflerfish.service.serial.*;

class SPD implements SerialPortDevice {
  private static final int SERIAL_WAIT = 1000;
  private static final String[] clazzes=new String[] {
    Device.class.getName(),
    SerialPortDevice.class.getName()
  };

  private ServiceRegistration sreg;
  private boolean dying;
  private CommPortIdentifier cpi;
  private SerialPort sp;

  void start(BundleContext bc, String port,
	     String product, String revision) throws Exception {
    this.sp=sp;

    cpi=CommPortIdentifier.getPortIdentifier(port);
    if(cpi==null) throw new IOException();

    Dictionary props=new Hashtable();
    props.put("DEVICE_CATEGORY",new String[] {"Serial"});
    props.put("DEVICE_DESCRIPTION","Serial device");
    props.put("org.knopflerfish.service.serial.port",port);
    props.put("org.knopflerfish.service.serial.product",product);
    if (revision != null)
      props.put("org.knopflerfish.service.serial.revision",revision);

    if(dying) return;
    sreg=bc.registerService(clazzes,this,props);
    synchronized(this) {
      if(!dying) return;
    }
    close();
  }

  void close() {
    synchronized(this) {
      if(sreg==null) {
	dying=true;
	return;
      }
    }
    sreg.unregister();
    synchronized(this) {
      releaseSerialPort();
      cpi=null;
    }
  }

  public synchronized SerialPort allocateSerialPort() {
    if(cpi!=null && sp==null) {
      try {
	sp=(SerialPort)cpi.open("serial",SERIAL_WAIT);
      } catch(PortInUseException e) {}
      return sp;
    } else {
      return null;
    }
  }

  public synchronized void releaseSerialPort() {
    if(sp!=null) {
      sp.close();
      sp=null;
    }
  }

  public void noDriverFound() {}
}
