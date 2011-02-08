/*
 * Copyright (c) 2005-2010, KNOPFLERFISH project
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.FileScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.taskdefs.Manifest;
import org.apache.tools.ant.taskdefs.ManifestException;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ZipFileSet;
import org.osgi.framework.Version;

/**
 * <p>
 * An extension of the
 * <a href="http://ant.apache.org/manual/CoreTasks/jar.html" target="_top">Jar</a> task that
 * builds an OSGi bundle. It can generate the Bundle-Activator,
 * Bundle-ClassPath and Import-Package manifest headers based on the content
 * specified in the task.
 * </p>
 *
 * <h3>Parameters</h3>
 * <table border="1px">
 *   <tr>
 *     <th align="left">Attribute</th>
 *     <th align="left">Description</th>
 *     <th align="left">Required</th>
 *   </tr>
 *   <tr>
 *     <td valign="top">file</td>
 *     <td valign="top">The bundle file to create.</td>
 *     <td valign="top">Yes</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">activator</td>
 *     <td valign="top">
 *       The bundle activator class name. If set to "none" no Bundle-Activator
 *       manifest header will be generated. If set to "auto" the bundle task
 *       will try to find an activator in the included class files.
 *     </td>
 *     <td valign="top">No, default is "auto"</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">packageanalysis</td>
 *     <td valign="top">
 *       Analyzes the class files of the bundle and the contents of the
 *       <tt>exportpackage</tt> and <tt>importpackage</tt> nested elements.
 *       <ul>
 *         <li>none &ndash; no analysis is performed.</li>
 *         <li>warn &ndash; a warning will be displayed for each
 *         referenced package not found in the bundle or the
 *         <tt>importpackage</tt> nested elements.</li>
 *         <li>auto &ndash; each referenced package not found in the
 *         bundle or the <tt>importpackage</tt> nested elements will
 *         be added to the Import-Package manifest header. Packages
 *         exported by the bundle will be added to the Import-Package
 *         manifest header with a version range following the OSGi
 *         versioning recommendation (if a package is incompatible
 *         with previous version then the major number of the version
 *         must be incremented). E.g., if the bundles exports version
 *         1.0.2 the version range in the import will be
 *         "[1.0.2,2)".</li>
 *       </ul>
 *     </td>
 *     <td valign="top">No, default is "warn"</td>
 *   </tr>
 * </table>
 *
 * <h3>Nested elements</h3>
 *
 * <h4>classes</h4>
 * <p>
 * The nested <tt>classes</tt> element specifies a
 * <a href="http://ant.apache.org/manual/CoreTypes/zipfileset.html" target="_top">ZipFileSet</a>.
 * The <tt>prefix</tt> attribute will be added to the Bundle-ClassPath manifest
 * header. The classes specified by the file set will be included in the class
 * analysis.
 * </p>
 *
 * <h4>lib</h4>
 * <p>
 * The nested <tt>lib</tt> element specifies a
 * <a href="http://ant.apache.org/manual/CoreTypes/zipfileset.html" target="_top">ZipFileSet</a>.
 * The locations of all files in the file set will be added to the
 * Bundle-ClassPath manifest header. All files of this file set must be either
 * zip or jar files. The classes available in the zip or jar files will be
 * included in the class analysis.
 * </p>
 *
 * <h4>exportpackage</h4>
 * <p>
 * The nested <tt>exportpackage</tt> element specifies the name and
 * version of a package to add to the Export-Package manifest
 * header. If package analysis is not turned off, a warning will be
 * issued if the specified package cannot be found in the bundle.
 * When package analysis is turned on the version from the
 * <tt>packageinfo</tt>-file in the directory of the package is read
 * and used as the version of the exported package. In this case, if a
 * version is specified in the <tt>exportpackage</tt> element that
 * version is compared with the one from the <tt>packageinfo</tt>-file
 * and if there is a mismatch a build error will be issued.
 * </p>
 *
 * <h4>importpackage</h4>
 * <p>
 * The nested <tt>importpackage</tt> element specifies the name and
 * version of a package to add to the Import-Package manifest header.
 * </p>
 *
 * <h4>standardpackage</h4>
 * <p>
 * The nested <tt>standardpackage</tt> element specifies the name or prefix
 * of a package that should be excluded from the package analysis. It can
 * be used to avoid importing packages that are available in the underlying
 * runtime environment (i.e., via boot delegation).
 * </p>
 *
 * <h3>Implicit file set</h3>
 * <p>
 * The implicit fileset is specified by the <tt>baseDir</tt> attribute of the
 * bundle task and the nested <tt>include</tt> and <tt>exclude</tt> elements.
 * </p>
 * <p>
 * The implicit fileset of the <tt>bundle</tt> task will be included in the
 * class analysis and in the Bundle-ClassPath manifest header if needed.
 * </p>
 *
 * <h3>Examples</h3>
 * <pre>
 * &lt;bundle activator="auto"
 *         packageanalysis="auto"
 *         file="out/${ant.project.name}.jar"&gt;
 *
 *   &lt;standardpackage name="javax.imageio"/&gt;
 *
 *   &lt;exportpackage name="se.weilenmann.bundle.test" version="1.0"/&gt;
 *
 *   &lt;manifest&gt;
 *     &lt;attribute name="Bundle-Name" value="testbundle"/&gt;
 *     &lt;attribute name="Bundle-Version" value="1.0"/&gt;
 *     &lt;attribute name="Bundle-Vendor" value="Kaspar Weilenmann"/&gt;
 *   &lt;/manifest&gt;
 *
 *   &lt;classes dir="out/classes"&gt;
 *     &lt;include name="se/weilenmann/bundle/test/**"/&gt;
 *   &lt;/classes&gt;
 *   &lt;classes dir="out/classes" prefix="util"&gt;
 *     &lt;include name="se/weilenmann/util/**"/&gt;
 *   &lt;/classes&gt;
 *   &lt;classes src="osgi/jars/log/log_api.jar" prefix="log_api"&gt;
 *     &lt;include name="*&#42;/*.class"/&gt;
 *   &lt;/classes&gt;
 *
 *   &lt;lib dir="osgi/jars/cm" includes="cm_api.jar" prefix="osgi"/&gt;
 *   &lt;lib dir="lib/commons" includes="commons-logging.jar" prefix="commons"/&gt;
 *
 * &lt;/bundle&gt;
 * </pre>
 * <p>Creates a bundle with the following manifest:<p>
 * <pre>
 * Manifest-Version: 1.0
 * Ant-Version: Apache Ant 1.6.2
 * Created-By: 1.4.2_02-b03 (Sun Microsystems Inc.)
 * Bundle-Name: testbundle
 * Bundle-Version: 1.0
 * Bundle-Vendor: Kaspar Weilenmann
 * Bundle-Activator: se.weilenmann.bundle.test.Activator
 * Bundle-ClassPath: .,util,log_api,osgi/cm_api.jar,commons/commons-loggi
 * ng.jar
 * Import-Package: se.weilenmann.bundle.test;specification-version=1.0,or
 * g.osgi.framework
 * Export-Package: se.weilenmann.bundle.test;specification-version=1.0
 * </pre>
 *
 * @author <a href="mailto:kaspar@weilenmann.se">Kaspar Weilenmann</a>
 */
