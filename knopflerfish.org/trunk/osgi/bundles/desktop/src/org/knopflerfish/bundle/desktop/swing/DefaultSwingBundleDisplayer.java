/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
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

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.knopflerfish.service.desktop.BundleSelectionListener;
import org.knopflerfish.service.desktop.BundleSelectionModel;
import org.knopflerfish.service.desktop.DefaultBundleSelectionModel;
import org.knopflerfish.service.desktop.SwingBundleDisplayer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public abstract class DefaultSwingBundleDisplayer
  implements
    SwingBundleDisplayer,
    BundleListener,
    ServiceListener,
    BundleSelectionListener
{

  String               name;
  String               desc;
  boolean              bDetail;

  boolean              bAlive = false;
  BundleSelectionModel bundleSelModel = new DefaultBundleSelectionModel();
  private Bundle[]     bundles;

  ServiceRegistration  reg  = null;

  boolean bUseListeners           = true;
  boolean bUpdateOnBundleChange   = false;
  boolean bUpdateOnServiceChange  = false;

  BundleContext bc;

  public DefaultSwingBundleDisplayer(BundleContext bc,
                                     String        name,
                                     String        desc,
                                     boolean       bDetail) {
    this.bc      = bc;
    this.name    = name;
    this.desc    = desc;
    this.bDetail = bDetail;
  }


  protected Bundle[] getBundleArray() {

    if(bundles == null) {
      bundles = getAndSortBundles();
    }

    return bundles;
  }

  public ServiceRegistration register() {
    if(reg != null) {
      return reg;
    }

    open();

    Hashtable props = new Hashtable();
    props.put(SwingBundleDisplayer.PROP_NAME,        getName());
    props.put(SwingBundleDisplayer.PROP_DESCRIPTION, getDescription());
    props.put(SwingBundleDisplayer.PROP_ISDETAIL,
              isDetail()
              ? Boolean.TRUE
              : Boolean.FALSE);

    reg = Activator.getBC()
      .registerService(SwingBundleDisplayer.class.getName(),
                       this,
                       props);

    return reg;
  }



  public void unregister() {
    if(reg == null) {
      return;
    }

    reg.unregister();
    close();
    reg = null;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return desc;
  }
  public boolean isDetail() {
    return bDetail;
  }

  public abstract JComponent newJComponent();

  public void open() {
    bAlive = true;

    if(bUseListeners) {
      Activator.getTargetBC().addBundleListener(this);
      Activator.getTargetBC().addServiceListener(this);
    }

    bundleSelModel.addBundleSelectionListener(this);

    if(bUseListeners) {
      getAllBundles();
      getAllServices();
    }
  }


  void getAllServices() {
    try {
      ServiceReference[] srl = Activator.getTargetBC().getServiceReferences(null, null);
      for(int i = 0; srl != null && i < srl.length; i++) {
        serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, srl[i]));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  void getAllBundles() {
    try {
      int delay = 0;

      Bundle[] bl = Activator.getBundles();

      // do something reasonable with bundles already installed
      for(int i = 0; bl != null && i < bl.length; i++) {
        BundleEvent ev = null;
        switch(bl[i].getState()) {
        case Bundle.ACTIVE:
          ev = new BundleEvent(BundleEvent.STARTED, bl[i]);
          break;
        default:
          ev = new BundleEvent(BundleEvent.INSTALLED, bl[i]);
          break;
        }
        bundleChanged(ev);
        if(delay > 0) {
          Thread.sleep(delay);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void close() {
    bAlive = false;

    if(bundleSelModel != null) {
      bundleSelModel.removeBundleSelectionListener(this);
    }
    Activator.getTargetBC().removeBundleListener(this);
    Activator.getTargetBC().removeServiceListener(this);

    for(Iterator it = components.iterator(); it.hasNext();) {
      JComponent comp = (JComponent)it.next();
      disposeJComponent(comp);
    }
    components.clear();
  }

  protected boolean bInValueChanged = false;

  public void valueChangedLazy(long bid) {
    repaintComponents();
  }

  long lastBID = -1;
  public void valueChanged(long bid) {
    boolean bHasVisible = false;
    for(Iterator it = components.iterator(); it.hasNext(); ) {
      JComponent comp = (JComponent)it.next();
      if(comp.isVisible() && comp.isShowing()) {
        bHasVisible = true;
        break;
      }
    }
    lastBID = bid;

    if(bHasVisible) {
      valueChangedLazy(bid);
    }
  }

  public void setTabSelected() {
    valueChangedLazy(lastBID);
  }


  // BundleListener interface, only called when bUseListeners = true
  public void bundleChanged(BundleEvent ev) {
    if(!bAlive) {
      return;
    }

    bundles = getAndSortBundles();

    if(bUpdateOnBundleChange) {
      updateComponents(Activator.desktop.getSelectedBundles());
    }
  }

  // ServiceListener interface, only called when bUseListeners = true
  public void serviceChanged(ServiceEvent ev) {
    if(!bAlive) {
      return;
    }

    if(bUpdateOnServiceChange) {
      if(Activator.desktop != null) {
        updateComponents(Activator.desktop.getSelectedBundles());
      }
    }

  }

  void updateComponents(final Bundle[] bl) {
    SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          for(Iterator it = components.iterator(); it.hasNext(); ) {
            JComponent comp = (JComponent)it.next();
            if(comp.isVisible()) {
              if(comp instanceof JHTMLBundle) {
                JHTMLBundle jhtml = (JHTMLBundle)comp;
                jhtml.valueChanged(bl);
              }
            }
          }
        }
      });
  }

  protected Bundle[] getAndSortBundles() {
    Bundle[] bl = Activator.getBundles();
    SortedSet set = new TreeSet(Util.bundleIdComparator);
    for(int i = 0; i < bl.length; i++) {
      set.add(bl[i]);
    }

    set.toArray(bl);

    return bl;
  }



  public void setBundleSelectionModel(BundleSelectionModel model) {
    if(bundleSelModel != null) {
      bundleSelModel.removeBundleSelectionListener(this);
    }
    bundleSelModel = model;
    bundleSelModel.addBundleSelectionListener(this);
  }


  public BundleSelectionModel getBundleSelectionModel() {
    return bundleSelModel;
  }


  Set components = new HashSet();

  public JComponent createJComponent() {
    JComponent comp = newJComponent();
    components.add(comp);

    getAllBundles();

    return comp;
  }

  public void  disposeJComponent(JComponent comp) {
    components.remove(comp);
  }

  void repaintComponents() {
    for(Iterator it = components.iterator(); it.hasNext(); ) {
      JComponent comp = (JComponent)it.next();
      comp.invalidate();
      comp.repaint();
    }
  }


  public Icon       getLargeIcon() {
    return null;
  }

  public Icon       getSmallIcon() {
    return null;
  }


  public void       setTargetBundleContext(BundleContext bc) {
    this.bc = bc;
  }

  public void showBundle(Bundle b) {
  }

}
