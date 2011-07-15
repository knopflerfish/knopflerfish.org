/*
 * Copyright (c) 2010-2011, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
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

package org.knopflerfish.ant.taskdefs.bundle;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * A class that analyzes all bundle jar files given by a list of resource
 * collections (file sets).
 *
 */
public class BundleArchives {

  // This constant is missing from org.osgi.framework.Constants
  public static final String BUNDLE_BLUEPRINT = "Bundle-Blueprint";
  public static final String BUNDLE_LICENSE = "Bundle-License";
  public static final String SERVICE_COMPONENT = "Service-Component";

  /** Path prefix for source files included into the bundle archive. */
  public static final String SRC_PREFIX = "OSGI-OPT/src";

  /**
   * The ant task that is using this helper class. Used for logging etc.
   */
  private final Task task;

  /**
   * Map from bundle name (the bundles file without version and ".jar" suffixes)
   * to a sorted set of a bundle archive object with details about the bundle .
   * The set will contain more than one element when more than one version of
   * the bundle has been found.
   */
  final Map bnToBundleArchives = new TreeMap();

  /**
   * Map from bundle symbolic name to a sorted set of a bundle archive object
   * with details about the bundle. The set will contain more than one element
   * when more than one version of the bundle has been found.
   */
  final Map bsnToBundleArchives = new TreeMap();

  /**
   * Set with all bundle archives specified by the given resource collections.
   */
  final SortedSet allBundleArchives = new TreeSet();

  /**
   * Set with all packages exported from some bundle in this collection of
   * bundle archives. The key in the map is the package name, the value is a map
   * from package version to a set of bundle archives that exports that version
   * of the package.
   *
   * <p>
   * A call to {@link #doProviders()} must be made to compute this mapping.
   * </p>
   */
  final SortedMap allExports = new TreeMap();

  /**
   * Traverse the given list of resource collections and create
   * <code>BundleArchive</code>-objects for all bundle jars found.
   *
   * @param task
   *          The task that uses this class, used for logging and project
   *          access.
   * @param resourceCollections
   *          The collection of resource collections selecting the bundle
   *          archives to load.
   */
  public BundleArchives(final Task task, final List resourceCollections) {
    this(task, resourceCollections, false);
  }

  /**
   * Traverse the given list of resource collections and create
   * <code>BundleArchive</code>-objects for all bundle jars found.
   *
   * @param task
   *          The task that uses this class, used for logging and project
   *          access.
   * @param resourceCollections
   *          The collection of resource collections selecting the bundle
   *          archives to load.
   * @param parseExportImport
   *          If <code>true</code> then the created bundle archive objects will
   *          parse the import / export package / service headers.
   */
  public BundleArchives(final Task task, final List resourceCollections,
                        final boolean parseExportImport) {
    this.task = task;

    if (resourceCollections.size() == 0) {
      task.log("BundleArchives called without any bundle archives to analyse",
               Project.MSG_ERR);
      throw new BuildException("No resource collections specified");
    }

    try {
      for (Iterator it = resourceCollections.iterator(); it.hasNext();) {
        final ResourceCollection rc = (ResourceCollection) it.next();

        // Ignore file sets with a non existing root dir.
        if (rc instanceof FileSet) {
          final FileSet fs = (FileSet) rc;
          final File fsRootDir = fs.getDir(task.getProject());
          if (!fsRootDir.exists()) {
            task.log("Skipping nested file set rooted at '" + fsRootDir
                + "' since that directory does not exist.", Project.MSG_WARN);
            continue;
          }
          try {
            if (fs.size()<1) {
              task.log("Skipping nested file set rooted at '" + fsRootDir
                  + "' since that file set is empty.", Project.MSG_VERBOSE);
              continue;
              
            }
          } catch (Exception e) {
            task.log("Skipping nested file set rooted at '" + fsRootDir
                + "' since size computation throws exception.", e, Project.MSG_VERBOSE);
            continue;
          }
        }

        for (Iterator rcIt = rc.iterator(); rcIt.hasNext();) {
          final Resource res = (Resource) rcIt.next();
          if (res.getName().endsWith(".jar")) {
            task.log("Adding bundle: " + res, Project.MSG_VERBOSE);
            final BundleArchive ba = new BundleArchive(task,
                                                       (FileResource) res, parseExportImport);
            allBundleArchives.add(ba);
            addToMap(bnToBundleArchives, ba.bundleName, ba);
            addToMap(bsnToBundleArchives, ba.bsn, ba);
          }
        }
      }// Scan done

    } catch (BuildException be) {
      throw be;
    } catch (Exception e) {
      final String msg = "Failed to analyze bundle archives: " + e;
      task.log(msg);
      throw new BuildException(msg, e);
    }
  }

