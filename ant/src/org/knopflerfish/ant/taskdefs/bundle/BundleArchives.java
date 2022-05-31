/*
 * Copyright (c) 2010-2022, KNOPFLERFISH project
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
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
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
import org.osgi.framework.VersionRange;
import org.knopflerfish.ant.taskdefs.bundle.Util.HeaderEntry;

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
  final Map<String, SortedSet<BundleArchive>> bnToBundleArchives =
    new TreeMap<String, SortedSet<BundleArchive>>();

  /**
   * Map from bundle symbolic name to a sorted set of a bundle archive object
   * with details about the bundle. The set will contain more than one element
   * when more than one version of the bundle has been found.
   */
  final Map<String, SortedSet<BundleArchive>> bsnToBundleArchives =
    new TreeMap<String, SortedSet<BundleArchive>>();

  /**
   * Set with all bundle archives specified by the given resource collections.
   */
  final SortedSet<BundleArchive> allBundleArchives = new TreeSet<BundleArchive>();

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
  final SortedMap<String, SortedMap<Version, SortedSet<BundleArchive>>> allExports =
    new TreeMap<String, SortedMap<Version, SortedSet<BundleArchive>>>();

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
  public BundleArchives(final Task task,
                        final List<ResourceCollection> resourceCollections)
  {
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
  public BundleArchives(final Task task,
                        final List<ResourceCollection> resourceCollections,
                        final boolean parseExportImport)
  {
    this.task = task;

    if (resourceCollections.size() == 0) {
      task.log("BundleArchives called without any bundle archives to analyse",
               Project.MSG_ERR);
      throw new BuildException("No resource collections specified");
    }

    try {
      for (final ResourceCollection rc : resourceCollections) {
        // Ignore file sets with a non existing root directory.
        if (rc instanceof FileSet) {
          final FileSet fs = (FileSet) rc;
          final File fsRootDir = fs.getDir(task.getProject());
          if (!fsRootDir.exists()) {
            task.log("Skipping nested file set rooted at '" + fsRootDir
                         + "' since that directory does not exist.",
                     Project.MSG_WARN);
            continue;
          }
          try {
            if (fs.size() < 1) {
              task.log("Skipping nested file set rooted at '" + fsRootDir
                       + "' since that file set is empty.", Project.MSG_VERBOSE);
              continue;

            }
          } catch (final Exception e) {
            task.log("Skipping nested file set rooted at '" + fsRootDir
                         + "' since size computation throws exception.", e,
                     Project.MSG_VERBOSE);
            continue;
          }
        }

        for (@SuppressWarnings("unchecked")
        final Iterator<Resource> rcIt = rc.iterator(); rcIt.hasNext();) {
          final Resource res = rcIt.next();
          if (res.getName().endsWith(".jar")) {
            task.log("Adding bundle: " + res, Project.MSG_VERBOSE);
            try {
              final BundleArchive ba =
                new BundleArchive(task, (FileResource) res, parseExportImport);
              allBundleArchives.add(ba);
              addToMap(bnToBundleArchives, ba.bundleName, ba);
              addToMap(bsnToBundleArchives, ba.bsn, ba);
            } catch (final Exception e) {
              final String msg =
                "Failed to analyze bundle archive: " + res + "; reason: " + e;
              task.log(msg);
              throw new BuildException(msg, e);
            }
          }
        }
      }// Scan done

    } catch (final BuildException be) {
      throw be;
    } catch (final Exception e) {
      final String msg = "Failed to analyze bundle archives: " + e;
      task.log(msg);
      throw new BuildException(msg, e);
    }
  }

  SortedSet<String> getKnownNames() {
    final TreeSet<String> knownNames = new TreeSet<String>(bnToBundleArchives.keySet());
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
    for (final BundleArchive ba : allBundleArchives) {
      // Clear bundle archive maps holding results from this analysis
      ba.pkgProvidersMap.clear();
      ba.pkgUnprovidedMap.clear();
      ba.pkgProvidedMap.clear();

      for (final Entry<String,Version> eE : ba.pkgExportMap.entrySet()) {
        final String pkgName = eE.getKey();
        final Version pkgVersion = eE.getValue();

        SortedMap<Version, SortedSet<BundleArchive>> versions = allExports.get(pkgName);
        if (null == versions) {
          versions = new TreeMap<Version, SortedSet<BundleArchive>>();
          allExports.put(pkgName, versions);
        }

        SortedSet<BundleArchive> exporters = versions.get(pkgVersion);
        if (null == exporters) {
          exporters = new TreeSet<BundleArchive>();
          versions.put(pkgVersion, exporters);
        }
        exporters.add(ba);
      }
    }

    // For each bundle build the pkgProvidersMap
    for (final BundleArchive ba : allBundleArchives) {
      for (final Entry<String,VersionRange> iE : ba.pkgImportMap.entrySet()) {
        final String pkgName = iE.getKey();
        final VersionRange range = iE.getValue();

        final SortedMap<Version, SortedSet<BundleArchive>> versions = allExports.get(pkgName);
        if (null != versions) {
          for (final Entry<Version,SortedSet<BundleArchive>> vE : versions.entrySet()) {
            final Version pkgVersion = vE.getKey();

            if (range.includes(pkgVersion)) {
              final SortedSet<BundleArchive> providers = vE.getValue();

              for (final BundleArchive provider : providers) {
                // The package pkgName may be imported by ba from provider,
                // update ba's providers map
                SortedSet<String> pkgNames = ba.pkgProvidersMap
                  .get(provider);
                if (null == pkgNames) {
                  pkgNames = new TreeSet<String>();
                  ba.pkgProvidersMap.put(provider, pkgNames);
                }
                pkgNames.add(pkgName);

                // Non self exported package, add to pkgCtProvidersMap
                if (!ba.pkgExportMap.containsKey(pkgName)) {
                  SortedSet<String> pkgNamesCt = ba.pkgCtProvidersMap
                    .get(provider);
                  if (null == pkgNamesCt) {
                    pkgNamesCt = new TreeSet<String>();
                    ba.pkgCtProvidersMap.put(provider, pkgNamesCt);
                  }
                  pkgNamesCt.add(pkgName);
                }

                // The package pkgName is provided (exported) by
                // provider to ba, update provider.pkgProvidedMap to
                // reflect this.
                SortedSet<String> pkgNamesProvidedToBa = provider.pkgProvidedMap
                  .get(ba);
                if (null == pkgNamesProvidedToBa) {
                  pkgNamesProvidedToBa = new TreeSet<String>();
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
    final StringBuilder sb = new StringBuilder(40);

    sb.append(version.getMajor()).append(".");
    sb.append(version.getMinor()).append(".");
    sb.append(version.getMicro());

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
  private void addToMap(final Map<String, SortedSet<BundleArchive>> map,
                        final String key,
                        final BundleArchive ba)
  {
    if (null == key) {
      return;
    }

    SortedSet<BundleArchive> bas = map.get(key);
    if (null == bas) {
      bas = new TreeSet<BundleArchive>();
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
  static class BundleArchive implements Comparable<BundleArchive> {
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
    final Map<String, Version> pkgExportMap;

    /** Mapping from imported package name to its version range constraint. */
    final Map<String, VersionRange> pkgImportMap;

    /** Set with names of imported packages that are optional. */
    final Set<String> pkgImportOptional = new TreeSet<String>();

    /**
     * Collection of bundles that provides packages that this bundle are
     * importing. The mapping key is a bundle archive and the value is the set
     * of package names that the bundle archive may provide to this bundle.
     *
     * <p>
     * Initially empty, to fill in this map call {@link #doProviders()}.
     * </p>
     */
    final Map<BundleArchive, SortedSet<String>> pkgProvidersMap =
      new TreeMap<BundleArchive, SortedSet<String>>();

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
    final Map<BundleArchive, SortedSet<String>> pkgCtProvidersMap =
      new TreeMap<BundleArchive, SortedSet<String>>();

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
    final Map<BundleArchive, SortedSet<String>> pkgProvidedMap =
      new TreeMap<BundleArchive, SortedSet<String>>();

    /**
     * Sub set of the entries in the imported packages map for which there are
     * no matching exporter. Mapping from package name to its version range
     * constraint.
     *
     * <p>
     * Initially empty, to fill in this map call {@link #doProviders()}.
     * </p>
     */
    final Map<String, VersionRange> pkgUnprovidedMap = new TreeMap<String, VersionRange>();

    /** Mapping from exported service name to its version. */
    final Map<String, Version> serviceExportMap;

    /** Mapping from imported service name to its version range constraint. */
    final Map<String, VersionRange> serviceImportMap;

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
    @SuppressWarnings("deprecation")
	public BundleArchive(final Task task, final FileResource resource,
                         final boolean parseExportImport) throws IOException {
      this.task = task;
      this.file = resource.getFile();
      this.relPath = resource.getName();

      // Get data from the bundles manifest and contents
      final JarFile bundle = new JarFile(file);
      try {
        final Manifest manifest = bundle.getManifest();
        this.mainAttributes = manifest.getMainAttributes();
        this.manifestVersion = deriveBundleManifestVersion();
        this.bsn = deriveBundleSymbolicName();
        this.version = deriveBundleVersion();
        this.name = mainAttributes.getValue(Constants.BUNDLE_NAME);

        int count = 0;
        for (final Enumeration<JarEntry> e = bundle.entries(); e.hasMoreElements();) {
          final ZipEntry entry = e.nextElement();

          if (entry.getName().startsWith(SRC_PREFIX)) {
            count++;
          }
        }
        srcCount = count;

      } finally {
        if (null != bundle) {
          try {
            bundle.close();
          } catch (final IOException _ioe) {
          }
        }
      }

      // Derive bundle name, bn, from the file name and the manifest version.
      // The file name format is "<bundleName>-<version>.jar"
      String bn = null;
      final String fileName = file.getName();
      final String versionSuffix = "-" +this.version.toString() +".jar";
      if (fileName.endsWith(versionSuffix)) {
        // Simple case when Version.toString() returns the version
        // string in the file name.
        bn = fileName.substring(0, fileName.length() -versionSuffix.length());
      } else {
        // Try to find a valid version in the file name that matches
        // the one in the manifest
        int ix = fileName.lastIndexOf('-');
        while (0<ix && null==bn) {
          final String versionS
            = fileName.substring(ix + 1, fileName.length() - 4);
          try {
            // Compare version from the file name with the one from the manifest
            final Version nameVersion = new Version(versionS);
            if (0 == nameVersion.compareTo(version)) {
              bn = fileName.substring(0, ix);
            } else {
              task.log("Found version '" + nameVersion
                       + "' in the file name '" + fileName
                       + "', but the version in the bundle's manifest is '"
                       + version + "'.", Project.MSG_DEBUG);
            }
          } catch (final NumberFormatException nfe) {
            task.log("Invalid version in bundle file name '" + versionS + "': "
                     + nfe, Project.MSG_VERBOSE);
          } catch (final IllegalArgumentException iae) {
            task.log("Invalid version in bundle file name '" + versionS + "': "
                     + iae, Project.MSG_VERBOSE);
          }
          ix = fileName.lastIndexOf('-', ix-1);
        }
        if (null==bn) {
          // No valid version in file name, just remove the ".jar"-suffix
          bn = fileName.substring(0, fileName.length() - 4);
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
        pkgExportMap = parseNames(Constants.EXPORT_PACKAGE, false, null, Version.class);
        pkgImportMap = parseNames(Constants.IMPORT_PACKAGE, true, pkgImportOptional, VersionRange.class);
        serviceExportMap = parseNames(Constants.EXPORT_SERVICE, false, null, Version.class);
        serviceImportMap = parseNames(Constants.IMPORT_SERVICE, true, null, VersionRange.class);
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
    @Override
    public int compareTo(BundleArchive other)
    {
      // The bsn may be null for pre OSGi R4 bundles!
      final String objName = this.bsn != null
        ? this.bsn : (this.name != null ? this.name : this.bundleName);

      final String otherName = other.bsn != null
        ? other.bsn : (other.name != null ? other.name : other.bundleName);

      final int res = objName.compareTo(otherName);
      return res != 0 ? res : this.version.compareTo(other.version);
    }

    @Override
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
      } catch (final NumberFormatException nfe) {
        if ("1".equals(manifestVersion)) {
          // Pre OSGi R4 bundle with non-standard version format; use
          // the default version.
          return Version.emptyVersion;
        }
        final String msg = "Invalid bundle version '" + versionS
          + "' found in " + file + ": " + nfe;
        task.log(msg, Project.MSG_ERR);
        throw new BuildException(msg, nfe);
      } catch (final IllegalArgumentException iae) {
        if ("1".equals(manifestVersion)) {
          // Pre OSGi R4 bundle with non-standard version format; use
          // the default version.
          return Version.emptyVersion;
        }
        final String msg = "Invalid bundle version '" + versionS
          + "' found in " + file + ": " + iae;
        task.log(msg, Project.MSG_ERR);
        throw new BuildException(msg, iae);
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
     * @param valueType
     *          The desired type of the value in the returned Mapping. Supported
     *          types are {@link Version} and {@link VersionRange}.
     *
     * @return Mapping from package/service name to version/version range.
     */
    @SuppressWarnings("deprecation")
	private <V> Map<String, V> parseNames(String s,
                                          boolean range,
                                          Set<String> optionals,
                                          Class<V> valueType)
    {
      final TreeMap<String, V> res = new TreeMap<String, V>();

      final String v = mainAttributes.getValue(s);
      final List<HeaderEntry> entries =
        Util.parseManifestHeader(s, v, false, true, false);

      for (final HeaderEntry entry : entries) {
        String versionS =
          (String) entry.attributes.get(Constants.VERSION_ATTRIBUTE);
        // Fall back to "specification-version" for pre OSGi R4 bundles.
        if (null == versionS) {
          versionS =
            (String) entry.attributes
                .get(Constants.PACKAGE_SPECIFICATION_VERSION);
        }
        if (null == versionS) {
          versionS = "0"; // The default version
        }
        if (valueType.equals(Version.class)) {
          @SuppressWarnings("unchecked")
          final
          V version = (V) new Version(versionS);
          for (final String key : entry.getKeys()) {
            res.put(key, version);
          }
        } else if (valueType.equals(VersionRange.class)) {
          @SuppressWarnings("unchecked")
          final
          V versionRange =  (V) new VersionRange(versionS);
          for (final String key : entry.getKeys()) {
            res.put(key, versionRange);
          }
        }

        if (entry.directives.containsKey(Constants.RESOLUTION_DIRECTIVE)
            && Constants.RESOLUTION_OPTIONAL.equals(entry.directives
                .get(Constants.RESOLUTION_DIRECTIVE))) {
          optionals.addAll(entry.getKeys());
        }
      }
      return res;
    }

    public String getBundleDescription() {
      final String res = mainAttributes.getValue(Constants.BUNDLE_DESCRIPTION);
      return null == res ? res : res.trim();
    }

    public List<HeaderEntry> getBundleLicense() {
      String value = mainAttributes.getValue(BundleArchives.BUNDLE_LICENSE);
      if (null != value) {
        value = value.trim();
      }
      try {
        return Util.parseManifestHeader(BundleArchives.BUNDLE_LICENSE,
                                        value, true,
                                        true, false);
      } catch (final Exception e) {
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
    public List<String> extractSources(File destDir) throws IOException {
      final List<String> res = new ArrayList<String>();
      final JarFile jarFile = new JarFile(file);
      for (final Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();) {
        final ZipEntry entry = e.nextElement();

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
        final String sourceDir = mainAttributes.getValue("Built-From");
        if (sourceDir != null && !"".equals(sourceDir)) {
          final File src = new File(sourceDir, "src");
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
        } catch (final Exception ignored) {
        }
        try {
          out.close();
        } catch (final Exception ignored) {
        }
      }
    }

    private static List<String> copyTree(File dest, File src, String prefix,
                                 String suffix) throws IOException {
      final List<String> res = new ArrayList<String>();

      if (src.isDirectory()) {
        if (!dest.exists()) {
          dest.mkdirs();
        }

        final String[] files = src.list();
        for (final String file2 : files) {
          res.addAll(copyTree(new File(dest, file2),
                              new File(src, file2), prefix, suffix));
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
     * Git-repository for the bundle. The repository URL of a source file is the
     * repoUrl followed by the part of the file path that remains when the
     * pathPrefix has been removed.
     *
     * @param pathPrefix
     *          The path of source files to include must start with this value.
     * @param repoURL
     *          The URL to the Git repository.
     *
     * @return Mapping where the key is the name (path) of the source file and
     *         the value the repository URL to it.
     * @throws IOException
     */
    public Map<String, String> getSrcRepositoryLinks(final String pathPrefix, final URL repoURL)
      throws IOException {
      final Map<String, String> res = new TreeMap<String, String>();

      // Check if we can locate a source tree based on the
      // "Built-From" header.
      final String rootDir = mainAttributes.getValue("Built-From");
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

    private Map<String, String> srcRepositoryLinks(final String pathPrefix, final URL repoURL,
                                   final File src, final String prefix) throws IOException {
      final Map<String, String> res = new TreeMap<String, String>();

      if (src.isDirectory()) {
        final String[] files = src.list();
        for (final String file2 : files) {
          res.putAll(srcRepositoryLinks(pathPrefix, repoURL, new File(src,
                                                                      file2), prefix));
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
