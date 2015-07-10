/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.osgi.framework.Constants;

/**
 * Manifest subclass which modifies some of the attributes automatically.
 * This can be used to add or remove manifest headers to any loaded bundle. For example,
 * adding package import/export headers.
 *
 * <p>
 * To enable automatic manifest generation, set the system property
 * </p>
 * <pre>
 *  org.knopflerfish.framework.automanifest=true
 * </pre>
 *
 * <p>
 * The automatic manifest config can be specified as an URL using
 * the system property:
 * </p>
 * <pre>
 *  org.knopflerfish.framework.automanifest.config=[URL]
 * </pre>
 *
 * <p>
 * Default is <code>"!!/automanifest.props"</code> The <code>"!!"</code> part is used to refer
 * to a resource on the system classloader, but any URL can be used.
 * </p>
 *
 * <p>
 * The configuration file for automatic manifest generation is on java property
 * file format with any number of sections as:
 * <pre>
 * [id].match.filter=[ldap filter]
 * [id].export.filter=[ldap filter]
 * [id].export.file.filter=[ldap filter]
 * [id].header.[header-name]=[header-value]
 * [id].header.[header-name]=[header-value]
 * [id].header.[header-name]=[header-value]
 * ...
 * </pre>
 *
 * <p>
 * The default config file only has one section, setting all installed
 * bundles to automatically export all of their packages, and dynamically
 * install every class. This is implemented by a single section:
 * </p>
 * <pre>
 * 1.match.filter=(location=*)
 * 1.export.filter=(pkg=*)
 * 1.export.file.filter=(file=*.class)
 * 1.header.DynamicImport-Package=*
 * 1.header.Import-Package=[remove]
 * 1.header.Export-Package=[autoexport]
 * </pre>
 *
 * <p>
 * <code>[id]</code> is any string. The <code>[id]</code> is also used for sorting the sections. Matching is
 * done is this sort order.
 * </p>
 *
 * </p>
 * The <code>match.filter</code> value is an LDAP filter expression used to select
 * which bundles the section applies to. Only the first matching section is applied to
 * a bundle. All original bundle manifest attributes can be used in the matching, plus the
 * special key "location" which represents the bundle location string.
 * </p>
 *
 * <p>
 * The <code>export.filter</code> value is an LDAP filter expression which selects
 * which resources are valid for automatic package export. The special
 * key "pkg" represents the export candidate. E.g. a filter of <code>"(pkg=*)"</code>
 * exports all packages.
 * </p>
 *
 * <p>
 * The <code>export.file.filter</code> value is an optional LDAP filter expression which selects
 * which files are considered for auto package generation. The special key
 * key "file" represents the file candidate. E.g. a filter of <code>"(name=*)"</code>
 * considers all files. Default is <code>"(file=*.class)</code>
 * </p>
 *
 * <p>
 * The <code>[header-name]</code> values are stored directly in the generated
 * manifest. Any number of header names can be specified.
 * </p>
 *
 * <p>
 * Two special header values can be used:
 * </p>
 * <pre>
 *  "[remove]"     -  Removes the header completely from the manifest
 *  "[autoexport]" -  Uses the generated package names as value.
 *                    Package names are only present if the passes
 *                    the export.filter
 * </pre>
 * <p>
 * <b>Note 1</b>: An extra attribute
 *       <code>"Bundle-AutoManifest-config"</code>
 *       is always added to the manifest. The value is the URL of the config file
 * </p>
 *
 * <p>
 * <b>Note 2</b>: Debug output of automatic manifest can be enabled by setting the system property
 * </p>
 * <pre>
 * org.knopflerfish.framework.debug.automanifest=true
 * </pre>
 *
 * @author Erik Wistrand
 */
public class AutoManifest extends Manifest {
  FrameworkContext fwCtx;
  Manifest   mf;
  String     location;
  AutoInfo   autoInfo;
  Attributes mainAttrs;
  Set<String> packages = new TreeSet<String>();

  static String configSource = null;
  static Map<String, AutoInfo>    configs      = null;

  /**
   * Create an AutoManifest instance based on an original manifest
   * and a bundle location.
   *
   * <p>
   * An AutoManifest instance is a proxy to the original manifest except in the
   * case of getting the main attributes. In this case the result may be modified
   * according the auto manifest configuration file.
   * </p>
   *
   * <p>
   * Note: The first time an AutoManifest instance is created, the configuration
   * file is read.
   * </p>
   *
   * @param mf original manifest. Must not be null.
   * @param location bundle location. Must not be null.
   */
  public AutoManifest(FrameworkContext fwCtx, Manifest mf, String location) {
    if(mf == null) {
      throw new NullPointerException("Manifest cannot be null");
    }

    if(location == null) {
      throw new NullPointerException("location cannot be null");
    }

    this.fwCtx = fwCtx;
    this.mf       = mf;
    this.location = location;

    // just read the config once
    if(configs == null) {
      if(fwCtx.props.getBooleanProperty(FWProps.AUTOMANIFEST_PROP)) {
        configSource = fwCtx.props.getProperty(FWProps.AUTOMANIFEST_CONFIG_PROP);
        configs      = loadConfig(configSource);
        if(fwCtx.debug.automanifest) {
          fwCtx.debug.println("Loaded auto manifest config from " + configSource);
        }
      } else {
        configs = new TreeMap<String, AutoInfo>();
      }
    }

    autoInfo = findConfig();

    if(isAuto() && fwCtx.debug.automanifest) {
      fwCtx.debug.println("Using auto manifest for bundlelocation " + location);
    }
  }