  SortedSet getKnownNames() {
    final TreeSet knownNames = new TreeSet(bnToBundleArchives.keySet());
    knownNames.addAll(bsnToBundleArchives.keySet());
    return knownNames;
  }

  /**
   * Computes the global <code>allExports</code> mapping and the
   * <code>pkgProvidersMap</code> for all bundle archives.
   */
  void doProviders() {
    // First build the allExports structure.
    allExports.clear();
    for (Iterator itBa = allBundleArchives.iterator(); itBa.hasNext();) {
      final BundleArchive ba = (BundleArchive) itBa.next();
      // Clear bundle archive maps holding results from this analysis
      ba.pkgProvidersMap.clear();
      ba.pkgUnprovidedMap.clear();
      ba.pkgProvidedMap.clear();

      final Iterator itEe = ba.pkgExportMap.entrySet().iterator();
      while (itEe.hasNext()) {
        final Map.Entry eE = (Map.Entry) itEe.next();
        final String pkgName = (String) eE.getKey();
        final Version pkgVersion = (Version) eE.getValue();

        SortedMap versions = (SortedMap) allExports.get(pkgName);
        if (null == versions) {
          versions = new TreeMap();
          allExports.put(pkgName, versions);
        }

        SortedSet exporters = (SortedSet) versions.get(pkgVersion);
        if (null == exporters) {
          exporters = new TreeSet();
          versions.put(pkgVersion, exporters);
        }
        exporters.add(ba);
      }
    }

    // For each bundle build the pkgProvidersMap
    for (Iterator itBa = allBundleArchives.iterator(); itBa.hasNext();) {
      final BundleArchive ba = (BundleArchive) itBa.next();

      final Iterator itIe = ba.pkgImportMap.entrySet().iterator();
      while (itIe.hasNext()) {
        final Map.Entry iE = (Map.Entry) itIe.next();
        final String pkgName = (String) iE.getKey();
        final VersionRange range = (VersionRange) iE.getValue();

        SortedMap versions = (SortedMap) allExports.get(pkgName);
        if (null != versions) {
          final Iterator itV = versions.entrySet().iterator();
          while (itV.hasNext()) {
            final Map.Entry vE = (Map.Entry) itV.next();
            final Version pkgVersion = (Version) vE.getKey();

            if (range.contains(pkgVersion)) {
              final SortedSet providers = (SortedSet) vE.getValue();

              final Iterator itP = providers.iterator();
              while (itP.hasNext()) {
                final BundleArchive provider = (BundleArchive) itP.next();

                // The package pkgName may be imported by ba from provider,
                // update ba's providers map
                SortedSet pkgNames = (SortedSet) ba.pkgProvidersMap
                  .get(provider);
                if (null == pkgNames) {
                  pkgNames = new TreeSet();
                  ba.pkgProvidersMap.put(provider, pkgNames);
                }
                pkgNames.add(pkgName);

                // Non self exported package, add to pkgCtProvidersMap
                if (!ba.pkgExportMap.containsKey(pkgName)) {
                  SortedSet pkgNamesCt = (SortedSet) ba.pkgCtProvidersMap
                    .get(provider);
                  if (null == pkgNamesCt) {
                    pkgNamesCt = new TreeSet();
                    ba.pkgCtProvidersMap.put(provider, pkgNamesCt);
                  }
                  pkgNamesCt.add(pkgName);
                }

                // The package pkgName is provided (exported) by
                // provider to ba, update provider.pkgProvidedMap to
                // reflect this.
                SortedSet pkgNamesProvidedToBa = (SortedSet) provider.pkgProvidedMap
                  .get(ba);
                if (null == pkgNamesProvidedToBa) {
                  pkgNamesProvidedToBa = new TreeSet();
                  provider.pkgProvidedMap.put(ba, pkgNamesProvidedToBa);
                }
                pkgNamesProvidedToBa.add(pkgName);
              }
            }
          }
        } else {
          ba.pkgUnprovidedMap.put(pkgName, range);
          task.log(ba + " importing no provider for package " + pkgName + " "
                   + range, Project.MSG_DEBUG);
        }
      }
    }
  }

