/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
//import java.util.function.Predicate;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.Restrict;
import org.apache.tools.ant.types.resources.selectors.ResourceSelector;
import org.apache.tools.ant.util.StringUtils;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.knopflerfish.ant.taskdefs.bundle.Util.HeaderEntry;

/**
 * Task that analyzes sets of class files and jar files that will be
 * the contents of some bundle. The output is the set of packages to
 * be exported and imported by the bundle. Also tries to find any
 * class implementing <tt>org.osgi.framework.BundleActivator</tt>.
 *
 * <p>The set of files to analyze are specified as nested ant file sets.</p>
 *
 * <h3>Parameters</h3>
 *
 * <table border="1">
 *  <tr>
 *   <td valign=top><b>Attribute</b></td>
 *   <td valign=top><b>Description</b></td>
 *   <td valign=top><b>Required</b></td>
 *  </tr>
 *  <tr>
 *   <td valign=top>imports</td>
 *   <td valign=top>Name of property that will receive a comma-separated list
 *       of all used packages.
 *       <p>
 *       If set to empty string, no property will be set.
 *       </p>
 *       <p>
 *       <b>Note</b>: Some default packages are always added. These
 *       defaults can be set using the <tt>defaultimports</tt> parameter.
 *       </p>
 *   </td>
 *   <td valign=top>No.<br> Default value is ""</td>
 *  </tr>
 *  <tr>
 *   <td valign=top>exports</td>
 *   <td valign=top>Name of property that will receive a comma-separated
 *       list of all defined packages.
 *       <p>
 *       If set to empty string, no property will be set.
 *       </p>
 *   </td>
 *   <td valign=top>No.<br> Default value is ""</td>
 *  </tr>
 *  <tr>
 *   <td valign=top>activator</td>
 *   <td valign=top>
 *       Name of property that will receive name of class which implements
 *       <tt>org.osgi.framework.BundleActivator</tt>
 *       <p>
 *       If set to empty string, no property will be set.
 *       </p>
 *       <p>
 *       If set to non-empty, and multiple activators are found, or
 *       an activator not equal to any previous content in the named property
 *       is found - a warning will be logged.
 *       </p>
 *  </td>
 *   <td valign=top>No.<br> Default value is ""</td>
 *  </tr>
 *  <tr>
 *   <td valign=top>stdimports</td>
 *   <td valign=top>
 *
 *       Comma-separated list of package name prefixes for referenced
 *       packages that shall not be included in the Import-Package
 *       manifest attribute.
 *
 *       <p>Example of packages to list in this attribute are
 *       <ul>
 *         <li>non-standard packages obtained via boot-delegation,
 *         <li>packages exported by a required bundle,
 *         <li>packages referenced to by code in nested jar-files that
 *             are never used.
 *       </ul>
 *
 *       The default value, "java.", is added to any given list
 *       since packages starting with "java.*" shall never be
 *       imported. They are always made available to the bundle by
 *       delegation to the parent classloader.
 *
 *   </td>
 *   <td valign=top>
 *     No.<br>
 *     Default value is "java."
 *   </td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>defaultimports</td>
 *   <td valign=top>
 *       Comma-separated list of packages that will be unconditionally
 *       added to the derived set of packages that the bundle needs to
 *       import.
 *   </td>
 *   <td valign=top>
 *     No.<br>
 *     Default value is ""
 *   </td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>extraimports</td>
 *   <td valign=top>
 *       Comma-separated list of package names that must be present
 *       in the import list even though they are not explicitly
 *       referenced from the bundles code. E.g., packages from which
 *       all classes are loaded using reflection.
 *   </td>
 *   <td valign=top>
 *     No.<br>
 *     Default value is ""
 *   </td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>importsOnly</td>
 *   <td valign=top>
 *       If set to <tt>true</tt> then do not update Export-Package
 *       manifest header.
 *  </td>
 *   <td valign=top>No.<br>Default value is "false".</td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>implicitImports</td>
 *   <td valign=top>
 *       Flag for ensuring that each exported packages is included in the
 *       import package list.
 *
 *       <p>
 *       If set to "true", the task will add all packages mentioned in
 *       the property named by <code>exports</code> to the list of
 *       imported packages in the property named by
 *       <code>imports</code> if not already present. When this task
 *       adds an import declaration for an exported package the import
 *       entry will be given a version range starting with the version
 *       from the export statement and up to the next major version
 *       (exclusive). If some other version range is needed an
 *       explicit import with that range should be added to the
 *       bundle manifest template.
 *
 *       <p>
 *       This emulates the implicit import behavior present in OSGi R1-R3.
 *       </p>
 *   </td>
 *   <td valign=top>
 *     No.<br>
 *     Default value is "true"
 *   </td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>serviceComponent</td>
 *   <td valign=top>
 *       The value of the <tt>Service-Component</tt> manifest header.
 *
 *       <p>If set to non-empty, leave the value of the activator
 *       property untouched and do not complain if there are no class
 *       that implements BundleActivator in the bundle. A bundle using
 *       declarative services does not need a bundle activator but it
 *       may have one anyhow.</p>
 *
 *  </td>
 *   <td valign=top>No.<br> Default value is ""</td>
 *  </tr>
 *  <tr>
 *   <td valign=top>fragmentHost</td>
 *   <td valign=top>
 *       The value of the <tt>Fragment-Host</tt> manifest header.
 *
 *       <p>If set to non-empty (i.e., this is a fragment bundle),
 *       leave the value of the activator property untouched and do
 *       not complain if there are no class that implements
 *       BundleActivator in the bundle.</p>
 *
 *  </td>
 *   <td valign=top>No.<br> Default value is ""</td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>manifestVersion</td>
 *   <td valign=top>
 *       The value of the <tt>Bundle-ManifestVersion</tt> manifest header.
 *       <p>
 *       If set to "2" and the <tt>uses</tt> attribute is also
 *       <tt>true</tt> then a "uses" directive is computed and added
 *       to each package in the Export-Package header generated by
 *       this task.
 *       </p>
 *  </td>
 *   <td valign=top>No.<br> Default value is "1" (pre OSGi R4 bundle).</td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>uses</td>
 *   <td valign=top>
 *
 *       If set to <tt>true</tt> then add / update <tt>uses</tt>
 *       directive in the value of the Export-Package manifest header.
 *       <p>
 *       Note that <tt>uses</tt>-directives are not added for pre OSGi
 *       R4 bundle. That is for bundle with <tt>manifestVersion</tt>
 *       less than "2".
 *       </p>
 *  </td>
 *   <td valign=top>No.<br>Default value is "true".</td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>failOnExports</td>
 *   <td valign=top>
 *       If an error is detected in the given export package header
 *       and this attribute is set to <tt>true</tt> then a build
 *       failure is trigger.
 *   </td>
 *   <td valign=top>
 *     No.<br>
 *     Default value is "true"
 *   </td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>failOnImports</td>
 *   <td valign=top>
 *       If an error is detected in the given import package header
 *       and this attribute is set to <tt>true</tt> then a build
 *       failure is trigger.
 *   </td>
 *   <td valign=top>
 *     No.<br>
 *     Default value is "true"
 *   </td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>failOnActivator</td>
 *   <td valign=top>
 *       If an error is detected in the given bundle activator header
 *       and this attribute is set to <tt>true</tt> then a build
 *       failure is trigger.
 *   </td>
 *   <td valign=top>
 *     No.<br>
 *     Default value is "true"
 *   </td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>failOnClassPath</td>
 *   <td valign=top>
 *       If a non-existing entry is detected in the given bundle
 *       classpath header and this attribute is set to <tt>true</tt>
 *       then a build failure is trigger.
 *   </td>
 *   <td valign=top>
 *     No.<br>
 *     Default value is "true"
 *   </td>
 *  </tr>
 *  <tr>
 *   <td valign=top>addPackageinfoPackages</td>
 *   <td valign=top>
 *       If set to <tt>true</tt> then add packages that contains
 *       a <tt>packageinfo</tt> file, even if the package is empty.
 *   </td>
 *   <td valign=top>
 *     No.<br>
 *     Default value is "false"
 *   </td>
 *  </tr>
 * </table>
 *
 * <h3>Parameters specified as nested elements</h3>
 *
 * The set of provided packages (classes) is the union of the packages
 * found via &lt;exports&gt;, &lt;exportsBundleClasspath&gt; and
 * &lt;impls&gt;, &lt;implsBundleClasspath&gt; resource
 * collections. The <code>import-package</code> header is derived
 * (checked against) the set of provided packages.
 *
 * <h4>exports</h4>
 *
 * (optional)<br>
 *
 * <p>Nested &lt;exports&gt;-elements are filesets that will be
 * analyzed to determine the set of provided Java packages to be
 * exported by the bundle.</p>
 *
 * <p>Unsupported file types matched by the file set are ignored.</p>
 *
 * <h4>exportsBundleClasspath</h4>
 *
 * (optional)<br>
 *
 * <p>Nested &lt;exportsBundleClasspath&gt;-elements are instances of
 * {@link BundleClasspathTask} that will derive a list of file sets to
 * be analyzed to determine the set of provided Java packages to be
 * exported by the bundle.</p>
 *
 * <p>Unsupported file types matched by the file set are ignored.</p>
 *
 * <h4>impls</h4>
 *
 * (optional)<br>
 *
 * <p>Nested &lt;impls&gt;-elements are filesets that will be analyzed
 * to determine the set of private Java packages provided by the
 * bundle.</p>
 *
 * <p>Unsupported file types matched by the file set are ignored.</p>
 *
 * <h4>implsBundleClasspath</h4>
 *
 * (optional)<br>
 *
 * <p>Nested &lt;implsBundleClasspath&gt;-elements are instances of
 * {@link BundleClasspathTask} that will derive a list of file sets
 * are to be analyzed to determine the set of private Java packages
 * provided by the bundle.</p>
 *
 * <p>Unsupported file types matched by the file set are ignored.</p>
 *
 *
 * <h3>Examples</h3>
 *
 * <h4>Check all imports and activator in implementation classes</h4>
 *
 * <p>
 * This example assumes all implementation classes are in the package
 * <tt>test.impl.*</tt>
 * </p>
 *
 * <pre>
 *  &lt;bundleinfo  activator = "bmfa.Bundle-Activator"
 *               imports   = "impl.import.package"&gt;
 *   &lt;impls dir="classes" includes="test/impl/*"/&gt;
 *  &lt;/bundleinfo&gt;
 *  &lt;echo message="imports   = ${impl.import.package}"/&gt;
 *  &lt;echo message="activator = ${bmfa.Bundle-Activator}"/&gt;
 * </pre>
 *
 *
 * <h4>Check all imports and exports in API classes</h4>
 *
 * <p>
 * This example assumes all API classes are in the package
 * <tt>test.*</tt>
 * </p>
 *
 * <pre>
 *  &lt;bundleinfo  exports  = "api.export.package"
 *               imports  = "api.import.package"&gt;
 *   &lt;exports dir="classes" includes="test/*"/&gt;
 *   &lt;impls   dir="classes" includes="test/impl/*"/&gt;
 *  &lt;/bundleinfo&gt;
 *  &lt;echo message="imports  = ${api.import.package}"/&gt;
 *  &lt;echo message="exports  = ${api.export.package}"/&gt;
 * </pre>
 *
 */