  /**
   * Check if manifest generation is enabled for this instance.
   */
  public boolean isAuto() {
    return autoInfo != null;
  }


  /**
   * Delegate to original manifest.
   */
  @Override
  public void 	clear() {
    mf.clear();
    mainAttrs = null;
  }

  /**
   * Delegate to original manifest.
   */
  @Override
  public  Attributes 	getAttributes(String name) {
    return mf.getAttributes(name);
  }

  /**
   * Delegate to original manifest.
   */
  @Override
  public Map<String,Attributes> getEntries()
  {
    return mf.getEntries();
  }

  /**
   * Delegate to original manifest.
   */
  @Override
  public  void 	read(InputStream is) throws IOException {
    mf.read(is);
    mainAttrs = null;
  }

  /**
   * Delegate to original manifest.
   */
  @Override
  public  void 	write(OutputStream out) throws IOException {
    mf.write(out);
  }

  /**
   * Get the original manifest
   */
  public Manifest getManifest() {
    return mf;
  }

  /**
   * Get the bundle location
   */
  public String getLocation() {
    return location;
  }


  /**
   * AutoManifests are equal if both the original manifest
   * and the bundle location are equal.
   */
  @Override
  public boolean equals(Object obj) {
    if(obj == null || !(obj instanceof AutoManifest)) {
      return false;
    }
    final AutoManifest af = (AutoManifest)obj;
    return mf.equals(af.mf) && location.equals(af.location);
  }

  /**
   * Hash code base on original manifest and loation
   */
  @Override
  public int hashCode() {
    return mf.hashCode() + 17 * location.hashCode();
  }


  /**
   * Get the main manifest attributes, possibly modified.
   */
  @Override
  public  Attributes 	getMainAttributes() {
    if(mainAttrs == null) {

      // Get the original attribs and modify them according to
      // autoInfo config (if present)
      mainAttrs = mf.getMainAttributes();


      if(autoInfo != null) {
        // store a flag for debug purposes
        mainAttrs.putValue("Bundle-AutoManifest-config", configSource);

        for (final String key : autoInfo.headers.keySet()) {
          final String val = autoInfo.headers.get(key);

          if("[remove]".equals(val)) {
            mainAttrs.remove(new Attributes.Name(key));
          } else if("[autoexport]".equals(val)) {
            final String exports = getExports();

            if(fwCtx.debug.automanifest) {
              fwCtx.debug.println("Auto exports for " + location + ": " + exports);
            }

            if(exports.length() > 0) {
              mainAttrs.putValue(Constants.EXPORT_PACKAGE, exports);
            } else {
              mainAttrs.remove(new Attributes.Name(Constants.EXPORT_PACKAGE));
            }
          } else {
            mainAttrs.putValue(key, val);
          }
        }
      }
    }
    return mainAttrs;
  }

  /**
   * Add a File for automatic package export consideration.
   */
  public void addFile(File file) throws IOException {
    addFile(file.getAbsolutePath(), file);
  }

  /**
   * Add a File for automatic package export consideration.
   */
  public void addFile(String prefix, File file) throws IOException {
    final String f = prefix.length() < file.getAbsolutePath().length()
      ? file.getAbsolutePath().substring(prefix.length() + 1)
      : file.getAbsolutePath();

    if(f.endsWith(".jar")) {
      addZipFile(new ZipFile(file));
    } else if(isValidFileName(f)) {
      addFileName(f);
    } else if(file.isDirectory()) {
      final String[] files = file.list();
      for (final String file2 : files) {
        addFile(prefix, new File(file.getAbsolutePath(), file2));
      }
    }
  }

  // used to avoid recreating a new hash table at each isValidFileName() call
  private final Hashtable<String, String> fileProps = new Hashtable<String, String>();

  private boolean isValidFileName(String f) {
    if(autoInfo != null && autoInfo.fileNameFilter != null) {
      fileProps.put("file", f);
      return autoInfo.fileNameFilter.evaluate(fileProps, true);
    } else {
      return f.endsWith(".class");
    }
  }

  /**
   * Add contents of a ZipFile for automatic package export consideration.
   */
  public void addZipFile(ZipFile jar) {
    for(final Enumeration<? extends ZipEntry> e = jar.entries(); e.hasMoreElements(); ) {
      final ZipEntry ze = e.nextElement();
      final String   f  = ze.getName();
      if(isValidFileName(f)) {
        addFileName(f);
      }
    }
  }