  /**
   * Format the OSGi version as Maven 2 does in versioned file names.
   */
  static private String toMavenVersion(final Version version) {
    final StringBuffer sb = new StringBuffer(40);

    sb.append(String.valueOf(version.getMajor())).append(".");
    sb.append(String.valueOf(version.getMinor())).append(".");
    sb.append(String.valueOf(version.getMicro()));

    final String qualifier = version.getQualifier();
    if (0 < qualifier.length()) {
      sb.append("-").append(qualifier);
    }

    return sb.toString();
  }

  /**
   * Add a bundle archive to a map using the given key. The values of the map
   * are sorted sets of bundle archives.
   */
  private void addToMap(final Map map, final String key, final BundleArchive ba) {
    if (null == key)
      return;

    SortedSet bas = (SortedSet) map.get(key);
    if (null == bas) {
      bas = new TreeSet();
      map.put(key, bas);
      task.log("Found bundle '" + key + "'.", Project.MSG_DEBUG);
    }
    if (bas.add(ba)) {
      task.log("Found bundle '" + key + "' '" + ba.version + "'.",
               Project.MSG_DEBUG);
    }
  }

  static String encodeBundleName(final String bundleName) {
    String name = bundleName;
    if (null != name) {
      name = name.replace(':', '.');
      name = name.replace(' ', '_');
    }
    return name;
  }

  /**
   * A BundleArchive-object describes one bundle with data derived from its file
   * name and manifest.
   */
  static class BundleArchive implements Comparable {
    /** Task that uses this class, for logging. */
    final Task task;

    /** File object referencing the bundle jar. */
    final File file;

    /** The relative path from the root of the file set holding the bundle. */
    final String relPath;

    /** The name of the bundle file without version and ".jar" suffix. */
    final String bundleName;

    /** The name of the project that the bundle belongs to. */
    final String projectName;

    // The manifest attributes and some parsed values
    final Attributes mainAttributes;
    final String manifestVersion;
    final String bsn;
    final Version version;
    final String name;

    /** Mapping from exported package name to its version. */
    final Map pkgExportMap;

    /** Mapping from imported package name to its version range constraint. */
    final Map pkgImportMap;

    /** Set with names of imported packages that are optional. */
    final Set pkgImportOptional = new TreeSet();

    /**
     * Collection of bundles that provides packages that this bundle are
     * importing. The mapping key is a bundle archive and the value is the set
     * of package names that the bundle archive may provide to this bundle.
     *
     * <p>
     * Initially empty, to fill in this map call {@link #doProviders()}.
     * </p>
     */
    final Map pkgProvidersMap = new TreeMap();

    /**
     * Collection of bundles that provides packages that this bundle needs
     * access to at compile time. I.e., packages that the bundle is importing
     * but not exporting. The mapping key is a bundle archive and the value is
     * the set of package names that the bundle archive may provide to this
     * bundle.
     *
     * <p>
     * Initially empty, to fill in this map call {@link #doProviders()}.
     * </p>
     */
    final Map pkgCtProvidersMap = new TreeMap();

    /**
     * Collection of bundles that this bundle provides packages to, i.e.,
     * bundles importing the exports of this bundle. The mapping key is a bundle
     * archive and the value is the set of package names that the bundle archive
     * may import from this bundle.
     *
     * <p>
     * Initially empty, to fill in this map call {@link #doProviders()}.
     * </p>
     */
    final Map pkgProvidedMap = new TreeMap();

    /**
     * Sub set of the entries in the imported packages map for which there are
     * no matching exporter. Mapping from package name to its version range
     * constraint.
     *
     * <p>
     * Initially empty, to fill in this map call {@link #doProviders()}.
     * </p>
     */
    final Map pkgUnprovidedMap = new TreeMap();

    /** Mapping from exported service name to its version. */
    final Map serviceExportMap;

    /** Mapping from imported service name to its version range constraint. */
    final Map serviceImportMap;

    /** Number of source files inside the bundle archive. */
    final int srcCount;

