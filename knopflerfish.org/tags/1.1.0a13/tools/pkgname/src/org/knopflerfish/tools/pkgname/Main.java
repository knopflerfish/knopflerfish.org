
package org.knopflerfish.tools.pkgname;

import java.io.*;
import java.util.*;

/**
 * Misc utilities for changing package names, adding copyright text and 
 * removing comments from java source files.
 */
public class Main {
  public static void main(String[] argv) {

    if(argv.length == 0) {
      printSyntax();
    }

    try {
      for(int i = 0; i < argv.length; i++) {
	if("-renamepkg".equals(argv[i])) {
	  cmdRename(argv[i+1], argv[i+2], argv[i+3]);
	  i += 3;
	} else if("-addcopyright".equals(argv[i])) {
	  cmdCopyright(argv[i+1], argv[i+2]);
	  i += 2;
	} else if("-removecomments".equals(argv[i])) {
	  cmdRemoveComments(new File(argv[i+1]));
	  i += 1;
	} else {
	  printSyntax();
	  System.exit(0);
	}
      }
    } catch (Exception e) {
      printSyntax();
      e.printStackTrace();
      System.exit(0);
    }
  }
  
  static void printSyntax() {
    System.err.println("Usage: [-renamepkg dir oldname newname] [-addcopyright dir prefixfile] [-removecomments dir]");
  }

  static void cmdRemoveComments(File dir) {
    if(dir.isDirectory()) {
      String[] files = dir.list();
      for(int i = 0; i < files.length; i++) {
	File f = new File(dir, files[i]);
	
	if(f.isDirectory()) {
	  cmdRemoveComments(f);
	} else if(files[i].endsWith(".java")) {
	  removeComments(f);
	} else {
	  // skip all other
	}
      }
    }
  }

