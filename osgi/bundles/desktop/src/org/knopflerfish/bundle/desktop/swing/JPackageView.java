package org.knopflerfish.bundle.desktop.swing;


import org.knopflerfish.bundle.desktop.swing.graph.EmptyNode;
import org.knopflerfish.bundle.desktop.swing.graph.Node;
import org.knopflerfish.bundle.desktop.swing.graph.PackageNode;
import org.knopflerfish.service.desktop.BundleSelectionModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class JPackageView extends JSoftGraphBundle {

  private static final long serialVersionUID = 1L;
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

