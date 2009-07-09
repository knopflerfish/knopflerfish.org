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
package org.knopflerfish.bundle.soap.remotefw.client;

import java.io.*;
import java.net.*;
import java.util.*;

import org.osgi.framework.*;
import org.osgi.service.startlevel.*;
import org.osgi.util.tracker.*;

import org.ksoap2.serialization.SoapPrimitive;

import org.knopflerfish.service.log.LogRef;

import org.knopflerfish.service.soap.remotefw.*;

public class ServiceReferenceImpl implements ServiceReference {

  long       sid;
  BundleImpl bundle;
  String[]   keys;
  RemoteFWClient fw;

  Hashtable props = new Hashtable();

  ServiceReferenceImpl(RemoteFWClient fw, BundleImpl bundle, long sid) {
    this.fw = fw;
    this.bundle = bundle;
    this.sid    = sid;
    update();
  }

  void update() {
    synchronized(props) {
      Vector vector = bundle.fw.getServiceProperties(sid);

      props.clear();
      keys = new String[vector.size() / 2];

      int keyi = 0;
      for (Enumeration enumIsReserved = vector.elements(); enumIsReserved.hasMoreElements(); keyi++) {
        String key = enumIsReserved.nextElement().toString().toLowerCase();
        if (!enumIsReserved.hasMoreElements()) break;
        Object val = enumIsReserved.nextElement();
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
        } else if (val instanceof Vector) {
          String[] array = new String[((Vector) val).size()];
          //String[] array = ((Vector) val).toArray(new String[((Vector) val).size()]);
          for (int arrayi=0; arrayi<array.length; arrayi++) {
            array[arrayi] = ((Vector) val).elementAt(arrayi).toString();
          }
          val = array;
        }

        props.put(key, val);
        keys[keyi] = key;
      }
    }
  }

  public Bundle getBundle() {
    return bundle;
  }

  public Object getProperty(String key) {
    if (key == null) return "TODO";
    return props.get(key.toLowerCase());
  }

  public String[] getPropertyKeys() {
    return keys;
  }

  public Bundle[] getUsingBundles() {
    long[] bids = fw.getUsingBundles(sid);
    if (bids == null) {
      return new Bundle[0];
    }
    Bundle[] bundles = new Bundle[bids.length];
    for (int i=0; i<bids.length; i++) {
      bundles[i] = new BundleImpl(fw, bids[i]);
    }
    return bundles;
  }

  public String toString() {
    return "ServiceReferenceImpl[" +
      "service.id=" + sid +
      ", objectclass=" + RemoteFWClient.toDisplay(getProperty("objectclass")) +
      "]";

  }

public boolean isAssignableTo(Bundle bundle, String className) {
	// TODO Auto-generated method stub
	return false;
}
}
