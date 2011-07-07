package org.knopflerfish.bundle.soap.desktop;

import org.osgi.framework.*;
import javax.swing.*;
import java.util.*;

import org.knopflerfish.service.desktop.*;

public class Activator implements BundleActivator {

  static BundleContext bc;
  static boolean       bVerbose = false; 

  SOAPDisplayer        displayer;

  public void start(BundleContext _bc) {
    
    this.bc = _bc;

    displayer = new SOAPDisplayer();

    Hashtable props = new Hashtable();
    props.put(SwingBundleDisplayer.PROP_NAME,        "SOAP Services");
    props.put(SwingBundleDisplayer.PROP_DESCRIPTION, "Displays and invokes SOAP services");
    props.put(SwingBundleDisplayer.PROP_ISDETAIL,    Boolean.FALSE);

    bc.registerService(SwingBundleDisplayer.class.getName(),
		       displayer,
		       props);

  }

  public void stop(BundleContext bc) {
    this.bc        = null;
    this.displayer = null;
  }

}

