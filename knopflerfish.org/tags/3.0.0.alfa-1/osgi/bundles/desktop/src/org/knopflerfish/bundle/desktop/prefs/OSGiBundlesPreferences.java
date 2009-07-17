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

import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;
import java.util.prefs.*;


import javax.swing.*;
import java.awt.event.*;
import org.knopflerfish.bundle.desktop.swing.Activator;
import org.knopflerfish.bundle.desktop.swing.Util;
import org.osgi.framework.*;

public class OSGiBundlesPreferences 
  extends MountedPreferences
{
  static final String[]              EMPTY_STRINGS = new String[0];
  protected Bundle[] bl;

  // String -> OSGiBundlesPreference
  protected Map      childNodes = new LinkedHashMap();
  protected String[] childNames;

  public OSGiBundlesPreferences(Bundle[] bl) {
    super();

    this.bl = bl;

    childNames = new String[bl.length];

    for(int i = 0; i < bl.length; i++) {
      String                name = getBundleNodeName(bl[i]);
      OSGiBundlePreferences node = new OSGiBundlePreferences(bl[i]);
      node.open();

      childNodes.put(name, node);
      childNames[i] = name;
      mount(node, name);
    }
  }

  public void close() {
    for(Iterator it = childNodes.keySet().iterator(); it.hasNext(); ) {
      String                name = (String)it.next();
      OSGiBundlePreferences node = (OSGiBundlePreferences)childNodes.get(name);
      node.close();
    }
  }

  public Bundle[] getBundles() {
    return bl;
  }

  public String getBundleNodeName(Bundle b) {
    return Util.getBundleName(b) + " (#" + b.getBundleId() + ")";
  }
}
