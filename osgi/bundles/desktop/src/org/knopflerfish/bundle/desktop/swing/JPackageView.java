package org.knopflerfish.bundle.desktop.swing;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.font.TextAttribute;
import java.util.*;
import java.awt.geom.AffineTransform;
import javax.swing.border.*;
import org.osgi.framework.*;
import java.awt.geom.Point2D;
import org.knopflerfish.bundle.desktop.swing.graph.*;
import java.awt.geom.*;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;
import org.osgi.util.tracker.ServiceTracker;
import org.knopflerfish.service.desktop.*;

public class JPackageView extends JSoftGraphBundle {

  ServiceTracker pkgTracker;

  public JPackageView(GraphDisplayer.JMainBundles jmb, BundleContext bc, Bundle b, BundleSelectionModel bundleSelModel) {
    super(jmb, bc, b, bundleSelModel);

    pkgTracker = new ServiceTracker(bc, PackageAdmin.class.getName(), null);
    pkgTracker.open();

    setMaxDepth(8);
    currentNode = makeRootNode();
    setLabel(Strings.get("str_packages"));
  }

  public Node makeRootNode() {
    if(Activator.desktop != null && Activator.desktop.alive) {
      Node node = new PackageNode(Activator.desktop.pm, b, 0, "#" + b.getBundleId());
      return node;
    } else {
      return new EmptyNode("", 0, "");
    }
  }
  
  void bundleChanged() {
  }

  public void close() {        
    super.close();
    pkgTracker.close();
  }  
}

