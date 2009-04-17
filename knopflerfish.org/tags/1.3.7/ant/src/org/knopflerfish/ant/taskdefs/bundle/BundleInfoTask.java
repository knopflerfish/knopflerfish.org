/*
 * Copyright (c) 2003, KNOPFLERFISH project
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
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Utility;
import org.apache.bcel.generic.BasicType;
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
 *       Comma-separated list of all default imported packages. 
 *       <p>
 *       <b>Note</b>: Do not set <tt>defaultimports</tt> to the empty 
 *       string, since that might 
 *       cause an later illegal bundle manifest file if <i>no</i> imported 
 *       packages are found.
 *       </p>
 *   </td>
 *   <td valign=top>
 *     No.<br> 
 *     Default value is "org.osgi.framework"
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
 *  &lt;bundleinfo  activator = "bundle.activator" 
 *               imports   = "impl.import.package"&gt;
 *   &lt;fileset dir="classes" includes="test/impl/&#042;"/&gt;
 *  &lt;/bundleinfo&gt;
 *  &lt;echo message="imports   = ${impl.import.package}"/&gt;
 *  &lt;echo message="activator = ${bundle.activator}"/&gt;
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
  
  private Set     stdImports       = new TreeSet();
  private boolean bDebug           = false;

  private boolean bPrintClasses      = false;

  private boolean bCheckFoundationEE = false;
  private boolean bCheckMinimumEE    = false;
  private boolean bCheckSMFEE        = false;

  private Set importSet            = new TreeSet();
  private Set exportSet            = new TreeSet();
  private Set activatorSet         = new TreeSet();

  private Set classSet             = new TreeSet();
  private Set ownClasses           = new TreeSet();
  
  public BundleInfoTask() {
    fileUtils = FileUtils.newFileUtils();
    setDefaultImports("org.osgi.framework");
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

  /**
   * Set default import set.
   *
   * @param packageList Comma-separated list of package names.
   */
  public void setDefaultImports(String packageList) {
    Vector v = StringUtils.split(packageList.trim(),',');  
    importSet.clear();
    importSet.addAll(v);
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

    importSet.removeAll(exportSet);

    if(!"".equals(importsProperty)) {
      proj.setNewProperty(importsProperty,   toString(importSet, ","));
    }

    if(!"".equals(exportsProperty)) {
      proj.setNewProperty(exportsProperty,  toString(exportSet, ","));
    }

    // Try to be a bit clever when writing back bundle activator
    if(!"".equals(activatorProperty)) {
      switch(activatorSet.size()) {
      case 0:
	System.out.println("info - no class implementing " + 
			   "BundleActivator found");
	break;
      case 1:
	{
	  String clazz = (String)activatorSet.iterator().next();
	  String clazz0 = proj.getProperty(activatorProperty);
	  if(clazz0 == null || "".equals(clazz0)) {
	    // System.out.println("set activator " + activatorProperty + "=" + clazz);
	  } else {
	    if(!clazz.equals(clazz0)) {
	      System.out.println("*** Warning - the class found implementing " + 
				 " BundleActivator '" + clazz + "' " + 
				 " does not match the one set as " + 
				 activatorProperty + "=" + clazz0);
	    } else {
	      if(bDebug) {
		System.out.println("correct activator " + 
				   activatorProperty + "=" + clazz0);
	      }
	    }
	  }
	  proj.setNewProperty(activatorProperty, clazz);
	}
	break;
      default:
	System.out.println("*** Warning - more than one class " + 
			   "implementing BundleActivator found:");
	for(Iterator it = activatorSet.iterator(); it.hasNext();) {
	  System.out.println(" " + it.next());
	}
	break;
	
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
  }

  


  /**
   * Analyze a file by checking its suffix and delegate to 
   * <tt>analyzeClass</tt>, <tt>analyzeJava</tt> etc
   */
  protected void analyze(File file) throws BuildException {
    if(file.getName().endsWith(".class")) {
      analyzeClass(file);
    } else if(file.getName().endsWith(".java")) {
      analyzeJava(file);
    } else {
      // Just ignore all other files
    }
  }


  protected void addExportedPackageString(String name) {
    if(name == null || "".equals(name)) {
      return;
    }

    if(bDebug) {
      System.out.println(" package " + name);
    }
    exportSet.add(name);
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
      // Ignore all basic types
    } else {
      addImportedString(t.toString());
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
      return;
    }

    // only add packages defined outside this set of files
    if(!exportSet.contains(name)) {

      // ...and only add non-std packages
      if(!isStdImport(name)) {
	importSet.add(name);
      }
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
    throw new BuildException("Jar file analyzing not yet supported");
  }

  protected void analyzeClass(File file) throws BuildException {
    if(bDebug) {
      System.out.println("Analyze class file " + file.getAbsolutePath());
    }

    try {
      ClassParser        parser   = new ClassParser(file.getAbsolutePath());
      JavaClass          clazz    = parser.parse();
      ConstantPool  constant_pool = clazz.getConstantPool();

      ownClasses.add(clazz.getClassName());
      addExportedPackageString(clazz.getPackageName());

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

      Constant[] constants = constant_pool.getConstantPool();
      for (int i = 0; i < constants.length; i++) {
        Constant constant = constants[i];
        if (constant instanceof ConstantClass) {
          ConstantClass constantClass = (ConstantClass) constant;
          String referencedClass = constantClass.getBytes(constant_pool);
          if (referencedClass.charAt(0) == '[') {
            referencedClass = Utility.signatureToString(referencedClass, false);
          } else {
            referencedClass = Utility.compactClassName(referencedClass, false);
          }
          addImportedString(referencedClass);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new BuildException("Failed to parse .class file " + 
			       file + ", exception=" + e);
    }
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
	    addExportedPackageString(name);
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
