package org.knopflerfish.ant.taskdefs.bundle;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;


/**
 * <p>
 *  This task is used when building distributions of Knopflerfish. 
 *  If you don't intend to create a new distribution type of 
 *  Knopflerfish then you're in the wrong place.
 * </p>
 * 
 * <p>
 *  Task that creates web sites given a template and a source file. 
 *  Currently used to create the htdocs directory in the KF dist.
 *  It does this by simply replacing certain text strings with 
 *  others. For more information on which text strings this is
 *  please check the source code.
 * </p>
 * 
 * <p>
 *  Here is a outline of how to use the task and a description
 *  of different parameters and used system properties.
 * </p>
 * 
 * <p>
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
 *    What dir to put the actual the generated file
 *   </td>
 *   <td>
 *    Yes
 *   </td>
 *  </tr>
 *   <td> 
 *    tofile
 *   </td>
 *   <td>
 *    The releative path to where the generated file should be
 *    copied. That is the actual location of the generated file
 *    will be <code>outdir</code>/<code>tofile</code>
 *   </td>
 *   <td>
 *    Yes
 *   </td>
 *  </tr>
 *  <tr>
 *   <td>
 *    template
 *   </td>
 *   <td>
 *    The file which describes what the page should look like
 *   </td>
 *   <td>
 *    Yes
 *   </td>
 *  </tr>
 *  <tr>
 *   <td>
 *    title
 *   </td>
 *   <td>
 *    The page's title
 *   </td>
 *   <td>
 *    No, default is ""
 *   </td>
 *  </tr>
 *  <tr>
 *   <td>
 *    description
 *   </td>
 *   <td>
 *    The page's description
 *   </td>
 *   <td>
 *    No, default is ""
 *   </td>
 *  </tr>
 *  <tr>
 *   <td>
 *    disable
 *   </td>
 *   <td>
 *    Allows you to disable certain links. This attribute is very ad hoc.
 *    It will use the properties <code>link.disabled.class</code>
 *    and <code>link.enabled.class</code>. The task will then use the 
 *    values of these properties to generate the file.
 *   </td>
 *   <td>
 *    No
 *   </td>
 *  </tr>
 * </table>
 * <p>
 *  <b>Note:</b> instead of using the attributes <code>fromfile</code> and 
 *  <code>tofile</code> one can use filesets. It will simply run through
 *  and perform the task on all given files.
 * </p>
 */

public class MakeHTMLTask extends Task {
  
  private final static String TIMESTAMP = new SimpleDateFormat("EE MMMM d yyyy, HH:mm:ss", Locale.ENGLISH).format(new Date());
  
  /**
   * The source file
   */
  private File fromFile;
  
  /**
   * Target directory, where everything will end up
   */
  private File outdir;
  
  /**
   * The relative path to the target file from output dir
   */
  private File toFile;
  
  /**
   * File's title 
   */
  private String title;
  
  /**
   * Description
   */
  private String description;
  
  /**
   * Template file
   */
  private File template;
  
  /**
   * Bundle list
   */
  private String bundleList;
  
  private ArrayList filesets = new ArrayList();
  private HashMap links = new HashMap();
  
  public void setFromfile(String s) {
    fromFile = new File(s);
  }
  