    /**
     * Create a bundle archive object for the given bundle jar file.
     *
     * @param task
     *          The task that uses this class, used for logging and project
     *          access.
     * @param resource
     *          The bundle jar file to create a bundle archive object for.
     * @param parseExportImport
     *          If <code>true</code> then populate the pkgExportMap,
     *          pkgImportMap, pkgImportOptional set with data parsed from the
     *          import / export package manifest attributes.
     */
    public BundleArchive(final Task task, final FileResource resource,
                         final boolean parseExportImport) throws IOException {
      this.task = task;
      this.file = resource.getFile();
      this.relPath = resource.getName();

      // The version derived from the file name, to be checked against
      // the version from the manifest.
      Version nameVersion = null;

      // Derive bundle name from the file name
      final String fileName = file.getName();
      String bn = null;
      // The file name format is "<bundleName>-<version>.jar"
      final int ix = fileName.lastIndexOf('-');
      if (0 < ix) {
        bn = fileName.substring(0, ix);
        final String versionS = fileName.substring(ix + 1,
                                                   fileName.length() - 4);
        try {
          nameVersion = new Version(versionS);
        } catch (NumberFormatException nfe) {
          bn = null; // Not valid due to missing version.
          task.log("Invalid version in bundle file name '" + versionS + "': "
                   + nfe, Project.MSG_VERBOSE);
        }
      } else {
        // No version in file name, just remove the ".jar"-suffix
        bn = fileName.substring(0, fileName.length() - 4);
      }

      final JarFile bundle = new JarFile(file);
      try {
        final Manifest manifest = bundle.getManifest();
        this.mainAttributes = manifest.getMainAttributes();
        this.manifestVersion = deriveBundleManifestVersion();
        this.bsn = deriveBundleSymbolicName();
        this.version = deriveBundleVersion();
        this.name = mainAttributes.getValue(Constants.BUNDLE_NAME);

        int count = 0;
        for (Enumeration e = bundle.entries(); e.hasMoreElements();) {
          ZipEntry entry = (ZipEntry) e.nextElement();

          if (entry.getName().startsWith(SRC_PREFIX)) {
            count++;
          }
        }
        srcCount = count;

      } finally {
        if (null != bundle) {
          try {
            bundle.close();
          } catch (IOException _ioe) {
          }
        }
      }

      // Compare version from the file name with the one from the manifest
      if (null != nameVersion && 0 != nameVersion.compareTo(version)) {
        if (nameVersion.getMajor() == version.getMajor()
            && nameVersion.getMinor() == version.getMinor()
            && nameVersion.getMicro() == version.getMicro()
            && "".equals(nameVersion.getQualifier())) {
          task.log("Found version '" + nameVersion + "' in the file name '"
                   + fileName + "', but the version in the bundle's manifest "
                   + "has qualifier '" + version + "'.", Project.MSG_DEBUG);
        } else {
          task.log("Found version '" + nameVersion + "' in the file name '"
                   + fileName + "', but the version in the bundle's manifest is '"
                   + version + "'.", Project.MSG_INFO);
        }
      }

      if (0 < version.getQualifier().length()) {
        // Maven uses '-' and not '.' as separator for the qualifier
        // in its bundle names. Check if bundleName needs to be
        // updated.
        final String mavenSuffix = "-" + toMavenVersion(version) + ".jar";
        if (fileName.endsWith(mavenSuffix)) {
          bn = fileName.substring(0, fileName.length() - mavenSuffix.length());
        }
      }

      // The bundle name is now ready!
      this.bundleName = bn;

      // The project name is the name of the parent directory of the
      // bundle in its relative path, if empty use the bundle name
      // without "_all" / "_impl" suffix.
      final File parentDir = new File(relPath).getParentFile();
      final String parentDirName = null == parentDir ? null : parentDir
        .getName();
      if (null != parentDirName && 0 < parentDirName.length()) {
        this.projectName = parentDirName;
      } else if (bn.endsWith("_api") || bn.endsWith("_all")) {
        this.projectName = bn.substring(0, bn.length() - 4);
      } else {
        this.projectName = bn;
      }

      if ("2".equals(manifestVersion) && null == bsn) {
        final String msg = "Found bundle with Bundle-MainfestVersion >= 2 "
          + "without Bundle-SymbolicName: " + fileName;
        throw new BuildException(msg);
      }

      if (parseExportImport) {
        pkgExportMap = parseNames("Export-Package", false, null);
        pkgImportMap = parseNames("Import-Package", true, pkgImportOptional);
        serviceExportMap = parseNames("Export-Service", false, null);
        serviceImportMap = parseNames("Import-Service", true, null);
      } else {
        pkgExportMap = null;
        pkgImportMap = null;
        serviceExportMap = null;
        serviceImportMap = null;
      }
    }

