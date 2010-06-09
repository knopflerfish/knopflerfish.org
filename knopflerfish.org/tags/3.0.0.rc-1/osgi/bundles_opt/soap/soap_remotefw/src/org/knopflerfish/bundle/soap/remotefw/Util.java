package org.knopflerfish.bundle.soap.remotefw;

import org.osgi.framework.*;
import org.osgi.util.tracker.*;

import java.util.*;
import org.knopflerfish.service.log.LogRef;
import org.osgi.service.startlevel.*;

import java.lang.reflect.*;

public class Util {

  public static long[] referencesToLong(ServiceReference[] srl) {
    if(srl == null) {
      return new long[0];
    }
    long[] r = new long[srl.length];
    for(int i = 0; i < srl.length; i++) {
      r[i] = ((Long)srl[i].getProperty(Constants.SERVICE_ID)).longValue();
    }
    return r;
  }

  public static String encodeAsString(Object val) {

    if(val == null) {
      return "[null]";
    }

    if(val.getClass().isArray()) {
      StringBuffer sb = new StringBuffer();
      sb.append(val.getClass().getName());
      sb.append("[#" + Array.getLength(val) + "#");
      for(int i = 0; i < Array.getLength(val); i++) {
	Object item = Array.get(val, i);
	sb.append(encodeAsString(item));
	if(i < Array.getLength(val) - 1) {
	  sb.append(",");
	}
      }
      sb.append("#]");
    }

    return "[@" + val.getClass().getName() + "::" + val + "@]";
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

}
