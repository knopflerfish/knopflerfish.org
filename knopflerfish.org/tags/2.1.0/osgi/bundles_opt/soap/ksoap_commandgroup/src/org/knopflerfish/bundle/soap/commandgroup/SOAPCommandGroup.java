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
package org.knopflerfish.bundle.soap.commandgroup;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.util.tracker.*;

import org.knopflerfish.service.log.LogRef;
import org.knopflerfish.util.*;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import org.knopflerfish.service.console.CommandGroupAdapter;
import org.knopflerfish.service.console.Session;

public class SOAPCommandGroup extends CommandGroupAdapter {

  private static final String NULL_STR = "null";

  boolean  bDebug   = "true".equals(System.getProperty("org.knopflerfish.soap.remotefw.client.debug", "false"));

  public SOAPCommandGroup() {
    super("ksoap", "kSOAP test commands");
  }

  //
  // call command
  //

  public final static String USAGE_CALL = "<url> <method_name> [<params>] ...";
  public final static String [] HELP_CALL = new String [] {
    "Call a SOAP method.",
    "<url>         ",
    "<method_name> ",
    "<params>      Each parameter to the call are represented by two values:",
    "              type  - The following types are suported: string, int, long, null.",
    "              value - The value as a string that can be converted to the right type.",
    "Example: call http://localhost:8091/soap/services/soapadmin getPublishedServiceNames",
    "         call http://localhost:8080/soap/services/OSGiFramework startBundle long 18",
    "         call http://localhost:8080/soap/services/my_own take_a_string_and_a_null string foo null null",
  };

  public int cmdCall(Dictionary opts, Reader in, PrintWriter out, Session session) {
    String url = (String) opts.get("url");
    String methodName = (String) opts.get("method_name");
    String[] inparams = (String[]) opts.get("params");
    if (inparams == null) {
      inparams = new String[0];
    }

    Object[] params = new Object[inparams.length / 2];
    int pi = 0;
    for (int ini = 0; ini<inparams.length; ini++) {
      String type = inparams[ini];
      String value = inparams[++ini];
      if ("int".equals(type)) {
        params[pi++] = new Integer(value);
      } else if ("long".equals(type)) {
        params[pi++] = new Long(value);
      } else if ("string".equals(type)) {
        params[pi++] = value;
      } else if ("null".equals(type)) {
        params[pi++] = null;
      } else {
        //TODO
      }
    }

    HttpTransportSE httpTransport = new HttpTransportSE(url);

    SoapSerializationEnvelope soapEnvelope = new SoapSerializationEnvelope(SoapEnvelope.VER10);
    httpTransport.debug = bDebug;
    soapEnvelope.env = "http://www.w3.org/2001/12/soap-envelope";

    try {
      SoapObject rpc = new SoapObject("http://www.w3.org/2001/12/soap-envelope", methodName);
      for (int i=0; i<params.length; i++) {
        if(bDebug) System.out.println("doCall   param " + i + " = " + params[i]);
        rpc.addProperty("item"+i, params[i]);
      }
      soapEnvelope.bodyOut = rpc;

      if(bDebug) System.out.println("doCall " + methodName + "(" + toDisplay(params) + ")");
      httpTransport.call(methodName, soapEnvelope);

      out.println(soapEnvelope.getResult());

      return 1;
    } catch (Exception e) {
      e.printStackTrace();
    }

    return 0;
  }

  private Vector soapObjectToVector(SoapObject sobj) {
    Vector vector = new Vector();
    for (int i = 0; i < sobj.getPropertyCount(); i++) {
      Object obj = sobj.getProperty(i);
      if (obj instanceof SoapObject) {
        obj = soapObjectToVector((SoapObject) obj);
      }
      vector.addElement(obj);
    }
    return vector;
  }

  static Map vectorToMap(Vector vector) {
    Map result = new HashMap();
    for (Enumeration enumIsReserved = vector.elements(); enumIsReserved.hasMoreElements();) {
      Object key = enumIsReserved.nextElement();
      if (!enumIsReserved.hasMoreElements()) break;
      Object val = enumIsReserved.nextElement();
      if (key instanceof SoapPrimitive) key = key.toString();
      if (val instanceof SoapPrimitive) {
        String name = ((SoapPrimitive)val).getName();
        if ("long".equals(name)) {
          val = new Long(val.toString());
        } else if ("int".equals (name)) {
          val = new Integer(val.toString());
        } else if ("boolean".equals (name)) {
          val = new Boolean(val.toString());
        } else {
          val = val.toString();
        }
      }
      result.put(key, val);
    }
    return result;
  }

  static long[] toLongArray(Object obj) {
    if(obj == null) {
      return null;
    }
    long[] la;
    if (obj instanceof SoapObject) {
      SoapObject so = (SoapObject) obj;
      la = new long[so.getPropertyCount()];
      for(int i = 0; i < la.length; i++) {
        la[i] = new Long(so.getProperty(i).toString()).longValue();
      }
    } else {
      la = new long[Array.getLength(obj)];
      for(int i = 0; i < la.length; i++) {
        la[i] = ((Long)Array.get(obj, i)).longValue();
      }
    }

    return la;
  }

  static String toString(Object obj) {
    return (obj == null ? null : obj.toString());
  }

  public static Object toDisplay(Object val) {
    if(val != null) {
      if(NULL_STR.equals(val)) {
        return "null";
      }
      if(val instanceof String) {
        return "\"" + val + "\"";
      }
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

