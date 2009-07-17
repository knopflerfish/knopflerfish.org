package org.knopflerfish.bundle.soap.desktop;

import java.util.*;
import java.io.*;

public class PrintVisitor implements XSDVisitor {
  PrintWriter out;
  
  public PrintVisitor() {
    this(new PrintWriter(System.out));
  }

  public PrintVisitor(PrintWriter out) {
    this.out = out;
  }

  public void visit(XSDObj obj, Object data, int level) {
    HashMap typeMap = (HashMap)data;

    String p = "org.knopflerfish.bundle.soap.desktop.";

    StringBuffer sb = new StringBuffer();
    
    if(obj instanceof XSDElement) {
      XSDElement el = (XSDElement)obj;
      
      sb.append(obj.getClass().getName().substring(p.length()));
      sb.append(" ");
      sb.append(el.getName());

      String type = el.getType();

      if(type.startsWith("typens:")) {
	XSDElement ref =(XSDElement)typeMap.get(type.substring(7));
	if(ref != null) {
	  StringWriter sw = new StringWriter();
	  PrintVisitor pv = new PrintVisitor(new PrintWriter(sw));
	  XSDWalker.doVisit(ref, pv, typeMap, level + 1);
	  sb.append("\n");
	  sb.append(sw.toString());
	} else {
	  sb.append(", unresolved " + type);
	}
      } else {
	if(!(obj instanceof XSDComplexType)) {
	  if(!"".equals(el.getType())) {
	    sb.append(", ");
	    sb.append(el.getType());
	  }
	}
      }
    } else {
      sb.append(obj.getClass().getName().substring(p.length()));
    }
    
    out.println(indent(level) + sb.toString());
  }

  String indent(int n) {
    StringBuffer sb = new StringBuffer();
    
    while(n --> 0) {
      sb.append(" ");
    }
    return sb.toString();
  }

}
