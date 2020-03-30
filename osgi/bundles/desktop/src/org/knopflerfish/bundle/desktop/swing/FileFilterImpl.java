/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
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

package org.knopflerfish.bundle.desktop.swing;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.filechooser.FileFilter;

public class FileFilterImpl extends FileFilter implements java.io.FileFilter {

  private Map<String, FileFilterImpl> filters
    = new HashMap<String, FileFilterImpl>();
  private String    description                = null;
  private String    fullDescription            = null;
  private boolean   useExtensionsInDescription = true;

  public FileFilterImpl() {
  }

  public boolean accept(File f)
  {
    if (f != null) {
      if (f.isDirectory()) {
        return true;
      }
      final String extension = getExtension(f);
      if (extension != null && filters.get(extension) != null) {
        return true;
      }
    }
    return false;
  }

  public String getExtension(File f)
  {
    if (f != null) {
      final String filename = f.getName();
      int i = filename.lastIndexOf('.');
      if (i > 0) {
        return filename.substring(i + 1).toLowerCase();
      }
    }
    return null;
  }

  public void addExtension(String extension) {
    filters.put(extension.toLowerCase(), this);
    fullDescription = null;
  }


  public String getDescription()
  {
    if (fullDescription == null) {
      final StringBuilder sb = new StringBuilder();
      if (description == null || isExtensionListInDescription()) {
        if (description != null) {
          sb.append(description).append(" ");
        }
        sb.append("(");
        // build the description from the extension list
        for (String extension : filters.keySet()) {
          if (sb.charAt(sb.length()-1) != '(') {
            sb.append(", ");
          }
          sb.append(".").append(extension);
        }
        sb.append(")");
      } else {
        sb.append(description);
      }
      fullDescription = sb.toString();
    }
    return fullDescription;
  }

  public void setDescription(String description) {
    this.description = description;
    fullDescription = null;
  }

  public void setExtensionListInDescription(boolean b) {
    useExtensionsInDescription = b;
    fullDescription = null;
  }

  public boolean isExtensionListInDescription() {
    return useExtensionsInDescription;
  }
}