    /**
     * Sort on bundle symbolic name then on the bundle version. Compares bundle
     * symbolic name and then bundle version.
     *
     * @param o
     *          the object to compare this bundle archive to.
     * @return a negative integer, zero, or a positive integer as this object is
     *         less than, equal to, or greater than the specified object.
     */
    public int compareTo(Object o) {
      BundleArchive other = (BundleArchive) o;
      // The bsn may be null for pre OSGi R4 bundles!
      String objName = this.bsn != null ? this.bsn : this.name;
      String otherName = other.bsn != null ? other.bsn : other.name;

      int res = objName.compareTo(otherName);
      return res != 0 ? res : this.version.compareTo(other.version);
    }

    public String toString() {
      return file.toString();
    }

    /**
     * Get the bundle manifest version from the manifest.
     */
    private String deriveBundleManifestVersion() throws NumberFormatException {
      String manifestVersion = mainAttributes
        .getValue(Constants.BUNDLE_MANIFESTVERSION);
      if (null == manifestVersion) {
        // Pre OSGi R4 bundle with non-standard version format; use
        // version "1".
        manifestVersion = "1";
      }
      return manifestVersion.trim();
    }

    /**
     * Get the bundle symbolic name from the manifest. If the
     * <tt>Bundle-SymbolicName</tt> attribute is missing use the
     * <tt>Bundle-Name</tt> but replace all ':' with '.' and all '&nbsp;' with
     * '_' in the returned value.
     *
     */
    private String deriveBundleSymbolicName() {
      String name = mainAttributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
      if (null == name) {
        if ("1".equals(manifestVersion)) {
          name = mainAttributes.getValue(Constants.BUNDLE_NAME);
        }
      } else {
        // Remove any directive from the name
        final int semiPos = name.indexOf(";");
        if (-1 < semiPos) {
          name = name.substring(0, semiPos);
        }
        name = name.trim();
      }

      return BundleArchives.encodeBundleName(name);
    }

    /**
     * Get the bundle version from the manifest.
     */
    private Version deriveBundleVersion() throws NumberFormatException {
      final String versionS = mainAttributes.getValue(Constants.BUNDLE_VERSION);
      if (null == versionS) {
        return Version.emptyVersion;
      }

      try {
        return new Version(versionS);
      } catch (NumberFormatException nfe) {
        if ("1".equals(manifestVersion)) {
          // Pre OSGi R4 bundle with non-standard version format; use
          // the default version.
          return Version.emptyVersion;
        }
        final String msg = "Invalid bundle version '" + versionS
          + "' found in " + file + ": " + nfe;
        task.log(msg, Project.MSG_ERR);
        throw new BuildException(msg, nfe);
      }
    }

    /**
     * Parse import/export package/service headers.
     *
     * @param s
     *          The name the header to parse.
     * @param range
     *          if versions shall be parsed as ranges or not.
     * @param optionals
     *          Optional Set to add packages that are marked with the directive
     *          resolution:=optional to.
     *
     * @return Mapping from package/service name to version/version range.
     */
    private Map parseNames(String s, boolean range, Set optionals) {
      final Map res = new TreeMap();

      final String v = mainAttributes.getValue(s);
      final Iterator pathIter = Util.parseEntries(s, v, false, true, false);

      while (pathIter.hasNext()) {
        final Map pathMap = (Map) pathIter.next();
        String versionS = (String) pathMap.get("version");
        // Fall back to "specification-version" for pre OSGi R4 bundles.
        if (null == versionS) {
          versionS = (String) pathMap.get("specification-version");
        }
        if (null == versionS) {
          versionS = "0"; // The default version
        }
        final Object version = range ? (Object) new VersionRange(versionS)
          : (Object) new Version(versionS);

        final Iterator nameIter = ((List) pathMap.get("$keys")).iterator();
        while (nameIter.hasNext()) {
          final String pkgName = (String) nameIter.next();
          res.put(pkgName, version);
          // Is this package/service optional
          if (null != optionals) {
            Set directiveNames = (Set) pathMap.get("$directives");
            if (directiveNames.contains("resolution")
                && "optional".equals(pathMap.get("resolution"))) {
              optionals.add(pkgName);
            }
          }
        }
      }
      return res;
    }

