/*
 * Copyright (c) 2008-2022, KNOPFLERFISH project
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

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.osgi.service.prefs.PreferencesService;

/**

 */
public class OSGiUsersPreferences 
  extends AbstractPreferences
  implements ExtPreferences
{
  protected PreferencesService prefsService;

  private final Map<String, OSGiPreferences> children = new HashMap<>();

  public OSGiUsersPreferences(AbstractPreferences parent,
                              PreferencesService prefsService) {
    super(parent, "");
    this.prefsService = prefsService;
  }

  @Override
  protected String[] childrenNamesSpi() {
    return prefsService.getUsers();
  }

  @Override
  protected AbstractPreferences  childSpi(String name) {
    synchronized(children) {
      OSGiPreferences child = children.get(name);
      if(child == null) {
        org.osgi.service.prefs.Preferences node = 
          prefsService.getUserPreferences(name);
        
        child = new OSGiPreferences(this, node, name);
        children.put(name, child);
      }
      return child;
    }
  }
  
  @Override
  protected void  flushSpi() {
    // NOOP
  }


  @Override
  protected String getSpi(String key) {
    return null;
  }

  final static String[] EMPTY = new String[0];

  @Override
  protected String[] keysSpi() {
    return EMPTY;
  }
  
  @Override
  protected void putSpi(String key, String value) {
    throw new RuntimeException("This node " + absolutePath()  + " cannot have keys");
  }

  @Override
  protected void 	removeNodeSpi() throws BackingStoreException {
    throw new BackingStoreException("This node " + absolutePath() + " cannot be removed");
  }
  
  @Override
  protected void 	removeSpi(String key) {
    // NOOP
  }

  @Override
  protected void 	syncSpi() {
    // NOOP
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
