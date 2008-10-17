package org.knopflerfish.ant.taskdefs.bundle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * <p>
 *  This task is currently used in conjunction with MakeHTMLTask, which
 *  is used to create the htdocs directory in the Knopflerfish 
 *  distribtions.
 * </p>
 * 
 * <p>
 *  The task is used to create extra documentation of bundles. The task
 *  is given a set of files it searches them for documentation, and
 *  generates a index-file that links to these sites.
 * </p>
 * 
 * <table border=1>
 *  <tr>
 *   <td valign=top><b>Attribute</b></td>
 *   <td valign=top><b>Description</b></td>
 *   <td valign=top><b>Required</b></td>
 *  </tr>
 *  <tr>
 *   <td>
 *    outdir
 *   </td>
 *   <td>
 *    Directory where the task copies the directories
 *   </td>
 *   <td>
 *    Yes
 *   </td>
 *  </tr>
 * </table>
 * 
 * <p>
 *  The task includes the directories that contains an <code>index.html</code>,
 *  others are not included.
 * </p>
 * 
 * <p>
 *  If a directory contains an <code>index.html</code> and <code>doc.properties</code>
 *  the task will read the <code>doc.properties</code> file. 
 *  The properties used are <code>Title</code> and <code>Description</code>. These 
 *  properties will be used when creating the main index site. 
 * </p>
 * 
 * <p>
 *  If the directory does not contain any <code>doc.properties</code> the task
 *  will attempt to create a valid link name.
 * </p>
 * 
 */

public class ExtraBundleDocTask extends Task {
  
  private static final String TITLE = "Title";
  private static final String BODY  = "Description";
  private static final String INDEX_FILE = "index.html";
  private static final String PROPS_FILE = "doc.properties";

  
  private ArrayList filesets = new ArrayList();
  private File outputDir;
  private String template;
  private String pattern;
  
  public void setOut(String s) {
    outputDir = new File(s);  
  }

  public void setTemplate(String template) {
    this.template = template;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }
  
  public void addFileset(FileSet set) {
    filesets.add(set);
  }
  
  public void execute() {
    StringBuffer buf = new StringBuffer();
    for (Iterator iter=filesets.iterator(); iter.hasNext(); ) {
      FileSet set = (FileSet) iter.next();
      File dir = set.getDir(getProject());
      DirectoryScanner ds = set.getDirectoryScanner(getProject());
      String[] dirs = ds.getIncludedDirectories();
      
      // copy the files
      for (int i = 0; i < dirs.length; i++) {
        try {
          File sourceDir = new File(dir, dirs[i]);
          if (!new File(sourceDir, INDEX_FILE).isFile()) {
            System.out.println(sourceDir + " does not contain any index.html. Skipping.");
            continue;
          }
          
          copyDirectory(sourceDir, new File(outputDir, dirs[i]));

        } catch (IOException e) {
          e.printStackTrace();
          throw new BuildException(e);
        }
      }
      
      // create the index-file
  
      
      for (int i = 0; i < dirs.length; i++) {
        File sourceDir = new File(dir, dirs[i]);
        if (!new File(sourceDir, INDEX_FILE).isFile()) {
          System.out.println(sourceDir + " does not contain any " + INDEX_FILE + ". Skipping.");
          continue;
        }
        
        String p = pattern;
        Properties props = new Properties();
        
        props.put(TITLE, sourceDir.getParentFile().getName());
        props.put(BODY, "");

        File propsFile = new File(sourceDir, PROPS_FILE);
        if (propsFile.isFile() && propsFile.canRead()) {
          try {
            props.load(new FileInputStream(propsFile));
          } catch (IOException e) {
            throw new BuildException(e);
          }
        }
        
        p = Util.replace(p, "$(URL)", new File(dirs[i], INDEX_FILE).toString());
        p = Util.replace(p, "$(TITLE)", props.getProperty(TITLE));
        p = Util.replace(p, "$(BODY)", props.getProperty(BODY));
        buf.append(p);
        buf.append("\n");
      }
    }
      
    try {
      Util.writeStringToFile(new File(outputDir, INDEX_FILE), 
                             Util.replace(Util.loadFile(template), 
                                          "$(EXTRA_DOC_BODY)", 
                                          buf.toString()));
    } catch (IOException e) {
      throw new BuildException(e);
    }
  }
  
  private void copyDirectory(File sourceLocation, File targetLocation) throws IOException {
	  
    if (sourceLocation.isHidden()) {
     return ; 
    }
    
	  if (sourceLocation.isDirectory()) {
	    if (!targetLocation.exists()) {
	      if (!targetLocation.mkdirs()) {
         throw new IOException("Could not create directory " + targetLocation); 
        }
	    }

	    String[] children = sourceLocation.list();
	    for (int i=0; i<children.length; i++) {
	      copyDirectory(new File(sourceLocation, children[i]), new File(targetLocation, children[i]));
	    }
      
	  } else {
	    InputStream in = new FileInputStream(sourceLocation);
	    OutputStream out = new FileOutputStream(targetLocation);

	    byte[] buf = new byte[1024];
	    int len;
	    while ((len = in.read(buf)) > 0) {
	      out.write(buf, 0, len);
	    }
	    in.close();
	    out.close();
	  }
	}
}
