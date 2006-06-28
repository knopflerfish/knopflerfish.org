/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
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
package org.knopflerfish.bundle.soap.remotefw;

import org.osgi.framework.*;
import org.osgi.util.tracker.*;

import java.util.*;
import org.knopflerfish.service.log.LogRef;
import org.osgi.service.startlevel.*;

import java.lang.reflect.*;

public class Util {

  public static final String LOC_PROT  = "remotefw:";
  public static final String FILE_PROT  = "file:";
  public static final String B64_START = "B64(";
  public static final String B64_END = "):";

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
