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
}