public class BundleInfoTask extends Task {

  private final ArrayList<ResourceCollection> implsResourceCollections =
    new ArrayList<ResourceCollection>();
  private final List<ResourceCollection> exportsResourceCollections =
    new ArrayList<ResourceCollection>();

  private String importsProperty   = "";
  private String exportsProperty   = "";
  private String activatorProperty = "";
  private String serviceComponent  = "";
  private String fragmentHost      = "";
  private Version manifestVersion  = new Version("1");
  private boolean bUses            = true;

  private final Set<String> stdImports = new TreeSet<String>();

  private boolean bPrintClasses      = false;

  private boolean bImplicitImports       = true;
  private boolean bSetActivator          = true;
  private boolean bActivatorOptional     = false;
  private boolean failOnExports          = true;
  private boolean failOnImports          = true;
  private boolean failOnActivator        = true;
  private boolean failOnClassPath        = true;
  private boolean bImportsOnly           = false;
  private boolean addPackageinfoPackages = false;
  /**
   * The set of packages referenced by the included classes but not
   * provided by them.
   */
  private final TreeSet<String> importSet            = new TreeSet<String>();
  /**
   * A set of packages used by the included classes but not
   * referenced from them.
   */
  private final TreeSet<String> extraImportSet       = new TreeSet<String>();

