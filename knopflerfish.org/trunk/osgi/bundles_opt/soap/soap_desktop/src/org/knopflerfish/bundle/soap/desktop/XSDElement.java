package org.knopflerfish.bundle.soap.desktop;

import java.util.*;

public class XSDElement extends XSDObj implements XSDParent {
  String type;

  Class  clazz = null;

  boolean bArray = false;

  public XSDElement(String name, String type) {
    super(name);

    if(type.startsWith("xsd:")) {
      type = type.substring(4);
    }

    if(type.endsWith("[]")) {
      bArray = true;
      type = type.substring(0, type.length() - 2);
    }

    this.type = type;

    if("string".equals(type)) {
      clazz = String.class;
    } else if("boolean".equals(type)) {
      clazz = Boolean.class;
    } else if("float".equals(type)) {
      clazz = Float.class;
    } else if("int".equals(type)) {
      clazz = Integer.class;
    } else if("long".equals(type)) {
      clazz = Long.class;
    } else if("double".equals(type)) {
      clazz = Double.class;
    } else if("datetime".equals(type)) {
      clazz = Date.class;
    }
  }

  List objs = new ArrayList();

  public void add(XSDObj obj) {
    objs.add(obj);
  }

  public Iterator getChildren() {
    return objs.iterator();
  }

  public String getType() {
    return type;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();

    sb.append(getClass().getName() + "[" + 
	     "name=" + getName() +
	     ", type=" + getType() + 
	     ", array=" + isArray());

    
    if(objs.size() > 0) {
      sb.append(", {");
      for(Iterator it = objs.iterator(); it.hasNext(); ) {
	XSDObj obj = (XSDObj)it.next();
	sb.append(obj.toString());
	if(it.hasNext()) {
	  sb.append(", ");
	}
      }
      sb.append("}");
    }

    sb.append("]");
    return sb.toString();
  }
  
  public boolean isArray() {
    return bArray;
  }
}
