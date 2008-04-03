package org.knopflerfish.ant.taskdefs.bundle;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

public class BIndexTask extends Task {

  private Vector    filesets = new Vector();

  private File   baseDir             = null;
  private String baseURL             = "";
  private String outFile             = "bindex.xml";
  private String bindexJar           = "bindex.jar";

  /**
   * The working directory of the BIndex process. The bundle URLs will
   * be on the form
   * <tt><i>BaseURL</i>/<i>&lt;relative path to JAR&gt;</i></tt>
   * where the relative path to JAR is computed relative to this base
   * dir.
   * @param f The base dir for relative part of the generated URLs.
   */
  public void setBaseDir(File f) {
    this.baseDir = f;
  }

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

      List cmdList = new ArrayList( 10 + jarSet.size() );

      cmdList.add("java");
      cmdList.add("-jar");
      cmdList.add(bindexJar);
      cmdList.add("-r");
      cmdList.add(outFile);
      cmdList.add("-t");
      cmdList.add(baseURL +"/%p/%f ");

      for (Iterator iter = jarSet.iterator(); iter.hasNext(); ) {
        String file = ((File) iter.next()).getAbsolutePath();
        cmdList.add(file);
      }

      try {
        Runtime rt = Runtime.getRuntime();
        System.out.println("Running BIndex...");
        rt.exec((String[]) cmdList.toArray(new String[cmdList.size()]),
                null,
                baseDir);
        System.out.println(new File(outFile).getAbsolutePath());
      } catch (Exception e) {
        e.printStackTrace();
      }
    } catch (Exception e) { e.printStackTrace(); }
  }
}
