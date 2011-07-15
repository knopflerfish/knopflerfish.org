/*
 * Copyright (c) 2009-2011, KNOPFLERFISH project
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
import java.util.*;

import org.osgi.framework.*;

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
  private ArrayList /* FileArchive */archives = new ArrayList(4);

  /**
   *
   */
  private Map nativeLibs;

  /**
   *
   */
  private Debug debug;

  /**
   *
   */
  private long bid;


  /**
   * Create class loader for specified bundle.
   * 
   * @throws BundleException if native code resolve failed.
   */
  BundleClassPath(BundleArchive ba, List /* BundleGeneration */frags, FrameworkContext fwCtx)
      throws BundleException {
    this.fwCtx = fwCtx;
    debug = fwCtx.debug;
    bid = ba.getBundleId();
    checkBundleArchive(ba, frags);
    if (frags != null) {
      for (Iterator i = frags.iterator(); i.hasNext();) {
        checkBundleArchive(((BundleGeneration)i.next()).archive, null);
      }
    }
    resolveNativeCode(ba, false);
    if (frags != null) {
      for (Iterator i = frags.iterator(); i.hasNext();) {
        resolveNativeCode(((BundleGeneration)i.next()).archive, true);
      }
    }
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
  Vector componentExists(String component, boolean onlyFirst) {
    Vector v = null;
    if (component.startsWith("/")) {
      component = component.substring(1);
    }
    if (debug.classLoader) {
      debug.println(this + "compentExists: " + component);
    }
    if (0 == component.length()) {
      // The special case asking for "/"
      if (onlyFirst) {
        v = new Vector(1);
        v.addElement(archives.get(0));
        if (debug.classLoader) {
          debug.println(this + "compentExists added first top in classpath.");
        }
      } else {
        v = new Vector(archives);
        if (debug.classLoader) {
          debug.println(this + "compentExists added all tops in classpath.");
        }
      }
    } else {
      for (Iterator i = archives.iterator(); i.hasNext();) {
        FileArchive fa = (FileArchive)i.next();
        InputStream ai = fa.getBundleResourceStream(component);
        if (ai != null) {
          if (v == null) {
            v = new Vector();
          }
          v.addElement(fa);
          if (debug.classLoader) {
            debug.println(this + "compentExists added: " + fa);
          }
          try {
            ai.close();
          } catch (IOException ignore) {
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
   * @param ix index of sub archives. A postive number is the classpath entry
   *          index. 0 means look in the main bundle.
   * @return InputStream to entry or null if it doesn't exist.
   */
  InputStream getInputStream(String component, int ix) {
    if (component.startsWith("/")) {
      component = component.substring(1);
    }
    return ((FileArchive)archives.get(ix)).getBundleResourceStream(component);
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
      FileArchive fa = (FileArchive)nativeLibs.get(key);
      if (fa == null) {
        // Try other non-default lib-extensions
        final String libExtensions = fwCtx.props
            .getProperty(Constants.FRAMEWORK_LIBRARY_EXTENSIONS);
        final int pos = key.lastIndexOf(".");
        if (libExtensions.length() > 0 && pos > -1) {
          final String baseKey = key.substring(0, pos + 1);
          final String[] exts = Util.splitwords(libExtensions, ", \t");
          for (int i = 0; i < exts.length; i++) {
            key = baseKey + exts[i];
            if (debug.classLoader) {
              debug.println(this + "getNativeLibrary: try, " + key);
            }
            fa = (FileArchive)nativeLibs.get(key);
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
  public String toString() {
    return "BundleClassPath(#" + bid + ").";
  }


  //
  // Private methods
  //

  /**
   *
   */
  private void checkBundleArchive(BundleArchive ba, List /* BundleImpl */frags) {
    String bcp = ba.getAttribute(Constants.BUNDLE_CLASSPATH);

    if (bcp != null) {
      StringTokenizer st = new StringTokenizer(bcp, ",");
      while (st.hasMoreTokens()) {
        String path = st.nextToken().trim();
        FileArchive a = ba.getFileArchive(path);
        if (a == null && frags != null) {
          for (Iterator i = frags.iterator(); i.hasNext();) {
            a = ((BundleGeneration)i.next()).archive.getFileArchive(path);
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
    String bnc = ba.getAttribute(Constants.BUNDLE_NATIVECODE);
    if (bnc != null) {
      final ArrayList proc = new ArrayList(3);
      String procP = fwCtx.props.getProperty(Constants.FRAMEWORK_PROCESSOR);
      proc.add(fwCtx.props.getProperty(Constants.FRAMEWORK_PROCESSOR).toLowerCase());
      String procS = System.getProperty("os.arch").toLowerCase();
      if (!procP.equals(procS)) {
        proc.add(procS);
      }
      // Handle deprecated value "arm"
      if (procP.startsWith("arm_")) {
        proc.add("arm");
      }
      final ArrayList os = new ArrayList();
      String osP = fwCtx.props.getProperty(Constants.FRAMEWORK_OS_NAME).toLowerCase();
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
      List best = null;
      VersionRange bestVer = null;
      boolean bestLang = false;

      for (Iterator i = Util
          .parseEntries(Constants.BUNDLE_NATIVECODE, bnc, false, false, false); i.hasNext();) {
        VersionRange matchVer = null;
        boolean matchLang = false;
        Map params = (Map)i.next();

        List keys = (List)params.get("$keys");
        if (keys.size() == 1 && "*".equals(keys.get(0)) && !i.hasNext()) {
          optional = true;
          break;
        }

        List pl = (List)params.get(Constants.BUNDLE_NATIVECODE_PROCESSOR);
        if (pl != null) {
          if (!containsIgnoreCase(proc, pl)) {
            continue;
          }
        } else {
          // NYI! Handle null
          continue;
        }

        List ol = (List)params.get(Constants.BUNDLE_NATIVECODE_OSNAME);
        if (ol != null) {
          if (!containsIgnoreCase(os, ol)) {
            continue;
          }
        } else {
          // NYI! Handle null
          continue;
        }

        List ver = (List)params.get(Constants.BUNDLE_NATIVECODE_OSVERSION);
        if (ver != null) {
          boolean okVer = false;
          for (Iterator v = ver.iterator(); v.hasNext();) {
            // NYI! Handle format Exception
            matchVer = new VersionRange((String)v.next());
            if (matchVer.withinRange(osVer)) {
              okVer = true;
              break;
            }
          }
          if (!okVer) {
            continue;
          }
        }

        List lang = (List)params.get(Constants.BUNDLE_NATIVECODE_LANGUAGE);
        if (lang != null) {
          for (Iterator l = lang.iterator(); l.hasNext();) {
            if (osLang.equalsIgnoreCase((String)l.next())) {
              // Found specfied language version, search no more
              matchLang = true;
              break;
            }
          }
          if (!matchLang) {
            continue;
          }
        }

        List sf = (List)params.get(Constants.SELECTION_FILTER_ATTRIBUTE);
        if (sf != null) {
          String sfs = (String)sf.get(0);
          if (sf.size() == 1) {
            try {
              if (!(FrameworkUtil.createFilter(sfs)).match(fwCtx.props.getProperties())) {
                continue;
              }
            } catch (InvalidSyntaxException ise) {
              throw new BundleException("Invalid syntax for native code selection filter: "
                  + sfs, BundleException.NATIVECODE_ERROR, ise);
            }
          } else {
            throw new BundleException("Invalid character after native code selection filter: "
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
            int d = bestVer.compareTo(matchVer);
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
          throw new BundleException("No matching native code libraries found.",
              BundleException.NATIVECODE_ERROR);
        }
      }
      nativeLibs = new HashMap();
      bloop: for (Iterator p = best.iterator(); p.hasNext();) {
        String name = (String)p.next();
        for (Iterator i = archives.iterator(); i.hasNext();) {
          FileArchive fa = (FileArchive)i.next();
          if (!isFrag || fa.getBundleGeneration().archive == ba) {
            String key = fa.checkNativeLibrary(name);
            if (key != null) {
              nativeLibs.put(key, fa);
              if (debug.classLoader) {
                debug.println(this + "- Registered native library: " + key + " -> " + fa);
              }
              continue bloop;
            }
          }
        }
        throw new BundleException("Failed to resolve native code: " + name,
            BundleException.NATIVECODE_ERROR);
      }
    } else {
      // No native code in this bundle
      nativeLibs = null;
    }
  }


  /**
   * Check if a string exists in a list. Ignore case when comparing.
   */
  private boolean containsIgnoreCase(List fl, List l) {
    for (Iterator i = l.iterator(); i.hasNext();) {
      String s = ((String)i.next()).toLowerCase();
      for (Iterator j = fl.iterator(); j.hasNext();) {
        if (Util.filterMatch((String)j.next(), s)) {
          return true;
        }
      }
    }
    return false;
  }

}
