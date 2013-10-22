package org.knopflerfish.bundle.desktop.swing;


import org.knopflerfish.bundle.desktop.swing.graph.BundleServiceNode;
import org.knopflerfish.bundle.desktop.swing.graph.Node;
import org.knopflerfish.service.desktop.BundleSelectionModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class JServiceView extends JSoftGraphBundle {

  private static final long serialVersionUID = 1L;

  public JServiceView(GraphDisplayer.JMainBundles jmb,
                      BundleContext bc,
                      Bundle b,
                      BundleSelectionModel bundleSelModel) {
    super(jmb, bc, b, bundleSelModel);

    currentNode = makeRootNode();
    setLabel(Strings.get("str_services"));
  }

  public Node makeRootNode() {
    Node node = new BundleServiceNode(b, 0);
    return node;
  }
}
