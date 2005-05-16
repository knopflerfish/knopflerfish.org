/*
 * Copyright (c) 2004, Richard S. Hall
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *   * Neither the name of the ungoverned.org nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Original source: Richard S. Hall (heavy@ungoverned.org)
 * KF Console API adaption: Erik Wistrand (wistrand@knopflerfish.org)
 *
 */

package org.knopflerfish.osgi.bundle.bundlerepository;

import java.io.*;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.*;
import org.ungoverned.osgi.service.bundlerepository.BundleRecord;
import org.ungoverned.osgi.service.bundlerepository.BundleRepositoryService;

import org.ungoverned.osgi.bundle.bundlerepository.Util;
import org.ungoverned.osgi.bundle.bundlerepository.FileUtil;

import org.knopflerfish.service.console.*;


/**
 * Export the following OBR commands as a KF console command group "obr"
 * 
 * <pre>
 *  urls [<repository-file-url> ...]
 *  list [<string> ...]
 *  info <bundle-name>[;<version>] ...
 *  deploy [-nodeps] <bundle-name>[;<version>] ... | <bundle-id> ...
 *  install [-nodeps] <bundle-name>[;<version>] ...
 *  start [-nodeps] <bundle-name>[;<version>] ...
 *  update -check
 *  update [-nodeps] <bundle-name>[;<version>] ... | <bundle-id> ...
 *  source [-x] <local-dir> <bundle-name>[;<version>] ...
 * </pre>
 */
public class ObrCommandGroup extends CommandGroupAdapter
{
  private   BundleContext           bc    = null;
  private   BundleRepositoryService brs   = null;
  protected ServiceRegistration     reg   = null;

  public ObrCommandGroup(BundleContext bc) {
    super("obr", "OBR commands");
    this.bc  = bc;

    ServiceReference sr = bc.getServiceReference(BundleRepositoryService.class.getName());
    
    this.brs = (BundleRepositoryService)bc.getService(sr);

    if(this.brs == null) {
      throw new RuntimeException("BundleRepositoryService must be available");
    }
  }
  
  public void register() {
    if(reg == null) {
      Hashtable props = new Hashtable();
      props.put("groupName", getGroupName());
      reg = bc.registerService(CommandGroup.class.getName(),
			       this,
			       props);
    }
  }

  void unregister() {
    if(reg != null) {
      reg.unregister();
      reg = null;
    }
  }

  public final static String USAGE_URLS = "[<url>] ...";
  public final static String [] HELP_URLS = new String [] {
    "List or set repository URLs",
    "<url> repository URL" };
  
  public int cmdUrls(Dictionary opts, Reader in, PrintWriter out, Session session) {
    String[] urls = (String[])opts.get("url");
    
    if(urls != null && urls.length > 0) {
      brs.setRepositoryURLs(urls);
    } else {
      urls = brs.getRepositoryURLs();
      if (urls != null)	{
	for (int i = 0; i < urls.length; i++)	  {
	  out.println(urls[i]);
	}
      } else {
	out.println("No repository URLs are set.");
      }
    }
    return 0;
  }

  public final static String USAGE_LIST = "[-l] [<name>]";
  public final static String [] HELP_LIST = new String [] {
    "List contents of repository",
    "-l   -  long format",
    "name -  name (or substring) for bundles to list",
    "        If no name is given, list all bundles.",
  };

