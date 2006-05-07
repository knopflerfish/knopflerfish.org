/*
 * Copyright (c) 2006-2006, KNOPFLERFISH project
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

import java.util.StringTokenizer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Task that creates a pattern suitable for use as the includes
 * attribute in a file set that will find all classes and jars that
 * the framework may use given the specified Bundle-Classpath manifest
 * attribute.
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
 *   <td valign=top>BundleClasspath</td>
 *   <td valign=top>The bundle class path to convert into an includes
 *       pattern.
 *       <p>
 *       If unset, set to empty string, or set to the special empty 
 *       value <code>[bundle.emptystring]</code> the default bundle
 *       classpath, "." will be used.
 *       </p>
 *       <p>
 *       Note: The current value of this property will be overwritten
 *       by the derived pattern.
 *       </p>
 *   </td>
 *   <td valign=top>No.<br> Default value is "."</td>
 *  </tr>
 *  <tr>
 *   <td valign=top>propertyName</td>
 *   <td valign=top>Name of property that will receive the resulting
 *       pattern.
 *   </td>
 *   <td valign=top>Yes.<br> No default value.</td>
 *  </tr>
 * </table>
 *
 * <h3>Examples</h3>
 * The table below shows how different bundle class path entries are
 * translated int patterns.
 * <table border="1">
 *  <th><td>Entry</td><td>Pattern</td></th>
 *  <tr><td>.</td><td>&#x2A;&#x2A;/&#x2A;.class</td></tr>
 *  <tr><td> rxtx</td><td>rxtx/&#x2A;&#x2A;/&#x2A;.class</td></tr>
 *  <tr><td>/rxtx</td><td>rxtx/&#x2A;&#x2A;/&#x2A;.class</td></tr>
 *  <tr><td>required.jar</td><td>required.jar</td></tr>
 *  <tr><td>xx/required.jar</td><td>xx/required.jar</td></tr>
 *  <tr><td>/xx/required.jar</td><td>xx/required.jar</td></tr>
 * </table>
 */
public class BundleClasspathTask extends Task {

  private String bundleClasspath = ".";
  private String propertyName;

  public BundleClasspathTask() {
  }

  /**
   * Set bundle class path to create a pattern for.
   */
  public void setBundleClasspath(String s) {
    this.bundleClasspath  = s;
  }

  /**
   * Set property receiving the bundle class path pattern.
   */
  public void setPropertyName(String s) {
    this.propertyName  = s;
  }

  // Implements Task
  //
  public void execute() throws BuildException {
    if (null==propertyName) {
      throw new BuildException("No property name specified");
    }

    if (null==bundleClasspath || 0==bundleClasspath.length())
      bundleClasspath = ".";

    StringBuffer    sb = new StringBuffer(100);

    // Convert path entries to patterns.
    StringTokenizer st = new StringTokenizer(bundleClasspath, ",");
    while (st.hasMoreTokens()) {
      String entry = st.nextToken().trim();
      if (".".equals(entry)) {
        sb.append("**/*.class");
      } else if (entry.endsWith(".jar")) {
        if (entry.startsWith("/")) entry = entry.substring(1);
        sb.append(entry);
      } else {
        if (entry.startsWith("/")) entry = entry.substring(1);
        sb.append(entry + "/**/*.class");
      }
      if (st.hasMoreTokens())
        sb.append(",");
    }

    // Conversion done - write back properties
    Project proj = getProject();
    proj.setProperty(propertyName, sb.toString());
    log("Converted \"" +bundleClasspath +"\" to pattern \""
        +sb.toString() +"\"",
        Project.MSG_INFO);
  }


}
