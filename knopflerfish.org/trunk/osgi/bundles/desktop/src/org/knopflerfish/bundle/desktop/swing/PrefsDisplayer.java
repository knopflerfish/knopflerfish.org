/*
 * Copyright (c) 2008-2009, KNOPFLERFISH project
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

package org.knopflerfish.bundle.desktop.swing;

import java.util.*;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.*;
import java.awt.BorderLayout;
import java.util.prefs.*;
import org.osgi.service.prefs.PreferencesService;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import org.knopflerfish.bundle.desktop.prefs.*;
import javax.swing.tree.*;

public class PrefsDisplayer extends DefaultSwingBundleDisplayer {

  ServiceTracker psTracker;

  public PrefsDisplayer(BundleContext bc) {
    super(bc, "Prefs", "Show preferences", true);

    // We're not interested in bundle events, nor in service events
    bUseListeners = false;
  }

  public void open() {
    super.open();

    psTracker = new ServiceTracker(bc,
                                   PreferencesService.class.getName(),
                                   null) {
        public Object addingService(ServiceReference sr) {
          Object obj = super.addingService(sr);
          reinit();
          return obj;
        }
        public void removedService(ServiceReference sr, Object service) {
          reinit();
          super.removedService(sr, service);
        }
      };
    psTracker.open();
  }


  public void close() {
    for(Iterator it = components.iterator(); it.hasNext(); ) {
      JPrefs comp = (JPrefs)it.next();
      comp.close();
    }
    psTracker.close();
    super.close();
  }

  public JComponent newJComponent() {
    JPrefs comp = new JPrefs();
    return comp;
  }

  void reinit() {
    SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          for(Iterator it = components.iterator(); it.hasNext(); ) {
            JPrefs comp = (JPrefs)it.next();
            comp.init();
          }
        }
      });
  }

  public void valueChangedLazy(long  bid) {
    Bundle[] bl = Activator.desktop.getSelectedBundles();

    for(Iterator it = components.iterator(); it.hasNext(); ) {
      JPrefs comp = (JPrefs)it.next();
      comp.valueChanged(bl);
    }
  }

  static int count = 00;

  class JPrefs extends JPanel {
    MountedPreferences rootNode;
    OSGiBundlesPreferences bundlesNode;
    JPrefsEditor editor;
    MountedPreferences jvmNode;


    JPrefs() {
      super(new BorderLayout());

      init();
    }

    void init() {
      if(editor != null) {
        remove(editor);
        editor = null;
      }

      if(bundlesNode != null) {
        bundlesNode.close();
        bundlesNode = null;
      }

      editor = new JPrefsEditor();

      if(Activator.desktop != null) {
        Bundle[] bl = Activator.desktop.getSelectedBundles();
        if(bl != null) {
          mountBundles(bl);
        }
      }

      editor.setEditable(true);
      editor.getJPrefsTree().setRootVisible(false);

      add(editor, BorderLayout.CENTER);
    }

    void valueChanged(final Bundle[] bl) {
      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            mountBundles(bl);
          }
        });
    }

    String BUNDLES_NAME = "OSGi Bundle Prefs";
    String JVM_NAME     = "Java Util Prefs";

    void mountBundles(Bundle[] bl) {
      if(bundlesNode != null) {
        if(bl.length == 0) {
          return;
        }
        Bundle[] bl0 = bundlesNode.getBundles();
        if(bl.length == bl0.length) {
          boolean bSame = true;
          for(int i = 0; i < bl.length; i++) {
            if(bl[i].getBundleId() != bl0[i].getBundleId()) {
              bSame = false;
            }
          }
          if(bSame) {
            return;
          }
        }
      }

      if(bundlesNode != null) {
        bundlesNode.close();
        bundlesNode = null;
      }

      rootNode = new MountedPreferences();

      Preferences sys  = Preferences.systemRoot();
      Preferences user = Preferences.userRoot();

      jvmNode = new MountedPreferences();
      jvmNode.mount((AbstractPreferences)user, "user");
      jvmNode.mount((AbstractPreferences)sys, "sys");

      rootNode.mount(jvmNode, JVM_NAME);

      boolean hasPS = psTracker.getService() != null;

      if(hasPS) {
        bundlesNode =
          new OSGiBundlesPreferences(bl != null ? bl : (new Bundle[0]));


        rootNode.mount(bundlesNode, BUNDLES_NAME);
      }

      editor.setPreferences(rootNode);

      if(hasPS) {
        editor.getJPrefsTree().searchAndExpand(BUNDLES_NAME, 3);
      }
      editor.getJPrefsTree().searchAndExpand(JVM_NAME, 3);

      editor.setPreferences(rootNode);

    }

    void close() {
      //
    }
  }
}