  static void cmdCopyright(String dirname, String prefixfilename) {
    File dir        = new File(dirname);
    File prefixFile = new File(prefixfilename);

    BufferedReader reader = null;
    StringBuffer   sb     = new StringBuffer();
    String line;
    int    lines = 0;
    System.out.println("cmdCopyright " + dirname + " " + prefixfilename);

    try {
      reader = new BufferedReader(new FileReader(prefixFile));
      while(null != (line = reader.readLine())) {
	sb.append(line);
	sb.append('\n');
	lines++;
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to read " + prefixFile + ", err=" + e);
    } finally {
      if(reader != null) {
	try { reader.close(); } catch (Exception ignored) { }
      }
    }

    addCopyright(dir, sb.toString());
  }
  
  
  static void addCopyright(File dir, String prefix) {
    if(dir.isDirectory()) {
      String[] files = dir.list();
      for(int i = 0; i < files.length; i++) {
	File f = new File(dir, files[i]);
	
	if(f.isDirectory()) {
	  addCopyright(f, prefix);
	} else if(files[i].endsWith(".java")) {
	  
	  prefixText(f, prefix);
	} else {
	  // skip all other
	}
      }
    }
  }

  static void prefixText(File f, String prefix) {
    BufferedReader reader = null;
    Vector         content = new Vector();
    boolean        bFoundPackage = false;


    try {
      String line;
      reader = new BufferedReader(new FileReader(f));
      while(null != (line = reader.readLine())) {
	String l2 = line.replace('\t', ' ').trim();
	if(l2.startsWith("package ")) {
	  bFoundPackage = true;
	}
	if(bFoundPackage) {
	  content.addElement(line);
	}
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to read " + f + ", err=" + e);
    } finally {
      try { reader.close(); } catch (Exception ignored) { }
    }

    if(!bFoundPackage) {
      System.out.println("found no package in " + f.getAbsolutePath());
      return;
    }

    System.out.println("prefix " + f.getAbsolutePath());

    PrintWriter    writer = null;
    
    try {
      writer = new PrintWriter(new FileOutputStream(f));
      writer.print(prefix);
      writer.print('\n');

      for(int i = 0; i < content.size(); i++) {
	String line = (String)content.elementAt(i);
	writer.print(line);
	writer.print('\n');
      }
      writer.flush();
    } catch(Exception e) {
      throw new RuntimeException("Failed to write output file " + f.getAbsolutePath() + ": err=" + e);
    } finally {
      try { writer.close(); } catch (Exception ignored) { }
    }
  }


  static void removeComments(File f) {
    BufferedReader reader = null;
    Vector         content = new Vector();
    int            skipped = 0;

    try {
      String line;
      reader = new BufferedReader(new FileReader(f));
      boolean bComment = false;
      while(null != (line = reader.readLine())) {
	int ix;
	if(-1 != (ix = line.indexOf("/*"))) {
	  int i;
	  for(i = 0; i < ix; i++) {
	    if(!(line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
	      break;
	    }
	  }
	  bComment = i == ix;
	}
	if(!bComment) {
	  content.addElement(line);
	} else {
	  skipped++;
	}
	if(-1 != (ix = line.indexOf("*/"))) {
	  bComment = false;
	}
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to read " + f + ", err=" + e);
    } finally {
      try { reader.close(); } catch (Exception ignored) { }
    }

    if(skipped == 0) {
      System.out.println("found no comments in " + f.getAbsolutePath());
      return;
    } else {
      System.out.println("removed " + skipped + " lines in " + f.getAbsolutePath());
    }

    PrintWriter    writer = null;
    
    try {
      writer = new PrintWriter(new FileOutputStream(f));

      for(int i = 0; i < content.size(); i++) {
	String line = (String)content.elementAt(i);
	writer.print(line);
	writer.print('\n');
      }
      writer.flush();
    } catch(Exception e) {
      throw new RuntimeException("Failed to write output file " + f.getAbsolutePath() + ": err=" + e);
    } finally {
      try { writer.close(); } catch (Exception ignored) { }
    }
  }


  static void cmdRename(String dirname, String oldname, String newname) {
    File dir = new File(dirname);

    Hashtable dirs = new Hashtable();

    rename(dir, oldname, newname, dirs);

    String oldfilepart = replace(oldname, ".", "/");
    String newfilepart = replace(newname, ".", "/");

    for(Enumeration e = dirs.keys(); e.hasMoreElements(); ) {
      String f1 = (String)e.nextElement();

      System.out.println("delete " + f1);

      FileTree ft = new FileTree(f1);
      ft.delete();
    }
  }


  static int rename(File dir, 
		    String oldname, 
		    String newname, 
		    Dictionary dirs) {
    int count = 0;
    if(dir.isDirectory()) {
      String[] files = dir.list();
      for(int i = 0; i < files.length; i++) {
	File f = new File(dir, files[i]);
	
	if(f.isDirectory()) {
	  count += rename(f, oldname, newname, dirs);
	} else if(files[i].endsWith(".java")) {
	  count += replace(f, oldname, newname, dirs);
	} else if(files[i].endsWith("build.xml")) {
	  count += replace(f, oldname, newname, dirs);
	} else {
	  // skip all other
	}
      }
    }
    return count;
  }

  static int replace(File f, String oldname, String newname, Dictionary dirs) {
    String fname = replace(f.getAbsolutePath(), "\\", "/");

    BufferedReader reader = null;

    Vector content = new Vector();

    String oldfilepart = replace(oldname, ".", "/");
    String newfilepart = replace(newname, ".", "/");

    String fname2 = replace(fname, 
			    oldname.replace('.', '/'),
			    newname.replace('.', '/'));

    int replaceCount = 0;

    try {
      String line;
      int    lineNo = 0;
      
      reader = new BufferedReader(new FileReader(f));
      
      while(null != (line = reader.readLine())) {
	lineNo++;
	String l2 = line.replace(';', ' ').replace('\t', ' ').trim();

	if(f.getName().endsWith("build.xml")) {
	  if(-1 != line.indexOf(oldname) ||
	     -1 != line.indexOf(oldfilepart)) {
	    line = replace(line, oldname, newname);
	    line = replace(line, oldfilepart, newfilepart);
	    replaceCount++;
	    
	    System.out.println("build.xml: " + f.getAbsolutePath().replace('\\', '/'));
	  }
	}

	if(-1 != line.indexOf("GATESPACE.ORG")) {
	  line = replace(line, "GATESPACE.ORG", "KNOPFLERFISH");
	  replaceCount++;
	}

	if(-1 != line.indexOf("KNOPFLERFISH.ORG")) {
	  line = replace(line, "KNOPFLERFISH.ORG", "KNOPFLERFISH");
	  replaceCount++;
	}

	if(l2.startsWith("package")) {
	  String[] v = splitwords(l2);
	  if(v.length > 1 && "package".equals(v[0])) {
	    String name = v[1];
	    if(name.startsWith(oldname)) {
	      //	      System.out.println(f.getName() + ": package " + name + ", " + oldname + "->" + newname);
	      
	      line = replace(line, oldname, newname);
	      replaceCount++;

	      int ix = fname.indexOf(oldfilepart);
	      String dirtodelete = fname.substring(0, ix + oldfilepart.length() + 1);
	      dirs.put(dirtodelete, dirtodelete);
	    }
	  }
	}
	if(l2.startsWith("import")) {
	  String[] v = splitwords(l2);
	  if(v.length > 1 && "import".equals(v[0])) {
	    String name = v[1];
	    if(name.startsWith(oldname)) {
	      //	      System.out.println(f.getName() + ": import " + name + ", " + oldname + "->" + newname);
	      line = replace(line, oldname, newname);
	      replaceCount++;
	    }
	  }
	}

	// handle embedded stuff
	if(f.getName().endsWith(".java")) {
	  if(-1 != line.indexOf(oldname)) {
	    line = replace(line, oldname, newname);
	    replaceCount++;
	  }
	}
	  

	content.addElement(line);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to scan " + f + ", err=" + e);
    } finally {
      if(reader != null) {
	try { reader.close(); } catch (Exception ignored) { }
      }
    }

    if(replaceCount > 0) {
      File f2 = new File(fname2);
      File f2dir = f2.getParentFile();
      if(!f2dir.exists()) {
	System.out.println("creating " + f2dir.getAbsolutePath().replace('\\', '/'));
	f2dir.mkdirs();
      }
      
      //      System.out.println("new file " + f2.getAbsolutePath().replace('\\', '/'));
      PrintWriter writer = null;
      try {
	writer = new PrintWriter(new FileOutputStream(f2));
	for(int i = 0; i < content.size(); i++) {
	  String line = (String)content.elementAt(i);
	  writer.print(line);
	  writer.print('\n');
	}
	writer.flush();
      } catch(Exception e) {
	throw new RuntimeException("Failed to write output file " + f2.getAbsolutePath() + ": err=" + e);
      } finally {
	try { writer.close(); } catch (Exception ignored) { }
      }
    }
    return 0;
  }
  
  public static String replace(final String s, final String v1, final String v2) {
    if(s == null 
       || v1 == null 
       || v2 == null 
       || v1.length() == 0 
       || v1.equals(v2)) return s;

    // This code is kind of optimized to use as few as possible
    // String-creating methods as String.substring() and String concatenations.

    // Resulting string will hopefully be somewhere near this size
    StringBuffer  r   = new StringBuffer(s.length() * v2.length() / v1.length());

    int start    = 0;
    int ix       = 0;
    int v1Length = v1.length(); // help sloppy compiler

    while(-1 != (ix = s.indexOf(v1, start))) {
      while(start < ix) r.append(s.charAt(start++)); // avoid substring()
      r.append(v2);
      start += v1Length;
    }
    ix = s.length(); // again, help sloppy compiler
    while(start < ix) r.append(s.charAt(start++));   // avoid substring()

    return r.toString();
  }

  protected static String  WHITESPACE = " \t\n\r";

  protected static char   CITCHAR    = '"';
  
  
  public static String [] splitwords(String s) {
    return splitwords(s, WHITESPACE);
  }
  
  public static String [] splitwords(String s, String whiteSpace) {
    boolean       bCit  = false;        // true when inside citation chars.
    Vector        v     = new Vector(); // (String) individual words after splitting
    StringBuffer  buf   = null; 
    int           i     = 0; 
    
    while(i < s.length()) {
      char c = s.charAt(i);

      if(bCit || whiteSpace.indexOf(c) == -1) {
	// Build up word until we breaks on either a citation char or whitespace
	if(c == CITCHAR) {
	  bCit = !bCit;
	} else {
	  if(buf == null) {
	    buf = new StringBuffer();
	  }
	  buf.append(c);
	}
	i++;
      } else {	
	// found whitespace or end of citation, append word if we have one
	if(buf != null) {
	  v.addElement(buf.toString());
	  buf = null;
	}

	// and skip whitespace so we start clean on a word or citation char
	while((i < s.length()) && (-1 != whiteSpace.indexOf(s.charAt(i)))) {
	  i++;
	}
      }
    }
    // Add possible remaining word
    if(buf != null) {
      v.addElement(buf.toString());
    }
    
    // Copy back into an array
    String [] r = new String[v.size()];
    v.copyInto(r);
    
    return r;
  }
}

class FileTree extends File
{

  public FileTree(String name) {
    super(name);
  }


  public FileTree(File file, String name) {
    super(file, name);
  }


  public FileTree(String n1, String n2) {
    super(n1, n2);
  }


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


  public boolean delete()
  {
    if (isDirectory()) {
      String [] dirs = list();
      if(dirs != null) {
	for (int i = dirs.length - 1; i>= 0; i--) {
	  (new FileTree(this, dirs[i])).delete();
	}
      }
    }
    return super.delete();
  }
}

