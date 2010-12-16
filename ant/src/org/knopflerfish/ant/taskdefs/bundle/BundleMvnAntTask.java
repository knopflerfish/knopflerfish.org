/*
 * Copyright (c) 2010-2010, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
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

package org.knopflerfish.ant.taskdefs.bundle;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.SortedSet;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;

import org.osgi.framework.Version;

/**
 * Task that analyzes a set of bundle jar files and builds an ant
 * build file that can deploy those jar files to a Maven 2 repository.
 *
 * <p>
 *
 * <h3>Parameters</h3>
 *
 * <table border=>
 *
 *  <tr>
 *   <th valign=top><b>Attribute</b></th>
 *   <th valign=top><b>Description</b></th>
 *   <th valign=top><b>Required</b></th>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>outFile</td>
 *   <td valign=top>
 *    Path for the resulting ant build file.
 *   </td>
 *   <td valign=top>Yes.<br> No default value.</td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>templateAntFile</td>
 *   <td valign=top>
 *   Path to a template ant file for creating Maven 2 repositories.
 *   </td>
 *  </td>
 *   <td valign=top>Yes.<br>No default value.</td>
 *  </tr>
 *
 *  <tr>
 *   <td valign=top>groupId</td>
 *   <td valign=top>
 *   Maven group id to use for bundles, for which a group id can not
 *   be derived from the bundles symbolic name.
 *   </td>
 *  </td>
 *   <td valign=top>No.<br>Default 'org.knopflerfish'.</td>
 *  </tr>
 *
 * </table>
 *
 * <h3>Parameters specified as nested elements</h3>
 * <h4>fileset</h4>
 *
 * (required)<br>
 * <p>
 * All jar files selected by the fileset will be included.
 * </p>
 *
 * <h3>Examples</h3>
 *
 * <pre>
 * &lt;bundlemvnant templateAntFile  = "${ant.dir}/ant_templates/toMvn.xml"
 *                  outFile          = "${root.out.dir}/toMvn.xml"
 *   &gt;
 *
 *     &lt;fileset dir="${release.dir}/osgi/jars"&gt;
 *       &lt;include name = "&ast;&ast;/&ast;.jar"/&gt;
 *     &lt;/fileset&gt;
 * </pre>
 *
 */
public class BundleMvnAntTask extends Task {

  private BundleArchives bas;

  public BundleMvnAntTask() {
  }


  private String groupId = "org.knopflerfish";
  public void setGroupId(final String s) {
    this.groupId = s;
  }

  private File templateAntFile;
  public void setTemplateAntFile(File f) {
    templateAntFile = f;

    if(!templateAntFile.exists()) {
      throw new BuildException("templateAntFile: " + f + " does not exist");
    }
    if(!templateAntFile.isFile()) {
      throw new BuildException("templateAntFile: " + f + " is not a file");
    }
  }

  private File outFile;
  public void setOutFile(File f) {
    outFile = f;
    if(outFile.exists() && !outFile.canWrite()) {
      throw new BuildException("outFile: " + f + " exists but is not writable");
    }
  }

  private List rcs = new ArrayList();
  public void addFileSet(FileSet fs) {
    rcs.add(fs);
  }

  // Implements Task
  public void execute() throws BuildException {
    log("Loading bundle information:", Project.MSG_VERBOSE);
    bas = new BundleArchives(this, rcs, true);
    bas.doProviders();

    try {
      writeAntFile();
    } catch (Exception e) {
      final String msg = "Failed to create ant build file: " +e;
      log(msg, Project.MSG_ERR);
      throw new BuildException(msg, e);
    }

  }

  private void writeAntFile()
    throws IOException
  {
    log("Creating build file: " +outFile, Project.MSG_VERBOSE);

    String ant = Util.loadFile(templateAntFile.getAbsolutePath());

    final StringBuffer targetNames = new StringBuffer(2048);
    final StringBuffer targets = new StringBuffer(2048);

    final String prefix1 = "  ";
    final String prefix2 = prefix1 + "  ";
    final String prefix3 = prefix2 + "  ";

    final Iterator it = bas.bsnToBundleArchives.entrySet().iterator();
    while (it.hasNext()) {
      final Map.Entry entry = (Map.Entry) it.next();
      final Set bsnSet = (Set) entry.getValue();
      //Sorted set with bundle archives, same bsn, different versions
      for (Iterator itV = bsnSet.iterator(); itV.hasNext(); ) {
        final BundleArchives.BundleArchive ba
          = (BundleArchives.BundleArchive) itV.next();
        final String targetName = ba.bsn +"-" +ba.version;

        targetNames.append(",\n").append(targetName);
        targets.append("\n\n")
          .append("<!-- ").append(ba.relPath).append(" -->\n")
          .append("<target name=\"").append(targetName).append("\">\n")
          .append(prefix1).append("<mvn_deploy_bundle\n")
          .append(prefix3).append("projDirName=\"").append(ba.projectName)
          .append("\"\n");
        addMavenCoordinates(targets, ba, prefix3);
        targets.append("\n").append(prefix3)
          .append("artifactName=\"").append(ba.name)
          .append("\"\n").append(prefix3)
          .append("artifactBundle=\"").append(ba.file.getAbsolutePath())
          .append("\"");

        // Optional attributes
        final String description = ba.getBundleDescription();
        if (null!=description) {
          targets.append("\n").append(prefix3)
            .append("description=\"").append(description).append("\"");
        }

        // Packing kind
        if (ba.pkgExportMap.containsKey("org.osgi.framework")) {
          targets.append("\n").append(prefix3).append("packing=\"jar\"");
        } else {
          targets.append("\n").append(prefix3).append("packing=\"bundle\"");
        }

        targets.append(">\n");

        addLicenses(targets, ba, "    ");
        addDependencies(targets, ba, "    ");

        targets.append("  </mvn_deploy_bundle>\n")
          .append("</target>");
      }
    }

    ant = replace(ant, "$(ANT_DIR)", project.getProperty("ant.dir"));
    ant = replace(ant, "$(DEPLOY_BUNDLE_TARGET_NAMES)", targetNames.toString());
    ant = replace(ant, "$(DEPLOY_BUNDLE_TARGETS)", targets.toString());

    Util.writeStringToFile(outFile, ant);
    log("wrote " + outFile, Project.MSG_INFO);
  }


