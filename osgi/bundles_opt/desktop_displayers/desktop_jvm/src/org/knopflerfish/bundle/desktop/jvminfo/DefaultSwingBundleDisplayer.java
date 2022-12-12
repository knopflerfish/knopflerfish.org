/*
 * Copyright (c) 2004-2022, KNOPFLERFISH project
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

package org.knopflerfish.bundle.desktop.jvminfo;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JComponent;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import org.knopflerfish.service.desktop.BundleSelectionListener;
import org.knopflerfish.service.desktop.BundleSelectionModel;
import org.knopflerfish.service.desktop.DefaultBundleSelectionModel;
import org.knopflerfish.service.desktop.SwingBundleDisplayer;

public abstract class DefaultSwingBundleDisplayer
  implements 
    SwingBundleDisplayer,
    BundleSelectionListener
{

  String               name;
  String               desc;
  boolean              bDetail;

  BundleContext        bc;
  boolean              bAlive = false;
  BundleSelectionModel bundleSelModel = new DefaultBundleSelectionModel();
  ServiceRegistration<SwingBundleDisplayer> reg  = null;

  boolean bUseListeners = true;

  public DefaultSwingBundleDisplayer(BundleContext bc,
				     String        name,
				     String        desc,
				     boolean       bDetail) {
    this.bc      = bc;
    this.name    = name;
    this.desc    = desc;
    this.bDetail = bDetail;
  }


  public void register() {
    if (reg != null) {
      return;
    }

    open();
    
    Hashtable<String, Object> props = new Hashtable<>();
    props.put(SwingBundleDisplayer.PROP_NAME,        getName());
    props.put(SwingBundleDisplayer.PROP_DESCRIPTION, getDescription());
    props.put(SwingBundleDisplayer.PROP_ISDETAIL,    
	      isDetail() 
	      ? Boolean.TRUE 
	      : Boolean.FALSE);
    
    reg = bc.registerService(SwingBundleDisplayer.class,
			     this,
			     props);
  }

  public void unregister() {
    if (reg == null) {
      return;
    }

    reg.unregister();
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

  public void showBundle(Bundle b) {
      // NYI
  }

  public abstract JComponent newJComponent();

  public void open() {
    bAlive = true;

    bundleSelModel.addBundleSelectionListener(this);

  }



  public void close() {
    bAlive = false;

    if(bundleSelModel != null) {
      bundleSelModel.removeBundleSelectionListener(this); 
    }

    closeComponents();
  }

  public void valueChanged(long bid) {
    //    System.out.println(getClass().getName() + ".valueChanged " + (b != null ? ("#" + b.getBundleId()) : "null"));
    repaintComponents();
  }


  public void setBundleSelectionModel(BundleSelectionModel model) {
    if(bundleSelModel != null) {
      bundleSelModel.removeBundleSelectionListener(this); 
    }
    bundleSelModel = model;
    bundleSelModel.addBundleSelectionListener(this);
  }

  Set<JComponent> components = new HashSet<>();

  public JComponent createJComponent() {
    JComponent comp = newJComponent();
    components.add(comp);

    return comp;
  }

  public void  disposeJComponent(JComponent comp) {
    components.remove(comp);
  }

  void closeComponents() {
    for (JComponent component : components) {
      closeComponent(component);
    }
  }

  void closeComponent(JComponent comp) {
    // Should be overridden 
  }

  void repaintComponents() {
    for (JComponent component : components) {
      component.invalidate();
      component.repaint();
    }
  }

  public Icon       getLargeIcon() {
    return null;
  }

  public Icon       getSmallIcon() {
    return null;
  }

  public void       setTargetBundleContext(BundleContext bc) {
    // NYI
  }
}