  private final BundlePackagesInfo bpInfo = new BundlePackagesInfo(this);
  private final ClassAnalyserASM asmAnalyser
    = new ClassAnalyserASM(bpInfo, this);

  public BundleInfoTask() {
    setDefaultImports("");
    setStdImports("java.");
  }

  /**
   * Set property receiving list of imported packages.
   */
  public void setImports(String s) {
    this.importsProperty  = s;
  }

  public void setPrintClasses(String s) {
    this.bPrintClasses = "true".equals(s);
  }

  public void setFailOnExports(boolean b) {
    this.failOnExports = b;
  }

  public void setFailOnImports(boolean b) {
    this.failOnImports = b;
  }

  public void setFailOnActivator(boolean b) {
    this.failOnActivator = b;
  }

  public void setFailOnClassPath(boolean b) {
    this.failOnClassPath = b;
  }

  public void setImplicitImports(String s) {
    this.bImplicitImports = "true".equals(s);
  }

  /**
   * Set default import set.
   *
   * @param packageList Comma-separated list of package names.
   */
  public void setDefaultImports(String packageList) {
    importSet.clear();
    if (null!=packageList) {
      packageList = packageList.trim();
      if (0<packageList.length()) {
        @SuppressWarnings("unchecked")
        final Vector<String> v = StringUtils.split(packageList,',');
        for (final String pkg : v) {
          importSet.add(pkg.trim());
        }
      }
    }
  }


  /**
   * Set the extra imports set.
   *
   * @param packageList Comma-separated list of package names.
   */
  public void setExtraImports(String packageList) {
    extraImportSet.clear();
    if (null!=packageList) {
      packageList = packageList.trim();
      if (packageList.length()>0) {
        @SuppressWarnings("unchecked")
        final Vector<String> v = StringUtils.split(packageList,',');
        for (final String pkg : v) {
          extraImportSet.add(pkg.trim());
        }
      }
    }
  }


  /**
   * Set property receiving list of exported packages.
   */
  public void setExports(String propName) {
    this.exportsProperty = propName;
  }

  /**
   * Set property receiving any BundleActivator class.
   */
  public void setActivator(String propName) {
    this.activatorProperty = propName;
  }

  /**
   * Set name value of the Service-Component manifest header.
   */
  public void setServiceComponent(String serviceComponent) {
    this.serviceComponent = serviceComponent;
    if (!BundleManifestTask.isPropertyValueEmpty(this.serviceComponent)) {
      // A bundle with service components may have an activator but it
      // is not recommended.
      bActivatorOptional = true;
    }
  }

  /**
   * Set value of the Fragment-Host manifest header.
   */
  public void setFragmentHost(String fragmentHost) {
    this.fragmentHost = fragmentHost;
    if (!BundleManifestTask.isPropertyValueEmpty(this.fragmentHost)) {
      // A fragment bundle must not have an activator.
      bSetActivator = false;
    }
  }

  /**
   * Set value of the Bundle-ManifestVersion manifest header.
   */
  public void setManifestVersion(String manifestVersion) {
    this.manifestVersion = new Version(manifestVersion);
  }

  /**
   * Shall uses directives be added to the Export-Package header or not.
   */
  public void setUses(boolean uses) {
    this.bUses = uses;
  }

  /**
   * Set set of packages always imported.
   *
   * @param packageList Comma-separated list of package names.
   */
  public void setStdImports(String packageList) {
    stdImports.clear();
    stdImports.add("java.");
    @SuppressWarnings("unchecked")
    final Vector<String> v = StringUtils.split(packageList,',');
    for (final String imp : v) {
      stdImports.add(imp.trim());
    }
  }