public class Bundle extends Jar {

  // private fields

  private static final String BUNDLE_CLASS_PATH_KEY = "Bundle-ClassPath";
  private static final String BUNDLE_ACTIVATOR_KEY = "Bundle-Activator";
  private static final String IMPORT_PACKAGE_KEY = "Import-Package";
  private static final String EXPORT_PACKAGE_KEY = "Export-Package";

  private static final String ACTIVATOR_NONE = "none";
  private static final String ACTIVATOR_AUTO = "auto";

  private static final String PACKAGE_ANALYSIS_NONE = "none";
  private static final String PACKAGE_ANALYSIS_WARN = "warn";
  private static final String PACKAGE_ANALYSIS_AUTO = "auto";

  private String activator = ACTIVATOR_AUTO;
  private String packageAnalysis = PACKAGE_ANALYSIS_WARN;
  private Map importPackage = new HashMap();
  private Map exportPackage = new HashMap();
  private List libs = new ArrayList();
  private List classes = new ArrayList();

  private File baseDir = null;
  private List zipgroups = new ArrayList();
  private List srcFilesets = new ArrayList();

  private Manifest generatedManifest = new Manifest();

  private Set standardPackagePrefixes = new HashSet(); {
    standardPackagePrefixes.add("java.");
  }

  private final BundlePackagesInfo bpInfo = new BundlePackagesInfo(this);
  private final ClassAnalyserASM   asmAnalyser
    = new ClassAnalyserASM(bpInfo, this);


