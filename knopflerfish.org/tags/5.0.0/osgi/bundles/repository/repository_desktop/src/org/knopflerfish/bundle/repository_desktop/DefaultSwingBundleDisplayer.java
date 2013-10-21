/*
 * Copyright (c) 2004-2013, KNOPFLERFISH project
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

package org.knopflerfish.bundle.repository_desktop;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.Icon;
import javax.swing.JComponent;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import org.knopflerfish.service.desktop.BundleSelectionListener;
import org.knopflerfish.service.desktop.BundleSelectionModel;
import org.knopflerfish.service.desktop.DefaultBundleSelectionModel;
import org.knopflerfish.service.desktop.SelectionAware;
import org.knopflerfish.service.desktop.SwingBundleDisplayer;

public abstract class DefaultSwingBundleDisplayer
  implements SwingBundleDisplayer, SelectionAware, BundleSelectionListener
{
  final String name;
  String desc;
  boolean bDetail;

  boolean bAlive = false;
  BundleSelectionModel bundleSelModel = new DefaultBundleSelectionModel();

  ServiceRegistration<SwingBundleDisplayer> reg = null;

  /** The bundle context of the target framework to present data for. */
  static BundleContext bc;

  public DefaultSwingBundleDisplayer(BundleContext bc, String name,
                                     String desc, boolean bDetail)
  {
    DefaultSwingBundleDisplayer.bc = bc;
    this.name = name;
    this.desc = desc;
    this.bDetail = bDetail;
  }

  protected Bundle[] getBundleArray()
  {
    return getAllBundlesSortedByName().toArray(new Bundle[0]);
  }

  public ServiceRegistration<SwingBundleDisplayer> register()
  {
    if (reg != null) {
      return reg;
    }

    open();

    final Dictionary<String, Object> props = new Hashtable<String, Object>();
    props.put(SwingBundleDisplayer.PROP_NAME, getName());
    props.put(SwingBundleDisplayer.PROP_DESCRIPTION, getDescription());
    props.put(SwingBundleDisplayer.PROP_ISDETAIL, isDetail()
      ? Boolean.TRUE
      : Boolean.FALSE);

    // Register this displayer service in the local framework.
    reg = Activator.bc.registerService(SwingBundleDisplayer.class, this, props);

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

    bundleSelModel.addBundleSelectionListener(this);
  }

  public void close()
  {
    bAlive = false;

    if (bundleSelModel != null) {
      bundleSelModel.removeBundleSelectionListener(this);
    }

    synchronized (components) {
      // Must clone components to avoid concurrent modification since dispose
      // will remove items from components.
      for (final JComponent comp : new HashSet<JComponent>(components)) {
        disposeJComponent(comp);
      }
      components.clear(); // Should be a noop since disposeJComponent shall
                          // remove it...
    }
  }

  /**
   * A displayer should override this method to update its UI for new
   * selections. A sub-class that overrides this method do not need to call the
   * {link super{@link #valueChangedLazy(long)}.
   *
   * @param bid
   *          One of the bundles in the new selection. Ask the selection model
   *          to get all.
   */
  protected void valueChangedLazy(long bid)
  {
    repaintComponents();
  }

  /**
   * The value of the {@code bid} argument in the last call to
   * {@link #valueChanged(long)} is saved in this field. The field value is used
   * as the argument in the call to {@link #valueChangedLazy(long)} made from
   * subclasses. E.g., in {@link RepositoryDisplayer#displayerSelected()}.
   */
  protected long lastBID = -1;

  @Override
  public void valueChanged(long bid)
  {
    boolean bHasVisibleComponent = false;
    synchronized (components) {
      for (final JComponent comp : components) {
        if (comp.isShowing()) {
          bHasVisibleComponent = true;
          break;
        }
      }
    }
    lastBID = bid;

    if (bHasVisibleComponent) {
      valueChangedLazy(bid);
    }
  }

  @Override
  public void displayerSelected()
  {
    valueChangedLazy(lastBID);
  }

  static private Bundle[] getBundles()
  {
    final BundleContext tbc = getTargetBundleContext();
    final Bundle[] bl = tbc == null ? null : tbc.getBundles();
    return bl;
  }

  public static SortedSet<Bundle> getAllBundlesSortedByName()
  {
    final Bundle[] bl = getBundles();
    final SortedSet<Bundle> set =
      new TreeSet<Bundle>(Util.bundleNameComparator);
    if (bl != null) {
      set.addAll(Arrays.asList(bl));
    }
    return set;
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

  Set<JComponent> components = new HashSet<JComponent>();

  @Override
  public JComponent createJComponent()
  {
    final JComponent comp = newJComponent();
    synchronized (components) {
      components.add(comp);
    }

    return comp;
  }

  @Override
  public void disposeJComponent(JComponent comp)
  {
    synchronized (components) {
      components.remove(comp);
    }
  }

  void repaintComponents()
  {
    synchronized (components) {
      for (final JComponent comp : components) {
        comp.invalidate();
        comp.repaint();
      }
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
    if (DefaultSwingBundleDisplayer.bc != bc) {
      DefaultSwingBundleDisplayer.bc = bc;
    }
  }

  static protected BundleContext getTargetBundleContext()
  {
    return bc;
  }

  @Override
  public void showBundle(Bundle b)
  {
  }
}