  public int cmdList(Dictionary opts, Reader in, PrintWriter out, Session session) {
    String substr = (String)opts.get("name");
    boolean bLong = null != opts.get("-l");

    int nCount = 0;
    int count = brs.getBundleRecordCount();
    if(count == 0) {
      out.println("No bundles in repositories");
    } else {
      if(bLong) {
	out.println("No   Name                Update-location");
      } else {
	out.println("No   Name");
      }
      for (int i = 0; i < brs.getBundleRecordCount(); i++) {
	BundleRecord record = brs.getBundleRecord(i);
	String name = (String) record.getAttribute(BundleRecord.BUNDLE_NAME);
	if (name != null) {
	  if ((substr == null) ||
	      (name.toLowerCase().indexOf(substr) >= 0))  {
	    nCount++;
	    String version =
	      (String) record.getAttribute(BundleRecord.BUNDLE_VERSION);
	    boolean bCit = true; // name.indexOf(" ") != -1;
	    
	    
	    StringBuffer sb = new StringBuffer();
	    sb.append(" ");
	    sb.append(Integer.toString(i + 1));
	    pad(sb, 5);
	    if(bCit) {
	      sb.append("\"");
	    }
	    if (version != null) {
	      sb.append(name + ";" + version);
	    } else {
	      sb.append(name);
	    }
	    if(bCit) {
	      sb.append("\"");
	    }
	    if(bLong) {
	      sb.append(" ");
	      pad(sb, 25);
	      sb.append(record.getAttribute(BundleRecord.BUNDLE_UPDATELOCATION));
	    }
	    out.println(sb.toString());
	  }
	}
      }
    }
    
    if (nCount == 0)      {
      out.println("No matching bundles.");
    }
    return 0;
  }

  static StringBuffer pad(StringBuffer sb, int len) {
    while(sb.length() < len) {
      sb.append(" ");
    }
    return sb;
  }
  
  public final static String USAGE_INFO = "<name;version> ...";
  public final static String [] HELP_INFO = new String [] {
    "Show bundle info",
    "name - name (or substring) for bundles to list",
    "       if name is an integer, use bundle with",
    "       this index (from obr list)"};

  public int cmdInfo(Dictionary opts, Reader in, PrintWriter outPW, Session session) {
    String[] infos = (String[])opts.get("name;version");

    ParsedCommand pc = parseInfo(infos);
    for (int i = 0; (pc != null) && (i < pc.getTargetCount()); i++) { 
      BundleRecord record = null;
      
      // If there is no version, then try to retrieve by
      // name, but error if there are multiple versions.
      if (pc.getTargetVersion(i) == null) {
	BundleRecord[] records =
	  brs.getBundleRecords(pc.getTargetName(i));
	
	if (records.length == 1) {
	  record = records[0];
	} else {
	  
	}
      } else {
	record = 
	  brs.getBundleRecord(pc.getTargetName(i),
				Util.parseVersionString(
							pc.getTargetVersion(i)));
      }
      
      if (record != null) {
	PrintStream outStream = new PrintWriterStream(outPW);
	
	record.printAttributes(outStream);
      } else {
	outPW.println("Unknown bundle or ambiguous version: "
		      + pc.getTargetName(i));

	for (int j = 0; j < brs.getBundleRecordCount(); j++) {
	  BundleRecord r2 = brs.getBundleRecord(j);
	  String name    = (String) r2.getAttribute(BundleRecord.BUNDLE_NAME);
	  String version = (String) r2.getAttribute(BundleRecord.BUNDLE_VERSION);
	  if(name.equals(pc.getTargetName(i))) {
	    outPW.println(" \"" + name + ";" + version + "\"");
	  }
	}
      }
      outPW.println("");
    }
    return 0;
  }
  

  public final static String USAGE_DEPLOY = "[-nodeps] <name;version> ...";
  public final static String [] HELP_DEPLOY = new String [] {
    "Deploy bundle(s)",
    "name;version - name (and optional version)"
  };

