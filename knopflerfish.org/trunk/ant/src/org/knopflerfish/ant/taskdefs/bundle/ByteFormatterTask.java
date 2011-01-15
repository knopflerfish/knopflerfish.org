/*
 * Copyright (c) 2008-2010, KNOPFLERFISH project
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
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;


/**
 * Sets a property to a formatted value in ki, Mi, Gi, Ti, Pi, Ei, Zi
 * and Yi with an optional unit.
 *
 * Here <tt>ki</tt> is short for <tt>kibi</tt>, (a contraction of kilo
 * binary) see <a
 * href="http://en.wikipedia.org/wiki/Kibibyte">http://en.wikipedia.org/wiki/Kibibyte</a>
 * for a detailed explanation.  <p>
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
 *    <td valign="top">property</td>
 *    <td valign="top">
 *      The name of the property to assign the formatted value to.</td>
 *    <td valign="top" align="center">Yes.</td>
 *  </tr>
 *  <tr>
 *    <td valign="top">binaryPrefixURL</td>
 *    <td valign="top">An URL pointing to a page explaining the binary
 *                     unit suffixes.</td>
 *    <td valign="top" align="center">
 *      http://en.wikipedia.org/wiki/Binary_prefix#IEC_standard_prefixes
 *    </td>
 *  </tr>
 *  <tr>
 *    <td valign="top">unit</td>
 *    <td valign="top">The unit to append to the formatted value. E.g., byte</td>
 *    <td valign="top" align="center">
 *      No, default is the empty string.</td>
 *  </tr>
 *  <tr>
 *    <td valign="top">sep</td>
 *    <td valign="top">The string placed between the number and the
 *                     unit.</td>
 *    <td valign="top" align="center">
 *      No, default is the HTML non-breaking space, "&amp;nbsp;".</td>
 *  </tr>
 *  <tr>
 *    <td valign="top">value</td>
 *    <td valign="top">The value to format.</td>
 *    <td valign="top" align="center">
 *      One of value and file must be given.</td>
 *  </tr>
 *  <tr>
 *    <td valign="top">file</td>
 *    <td valign="top">The file whose size is the value to format.</td>
 *    <td valign="top" align="center">
 *      One of value and file must be given.</td>
 *  </tr>
 * </table>
 *
 * <h3>Nested elements</h3>
 *
 * Not applicable.
 *
 * <h3>Examples</h3>
 *
 * <h4>Format a value as bytes</h4>
 *
 * <pre>
 *  &lt;byteformatter value="9093663"
 *                    property="myFormatedFilesize"
 *                    unit="B" /&gt;
 * </pre>
 *
 *
 * <h4>Format the size of the file <tt>archive.jar</tt> appending the
 * unit <tt>B</tt></h4>
 *
 * <pre>
 *  &lt;byteformatter file="archive.jar"
 *                    property="archive.size"
 *                    unit="B" /&gt;
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

  private String property;
  /**
   * The name of the property to save the formatted value to.
   *
   * @param property the name of the property to set.
   */
  public void setProperty(String property) {
    this.property = property;
  }


  private String unit = "";
  /**
   * The unit to append to the formatted value.
   *
   * @param unit the unit text to append to the formatted value.
   */
  public void setUnit(String unit) {
    this.unit = unit;
  }


  private String binaryPrefixURL
    = "http://en.wikipedia.org/wiki/Binary_prefix#IEC_standard_prefixes";
  /**
   * The URL that explains binary prefixes.
   *
   * @param url The url to let the binary prefix point to.
   */
  public void setBinaryPrefixURL(String url) {
    this.binaryPrefixURL = url;
  }


  private String sep = "&nbsp;";
  /**
   * The separator between the numeral and the prefixed unit.
   *
   * @param sep the separator string.
   */
  public void setSep(String sep) {
    this.sep = sep;
  }


  private long value;
  /**
   * Set the value to format.
   *
   * @param value the value to format
   */
  public void setValue(long value) {
    this.value = value;
  }


  /**
   * Set the file to get the size of as the the value to format.
   *
   * @param file the file to return a formatted file size for.
   */
  public void setFile(final File file) {
    this.value = file.length();
  }



  static final long step = 1024;

  /**
   * Performs the requested formatting.
   *
   * @throws BuildException if the manifest cannot be written.
   */
  public void execute() {
    String formatedValue = "";
    String[] suffixes = new String[]{ "",  "Ki","Mi",
                                      "Gi","Ti","Pi",
                                      "Ei","Zi","Yi"};
    if (binaryPrefixURL!=null && binaryPrefixURL.length()>0) {
      for (int i=0; i<suffixes.length; i++) {
        if (suffixes[i].length()>0) {
          suffixes[i] = "<a href=\"" +binaryPrefixURL +"\">"
            +suffixes[i] + "</a>";
        }
      }
    }

    int ix = 0;
    long factor = 1;

    while(value/factor>step && ix<suffixes.length) {
      factor *= step;
      ix++;
    }

    long i = value/factor;    // Integral part of reduced value
    long r = value -i*factor; // Remainder.
    // Convert r to a decimal fraction
    double fraction  = ((double) r)/((double) factor);
    formatedValue = formatValue(i, fraction);

    Project project = getProject();
    project.setProperty(property, formatedValue +sep +suffixes[ix] +unit);
  }

  private String formatValue( long integral, double fraction)
  {
    String res = String.valueOf(integral);
    if (integral<10) { // Append two digits from the fraction
      int dec = (int) (fraction*100);
      res = res +"." +(dec<10?"0":"") +String.valueOf(dec);
    } else if (integral<100) { // Append one digit from the fraction
      int dec = (int) (fraction*10);
      res = res +"." +String.valueOf(dec);
    }
    return res;
  }
}