  /**
   * Add a file set with classes that shall be exported.
   *
   * @param set The file set with exports to add.
   */
  public void addConfiguredExports(FileSet set) {
    final Restrict restrict = new Restrict();
    restrict.add(set);
    restrict.add(analyzeRestriction);
    exportsResourceCollections.add(restrict);
  }

  /**
   * Add file sets for classes exported from the bundle classpath.
   *
   * @param bcpt The bundle classpath that the bundle will have.
   */
  public void addConfiguredExportsBundleClasspath(BundleClasspathTask bcpt) {
    for (final Object element : bcpt.getFileSets(failOnClassPath)) {
      final Restrict restrict = new Restrict();
      restrict.add((ResourceCollection) element);
      restrict.add(analyzeRestriction);
      exportsResourceCollections.add(restrict);
    }
  }

  /**
   * Add a file set with classes that are private to the bundles.
   *
   * @param set The file set with private implementations to add.
   */
  public void addConfiguredImpls(FileSet set) {
    final Restrict restrict = new Restrict();
    restrict.add(set);
    restrict.add(analyzeRestriction);
    implsResourceCollections.add(restrict);
  }

  /**
   * Add file sets for private classes on the bundle classpath.
   *
   * @param bcpt The bundle classpath that the bundle will have.
   */
  public void addConfiguredImplsBundleClasspath(BundleClasspathTask bcpt) {
    for (final Object element : bcpt.getFileSets(failOnClassPath)) {
      final Restrict restrict = new Restrict();
      restrict.add((ResourceCollection) element);
      restrict.add(analyzeRestriction);
      implsResourceCollections.add(restrict);
    }
  }

  /**
   * If set to <tt>true</tt> then do not update Export-Package
   * manifest header.
   */
  public void setImportsOnly(boolean importsOnly) {
    this.bImportsOnly = importsOnly;
  }

  /**
   * If set to <tt>true</tt> then add packages that contains
   * a packageinfo file, even if the package is empty.
   */
  public void setAddPackageinfoPackages(boolean addPackageinfoPackages) {
    this.addPackageinfoPackages = addPackageinfoPackages;
  }

