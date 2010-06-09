/*
 * Copyright (c) 2008, KNOPFLERFISH project
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


package org.knopflerfish.bundle.desktop.prefs;

import java.util.*;
import java.util.prefs.*;


// import org.osgi.service.prefs.*;

/**

 */
public class OSGiPreferences 
  extends AbstractPreferences
  implements ExtPreferences
{
  org.osgi.service.prefs.Preferences target;
  
  public OSGiPreferences(AbstractPreferences parent, 
                         org.osgi.service.prefs.Preferences target) {
    this(parent, target, null);
  }

  public OSGiPreferences(AbstractPreferences parent, 
                         org.osgi.service.prefs.Preferences target,
                         String name) {
    super(parent, name != null ? name : target.name());
    this.target = target;
  }


  // AbstractPreferences implementation
  protected String[] childrenNamesSpi() throws BackingStoreException {
    try {
      return target.childrenNames();
    } catch (org.osgi.service.prefs.BackingStoreException e) {
      throw new BackingStoreException(e);
    }
  }

  // AbstractPreferences implementation

  Map children = new HashMap();

  protected AbstractPreferences  childSpi(String name) {
    synchronized(children) {
      OSGiPreferences child = (OSGiPreferences)children.get(name);
      if(child == null) {
        child = new OSGiPreferences(this, target.node(name));
        children.put(name, child);
      }
      return child;
    }
  }
  
  // AbstractPreferences implementation
  protected void  flushSpi() throws BackingStoreException {
    try {
      target.flush();
    } catch (org.osgi.service.prefs.BackingStoreException e) {
      throw new BackingStoreException(e);
    }
  }


  // AbstractPreferences implementation
  protected String getSpi(String key) {
    return target.get(key, null);
  }

  // AbstractPreferences implementation
  protected String[] keysSpi() throws BackingStoreException {
    try {
      return target.keys(); 
    } catch (org.osgi.service.prefs.BackingStoreException e) {
      throw new BackingStoreException(e);
    }
  }
  
  // AbstractPreferences implementation
  protected void putSpi(String key, String value) {
    target.put(key, value);
  }

  // AbstractPreferences implementation
  protected void 	removeNodeSpi() throws BackingStoreException {
    try {
      Preferences parent = parent();
      if(parent instanceof OSGiPreferences) {      
        ((OSGiPreferences)parent).clearCachedChild(name());
      }
      target.removeNode();
    } catch (org.osgi.service.prefs.BackingStoreException e) {
      throw new BackingStoreException(e);
    }  
  }

  public void clearCachedChild(String name) {
    synchronized(children) {
      children.remove(name);
    }
  }


  // AbstractPreferences implementation
  protected void 	removeSpi(String key) {
    target.remove(key);
  }

  // AbstractPreferences implementation
  protected void 	syncSpi() throws BackingStoreException {
    try {
      target.sync();
    } catch (org.osgi.service.prefs.BackingStoreException e) {
      throw new BackingStoreException(e);
    }
  }

  public String[] getExtPropNames(String key) {
    return null;
  }

  public String getProperty(String key, String propName, String defValue) {
    return defValue;
  }

  public void setProperty(String key, String propName, String val) {
  }


  public boolean equals(Object obj) {
    if(null == obj) {
      return false;
    }
    if(!(obj instanceof Preferences)) {
      return false;
    }
    
    Preferences prefs = (Preferences)obj;

    return absolutePath().equals(prefs.absolutePath());
  }

  public int hashCode() {
    return absolutePath().hashCode();
  }

}