  public int cmdDeploy(Dictionary opts, Reader in, PrintWriter outPW, Session session) {
    String[] infos = (String[])opts.get("name;version");

    ParsedCommand pc = parseInfo(infos);
    boolean bResolve = (null == opts.get("-nodeps"));
    
    for (int i = 0; (pc != null) && (i < pc.getTargetCount()); i++) {
      // Find either the local bundle or the bundle
      // record so we can get the update location attribute.
      String updateLocation = null;
      
      // First look for update location locally.            
      Bundle bundle =
	findLocalBundle(pc.getTargetName(i), pc.getTargetVersion(i));
      if (bundle != null) {
	updateLocation = (String)
	  bundle.getHeaders().get(Constants.BUNDLE_UPDATELOCATION);
      }
      
      // If update location wasn't found locally, look in repository.            
      if (updateLocation == null) {
	BundleRecord record =
	  findBundleRecord(pc.getTargetName(i), pc.getTargetVersion(i));
	if (record != null) {
	  updateLocation = (String)
	    record.getAttribute(BundleRecord.BUNDLE_UPDATELOCATION);
	}
      }
      
      if (updateLocation != null) {
	PrintStream outStream = new PrintWriterStream(outPW);
	
	brs.deployBundle(
			   outStream, // Output stream.
			   outStream, // Error stream.
			   updateLocation, // Update location.
			   bResolve, // Resolve dependencies.
			   false); // Start.
      } else {
	outPW.println("Unknown bundle or ambiguous version: "
		      + pc.getTargetName(i));
	return 1;
      }
    }
    return 0;
  }
  
  public final static String USAGE_INSTALL = "[-nodeps] <name;version> ...";
  public final static String [] HELP_INSTALL = new String [] {
    "Install bundle",
    "name;version - name and optional version.",
    "               If name starts with '=', use number from obr list",
};
  
  public int cmdInstall(Dictionary opts, Reader in, PrintWriter out, Session session) {
    return doInstallOrStart(opts, in, out, session, false);
  }
  
  public final static String USAGE_START = "[-nodeps] <name;version> ...";
  public final static String [] HELP_START = new String [] {
    "Install and start bundle",
    "name;version - name and optional version.",
    "               If name starts with =, use number from obr list",
};
  
  public int cmdStart(Dictionary opts, Reader in, PrintWriter out, Session session) {
    return doInstallOrStart(opts, in, out, session, true);
  }
  
  public int doInstallOrStart(Dictionary opts, Reader in, PrintWriter outPW, Session session, boolean bStart) {
    String[] infos = (String[])opts.get("name;version");

    ParsedCommand pc = parseInfo(infos);
    boolean bResolve = (null == opts.get("-nodeps"));
    

    // Loop through each local target and try to find
    // the corresponding bundle record from the repository.
    for (int targetIdx = 0;
	 (pc != null) && (targetIdx < pc.getTargetCount());
	 targetIdx++)                
      {
	// Get the current target's name and version.
	String targetName = pc.getTargetName(targetIdx);
	String targetVersionString = pc.getTargetVersion(targetIdx);
	
	// Make sure the bundle is not already installed.
	Bundle bundle = findLocalBundle(targetName, targetVersionString);
	if (bundle == null) {
	  // Find the targets bundle record.
	  BundleRecord record = findBundleRecord(targetName, targetVersionString);
	  
	  // If we found a record, try to install it.
	  if (record != null) {
	    PrintStream outStream = new PrintWriterStream(outPW);
	    
	    brs.deployBundle(
			       outStream, // Output stream.
			       outStream, // Error stream.
			       (String) record.getAttribute(BundleRecord.BUNDLE_UPDATELOCATION), // Update location.
			       bResolve, // Resolve dependencies.
			       bStart);
	  } else {
	    outPW.println("Not in repository: " + targetName);
	    return 1;
	  }
	} else  {
	  outPW.println("Already installed: " + targetName);
	}
      }
    return 0;
  }
  
  public final static String USAGE_UPDATE = "[-nodeps] [-check] <name;version> ...";
  public final static String [] HELP_UPDATE = new String [] {
    "Update bundle",
    "name;version - name and optional version" };
  
