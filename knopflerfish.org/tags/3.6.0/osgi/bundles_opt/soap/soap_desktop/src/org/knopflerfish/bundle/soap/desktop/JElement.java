package org.knopflerfish.bundle.soap.desktop;

import java.util.*;

import java.awt.*;
import javax.swing.*;
import java.util.*;

import org.w3c.dom.*;

public class JElement extends JPanel {
  XSDElement xsd;
  public boolean    bArray;

  java.util.List children = new ArrayList();

  private JElement() {
  }

  public JElement(XSDElement xsd) {
    this(null, xsd);
  }

  public JElement(LayoutManager layout, XSDElement xsd) {
    super(layout);
    this.xsd = xsd;
  }
  
  public XSDElement getXSD() {
    return xsd;
  }

  public java.util.List getXSDChildren() {
    return children;
  }

  public void addXSD(XSDElement el) {
    children.add(el);
  }

  public Element toElement() {

    return null;
  }
}
