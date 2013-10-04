package org.knopflerfish.bundle.repository.xml;

import java.util.HashMap;
import java.util.Map;

import org.osgi.resource.Resource;

public class Data {
  String namespace;
  Map<String, String> directives = new HashMap<String, String>();
  Map<String, Object> attributes = new HashMap<String, Object>();
  Resource resource;
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[namespace=" + namespace + "]\n");
    sb.append("[directives=" + directives + "]\n");
    sb.append("[attributes=" + attributes + "]\n");
    return sb.toString();
    
  }
}
