/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.DescendingVisitor;
import org.apache.bcel.classfile.EmptyVisitor;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Utility;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.Type;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.StringUtils;

import org.osgi.framework.Version;

/**
 * Task that analyzes a set of java sources or class files, and lists all
 * imported and defined packages. Also tries to find any class implementing
 * <tt>org.osgi.framework.BundleActivator</tt>.
 *
 * <ul>
 * <li><b>Class files</b>
 * <p>
 * Java class files are analyzed using the BCEL lib available from
 * <a href="http://jakarta.apache.org/bcel">http://jakarta.apache.org/bcel</a>.
 * <br>
 * <b>Note</b>: Scanning <i>only</i> applies
 * to the listed files - a complete closure of all referenced classes is
 * not done.
 * </p>
 *
 * <li><b>Source code</b>
 * <p>
 * Java source code is analyzed using very simple line-based scanning of
 * files. <br>
 * <b>Note</b>: Source code analysis does not attempt to find any
 * <tt>BundleActivator</tt>
 * </p>
 *
 * <li><b>Jar files</b>
 * <p>
 * Jar file analysis is not yet implemented
 * </p>
 *
 * </ul>
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
 *       imported. They are allways made available to the bundle by
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
 *       Flag for adding all exported packages to the import list.
 *       <p>
 *       If set to "true", the task will add all packages mentioned in
 *       the property named by <code>exports</code> to the list of
 *       imported packages in the property named by <code>imports</code>.
 *       This emulates the implicit import behaviour present in OSG R1-3.
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
 *       <p>
 *       If set to non-empty, leave the value of the activator
 *       property untouched and do not complain if there are no class
 *       that implements BundleActivator in the bundle.
 *       </p>
 *  </td>
 *   <td valign=top>No.<br> Default value is ""</td>
 *  </tr>
 *  <tr>
 *   <td valign=top>fragmentHost</td>
 *   <td valign=top>
 *       The value of the <tt>Fragment-Host</tt> manifest header.
 *       <p>
 *       If set to non-empty (i.e., this is a fragment bundle), leave
 *       the value of the activator property untouched and do not
 *       complain if there are no class that implements
 *       BundleActivator in the bundle.
 *       </p>
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
 *   <td valign=top>checkMinimumEE</td>
 *   <td valign=top>
 *       Flag for testing for the Minum Execution Environment
 *       <p>
 *       If set to "true", the task will check if all used classes
 *       is in the set of the Minimum Execution Environment.
 *       </p>
 *   </td>
 *   <td valign=top>
 *     No.<br>
 *     Default value is "false"
 *   </td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>checkSMFEE</td>
 *   <td valign=top>
 *       Flag for testing for the SMF Execution Environment
 *       <p>
 *       If set to "true", the task will check if all used classes
 *       is in the set of the SMF profile Execution Environment.
 *       </p>
 *   </td>
 *   <td valign=top>
 *     No.<br>
 *     Default value is "false"
 *   </td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>checkFoundationEE</td>
 *   <td valign=top>
 *       Flag for testing for the Foundation Execution Environment
 *       <p>
 *       If set to "true", the task will check if all used classes
 *       is in the set of the OSGi Foundation Execution Environment.
 *       </p>
 *   </td>
 *   <td valign=top>
 *     No.<br>
 *     Default value is "false"
 *   </td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>failOnExports</td>
 *   <td valign=top>
 *       If an error is detected in the given export package header
 *       and this attibute is set to <tt>true</tt> then a build
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
 *       and this attibute is set to <tt>true</tt> then a build
 *       failure is trigger.
 *   </td>
 *   <td valign=top>
 *     No.<br>
 *     Default value is "true"
 *   </td>
 *  </tr>
 * </table>
 *
 * <h3>Parameters specified as nested elements</h3>
 * <h4>fileset</h4>
 *
 * (required)<br>
 * <p>
 * All files must be specified as a fileset. Unsupported file types
 * are ignored.
 * </p>
 *
 * <h3>Examples</h3>
 *
 * <h4>Check all imports and activator in implementation classes</h4>
 *
 * <p>
 * This example assumes all implemention classes are in the package
 * <tt>test.impl.*</tt>
 * </p>
 *
 * <pre>
 *  &lt;bundleinfo  activator = "bmfa.Bundle-Activator"
 *               imports   = "impl.import.package"&gt;
 *   &lt;fileset dir="classes" includes="test/impl/*"/&gt;
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
 *   &lt;fileset dir="classes" includes="test/*"/&gt;
 *  &lt;/bundleinfo&gt;
 *  &lt;echo message="imports  = ${api.import.package}"/&gt;
 *  &lt;echo message="exports  = ${api.export.package}"/&gt;
 * </pre>
 *
 */