  public int cmdUpdate(Dictionary opts, Reader in, PrintWriter outPW, Session session) throws IOException {
    String[] infos    = (String[])opts.get("name;version");
    boolean  bCheck   = (null == opts.get("-check"));
    boolean  bResolve = (null == opts.get("-nodeps"));
    
    ParsedCommand pc  = parseInfo(infos);
    PrintStream   out = new PrintWriterStream(outPW);
    
    if (bCheck) {
      updateCheck(out, out);
    } else {
      // Loop through each local target and try to find
      // the corresponding locally installed bundle.
      for (int targetIdx = 0;
	   (pc != null) && (targetIdx < pc.getTargetCount());
	   targetIdx++)                
	{
	  // Get the current target's name and version.
	  String targetName = pc.getTargetName(targetIdx);
	  String targetVersionString = pc.getTargetVersion(targetIdx);
	  
	  // Find corresponding locally installed bundle.
	  Bundle bundle = findLocalBundle(targetName, targetVersionString);
	  
	  // If we found a locally installed bundle, then
	  // try to update it.
	  if (bundle != null) {
	    brs.deployBundle(
			       out, // Output stream.
			       out, // Error stream.
			       (String) bundle.getHeaders().get(Constants.BUNDLE_UPDATELOCATION), // Local bundle to update.
			       bResolve, // Resolve dependencies.
			       false); // Start.
	  } else {
	    outPW.println("Not installed: " + targetName);
	  }
	}
    }
    return 0;
  }
  
  private void updateCheck(PrintStream out, PrintStream err)
    throws IOException
  {
    Bundle[] bundles = bc.getBundles();
    
    // Loop through each local target and try to find
    // the corresponding locally installed bundle.
    for (int bundleIdx = 0;
	 (bundles != null) && (bundleIdx < bundles.length);
	 bundleIdx++)
      {
	// Ignore the system bundle.
	if (bundles[bundleIdx].getBundleId() == 0) {
	  continue;
	}
	
	// Get the local bundle's update location.
	String localLoc = (String)
	  bundles[bundleIdx].getHeaders().get(Constants.BUNDLE_UPDATELOCATION);
	if (localLoc == null) {
	  // Without an update location, there is no way to
	  // check for an update, so ignore the bundle.
	  continue;
	}
	
	// Get the local bundle's version.
	String localVersion = (String)
	  bundles[bundleIdx].getHeaders().get(Constants.BUNDLE_VERSION);
	localVersion = (localVersion == null) ? "0.0.0" : localVersion;
	
	// Get the matching repository bundle records.
	BundleRecord[] records = brs.getBundleRecords(
							(String) bundles[bundleIdx].getHeaders().get(Constants.BUNDLE_NAME));
	
	// Loop through all records to see if there is an update.
	for (int recordIdx = 0;
	     (records != null) && (recordIdx < records.length);
	     recordIdx++)
	  {
	    String remoteLoc = (String)
	      records[recordIdx].getAttribute(BundleRecord.BUNDLE_UPDATELOCATION);
	    if (remoteLoc == null)  {
	      continue;
	    }
	    
	    // If the update locations are equal, then compare versions.
	    if (remoteLoc.equals(localLoc)) {
	      String remoteVersion = (String)
		records[recordIdx].getAttribute(BundleRecord.BUNDLE_VERSION);
	      if (remoteVersion != null) {
		int result = 
		  Util.compareVersion(Util.parseVersionString(remoteVersion),
				      Util.parseVersionString(localVersion));
		if (result > 0) {
		  out.println(
			      records[recordIdx].getAttribute(BundleRecord.BUNDLE_NAME)
			      + " update available.");
		  break;
		}
	      }
	    }
	  }
      }
  }
  
  /*
  public final static String USAGE_SOURCE = "[-x] <localDir> <name;version> ...";
  public final static String [] HELP_SOURCE = new String [] {
    "Get source for a bundle",
    "name;version - name and optional version" };
  
  public int cmdSource(Dictionary opts, Reader in, PrintWriter outPW, Session session) {
    String[] infos   = (String[])opts.get("name;version");
    String localDir  = (String)opts.get("localDir");
    boolean bExtract = opts.get("-x") != null;
    
    if(localDir == null) {
      localDir = ".";
    }
    
    ParsedCommand pc = parseInfo(infos);

    PrintStream out = new PrintWriterStream(outPW);
    
    for (int i = 0; i < pc.getTargetCount(); i++)
      {
	BundleRecord record = findBundleRecord(
					       pc.getTargetName(i), pc.getTargetVersion(i));
	if (record != null) {
	  String srcURL = (String)
	    record.getAttribute(BundleRecord.BUNDLE_SOURCEURL);
	  if (srcURL != null) {
	    FileUtil.downloadSource(
				    out, 
				    out, 
				    srcURL, 
				    localDir, 
				    bExtract);
	  } else {
	    outPW.println("Missing source URL: " + pc.getTargetName(i));
	    return 1;
	  }
	} else {
	  outPW.println("Not in repository: " + pc.getTargetName(i));
	  return 1;
	}
      }
    return 0;
  }
  */
  