  private void analyze() {
    if (activator == ACTIVATOR_AUTO ||
        packageAnalysis != PACKAGE_ANALYSIS_NONE) {
      addZipGroups();
      addImplicitFileset();

      for (Iterator i = srcFilesets.iterator(); i.hasNext();) {
        FileSet fileset = (FileSet) i.next();

        for (Iterator fsIt = fileset.iterator(); fsIt.hasNext();) {
          final Resource res = (Resource) fsIt.next();
          analyze(res);
        }
      }
      // Scan done
      bpInfo.toJavaNames();

      final Set publicPackages = exportPackage.keySet();

      if (packageAnalysis != PACKAGE_ANALYSIS_NONE) {
        for (Iterator i = publicPackages.iterator(); i.hasNext();) {
          final String packageName = (String) i.next();
          if (!bpInfo.providesPackage(packageName)) {
            log("Exported package not provided by bundle: " +packageName,
                Project.MSG_WARN);
          }
          // The Version from the packageinfo-file or null
          final Version piVersion =
            bpInfo.getProvidedPackageVersion(packageName);
          if (null!=piVersion) {
            final String epVersionS = (String) exportPackage.get(packageName);
            if (null==epVersionS) {
              // Use the version form the packageinfo-file
              exportPackage.put(packageName, piVersion.toString());
            } else {
              // Check that the versions match, if not trigger a build error
              try {
                final Version epVersion = Version.parseVersion(epVersionS);
                if (0!=epVersion.compareTo(piVersion)) {
                  final String msg = "Multiple versions found for export of "
                    +"the package '" +packageName
                    +"'. The packageinfo file ("
                    +bpInfo.getProvidedPackageVersionSource(packageName)
                    +") states '" +piVersion.toString()
                    +"' but the <exportpackage> element says '"
                    +epVersion.toString() +"'.";
                  log(msg, Project.MSG_ERR);
                  throw new BuildException(msg);
                }
              } catch (IllegalArgumentException iae) {
                final String msg = "Invalid version '" +epVersionS
                  +"' in <exportpackage name=\""+packageName +"\" ...>: "
                  +iae.getMessage();
                log(msg, Project.MSG_ERR);
                throw new BuildException(msg, iae);
              }
            }
          }
        }
      }

      final SortedSet privatePackages = bpInfo.getProvidedPackages();
      privatePackages.removeAll(publicPackages);

      final SortedSet referencedPackages = bpInfo.getReferencedPackages();
      referencedPackages.removeAll(privatePackages);
      for (Iterator iterator = referencedPackages.iterator();
           iterator.hasNext();) {
        final String packageName = (String) iterator.next();
        if (!isStandardPackage(packageName) &&
            !importPackage.containsKey(packageName)) {
          if (packageAnalysis == PACKAGE_ANALYSIS_AUTO) {
            final String version = (String) exportPackage.get(packageName);
            try {
              importPackage.put(packageName, toImportRange(version));
            } catch (IllegalArgumentException iae) {
              final String msg = "Invalid version value, '" +version
                +"' for exported package \""+packageName
                +"\" can not derive version range for auto-import. "
                +iae.getMessage();
              log(msg, Project.MSG_ERR);
              throw new BuildException(msg, iae);
            }
          } else if (packageAnalysis == PACKAGE_ANALYSIS_WARN) {
            log("Referenced package not found in bundle or imports: "
                +packageName,
                Project.MSG_WARN);
          }
        }
      }
    }
  }

