/*
 * Copyright (c) 2009-2013, KNOPFLERFISH project
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;

import org.knopflerfish.framework.Util.HeaderEntry;

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
  BundleClassPath(BundleArchive ba, List<BundleGeneration> frags, FrameworkContext fwCtx)
      throws BundleException {
    this.fwCtx = fwCtx;
    debug = fwCtx.debug;
    bid = ba.getBundleId();
    checkBundleArchive(ba, frags);
    if (frags != null) {
      for (final BundleGeneration bundleGeneration : frags) {
        checkBundleArchive(bundleGeneration.archive, null);
      }
    }
    resolveNativeCode(ba, false);
    if (frags != null) {
      for (final BundleGeneration bundleGeneration : frags) {
        resolveNativeCode(bundleGeneration.archive, true);
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
    resolveNativeCode(gen.archive, true);
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
      debug.println(this + "compentExists: " + component);
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
      String key = System.mapLibraryName(libName);
      if (debug.classLoader) {
        debug.println(this + "getNativeLibrary: try, " + key);
      }
      FileArchive fa = nativeLibs.get(key);
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
        if (fa == null) {
          return null;
        }
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
          fwCtx.listeners.frameworkWarning(ba.getBundleGeneration().bundle,
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
  private void resolveNativeCode(BundleArchive ba, boolean isFrag) throws BundleException {
    final String bnc = ba.getAttribute(Constants.BUNDLE_NATIVECODE);
    if (bnc != null) {
      final ArrayList<String> proc = new ArrayList<String>(3);
      final String procP = fwCtx.props.getProperty(Constants.FRAMEWORK_PROCESSOR);
      proc.add(fwCtx.props.getProperty(Constants.FRAMEWORK_PROCESSOR).toLowerCase());
      final String procS = System.getProperty("os.arch").toLowerCase();
      if (!procP.equals(procS)) {
        proc.add(procS);
      }
      // Handle deprecated value "arm"
      if (procP.startsWith("arm_")) {
        proc.add("arm");
      }
      final ArrayList<String> os = new ArrayList<String>();
      final String osP = fwCtx.props.getProperty(Constants.FRAMEWORK_OS_NAME).toLowerCase();
      os.add(osP);
      String osS = System.getProperty("os.name").toLowerCase();
      if (!osS.equals(osP)) {
        os.add(osS);
      } else {
        osS = null;
      }
      for (int i = 0; i < Alias.osNameAliases.length; i++) {
        if (osP.equalsIgnoreCase(Alias.osNameAliases[i][0])) {
          for (int j = 1; j < Alias.osNameAliases[i].length; j++) {
            if (osS == null || !osS.equals(Alias.osNameAliases[i][j])) {
              os.add(Alias.osNameAliases[i][j]);
            } else {
              osS = null;
            }
          }
          break;
        }
      }
      final Version osVer = new Version(fwCtx.props.getProperty(Constants.FRAMEWORK_OS_VERSION));
      final String osLang = fwCtx.props.getProperty(Constants.FRAMEWORK_LANGUAGE);
      boolean optional = false;
      List<String> best = null;
      VersionRange bestVer = null;
      boolean bestLang = false;

      final List<HeaderEntry> hes = Util
          .parseManifestHeader(Constants.BUNDLE_NATIVECODE, bnc, false, false,
                               false);
      for (final Iterator<HeaderEntry> heIt = hes.iterator(); heIt.hasNext();) {
        final HeaderEntry he = heIt.next();
        VersionRange matchVer = null;
        boolean matchLang = false;

        final
        List<String> keys = he.getKeys();
        if (keys.size() == 1 && "*".equals(keys.get(0)) && !heIt.hasNext()) {
          optional = true;
          break;
        }

        @SuppressWarnings("unchecked")
        final
        List<String> pl = (List<String>) he.getAttributes().get(Constants.BUNDLE_NATIVECODE_PROCESSOR);
        if (pl != null) {
          if (!containsIgnoreCase(proc, pl)) {
            continue;
          }
        } else {
          // NYI! Handle null
          continue;
        }

        @SuppressWarnings("unchecked")
        final
        List<String> ol = (List<String>) he.getAttributes().get(Constants.BUNDLE_NATIVECODE_OSNAME);
        if (ol != null) {
          if (!containsIgnoreCase(os, ol)) {
            continue;
          }
        } else {
          // NYI! Handle null
          continue;
        }

        @SuppressWarnings("unchecked")
        final
        List<String> ver = (List<String>) he.getAttributes().get(Constants.BUNDLE_NATIVECODE_OSVERSION);
        if (ver != null) {
          boolean okVer = false;
          for (final String string : ver) {
            // NYI! Handle format Exception
            matchVer = new VersionRange(string);
            if (matchVer.withinRange(osVer)) {
              okVer = true;
              break;
            }
          }
          if (!okVer) {
            continue;
          }
        }

        @SuppressWarnings("unchecked")
        final
        List<String> lang = (List<String>) he.getAttributes().get(Constants.BUNDLE_NATIVECODE_LANGUAGE);
        if (lang != null) {
          for (final String string : lang) {
            if (osLang.equalsIgnoreCase(string)) {
              // Found specified language version, search no more
              matchLang = true;
              break;
            }
          }
          if (!matchLang) {
            continue;
          }
        }

        @SuppressWarnings("unchecked")
        final
        List<String> sf = (List<String>) he.getAttributes().get(Constants.SELECTION_FILTER_ATTRIBUTE);
        if (sf != null) {
          final String sfs = sf.get(0);
          if (sf.size() == 1) {
            try {
              if (!(FrameworkUtil.createFilter(sfs)).match(fwCtx.props.getProperties())) {
                continue;
              }
            } catch (final InvalidSyntaxException ise) {
              throw new BundleException("Bundle#" + bid +
                                        ", Invalid syntax for native code selection filter: "
                                        + sfs, BundleException.NATIVECODE_ERROR, ise);
            }
          } else {
            throw new BundleException("Bundle#" + bid +
                                      ", Invalid character after native code selection filter: "
                                      + sfs, BundleException.NATIVECODE_ERROR);
          }
        }

        // Compare to previous best
        if (best != null) {
          boolean verEqual = false;
          if (bestVer != null) {
            if (matchVer == null) {
              continue;
            }
            final int d = bestVer.compareTo(matchVer);
            if (d == 0) {
              verEqual = true;
            } else if (d > 0) {
              continue;
            }
          } else if (matchVer == null) {
            verEqual = true;
          }
          if (verEqual && (!matchLang || bestLang)) {
            continue;
          }
        }
        best = keys;
        bestVer = matchVer;
        bestLang = matchLang;
      }
      if (best == null) {
        if (optional) {
          return;
        } else {
          throw new BundleException("Bundle#" + bid +
                                    ", no matching native code libraries found for os="
                                    + os + " version=" + osVer + ", processor="
                                    + proc + " and language=" + osLang + ".",
                                    BundleException.NATIVECODE_ERROR);
        }
      }
      nativeLibs = new HashMap<String, FileArchive>();
      bloop: for (final String name : best) {
        for (final FileArchive fa : archives) {
          if (!isFrag || fa.getBundleGeneration().archive == ba) {
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
        throw new BundleException("Bundle#" + bid + ", failed to resolve native code: "
                                  + name, BundleException.NATIVECODE_ERROR);
      }
    } else {
      // No native code in this bundle
      nativeLibs = null;
    }
  }


  /**
   * Check if a string exists in a list. Ignore case when comparing.
   */
  private boolean containsIgnoreCase(List<String> fl, List<String> l) {
    for (final String string : l) {
      final String s = string.toLowerCase();
      for (final String string2 : fl) {
        if (Util.filterMatch(string2, s)) {
          return true;
        }
      }
    }
    return false;
  }

}
