/*
 * Copyright (c) 2003-2020, KNOPFLERFISH project
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

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import org.knopflerfish.service.desktop.BundleSelectionListener;
import org.knopflerfish.service.desktop.BundleSelectionModel;
import org.knopflerfish.service.desktop.DefaultBundleSelectionModel;
import org.knopflerfish.service.desktop.SelectionAware;
import org.knopflerfish.service.desktop.SwingBundleDisplayer;

public abstract class DefaultSwingBundleDisplayer
  implements SwingBundleDisplayer, SelectionAware, BundleListener,
  ServiceListener, BundleSelectionListener
{

  final String name;
  String desc;
  boolean bDetail;

  boolean bAlive = false;
  BundleSelectionModel bundleSelModel = new DefaultBundleSelectionModel();

  ServiceRegistration<SwingBundleDisplayer> reg = null;

  boolean bUseListeners = true;
  boolean bUpdateOnBundleChange = false;
  boolean bUpdateOnServiceChange = false;

  BundleContext bc;

  public DefaultSwingBundleDisplayer(BundleContext bc, String name,
                                     String desc, boolean bDetail)
  {
    this.bc = bc;
    this.name = name;
    this.desc = desc;
    this.bDetail = bDetail;
  }

  protected Bundle[] getBundleArray()
  {
    // Using a bundle listener and caching the list of bundles will not work
    // since the caller of this method may be notified by another bundle
    // listener before we are.
    return getAndSortBundles();
  }

  public ServiceRegistration<SwingBundleDisplayer> register()
  {
    if (reg != null) {
      return reg;
    }

    open();

    final Dictionary<String, Object> props = new Hashtable<>();
    props.put(SwingBundleDisplayer.PROP_NAME, getName());
    props.put(SwingBundleDisplayer.PROP_DESCRIPTION, getDescription());
    props.put(SwingBundleDisplayer.PROP_ISDETAIL, isDetail()
      ? Boolean.TRUE
      : Boolean.FALSE);

    reg =
      Activator.getBC()
          .registerService(SwingBundleDisplayer.class, this, props);

    return reg;
  }

  public void unregister()
  {
    if (reg == null) {
      return;
    }

    reg.unregister();
    close();
    reg = null;
  }

  public String getName()
  {
    return name;
  }

  public String getDescription()
  {
    return desc;
  }

  public boolean isDetail()
  {
    return bDetail;
  }

  public abstract JComponent newJComponent();

  public void open()
  {
    bAlive = true;

    if (bUseListeners) {
      Activator.getTargetBC().addBundleListener(this);
      Activator.getTargetBC().addServiceListener(this);
    }

    bundleSelModel.addBundleSelectionListener(this);

    if (bUseListeners) {
      getAllBundles();
      getAllServices();
    }
  }

  public void close()
  {
    bAlive = false;

    if (bundleSelModel != null) {
      bundleSelModel.removeBundleSelectionListener(this);
    }
    Activator.getTargetBC().removeBundleListener(this);
    Activator.getTargetBC().removeServiceListener(this);

    // Must clone components to avoid concurrent modification since dispose will
    // remove items from components.
    for (final JComponent comp : new HashSet<>(components)) {
      disposeJComponent(comp);
    }
    components.clear(); // Should be a noop since disposeJComponent shall remove it...
  }


  /**
   * Send out bundle changed events for all existing bundles so that displayers
   * that overrides {@link #bundleChanged(BundleEvent)} gets notified about all
   * installed bundles.
   */
  void getAllBundles()
  {
    try {
      final Bundle[] bundles = Activator.getBundles();

      // do something reasonable with bundles already installed
      for (int i = 0; bundles != null && i < bundles.length; i++) {
        BundleEvent bundleEvent;
        if (bundles[i].getState() == Bundle.ACTIVE) {
          bundleEvent = new BundleEvent(BundleEvent.STARTED, bundles[i]);
        } else {
          bundleEvent = new BundleEvent(BundleEvent.INSTALLED, bundles[i]);
        }
        bundleChanged(bundleEvent);
      }
    } catch (final Exception e) {
      Activator.log
          .error("Failed to send catch-up events to bundle listeners: "
                     + e.getMessage(), e);
    }
  }

  void getAllServices()
  {
    try {
      final ServiceReference<?>[] srl =
        Activator.getTargetBC_getServiceReferences();
      for (int i = 0; srl != null && i < srl.length; i++) {
        serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, srl[i]));
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * This field is set to true when entering the {@link #valueChanged(long)}
   * method and set to false when leaving it. The row selection model uses this
   * to determine the source of a selection change and do different actions if
   * the call is caused by a selection in the table UI or if the selection
   * changed originates from some other place.
   */
  protected boolean bInValueChanged = false;

  public void valueChangedLazy(long bid)
  {
    repaintComponents();
  }

  long lastBID = -1;

  @Override
  public void valueChanged(long bid)
  {
    boolean bHasVisible = false;
    for (final JComponent comp : components) {
      if (comp.isShowing()) {
        bHasVisible = true;
        break;
      }
    }
    lastBID = bid;

    if (bHasVisible) {
      valueChangedLazy(bid);
    }
  }

  @Override
  public void displayerSelected()
  {
    valueChangedLazy(lastBID);
  }

  // BundleListener interface, only called when bUseListeners = true
  @Override
  public void bundleChanged(BundleEvent ev)
  {
    if (!bAlive) {
      return;
    }

    if (bUpdateOnBundleChange) {
      updateComponents(Activator.desktop.getSelectedBundles());
    }
  }

  // ServiceListener interface, only called when bUseListeners = true
  @Override
  public void serviceChanged(ServiceEvent ev)
  {
    if (!bAlive) {
      return;
    }

    if (bUpdateOnServiceChange) {
      if (Activator.desktop != null) {
        updateComponents(Activator.desktop.getSelectedBundles());
      }
    }

  }

  void updateComponents(final Bundle[] bl)
  {
    SwingUtilities.invokeLater(() -> {
      for (final JComponent comp : components) {
        if (comp.isShowing()) {
          if (comp instanceof JHTMLBundle) {
            final JHTMLBundle jhtml = (JHTMLBundle) comp;
            jhtml.valueChanged(bl);
          }
        }
      }
    });
  }

  protected Bundle[] getAndSortBundles()
  {
    final Bundle[] bundles = Activator.getBundles();
    Arrays.sort(bundles, Util.bundleIdComparator);
    return bundles;
  }

  @Override
  public void setBundleSelectionModel(BundleSelectionModel model)
  {
    if (bundleSelModel != null) {
      bundleSelModel.removeBundleSelectionListener(this);
    }
    bundleSelModel = model;
    bundleSelModel.addBundleSelectionListener(this);
  }

  public BundleSelectionModel getBundleSelectionModel()
  {
    return bundleSelModel;
  }

  Set<JComponent> components = new HashSet<>();

  @Override
  public JComponent createJComponent()
  {
    final JComponent comp = newJComponent();
    components.add(comp);

     return comp;
  }

  @Override
  public void disposeJComponent(JComponent comp)
  {
    components.remove(comp);
  }

  void repaintComponents()
  {
    for (final JComponent comp : components) {
      comp.invalidate();
      comp.repaint();
    }
  }

  @Override
  public Icon getLargeIcon()
  {
    return null;
  }

  @Override
  public Icon getSmallIcon()
  {
    return null;
  }

  @Override
  public void setTargetBundleContext(BundleContext bc)
  {
    if (this.bc != bc) {
      this.bc.removeBundleListener(this);
      this.bc.removeServiceListener(this);

      // TODO Send bundle / service events to remove bundles / services for the
      // old framework

      this.bc = bc;

      if (bUseListeners) {
        this.bc.addBundleListener(this);
        this.bc.addServiceListener(this);

        getAllBundles();
        getAllServices();
      }
    }
  }

  @Override
  public void showBundle(Bundle b)
  {
  }

  public static final String PREFS_NODE_NAME = "SwingBundleDisplayer";
  final int maxErr = 10;
  int errCount = 0;

  /**
   * Get the preferences node to store user settings for this displayer in.
   */
  public Preferences getPrefs()
  {
    final Preferences prefsBase = Preferences.userNodeForPackage(getClass());

    String spid = Activator.getBC().getProperty("org.osgi.provisioning.spid");
    if (spid == null) {
      spid = "default";
    }

    final String relPath =
      PREFS_NODE_NAME + "/" + spid + "/" + name.replace('/', '_');
    final Preferences prefs = prefsBase.node(relPath);
    try {
      prefs.sync(); // Get the latest version of the node.
    } catch (final Exception e) {
      errCount++;
      if (errCount < maxErr) {
        Activator.log.warn("Failed to get preferences node " + relPath, e);
      }
    }
    return prefs;
  }

}
