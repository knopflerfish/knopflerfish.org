/*
 * Copyright (c) 2009-2016, KNOPFLERFISH project
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import org.knopflerfish.framework.Util.HeaderEntry;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

/**
 * Bundle Class Path handler.
 *
 * @author Jan Stein
 */
public class BundleClassPath {
  /**
   * Framework context.
   */
  final private FrameworkContext fwCtx;

  /**
   * Archives that we load code from.
   */
  private final ArrayList<FileArchive> archives = new ArrayList<FileArchive>(4);

  /**
   *
   */
  private Map<String, FileArchive> nativeLibs;

  /**
   *
   */
  private final Debug debug;

  /**
   *
   */
  private final long bid;


  /**
   * Create class loader for specified bundle.
   *
   * @throws BundleException if native code resolve failed.
   */
  BundleClassPath(BundleArchive ba, BundleGeneration gen)
      throws BundleException {
    this.fwCtx = gen.bundle.fwCtx;
    debug = fwCtx.debug;
    bid = gen.bundle.id;
    checkBundleArchive(ba, gen.fragments);
    if (gen.fragments != null) {
      for (final BundleGeneration bundleGeneration : gen.fragments) {
        checkBundleArchive(bundleGeneration.archive, null);
      }
    }
    resolveNativeCode(gen);
    if (gen.fragments != null) {
      for (final BundleGeneration bundleGeneration : gen.fragments) {
        resolveNativeCode(bundleGeneration);
      }
    }
  }


  /**
   *
   */
  BundleClassPath(BundleArchive ba, FrameworkContext fwCtx) {
    this.fwCtx = fwCtx;
    debug = fwCtx.debug;
    bid = ba.getBundleId();
    checkBundleArchive(ba, null);
  }


  /**
   * @throws BundleException
   *
   */
  void attachFragment(BundleGeneration gen) throws BundleException {
    checkBundleArchive(gen.archive, null);
    resolveNativeCode(gen);
  }


  /**
   * Check if named entry exist in bundle class path. Leading '/' is stripped.
   *
   * @param component Entry to get reference to.
   * @param onlyFirst End search when we find first entry if this is true.
   * @return Vector or entry numbers, or null if it doesn't exist.
   */
  Vector<FileArchive> componentExists(String component, boolean onlyFirst, boolean dirs) {
    Vector<FileArchive> v = null;
    if (component.startsWith("/")) {
      component = component.substring(1);
    }
    if (debug.classLoader) {
      debug.println(this + "componentExists: " + component);
    }
    if (0 == component.length()) {
      // The special case asking for "/"
      if (onlyFirst) {
        v = new Vector<FileArchive>(1);
        v.addElement(archives.get(0));
        if (debug.classLoader) {
          debug.println(this + "compentExists added first top in classpath.");
        }
      } else {
        v = new Vector<FileArchive>(archives);
        if (debug.classLoader) {
          debug.println(this + "compentExists added all tops in classpath.");
        }
      }
    } else {
      for (final FileArchive fa : archives) {
        if (fa.exists(component, dirs)) {
          if (v == null) {
            v = new Vector<FileArchive>();
          }
          v.addElement(fa);
          if (debug.classLoader) {
            debug.println(this + "compentExists added: " + fa);
          }
          if (onlyFirst) {
            break;
          }
        }
      }
    }
    return v;
  }


  /**
   * Get an specific InputStream to named entry inside a bundle. Leading '/' is
   * stripped.
   *
   * @param component Entry to get reference to.
   * @param ix index of sub archives. A positive number is the classpath entry
   *          index. 0 means look in the main bundle.
   * @return InputStream to entry or null if it doesn't exist.
   */
  InputStream getInputStream(String component, int ix) {
    if (component.startsWith("/")) {
      component = component.substring(1);
    }
    return archives.get(ix).getBundleResourceStream(component);
  }


