/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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

package org.knopflerfish.framework;

import java.io.*;


/**
 * FileTree is extension to java.io.File that handles copying
 * and deletion of complete file structures.
 *
 * @author Jan Stein
 */
public class FileTree extends File
{

  /**
   * Creates a new FileTree instance based on given pathname string.
   */
  public FileTree(String name) {
    super(name);
  }


  /**
   * Creates a new Filetree instance by a pathname string to an existing
   * File or FileTree.
   */
  public FileTree(File file, String name) {
    super(file, name);
  }


  /**
   * Creates a new FileTree instance from a parent pathname string and
   * a child pathname string.
   */
  public FileTree(String n1, String n2) {
    super(n1, n2);
  }


  /**
   * Copy this file tree to specified destination.
   *
   * @param copyFile File object representing the destination.
   * @exception IOException if copy failed. Will leave destination
   *            in an unspecified state.
   */
  public void copyTo(File copyFile) throws IOException
  {
    if (isDirectory()) {
      copyFile.mkdirs();
      String [] dirs = list();
      for (int i = dirs.length - 1; i >= 0; i--) {
	(new FileTree(this, dirs[i])).copyTo(new File(copyFile, dirs[i]));
      }
    } else {
      InputStream is = null; 
      OutputStream os = null;
      try {
	is = new BufferedInputStream(new FileInputStream(this));
	os = new BufferedOutputStream(new FileOutputStream(copyFile));
	byte[] buf=new byte[4096];
	for (;;) {
	  int n=is.read(buf);
	  if (n<0) {
	    break;
	  }
	  os.write(buf, 0, n);
	}
      } finally {
	try {
	  if (is != null) {
	    is.close();
	  }
	} finally {
	  if (os != null) {
	    os.close();
	  }
	}
      }
    }
  }


  /**
   * Delete this file tree from disk.
   *
   * @return True if operation completed okay.
   */
  public boolean delete()
  {
    boolean allDeleted = true;
    if (isDirectory()) {
      String [] dirs = list();
      if(dirs != null) {
	for (int i = dirs.length - 1; i>= 0; i--) {
	  allDeleted &= (new FileTree(this, dirs[i])).delete();
	}
      }
    }
    boolean thisDeleted = super.delete();

    return allDeleted & thisDeleted;
  }
}