  private BundleRecord findBundleRecord(String name, String versionString)
  {
    BundleRecord record = null;

    if(name.startsWith("=")) {
      int id = -1;
      try {
	id = Integer.parseInt(name.substring(1));
	record = brs.getBundleRecord(id - 1);
	if(record != null) {
	  System.out.println(id + " " + record.getAttribute(BundleRecord.BUNDLE_UPDATELOCATION));
	  return record;
	}
      } catch (Exception ignored) {
      }
    }

    // If there is no version, then try to retrieve by
    // name, but error if there are multiple versions.
    if (versionString == null)
      {
	BundleRecord[] records =
	  brs.getBundleRecords(name);
	if (records.length == 1)
	  {
	    record = records[0];
	  }
      }
    else
      {
	record = brs.getBundleRecord(
				     name, Util.parseVersionString(versionString));
      }
    
    return record;
  }
  
  
  private Bundle findLocalBundle(String name, String versionString)
  {
    Bundle bundle = null;
    
    // Get the name only if there is no version, but error
    // if there are multiple matches for the same name.
    if (versionString == null)
      {
	// Perhaps the target name is a bundle ID and
	// not a name, so try to interpret as a long.
	try
	  {
	    bundle = bc.getBundle(Long.parseLong(name));
	  }
	catch (NumberFormatException ex)
	  {
	    // The bundle is not a number, so look for a local
	    // bundle with the same name.
	    Bundle[] matchingBundles = findLocalBundlesByName(name);
	    
	    // If only one matches, then select is.
	    if (matchingBundles.length == 1)
                {
		  bundle = matchingBundles[0];
                }
	  }
      }
    else
      {
	// Find the local bundle by name and version.
	bundle = findLocalBundleByVersion(
					  name, Util.parseVersionString(versionString));
      }
    
    return bundle;
  }

  private Bundle findLocalBundleByVersion(String name, int[] version)
  {
    // Get bundles with matching name.
    Bundle[] targets = findLocalBundlesByName(name);
    
    // Find bundle with matching version.
    if (targets.length > 0)
      {
	for (int i = 0; i < targets.length; i++)
	  {
	    String targetName = (String)
	      targets[i].getHeaders().get(BundleRecord.BUNDLE_NAME);
	    int[] targetVersion = Util.parseVersionString((String)
							  targets[i].getHeaders().get(BundleRecord.BUNDLE_VERSION));
            
	    if ((targetName != null) &&
		targetName.equalsIgnoreCase(name) &&
		(Util.compareVersion(targetVersion, version) == 0))
	      {
		return targets[i];
	      }
	  }
      }
    
    return null;
  }
  
  private Bundle[] findLocalBundlesByName(String name)
  {
    // Get local bundles.
    Bundle[] bundles = bc.getBundles();
    
    // Find bundles with matching name.
    Bundle[] targets = new Bundle[0];
    for (int i = 0; i < bundles.length; i++)
        {
            String targetName = (String)
                bundles[i].getHeaders().get(BundleRecord.BUNDLE_NAME);
            if (targetName == null)
            {
                targetName = bundles[i].getLocation();
            }
            if ((targetName != null) && targetName.equalsIgnoreCase(name))
            {
                Bundle[] newTargets = new Bundle[targets.length + 1];
                System.arraycopy(targets, 0, newTargets, 0, targets.length);
                newTargets[targets.length] = bundles[i];
                targets = newTargets;
            }
        }

        return targets;
    }


