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

package org.knopflerfish.tools.jarunpacker;


import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.io.*;
import java.lang.reflect.*;

import javax.swing.border.*;
import javax.swing.filechooser.*;

public class JFileReplacePanel extends JPanel {

  public JFileReplacePanel(String origFile,
			   String newFile,
			   Date   newDate,
			   long   newSize) 
  {
    super(new BorderLayout(0, 10));
    
    File f = new File(origFile);
    
    JLabel topL = new JLabel(Strings.fmt("destfolder_contains", f.getName()));
    
    add(topL, BorderLayout.NORTH);
    
    JPanel main = new JPanel(new BorderLayout());
    
    main.setBorder(new EmptyBorder(10, 10, 10, 10));

    J3 j3 = new J3(Strings.get("title_origfile"),
		   getSystemIcon(origFile),
		   Strings.fmt("file_info_fmt", 
			       (new Date(f.lastModified())).toString(),
			       Long.toString(f.length())));
    
    main.add(j3, BorderLayout.NORTH);
    main.add(new JLabel("  "), BorderLayout.CENTER);

    j3 = new J3(Strings.get("title_newfile"),
		getSystemIcon(origFile),
		Strings.fmt("file_info_fmt", 
			    newDate.toString(),
			    Long.toString(newSize)));
    
    main.add(j3, BorderLayout.SOUTH);
    
    add(main, BorderLayout.CENTER);
  }

  /**
   * Try to get an icon for a file
   *
   * @param  iconFile full path to a file
   * @return Icon     icon matching file, or <tt>null</tt> if no icon is found
   */
  Icon getSystemIcon(String iconFile) {
    if("true".equals(System.getProperty("org.knopflerfish.sysicons"))) {
      return getSystemIcon14(iconFile);
    } else {
      return getSystemIconInternal(iconFile);
    }
  }

  String getIconResourceName(String iconFile) {
    String s = iconFile.toLowerCase();
    
    boolean java12 = true;
    try {
      java12 = System.getProperty("java.version").startsWith("1.2");
    } catch (Exception ignored) {
    }

    // Bluerg. java1.2 doesn't read png files with 8-bit alpha
    // so keep gif variants too
    String suffix = java12 ? ".gif" : ".png";

    String name = "";

    if(s.endsWith("build.xml")) {
      name =  "/noia/32x32/mimetypes/xml_ant";
    } else if(s.endsWith(".java")) {
      name =  "/noia/32x32/mimetypes/java_src";
    } else if(s.endsWith(".txt")) {
      name =  "/noia/32x32/mimetypes/mime_txt";
    } else if(s.endsWith(".jar")) {
      name =  "/noia/32x32/mimetypes/misc_doc";
    } else if(s.endsWith(".html")) {
      name =  "/noia/32x32/mimetypes/mime_html";
    } else if(s.endsWith(".gif") ||
	      s.endsWith(".jpg") ||
	      s.endsWith(".png") ||
	      s.endsWith(".psp")) {
      name =  "/noia/32x32/mimetypes/image";
    } else {
      name =  "/noia/32x32/mimetypes/mime_empty";
    }

    return name + suffix;
  }

  Icon getSystemIconInternal(String iconFile) {
    String s = getIconResourceName(iconFile);

    Icon icon = new ImageIcon(getClass().getResource(s));
    
    return icon; 
  }

  /**
   * See if we can get the system icon for a given file.
   */
  Icon getSystemIcon14(String iconFile) {
    Icon icon = null;

    try {
      String version = System.getProperty("java.version");
      if(version.startsWith("1.4") ||
	 version.startsWith("1.5") ||
	 version.startsWith("1.6")) {
	FileSystemView view = FileSystemView.getFileSystemView();
	
	// The hard way to avoid forced compilation with 1.4
	// (getSystemIcon is not present in pre1.4)
	Method m = FileSystemView.class.
	  getMethod("getSystemIcon", new Class[] { File.class  });
	icon = (Icon)m.invoke(view, new Object[] {new File(iconFile) });
      }
    } catch (Throwable ignored) {
    }
    return icon;
  }

}

/**
 * Utility component laid out like this:
 *
 * <pre>
 *  +-----------------+
 *  | title           |
 *  +------+----------+
 *  | icon | txt      |
 *  +------+----------+
 * </pre>
 *
 */
class J3 extends JPanel {
  
  /**
   * @param title  Title string
   * @param icon   Icon. If <tt>null</ll> no icon is shown
   * @param txt    Text string
   */
  J3(String title, Icon icon, String txt) {
    super(new BorderLayout(10, 8));
    add(new JLabel(title), BorderLayout.NORTH);
    
    JPanel main = new JPanel(new BorderLayout());
    main.add(icon != null 
	     ? (new JLabel(icon)) 
	     : (new JLabel()), BorderLayout.WEST);
    main.add(new JLabel(txt),  BorderLayout.CENTER);
    
    add(main, BorderLayout.CENTER);
  }
}

