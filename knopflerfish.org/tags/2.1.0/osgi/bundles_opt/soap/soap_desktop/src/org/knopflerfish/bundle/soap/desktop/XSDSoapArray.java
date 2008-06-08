package org.knopflerfish.bundle.soap.desktop;

import java.util.*;

public class XSDSoapArray extends XSDElement  {
  public XSDSoapArray(String name, String type) {
    super(name, type);
  }
  
  public String toString() {
    return "XSDSoapArray[" + 
      "type=" + getType() + 
      "]";
  }
}

