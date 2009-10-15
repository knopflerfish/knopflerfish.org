package org.knopflerfish.bundle.desktop.swing.graph;


import java.util.*;
import java.awt.geom.Point2D;
import org.osgi.framework.*;
import org.knopflerfish.bundle.desktop.swing.Util;
import org.knopflerfish.bundle.desktop.swing.Activator;

public class ServiceLink extends DefaultLink {
  ServiceReference sr;

  public ServiceLink(ServiceReference sr, 
                     Node from,
                     Node to,
                     int depth,
                     String id) {
    super(from, to, depth, id, id);
    this.sr = sr;
  }

  public String toString() {
    return 
      "#" + sr.getProperty("service.id") + 
      " " + Util.getClassNames(sr);
    
  }

  public String toStringLong() {
    return 
      "id=" + id + ", depth=" + depth + "\n" + 
      "#" + sr.getProperty("service.id") + " " + Util.getClassNames(sr) + "\n" + 
      "from " + from.toString() + "\n" +  
      "to " + to.toString();
  }
}
