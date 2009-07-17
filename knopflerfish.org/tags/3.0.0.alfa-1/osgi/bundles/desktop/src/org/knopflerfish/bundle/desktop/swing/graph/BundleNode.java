package org.knopflerfish.bundle.desktop.swing.graph;


import java.util.*;
import java.awt.geom.Point2D;
import java.awt.Color;
import org.osgi.framework.*;
import org.knopflerfish.bundle.desktop.swing.Util;
import org.knopflerfish.bundle.desktop.swing.Activator;

public class BundleNode extends DefaultNode  {
  Bundle b;

  public BundleNode(Bundle b, int depth, String id) {
    super("", depth, id);
    this.b     = b;
  }

  public Bundle getBundle() {
    return b;
  }
  
  public String toString() {
    return 
      "#" + b.getBundleId() + " " + Util.getBundleName(b);    
  }
  
}
