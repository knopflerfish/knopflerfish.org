/*
 * Copyright (c) 2013-2013, KNOPFLERFISH project
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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.SortedMap;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

class WeavingHooks {
  final private FrameworkContext fwCtx;
  ServiceTracker<WeavingHook, TrackedWeavingHook> weavingHookTracker;

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
          public TrackedWeavingHook addingService(
              ServiceReference<WeavingHook> reference) {
            return new TrackedWeavingHook(
                fwCtx.systemBundle.bundleContext
                    .getService(reference), reference);
          }

          public void modifiedService(ServiceReference<WeavingHook> reference,
              TrackedWeavingHook service) {

          }

          public void removedService(ServiceReference<WeavingHook> reference,
              TrackedWeavingHook service) {
          }
        });

    weavingHookTracker.open();
  }

  synchronized void close() {
    weavingHookTracker.close();
    weavingHookTracker = null;
  }

  synchronized public boolean isOpen() {
    return weavingHookTracker != null;
  }

  synchronized void callHooks(WovenClassImpl wc) throws Exception {
    if (!isOpen()) {
      return;
    }

    if (wc.isWeavingComplete()) {
      throw new RuntimeException("ERROR!!");
    }

    try {
      final SortedMap<ServiceReference<WeavingHook>, TrackedWeavingHook> hooks = weavingHookTracker
          .getTracked();

      for (final TrackedWeavingHook twh : hooks.values()) {
        if (twh.isBlackListed())
          continue;
        try {
          twh.weave(wc);
        } catch (final WeavingException we) {
          fwCtx.listeners.frameworkError(twh.reference.getBundle(), we);
          final ClassFormatError cfe = new ClassFormatError(
              "WeavingException thrown: " + we.getMessage() + " by hook "
                  + twh.getClass().getName());
          cfe.initCause(we);
          throw cfe;
        } catch (final Throwable t) {
          fwCtx.listeners.frameworkError(twh.reference.getBundle(), t);
          twh.blacklist();
          final ClassFormatError cfe = new ClassFormatError("Exception throw: " + t
              + " while calling hook " + twh.getClass().getName());
          cfe.initCause(t);
          throw cfe;
        }
      }
    } finally {
      wc.markAsComplete();
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
    byte[] current = null;
    boolean complete = false;
    Class<?> c = null;
    final List<String> dynamicImports = new DynamicImportList<String>(this);
  
    WovenClassImpl(BundleImpl bundle, String name, byte[] initial) {
      this.bundle = bundle;
      this.name = name;
      this.current = initial;
    }
  
    public byte[] getBytes() {
      bundle.fwCtx.perm.checkWeaveAdminPerm(bundle);
      if (complete) {
        final byte[] r = new byte[current.length];
        System.arraycopy(current, 0, r, 0, current.length);
        return r;
      } else {
        return current;
      }
    }
  
    public void setBytes(byte[] newBytes) {
      bundle.fwCtx.perm.checkWeaveAdminPerm(bundle);
      if (complete)
        throw new IllegalStateException(
            "Trying to call WovenClass.setBytes(byte[]) after weaving is complete");
      if (newBytes == null)
        throw new NullPointerException(
            "Trying to call WovenClass.setBytes(byte[]) with null newBytes");
      current = newBytes;
    }
  
    public List<String> getDynamicImports() {
      return dynamicImports;
    }
  
    public boolean isWeavingComplete() {
      return complete;
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
      return bundle.current().bundleRevision.getWiring();
    }
  
    void markAsComplete() {
      complete = true;
    }
  
    void setDefinedClass(Class<?> c) {
      markAsComplete();
      this.c = c;
    }
  
    @Override
    public String toString() {
      return "WovenClass[" + name + ", " + toString(dynamicImports) + ", byte["
          + current.length + "]=" + current + "]";
    }
  
    String getDynamicImportsAsString() {
      final StringBuffer sb = new StringBuffer();
      for (final String s : dynamicImports) {
        if (sb.length() > 0) {
          sb.append(", ");
        }
        sb.append(s);
      }
      return sb.toString();
    }
  
    String toString(List<String> sl) {
      final StringBuffer sb = new StringBuffer();
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

  public static class DynamicImportList<E> implements List<E> {
  
    final private List<E> org;
    final private WovenClassImpl parent;
    
    public DynamicImportList(WovenClassImpl parent) {
      this.parent = parent;
      this.org = new ArrayList<E>();
    }


    public DynamicImportList(WovenClassImpl parent, List<E> subList) {
      this.parent = parent;
      org = subList;
    }


    @Override
    public boolean add(E elem) {
      checkChangeAllowed();
      return org.add(elem);
    }


    @Override
    public void add(int index, E elem) {
      checkChangeAllowed();
      org.add(index, elem);
    }
  

    @Override
    public boolean addAll(Collection<? extends E> elems) {
      checkChangeAllowed();
      return org.addAll(elems);
    }


    @Override
    public boolean addAll(int index, Collection<? extends E> elems) {
      checkChangeAllowed();
      return org.addAll(index, elems);
    }


    @Override
    public void clear() {
      checkChangeAllowed();
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
    public E get(int index) {
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
    public Iterator<E> iterator() {
      return new DynamicListIterator<E>(parent, org.listIterator());
    }


    @Override
    public int lastIndexOf(Object elem) {
      return org.lastIndexOf(elem);
    }


    @Override
    public ListIterator<E> listIterator() {
      return new DynamicListIterator<E>(parent, org.listIterator());
    }


    @Override
    public ListIterator<E> listIterator(int index) {
      return new DynamicListIterator<E>(parent, org.listIterator(index));
    }


    @Override
    public E remove(int index) {
      checkChangeAllowed();
      return org.remove(index);
    }
  

    @Override
    public boolean remove(Object elem) {
      checkChangeAllowed();
      return org.remove(elem);
    }
  

    @Override
    public boolean removeAll(Collection<?> elems) {
      checkChangeAllowed();
      return org.removeAll(elems);
    }
  

    @Override
    public boolean retainAll(Collection<?> elems) {
      checkChangeAllowed();
      return org.removeAll(elems);
    }


    @Override
    public E set(int index, E elem) {
      checkChangeAllowed();
      return org.set(index, elem);
    }


    @Override
    public int size() {
      return org.size();
    }


    @Override
    public List<E> subList(int from, int to) {
      return new DynamicImportList<E>(parent, org.subList(from, to));
    }


    @Override
    public Object[] toArray() {
      return org.toArray();
    }


    @Override
    public <T> T[] toArray(T[] a) {
      return org.toArray(a);
    }


    private void checkChangeAllowed() throws UnsupportedOperationException {
      if (parent.isWeavingComplete()) {
        throw new IllegalStateException("Parent WovenClass is frozen");
      }
      parent.bundle.fwCtx.perm.checkWeaveAdminPerm(parent.bundle);
    }
  
  }

  public static class DynamicListIterator<E> implements ListIterator<E> {

    final private WovenClassImpl parent;
    final private ListIterator<E> org;
 

    public DynamicListIterator(WovenClassImpl parent, ListIterator<E> org) {
      this.parent = parent;
      this.org = org;
    }

    @Override
    public void add(E elem) {
      checkChangeAllowed();
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
    public E next() {
      return org.next();
    }

    @Override
    public int nextIndex() {
      return org.nextIndex();
    }

    @Override
    public E previous() {
      return org.previous();
    }

    @Override
    public int previousIndex() {
      return org.previousIndex();
    }

    @Override
    public void remove() {
      checkChangeAllowed();
      org.remove();
    }

    @Override
    public void set(E elem) {
      checkChangeAllowed();
      org.set(elem);
    }
  
    private void checkChangeAllowed() throws UnsupportedOperationException {
      if (parent.isWeavingComplete()) {
        throw new IllegalStateException("Parent WovenClass is frozen");
      }
      parent.bundle.fwCtx.perm.checkWeaveAdminPerm(parent.bundle);
    }
  
  }

  
}