  public void setTofile(String s) {
    toFile = new File(s);
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setDescription(String description) {
    this.description = description;
  }
  
  public void setOutdir(String s) {
    outdir = new File(s);
  }
  
  public void setTemplate(String template) {
    this.template = new File(template);
  }

  public void setBundleList(String bundleList) {
    this.bundleList = bundleList;
  }
  
  
  private void initLinks() {
    String enabled = getProject().getProperty("link.enabled.class");
    links.put("$(CLASS_NAVIGATION)", enabled);
    links.put("$(CLASS_NAVIGATION_INDEX)", enabled);
    links.put("$(CLASS_NAVIGATION_COMP)", enabled);
    links.put("$(CLASS_NAVIGATION_CHANGELOG)", enabled);
    links.put("$(CLASS_NAVIGATION_CONTACTS)", enabled);
    links.put("$(CLASS_NAVIGATION_DOWNLOAD)", enabled);
    links.put("$(CLASS_NAVIGATION_PROG)", enabled);
    links.put("$(CLASS_NAVIGATION_DESKTOP)", enabled);
    links.put("$(CLASS_NAVIGATION_LINKS)", enabled);
    links.put("$(CLASS_NAVIGATION_LICENSE)", enabled);
    links.put("$(CLASS_NAVIGATION_SVN)", enabled);
    links.put("$(CLASS_NAVIGATION_BUNDLES)", enabled);
    links.put("$(CLASS_NAVIGATION_PREF)", enabled);
  }
  
  public void setDisable(String disabled) {
    initLinks();
    String[] tags = Util.splitwords(disabled);
    for (int i = 0; i < tags.length; i++) {
      links.put("$(" + tags[i] + ")", getProject().getProperty("link.disabled.class"));      
    }
  }
  
  public void addFileset(FileSet fs) {
    filesets.add(fs);
  }
  
  public void execute() {
    if (template == null) throw new BuildException("template must be set");
    if (title == null) title = "";
    if (description == null) description = "";
    
    if (links.isEmpty()) {
      initLinks();
    }
    
    if (filesets.isEmpty() && fromFile == null && toFile == null) {
      throw new BuildException("Need to specify tofile and fromfile or give a fileset");
    }

    if (filesets.isEmpty()) {
      transform(fromFile.toString(), toFile.toString());
    } else {
      if (fromFile != null) throw new BuildException("Can not specify fromfile when using filesets");
      if (toFile != null) throw new BuildException("Can not specify tofile when using filesets");
      
      for (Iterator iter = filesets.iterator(); iter.hasNext(); ) {
        FileSet fs = (FileSet) iter.next();
        DirectoryScanner ds = fs.getDirectoryScanner(getProject());
        String[] files = ds.getIncludedFiles();
        
        for (int i = 0; i < files.length; i++) {
          transform(new File(fs.getDir(getProject()), files[i]).getAbsolutePath(), files[i]);
        }
      }
    }
  }
  
  private void transform(String fromFile, String toFile) {
    if (fromFile == null) throw new BuildException("fromfile must be set");
    if (toFile == null) throw new BuildException("tofile must be set"); 
    
    try { 
      Project proj = getProject();
      File tmp = new File(outdir, toFile).getParentFile();

      if (!tmp.exists()) {
        if (tmp.exists() || !tmp.mkdirs()) {
          throw new IOException("Could not create " + tmp);
        }
      }
      
      tmp = new File(toFile);
      String pathToRoot = ".";

      while ((tmp = tmp.getParentFile()) != null) {
        pathToRoot = pathToRoot + "/..";
      }
      
      String content = Util.loadFile(template.toString());
      content = Util.replace(content, "$(MAIN)", Util.loadFile(fromFile));
      content = Util.replace(content, "$(TITLE)", title);
      content = Util.replace(content, "$(DESC)", description);
      content = Util.replace(content, "$(TSTAMP)", TIMESTAMP);
      content = Util.replace(content, "$(USER)", System.getProperty("user.name"));
      content = Util.replace(content, "$(VERSION)", proj.getProperty("version"));
      content = Util.replace(content, "$(MESSAGE)", proj.getProperty("release"));
      content = Util.replace(content, "$(BUNDLE_LIST)", bundleList);
      content = Util.replace(content, "$(ROOT)", pathToRoot);
      content = Util.replace(content, "$(JAVADOC)", proj.getProperty("JAVADOC"));
      content = Util.replace(content, "$(CLASS_NAVIGATION)", proj.getProperty("link.enabled.class"));
      
      // fix the links
      for (Iterator iter = links.entrySet().iterator(); iter.hasNext(); ) {
        Entry link = (Entry) iter.next();
        content = Util.replace(content, (String) link.getKey(), (String) link.getValue());
      }
    
      Util.writeStringToFile(new File(outdir, toFile), content);
      System.out.println("wrote " + new File(outdir, toFile));
    } catch (IOException e) {
      e.printStackTrace();
      throw new BuildException(e);
    } catch (Exception e) {
      e.printStackTrace();
      throw new BuildException(e);
    }
    
  }
  
}
