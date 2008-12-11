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
import org.knopflerfish.service.desktop.*;

public class JServiceView extends JSoftGraphBundle {
  
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

