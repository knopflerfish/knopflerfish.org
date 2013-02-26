/*
 * Copyright (c) 2013, KNOPFLERFISH project
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

import java.net.URL;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
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
    final MockBundleWiring bw;
    final String name;
    byte[] current = null;
    boolean complete = false;
    Class<?> c = null;
    final List<String> dynamicImports = new ArrayList<String>();

    WovenClassImpl(BundleImpl b, String name, byte[] initial) {
      this.bw = new MockBundleWiring(b);
      this.name = name;
      this.current = initial;
    }

    public byte[] getBytes() {
      if (complete) {
        final byte[] r = new byte[current.length];
        System.arraycopy(current, 0, r, 0, current.length);
        return r;
      } else {
        return current;
      }
    }

    public void setBytes(byte[] newBytes) {
      if (complete)
        throw new IllegalStateException(
            "Trying to call WovenClass.setBytes(byte[]) after weaving is complete");
      if (newBytes == null)
        throw new NullPointerException(
            "Trying to call WovenClass.setBytes(byte[]) with null newBytes");
      current = newBytes;
    }

    public List<String> getDynamicImports() {
      if (complete) {
        return Collections.unmodifiableList(dynamicImports);
      } else {
        return dynamicImports;
      }
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
      return bw;
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

  static class MockBundleWiring implements BundleWiring {
    final BundleImpl b;

    MockBundleWiring(BundleImpl b) {
      this.b = b;
    }

    public Bundle getBundle() {
      return b;
    }

    public boolean isCurrent() {
      return true;
    }

    public boolean isInUse() {
      return true;
    }

    public List<BundleCapability> getCapabilities(String namespace) {
      @SuppressWarnings("unchecked")
      final List<BundleCapability> res = Collections.EMPTY_LIST;
      return res;
    }

    public List<BundleRequirement> getRequirements(String namespace) {
      @SuppressWarnings("unchecked")
      final List<BundleRequirement> res = Collections.EMPTY_LIST;
      return res;
    }

    public List<BundleWire> getProvidedWires(String namespace) {
      @SuppressWarnings("unchecked")
      final List<BundleWire> res = Collections.EMPTY_LIST;
      return res;
    }

    public List<BundleWire> getRequiredWires(String namespace) {
      @SuppressWarnings("unchecked")
      final List<BundleWire> res = Collections.EMPTY_LIST;
      return res;
    }

    public BundleRevision getRevision() {
      return new BundleRevision() {
        public Bundle getBundle() {
          return MockBundleWiring.this.b;
        }

        public String getSymbolicName() {
          return MockBundleWiring.this.b.getSymbolicName();
        }

        public Version getVersion() {
          return MockBundleWiring.this.b.getVersion();
        }

        public List<BundleCapability> getDeclaredCapabilities(String namespace) {
          @SuppressWarnings("unchecked")
          final List<BundleCapability> res = Collections.EMPTY_LIST;
          return res;
        }

        public List<BundleRequirement> getDeclaredRequirements(String namespace) {
          @SuppressWarnings("unchecked")
          final List<BundleRequirement> res = Collections.EMPTY_LIST;
          return res;
        }

        public int getTypes() {
          return 0;
        }

        public BundleWiring getWiring() {
          return MockBundleWiring.this;
        }
      };
    }

    public ClassLoader getClassLoader() {
      return b.getClassLoader();
    }

    public List<URL> findEntries(String path, String filePattern, int options)
    {
      @SuppressWarnings("unchecked")
      final List<URL> res = Collections.EMPTY_LIST;
      return res;
    }

    public Collection<String> listResources(String path,
                                            String filePattern,
                                            int options)
    {
      @SuppressWarnings("unchecked")
      final Collection<String> res = Collections.EMPTY_LIST;
      return res;
    }
  }
}
