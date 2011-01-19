/*
 * Copyright (c) 2010-2011, KNOPFLERFISH project
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.knopflerfish.ant.taskdefs.bundle.BundleArchives.BundleArchive;
import org.osgi.framework.Version;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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
 *  <tr>
 *   <td valign=top>dependencyManagementFile</td>
 *   <td valign=top>
 *    Path to write an XML file with a &lt;dependencyManagement&gt;-element
 *    describing all the artifacts that will be created by the generated build
 *    file.
 *   </td>
 *   <td valign=top>No.<br> No default value.</td>
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

  private File dependecyManagementFile;
  public void setDependencyManagementFile(File f) {
    dependecyManagementFile = f;
    if(dependecyManagementFile.exists() && !dependecyManagementFile.canWrite()) {
      throw new BuildException("dependencyManagementFile: " + f + " exists but is not writable");
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
      writeBuildFile();
      writeDependencyManagementFile();
    } catch (Exception e) {
      final String msg = "Failed to create ant build file: " +e;
      log(msg, Project.MSG_ERR);
      throw new BuildException(msg, e);
    }

  }

  private void writeBuildFile()
    throws IOException
  {
    log("Creating build file: " + outFile, Project.MSG_VERBOSE);

    final String prefix1 = "  ";
    final String prefix2 = prefix1 + "  ";

    final Document doc = Util.loadXML(templateAntFile);
    final Element project = (Element) doc.getDocumentElement();

    setPropertyLocation(project,"ant.dir", getProject().getProperty("ant.dir"));

    final StringBuffer targetNames = new StringBuffer(2048);

    final Iterator it = bas.bsnToBundleArchives.entrySet().iterator();
    while (it.hasNext()) {
      final Map.Entry entry = (Map.Entry) it.next();
      final Set bsnSet = (Set) entry.getValue();
      // Sorted set with bundle archives, same bsn, different versions
      for (Iterator itV = bsnSet.iterator(); itV.hasNext();) {
        final BundleArchive ba = (BundleArchive) itV.next();
        final String targetName = ba.bsn + "-" + ba.version;
        targetNames.append(",").append(targetName);

        final Comment comment = doc.createComment(ba.relPath);
        final Element target = doc.createElement("target");
        target.setAttribute("name", targetName);

        final Element mvnDeployBundle = doc.createElement("mvn_deploy_bundle");
        target.appendChild(doc.createTextNode("\n"+prefix2));
        target.appendChild(mvnDeployBundle);

        mvnDeployBundle.setAttribute("projDirName", ba.projectName);
        addMavenCoordinates(mvnDeployBundle, ba);
        mvnDeployBundle.setAttribute("artifactName", ba.name);
        mvnDeployBundle.setAttribute("artifactBundle", ba.file.getAbsolutePath());

        // Optional attributes
        final String description = ba.getBundleDescription();
        if (null != description) {
          mvnDeployBundle.setAttribute("description", description);
        }

        // Packing kind
        if (ba.pkgExportMap.containsKey("org.osgi.framework")) {
          mvnDeployBundle.setAttribute("packing", "jar");
        } else {
          mvnDeployBundle.setAttribute("packing", "bundle");
        }

        addLicense(mvnDeployBundle, ba, prefix2);
        addDependencies(mvnDeployBundle, ba, prefix2);

        // Put the end of the target-element on a separate line
        target.appendChild(doc.createTextNode("\n"+prefix1));

        project.appendChild(doc.createTextNode("\n"+prefix1));
        project.appendChild(comment);
        project.appendChild(doc.createTextNode("\n"+prefix1));
        project.appendChild(target);
        project.appendChild(doc.createTextNode("\n"+prefix1));
      }
    }

    setTargetAttr(project, "all", "depends", "init" +targetNames.toString());

    Util.writeDocumentToFile(outFile, doc);
    log("wrote " + outFile, Project.MSG_VERBOSE);
  }

  private void writeDependencyManagementFile()
    throws IOException
  {
    log("Creating dependency management file: " + dependecyManagementFile, Project.MSG_VERBOSE);

    final String prefix1 = "  ";
    final String prefix2 = prefix1 + "  ";
    final String prefix3 = prefix2 + "  ";

    final Document doc = Util.createXML("dependencyManagement");
    final Element root = (Element) doc.getDocumentElement();

    root.appendChild(doc.createTextNode("\n"+prefix1));
    final Element dependencies = doc.createElement("dependencies");
    root.appendChild(dependencies);

    final Iterator it = bas.bsnToBundleArchives.entrySet().iterator();
    while (it.hasNext()) {
      final Map.Entry entry = (Map.Entry) it.next();
      final Set bsnSet = (Set) entry.getValue();
      // Sorted set with bundle archives, same bsn, different versions
      for (Iterator itV = bsnSet.iterator(); itV.hasNext();) {
        final BundleArchive ba = (BundleArchive) itV.next();

        dependencies.appendChild(doc.createTextNode("\n\n" +prefix2));
        dependencies.appendChild(doc.createComment(ba.relPath));
        dependencies.appendChild(doc.createTextNode("\n" +prefix2));

        final Element dependency = doc.createElement("dependency");
        dependencies.appendChild(dependency);

        // Dummy element to read mvn coordinates from
        final Element coordinateEl = doc.createElement("dummy");
        addMavenCoordinates(coordinateEl, ba);

        dependency.appendChild(doc.createTextNode("\n" +prefix3));
        final Element groupId = doc.createElement("groupId");
        dependency.appendChild(groupId);
        groupId.appendChild(doc.createTextNode(coordinateEl.getAttribute("groupId")));

        dependency.appendChild(doc.createTextNode("\n" +prefix3));
        final Element artifactId = doc.createElement("artifactId");
        dependency.appendChild(artifactId);
        artifactId.appendChild(doc.createTextNode(coordinateEl.getAttribute("artifactId")));

        dependency.appendChild(doc.createTextNode("\n" +prefix3));
        final Element version = doc.createElement("version");
        dependency.appendChild(version);
        version.appendChild(doc.createTextNode(coordinateEl.getAttribute("version")));

        dependency.appendChild(doc.createTextNode("\n" +prefix2));
      }
    }
    dependencies.appendChild(doc.createTextNode("\n" +prefix1));
    root.appendChild(doc.createTextNode("\n"));

    Util.writeDocumentToFile(dependecyManagementFile, doc);
    log("wrote " + dependecyManagementFile, Project.MSG_VERBOSE);
  }

  /**
   * Set the location of the named ant property. The property must exist and be
   * a child of the specified element.
   *
   * @param elem
   *          The element owning the property element to update
   * @param name
   *          The name of the property to set location of.
   * @param location
   *          The new location value.
   */
  private void setPropertyLocation(final Element el, final String name, final String location) {
    final NodeList propertyNL = el.getElementsByTagName("property");
    boolean found = false;
    for (int i = 0; i<propertyNL.getLength(); i++) {
      final Element property = (Element) propertyNL.item(i);
      if (name.equals(property.getAttribute("name"))) {
        log("Setting <property name=\"" +name +"\" location=\"" +location +"\" ...>.", Project.MSG_DEBUG);
        property.setAttribute("location", location);
        found = true;
        break;
      }
    }
    if (!found) {
      throw new BuildException("No <property name=\"" +name +"\" ...> in XML document " +el);
    }
  }

  /**
   * Set an attribute on the named target element. The target element must exist
   * and be a child of the project-element.
   *
   * @param el
   *          The element owning the target element to be updated.
   * @param name
   *          The name of the target-element to set an attribute for.
   * @param attrName
   *          The name of the attribute to set.
   * @param attrValue
   *          The new attribute value.
   */
  private void setTargetAttr(final Element el, final String name, final String attrName, final String attrValue) {
    final NodeList propertyNL = el.getElementsByTagName("target");
    boolean found = false;
    for (int i = 0; i<propertyNL.getLength(); i++) {
      final Element target = (Element) propertyNL.item(i);
      if (name.equals(target.getAttribute("name"))) {
        log("Setting <target name=\"" +name +"\" attrName =\"" +attrValue +"\" ...>.", Project.MSG_DEBUG);
        target.setAttribute(attrName, attrValue);
        found = true;
        break;
      }
    }
    if (!found) {
      throw new BuildException("No <property name=\"" +name +"\" ...> in XML document " +el);
    }
  }

  /**
   * Add Maven coordinates, attributes for group id, artifact id and version to
   * the given element.
   *
   * @param el
   *          The element to add Maven coordinates to.
   * @param ba
   *          The bundle archive to defining the coordinates.
   */
  private void addMavenCoordinates(final Element el, final BundleArchive ba) {
    final int ix = ba.bsn.lastIndexOf('.');
    final String aId = -1==ix ? ba.bsn : ba.bsn.substring(ix+1);
    final String gId = -1==ix ? (String) groupId : ba.bsn.substring(0,ix);
    final Version v = ba.version;

    if (null!=gId) {
      el.setAttribute("groupId", gId);
    }
    el.setAttribute("artifactId", aId);
    el.setAttribute("version", v.toString());
  }

  /**
   * Add licenses element for the given bundle to the string buffer.
   *
   * @param el
   *          element to add the license element to.
   * @param ba
   *          The bundle archive to defining the coordinates.
   * @param prefix
   *          Whitespace to added before the owning element.
   */
  private void addLicense(final Element el, final BundleArchive ba, final String prefix)
  {
    final Element licenses = el.getOwnerDocument().createElement("licenses");
    final String prefix1 = prefix + "  ";
    final String prefix2 = prefix1 + "  ";

    el.appendChild(el.getOwnerDocument().createTextNode("\n"+prefix1));
    el.appendChild(licenses);
    el.appendChild(el.getOwnerDocument().createTextNode("\n"+prefix));

    boolean addDefault = true;

    final Iterator licenseIt = ba.getBundleLicense();
    while (licenseIt.hasNext()) {
      addDefault = false;
      final Map licenseMap = (Map) licenseIt.next();
      final Element license = el.getOwnerDocument().createElement("license");
      license.setAttribute("name", licenseMap.get("$key").toString());
      licenses.appendChild(el.getOwnerDocument().createTextNode("\n"+prefix2));
      licenses.appendChild(license);

      if (licenseMap.containsKey("description")) {
        license.setAttribute("comments", licenseMap.get("description").toString());
      }
      if (licenseMap.containsKey("link")) {
        license.setAttribute("url", licenseMap.get("link").toString());
      }
    }

    if (addDefault) {
      final Element license = el.getOwnerDocument().createElement("license");
      license.setAttribute("name", "<<EXTERNAL>>");
      licenses.appendChild(el.getOwnerDocument().createTextNode("\n"+prefix2));
      licenses.appendChild(license);
    }
    licenses.appendChild(el.getOwnerDocument().createTextNode("\n"+prefix1));
  }

  /**
   * Add dependencies element for the given bundle to the string buffer.
   *
   * @param el
   *          element to add the dependencies to.
   * @param ba
   *          The bundle archive to defining the coordinates.
   * @param prefix
   *          Whitespace to add before the new element.
   */
  private void addDependencies(Element el, BundleArchive ba, String prefix)
  {
    final Element dependencies = el.getOwnerDocument().createElement("dependencies");
    final String prefix1 = prefix + "  ";
    final String prefix2 = prefix1 + "  ";

    el.appendChild(el.getOwnerDocument().createTextNode("\n"+prefix1));
    el.appendChild(dependencies);
    el.appendChild(el.getOwnerDocument().createTextNode("\n"+prefix));

    final Iterator depEntryIter = selectCtDeps(ba).entrySet().iterator();
    while (depEntryIter.hasNext()) {
      final Map.Entry depEntry = (Map.Entry) depEntryIter.next();
      final BundleArchives.BundleArchive depBa = (BundleArchives.BundleArchive)
        depEntry.getKey();
      final Set pkgNames = (Set) depEntry.getValue();

      dependencies.appendChild(el.getOwnerDocument().createTextNode("\n"+prefix2));
      dependencies.appendChild(el.getOwnerDocument().createComment(pkgNames.toString()));

      final Element dependency = el.getOwnerDocument().createElement("dependency");
      addMavenCoordinates(dependency, depBa);
      if (pkgNames.contains("org.osgi.framework")) {
        dependency.setAttribute("scope", "provided");
      }

      dependencies.appendChild(el.getOwnerDocument().createTextNode("\n"+prefix2));
      dependencies.appendChild(dependency);
      dependencies.appendChild(el.getOwnerDocument().createTextNode("\n"));
    }
    dependencies.appendChild(el.getOwnerDocument().createTextNode("\n"+prefix1));

    if (0<ba.pkgUnprovidedMap.size()) {
      log("  Imports without any provider: " +ba.pkgUnprovidedMap,
          Project.MSG_DEBUG);
    }
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

    // The sub-set of the dependency entries that are API-bundles.
    final List depsApi = new ArrayList();

    // The sub-set of the dependency entries that are not API-bundles.
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
   * @param res  The selected providers to make dependencies of.
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
