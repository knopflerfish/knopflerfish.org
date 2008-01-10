package org.knopflerfish.ant.taskdefs.bundle;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

public class BIndexTask extends Task {

  private Vector    filesets = new Vector();
  
  private String baseURL             = "";
  private String outFile             = "bindex.xml";
  private String bindexJar           = "bindex.jar";
  
  public void setBaseURL(String s) {
    this.baseURL = s;
  }
  
  public void setOutFile(String s) {
    this.outFile = s;
  }

  public void addFileset(FileSet set) {
    filesets.addElement(set);
  }
  
  public void setBindexJar(String s) {
    bindexJar = s;
  }

  // File -> BundleInfo
  //Set jarMap = new HashSet();
  
  // Implements Task
  public void execute() throws BuildException {
    if (filesets.size() == 0) {
      throw new BuildException("No fileset specified");
    }
    
    Set jarSet = new HashSet();
    
    System.out.println("loading bundle info...");
    
    try {
      for (int i = 0; i < filesets.size(); i++) {
        FileSet          fs      = (FileSet) filesets.elementAt(i);
        DirectoryScanner ds      = fs.getDirectoryScanner(project);
        File             projDir = fs.getDir(project);
  
        String[] srcFiles = ds.getIncludedFiles();
        
        for (int j = 0; j < srcFiles.length ; j++) {
          File file = new File(projDir, srcFiles[j]); 
          if(file.getName().endsWith(".jar")) {
            jarSet.add(file);
          }
        }
      }
      
      Set removeSet = new HashSet();
      for(Iterator it = jarSet.iterator(); it.hasNext();) {
        File   file = (File)it.next();
        String name = file.getAbsolutePath();
        if(-1 != name.indexOf("_all-")) {
          File f2 = new File(Util.replace(name, "_all-", "-"));
          removeSet.add(f2);
          System.out.println("skip " + f2);
        }
      }
      
      if(removeSet.size() > 0) {
        System.out.println("skipping " + removeSet.size() + " bundles");
      }
      
      for(Iterator it = removeSet.iterator(); it.hasNext();) {
        File f = (File)it.next();
        jarSet.remove(f);
      }
      
      System.out.println("writing bundle BR to " + outFile);      
      
      StringBuffer buf = new StringBuffer();
      buf.append("java -jar " + bindexJar + " -r ").append(outFile).append(" ");
      buf.append("-t " + baseURL + "/%p/%f ");
      
      for (Iterator iter = jarSet.iterator(); iter.hasNext(); ) {
        String file = ((File) iter.next()).getAbsolutePath();
        buf.append(file).append(" ");
      }
      
      try {
        Runtime rt = Runtime.getRuntime();
        System.out.println("Running BIndex...");
        rt.exec(buf.toString());
        System.out.println(new File(outFile).getAbsolutePath());
      } catch (Exception e) {
        e.printStackTrace();
      }
    } catch (Exception e) { e.printStackTrace(); }
  }
}
