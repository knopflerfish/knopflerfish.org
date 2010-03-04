/*
 * Copyright (c) 2005-2009, KNOPFLERFISH project
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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.FileScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.taskdefs.Manifest;
import org.apache.tools.ant.taskdefs.ManifestException;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ZipFileSet;
//import org.apache.tools.zip.ZipFile;


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
 *         <li>warn &ndash; a warning will be displayed for each referenced
 *         package not found in the bundle or the <tt>importpackage</tt> nested
 *         elements.</li>
 *         <li>auto &ndash; each referenced package not found in the bundle or
 *         the <tt>importpackage</tt> nested elements will be added to the
 *         Import-Package manifest header. Packages exported by the bundle will
 *         be added to the Import-Package manifest header with the version as
 *         specified in the <tt>exportpackage</tt> nested element.</li>
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
 * specification version of a package to add to the Export-Package manifest
 * header. If package analysis is not turned off, a warning will be issued if
 * the specified package cannot be found in the bundle.
 * </p>
 *
 * <h4>importpackage</h4>
 * <p>
 * The nested <tt>importpackage</tt> element specifies the name and
 * specification version of a package to add to the Import-Package manifest
 * header.
 * </p>
 *
 * <h4>standardpackage</h4>
 * <p>
 * The nested <tt>standardpackage</tt> element specifies the name or prefix
 * of a package that should be excluded from the package analysis. It can
 * be used to avoid importing packages that are available in the underlying
 * runtime environment.
 * </p>
 *
 * <h3>Implicit fileset</h3>
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
 *         file="out/${ant.project.name}.jar">
 *
 *   &lt;standardpackage name="javax.imageio"/>
 *
 *   &lt;exportpackage name="se.weilenmann.bundle.test" version="1.0"/>
 *
 *   &lt;manifest>
 *     &lt;attribute name="Bundle-Name" value="testbundle"/>
 *     &lt;attribute name="Bundle-Version" value="1.0"/>
 *     &lt;attribute name="Bundle-Vendor" value="Kaspar Weilenmann"/>
 *   &lt;/manifest>
 *
 *   &lt;classes dir="out/classes">
 *     &lt;include name="se/weilenmann/bundle/test/**"/>
 *   &lt;/classes>
 *   &lt;classes dir="out/classes" prefix="util">
 *     &lt;include name="se/weilenmann/util/**"/>
 *   &lt;/classes>
 *   &lt;classes src="osgi/jars/log/log_api.jar" prefix="log_api">
 *     &lt;include name="*&#42;/*.class"/>
 *   &lt;/classes>
 *
 *   &lt;lib dir="osgi/jars/cm" includes="cm_api.jar" prefix="osgi"/>
 *   &lt;lib dir="lib/commons" includes="commons-logging.jar" prefix="commons"/>
 *
 * &lt;/bundle>
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

  private Set referencedPackages = new HashSet();
  private Set standardPackagePrefixes = new HashSet(); {
    standardPackagePrefixes.add("java.");
  }

  private final BundlePackagesInfo bpInfo = new BundlePackagesInfo();
  private final ClassAnalyserASM   asmAnalyser
    = new ClassAnalyserASM(bpInfo, this);


  /**
   * The set of classes that are refrenced from the included classes.
   * May be used to check execution environment, see {@link BundleInfo}.
   */
  private Set classSet             = new TreeSet();
  /**
   * A mapping from package name of included classes to a set of
   * package names that the classes in the key package references.
   */
  private Map packageUsingMap      = new HashMap();


  // private methods

  private void analyze() {
    if (activator == ACTIVATOR_AUTO ||
        packageAnalysis != PACKAGE_ANALYSIS_NONE) {
      addZipGroups();
      addImplicitFileset();

      for (Iterator i = srcFilesets.iterator(); i.hasNext();) {
        FileSet fileset = (FileSet) i.next();
        File srcFile = getZipFile(fileset);
        if (srcFile == null) {
          File filesetBaseDir = fileset.getDir(getProject());
          DirectoryScanner ds = fileset.getDirectoryScanner(getProject());
          String[] files = ds.getIncludedFiles();
          for (int j = 0; j < files.length; j++) {
            String fileName = files[j];
            if (fileName.endsWith(".class")) {
              File file = new File(filesetBaseDir, fileName);
              try {
                asmAnalyser.analyseClass(file);
              } catch (Exception e) {
                e.printStackTrace();
                throw new BuildException
                  ("Failed to analyze class file " +file +", exception=" +e,
                   getLocation());
              }
            }
          }
        } else {
          try {
            ZipFile zipFile = new ZipFile(srcFile);
            DirectoryScanner ds = fileset.getDirectoryScanner(getProject());
            String[] entries = ds.getIncludedFiles();
            for (int kk = 0; kk<entries.length; kk++) {
              if (entries[kk].endsWith(".class")) {
                try {
                  ZipEntry entry = zipFile.getEntry(entries[kk]);
                  asmAnalyser.analyseClass(zipFile.getInputStream(entry),
                                           entries[kk]);
                } catch (IOException ioe) {
                  throw new BuildException
                    ("Failed to parse analyze class file " +entries[kk]
                     +" from zip archive: " +srcFile.getAbsolutePath(),
                     ioe);
                }
              }
            }
          } catch (IOException ioe) {
            throw new BuildException
              ("Failed to read zip file: " +srcFile.getAbsolutePath(),
               ioe);
          }
        }
      }
      // Scan done
      bpInfo.toJavaNames();

      Set publicPackages = exportPackage.keySet();

      if (packageAnalysis != PACKAGE_ANALYSIS_NONE) {
        for (Iterator i = publicPackages.iterator(); i.hasNext();) {
          String packageName = (String) i.next();
          if (!bpInfo.providesPackage(packageName)) {
            log("Exported package not provided by bundle: " +packageName,
                Project.MSG_WARN);
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
            if (exportPackage.containsKey(packageName)) {
              // TODO: do we want to import with version?
              importPackage.put(packageName, exportPackage.get(packageName));
            } else {
              importPackage.put(packageName, null);
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

  private File getZipFile(FileSet fileset) {
    if (fileset instanceof ZipFileSet) {
      ZipFileSet zipFileset = (ZipFileSet) fileset;
      return zipFileset.getSrc(getProject());
    } else {
      return null;
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

  private void addPackageHeader(String headerName, Map packageMap) throws ManifestException {
    final Iterator i = packageMap.entrySet().iterator();
    if (i.hasNext()) {
      final StringBuffer valueBuffer = new StringBuffer();
      while (i.hasNext()) {
        Map.Entry entry = (Map.Entry) i.next();
        final String name = (String) entry.getKey();
        final String version = (String) entry.getValue();
        valueBuffer.append(name);
        if (version != null) {
          valueBuffer.append(";specification-version=");
          valueBuffer.append(version);
        }
        valueBuffer.append(',');
      }
      valueBuffer.setLength(valueBuffer.length() - 1);
      final String value = valueBuffer.toString();
      generatedManifest.addConfiguredAttribute(createAttribute(headerName, value));
      log(headerName + ": " + value, Project.MSG_INFO);
    }
  }

  private static Manifest.Attribute createAttribute(String name, String value) {
    final Manifest.Attribute attribute = new Manifest.Attribute();
    attribute.setName(name);
    attribute.setValue(value);
    return attribute;
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

      // TODO: better merge may be needed, currently overwrites pre-existing headers
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
