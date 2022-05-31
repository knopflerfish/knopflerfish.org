/*
 * Copyright (c) 2013-2022, KNOPFLERFISH project
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

package org.knopflerfish.framework;

import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.SortedMap;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.hooks.weaving.WovenClassListener;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

class WeavingHooks {
  final private FrameworkContext fwCtx;
  ServiceTracker<WeavingHook, TrackedWeavingHook> weavingHookTracker;
  ServiceTracker<WovenClassListener,WovenClassListener> listenerTracker;

  WeavingHooks(FrameworkContext fwCtx) {
    this.fwCtx = fwCtx;
  }

  synchronized void open() {
    if (fwCtx.debug.hooks) {
      fwCtx.debug.println("Begin Tracking Weaving Hooks");
    }

    weavingHookTracker = new ServiceTracker<WeavingHook, TrackedWeavingHook>(
        fwCtx.systemBundle.bundleContext, WeavingHook.class,
        new ServiceTrackerCustomizer<WeavingHook, TrackedWeavingHook>() {
          int cnt = 0;
          public TrackedWeavingHook addingService(
              ServiceReference<WeavingHook> reference) {
            if (cnt++ == 0) {
              openListener();
            }
            return new TrackedWeavingHook(
                fwCtx.systemBundle.bundleContext
                    .getService(reference), reference);
          }

          public void modifiedService(ServiceReference<WeavingHook> reference,
              TrackedWeavingHook service) {
          }

          public void removedService(ServiceReference<WeavingHook> reference,
              TrackedWeavingHook service) {
            if (--cnt == 0) {
              closeListener();
            }
          }
        });

    weavingHookTracker.open();
  }

  private void openListener() {
    listenerTracker = new ServiceTracker<WovenClassListener,WovenClassListener>
      (fwCtx.systemBundle.bundleContext, WovenClassListener.class, null);
    listenerTracker.open();
  }

  synchronized void close() {
    weavingHookTracker.close();
    weavingHookTracker = null;
  }

  private void closeListener() {
    listenerTracker.close();
    listenerTracker = null;
  }

  /**
   * Call hooks
   * Called by from a syncronized BundleClassLoader
   *
   * @param wc
   */
  void callHooks(WovenClassImpl wc) throws Exception {
    boolean ok = false;
    final SortedMap<ServiceReference<WeavingHook>, TrackedWeavingHook> hooks;

    if (wc.isWeavingComplete()) {
      throw new RuntimeException("ERROR!!");
    }

    synchronized(this) {
      if (weavingHookTracker == null) {
        return;
      }
      hooks = weavingHookTracker.getTracked();
    }

    try {
      for (final TrackedWeavingHook twh : hooks.values()) {
        if (twh.isBlackListed())
          continue;
        try {
          twh.weave(wc);
        } catch (final WeavingException we) {
          fwCtx.frameworkError(twh.reference.getBundle(), we);
          final ClassFormatError cfe = new ClassFormatError(
              "WeavingException thrown: " + we.getMessage() + " by hook "
                  + twh.getClass().getName());
          cfe.initCause(we);
          throw cfe;
        } catch (final Throwable t) {
          fwCtx.frameworkError(twh.reference.getBundle(), t);
          twh.blacklist();
          final ClassFormatError cfe = new ClassFormatError("Exception throw: " + t
              + " while calling hook " + twh.getClass().getName());
          cfe.initCause(t);
          throw cfe;
        }
      }
      ok = true;
    } finally {
      wc.markAsComplete(ok);
    }
  }

  /**
   * Call listeners
   * Called indirectly from a syncronized BundleClassLoader
   *
   * @param wc
   */
  void callListeners(WovenClassImpl wc) {
    WovenClassListener wcl = fwCtx.perm.getWovenClassListener();
    ServiceReference<WovenClassListener> [] srs;
    if (wcl != null) {
      wcl.modified(wc);
    }
    synchronized (this) {
      if (listenerTracker == null) {
        return;
      }
      srs = listenerTracker.getServiceReferences();
    }
    if (srs != null) {
      for (ServiceReference<WovenClassListener> wlsr : srs) {
        try {
          fwCtx.systemBundle.bundleContext.getService(wlsr).modified(wc);
        } catch (final Throwable t) {
          fwCtx.frameworkWarning(wlsr.getBundle(), t);
        }
      }
    }
  }


  static class TrackedWeavingHook implements WeavingHook {
    final WeavingHook tracked;
    final ServiceReference<WeavingHook> reference;
    boolean blacklisted = false;

    TrackedWeavingHook(WeavingHook tracked,
        ServiceReference<WeavingHook> reference) {
      this.tracked = tracked;
      this.reference = reference;
    }

    public void weave(WovenClass wovenClass) {
      tracked.weave(wovenClass);
    }

    void blacklist() {
      blacklisted = true;
    }

    boolean isBlackListed() {
      return blacklisted;
    }
  }

  static class WovenClassImpl implements WovenClass {
    final BundleImpl bundle;
    final String name;
    byte[] current;
    int state = WovenClass.TRANSFORMING;
    Class<?> c = null;
    final List<String> dynamicImports = new DynamicImportList(this);
  
    WovenClassImpl(BundleImpl bundle, String name, byte[] initial) {
      this.bundle = bundle;
      this.name = name;
      this.current = initial;
    }
  
    public byte[] getBytes() {
      bundle.fwCtx.perm.checkWeaveAdminPerm(bundle);
      if (state == WovenClass.TRANSFORMING) {
        return current;
      } else {
        final byte[] r = new byte[current.length];
        System.arraycopy(current, 0, r, 0, current.length);
        return r;
      }
    }
  
    public void setBytes(byte[] newBytes) {
      bundle.fwCtx.perm.checkWeaveAdminPerm(bundle);
      if ((state & (WovenClass.TRANSFORMED | WovenClass.DEFINED |
                    WovenClass.TRANSFORMING_FAILED | WovenClass.DEFINE_FAILED)) != 0) {
        throw new IllegalStateException(
            "Trying to call WovenClass.setBytes(byte[]) after weaving is complete");
      }
      if (newBytes == null) {
        throw new NullPointerException(
            "Trying to call WovenClass.setBytes(byte[]) with null newBytes");
      }
      current = newBytes;
    }
  
    public List<String> getDynamicImports() {
      return dynamicImports;
    }
  
    public boolean isWeavingComplete() {
      return (state & (WovenClass.DEFINED | WovenClass.TRANSFORMING_FAILED | WovenClass.DEFINE_FAILED)) != 0;
    }
  
    public String getClassName() {
      return name;
    }
  
    public ProtectionDomain getProtectionDomain() {
      return c == null ? null : c.getProtectionDomain();
    }
  
    public Class<?> getDefinedClass() {
      return c;
    }
  
    public BundleWiring getBundleWiring() {
      BundleRevisionImpl br = bundle.current().bundleRevision;
      if (br != null) {
        return br.getWiring();
      }
      return null;
    }

    public int getState() {
      return state;
    }

    void markAsComplete(boolean ok) {
      state = ok ? WovenClass.TRANSFORMED : WovenClass.TRANSFORMING_FAILED;
      bundle.fwCtx.weavingHooks.callListeners(this);
    }
  
    void setDefinedClass(Class<?> c) {
      state = c != null ? WovenClass.DEFINED : WovenClass.DEFINE_FAILED;
      this.c = c;
      bundle.fwCtx.weavingHooks.callListeners(this);
    }
  
    @Override
    public String toString() {
      return "WovenClass[" + name + ", " + toString(dynamicImports) + ", byte["
          + current.length + "]=" + current + "]";
    }
  
    String getDynamicImportsAsString() {
      final StringBuilder sb = new StringBuilder();
      for (final String s : dynamicImports) {
        if (sb.length() > 0) {
          sb.append(", ");
        }
        sb.append(s);
      }
      return sb.toString();
    }
  
    String toString(List<String> sl) {
      final StringBuilder sb = new StringBuilder();
      sb.append("(");
      for (final String s : sl) {
        if (sb.length() > 1) {
          sb.append(", ");
        }
        sb.append(s);
      }
      sb.append(")");
      return sb.toString();
    }
  
    public boolean hasAdditionalDynamicImports() {
      return !dynamicImports.isEmpty();
    }
  }


  public static class DynamicImportList implements List<String> {
  
    final private List<String> org;
    final private WovenClassImpl parent;
    
    public DynamicImportList(WovenClassImpl parent) {
      this.parent = parent;
      this.org = new ArrayList<String>();
    }


    public DynamicImportList(WovenClassImpl parent, List<String> subList) {
      this.parent = parent;
      org = subList;
    }


    @Override
    public boolean add(String elem) {
      checkChangeAllowed(Collections.singletonList(elem));
      return org.add(elem);
    }


    @Override
    public void add(int index, String elem) {
      checkChangeAllowed(Collections.singletonList(elem));
      org.add(index, elem);
    }
  

    @Override
    public boolean addAll(Collection<? extends String> elems) {
      checkChangeAllowed(elems);
      return org.addAll(elems);
    }


    @Override
    public boolean addAll(int index, Collection<? extends String> elems) {
      checkChangeAllowed(elems);
      return org.addAll(index, elems);
    }


    @Override
    public void clear() {
      checkChangeAllowed(null);
      org.clear();
    }


    @Override
    public boolean contains(Object elem) {
      return org.contains(elem);
    }


    @Override
    public boolean containsAll(Collection<?> elems) {
      return org.containsAll(elems);
    }


    @Override
    public String get(int index) {
      return org.get(index);
    }


    @Override
    public int indexOf(Object elem) {
      return org.indexOf(elem);
    }


    @Override
    public boolean isEmpty() {
      return org.isEmpty();
    }


    @Override
    public Iterator<String> iterator() {
      return new DynamicListIterator(parent, org.listIterator());
    }


    @Override
    public int lastIndexOf(Object elem) {
      return org.lastIndexOf(elem);
    }


    @Override
    public ListIterator<String> listIterator() {
      return new DynamicListIterator(parent, org.listIterator());
    }


    @Override
    public ListIterator<String> listIterator(int index) {
      return new DynamicListIterator(parent, org.listIterator(index));
    }


    @Override
    public String remove(int index) {
      checkChangeAllowed(null);
      return org.remove(index);
    }
  

    @Override
    public boolean remove(Object elem) {
      checkChangeAllowed(null);
      return org.remove(elem);
    }
  

    @Override
    public boolean removeAll(Collection<?> elems) {
      checkChangeAllowed(null);
      return org.removeAll(elems);
    }
  

    @Override
    public boolean retainAll(Collection<?> elems) {
      checkChangeAllowed(null);
      return org.removeAll(elems);
    }


    @Override
    public String set(int index, String elem) {
      checkChangeAllowed(Collections.singletonList(elem));
      return org.set(index, elem);
    }


    @Override
    public int size() {
      return org.size();
    }


    @Override
    public List<String> subList(int from, int to) {
      return new DynamicImportList(parent, org.subList(from, to));
    }


    @Override
    public Object[] toArray() {
      return org.toArray();
    }


    @Override
    public <T> T[] toArray(T[] a) {
      return org.toArray(a);
    }


    private void checkChangeAllowed(Collection<? extends String> elems) throws UnsupportedOperationException {
      if (parent.isWeavingComplete()) {
        throw new IllegalStateException("Parent WovenClass is frozen");
      }
      parent.bundle.fwCtx.perm.checkWeaveAdminPerm(parent.bundle);
      if (elems != null) {
        for (String pkg : elems) {
          parent.bundle.fwCtx.perm.checkImportPackagePermission(pkg);
        }
      }
    }
  
  }

  public static class DynamicListIterator implements ListIterator<String> {

    final private WovenClassImpl parent;
    final private ListIterator<String> org;
 

    public DynamicListIterator(WovenClassImpl parent, ListIterator<String> org) {
      this.parent = parent;
      this.org = org;
    }

    @Override
    public void add(String elem) {
      checkChangeAllowed(elem);
      org.add(elem);
    }

    @Override
    public boolean hasNext() {
      return org.hasNext();
    }

    @Override
    public boolean hasPrevious() {
      return org.hasPrevious();
    }

    @Override
    public String next() {
      return org.next();
    }

    @Override
    public int nextIndex() {
      return org.nextIndex();
    }

    @Override
    public String previous() {
      return org.previous();
    }

    @Override
    public int previousIndex() {
      return org.previousIndex();
    }

    @Override
    public void remove() {
      checkChangeAllowed(null);
      org.remove();
    }

    @Override
    public void set(String elem) {
      checkChangeAllowed(elem);
      org.set(elem);
    }
  
    private void checkChangeAllowed(String elem) throws UnsupportedOperationException {
      if (parent.isWeavingComplete()) {
        throw new IllegalStateException("Parent WovenClass is frozen");
      }
      parent.bundle.fwCtx.perm.checkWeaveAdminPerm(parent.bundle);
      if (elem != null) {
        parent.bundle.fwCtx.perm.checkImportPackagePermission(elem);
      }
    }
  
  }

  
}