  /**
   * Add Maven coordinates (group id, artifact id and version) for the
   * given bundle to the string buffer.
   * @param sb String buffer to append to.
   * @param ba Bundle archive to append coordinates for.
   * @param prefix Prefix to start each row with.
   */
  private void addMavenCoordinates(final StringBuffer sb,
                                   final BundleArchives.BundleArchive ba,
                                   final String prefix)
  {
    final int ix = ba.bsn.lastIndexOf('.');
    final String aId = -1==ix ? ba.bsn : ba.bsn.substring(ix+1);
    final String gId = -1==ix ? (String) groupId : ba.bsn.substring(0,ix);
    final Version v = ba.version;

    if (null!=gId) {
      sb.append(prefix).append("groupId=\"").append(gId).append("\"\n");
    }
    sb.append(prefix).append("artifactId=\"").append(aId).append("\"\n");
    sb.append(prefix).append("version=\"").append(v).append("\"");
  }


  /**
   * Add licenses element for the given bundle to the string
   * buffer.
   *
   * @param sb String buffer to append to.
   * @param ba Bundle archive to append dependencies for.
   * @param prefix Prefix to start each row with.
   */
  private void addLicenses(final StringBuffer sb,
                           final BundleArchives.BundleArchive ba,
                           final String prefix)
  {
    sb.append(prefix).append("<licenses>\n");

    final String prefix1 = prefix +"  ";
    final String prefix2 = prefix1 +"    ";
    boolean addDefault = true;

    final Iterator licenseIt = ba.getBundleLicense();
    while (licenseIt.hasNext()) {
      addDefault = false;
      final Map licenseMap = (Map) licenseIt.next();
      sb.append(prefix2).append("<license name=\"")
        .append(licenseMap.get("$key"))
        .append("\"");

      if (licenseMap.containsKey("description")) {
        sb.append("\n").append(prefix2).append("comments=\"")
          .append(licenseMap.get("description"))
          .append("\"");
      }

      if (licenseMap.containsKey("link")) {
        sb.append("\n").append(prefix2).append("url=\"")
          .append(licenseMap.get("link"))
          .append("\"");
      }

      sb.append("/>\n");
    }

    if (addDefault) {
      sb.append(prefix2)
        .append("<license name=\"&lt;&lt;EXTERNAL&gt;&gt;\"/>\n");
    }

    sb.append(prefix).append("</licenses>\n");
  }


  /**
   * Add dependencies element for the given bundle to the string
   * buffer.
   * @param sb String buffer to append to.
   * @param ba Bundle archive to append dependencies for.
   * @param prefix Prefix to start each row with.
   */
  private void addDependencies(final StringBuffer sb,
                               final BundleArchives.BundleArchive ba,
                               final String prefix)
  {
    sb.append(prefix).append("<dependencies>\n");
    final String prefix1 = prefix + "  ";
    final String prefix2 = prefix1 + "    ";

    final Iterator depEntryIter = selectCtDeps(ba).entrySet().iterator();
    while (depEntryIter.hasNext()) {
      final Map.Entry depEntry = (Map.Entry) depEntryIter.next();
      final BundleArchives.BundleArchive depBa = (BundleArchives.BundleArchive)
        depEntry.getKey();
      final Set pkgNames = (Set) depEntry.getValue();

      sb.append(prefix1).append("<dependency\n");
      addMavenCoordinates(sb, depBa, prefix2);
      if (pkgNames.contains("org.osgi.framework")) {
        sb.append("\n").append(prefix2).append("scope=\"provided\"");
      }
      sb.append(">\n");
      sb.append(prefix2).append("<!--").append(pkgNames).append("-->\n");
      sb.append(prefix1).append("</dependency>\n");
    }

    if (0<ba.pkgUnprovidedMap.size()) {
      log("  Imports without any provider: " +ba.pkgUnprovidedMap,
          Project.MSG_DEBUG);
    }

    sb.append(prefix).append("</dependencies>\n");
  }