  // Implements Task
  /**
   * Analyze all resources in the exports and impls resource
   * collections to determine the set of Java packages provided by
   * this bundle. Update the given properties for export- and import
   * package accordingly.
   */
  @Override
  public void execute() throws BuildException {
    if (0==exportsResourceCollections.size()
        && 0==implsResourceCollections.size()) {
      throw new BuildException("Neither exports nor impls specified",
                               getLocation());
    }

    // First analyze the exports resource collections
    for (final Object element : exportsResourceCollections) {
      final ResourceCollection rc = (ResourceCollection) element;

      for (@SuppressWarnings("unchecked")
      final Iterator<Resource> rcIt = rc.iterator(); rcIt.hasNext();) {
        final Resource res = rcIt.next();
        log("Exports resource: "+res, Project.MSG_DEBUG);
        analyze(res);
      }
    }// Scan done

    // Get the sub-set of the provided packages that are the exports set
    final Set<String> providedExportSet = new TreeSet<String>(bpInfo.getProvidedPackages());
    final SortedSet<String> manifestExportSet = getPredefinedExportSet();
    if (null!=manifestExportSet) {
      // An Export-Package header was given it shall contain
      // precisely the provided export set of Java packages.
      if (!manifestExportSet.equals(providedExportSet)) {
        // Found export package mismatch
        log("Provided package to export:  " +providedExportSet,
            Project.MSG_ERR);
        log("Given Export-Package header: " +manifestExportSet,
            Project.MSG_ERR);
        final StringBuilder msg = new StringBuilder();
        final TreeSet<String> tmp = new TreeSet<String>(manifestExportSet);
        tmp.removeAll(providedExportSet);
        if (0<tmp.size()) {
          msg.append("The following non-provided packages are present in the ")
            .append("Export-Package header: ")
            .append(tmp.toString())
            .append(". ");
        }
        tmp.clear();
        tmp.addAll(providedExportSet);
        tmp.removeAll(manifestExportSet);
        if (0<tmp.size()) {
          if (0<msg.length()) {
            msg.append("\n");
          }
          msg.append("The following packages are missing from ")
            .append("the given Export-Package header: ")
            .append(tmp.toString())
            .append(".");
        }
        tmp.clear();
        if (failOnExports) {
          log(msg.toString(), Project.MSG_ERR);
          throw new BuildException(msg.toString(), getLocation());
        } else {
          log(msg.toString(), Project.MSG_WARN);
        }
      }
    }
    log("Provided packages to export: " +providedExportSet,
        Project.MSG_VERBOSE);


    // Analyze the impls resource collections to find all provided
    // Java packages.
    for (final Object element : implsResourceCollections) {
      final ResourceCollection rc = (ResourceCollection) element;

      for (@SuppressWarnings("unchecked")
      final Iterator<Resource> rcIt = rc.iterator(); rcIt.hasNext();) {
        final Resource res = rcIt.next();
        log("Impl resource: "+res, Project.MSG_DEBUG);
        analyze(res);
      }
    }// Scan done

    // Get the set of packages provided by the bundle
    bpInfo.toJavaNames(); // Make package name in bpInfo '.' separated.
    log("All provided packages: "
        +bpInfo.getProvidedPackagesAsExportPackageValue(),
        Project.MSG_VERBOSE);

    // created importSet from the set of unprovided referenced packages
    final SortedSet<String> unprovidedReferencedPackages
      = bpInfo.getUnprovidedReferencedPackages();
    log("Un-provided referenced packages: " +unprovidedReferencedPackages,
        Project.MSG_DEBUG);

    // The set of referenced packages that matches one of the
    // stdImport patterns.
    final SortedSet<String> ignoredReferencedPackages = new TreeSet<String>();

    // Remove all packages with names like "java.*" (full set of
    // patterns are given by the stdImports set). Such packages must
    // not be present in the importSet.
    for (final Iterator<String> urpIt = unprovidedReferencedPackages.iterator();
         urpIt.hasNext(); ) {
      final String pkgName = urpIt.next();
      if (isStdImport(pkgName)) {
        urpIt.remove();
        ignoredReferencedPackages.add(pkgName);
      }
    }
    log("Referenced packages to import: " +unprovidedReferencedPackages,
        Project.MSG_DEBUG);
    importSet.addAll(unprovidedReferencedPackages);

    final SortedSet<String> unprovidedExtraImportSet = new TreeSet<String>(extraImportSet);
    unprovidedExtraImportSet.removeAll(bpInfo.getProvidedPackages());
    log("Un-provided extra packages to import: " +unprovidedExtraImportSet,
        Project.MSG_DEBUG);
    importSet.addAll(unprovidedExtraImportSet);

    // The set of packages that will be mentioned in the
    // Export-Package or the Import-Package header.
    final Set<String> allImpExpPkgs = new TreeSet<String>(providedExportSet);
    allImpExpPkgs.addAll(importSet);

    bpInfo.postProcessUsingMap(ignoredReferencedPackages, allImpExpPkgs);
    //log(bpInfo.toString(), Project.MSG_INFO);

    // Data collection done - write back properties
    final Project proj = getProject();

    if(!"".equals(exportsProperty)) {
      // A property name for the Export-Package header value has been specified
      final String exportsVal = proj.getProperty(exportsProperty);
      if (BundleManifestTask.isPropertyValueEmpty(exportsVal)) {
        // No value given, shall it be derived?
        if (!bImportsOnly) {
          if (0==providedExportSet.size()) {
            proj.setProperty(exportsProperty,
                             BundleManifestTask.BUNDLE_EMPTY_STRING);
            log("No packages exported, leaving '" +exportsProperty +"' empty.",
                Project.MSG_VERBOSE);
          } else {
            final String newExportsVal
              = buildExportPackagesValue(providedExportSet);
            log("Setting '" +exportsProperty +"' to '"+newExportsVal +"'",
                Project.MSG_VERBOSE);
            proj.setProperty(exportsProperty, newExportsVal);
          }
        }
      } else {
        // Export-Package given; add version and uses directives.
        final String newExportsVal = validateExportPackagesValue(exportsVal);
        if (!exportsVal.equals(newExportsVal)) {
          log("Updating \"" +exportsProperty +"\" to \""+newExportsVal +"\"",
              Project.MSG_VERBOSE);
          proj.setProperty(exportsProperty, newExportsVal);
        }
      }
    }


    if(!"".equals(importsProperty)) {
      String importsVal = proj.getProperty(importsProperty);
      if (BundleManifestTask.isPropertyValueEmpty(importsVal)) {
        // No Import-Package given; use derived value.
        if (0==importSet.size()) {
          log("No packages to import, leaving \"" +importsProperty +"\" empty.",
              Project.MSG_VERBOSE);
          proj.setProperty(importsProperty,
                           BundleManifestTask.BUNDLE_EMPTY_STRING);
        } else {
          importsVal = toString(importSet, ",");
          log("Setting \"" +importsProperty +"\" to \""+importsVal +"\"",
              Project.MSG_VERBOSE);
          proj.setProperty(importsProperty, importsVal);
        }
      } else {
        // Import-Package given; check that all derived packages are
        // present and that there are no duplicated packages.
        final TreeSet<String> givenImportSet = new TreeSet<String>();

        final List<HeaderEntry> entries =
          Util.parseManifestHeader(Constants.IMPORT_PACKAGE, importsVal, false,
                                   true, false);
        for (final HeaderEntry entry : entries) {
          for (final String pkgName : entry.getKeys()) {
            if (!givenImportSet.add(pkgName)) {
              final String msg =
                "The package '" + pkgName
                    + "' is mentioned twice in the given '"
                    + Constants.IMPORT_PACKAGE + "' manifest " + "header: '"
                    + importsVal + "'.";
              log(msg, Project.MSG_ERR);
              throw new BuildException(msg, getLocation());
            }
          }
        }

        givenImportSet.removeAll(bpInfo.getProvidedPackages());
        final TreeSet<String> missingImports = new TreeSet<String>(importSet);
        missingImports.removeAll(givenImportSet);
        if (0<missingImports.size()) {
          log("External packages: "+importSet,      Project.MSG_ERR);
          log("Imported packages: "+givenImportSet, Project.MSG_ERR);
          log("Provided packages: "+bpInfo.getProvidedPackages(),
              Project.MSG_ERR);

          final String msg = "The following external packages are used by "
            +"the bundle but not mentioned in the Import-Package manifest "
            +"header: " +missingImports;
          log(msg, Project.MSG_ERR );
          if (failOnImports) {
            throw new BuildException(msg, getLocation());
          }
        }
        final TreeSet<String> extraImports = new TreeSet<String>(givenImportSet);
        extraImports.removeAll(importSet);
        if (0<extraImports.size()) {
          log("External packages: "+importSet, Project.MSG_ERR);
          log("Imported packages: "+givenImportSet, Project.MSG_ERR);

          log("The following packages are mentioned in the Import-Package"
              +" manifest header but not used by the included classes: "
             +extraImports, Project.MSG_WARN );
        }
      }
    }

    // Try to be a bit clever when writing back bundle activator
    if(!"".equals(activatorProperty) && bSetActivator) {
      final String activatorVal = proj.getProperty(activatorProperty);
      if (BundleManifestTask.isPropertyValueEmpty(activatorVal)) {
        // No Bundle-Activator given; use derived value if possible.
        switch(bpInfo.countProvidedActivatorClasses()) {
        case 0:
          if (!bActivatorOptional) {
            final String msg1 = "Requested to derive Bundle-Activator but "
              +"there is no class implementing BundleActivator.";
            log(msg1, Project.MSG_ERR);
            if (failOnActivator) {
              throw new BuildException(msg1, getLocation());
            }
          }
          break;
        case 1:
          final String clazz = bpInfo.getActivatorClass();
          proj.setProperty(activatorProperty, clazz);
          break;
        default:
          final String msg2 = "Manual selection of Bundle-Activator "
            +"is needed since the set of included classes contains "
            +"more than one candidate: "
            +bpInfo.providedActivatorClassesAsString();
          log(msg2, Project.MSG_ERR);
          if (failOnActivator) {
            throw new BuildException(msg2, getLocation());
          }
        }
      } else {
        // Bundle-Activator given; check that it is correct.
        if (0==bpInfo.countProvidedActivatorClasses()) {
          log("No class implementing BundleActivator found", Project.MSG_ERR);
        } else {
          final String givenClazz = proj.getProperty(activatorProperty).trim();
          if (!bpInfo.providesActivatorClass(givenClazz)) {
            final String msg = "The specified BundleActivator '" +givenClazz
              +"' is not a member of the set of included classes that"
              +"  implements BundleActivator: "
              +bpInfo.providedActivatorClassesAsString();
            log(msg, Project.MSG_WARN);
            if (failOnActivator) {
              throw new BuildException(msg, getLocation());
            }
          }
        }
      }
    }

    for (final Object element : bpInfo.getReferencedClasses()) {
      final String s = (String)element;
      if(s.endsWith("[]")) {
      } else {
        if(!bpInfo.providesClass(s)) {
          if(bPrintClasses) {
            System.out.println(s);
          }
        }
      }
    }

    /* Handle the implicitImport flag. */
    if(bImplicitImports
       && !"".equals(importsProperty)
       && !"".equals(exportsProperty) ) {
      String importsSpec = proj.getProperty(importsProperty);
      log("implicitImport - before: "+importsSpec, Project.MSG_VERBOSE);
      importSet.clear();
      if (!BundleManifestTask.isPropertyValueEmpty(importsSpec)) {
        final List<HeaderEntry> entries =
            Util.parseManifestHeader(Constants.IMPORT_PACKAGE, importsSpec, false, true, false);
        for (final HeaderEntry entry : entries) {
          for (final String pkgName : entry.getKeys()) {
            importSet.add(pkgName);
          }
        }
      } else {
        // Spec is empty, must remove the emtpy value marker for now.
        importsSpec = "";
      }
      final StringBuilder sb = new StringBuilder(importsSpec);

      final String exportsSpec = proj.getProperty(exportsProperty);
      if (!BundleManifestTask.isPropertyValueEmpty(exportsSpec)) {
        final List<HeaderEntry> entries =
            Util.parseManifestHeader(Constants.EXPORT_PACKAGE, exportsSpec, false, true, false);

        for (final HeaderEntry entry : entries) {
          for (String pkg : entry.getKeys()) {
          if (!importSet.contains(pkg)) {
            final String ver = (String) entry.getAttributes().get(Constants.VERSION_ATTRIBUTE);
            @SuppressWarnings("deprecation")
			final String sver = (String) entry.getAttributes().get(Constants.PACKAGE_SPECIFICATION_VERSION);
            if (null!=ver) {
              pkg += ";version=" +toDefaultVersionRange(ver);
            } else if (null!=sver) {
              pkg += ";specification-version=" +toDefaultVersionRange(sver);
            } else {
              pkg += ";version="
                +toDefaultVersionRange(Version.emptyVersion.toString());
            }
            log("implicitImport - adding: "+pkg, Project.MSG_DEBUG);
            if (0<sb.length()) {
              sb.append(",");
            }
            sb.append(pkg);
          }
        }
        }

        importsSpec = sb.toString();
        if (0==importsSpec.length()) {
          importsSpec = BundleManifestTask.BUNDLE_EMPTY_STRING;
        }
        log("implicitImport - after: "+importsSpec, Project.MSG_VERBOSE);
        proj.setProperty(importsProperty, importsSpec );
      }
    }
  }