  ParsedCommand parseInfo(String[] infos) {
    ParsedCommand pc = new ParsedCommand();
    for(int i = 0; infos != null && i < infos.length; i++) {
      String name    = infos[i];
      String version = null;
      int ix = infos[i].indexOf(";");
      if(ix != -1) {
	name    = infos[i].substring(0, ix);
	version = infos[i].substring(ix + 1);
      }
      pc.addTarget(name, version);
    }

    return pc;
  }
  
  private static class ParsedCommand
  {
    private static final int NAME_IDX = 0;
    private static final int VERSION_IDX = 1;
    
    private String[][] m_targets = new String[0][];
    
    public int getTargetCount()
    {
      return m_targets.length;
    }
    
    public String getTargetName(int i)
    {
      if ((i < 0) || (i >= getTargetCount()))
	{
	  return null;
	}
      return m_targets[i][NAME_IDX];
    }
    
    public String getTargetVersion(int i)
    {
      if ((i < 0) || (i >= getTargetCount()))
	{
	  return null;
	}
      return m_targets[i][VERSION_IDX];
    }
    
    public void addTarget(String name, String version)
    {
      String[][] newTargets = new String[m_targets.length + 1][];
      System.arraycopy(m_targets, 0, newTargets, 0, m_targets.length);
      newTargets[m_targets.length] = new String[] { name, version };
      m_targets = newTargets;
    }
  }
  
  /**
   * Wrap a PrintWriter into a PrintStream by overriding all methods.
   */
  public static class PrintWriterStream extends PrintStream {
    PrintWriter pw;
    boolean     bClose = false;
    
    /**
     * @param pw underlying writer to which all data is send to
     * @param bClose if <tt>true</tt> close the underlying writer
     *               when <tt>PrintWriterStream.close()</tt> is called.
     */
    public PrintWriterStream(PrintWriter pw, boolean bClose) {
      super(new ByteArrayOutputStream()); // This is really a dummy stream
      this.pw     = pw;
      this.bClose = bClose;
    }
    
    /**
     * Same as <tt>PrintWriterStream(pw, false)</tt>
     */
    public PrintWriterStream(PrintWriter pw) {
      this(pw, false);
    }
    
    /**
     * Only closes the underlying stream if
     * constructued with the close flag.
     */
    public void close() {
      super.close();
      if(bClose) {
	pw.close();
      }
    }
    
    /**
     * Write using the trivial, but possibly not always correct translation:
     * <pre>
     *  write((int) byte)
     * </pre>
     */
    public void write(byte[] buf, int off, int len) { 
      for(int i = off; i < off + len; i++) {
	write((int)buf[i]);
      }
    } 
    
    public void write(int b) { 
      pw.write(b);
    } 
    
    public boolean checkError() {
      return pw.checkError();
    }
    
    public void flush() {
      pw.flush();
    }
    
    public void print(boolean b) {
      pw.print(b);
    }
    
    public void print(char c) {
      pw.print(c);
    }
    
    public void print(char[] s) {
      pw.print(s);
    }
    
    public void print(double d) {
      pw.print(d);
    }

    public void print(float f) {
      pw.print(f);
    }
    
    public void print(int i) {
      pw.print(i);
    }
    
    public void print(long l) {
      pw.print(l);
    }
    
    public void print(Object obj) {
      pw.print(obj);
    }
    
    public void print(String s) {
      pw.print(s);
    }
    
    public void println() {
      pw.println();
    }
    
    public void println(boolean x) {
      pw.println(x);
    }
    
    public void println(char x) {
      pw.println(x);
    }
    
    public void println(char[] x) { 
      pw.println(x);
    }
    
    public void println(double x) { 
      pw.println(x);
    } 
    
    public void println(float x) { 
      pw.println(x);
    }
    
    public void println(int x) { 
      pw.println(x);
    }
    
    public void println(long x) { 
      pw.println(x);
    }
    
    public void println(Object x) { 
      pw.println(x);
    }
    
    public void println(String x) { 
      pw.println(x);
    } 
  }

}



