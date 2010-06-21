package org.knopflerfish.bundle.soap.desktop;

import javax.wsdl.*;
import javax.wsdl.factory.*;
import javax.wsdl.xml.*;

import javax.wsdl.extensions.*;
import javax.xml.namespace.QName;
import javax.wsdl.Input;
import javax.wsdl.Output;

import java.net.*;
import java.io.*;
import java.util.*;
import org.w3c.dom.*;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;

public class Util {

  public static java.util.List getPartNames(Message msg) {
    java.util.List list = new ArrayList();

    Map partMap = msg.getParts();
    for(Iterator partIt = partMap.keySet().iterator(); partIt.hasNext();) {
      Object key  = partIt.next();
      Part   part = (Part)partMap.get(key);
      
      String name = part.getName();
      
      list.add(name);
    }

    return list;
  }

  public static String getTypeString(Part part) {
    QName  elQ   = part.getElementName();
    QName  typeQ = part.getTypeName();
    
    String type;
    
    if(typeQ != null) {
      type = typeQ.getLocalPart();
    } else {
      if(elQ != null) {
	type = elQ.getLocalPart();	     
      } else {
	type = "string";
	//	throw new IllegalArgumentException("No type in part=" + part);
      }
    }

    return type;
  }

  public static Object toDisplay(Object val) {
    if(val != null) {
      if(val.getClass().isArray()) {
	StringBuffer sb = new StringBuffer();
	sb.append("[");
	for(int i = 0; i < Array.getLength(val); i++) {
	  sb.append(toDisplay(Array.get(val, i)));
	  if(i < Array.getLength(val) - 1) {
	    sb.append(",");
	  }
	}
	sb.append("]");
	return sb.toString();
      }
    }

    return val;
  }

  public static void main(String[] argv) {
    Object val = new String[][] {
      new String[] {"apa",  "bepa"},
      new String[] {"cepa", "depa"},
    };

    System.out.println(toDisplay(val));
  }

}
