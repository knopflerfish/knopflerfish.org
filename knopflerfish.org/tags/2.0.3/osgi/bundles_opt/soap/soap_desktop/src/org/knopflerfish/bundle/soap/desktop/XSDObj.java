package org.knopflerfish.bundle.soap.desktop;

import java.util.*;

public class XSDObj {
  String name;
  
  public XSDObj(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
  
  public String toString() {
    return getClass().getName() + "[" + 
      "name=" + getName() + 
      "]";
  }

}
