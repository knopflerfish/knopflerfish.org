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
      for (Enumeration enum = vector.elements(); enum.hasMoreElements(); keyi++) {
        String key = enum.nextElement().toString().toLowerCase();
        if (!enum.hasMoreElements()) break;
        Object val = enum.nextElement();
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
}