  /**
   * Analyze a resource by checking its suffix and delegate to {@link
   * #analyzeClass(Resource)}, {@link #analyzeJar(Resource)}, etc.
   *
   * @param res The resource to be analyzed.
   */
  protected void analyze(Resource res) throws BuildException {
    if(res.getName().endsWith(".class")) {
      analyzeClass(res);
    } else if(res.getName().endsWith(".jar")) {
      analyzeJar(res);
    } else if(res.getName().endsWith("/packageinfo")) {
      analyzePackageinfo(res);
    } else {
      // Just ignore all other files
    }
  }

  protected void analyzeJar(Resource res) throws BuildException {
    log("Analyze jar file " + res, Project.MSG_VERBOSE);

    try {
      final JarInputStream jarStream = new JarInputStream(res.getInputStream());

      ZipEntry ze = jarStream.getNextEntry();
      while(null!=ze) {
        final String fileName = ze.getName();
        if (fileName.endsWith(".class")) {
          log("Analyze jar class file " + fileName, Project.MSG_VERBOSE);
          asmAnalyser.analyseClass(jarStream, fileName);
        }
        ze = jarStream.getNextEntry();
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new BuildException("Failed to analyze class-file " +
                               res + ", exception=" + e, getLocation());
    }
  }

  protected void analyzeClass(Resource res) throws BuildException {
    log("Analyze class file " + res, Project.MSG_VERBOSE);

    try {
      asmAnalyser.analyseClass(res.getInputStream(), res.toString());
    } catch (BuildException be) {
      throw be;
    } catch (Exception e) {
      e.printStackTrace();
      throw new BuildException("Failed to analyze class-file "
                               +res + ", exception=" + e,
                               getLocation());
    }
  }

  protected void analyzePackageinfo(Resource res) throws BuildException {
    log("Analyze packageinfo file " + res, Project.MSG_VERBOSE);

    bpInfo.setPackageVersion(res);
  }


  private void addZipGroups() {
    for (int i = 0; i < zipgroups.size(); i++) {
      FileSet fileset = (FileSet) zipgroups.get(i);
      FileScanner fs = fileset.getDirectoryScanner(getProject());
      String[] files = fs.getIncludedFiles();
      File basedir = fs.getBasedir();
      for (int j = 0; j < files.length; j++) {
        ZipFileSet zipfileset = new ZipFileSet();
        zipfileset.setSrc(new File(basedir, files[j]));
        srcFilesets.add(zipfileset);
      }
    }
  }

  private void addImplicitFileset() {
    if (baseDir != null) {
      FileSet fileset = (FileSet) getImplicitFileSet().clone();
      fileset.setDir(baseDir);
      srcFilesets.add(fileset);
    }
  }

  private boolean isStandardPackage(String packageName) {
    for (Iterator i = standardPackagePrefixes.iterator(); i.hasNext();) {
      final String prefix = (String) i.next();
      if (packageName.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  private void handleActivator() throws ManifestException {
    if (activator == ACTIVATOR_NONE) {
      log("No BundleActivator set", Project.MSG_DEBUG);
    } else if (activator == ACTIVATOR_AUTO) {
      switch (bpInfo.countProvidedActivatorClasses()) {
        case 0: {
          log("No class implementing BundleActivator found", Project.MSG_INFO);
          break;
        }
        case 1: {
          activator = bpInfo.getActivatorClass();
          break;
        }
        default: {
          log("More than one class implementing BundleActivator found: "
              +bpInfo.providedActivatorClassesAsString(),
              Project.MSG_WARN);
          break;
        }
      }
    }
    if (activator != ACTIVATOR_NONE && activator != ACTIVATOR_AUTO) {
      log("Bundle-Activator: " + activator, Project.MSG_INFO);
      generatedManifest.addConfiguredAttribute(createAttribute(BUNDLE_ACTIVATOR_KEY, activator));
    }
  }

  private void handleClassPath() throws ManifestException {
    final StringBuffer value = new StringBuffer();

    boolean rootIncluded = false;
    if (baseDir != null || classes.size() == 0) {
      value.append(".,");
      rootIncluded = true;
    }

    Iterator i = classes.iterator();
    while (i.hasNext()) {
      final ZipFileSet zipFileSet = (ZipFileSet) i.next();
      final String prefix = zipFileSet.getPrefix(getProject());
      if (prefix.length() > 0) {
        value.append(prefix);
        value.append(',');
      } else if (!rootIncluded) {
        value.append(".,");
        rootIncluded = true;
      }
    }

    i = libs.iterator();
    while (i.hasNext()) {
      final ZipFileSet fileset = (ZipFileSet) i.next();
      if (fileset.getSrc(getProject()) == null) {
        final DirectoryScanner ds = fileset.getDirectoryScanner(getProject());
        final String[] files = ds.getIncludedFiles();
        if (files.length != 0) {
          zipgroups.add(fileset);
          final String prefix = fixPrefix(fileset.getPrefix(getProject()));
          for (int j = 0; j < files.length; j++) {
            value.append(prefix.replace('\\', '/'));
            value.append(files[j].replace('\\', '/'));
            value.append(',');
          }
        }
      }
    }

    if (value.length() > 2) {
      generatedManifest.addConfiguredAttribute(createAttribute(BUNDLE_CLASS_PATH_KEY, value.substring(0, value.length() - 1)));
    }
  }

  private static String fixPrefix(String prefix) {
    if (prefix.length() > 0) {
      final char c = prefix.charAt(prefix.length() - 1);
      if (c != '/' && c != '\\') {
        prefix = prefix + "/";
      }
    }
    return prefix;
  }

  private void addPackageHeader(String headerName, Map packageMap)
    throws ManifestException
  {
    final Iterator i = packageMap.entrySet().iterator();
    if (i.hasNext()) {
      final StringBuffer valueBuffer = new StringBuffer();
      while (i.hasNext()) {
        Map.Entry entry = (Map.Entry) i.next();
        final String name = (String) entry.getKey();
        String version = (String) entry.getValue();
        valueBuffer.append(name);
        if (version != null) {
          version = version.trim();
          if (0<version.length()) {
            valueBuffer.append(";version=");
            final boolean quotingNeeded = -1!=version.indexOf(',')
              && '"'!=version.charAt(0);
            if (quotingNeeded) valueBuffer.append('"');
            valueBuffer.append(version);
            if (quotingNeeded) valueBuffer.append('"');
          }
        }
        valueBuffer.append(',');
      }
      valueBuffer.setLength(valueBuffer.length() - 1);
      final String value = valueBuffer.toString();
      generatedManifest.addConfiguredAttribute(createAttribute(headerName,
                                                               value));
      log(headerName + ": " + value, Project.MSG_INFO);
    }
  }

  private static Manifest.Attribute createAttribute(String name, String value) {
    final Manifest.Attribute attribute = new Manifest.Attribute();
    attribute.setName(name);
    attribute.setValue(value);
    return attribute;
  }

  /**
   * Given a precise version create a version range suitable for an
   * import package specification. Currently an input version of
   * M.N.U.Q will result in an output range "[M.N.U.Q, M+1)" following
   * the version usage recommended by OSGi (a package that is not
   * backwards compatible must increment the major number in its
   * version number).
   *
   * @param version an OSGi compatibel version on string form.
   * @return a quoted version range starting with the given version
   *         (inclusive) ending with the next major version
   *         (exclusive). If the specified version is
   *         <code>null</code> or an empty string a <code>null</code>
   *         is returned.
   */
  private static String toImportRange(final String version)
    throws IllegalArgumentException
  {
    if (null==version || 0==version.length()) return null;

    final Version vStart = Version.parseVersion(version);
    final Version vEnd = new Version(vStart.getMajor()+1, 0, 0, null);
    return "\"[" +vStart.toString() +"," +vEnd.toString() +")\"";
  }


  // public methods

  public void setActivator(String activator) {
    if (ACTIVATOR_NONE.equalsIgnoreCase(activator)) {
      this.activator = ACTIVATOR_NONE;
    } else if (ACTIVATOR_AUTO.equalsIgnoreCase(activator)) {
      this.activator = ACTIVATOR_AUTO;
    } else {
      this.activator = activator;
    }
  }

  public void setPackageAnalysis(String packageAnalysis) {
    packageAnalysis = packageAnalysis.trim().toLowerCase();
    if (PACKAGE_ANALYSIS_NONE.equals(packageAnalysis)) {
      this.packageAnalysis = PACKAGE_ANALYSIS_NONE;
    } else if (PACKAGE_ANALYSIS_WARN.equals(packageAnalysis)) {
      this.packageAnalysis = PACKAGE_ANALYSIS_WARN;
    } else if (PACKAGE_ANALYSIS_AUTO.equals(packageAnalysis)) {
      this.packageAnalysis = PACKAGE_ANALYSIS_AUTO;
    } else {
      throw new BuildException("Illegal value: " + packageAnalysis);
    }
  }

  public void addConfiguredStandardPackage(OSGiPackage osgiPackage) {
    final String name = osgiPackage.getName();
    final String prefix = osgiPackage.getPrefix();
    if (name != null && prefix == null) {
      bpInfo.addProvidedPackage(name);
    } else if (prefix != null && name == null) {
      standardPackagePrefixes.add(prefix);
    } else {
      throw new BuildException("StandardPackage must have exactly one of the name and prefix attributes defined");
    }
  }

  public void addConfiguredImportPackage(OSGiPackage osgiPackage) {
    final String name = osgiPackage.getName();
    if (name == null) {
      throw new BuildException("ImportPackage must have a name");
    } else if (osgiPackage.getPrefix() != null) {
      throw new BuildException("ImportPackage must not have a prefix attribute");
    } else {
      importPackage.put(name, osgiPackage.getVersion());
    }
  }

  public void addConfiguredExportPackage(OSGiPackage osgiPackage) {
    final String name = osgiPackage.getName();
    if (name == null) {
      throw new BuildException("ExportPackage must have a name");
    } else if (osgiPackage.getPrefix() != null) {
      throw new BuildException("ExportPackage must not have a prefix attribute");
    } else {
      exportPackage.put(name, osgiPackage.getVersion());
    }
  }

  public void addConfiguredLib(ZipFileSet fileset) {
    // TODO: handle refid
    if (fileset.getSrc(getProject()) == null) {
      addFileset(fileset);
      libs.add(fileset);
    } else {
      addClasses(fileset);
    }
  }

  public void addClasses(ZipFileSet fileset) {
    super.addZipfileset(fileset);
    srcFilesets.add(fileset);
    classes.add(fileset);
  }


  // extends Jar

  public void execute() {
    try {
      handleClassPath();

      analyze();

      handleActivator();

      addPackageHeader(IMPORT_PACKAGE_KEY, importPackage);
      addPackageHeader(EXPORT_PACKAGE_KEY, exportPackage);

      // TODO: better merge may be needed, currently overwrites
      // pre-existing headers
      addConfiguredManifest(generatedManifest);
    } catch (ManifestException me) {
      throw new BuildException("Error merging manifest headers", me);
    }
    super.execute();
  }

  public void setBasedir(File baseDir) {
    super.setBasedir(baseDir);
    this.baseDir = baseDir;
  }

  public void addZipGroupFileset(FileSet fileset) {
    super.addZipGroupFileset(fileset);
    zipgroups.add(fileset);
  }

  public void addZipfileset(ZipFileSet fileset) {
    super.addZipfileset(fileset);
  }

} // Bundle
