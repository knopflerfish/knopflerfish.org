package org.knopflerfish.bundle.soap.desktop;

import java.util.*;

public class XSDWalker {
  static void doVisit(XSDObj obj, XSDVisitor v, Object data, int level) {
    v.visit(obj, data, level);
    
    if(obj instanceof XSDParent) {
      XSDParent p = (XSDParent)obj;
      for(Iterator it = p.getChildren(); it.hasNext();) {
	doVisit((XSDObj)it.next(), v, data, level + 1);
      }
    }
  }
}
