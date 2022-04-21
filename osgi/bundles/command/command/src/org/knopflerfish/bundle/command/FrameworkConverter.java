/*
 * Copyright (c) 2010-2022, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * - Neither the name of the KNOPFLERFISH project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
