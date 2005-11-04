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
import java.awt.event.*;
import java.io.File;
import java.io.IOException;


public class JFile extends JPanel {
  JFileChooser fc = null;
  JTextField   text;
  JButton      browse;

  boolean bIsDir;

  public JFile(String path, boolean bDir) {
    super(new BorderLayout());
    this.bIsDir = bDir;

    text   = new JTextField(path);
    text.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  textChanged();
	}
      });
    
    browse = new JButton(Strings.get("browse"));

    if(bIsDir) {
      browse.setToolTipText(Strings.get("tt_browse_dir"));
    } else {
      browse.setToolTipText(Strings.get("tt_browse_file"));
    }
    browse.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  if(fc == null) {
	    fc = new JFileChooser();
	    fc.setFileSelectionMode(bIsDir 
				    ? JFileChooser.DIRECTORIES_ONLY
				    : JFileChooser.FILES_ONLY);
	    fc.setMultiSelectionEnabled(false); 
	    fc.setApproveButtonText(Strings.get("select"));
	    File file = getFile();
	    try { // Ensure that new files are place in CWD by default.
	      file = file.getCanonicalFile();
	    } catch (IOException ioe) {
	    }
	    fc.setSelectedFile(file);
	    if(bIsDir) {
	      fc.setDialogTitle(Strings.get("title_select_dir"));
	    } else {
	      fc.setDialogTitle(Strings.get("title_select_file"));
	    }
	  }
	
	  int returnVal = fc.showOpenDialog(browse);
	  if(returnVal == JFileChooser.APPROVE_OPTION) {
	    String s = fc.getSelectedFile().getAbsolutePath();
	    setText(s);
	  }
	}
      });
    add(browse, BorderLayout.EAST);
    add(text, BorderLayout.CENTER);
  }

  public void textChanged() {
  }

  public void setText(String s) {
    text.setText(s);
    textChanged();
  }

  public String getText() {
    return text.getText();
  }

  public File getFile() {
    return new File(getText());
  }
}