  private boolean doUses()
  {
    return bUses && manifestVersion.compareTo(new Version("1")) > 0;
  }


  /**
   * Get the set of Java packages to export according to the given
   * <code>Export-Package</code> header.
   *
   * @return the set of Java packages to export or null if no
   * <code>Export-Package</code> header was given.
   */
  private SortedSet<String> getPredefinedExportSet()
  {
    if(!"".equals(exportsProperty)) {
      final String exportsVal = getProject().getProperty(exportsProperty);

      if (!BundleManifestTask.isPropertyValueEmpty(exportsVal)) {
        log("Found non-empty Export-Package attribute: '" +exportsVal +"'",
            Project.MSG_DEBUG);

        final TreeSet<String> res = new TreeSet<String>();
        final List<HeaderEntry> entries =
            Util.parseManifestHeader(Constants.EXPORT_PACKAGE, exportsVal, false, true, false);

        for (final HeaderEntry entry : entries) {
          res.addAll(entry.getKeys());
        }
        return res;
      }
    }
    return null;
  }


  private void appendUsesDirective(final StringBuilder sb, final String pkgName)
  {
    if (doUses()) {
      final String sep = ",";
      final Set<String> usesPkgs = bpInfo.getPackagesReferencedFromPackage(pkgName);

      if (null!=usesPkgs && 0<usesPkgs.size()) {
        sb.append(";uses:=");
        if (1<usesPkgs.size()) {
          sb.append("\"");
        }
        for (final Iterator<String> usesIt = usesPkgs.iterator(); usesIt.hasNext(); ) {
          final String usesPkg = usesIt.next();
          sb.append(usesPkg);
          if (usesIt.hasNext()) {
            sb.append(sep);
          }
        }
        if (1<usesPkgs.size()) {
          sb.append("\"");
        }
      }
    }
  }