    public String getBundleDescription() {
      final String res = mainAttributes.getValue(Constants.BUNDLE_DESCRIPTION);
      return null == res ? res : res.trim();
    }

    public Iterator getBundleLicense() {
      String value = mainAttributes.getValue(BundleArchives.BUNDLE_LICENSE);
      if (null != value) {
        value = value.trim();
        if (value.startsWith("http://")) {
          // Unquoted URI, try to add quotes in a couple of simple cases
          int pos = value.indexOf(';');
          if (-1 < pos) {
            // param present, stop quote before it starts
            value = "\"" + value.substring(0, pos) + "\""
              + value.substring(pos);
          } else if (-1 < (pos = value.indexOf(','))) {
            // Multiple licenses present, quote the first one...
            value = "\"" + value.substring(0, pos) + "\""
              + value.substring(pos);
          } else {
            value = "\"" + value + "\"";
          }
        }
      }

      try {
        return Util.parseEntries(BundleArchives.BUNDLE_LICENSE, value, true,
                                 true, false);
      } catch (Exception e) {
        final String msg = "Failed to parse " + BundleArchives.BUNDLE_LICENSE
          + " manifest header '" + value + "' in " + file + ": " + e;
        throw new BuildException(msg, e);
      }
    }

    /**
     * @return <code>true</code> if this bundle includes Declarative Services
     *         Components, <code>false</code> otherwise.
     */
    public boolean isSCBundle() {
      return null != mainAttributes.getValue(BundleArchives.SERVICE_COMPONENT);
    }

    /**
     * @return <code>true</code> if this bundle includes blueprint components,
     *         <code>false</code> otherwise.
     */
    public boolean isBlueprintBundle() {
      // TODO: If header not present the default pattern,
      // "OSGI-INF/blueprint/*.xml" should be checked.

      return null != mainAttributes.getValue(BundleArchives.BUNDLE_BLUEPRINT);
    }

    /**
     * @return <code>true</code> if this bundle includes a bundle activator,
     *         <code>false</code> otherwise.
     */
    public boolean isActivatorBundle() {
      return null != mainAttributes.getValue(Constants.BUNDLE_ACTIVATOR);
    }

    /**
     * @return <code>true</code> if this bundle is an API only bundle. I.e. if
     *         it exports packages but does not have a bundle activator, service
     *         component or blueprint component. activator, <code>false</code>
     *         otherwise.
     */
    public boolean isAPIBundle() {
      return 0 < pkgExportMap.size() && !isActivatorBundle() && !isSCBundle()
        && !isBlueprintBundle();
    }

    /**
     * Extract source from inside the bundle to the given destination directory.
     * If bundle does not contain any source files try to copy source files from
     * the bundles src-directory (derived from the <code>Built-From</code>
     * manifest attribute.
     *
     * @param destDir
     *          The directory to extract the source files to.
     * @return List with one entry for each extracted file. The value is the
     *         path relative to <code>destDir</code>.
     * @throws IOException
     */
    public List extractSources(File destDir) throws IOException {
      final List res = new ArrayList();
      final JarFile jarFile = new JarFile(file);
      for (Enumeration e = jarFile.entries(); e.hasMoreElements();) {
        ZipEntry entry = (ZipEntry) e.nextElement();

        if (entry.getName().startsWith(SRC_PREFIX)) {
          if (0 == res.size()) {
            destDir.mkdirs();
          }

          if (entry.isDirectory()) {
            makeDir(destDir, entry, SRC_PREFIX);
          } else {
            copyEntry(destDir, jarFile, entry, SRC_PREFIX);
            res.add(Util.replace(entry.getName(), SRC_PREFIX + "/", ""));
          }
        }
      }

      if (0 == res.size()) {
        // Check if we can copy source from original pos
        String sourceDir = (String) mainAttributes.getValue("Built-From");
        if (sourceDir != null && !"".equals(sourceDir)) {
          File src = new File(sourceDir, "src");
          res.addAll(copyTree(src, destDir, src.toString() + File.separator, ".java"));
        }
      }
      return res;
    }

    private static void makeDir(final File destDir, final ZipEntry entry,
                                final String prefix) throws IOException {
      final File d = new File(destDir,
                              Util.replace(entry.getName(), prefix, ""));

      d.mkdirs();
    }

