/*
 * Copyright (c) 2003-2008, KNOPFLERFISH project
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
import java.util.Enumeration;
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
 * <table border=>
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
 *       Comma-separated list of all prefixes to standard packages that
 *       should be ignored in exports list.
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
 *   &lt;fileset dir="classes" includes="test/impl/&#042;"/&gt;
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
 *   &lt;fileset dir="classes" includes="test/&#042;"/&gt;
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

  private Set     stdImports       = new TreeSet();
  private boolean bDebug           = false;

  private boolean bPrintClasses      = false;

  private boolean bCheckFoundationEE = false;
  private boolean bCheckMinimumEE    = false;
  private boolean bCheckSMFEE        = false;
  private boolean bImplicitImports   = true;
  private boolean bSetActivator      = true;

  /** The set of packages that are provided by the inlcuded classes. */
  private Set providedSet          = new TreeSet();
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

  private Set classSet             = new TreeSet();
  private Set ownClasses           = new TreeSet();

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
    stdImports.addAll(StringUtils.split(packageList.trim(),','));
  }

  public void addFileset(FileSet set) {
    filesets.addElement(set);
  }

  // Implements Task
  //
  // Scan all files in fileset and delegate to analyze()
  // then write back values to properties.
  public void execute() throws BuildException {
    if (filesets.size() == 0) {
      throw new BuildException("No fileset specified");
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
    }

    // Scan done - write back properties

    Project proj = getProject();

    if(!"".equals(exportsProperty)) {
      String exportsVal = proj.getProperty(exportsProperty);
      if (BundleManifestTask.isPropertyValueEmpty(exportsVal)) {
        if (0==providedSet.size()) {
          proj.setProperty(exportsProperty,
                           BundleManifestTask.BUNDLE_EMPTY_STRING);
          log("No packages exported, leaving \"" +exportsProperty +"\" empty.",
              Project.MSG_VERBOSE);
        } else {
          exportsVal = toString(providedSet, ",");
          log("Setting \"" +exportsProperty +"\" to \""+exportsVal +"\"",
              Project.MSG_VERBOSE);
          proj.setProperty(exportsProperty, exportsVal);
        }
      } else {
        // Export-Package given; check that they are provided.
        Iterator expIt = Util.parseEntries("export.package",exportsVal,
                                           true, true, false );
        while (expIt.hasNext()) {
          Map expEntry = (Map) expIt.next();
          String exPkg = (String) expEntry.get("key");
          if (!providedSet.contains(exPkg)) {
            log("The package '"+exPkg +"' is in the Export-Package"
                +" manifest header, but there is no class belonging to it."
                +" The following packages are provided: "+providedSet,
                Project.MSG_ERR );
          }
        }
      }
    }

    importSet.addAll(extraImportSet);
    importSet.removeAll(providedSet);

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
        // present and that there are no dupplicated packages.
        TreeSet givenImportSet = new TreeSet();
        Iterator impIt = Util.parseEntries("import.package",importsVal,
                                           true, true, false );
        while (impIt.hasNext()) {
          Map impEntry = (Map) impIt.next();
          String pkgName = (String) impEntry.get("key");
          if (!givenImportSet.add( pkgName )) {
            log("The package '" +pkgName +"' is mentioned twice in the"
                +" given 'Import-Package' manifest header: '"
                +importsVal +"'.",
                Project.MSG_ERR);
            throw new BuildException("The package '" +pkgName
                                     +"' is imported twice.");
          }
        }
        givenImportSet.removeAll(providedSet);
        TreeSet missingImports = new TreeSet(importSet);
        missingImports.removeAll(givenImportSet);
        if (0<missingImports.size()) {
          log("External packages: "+importSet, Project.MSG_ERR);
          log("Imported packages: "+givenImportSet, Project.MSG_ERR);
          log("Provided packages: "+providedSet, Project.MSG_ERR);

          log("The following external packages are used by the bundle "
             +"but not mentioned in the Import-Package manifest header: "
             +missingImports, Project.MSG_ERR );
          /* Should make this configurable.
          throw new BuildException
            ("The following packages are used by the bundle but note "
             +"mentioned in the Import-Package manifest header: "
             +missingImports,
             getLocation() );
          */
        }
        TreeSet extraImports = new TreeSet(givenImportSet);
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
      String activatorVal = proj.getProperty(activatorProperty);
      if (BundleManifestTask.isPropertyValueEmpty(activatorVal)) {
        // No Bundle-Activator given; use derived value if possible.
        switch(activatorSet.size()) {
        case 0:
          log("No class implementing BundleActivator found", Project.MSG_ERR);
          throw new BuildException
            ("Requested to derive Bundle-Activator but there is "
             +"no class implementing BundleActivator.",
             getLocation() );
        case 1:
          String clazz = (String)activatorSet.iterator().next();
          proj.setProperty(activatorProperty, clazz);
          break;
        default:
          log("Manual selectio if Bundle-Activator is needed since"
              +" the set of included classes contains more than one"
              +" candidate: "+activatorSet,
              Project.MSG_ERR);
          throw new BuildException
            ("Requested to derive Bundle-Activator but there are "
             +"more than one class implementing BundleActivator.",
             getLocation() );
        }
      } else {
        // Bundle-Activator given; check that it is correct.
        if (0==activatorSet.size()) {
          log("No class implementing BundleActivator found", Project.MSG_ERR);
        } else {
          String givenClazz = proj.getProperty(activatorProperty).trim();
          if (!activatorSet.contains(givenClazz)) {
            log("The specified BundleActivator '" +givenClazz
                +"' is not a member of the set of included classes that"
                +"  implements BundleActivator: " +activatorSet,
                Project.MSG_WARN);
            throw new BuildException
              ("The specified BundleActivator '" +givenClazz
                +"' is not a member of the set of included classes that"
                +"  implements BundleActivator: " +activatorSet,
               getLocation() );
          }
        }
      }
    }

    Set foundationMissing = new TreeSet();
    Set minimumMissing    = new TreeSet();
    Set smfMissing        = new TreeSet();


    for(Iterator it = classSet.iterator(); it.hasNext();) {
      String s = (String)it.next();
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
      if(foundationMissing.size() > 0) {
        System.out.println("Missing " + foundationMissing.size() +
                           " classes from foundation profile");
      } else {
        System.out.println("Passes foundation EE");
      }
      for(Iterator it = foundationMissing.iterator(); it.hasNext();) {
        String s = (String)it.next();
        System.out.println("Not in foundation: " + s);
      }
    }

    if(bCheckMinimumEE) {
      if(minimumMissing.size() > 0) {
        System.out.println("Missing " + minimumMissing.size() +
                           " classes from minimum profile");
      } else {
        System.out.println("Passes minimum EE");
      }
      for(Iterator it = minimumMissing.iterator(); it.hasNext();) {
        String s = (String)it.next();
        System.out.println("Not in minimum: " + s);
      }
    }

    if(bCheckSMFEE) {
      if(smfMissing.size() > 0) {
        System.out.println("Missing " + smfMissing.size() +
                           " classes from SMF profile");
      } else {
        System.out.println("Passes SMF EE");
      }
      for(Iterator it = smfMissing.iterator(); it.hasNext();) {
        String s = (String)it.next();
        System.out.println("Not in SMF: " + s);
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
        Iterator impIt = Util.parseEntries("import.package",importsSpec,
                                           true, true, false );
        while (impIt.hasNext()) {
          Map impEntry = (Map) impIt.next();
          importSet.add( impEntry.get("key") );
        }
      }

      String exportsSpec = proj.getProperty(exportsProperty);
      if (!BundleManifestTask.isPropertyValueEmpty(exportsSpec)) {
        Iterator expIt = Util.parseEntries("export.package",exportsSpec,
                                           true, true, false );
        while (expIt.hasNext()) {
          Map expEntry = (Map) expIt.next();
          String pkg = (String) expEntry.get("key");
          if (!importSet.contains(pkg)) {
            String ver = (String) expEntry.get("version");
            String sver = (String) expEntry.get("specification-version");
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


  protected void addProvidedPackageString(String name) {
    if(name == null || "".equals(name)) {
      return;
    }
    log(" Provides package: " + name, Project.MSG_DEBUG);
    providedSet.add(name);
  }


  protected void addActivatorString(String s) {
    activatorSet.add(s);
  }

  /**
   * Add a type's package name to the list of imported packages.
   *
   * @param t Type of an object.
   */
  protected void addImportedType(Type t) {
    if(t instanceof BasicType) {
      log("   " +t +" skiped; basic", Project.MSG_DEBUG);
    } else {
      addImportedString(t.toString());
    }
  }

  /**
   * Add package names of all types in <code>ts</code> to the list of
   * imported packages.
   *
   * @param ts Array with Type objects.
   */
  protected void addImportedType(Type[] ts) {
    for (int i = ts.length-1; i>-1; i-- ) {
      addImportedType(ts[i]);
    }
  }

  /**
   * Add a class' package name to the list of imported packages.
   *
   * @param className Class name of an object. The class name is stripped
   *                  from the part after the last '.' and added to set
   *                  of imported packages, if its not one of the standard
   *                  packages. Primitive class names are ignore.
   */
  protected void addImportedString(String className) {

    if(className == null) {
      return;
    }

    String name = packageName(className);

    if("".equals(name)) {
      log("   " +className +" skipped; no package name", Project.MSG_DEBUG);
      return;
    }

    // only add packages defined outside this set of files
    if(!providedSet.contains(name)) {
      // ...and only add non-std packages
      if(!isStdImport(name)) {
        log("   " +name +" added ; unprovided non-standard package.",
            Project.MSG_DEBUG);
        importSet.add(name);
      } else {
        log("   " +name +" skiped; standard import.", Project.MSG_DEBUG);
      }
    } else {
      log("   " +name +" skiped; package provided.", Project.MSG_DEBUG);
    }
    classSet.add(className);
  }

  boolean isImported(String className) {
    for(Iterator it = importSet.iterator(); it.hasNext(); ) {
      String pkg = (String)it.next();
      if(className.startsWith(pkg)) {
        String rest = className.substring(pkg.length() + 1);
        if(-1 == rest.indexOf(".")) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Check if package is included in prefix list of standard packages.
   *
   * @return <tt>true</tt> if name is prefixed with any of the elements in
   *         <tt>stdImports</tt> set, <tt>false</tt> otherwise.
   */
  protected boolean isStdImport(String name) {
    for(Iterator it = stdImports.iterator(); it.hasNext();) {
      String s = (String)it.next();
      if(name.startsWith(s)) {
        return true;
      }
    }
    return false;
  }


  protected void analyzeJar(File file) throws BuildException {
    log("Analyze jar file " + file.getAbsolutePath(), Project.MSG_VERBOSE);

    try {
      JarFile jarFile = new JarFile(file);

      for (Enumeration entries = jarFile.entries();
           entries.hasMoreElements(); ) {
        ZipEntry     ze = (ZipEntry) entries.nextElement();
        String fileName = ze.getName();
        if (fileName.endsWith(".class")) {
          log("Analyze jar class file " + fileName, Project.MSG_VERBOSE);
          InputStream  is = jarFile.getInputStream(ze);
          analyzeClass( new ClassParser(is, fileName ) );
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new BuildException("Failed to analyze class-file " +
                               file + ", exception=" + e);
    }
  }

  protected void analyzeClass(File file) throws BuildException {
    log("Analyze class file " + file.getAbsolutePath(), Project.MSG_VERBOSE);

    try {
      analyzeClass( new ClassParser(file.getAbsolutePath()) );
    } catch (Exception e) {
      e.printStackTrace();
      throw new BuildException("Failed to analyze class-file " +
                               file + ", exception=" + e);
    }
  }

  protected void analyzeClass(ClassParser parser) throws Exception {
    final JavaClass       clazz            = parser.parse();
    final ConstantPool    constant_pool    = clazz.getConstantPool();
    final ConstantPoolGen constant_poolGen = new ConstantPoolGen(constant_pool);

    ownClasses.add(clazz.getClassName());
    addProvidedPackageString(clazz.getPackageName());

    // Scan all implemented interfaces to find
    // candidates for the activator AND to find
    // all referenced packages.
    String[] interfaces = clazz.getInterfaceNames();
    for(int i = 0; i < interfaces.length; i++) {
      if("org.osgi.framework.BundleActivator".equals(interfaces[i])) {
        addActivatorString(clazz.getClassName());
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
          addImportedString(referencedClass);
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
            String signature = obj.getSignature();
            Type type = Type.getType(signature);
            addImportedType(type);
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
            String signature = obj.getSignature();
            Type type = Type.getType(signature);
            addImportedType(type);
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
            String signature = obj.getSignature();
            Type returnType = Type.getReturnType(signature);
            Type[] argTypes = Type.getArgumentTypes(signature);
            addImportedType(returnType);
            addImportedType(argTypes);
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
          InstructionList il = new InstructionList(obj.getCode());
          for (InstructionHandle ih=il.getStart(); ih!=null; ih=ih.getNext()) {
            Instruction inst = ih.getInstruction();
            if (inst instanceof InvokeInstruction) {
              InvokeInstruction ii = (InvokeInstruction) inst;
              log("   " +ii.toString(constant_pool), Project.MSG_DEBUG);

              // These signatures have no index in the constant pool
              // thus no check/update in visitedSignatures[].
              String signature = ii.getSignature(constant_poolGen);
              Type returnType = Type.getReturnType(signature);
              Type[] argTypes = Type.getArgumentTypes(signature);
              addImportedType(returnType);
              addImportedType(argTypes);
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
      String line;
      int    lineNo = 0;

      reader = new BufferedReader(new FileReader(file));

      while(null != (line = reader.readLine())) {
        lineNo++;
        line = line.replace(';', ' ').replace('\t', ' ').trim();

        if(line.startsWith("package")) {
          Vector v = StringUtils.split(line, ' ');
          if(v.size() > 1 && "package".equals(v.elementAt(0))) {
            String name = (String)v.elementAt(1);
            addProvidedPackageString(name);
          }
        }
        if(line.startsWith("import")) {
          Vector v = StringUtils.split(line, ' ');
          if(v.size() > 1 && "import".equals(v.elementAt(0))) {
            String name = (String)v.elementAt(1);
            addImportedString(name);
          }
        }
      }
    } catch (Exception e) {
      throw new BuildException("Failed to scan " + file + ", err=" + e);
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
    StringBuffer sb = new StringBuffer();

    for(Iterator it = set.iterator(); it.hasNext(); ) {
      String name = (String)it.next();
      sb.append(name);
      if(it.hasNext()) {
        sb.append(separator);
      }
    }
    return sb.toString();
  }

}
