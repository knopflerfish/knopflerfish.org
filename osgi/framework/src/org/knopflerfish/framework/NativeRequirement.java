/*
 * Copyright (c) 2016-2016, KNOPFLERFISH project
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.namespace.NativeNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

import org.knopflerfish.framework.Util.HeaderEntry;

/**
 * Data structure for native code requirement handling.
 *
 * @author Jan Stein
 */
class NativeRequirement
  extends DTOId
  implements BundleRequirement
{
  final BundleGeneration gen;
  final List<NativeCodeEntry> entries = new ArrayList<NativeCodeEntry>();
  private boolean optional = false;


  NativeRequirement(final BundleGeneration gen, String bnc)
  {
    this.gen = gen;
    final List<HeaderEntry> hes =
      Util.parseManifestHeader(Constants.BUNDLE_NATIVECODE, bnc, false, false, false);
    for (final Iterator<HeaderEntry> heIt = hes.iterator(); heIt.hasNext();) {
      final HeaderEntry he = heIt.next();

      final List<String> keys = he.getKeys();
      if ("*".equals(keys.get(0))) {
        if (keys.size() != 1 || heIt.hasNext()) {
          throw new IllegalArgumentException("Bundle#" + gen.bundle.id +
                                             ", Unexpected characters after '*' in " + 
                                             Constants.BUNDLE_NATIVECODE + " header: " + bnc);
        }
        optional = true;
        break;
      }

      @SuppressWarnings("unchecked")
        final List<String> procs = (List<String>) he.getAttributes().get(Constants.BUNDLE_NATIVECODE_PROCESSOR);

      @SuppressWarnings("unchecked")
        final  List<String> oses = (List<String>) he.getAttributes().get(Constants.BUNDLE_NATIVECODE_OSNAME);

      @SuppressWarnings("unchecked")
        final List<String> vers = (List<String>) he.getAttributes().get(Constants.BUNDLE_NATIVECODE_OSVERSION);

      @SuppressWarnings("unchecked")
        final List<String> langs = (List<String>) he.getAttributes().get(Constants.BUNDLE_NATIVECODE_LANGUAGE);

      @SuppressWarnings("unchecked")
        final List<String> sfs = (List<String>) he.getAttributes().get(Constants.SELECTION_FILTER_ATTRIBUTE);
      String sf = null;
      if (sfs != null) {
        sf = sfs.get(0);
        if (sfs.size() != 1) {
          throw new IllegalArgumentException("Bundle#" + gen.bundle.id +
                                             ", Invalid character after native code selection filter: " + sf);
        }
      }
      try {
        entries.add(new NativeCodeEntry(keys, procs, oses, vers, langs, sf));
      } catch (InvalidSyntaxException ise) {
        throw new IllegalArgumentException("Bundle does not specify a valid " + Constants.BUNDLE_NATIVECODE +
                                           " header. Got exception: " + ise.getMessage());
      }
    }
  }

  /**
   * Check native code attribute.
   * 
   * @return 
   *
   * @throws BundleException if native code match fails.
   */
  List<String> checkNativeCode() throws BundleException {
    List<String> best = null;
    Version bestVer = null;
    boolean bestLang = false;
    Map<String,Object> nca = gen.bundle.fwCtx.systemBundle.getNativeCapability().getAttributes();
    for (NativeCodeEntry ne : entries) {
      if (ne.filter == null || ne.filter.matches(nca)) {
        // Compare to previous best
        if (best != null) {
          boolean verEqual = false;
          if (bestVer != null) {
            if (ne.minVersion == null) {
              continue;
            }
            final int d = bestVer.compareTo(ne.minVersion);
            if (d == 0) {
              verEqual = true;
            } else if (d > 0) {
              continue;
            }
          } else if (ne.minVersion == null) {
            verEqual = true;
          }
          if (verEqual && (!ne.matchLang || bestLang)) {
            continue;
          }
        }
        best = ne.files;
        bestVer = ne.minVersion;
        bestLang = ne.matchLang;
      }
    }
    if (best == null && !optional) {
      StringBuffer sb = new StringBuffer();
      sb.append("Bundle#").append(gen.bundle.id);
      sb.append(", no matching native code libraries, filters=[");
      boolean first = true;
      for (NativeCodeEntry ne : entries) {
        if (first) {
          first = false;
        } else {
          sb.append(", ");
        }
        sb.append(ne.filter);
      }
      sb.append("], attributes: ").append(nca);
      throw new BundleException(sb.toString(), BundleException.NATIVECODE_ERROR);
    }
    return best;
  }


  /**
   * String describing this object.
   *
   * @return String.
   */
  @Override
  public String toString() {
    return "NativeRequirement[filter=" + getFilter() + "]";
  }


  // BundleRequirement method
  @Override
  public String getNamespace() {
    return NativeNamespace.NATIVE_NAMESPACE;
  }


  // BundleRequirement method
  @Override
  public Map<String, String> getDirectives() {
    String filter = getFilter();
    if (filter != null) {
      if (optional) {
        Map<String, String> res = new TreeMap<String, String>();
        res.put(Constants.FILTER_DIRECTIVE, filter);
        res.put(Constants.RESOLUTION_DIRECTIVE, Constants.RESOLUTION_OPTIONAL);
        return res;
      } else {
        return Collections.singletonMap(Constants.FILTER_DIRECTIVE, filter);
      }
    } else {
      @SuppressWarnings("unchecked")
      Map<String, String> res = Collections.EMPTY_MAP;
      return res;
    }
  }


  // BundleRequirement method
  @Override
  public Map<String, Object> getAttributes() {
    @SuppressWarnings("unchecked")
    final Map<String, Object> res = Collections.EMPTY_MAP;
    return res;
  }


  // BundleRequirement method
  @Override
  public BundleRevision getResource() {
    return gen.bundleRevision;
  }


  // BundleRequirement method
  @Override
  public BundleRevision getRevision() {
    return gen.bundleRevision;
  }


  // BundleRequirement method
  @Override
  public boolean matches(BundleCapability capability) {
    if (BundleRevision.PACKAGE_NAMESPACE.equals(capability.getNamespace())) {
      for (NativeCodeEntry e : entries) {
        if (e.filter == null || e.filter.matches(capability.getAttributes())) {
          return true;
        }
      }
    }
    return false;
  }


  private String getFilter() {
    StringBuffer sb = new StringBuffer();
    int elems = 0;
    for (NativeCodeEntry e : entries) {
      if (e.filter != null) {
        sb.append(e.filter.toString());
        elems++;
      }
    }
    if (elems == 0) {
      return null;
    } else if (elems > 1) {
      sb.insert(0,"(|");
      sb.append(")");
    }
    return sb.toString();
  }


  class NativeCodeEntry
  {
    final List<String> files;
    final Filter filter;
    final boolean matchLang;
    final Version minVersion;


    NativeCodeEntry(List<String> files, List<String> procs, List<String> oses, List<String> vers, List<String> langs, String sf)
      throws InvalidSyntaxException
    {
      this.files = files;
      matchLang = langs != null;
      if (vers != null) {
        Version mv = null;
        List<VersionRange> vrs = new ArrayList<VersionRange>(vers.size());
        for (String s : vers) {
          VersionRange vr = new VersionRange(s);
          if (mv == null || mv.compareTo(vr.getLeft()) > 0) {
            mv = vr.getLeft();
          }
          vrs.add(vr);
        }
        minVersion = mv;
        filter = toFilter(procs, oses, vrs, langs, sf);
      } else {
        minVersion = null;
        filter = toFilter(procs, oses, null, langs, sf);
      }
    }


    private String orString(String key, List<?> vals) {
      if (vals == null) {
        return null;
      }
      final StringBuffer sb = new StringBuffer(80);
      if (vals.size() > 1) {
        sb.append("(|");
      }
      for (Object v : vals) {
        if (v instanceof VersionRange) {
          sb.append(((VersionRange)v).toFilterString(key));
        } else {
          sb.append('(');
          sb.append(key);
          sb.append("~=");
          sb.append(v.toString());
          sb.append(')');
        }
      }
      if (vals.size() > 1) {
        sb.append(')');
      }
      return sb.toString();
    }


    private int andAdd(StringBuffer sb, String str) {
      if (str == null) {
        return 0;
      }
      sb.append(str);
      return 1;
    }
        
    private Filter toFilter(List<String> procs, List<String> oses, List<VersionRange> vers, List<String> langs, String sf)
      throws InvalidSyntaxException
    {
      final StringBuffer sb = new StringBuffer(80);
      int elems = 0;
      
      elems = andAdd(sb, orString(NativeNamespace.CAPABILITY_PROCESSOR_ATTRIBUTE, procs));
      elems += andAdd(sb, orString(NativeNamespace.CAPABILITY_OSNAME_ATTRIBUTE, oses));
      elems += andAdd(sb, orString(NativeNamespace.CAPABILITY_OSVERSION_ATTRIBUTE, vers));
      elems += andAdd(sb, orString(NativeNamespace.CAPABILITY_LANGUAGE_ATTRIBUTE, langs));
      elems += andAdd(sb, sf);
      if (elems == 0) {
        return null;
      } else if (elems > 1) {
        sb.insert(0,"(&");
        sb.append(")");
      }
      return FrameworkUtil.createFilter(sb.toString());
    }

  }

}