    private static void copyEntry(File destDir, ZipFile file, ZipEntry entry,
                                  String prefix) throws IOException {
      final File destFile = new File(destDir, Util.replace(entry.getName(),
                                                           prefix, ""));

      final File dir = destFile.getParentFile();

      if (!dir.exists()) {
        dir.mkdirs();
      }
      copyStream(new BufferedInputStream(file.getInputStream(entry)),
                 new BufferedOutputStream(new FileOutputStream(destFile)));
    }

    private static void copyStream(final InputStream is, final OutputStream os)
      throws IOException {
      final byte[] buf = new byte[1024];

      BufferedInputStream in = null;
      BufferedOutputStream out = null;

      try {
        in = new BufferedInputStream(is);
        out = new BufferedOutputStream(os);
        int n;
        while ((n = in.read(buf)) > 0) {
          out.write(buf, 0, n);
        }
      } finally {
        try {
          in.close();
        } catch (Exception ignored) {
        }
        try {
          out.close();
        } catch (Exception ignored) {
        }
      }
    }

    private static List copyTree(File dest, File src, String prefix,
                                 String suffix) throws IOException {
      final List res = new ArrayList();

      if (src.isDirectory()) {
        if (!dest.exists()) {
          dest.mkdirs();
        }

        final String[] files = src.list();
        for (int i = 0; i < files.length; i++) {
          res.addAll(copyTree(new File(dest, files[i]),
                              new File(src, files[i]), prefix, suffix));
        }
      } else if (src.isFile()) {
        if (src.getName().endsWith(suffix)) {
          copyStream(new FileInputStream(src), new FileOutputStream(dest));
          res.add(Util.replace(Util.replace(src.getAbsolutePath(), prefix, ""),
                               "\\", "/"));
        }
      }
      return res;
    }

    /**
     * Build a mapping holding one entry for each source file found in the
     * SVN-repository for the bundle. The repository URL of a source file is the
     * repoUrl followed by the part of the file path that remains when the
     * pathPrefix has been removed.
     *
     * @param pathPrefix
     *          The path of source files to include must start with this value.
     * @param repoURL
     *          The URL to the SVN repository.
     *
     * @return Mapping where the key is the name (path) of the source file and
     *         the value the repository URL to it.
     * @throws IOException
     */
    public Map getSrcRepositoryLinks(final String pathPrefix, final URL repoURL)
      throws IOException {
      final Map res = new TreeMap();

      // Check if we can locate a source tree based on the
      // "Built-From" header.
      String rootDir = (String) mainAttributes.getValue("Built-From");
      if (null != rootDir && 0 < rootDir.length()) {
        final File src = new File(rootDir, "src");
        if (src.isDirectory()) {
          res.putAll(srcRepositoryLinks(pathPrefix, repoURL, src,
                                        src.getAbsolutePath() + File.separator));
        } else {
          task.log(
                   "No src sub-directory in 'Built-From' location, "
                   + src.getAbsolutePath() + ", can not locate source files.",
                   Project.MSG_VERBOSE);
        }
      } else {
        task.log("No 'Built-From' manifest header in bundle, "
                 + "can not locate source files.", Project.MSG_VERBOSE);
      }
      return res;
    }

    private Map srcRepositoryLinks(final String pathPrefix, final URL repoURL,
                                   final File src, final String prefix) throws IOException {
      final Map res = new TreeMap();

      if (src.isDirectory()) {
        final String[] files = src.list();
        for (int i = 0; i < files.length; i++) {
          res.putAll(srcRepositoryLinks(pathPrefix, repoURL, new File(src,
                                                                      files[i]), prefix));
        }
      } else if (src.isFile()) {
        final String path = src.getAbsolutePath();
        if (src.getName().endsWith(".java")) {
          final String bundlePath = Util.replace(path, prefix, "");
          if (path.startsWith(pathPrefix)) {
            final String repoPath = Util.replace(path, pathPrefix, "").replace(
                                                                               File.separatorChar, '/');
            final String href = new URL(repoURL, repoPath).toString();

            res.put(bundlePath, href);

            task.log("Found Java source file in repository, " + path
                     + " with href '" + href + "'.", Project.MSG_VERBOSE);
          } else {
            task.log("Skipping non-repository Java source file, " + path + ".",
                     Project.MSG_DEBUG);
          }

        } else {
          task.log("Skipping non-Java source file, " + path + ".",
                   Project.MSG_DEBUG);
        }
      }
      return res;
    }

  } // class BundleArchive

}