  /**
   * Return the value of the Export-Package based on the analysis.
   *
   * @param exportPackages The sub-set of provided packages to be exported.
   */
  protected String buildExportPackagesValue(final Set<String> exportPackages) {
    final String sep = ",";
    final String versionPrefix = ";version=";

    final StringBuilder sb = new StringBuilder();

    for(final String pkgName : exportPackages) {
      if (sb.length() > 0) {
        sb.append(sep);
      }

      sb.append(pkgName);

      final Version pkgVersion = bpInfo.getProvidedPackageVersion(pkgName);
      if (null!=pkgVersion) {
        sb.append(versionPrefix).append(pkgVersion);
      }

      appendUsesDirective(sb, pkgName);
    }
    return sb.toString();
  }

  /**
   * Validate a given Export-Package header value. Check existing
   * version parameters, add missing package versions. Check uses
   * directives and add those that are missing.
   *
   * @param oldExportsVal the Export-Package value to validate and update.
   * @throws BuildException when conflicting version specifications
   *         are found for a package.
   */
  protected String validateExportPackagesValue(final String oldExportsVal)
  {
    final StringBuilder sb = new StringBuilder();
    final String sep = ",";

    // TODO handle multiple keys per header entry (must not use destructive
    // operations on attributes and directives).
    final List<HeaderEntry> entries =
        Util.parseManifestHeader(Constants.EXPORT_PACKAGE, oldExportsVal, true, true, false);
    for (final HeaderEntry entry : entries) {
      for (final String pkgName : entry.getKeys()) {
        if (!bpInfo.providesPackage(pkgName)) {
          final String msg = "The package '"+pkgName +"' is in the Export-Package"
            +" manifest header, but there is no class belonging to it."
            +" The following packages are provided: "
            +bpInfo.getProvidedPackages();
          log(msg, Project.MSG_ERR );
          if (failOnExports) {
            throw new BuildException(msg, getLocation());
          }
        }

        if (sb.length()>0) {
          sb.append(sep);
        }
        sb.append(pkgName);

        // Add / check package version
        String versionKey = "version";
        String versionStr = (String) entry.getAttributes().remove(versionKey);
        if (null==versionStr) {
          // Fallback to pre OSGi R4 name
          versionKey = "specification-version";
          versionStr = (String) entry.getAttributes().remove(versionKey);
        }
        Version version = null;
        if (null!=versionStr) {
          try {
            version = new Version(versionStr);
          } catch (final Exception e) {
            final String msg = "Found invalid version value in given "
              +"Export-Package header for the package '"
              +pkgName +"': '"+versionStr +"'; Error: "
              +e.getMessage();
            log(msg, Project.MSG_ERR );
            throw new BuildException(msg, e);
          }
        }
        final Version curVersion = bpInfo.getProvidedPackageVersion(pkgName);
        if (null==version && null!=curVersion) {
          // Version is missing, add it
          version = curVersion;
        } else if (null!=version && null!=curVersion
                   && !version.equals(curVersion)) {
          final String msg = "Found conflicting versions for the package '"
            +pkgName +"'. The Export-Package header says '" +version +"', but '"
            +bpInfo.getProvidedPackageVersionSource(pkgName) +" claims '"
            +curVersion +"'.";

          log(msg, Project.MSG_ERR );
          throw new BuildException(msg);
        }
        if (null!=version) {
          // Finally insert version into the new Export-Package value
          sb.append(";").append(versionKey).append("=").append(version);
        }

        if (doUses()) {
          if (!entry.getDirectives().containsKey(Constants.USES_DIRECTIVE)) {
            appendUsesDirective(sb, pkgName);
          } else {
            // Validate the given uses directive.
            final Set<String> usesPkgs = bpInfo.getPackagesReferencedFromPackage(pkgName);
            final String usesValue = entry.getDirectives().remove(Constants.USES_DIRECTIVE);
            final TreeSet<String> uPkgsMan = new TreeSet<String>
              (Arrays.asList(Util.splitwords(usesValue, ", \t", '"')));
            final Set<String> uPkgsMis = new TreeSet<String>(usesPkgs);
            uPkgsMis.removeAll(uPkgsMan);
            if (0<uPkgsMis.size()) {
              final String msg = "The package '"+pkgName
                +"' in the Export-Package"
                +" manifest header has a usage directive with value '"
                +usesValue +"', but the following packages are are also used: '"
                +uPkgsMis +"' and should be added to the 'uses'-directive.";
              log(msg, Project.MSG_ERR );
              if (failOnExports) {
                throw new BuildException(msg, getLocation());
              }
            }
            final Set<String> uPkgsExtra = new TreeSet<String>(uPkgsMan);
            uPkgsExtra.removeAll(usesPkgs);
            if (0<uPkgsExtra.size()) {
              final String msg = "The package '"+pkgName
                +"' in the Export-Package"
                +" manifest header has a usage directive with value '" +usesValue
                +"', that contains the following unexpected (unused) packages: '"
                +uPkgsExtra +"' that could be removed from the 'uses'-directive.";
              log(msg, Project.MSG_ERR );
            }
          }
        }
        for (final Entry<String, Object> paramEntry : entry.getAttributes().entrySet()) {
          final String paramName = paramEntry.getKey();
          final String paramValue = paramEntry.getValue().toString();

          sb.append(";");
          sb.append(paramName);
          sb.append("=");
          final boolean quoteNeeded = -1<paramValue.indexOf(',')
            || -1<paramValue.indexOf(';')
            || -1<paramValue.indexOf(':')
            || -1<paramValue.indexOf('=');
          if (quoteNeeded) {
            sb.append("\"");
          }
          sb.append(paramValue);
          if (quoteNeeded) {
            sb.append("\"");
          }
        }
        for (final Entry<String, String> directiveEntry : entry.getDirectives().entrySet()) {
          final String directiveName = directiveEntry.getKey();
          final String directiveValue = directiveEntry.getValue();

          sb.append(";");
          sb.append(directiveName);
          sb.append(":=");
          final boolean quoteNeeded = -1<directiveValue.indexOf(',')
            || -1<directiveValue.indexOf(';')
            || -1<directiveValue.indexOf(':')
            || -1<directiveValue.indexOf('=');
          if (quoteNeeded) {
            sb.append("\"");
          }
          sb.append(directiveValue);
          if (quoteNeeded) {
            sb.append("\"");
          }
        }
      }
    }
    return sb.toString();
  }