public class BundleInfoTask extends Task {

  private Vector    filesets = new Vector();
  private FileUtils fileUtils;

  private String importsProperty   = "";
  private String exportsProperty   = "";
  private String activatorProperty = "";
  private String mainProperty      = "";
  private String serviceComponent  = "";
  private String fragmentHost      = "";
  private Version manifestVersion  = new Version("1");
  private boolean bUses            = true;

  private Set     stdImports       = new TreeSet();
  private boolean bDebug           = false;

  private boolean bPrintClasses      = false;

  private boolean bCheckFoundationEE = false;
  private boolean bCheckMinimumEE    = false;
  private boolean bCheckSMFEE        = false;
  private boolean bImplicitImports   = true;
  private boolean bSetActivator      = true;
  private boolean failOnExports      = true;
  private boolean failOnImports      = true;
  private boolean bImportsOnly        = false;

  /** The set of packages that are provided by the inlcuded classes. */
  private Set providedSet          = new TreeSet();

  /** The set of packages that are referenced by the inlcuded classes. */
  private Set referencedSet        = new TreeSet();

  /** The sub set of the included classes that implements BundleActivator. */
  private Set activatorSet         = new TreeSet();
  /**
   * The set of packages referenced by the included classes but not
   * provided by them.
   */
  private Set importSet            = new TreeSet();
  /**
   * A set of packages used by the included classes but not
   * referenced from them.
   */
  private Set extraImportSet       = new TreeSet();

  /**
   * The set of classes that are refrenced from the included classes.
   */
  private Set classSet             = new TreeSet();
  /**
   * The set of included classes.
   */
  private Set ownClasses           = new TreeSet();
  /**
   * A mapping from package name of included classes to a set of
   * package names that the classes in the key package references.
   */
  private Map packageUsingMap      = new HashMap();


  public BundleInfoTask() {
    fileUtils = FileUtils.newFileUtils();
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

  public void setCheckFoundationEE(String s) {
    this.bCheckFoundationEE = "true".equals(s);
  }

  public void setCheckMinimumEE(String s) {
    this.bCheckMinimumEE = "true".equals(s);
  }

  public void setCheckSMFEE(String s) {
    this.bCheckSMFEE = "true".equals(s);
  }

  public void setFailOnExports(boolean b) {
    this.failOnExports = b;
  }

  public void setFailOnImports(boolean b) {
    this.failOnImports = b;
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
        Vector v = StringUtils.split(packageList,',');
        importSet.addAll(v);
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
        Vector v = StringUtils.split(packageList,',');
        extraImportSet.addAll(v);
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
      bSetActivator = false;
    }
  }

