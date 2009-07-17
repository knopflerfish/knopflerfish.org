package org.knopflerfish.bundle.command;

import java.util.*;
import java.lang.reflect.*;
import org.osgi.service.command.*;
import org.osgi.framework.*;

public class FrameworkConverter implements Converter {
  
  static Class[] CLASSES = new Class[] {
    Bundle.class,
  };
  
  static String[] CLASSES_STRINGS;
  static {
    CLASSES_STRINGS = new String[CLASSES.length];
    for(int i = 0; i < CLASSES.length; i++) {
      CLASSES_STRINGS[i] = CLASSES[i].getName();
    }
  }
  
  public Object convert(Class desiredType, Object in) {
    if(in == null) {
      return null;
    }
    if(desiredType.isAssignableFrom(in.getClass())) {
      return in;
    }
    if(in instanceof Bundle) {
      Bundle b = (Bundle)in;
      if(desiredType == String.class) {
        return "#" + b.getBundleId();
      }
    }
    return null;
  }

  public CharSequence format(Object target, int level, Converter escape) {
    throw new RuntimeException("NYI");
  }
}