  /**
   * Analyze a resource by checking its suffix and delegate to {@link
   * #analyzeClass(Resource)} or {@link
   * #analyzePackageinfo(Resource)}.
   *
   * @param res The resource to be analyzed.
   */
  protected void analyze(Resource res) throws BuildException {
    if(res.getName().endsWith(".class")) {
      analyzeClass(res);
    } else if(res.getName().endsWith("packageinfo")) {
      analyzePackageinfo(res);
    } else {
      // Just ignore all other files
    }
  }

  protected boolean isImported(String className) {
    for (final Object element : importSet) {
      final String pkg = (String)element;
      if(className.startsWith(pkg)) {
        final String rest = className.substring(pkg.length() + 1);
        if(-1 == rest.indexOf(".")) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Check if package is included in the prefix list of packages that
   * does not need to be in the Import-Package list.
   *
   * @return <tt>true</tt> if name is prefixed with any of the elements in
   *         <tt>stdImports</tt> set, <tt>false</tt> otherwise.
   */
  protected boolean isStdImport(String name) {
    for (final Object element : stdImports) {
      final String s = (String)element;
      if(name.startsWith(s)) {
        return true;
      }
    }
    return false;
  }


  protected void analyzeClass(Resource res) throws BuildException {
    log("Analyze class file " + res, Project.MSG_VERBOSE);

    try {
      asmAnalyser.analyseClass(res.getInputStream(), res.toString());
    } catch (final Exception e) {
      e.printStackTrace();
      throw new BuildException("Failed to analyze class-file "
                               +res + ", exception=" + e,
                               getLocation());
    }
  }

  protected void analyzePackageinfo(Resource res) throws BuildException {
    log("Analyze packageinfo file " + res, Project.MSG_VERBOSE);

    final String pkgName = bpInfo.setPackageVersion(res);
    if (addPackageinfoPackages && pkgName != null) {
      bpInfo.addProvidedPackage(pkgName);
    }
  }


  /**
   * Given an OSGi version number on the form
   * <code>Major.Minor.Micro.Qualifier</code> return a version range
   * of the form <code>"[Major.Minor.Micro.Qualifier,Major)"</code>.
   *
   * @param version the version number to convert to a version range.
   * @return A quoted version range string.
   */
  public static final String toDefaultVersionRange(final String version)
  {
    final Version v = new Version(version);
    return "\"[" +v.toString() +"," +String.valueOf(v.getMajor()+1) +")\"";
  }


  public static final Set<String> ANALYZE_SUFFIXES = new TreeSet<String>() {
    private static final long serialVersionUID = 2628940819429424154L;
    {
      add(".class");
      add("/packageinfo"); // packageinfo-files inside jar-files
      add(File.separatorChar + "packageinfo"); // packageinfo-files in
                                               // the file system
    }
  };


  /**
   * A resource selector that selects files to be analyzed. I.e., it selects
   * <code>.class</code>-files and <code>packageinfo</code>-files.
   */
  public static final ResourceSelector analyzeRestriction =
    new ResourceSelector() {
      @Override
      public boolean isSelected(final Resource r)
      {
        for (final String suffix : BundleInfoTask.ANALYZE_SUFFIXES) {
          if (r.getName().endsWith(suffix)) {
            return true;
          }
        }
        return false;
      }
    };

  /**
   * Convert Set elements to a string.
   *
   * @param separator String to use as separator between elements.
   */
  static protected String toString(Set<String> set, String separator) {
    final StringBuilder sb = new StringBuilder();

    for(final String name : set) {
      if(sb.length() > 0) {
        sb.append(separator);
      }
      sb.append(name);
    }
    return sb.toString();
  }

}
