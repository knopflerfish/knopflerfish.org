/*
 * Copyright (c) 2010-2010, KNOPFLERFISH project
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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

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
 * A class that analyses all bundle jar files given by a
 * list of resource collections (file sets).
 *
 */
public class BundleArchives {

  // This constant is missing from org.osgi.framework.Constants
  public static final String BUNDLE_LICENSE = "Bundle-License";

  /**
   * The ant task that is using this helper class. Used for logging
   * etc.
   */
  private final Task task;

  /**
   * Mapp from bundle name (the bundles file without version and
   * ".jar" sufixes) to a sorted set of a bundle archive object with
   * details about the bundle . The set will contain more than one
   * element when more than one version of the bundle has been found.
   */
  final Map bnToBundleArchives = new TreeMap();

  /**
   * Mapp from bundle symbolic name to a sorted set of a bundle
   * archive object with details about the bundle. The set will
   * contain more than one element when more than one version of the
   * bundle has been found.
   */
  final Map bsnToBundleArchives = new TreeMap();

  /**
   * Set with all bundle archives specified by the given resource
   * collections.
   */
  final SortedSet allBundleArchives = new TreeSet();


  public BundleArchives(final Task task,
                        final List resourceCollections) {
    this.task = task;

    if (resourceCollections.size() == 0) {
      task.log("BundleArchives called without any bundle archives to analyse",
               Project.MSG_ERR);
      throw new BuildException("No resource collections specified");
    }

    try {
      for (Iterator it = resourceCollections.iterator(); it.hasNext(); ) {
        final ResourceCollection rc = (ResourceCollection) it.next();

        // Ignore file sets with a non existing root dir.
        if (rc instanceof FileSet) {
          final FileSet fs = (FileSet) rc;
          final File fsRootDir = fs.getDir(task.getProject());
          if (!fsRootDir.exists()) {
            task.log("Skipping nested file set rooted at '" +fsRootDir
                     +"' since that directory does not exist.",
                     Project.MSG_WARN);
            continue;
          }
        }

        for (Iterator rcIt = rc.iterator(); rcIt.hasNext();) {
          final Resource res = (Resource) rcIt.next();
          if(res.getName().endsWith(".jar")) {
            task.log("Adding bundle: "+res, Project.MSG_INFO);
            final BundleArchive ba
              = new BundleArchive( task, (FileResource) res);
            allBundleArchives.add(ba);
            addToMap(bnToBundleArchives, ba.bundleName, ba);
            addToMap(bsnToBundleArchives, ba.bsn, ba);
          }
        }
      }// Scan done

    } catch (BuildException be) {
      throw be;
    } catch (Exception e) {
      final String msg = "Failed to analyze bundle archives: " +e;
      task.log(msg);
      throw new BuildException(msg, e);
    }
  }

  SortedSet getKnownNames()
  {
    final TreeSet knownNames = new TreeSet(bnToBundleArchives.keySet());
    knownNames.addAll(bsnToBundleArchives.keySet());
    return knownNames;
  }

  /**
   * Format the OSGi version as Maven 2 does in versioned file names.
   */
  static private String toMavenVersion(final Version version)
  {
    final StringBuffer sb = new StringBuffer(40);

    sb.append(String.valueOf(version.getMajor())).append(".");
    sb.append(String.valueOf(version.getMinor())).append(".");
    sb.append(String.valueOf(version.getMicro()));

    final String qualifier = version.getQualifier();
    if (0<qualifier.length()) {
      sb.append("-").append(qualifier);
    }

    return sb.toString();
  }

  /**
   * Add a bundle archive to a map using the given key. The values of
   * the map are sorted sets of bundle arvchives.
   */
  private void addToMap(final Map map,
                        final String key,
                        final BundleArchive ba)
  {
    if (null==key) return;

    SortedSet bas = (SortedSet) map.get(key);
    if (null==bas) {
      bas = new TreeSet();
      map.put(key, bas);
      task.log("Found bundle '" +key +"'.", Project.MSG_DEBUG);
    }
    if (bas.add(ba)) {
      task.log("Found bundle '" +key +"' '" +ba.version +"'.",
               Project.MSG_DEBUG);
    }
  }

