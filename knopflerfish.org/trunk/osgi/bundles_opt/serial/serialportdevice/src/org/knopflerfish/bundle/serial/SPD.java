/**
 * Copyright (c) 2001 Gatespace AB. All Rights Reserved.
 *
 * $Header: /cvs/gs/gosg/gatespace_bundles/serialport/serialport/impl_src/SPD.java,v 1.3 2001/05/13 14:58:34 sparud Exp $
 * $Revision: 1.3 $
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
