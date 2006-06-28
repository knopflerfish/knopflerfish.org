/*
 * Copyright (c) 2003, KNOPFLERFISH project
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
import java.util.Hashtable;
import java.util.Enumeration;
import javax.swing.*;
import javax.swing.filechooser.*;

public class FileFilterImpl extends FileFilter implements java.io.FileFilter {

  private static String TYPE_UNKNOWN = "Type Unknown";
  private static String HIDDEN_FILE = "Hidden File";
  
  private Hashtable filters                    = null;
  private String    description                = null;
  private String    fullDescription            = null;
  private boolean   useExtensionsInDescription = true;
  
  public FileFilterImpl() {
    this.filters = new Hashtable();
  }
  
  public boolean accept(File f) {
    if(f != null) {
      if(f.isDirectory()) {
	return true;
      }
      String extension = getExtension(f);
      if(extension != null && filters.get(getExtension(f)) != null) {
	return true;
      };
    }
    return false;
  }
  
  public String getExtension(File f) {
    if(f != null) {
      String filename = f.getName();
      int i = filename.lastIndexOf('.');
      if(i>0 && i<filename.length()-1) {
	return filename.substring(i+1).toLowerCase();
      };
    }
    return null;
  }
  
  public void addExtension(String extension) {
    if(filters == null) {
      filters = new Hashtable(5);
    }
    filters.put(extension.toLowerCase(), this);
    fullDescription = null;
  }
  
  
  public String getDescription() {
    if(fullDescription == null) {
      if(description == null || isExtensionListInDescription()) {
	fullDescription = description==null ? "(" : description + " (";
	// build the description from the extension list
	Enumeration extensions = filters.keys();
	if(extensions != null) {
	  fullDescription += "." + (String) extensions.nextElement();
	  while (extensions.hasMoreElements()) {
	    fullDescription += ", " + (String) extensions.nextElement();
	  }
	}
	fullDescription += ")";
      } else {
	fullDescription = description;
      }
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