  static String encodeBundleName(final String bundleName)
  {
    String name = bundleName;
    if (null!=name) {
      name = name.replace(':', '.');
      name = name.replace(' ', '_');
    }
    return name;
  }


  /**
   * A BundleArchive-object describes one bundle wiht data derived
   * from its file name and manifest.
   */
  static class BundleArchive
    implements Comparable
  {
    /** Task that uses this class, for logging. */
    final Task task;

    /** File object referencing the bundle jar. */
    final File file;

    /** The relative path from the root of the file set holding the bundle.*/
    final String  relPath;

    /** The name of the bundle file without version and ".jar" suffix. */
    final String bundleName;

    /** The name of the project that the bundle belongs to. */
    final String projectName;

    // The manifest attributes and some parsed values
    final Attributes mainAttributes;
    final String  manifestVersion;
    final String  bsn;
    final Version version;
    final String  name;


    public BundleArchive(final Task task, final FileResource resource)
      throws IOException
    {
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
      if (0<ix) {
        bn = fileName.substring(0,ix);
        final String versionS = fileName.substring(ix+1,fileName.length()-4);
        try {
          nameVersion = new Version(versionS);
        } catch (NumberFormatException nfe) {
          bn = null; // Not valid due to missing version.
          task.log("Invalid version in bundle file name '" +versionS +"': "+nfe,
                   Project.MSG_VERBOSE);
        }
      } else {
        // No version in file name, just remove the ".jar"-suffix
        bn = fileName.substring(0,fileName.length()-4);
      }

      final JarFile bundle  = new JarFile(file);
      try {
        final Manifest   manifest = bundle.getManifest();
        this.mainAttributes  = manifest.getMainAttributes();
        this.manifestVersion = deriveBundleManifestVersion();
        this.bsn             = deriveBundleSymbolicName();
        this.version         = deriveBundleVersion();
        this.name = mainAttributes.getValue(Constants.BUNDLE_NAME);
      } finally {
        if (null!=bundle) {
          try { bundle.close(); } catch (IOException _ioe) {}
        }
      }

      // Compare version from the file name with the one from the manifest
      if (null!=nameVersion && 0!=nameVersion.compareTo(version)) {
        if (nameVersion.getMajor() == version.getMajor() &&
            nameVersion.getMinor() == version.getMinor() &&
            nameVersion.getMicro() == version.getMicro() &&
            "".equals(nameVersion.getQualifier()) ) {
          task.log("Found version '" +nameVersion +"' in the file name '"
                   +fileName +"', but the version in the bundle's manifest "
                   +"has qualifier '" +version +"'.",
                   Project.MSG_DEBUG);
        } else {
          task.log("Found version '" +nameVersion +"' in the file name '"
                   +fileName +"', but the version in the bundle's manifest is '"
                   +version +"'.",
                   Project.MSG_INFO);
        }
      }

      if (0<version.getQualifier().length()) {
        // Maven uses '-' and not '.' as separator for the qualifier
        // in its bundle names. Check if bundleName needs to be
        // updated.
        final String mavenSuffix = "-" +toMavenVersion(version) +".jar";
        if (fileName.endsWith(mavenSuffix)) {
          bn = fileName.substring(0, fileName.length() -mavenSuffix.length());
        }
      }

      // The bundle name is now ready!
      this.bundleName = bn;

      // The project name is the name of the parent directory of the
      // bundle in its relative path, if empty use the bundle name
      // without "_all" / "_impl" suffix.
      final File parentDir = new File(relPath).getParentFile();
      final String parentDirName = null==parentDir ? null : parentDir.getName();
      if (null!=parentDirName && 0<parentDirName.length()) {
        this.projectName = parentDirName;
      } else if (bn.endsWith("_api") || bn.endsWith("_all")) {
        this.projectName = bn.substring(0,bn.length()-4);
      } else {
        this.projectName = bn;
      }

      if ("2".equals(manifestVersion) && null==bsn) {
        final String msg = "Found bundle with Bundle-MainfestVersion >= 2 "
          +"without Bundle-SymbolicName: "+ fileName;
        throw new BuildException(msg);
      }
    }

    /**
     * Sort on bundle symbolic name then on the bundle version.
     * Compares bundle symbolic name and then bundle version.
     *
     * @param o the object to compare this bundle archive to.
     * @return a negative integer, zero, or a positive integer as this
     *         object is less than, equal to, or greater than the
     *         specified object.
     */
    public int compareTo(Object o)
    {
      BundleArchive other = (BundleArchive) o;
      // The bsn may be null for pre OSGi R4 bundles!
      String objName = this.bsn != null ? this.bsn : this.name;
      String otherName = other.bsn != null ? other.bsn : other.name;

      int res = objName.compareTo(otherName);
      return res!=0 ? res : this.version.compareTo(other.version);
    }

    public String toString()
    {
      return file.toString();
    }

    /**
     * Get the bundle manifest version from the manifest.
     */
    private String deriveBundleManifestVersion()
      throws NumberFormatException
    {
      String manifestVersion
        = mainAttributes.getValue(Constants.BUNDLE_MANIFESTVERSION);
      if (null==manifestVersion) {
        // Pre OSGi R4 bundle with non-standard version format; use
        // version "1".
        manifestVersion = "1";
      }
      return manifestVersion.trim();
    }


    /**
     * Get the bundle symbolic name from the manifest. If the
     * <tt>Bundle-SymbolicName</tt> attribute is missing use the
     * <tt>Bundle-Name</tt> but replace all ':' with '.' and all '&nbsp;'
     * with '_' in the returned value.
     *
     */
    private String deriveBundleSymbolicName()
    {
      String name = mainAttributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
      if (null==name) {
        if ("1".equals(manifestVersion)) {
          name = mainAttributes.getValue(Constants.BUNDLE_NAME);
        }
      } else {
        // Remove any directive from the name
        final int semiPos = name.indexOf(";");
        if (-1<semiPos) {
          name = name.substring(0,semiPos);
        }
        name = name.trim();
      }

      return BundleArchives.encodeBundleName(name);
    }

    /**
     * Get the bundle version from the manifest.
     */
    private Version deriveBundleVersion()
      throws NumberFormatException
    {
      final String versionS
        = mainAttributes.getValue(Constants.BUNDLE_VERSION);
      if (null==versionS) {
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
        final String msg = "Invalid bundle version '" +versionS
          +"' found in " +file +": "+nfe;
        task.log(msg, Project.MSG_ERR);
        throw new BuildException(msg, nfe);
      }
    }

    public String getBundleDescription()
    {
      final String res = mainAttributes.getValue(Constants.BUNDLE_DESCRIPTION);
      return null==res ? res : res.trim();
    }

    public Iterator getBundleLicense()
    {
      String value = mainAttributes.getValue(BundleArchives.BUNDLE_LICENSE);
      if (null!=value && value.startsWith("http://")) {
        // Unquoted URI, try to add qoutes
        int pos = value.indexOf(';');
        if (-1 < pos) {
          value = "\"" +value.substring(0, pos) +"\"" +value.substring(pos);
        } else if (-1 < (pos=value.indexOf(','))) {
          value = "\"" +value.substring(0, pos) +"\"" +value.substring(pos);
        }
      }

      try {
        return Util.parseEntries(BundleArchives.BUNDLE_LICENSE, value,
                                 true, true, false);
      } catch (Exception e) {
        final String msg = "Failed to parse " +BundleArchives.BUNDLE_LICENSE
          +" manifest header '" +value +"' in " +file +": " +e;
        throw new BuildException(msg, e);
      }
    }

  } // class BundleArchive

}
