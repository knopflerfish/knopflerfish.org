package org.knopflerfish.bundle.soap.desktop;

import java.util.*;

public class XSDSchema extends XSDElement {

  Map typeMap = new HashMap();

  public XSDSchema() {
    super("", "");
  }

  public Map getTypeMap() {
    return typeMap;
  }
}
