/**
 * Copyright (c) 2001 Gatespace AB. All Rights Reserved.
 *
 * $Header: /cvs/gs/gosg/gatespace_bundles/serialport/serialport/impl_src/Activator.java,v 1.3 2002/04/03 14:06:33 ar Exp $
 * $Revision: 1.3 $
 */

package org.knopflerfish.bundle.serial;

import java.io.*;
import java.util.*;
import javax.comm.*;
import org.osgi.framework.*;
import org.knopflerfish.service.log.LogRef;

public class Activator implements BundleActivator {
  private Config conf;
  private LogRef log;

  public void start(BundleContext bc) throws Exception {
    log = new LogRef(bc);
    conf=new Config(log);
    conf.start(bc);
  }

  public void stop(BundleContext bc) {
    conf.stop();
  }
}
