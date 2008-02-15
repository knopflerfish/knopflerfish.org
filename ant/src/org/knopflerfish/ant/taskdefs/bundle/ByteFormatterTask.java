/*
 * Copyright (c) 2008-2008, KNOPFLERFISH project
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
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Manifest;
import org.apache.tools.ant.taskdefs.ManifestException;
import org.apache.tools.ant.types.EnumeratedAttribute;


/**
 * Set a property to to a formated file size in k (kilo byte), M (Mega
 * byte) etc.
 * <p>
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
 *    <td valign="top">value</td>
 *    <td valign="top">The value to format.</td>
 *    <td valign="top" align="center">
 *      Yes.</td>
 *  </tr>
 *  <tr>
 *    <td valign="top">property</td>
 *    <td valign="top">
 *      The name of the property to assign the foramted value to.</td>
 *    <td valign="top" align="center">Yes.</td>
 *  </tr>
 * </table>
 *
 * <h3>Nested elements</h3>
 *
 * Not applicable.
 *
 * <h3>Examples</h3>
 *
 * <h4>Format a file size</h4>
 *
 * <pre>
 *  &lt;byteformatter value="9093663"
 *                    property="myFormatedFilesize"/&gt;
 * </pre>
 *
 *
 *
 */
public class ByteFormatterTask extends Task {
  /**
   * Default constructor.
   */
  public ByteFormatterTask() {
    super();
   }

  private long value;
  /**
   * Set the value to format.
   *
   * @param the value to format
   */
  public void setValue(long value) {
    this.value = value;
  }

  private String propertyName;
  /**
   * The name of the property to save the foramted value to.
   *
   * @param f the propety name.
   */
  public void setPropertyName(String propertyName) {
    this.propertyName = propertyName;
  }


  /**
   * Create or update the Manifest when used as a task.
   *
   * @throws BuildException if the manifest cannot be written.
   */
  public void execute() {
    final int factor = 1024;
    long k = value / factor;
    long m = k / factor;
    long g = m / factor;
    long t = g / factor;
    
    Project   project      = getProject();
    if (t>0) {
      project.setProperty(propertyName, String.valueOf(t)+" TB");
    } else if (g>0) {
      project.setProperty(propertyName, String.valueOf(g)+" GB");
    } else if (m>0) {
      project.setProperty(propertyName, String.valueOf(m)+" MB");
    } else if (k>0) {
      project.setProperty(propertyName, String.valueOf(k)+" kB");
    } else {
      project.setProperty(propertyName, String.valueOf(value)+" B");
    }
  }

}