  /**
   * Set value of the Fragment-Host manifest header.
   */
  public void setFragmentHost(String fragmentHost) {
    this.fragmentHost = fragmentHost;
    if (!BundleManifestTask.isPropertyValueEmpty(this.fragmentHost)) {
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
   * Set property receiving any Main class.
   * <p>
   * Not yet implemented.
   * </p>
   */
  public void setMain(String propName) {
    this.mainProperty = propName;
  }


  /**
   * Set set of packages always imported.
   *
   * @param packageList Comma-separated list of package names.
   */
  public void setStdImports(String packageList) {
    stdImports.clear();
    stdImports.add("java.");
    stdImports.addAll(StringUtils.split(packageList.trim(),','));
  }

  public void addFileset(FileSet set) {
    filesets.addElement(set);
  }

  /**
   * Shall uses directives be added to the Export-Package header or not.
   */
  public void setImportsOnly(boolean importsOnly) {
    this.bImportsOnly = importsOnly;
  }

  // Implements Task
  //
  // Scan all files in fileset and delegate to analyze()
  // then write back values to properties.
  public void execute() throws BuildException {
    if (filesets.size() == 0) {
      throw new BuildException("No fileset specified", getLocation());
    }

    for (int i = 0; i < filesets.size(); i++) {
      FileSet fs = (FileSet) filesets.elementAt(i);
      DirectoryScanner ds = fs.getDirectoryScanner(project);
      File fromDir = fs.getDir(project);

      String[] srcFiles = ds.getIncludedFiles();
      String[] srcDirs = ds.getIncludedDirectories();

      for (int j = 0; j < srcFiles.length ; j++) {
        analyze(new File(fromDir, srcFiles[j]));
      }

      for (int j = 0; j < srcDirs.length ; j++) {
        analyze(new File(fromDir, srcDirs[j]));
      }
    }// Scan done

    // created importSet from referencedSet
    for (Iterator refIt=referencedSet.iterator(); refIt.hasNext(); ) {
      final String refPkg = (String) refIt.next();
      // only packages defined outside this set of files
      if(!providedSet.contains(refPkg)) {
        // ...and only add non-std packages
        if(!isStdImport(refPkg)) {
          log("   " +refPkg +" added ; un-provided non-standard package.",
              Project.MSG_DEBUG);
          importSet.add(refPkg);
        } else {
          log("   " +refPkg +" skiped; standard import.", Project.MSG_DEBUG);
        }
      } else {
        log("   " +refPkg +" skiped; package provided.", Project.MSG_DEBUG);
      }
    }
    final TreeSet unprovidedExtraImportSet = new TreeSet(extraImportSet);
    unprovidedExtraImportSet.removeAll(providedSet);
    importSet.addAll(unprovidedExtraImportSet);

    // The sub-set of the provided packages that export
    final Set providedExportSet = getProvidedExportSet();

    // The set of packages that will be mentioned in the
    // Export-Package or the Import-Package header.
    final Set allImpExpPkgs = new TreeSet(providedExportSet);
    allImpExpPkgs.addAll(importSet);

    // Clean up the usingPackageMap; remove all stdimports, any self
    // reference, ensure that it only contains imported or exported packages.
    for (Iterator usingMapEntryIt=packageUsingMap.entrySet().iterator();
         usingMapEntryIt.hasNext(); ) {
      final Map.Entry entry = (Map.Entry) usingMapEntryIt.next();
      final Set usedSet = (Set) entry.getValue();
      usedSet.remove((String) entry.getKey());
      for (Iterator usedIt=usedSet.iterator(); usedIt.hasNext(); ) {
        final String usedPkg = (String) usedIt.next();
        if (isStdImport(usedPkg)) usedIt.remove();
      }
      usedSet.retainAll(allImpExpPkgs);
    }

    // Data collection done - write back properties
    final Project proj = getProject();

    if(!"".equals(exportsProperty)) {
      String exportsVal = proj.getProperty(exportsProperty);
      if (BundleManifestTask.isPropertyValueEmpty(exportsVal)) {
	if (!bImportsOnly) {
	  if (0==providedSet.size()) {
	    proj.setProperty(exportsProperty,
			     BundleManifestTask.BUNDLE_EMPTY_STRING);
	    log("No packages exported, leaving \"" +exportsProperty +"\" empty.",
		Project.MSG_VERBOSE);
	  } else {
	    exportsVal = buildExportPackagesValue();
	    log("Setting \"" +exportsProperty +"\" to \""+exportsVal +"\"",
		Project.MSG_VERBOSE);
	    proj.setProperty(exportsProperty, exportsVal);
	  }
	}
      } else {
        // Export-Package given; check that they are provided.
        final String newExportsVal = validateExportPackagesValue(exportsVal);
        log("Updating \"" +exportsProperty +"\" to \""+newExportsVal +"\"",
            Project.MSG_VERBOSE);
        proj.setProperty(exportsProperty, newExportsVal);
      }
    }


    if(!"".equals(importsProperty)) {
      String importsVal = proj.getProperty(importsProperty);
      if (BundleManifestTask.isPropertyValueEmpty(importsVal)) {
        // No Import-Package given; use derived value.
        log("importSet.size()="+importSet.size(),Project.MSG_VERBOSE);
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
        final TreeSet givenImportSet = new TreeSet();
        final Iterator impIt = Util.parseEntries("import.package",importsVal,
                                                 true, true, false );
        while (impIt.hasNext()) {
          final Map impEntry = (Map) impIt.next();
          final String pkgName = (String) impEntry.get("$key");
          if (!givenImportSet.add( pkgName )) {
            final String msg = "The package '" +pkgName
              +"' is mentioned twice in the given 'Import-Package' manifest "
              +"header: '" +importsVal +"'.";
            log(msg, Project.MSG_ERR);
            throw new BuildException(msg, getLocation());
          }
        }
        givenImportSet.removeAll(providedSet);
        final TreeSet missingImports = new TreeSet(importSet);
        missingImports.removeAll(givenImportSet);
        if (0<missingImports.size()) {
          log("External packages: "+importSet,      Project.MSG_ERR);
          log("Imported packages: "+givenImportSet, Project.MSG_ERR);
          log("Provided packages: "+providedSet,    Project.MSG_ERR);

          final String msg = "The following external packages are used by "
            +"the bundle but not mentioned in the Import-Package manifest "
            +"header: " +missingImports;
          log(msg, Project.MSG_ERR );
          if (failOnImports) {
            throw new BuildException(msg, getLocation());
          }
        }
        final TreeSet extraImports = new TreeSet(givenImportSet);
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
        switch(activatorSet.size()) {
        case 0:
          final String msg1 = "Requested to derive Bundle-Activator but "
            +"there is no class implementing BundleActivator.";
          log(msg1, Project.MSG_ERR);
          throw new BuildException(msg1, getLocation());
        case 1:
          String clazz = (String)activatorSet.iterator().next();
          proj.setProperty(activatorProperty, clazz);
          break;
        default:
          final String msg2 = "Manual selection of Bundle-Activator "
            +"is needed since the set of included classes contains "
            +"more than one candidate: "+activatorSet;
          log(msg2, Project.MSG_ERR);
          throw new BuildException(msg2, getLocation());
        }
      } else {
        // Bundle-Activator given; check that it is correct.
        if (0==activatorSet.size()) {
          log("No class implementing BundleActivator found", Project.MSG_ERR);
        } else {
          final String givenClazz = proj.getProperty(activatorProperty).trim();
          final String msg = "The specified BundleActivator '" +givenClazz
                +"' is not a member of the set of included classes that"
                +"  implements BundleActivator: " +activatorSet;
          if (!activatorSet.contains(givenClazz)) {
            log(msg, Project.MSG_WARN);
            throw new BuildException(msg, getLocation());
          }
        }
      }
    }

    final Set foundationMissing = new TreeSet();
    final Set minimumMissing    = new TreeSet();
    final Set smfMissing        = new TreeSet();

    for(Iterator it = classSet.iterator(); it.hasNext();) {
      final String s = (String)it.next();
      if(s.endsWith("[]")) {
      } else {
        if(!ownClasses.contains(s)) {
          if(bPrintClasses) {
            System.out.println(s);
          }
          if(!EE.isFoundation(s)) {
            if(!isImported(s)) {
              foundationMissing.add(s);
            }
          }
          if(!EE.isMinimum(s)) {
            if(!isImported(s)) {
              minimumMissing.add(s);
            }
          }
          if(!EE.isSMF(s)) {
            if(!isImported(s)) {
              smfMissing.add(s);
            }
          }
        }
      }
    }

    if(bCheckFoundationEE) {
      checkEE(foundationMissing, "foundation");
    }

    if(bCheckMinimumEE) {
      checkEE(minimumMissing, "minimum");
    }

    if(bCheckSMFEE) {
      checkEE(smfMissing, "SMF");
    }

    /* Handle the implicitImport flag. */
    if(bImplicitImports
       && !"".equals(importsProperty)
       && !"".equals(exportsProperty) ) {
      String importsSpec = proj.getProperty(importsProperty);
      log("implicitImport - before: "+importsSpec, Project.MSG_VERBOSE);
      importSet.clear();
      if (!BundleManifestTask.isPropertyValueEmpty(importsSpec)) {
        final Iterator impIt = Util.parseEntries("import.package",importsSpec,
                                                 true, true, false );
        while (impIt.hasNext()) {
          final Map impEntry = (Map) impIt.next();
          importSet.add( impEntry.get("$key") );
        }
      }

      final String exportsSpec = proj.getProperty(exportsProperty);
      if (!BundleManifestTask.isPropertyValueEmpty(exportsSpec)) {
        final Iterator expIt = Util.parseEntries("export.package",exportsSpec,
                                                 true, true, false );
        while (expIt.hasNext()) {
          final Map expEntry = (Map) expIt.next();
          String pkg = (String) expEntry.get("$key");
          if (!importSet.contains(pkg)) {
            final String ver = (String) expEntry.get("version");
            final String sver = (String) expEntry.get("specification-version");
            if (null!=ver) {
              pkg += ";version="+ver;
            } else if (null!=sver) {
              pkg += ";specification-version="+sver;
            }
            log("implicitImport - adding: "+pkg, Project.MSG_DEBUG);
            importsSpec += "," +pkg;
          }
        }
        log("implicitImport - after: "+importsSpec, Project.MSG_VERBOSE);
        proj.setProperty(importsProperty, importsSpec );
      }
    }
  }

  /**
   * Print check exectuion environment result.
   *
   * @param missing
   *        the set of classes that are used by the bundle but not
   *        within the execution environment.
   * @param profileName
   *        the name of the profile (execution environment) that this
   *        printout if made for.
   */
  private void checkEE(Set missing, String profileName)
  {
    if(missing.size() > 0) {
      System.out.println("Missing " + missing.size() +
                         " classes from "+ profileName +" profile");
      } else {
        System.out.println("Passes " +profileName +" EE");
      }
      for(Iterator it = missing.iterator(); it.hasNext();) {
        final String s = (String)it.next();
        System.out.println("Not in " +profileName +": " + s);
      }
  }


  private boolean doUses()
  {
    return bUses && manifestVersion.compareTo(new Version("1")) > 0;
  }

  /**
   * The sub-set of the provided packages that are exported.
   */
  private Set getProvidedExportSet()
  {
    final Set res = new TreeSet();

    if(!"".equals(exportsProperty)) {
      final String exportsVal = getProject().getProperty(exportsProperty);
      if (!BundleManifestTask.isPropertyValueEmpty(exportsVal)) {

        final Iterator expIt = Util.parseEntries("export.package", exportsVal,
                                                 true, true, false );
        while (expIt.hasNext()) {
          final Map expEntry = (Map) expIt.next();
          final String pkgName = (String) expEntry.get("$key");
          if (providedSet.contains(pkgName)) {
            res.add(pkgName);
          }
        }
      }
    } else {
      res.addAll(providedSet);
    }
    return res;
  }

  /**
   * Return the value of the ExportPackage based on the analyzis.
   */
  protected String buildExportPackagesValue() {
    final String sep = ",";
    final StringBuffer sb = new StringBuffer();
    final boolean addUses = doUses();

    for(Iterator it = providedSet.iterator(); it.hasNext(); ) {
      final String name = (String)it.next();
      sb.append(name);
      if (addUses) {
        final Set usesPkgs = (Set) packageUsingMap.get(name);
        if (0<usesPkgs.size()) {
          sb.append(";uses:=");
          if (1<usesPkgs.size()) {
            sb.append("\"");
          }
          for (Iterator usesIt = usesPkgs.iterator(); usesIt.hasNext(); ) {
            final String usesPkg = (String) usesIt.next();
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
      if(it.hasNext()) {
        sb.append(sep);
      }
    }
    return sb.toString();
  }

  /**
   * Validate a given Export-Package header value. Add uses directives
   * if they are missing.
   *
   * @param oldExportsVal the Export-Package value to validate and update.
   */
  protected String validateExportPackagesValue(final String oldExportsVal)
  {
    final StringBuffer sb = new StringBuffer();
    final String sep = ",";
    final boolean doUses = doUses();
    final Iterator expIt = Util.parseEntries("export.package", oldExportsVal,
                                             true, true, false );
    while (expIt.hasNext()) {
      final Map expEntry = (Map) expIt.next();
      final String pkgName = (String) expEntry.get("$key");
      if (!providedSet.contains(pkgName)) {
        final String msg = "The package '"+pkgName +"' is in the Export-Package"
          +" manifest header, but there is no class belonging to it."
          +" The following packages are provided: "+providedSet;
        log(msg, Project.MSG_ERR );
        if (failOnExports) {
          throw new BuildException(msg, getLocation());
        }
      }
      final Set directives = (Set) expEntry.get("$directives");
      sb.append(pkgName);
      if (doUses) {
        final Set usesPkgs = (Set) packageUsingMap.get(pkgName);
        if (!directives.contains("uses")) {
          // Add a new uses directive.
          if (0<usesPkgs.size()) {
            sb.append(";uses:=");
            if (1<usesPkgs.size()) {
              sb.append("\"");
            }
            for (Iterator usesIt=usesPkgs.iterator(); usesIt.hasNext(); ) {
              final String usesPkg = (String) usesIt.next();
              sb.append(usesPkg);
              if (usesIt.hasNext()) {
                sb.append(sep);
              }
            }
            if (1<usesPkgs.size()) {
              sb.append("\"");
            }
          }
        } else {
          // Validate the given uses directive.
          final String usesValue = (String) expEntry.get("uses");
          final Set uPkgsMan = new TreeSet
            (Arrays.asList(Util.splitwords(usesValue, ", \t", '"')));
          final Set uPkgsMis = new TreeSet(usesPkgs);
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
          final Set uPkgsExtra = new TreeSet(uPkgsMan);
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
      for (Iterator paramIt = expEntry.keySet().iterator(); paramIt.hasNext();){
        final String paramName= (String) paramIt.next();
        if ('$'==paramName.charAt(0)) continue; // Metadata
        sb.append(";");
        sb.append(paramName);
        if (directives.contains(paramName)) sb.append(":");
        sb.append("=");
        final String paramValue = (String) expEntry.get(paramName);
        final boolean quoteNeeded = -1<paramValue.indexOf(',')
          || -1<paramValue.indexOf(';')
          || -1<paramValue.indexOf(':')
          || -1<paramValue.indexOf('=');
        if (quoteNeeded) sb.append("\"");
        sb.append(paramValue);
        if (quoteNeeded) sb.append("\"");
      }
      if(expIt.hasNext()) {
        sb.append(sep);
      }
    }
    return sb.toString();
  }


  /**
   * Analyze a file by checking its suffix and delegate to
   * <tt>analyzeClass</tt>, <tt>analyzeJava</tt> etc
   */
  protected void analyze(File file) throws BuildException {
    if(file.getName().endsWith(".class")) {
      analyzeClass(file);
    } else if(file.getName().endsWith(".jar")) {
      analyzeJar(file);
    } else if(file.getName().endsWith(".java")) {
      analyzeJava(file);
    } else {
      // Just ignore all other files
    }
  }


  protected void addProvidedPackage(String name) {
    if(name == null || "".equals(name)) {
      return;
    }
    log(" Provides package: " + name, Project.MSG_DEBUG);
    providedSet.add(name);
  }


  protected void addActivator(String s) {
    activatorSet.add(s);
  }

  /**
   * Add a type's package name to the list of referenced packages.
   *
   * @param usingPackage The package that the class referencing
   *                     <tt>t</tt> belongs to.
   * @param t Type of an object.
   */
  protected void addReferencedType(String usingPackage, Type t) {
    if(t instanceof BasicType) {
      log("   " +t +" skiped; basic", Project.MSG_DEBUG);
    } else {
      addReferencedClass(usingPackage, t.toString());
    }
  }

  /**
   * Add package names of all types in <code>ts</code> to the list of
   * referenced packages.
   *
   * @param usingPackage The package that the class referencing
   *                  <tt>ts</tt> belongs to.
   * @param ts Array with Type objects.
   */
  protected void addReferencedType(String usingPackage, Type[] ts) {
    for (int i = ts.length-1; i>-1; i-- ) {
      addReferencedType(usingPackage, ts[i]);
    }
  }

  /**
   * Add data for a referenced class.
   *
   * @param usingPackage
   *                  The package that the class referencing
   *                  <tt>className</tt> belongs to.
   * @param className Name of the referenced class.
   */
  protected void addReferencedClass(String usingPackage, String className)
  {
    if(className == null) {
      return;
    }

    final String packageName = packageName(className);
    if("".equals(packageName)) {
      log("   " +className +" skipped; no package name", Project.MSG_DEBUG);
      return;
    }

    classSet.add(className);
    referencedSet.add(packageName);
    if (null!=usingPackage) {
      Set usingSet = (Set) packageUsingMap.get(usingPackage);
      if (null==usingSet) {
        usingSet = new TreeSet();
        packageUsingMap.put(usingPackage, usingSet);
      }
      usingSet.add(packageName);
    }
  }

  protected boolean isImported(String className) {
    for(Iterator it = importSet.iterator(); it.hasNext(); ) {
      final String pkg = (String)it.next();
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
    for(Iterator it = stdImports.iterator(); it.hasNext();) {
      final String s = (String)it.next();
      if(name.startsWith(s)) {
        return true;
      }
    }
    return false;
  }


  protected void analyzeJar(File file) throws BuildException {
    log("Analyze jar file " + file.getAbsolutePath(), Project.MSG_VERBOSE);

    try {
      final JarFile jarFile = new JarFile(file);

      for (Enumeration entries = jarFile.entries();
           entries.hasMoreElements(); ) {
        final ZipEntry     ze = (ZipEntry) entries.nextElement();
        final String fileName = ze.getName();
        if (fileName.endsWith(".class")) {
          log("Analyze jar class file " + fileName, Project.MSG_VERBOSE);
          final InputStream  is = jarFile.getInputStream(ze);
          analyzeClass( new ClassParser(is, fileName ) );
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new BuildException("Failed to analyze class-file " +
                               file + ", exception=" + e, getLocation());
    }
  }

  protected void analyzeClass(File file) throws BuildException {
    log("Analyze class file " + file.getAbsolutePath(), Project.MSG_VERBOSE);

    try {
      analyzeClass( new ClassParser(file.getAbsolutePath()) );
    } catch (Exception e) {
      e.printStackTrace();
      throw new BuildException("Failed to analyze class-file " +
                               file + ", exception=" + e, getLocation());
    }
  }

  protected void analyzeClass(ClassParser parser) throws Exception {
    final JavaClass       clazz            = parser.parse();
    final ConstantPool    constant_pool    = clazz.getConstantPool();
    final ConstantPoolGen constant_poolGen = new ConstantPoolGen(constant_pool);
    final String          clazzPackage     = clazz.getPackageName();

    ownClasses.add(clazz.getClassName());
    addProvidedPackage(clazzPackage);

    // Scan all implemented interfaces to find
    // candidates for the activator AND to find
    // all referenced packages.
    final String[] interfaces = clazz.getInterfaceNames();
    for(int i = 0; i < interfaces.length; i++) {
      if("org.osgi.framework.BundleActivator".equals(interfaces[i])) {
        addActivator(clazz.getClassName());
        break;
      }
    }

    /**
     * Use a descending visitor to find all classes that the given
     * clazz refers to and add them to the set of imported classes.
     */
    DescendingVisitor v = new DescendingVisitor(clazz, new EmptyVisitor() {
        /**
         * Keep track of the signatures visited to avoid processing
         * them more than once. The same signature may apply to
         * many methods or fields.
         */
        boolean[] visitedSignatures = new boolean[constant_pool.getLength()];

        /**
         * Add the class that the given ConstantClass object
         * represents to the set of imported classes.
         *
         * @param obj The ConstantClass object
         */
        public void visitConstantClass( ConstantClass obj ) {
          log(" visit constant class " +obj, Project.MSG_DEBUG);

          String referencedClass = obj.getBytes(constant_pool);
          referencedClass = referencedClass.charAt(0) == '['
            ? Utility.signatureToString(referencedClass, false)
            : Utility.compactClassName(referencedClass,  false);
          addReferencedClass(clazzPackage, referencedClass);
        }

        /**
         * Add the class used as types for the given field.
         * This is necessary since if no method is applied to an
         * object valued field there will be no ConstantClass object
         * in the ConstantPool for the class that is the type of the
         * field.
         *
         * @param obj A Field object
         */
        public void visitField( Field obj ) {
          if (!visitedSignatures[obj.getSignatureIndex()]) {
            log(" visit field " +obj, Project.MSG_DEBUG);

            visitedSignatures[obj.getSignatureIndex()] = true;
            final String signature = obj.getSignature();
            final Type type = Type.getType(signature);
            addReferencedType(clazzPackage, type);
          }
        }

        /**
         * Add all classes used as types for a local variable in the
         * class we are analyzing. This is necessary since if no
         * method is applied to an object valued local variable
         * there will be no ConstantClass object in the ConstantPool
         * for the class that is the type of the local variable.
         *
         * @param obj A LocalVariable object
         */
        public void visitLocalVariable( LocalVariable obj ) {
          if (!visitedSignatures[obj.getSignatureIndex()]) {
            log(" visit local variable " +obj, Project.MSG_DEBUG);

            visitedSignatures[obj.getSignatureIndex()] = true;
            final String signature = obj.getSignature();
            final Type type = Type.getType(signature);
            addReferencedType(clazzPackage, type);
          }
        }

        /**
         * Add all classes mentioned in the signature of the given
         * method. This is necessary since if no method is applied
         * to a parameter (return type) there will be no
         * ConstantClass object in the ConstantPool for the class
         * that is the type of that parameter (return type).
         *
         * @param obj A Method object
         */
        public void visitMethod( Method obj ) {
          if (!visitedSignatures[obj.getSignatureIndex()]) {
            log(" visit method " +obj, Project.MSG_DEBUG);

            visitedSignatures[obj.getSignatureIndex()] = true;
            final String signature = obj.getSignature();
            final Type returnType = Type.getReturnType(signature);
            final Type[] argTypes = Type.getArgumentTypes(signature);
            addReferencedType(clazzPackage, returnType);
            addReferencedType(clazzPackage, argTypes);
          }
        }

        /**
         * Look for packages for types in signatures of methods
         * invoked from code in the class.
         *
         * This typically finds packages from arguments in calls to a
         * superclass method in the current class where no local
         * variable (or field) was used for intermediate storage of
         * the parameter.
         *
         * @param obj The Code object to visit
         */
        public void visitCode( Code obj ) {
          final InstructionList il = new InstructionList(obj.getCode());
          for (InstructionHandle ih=il.getStart(); ih!=null; ih=ih.getNext()) {
            final Instruction inst = ih.getInstruction();
            if (inst instanceof InvokeInstruction) {
              final InvokeInstruction ii = (InvokeInstruction) inst;
              log("   " +ii.toString(constant_pool), Project.MSG_DEBUG);

              // These signatures have no index in the constant pool
              // thus no check/update in visitedSignatures[].
              final String signature = ii.getSignature(constant_poolGen);
              final Type returnType = Type.getReturnType(signature);
              final Type[] argTypes = Type.getArgumentTypes(signature);
              addReferencedType(clazzPackage, returnType);
              addReferencedType(clazzPackage, argTypes);
            }
          }
        }

      } );
    // Run the scanner on the loaded class
    v.visit();
  }

  /**
   * Analyze java source file by reading line by line and looking
   * for keywords such as "import", "package".
   *
   * <p>
   * <b>Note</b>: This code does not attempt to find any class implementing
   * <tt>BundleActivator</tt>
   */
  protected void analyzeJava(File file) throws BuildException {

    if(bDebug) {
      System.out.println("Analyze java file " + file.getAbsolutePath());
    }

    BufferedReader reader = null;

    try {
      String packageName = null;
      String line;
      int    lineNo = 0;

      reader = new BufferedReader(new FileReader(file));

      while(null != (line = reader.readLine())) {
        lineNo++;
        line = line.replace(';', ' ').replace('\t', ' ').trim();

        if(line.startsWith("package")) {
          final Vector v = StringUtils.split(line, ' ');
          if(v.size() > 1 && "package".equals(v.elementAt(0))) {
            packageName = (String)v.elementAt(1);
            addProvidedPackage(packageName);
          }
        }
        if(line.startsWith("import")) {
          final Vector v = StringUtils.split(line, ' ');
          if(v.size() > 1 && "import".equals(v.elementAt(0))) {
            final String name = (String)v.elementAt(1);
            addReferencedClass(packageName, name);
          }
        }
      }
    } catch (Exception e) {
      throw new BuildException("Failed to scan " + file + ", err=" + e,
                               getLocation());
    } finally {
      if(reader != null) {
        try { reader.close(); } catch (Exception ignored) { }
      }
    }
  }

  /**
   * Get package name of class string representation.
   */
  static String packageName(String s) {
    s = s.trim();
    int ix = s.lastIndexOf('.');
    if(ix != -1) {
      s = s.substring(0, ix);
    } else {
      s = "";
    }
    return s;
  }

  /**
   * Convert Set elements to a string.
   *
   * @param separator String to use as sperator between elements.
   */
  static protected String toString(Set set, String separator) {
    final StringBuffer sb = new StringBuffer();

    for(Iterator it = set.iterator(); it.hasNext(); ) {
      final String name = (String)it.next();
      sb.append(name);
      if(it.hasNext()) {
        sb.append(separator);
      }
    }
    return sb.toString();
  }

}