  /**
   * Get native library from class path.
   *
   * @param libName Name of Jar file to get.
   * @return A string with the path to the native library.
   */
  String getNativeLibrary(String libName) {
    if (debug.classLoader) {
      debug.println(this + "getNativeLibrary: lib=" + libName);
    }
    if (nativeLibs != null) {
      String [] keys = new String [] { System.mapLibraryName(libName), libName };
      FileArchive fa = null;
      String key = null;
      for (String k : keys) {
        key = k;
        if (debug.classLoader) {
          debug.println(this + "getNativeLibrary: try, " + key);
        }
        fa = nativeLibs.get(key);
        if (fa == null) {
          // Try other non-default lib-extensions
          final String libExtensions = fwCtx.props
              .getProperty(Constants.FRAMEWORK_LIBRARY_EXTENSIONS);
          final int pos = key.lastIndexOf(".");
          if (libExtensions.length() > 0 && pos > -1) {
            final String baseKey = key.substring(0, pos + 1);
            final String[] exts = Util.splitwords(libExtensions, ", \t");
            for (final String ext : exts) {
              key = baseKey + ext;
              if (debug.classLoader) {
                debug.println(this + "getNativeLibrary: try, " + key);
              }
              fa = nativeLibs.get(key);
              if (fa != null) {
                break;
              }
            }
          }
        }
        if (fa != null) {
          break;
        }
      }
      if (fa == null) {
        return null;
      }
      if (debug.classLoader) {
        debug.println(this + "getNativeLibrary: got, " + fa);
      }
      return fa.getNativeLibrary(key);
    }
    return null;
  }


  /**
   *
   */
  @Override
  public String toString() {
    return "BundleClassPath(#" + bid + ").";
  }


  //
  // Private methods
  //

  /**
   *
   */
  private void checkBundleArchive(BundleArchive ba, List<BundleGeneration> frags) {
    final String bcp = ba.getAttribute(Constants.BUNDLE_CLASSPATH);

    if (bcp != null) {
      final StringTokenizer st = new StringTokenizer(bcp, ",");
      while (st.hasMoreTokens()) {
        final String path = st.nextToken().trim();
        FileArchive a = ba.getFileArchive(path);
        if (a == null && frags != null) {
          for (final BundleGeneration bundleGeneration : frags) {
            a = bundleGeneration.archive.getFileArchive(path);
            if (a != null) {
              break;
            }
          }
        }
        if (a != null) {
          archives.add(a);
          if (debug.classLoader) {
            debug.println(this + "- Added path entry: " + a);
          }
        } else {
          fwCtx.frameworkWarning(ba.getBundleGeneration().bundle,
              new IllegalArgumentException(Constants.BUNDLE_CLASSPATH + " entry " + path
                  + " not found in bundle"));
          if (debug.classLoader) {
            debug.println(this + "- Failed to find class path entry: " + path);
          }
        }
      }
    } else {
      archives.add(ba.getFileArchive("."));
    }
  }


  /**
   * Resolve native code libraries.
   *
   * @throws BundleException if native code resolve failed.
   */
  private void resolveNativeCode(BundleGeneration gen) throws BundleException {
    nativeLibs = null;
    if (gen.nativeRequirement != null) {
      List<String> best = gen.nativeRequirement.checkNativeCode();
      if (best != null) {
        nativeLibs = new HashMap<String, FileArchive>();
      bloop:
        for (final String name : best) {
          for (final FileArchive fa : archives) {
            if (!gen.isFragment() || fa.getBundleGeneration() == gen) {
              final String key = fa.checkNativeLibrary(name);
              if (key != null) {
                nativeLibs.put(key, fa);
                if (debug.classLoader) {
                  debug.println(this + "- Registered native library: " + key + " -> " + fa);
                }
                continue bloop;
              }
            }
          }
          nativeLibs = null;
          throw new BundleException("Bundle#" + bid + ", failed to resolve native code: "
                                    + name, BundleException.NATIVECODE_ERROR);
        }
      }
    }
  }

  /**
   * Check if we have native code
   */
  Set<BundleGeneration> hasNativeRequirements() {
    if (nativeLibs != null) {
      Set<BundleGeneration> res = new HashSet<BundleGeneration>();
      for (FileArchive fa : nativeLibs.values()) {
        BundleGeneration bg = fa.getBundleGeneration();
        if (bg.nativeRequirement != null) {
          res.add(bg);
        }
      }
      return res;
    }
    return null;
  }

}
