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
 *    It will use the properties <code>htdocs.link.disabled.class</code>
 *    and <code>htdocs.link.enabled.class</code>. The task will then use the 
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
 * 
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
  private String disable;
  
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
  
    
  public void setDisable(String disabled) {
    this.disable = disabled;
  }
  
  public void addFileset(FileSet fs) {
    filesets.add(fs);
  }
  
  public void execute() {
    if (template == null) throw new BuildException("template must be set");
    if (title == null) title = "";
    if (description == null) description = "";
    
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
      content = Util.replace(content, "$(LINKS)", links());
      content = Util.replace(content, "$(MAIN)", Util.loadFile(fromFile));
      content = Util.replace(content, "$(TITLE)", title);
      content = Util.replace(content, "$(DESC)", description);
      content = Util.replace(content, "$(TSTAMP)", TIMESTAMP);
      content = Util.replace(content, "$(USER)", System.getProperty("user.name"));
      content = Util.replace(content, "$(VERSION)", proj.getProperty("version"));
      content = Util.replace(content, "$(DISTNAME)", proj.getProperty("distname"));
      content = Util.replace(content, "$(MESSAGE)", proj.getProperty("release"));
      content = Util.replace(content, "$(BUNDLE_LIST)", bundleList);
      content = Util.replace(content, "$(ROOT)", pathToRoot);
      content = Util.replace(content, "$(JAVADOC)", proj.getProperty("JAVADOC"));
      content = Util.replace(content, "$(CLASS_NAVIGATION)", proj.getProperty("css_navigation_enabled"));

      String s = proj.getProperty("navigation_pages");
      String navEnabled = proj.getProperty("css_navigation_enabled");
      String navDisabled = proj.getProperty("css_navigation_disabled");
      // System.out.println("Navigation pages: " + s);
      if (s != null) {
	String[] navPages = Util.splitwords(s);
	for (int i = 0; i < navPages.length; i++) {
	  // System.out.println("Checking: " + navPages[i]);
	  if (disable.equals(navPages[i])) {
	    // System.out.println("Disabling: " + "$(CLASS_NAVIGATION_" + navPages[i] + ")");
	    content = Util.replace(content, "$(CLASS_NAVIGATION_" + navPages[i] + ")", navDisabled);
	  }
	  else {
	    // System.out.println("Enabling: " + "$(CLASS_NAVIGATION_" + navPages[i] + ")");
	    content = Util.replace(content, "$(CLASS_NAVIGATION_" + navPages[i] + ")", navEnabled);
	  }
	}
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

  private static final String LINK_BASE = "htdocs.link.";
  private static final String LINK_ID = LINK_BASE + "id.";
  private static final String LINK_TYPE = LINK_BASE + "type.";
  private static final String LINK_NAME = LINK_BASE + "name.";
  private static final String LINK_URL = LINK_BASE + "url.";
  private static final String CSS_CLASS_ENABLED = "htdocs.link.enabled.class";
  private static final String CSS_CLASS_DISABLED = "htdocs.link.disabled.class";

  private String links() {
    Project proj = getProject();
    StringBuffer buf = new StringBuffer();
    
    for (int i = 0; ; i++) {
      String id   = proj.getProperty(LINK_ID + i);

      if (id == null) {
        break;
      }
      
      String type = proj.getProperty(LINK_TYPE + i);
      if (type == null) {
        throw new BuildException("must set htdocs.link.type." + i);
      }

      if (type.equals("separator")) {
        buf.append("<p></p>");
        
      } else if (type.equals("link")) {
        
        String name = proj.getProperty(LINK_NAME + i);
        String url  = proj.getProperty(LINK_URL + i);
        if (name == null) {
          throw new BuildException("Name not set for htdocs.link.url." + i);
        }
        
        String cssClass = null;
        
        if (disable != null && disable.equals(id)) {
          cssClass = getProject().getProperty(CSS_CLASS_DISABLED);
        } else {
          cssClass = getProject().getProperty(CSS_CLASS_ENABLED);
        }
        
        buf.append("<a class=\"" + cssClass + "\" href=\"" + url + "\">" + name + "</a><br/>\n");
      } else {
        throw new BuildException("Do not recognize type " + type);
      }
    }

    return buf.toString();
  }
}