  /**
   * Add a string file name for automatic package export consideration.
   */
  public void addFileName(String f) {

    // transform file path to package name
    f = f.replace('\\', '/');

    // ..and strip last part
    final int ix = f.lastIndexOf("/");
    if(ix != -1) {
      f = f.substring(0, ix);
    }
    f = f.replace('/', '.');

    // check all export patterns if the file matches.
    // If so, add to packages set
    if(autoInfo != null) {
      final Hashtable<String, String> props = new Hashtable<String, String>();
      props.put("pkg", f);
      if(autoInfo.exportFilter.evaluate(props, true)) {
        if(!packages.contains(f)) {
          packages.add(f);
        }
      }
    }

    // clearing mainAttrs will force rebuild of attribut set in getMainAttributes()
    mainAttrs = null;
  }




  /***
   * Create Export-Package string from package set
   * The package set is created by addFileName() calls
   */
  String getExports() {
    final StringBuffer sb = new StringBuffer();
    for (final String pkg : packages) {
      if(sb.length() > 0) {
        sb.append(",");
      }
      sb.append(pkg);
      if(autoInfo.version != null) {
        sb.append(";");
        sb.append(autoInfo.version);
      }
    }
    return sb.toString();
  }


  private AutoInfo findConfig() {
    final Hashtable<String, String> props = new Hashtable<String, String>();
    props.put("location", location);

    final Attributes attrs = mf.getMainAttributes();
    for (final Object key : attrs.keySet()) {
      final Object val = attrs.getValue(key.toString());
      props.put(key.toString(), val.toString());
    }

    for (final String id : configs.keySet()) {
      final AutoInfo ai = configs.get(id);

      if(ai.filter.evaluate(props, true)) {
        return ai;
      }
    }
    return null;
  }

  private Map<String, AutoInfo> loadConfig(String urlS) {
    if(urlS != null && !"".equals(urlS)) {
      URL         url = null;
      InputStream is  = null;
      try {
        if(urlS.startsWith("!!")) {
          url = AutoManifest.class.getResource(urlS.substring(2));
        } else {
          url = new URL(urlS);
        }
        is = url.openStream();
        return loadConfigFromInputStream(is);
      } catch (final Exception e) {
        fwCtx.debug.printStackTrace("Failed to load autoimportexport conf from " + url, e);
      } finally {
        try { is.close(); } catch (final Exception ignored) { }
      }
    }
    return new HashMap<String, AutoInfo>();
  }

  private Map<String, AutoInfo> loadConfigFromInputStream(InputStream is)
      throws IOException
  {
    final Properties props = new Properties();
    props.load(is);

    final Map<String, AutoInfo> configMap = new TreeMap<String, AutoInfo>();

    for (final Object object : props.keySet()) {
      final String key = (String) object;
      final int ix = key.indexOf(".");
      if(ix != -1) {
        final String id = key.substring(0, ix);

        final String pattern     = (String)props.get(id + ".match.filter");
        final String export      = (String)props.get(id + ".export.filter");
        String fileFilter  = (String)props.get(id + ".export.file.filter");
        final Map<String, String>    headers     = new HashMap<String, String>();

        if(fileFilter == null) {
          fileFilter = "(file=*.class)";
        }

        final String headerPre = id + ".header.";
        for (final Object object2 : props.keySet()) {
          final String key2 = (String) object2;
          final String val2 = (String) props.get(key2);
          if(key2.startsWith(headerPre)) {
            headers.put(key2.substring(headerPre.length()), val2);
          }
        }
        final AutoInfo ai = new AutoInfo(pattern, export, fileFilter, headers);
        ai.version = (String)props.get(id + ".export.version");
        configMap.put(id, ai);
      }
    }
    return configMap;
  }


  /**
   * Helper structure to hold configuration info.
   */
  static class AutoInfo {
    LDAPExpr filter;
    LDAPExpr exportFilter;
    LDAPExpr fileNameFilter;
    Map<String, String>      headers;
    String   version;

    AutoInfo(String filter, String export, String nameFilter, Map<String, String> headers) {
      try {
        this.filter  = new LDAPExpr(filter);
      } catch (final Exception e) {
        throw new RuntimeException("Bad filter '" + filter + "': " +e);
      }
      try {
        this.fileNameFilter  = new LDAPExpr(nameFilter);
      } catch (final Exception e) {
        throw new RuntimeException("Bad file name filter '" + nameFilter
                                   + "': " +e);
      }
      try {
        this.exportFilter  = new LDAPExpr(export);
      } catch (final Exception e) {
        throw new RuntimeException("Bad export filter '" + export + "': " +e);
      }
      this.headers = headers;
    }

    @Override
    public String toString() {
      return "AutoInfo[" +
        "filter=" + filter +
        ", exportFilter="+ exportFilter +
        ", version=" + version +
        ", headers=" + headers +
        "]";
    }
  }

}