  /**
   * Selects a subset of the compile time dependencies so that each
   * package is only provided once.
   *
   * @param ba Bundle archive to select dependencies for.
   */
  private Map selectCtDeps(final BundleArchives.BundleArchive ba)
  {
    log("Selecting dependencies for : "+ba, Project.MSG_VERBOSE);

    // The total set of packages that are provided by the dependencies.
    final Set pkgs = new TreeSet();

    // The sub-set of the dependency entires that are API-bundles.
    final List depsApi = new ArrayList();

    // The sub-set of the dependency entires that are not API-bundles.
    final List depsNonApi = new ArrayList();

    // The resulting collection of dependencies
    final Map res = new TreeMap();

    // Group providing bundles in to API-bundles and non-API-bundles
    // and the set of packages provided by all providers.
    final Iterator itCtP = ba.pkgCtProvidersMap.entrySet().iterator();
    while (itCtP.hasNext()) {
      final Map.Entry ctPEntry = (Map.Entry) itCtP.next();
      final BundleArchives.BundleArchive ctBa = (BundleArchives.BundleArchive)
        ctPEntry.getKey();
      final Set ctPkgs = (Set) ctPEntry.getValue();

      if (ctBa.isAPIBundle()) {
        log("  APIbundle: "+ctBa +" provides "+ctPkgs, Project.MSG_DEBUG);
        depsApi.add(ctPEntry);
      } else {
        log("  NonAPIbundle: "+ctBa +" provides "+ctPkgs, Project.MSG_DEBUG);
        depsNonApi.add(ctPEntry);
      }
      pkgs.addAll(ctPkgs);
    }

    log("  Provided imported packages: "+pkgs, Project.MSG_VERBOSE);

    final Comparator cmp = new ProvidesEntrySetComparator();
    Collections.sort(depsApi, cmp);
    Collections.sort(depsNonApi, cmp);

    selectProviders(res, pkgs, depsApi);
    selectProviders(res, pkgs, depsNonApi);

    return res;
  }


  /**
   * Add providing bundles from the <code>deps</code> list that
   * provides at least one of the packages in <code>pkgs</code>. Try
   * to avoid having multiple providers of the same package.
   *
   * @param res  The providers to make depencies of.
   * @param pkgs The set of packages that shall be provided by the
   *             providers added to res.
   * @param deps Provider bundles that are dependency candidates.
   */
  private void selectProviders(final Map res,
                               final Set pkgs,
                               final List deps)
  {
    // Iterate over dependency candidates that exports packages,
    // Add bundles that contributes at least one package to res.
    for (Iterator itDeps = deps.iterator(); 0<pkgs.size() && itDeps.hasNext();){
      final Map.Entry entry = (Map.Entry) itDeps.next();
      final BundleArchives.BundleArchive ba = (BundleArchives.BundleArchive)
        entry.getKey();
      final Set pPkgs = (Set) entry.getValue();

      log("  Trying provider: " +ba +": exporting " +pPkgs + " looking for: "
          +pkgs, Project.MSG_DEBUG);

      if (pkgs.removeAll(pPkgs)) {
        // entry provides needed packages, add its bundle to the result.
        log("  Selecting provider: " + ba + " exporting "+pPkgs,
            Project.MSG_VERBOSE);

        // Remove any provider that will not provide any unique
        // package after the addition of ba.
        for (Iterator itRes = res.entrySet().iterator(); itRes.hasNext();) {
          final Map.Entry resEntry = (Map.Entry) itRes.next();
          final BundleArchives.BundleArchive resBa
            = (BundleArchives.BundleArchive) resEntry.getKey();
          final Set resPkgs = (Set) resEntry.getValue();

          if (pPkgs.containsAll(resPkgs)) {
            log("  Removing redundant provider: "+resBa, Project.MSG_VERBOSE);
            itRes.remove();
          }
        }
        res.put(ba, pPkgs);
      }
    }
  }

  /**
   * Sort map entries that consists of a bundle archive as key and a
   * set as value in increasing order based on the size of the set and
   * if equal the natural order of the bundle archives.
   */
  static class ProvidesEntrySetComparator
    implements Comparator
  {
    public int compare(Object o1, Object o2)
    {
      Map.Entry e1 = (Map.Entry) o1;
      Map.Entry e2 = (Map.Entry) o2;

      int res = ((Set) e1.getValue()).size() - ((Set) e2.getValue()).size();

      return 0!=res ? res
        : ((BundleArchives.BundleArchive)e1.getKey())
        .compareTo((BundleArchives.BundleArchive)e1.getKey());
    }

    public boolean equals(Object obj)
    {
      return this==obj;
    }
  }

  static String replace(String src, String a, String b) {
    return Util.replace(src, a, b == null ? "" : b);
  }

}
