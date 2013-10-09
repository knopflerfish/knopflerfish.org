/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Manifest;
import org.apache.tools.ant.taskdefs.ManifestException;
import org.apache.tools.ant.types.EnumeratedAttribute;

import org.osgi.framework.Constants;

import org.knopflerfish.ant.taskdefs.bundle.Util.HeaderEntry;


/**
 * Extension of the standard Manifest task.
 * <p>
 * This task builds a manifest file from three different sources:
 * <ol>
 *   <li>A template manifest file.
 *   <li>Project properties with given prefix.
 *   <li>Nested attribute and section data.
 * </ol>
 * It may also be used to create properties (with a given prefix) for
 * each main section attribute in the template manifest file.
 *
 * <h3>Parameters</h3>
 *
 * <table border=>
 *  <tr>
 *   <td valign=top><b>Attribute</b></td>
 *   <td valign=top><b>Description</b></td>
 *   <td valign=top><b>Required</b></td>
 *  </tr>
 *  <tr>
 *    <td valign="top">file</td>
 *    <td valign="top">the manifest-file to create.</td>
 *    <td valign="top" align="center">
 *      Yes if "attributePropertyPrefix" is empty, otherwise No.</td>
 *  </tr>
 *  <tr>
 *    <td valign="top">encoding</td>
 *    <td valign="top">
 *      The encoding used to read the existing manifest when updating.</td>
 *    <td valign="top" align="center">No, defaults to UTF-8 encoding.</td>
 *  </tr>
 *  <tr>
 *    <td valign="top">mode</td>
 *    <td valign="top">
 *      One of "update", "replace", "template" and "templateOnly"
 *      default is "replace".
 *      <p>
 *      The "mode" determines which sources to use when creating the
 *      resulting manifest:
 *      <dl>
 *       <dt><code>replace</code> <dd>Use properties and nested data.
 *       <dt><code>update</code>  <dd>Use template, properties and nested data.
 *       <dt><code>template</code><dd>Use template and nested data.
 *       <dt><code>templateOnly</code><dd>Use template.
 *      </dl>
 *    </td>
 *    <td valign="top" align="center">No.</td>
 *  </tr>
 *  <tr>
 *    <td valign="top">templateFile</td>
 *    <td valign="top">the template manifest file to load.</td>
 *    <td valign="top" align="center">No.</td>
 *  </tr>
 *  <tr>
 *    <td valign="top">attributePropertyPrefix</td>
 *    <td valign="top">If set and a template file is given but no file
 *                     to write to export all attributes
 *                     from the main section of the template file as
 *                     properties.
 *                     <p>
 *                     If set and mode is one of "update", "replace"
 *                     then create main section attributes for all
 *                     project properties that starts with the prefix.
 *                     <p>
 *                     If set and "file" is given then export all attributes
 *                     written to the main section as properties.
 *                     <p>
 *                     The name of property that maps to a main
 *                     section attribute is the value of
 *                     "attributePropertyPrefix" followed by the
 *                     attribute name. The value is the attribute value.
 *    </td>
 *    <td valign="top" align="center">No.</td>
 *  </tr>
 *  <tr>
 *    <td valign="top">kind</td>
 *    <td valign="top">The kind of bundle that the manifest is for.
 *                     <p>
 *                     If given this string will be appended to the
 *                     following manifest attributes of the main section:
 *                     <ul>
 *                       <li>Bundle-Name
 *                       <li>Bundle-SymbolicName
 *                       <li>Bundle-UUID
 *                       <li>Bundle-Description
 *                       <li>Bundle-Category  (only for kind="api").
 *                    </ul>
 *                    All main section manifest attribute starting
 *                    with "kind-" will be replaced with a main section
 *                    attribute without the prefix. E.g., if kind="api"
 *                    and there is an attribute named "api-Export-Package"
 *                    then it will be renamed to "Export-Package", overriding
 *                    any previous definition of "Export-Package".
 *    </td>
 *    <td valign="top" align="center">No.</td>
 *  </tr>
 *  <tr>
 *    <td valign="top">allKinds</td>
 *    <td valign="top">A comma separated list specifying all kinds.
 *                     <p>
 *                     If given all main section attributes starting with
 *                     any of the kinds in <code>allKinds</code> will
 *                     be removed after the specified kind has been used to
 *                     override attributes. E.g., if <code>kind="all"</code>
 *                     and <code>allKinds="api,all"</code> and there is
 *                     an attribute named <code>"api-Export-Package"</code>
 *                     then that attribute will be removed from the
 *                     resulting manifest.
 *    </td>
 *    <td valign="top" align="center">No.</td>
 *  </tr>
 *  <tr>
 *    <td valign="top">mainAttributesToSkip</td>
 *    <td valign="top">Comma separated list with names of main section
 *                     attributes to weed out when writing the
 *                     manifest file.
 *    </td>
 *    <td valign="top" align="center">No.</td>
 *  </tr>
 *  <tr>
 *    <td valign="top">replaceEEmin</td>
 *    <td valign="top">Replace the filter part of any osgi.ee requirement
 *                     for the OSGi/Minimum EE with the given value.
 *                     E.g., <code>replaceEEmin="(&(osgi.ee=JavaSE)(version>=1.7))"</code>
 *                     will replace any filter matching 'OSGi/Minimum'
 *                     in the osgi.ee name space with the given filter expression.
 *    </td>
 *    <td valign="top" align="center">No.</td>
 *  </tr>
 *  <tr>
 *    <td valign="top">verbose</td>
 *    <td valign="top">If set to "true" then log the name of the
 *                     bundle activator together with the imported
 *                     and exported packages</td>
 *    <td valign="top" align="center">No.</td>
 *  </tr>
 * </table>
 *
 * <h3>Nested elements</h3>
 *
 * <h4><a name="attribute">attribute</h4></h4>
 * <p>One attribute for the manifest file.  Those attributes that are
 * not nested into a section will be added to the "Main" section.</p>
 * <table border="1" cellpadding="2" cellspacing="0">
 *   <tr>
 *     <td valign="top"><b>Attribute</b></td>
 *     <td valign="top"><b>Description</b></td>
 *     <td align="center" valign="top"><b>Required</b></td>
 *   </tr>
 *   <tr>
 *     <td valign="top">name</td>
 *     <td valign="top">the name of the attribute.</td>
 *     <td valign="top" align="center">Yes</td>
 *   </tr>
 *   <tr>
 *     <td valign="top">value</td>
 *     <td valign="top">the value of the attribute.</td>
 *     <td valign="top" align="center">Yes</td>
 *   </tr>
 * </table>
 *
 *
 * <h4>section</h4>
 * <p>A manifest section - you can nest <a
 *  href="#attribute">attribute</a> elements into sections.</p>
 *
 * <table border="1" cellpadding="2" cellspacing="0">
 *   <tr>
 *     <td valign="top"><b>Attribute</b></td>
 *     <td valign="top"><b>Description</b></td>
 *     <td align="center" valign="top"><b>Required</b></td>
 *   </tr>
 *   <tr>
 *     <td valign="top">name</td>
 *     <td valign="top">the name of the section.</td>
 *     <td valign="top" align="center">No, if omitted it will be assumed
 *        to be the main section.</td>
 *   </tr>
 * </table>
 *
 * <h3>Examples</h3>
 *
 * <h4>Build bundle manifest for the impl jar</h4>
 *
 * <pre>
 *  &lt;bundlemanifest mode="template"
 *                  kind="impl"
 *                  mainAttributesToSkip="Export-Package"
 *                  attributePropertyPrefix="bmfa."
 *                  templateFile="bundle.manifest"
 *                  verbose="true"
 *                  file="${outdir}/impl.mf"&gt;
 *     &lt;attribute name="Build-Date"       value="${bundle.date}"/&gt;
 *     &lt;attribute name="Built-From"       value="${proj.dir}"/&gt;
 *   &lt;/bundlemanifest&gt;
 * </pre>
 *
 *
 * <h4>Create properties for main section attributes in the template
 * manifest file</h4>
 *
 * <pre>
 *   &lt;bundlemanifest mode="template"
 *                   attributePropertyPrefix = "bmfa."
 *                   templateFile="bundle.manifest"&gt;
 *   &lt;/bundlemanifest&gt;
 * </pre>
 *
 */
public class BundleManifestTask extends Task {
  /**
   * Default constructor.
   */
  public BundleManifestTask() {
    super();
    mode = new Mode();
    mode.setValue("replace");
   }

  /**
   * Helper class for bundle manifest's mode attribute.
   */
  public static class Mode extends EnumeratedAttribute {
    /**
     * Get Allowed values for the mode attribute.
     *
     * @return a String array of the allowed values.
     */
    @Override
    public String[] getValues() {
      return new String[] {"update", "replace", "template", "templateOnly"};
    }
  }

  /**
   * The mode with which the manifest file is written
   */
  private Mode mode;

  /**
   * The encoding of the manifest template file.
   */
  private String encoding;

  /**
   * Comma separated list of names of attributes that must not be
   * present in the main section of the resulting manifest.
   */
  private String mainAttributesToSkip;

  /**
   * The kind of bundle to generate manifest for.
   * If given this string will be appended to the following manifest
   * attributes of the main section:
   * <ul>
   *   <li>Bundle-Name
   *   <li>Bundle-SymbolicName
   *   <li>Bundle-UUID
   * </ul>
   */
  private String bundleKind;

  /**
   * All known kinds of bundles. Used to remove unused kind specific main
   * section manifest attributes.
   */
  private final Set<String> allBundleKinds = new HashSet<String>();

  /**
   * Prefix of project properties to add main section attributes for.
   *
   * For each property in the project with a name that starts with this
   * prefix a manifest attribute in the main section will be created.
   * The attribute name will be the property name without the prefix
   * and the attribute value will be the property value.
   */
  private String attributePropertyPrefix;


  /**
   * The manifest template file.
   */
  private File manifestTemplateFile;

  /**
   * The manifest file to create.
   */
  private File manifestFile;

  /**
   * Holds explicit manifest data given in the build file.
   */
  private final Manifest manifestNested = new Manifest();

  /**
   * The encoding to use for reading in the manifest template file.
   * Default encoding is UTF-8.
   *
   * @param encoding the manifest template file encoding.
   */
  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  /**
   * The name of the template manifest file.
   *
   * @param f the template manifest file to load.
   */
  public void setTemplateFile(File f) {
    manifestTemplateFile = f;
  }

  /**
   * The name of the manifest file to create.
   *
   * @param f the manifest file to write.
   */
  public void setFile(File f) {
    manifestFile = f;
  }

  /**
   * Which sources to use when creating the resulting manifest.
   * <dl>
   *   <dt><code>replace</code>  <dd>Use properties and nested data.
   *   <dt><code>update</code>   <dd>Use template, properties and nested data.
   *   <dt><code>template</code> <dd>Use template and nested data.
   *   <dt><code>templateOnly</code><dd>Use template.
   * </dl>
   * @param m the mode value one of - <code>update</code>,
   *          <code>replace</code>, <code>template</code> and
   *          <code>templateOnly</code>.
   */
  public void setMode(Mode m) {
    mode = m;
  }

  /**
   * Comma separated list of attributes to skip from the main section.
   * @param s main section attributes to skip from out put.
   */
  public void setMainAttributesToSkip(String s) {
    mainAttributesToSkip = s.trim();
  }

  /**
   * Bundle kind, will be appended to some of the bundle specific
   * attributes in the main section.
   * @param s the kind of bundle that we are writing a manifest file for.
   */
  public void setKind(String s) {
    bundleKind = s.trim();
  }


  /**
   * Comma separated list of known bundle kinds. Any main section attribute
   * starting with one of the known kind values will be removed from the
   * resulting manifest after processing of kind specific overrides.
   *
   * @param s
   *          Comma separated list of bundle kinds.
   */
  public void setAllKinds(String s)
  {
    if (s != null && (s = s.trim()).length() > 0) {
      final StringTokenizer st = new StringTokenizer(s, ",");
      while (st.hasMoreTokens()) {
        final String kind = st.nextToken();
        allBundleKinds.add(kind.trim());
      }
    } else {
      allBundleKinds.clear();
    }
    log("All bundle kinds " + allBundleKinds + ".", Project.MSG_DEBUG);
  }

  /**
   * The name of the OSGi/Minimum Execution Environment.
   */
  private final String OSGI_MINIMUM_EE_NAME = "OSGi/Minimum";

  /**
   * Replacement filter for the filter in required capabilities in the
   * <code>osgi.ee</code> name-space that requires the OSGi/Minimum EE.
   */
  private String replaceEEmin;

  /**
   * Sets the replacement filter expression for required capabilities in the
   * osgi.ee name-space that requires the OSGi/Minimum EE.
   *
   * @param s
   */
  public void setReplaceEEmin(String s)
  {
    if (s != null && (s = s.trim()).length() > 0) {
      replaceEEmin = s;
      log("replaceEEmin: '" + replaceEEmin + "'.", Project.MSG_DEBUG);
    }
  }

  /**
   * If set to true the bundle activator, export package and import
   * package list in the written manifest will be printed on the
   * console.
   */
  private boolean verbose = false;

  /**
   * Set the verbosity of this task.
   * If set to true the bundle activator, export package and import
   * package list in the written manifest will be printed on the
   * console.
   * @param b verbose or not.
   */
  public void setVerbose(boolean b) {
    verbose = b;
  }

  private void doVerbose(Manifest mf)
  {
    if (verbose) {
      final Manifest.Section   ms = mf.getMainSection();
      doVerbose( ms, "Bundle-Activator", "activator");
      doVerbose( ms, "Export-Package", "exports");
      doVerbose( ms, "Import-Package", "imports");
    }
  }

  private void doVerbose(Manifest.Section ms, String attrName, String heading)
  {
    final Manifest.Attribute ma = ms.getAttribute(attrName);
    if (null!=ma) {
      final String val = ma.getValue();
      if (!isPropertyValueEmpty(val)) {
        log( heading +" = "+val, Project.MSG_INFO);
      }
    }
  }


  /**
   * Set the prefix of project properties to add main section
   * attributes for.
   *
   * For each property in the project with a name that starts with this
   * prefix a manifest attribute in the main section will be created.
   * The attribute name will be the property name without the prefix
   * and the attribute value will be the property value.
   *
   * @param s the property names prefix to check for.
   */
  public void setAttributePropertyPrefix( String s) {
    attributePropertyPrefix = s;
  }

  /**
   * If <code>attributePropertyPrefix</code> is set then iterate over
   * all properties and add attributes to the main section of
   * the given manifest for those properties that starts with the prefix.
   *
   * The name of the attribute will be the property name without the
   * prefix and the value will be the property value.
   *
   * @param mf The manifest to add the property based attributes to.
   */
  private void addAttributesFromProperties(Manifest mf)
  {
    if (null!=attributePropertyPrefix) {
      final int       prefixLength = attributePropertyPrefix.length();
      final Project   project      = getProject();
      final Manifest.Section mainS = mf.getMainSection();
      @SuppressWarnings("unchecked")
      final Hashtable<String,?> properties   = project.getProperties();
      for (final Enumeration<String> pe = properties.keys(); pe.hasMoreElements();) {
        final String key = pe.nextElement();
        if (key.startsWith(attributePropertyPrefix)) {
          final String attrName  = key.substring(prefixLength);
          final String attrValue = (String) properties.get(key);
          if(!BUNDLE_EMPTY_STRING.equals(attrValue)) {
            Manifest.Attribute attr = mainS.getAttribute(attrName);
            if (null!=attr) {
              throw new BuildException
                ( "Can not add main section attribute for property '"
                  +key+"' with value '"+attrValue+"' since a "
                  +"main section attribute with that name exists: '"
                  +attr.getName() +": "+attr.getValue() +"'.",
                  getLocation());
            }
            try {
              attr = new Manifest.Attribute(attrName, attrValue);
              mf.addConfiguredAttribute(attr);
              log("from propety '" +attrName +": "+attrValue+"'.",
                  Project.MSG_VERBOSE);
            } catch (final ManifestException me) {
              throw new BuildException
                ( "Failed to add main section attribute for property '"
                  +key+"' with value '"+attrValue+"'.\n"+me.getMessage(),
                  me, getLocation());
            }
          }
        }
      }
    }
  }

  /**
   * We must ensure that the case used in attribute names when they
   * are mapped to properties by
   * <code>updatePropertiesFromMainSectionAttributeValues()</code> are
   * the same as the one used in the build files, i.e., the one used
   * in the OSGi specification. If not it may happen that a we get two
   * properties defined for the same attribute (with different cases)
   * if this happens there will be an error when adding back the
   * properties to the generated manifest.  Thus we need a list of all
   * OSGi specified attribute names in the case used in the
   * specification.
   */
  private static final String[] osgiAttrNames = new String[]{
    "Application-Icon",
    "Bundle-APIVendor",
    "Bundle-Activator",
    "Bundle-Category",
    "Bundle-Classpath",
    "Bundle-Config",
    "Bundle-ContactAddress",
    "Bundle-Copyright",
    "Bundle-Description",
    "Bundle-DocURL",
    "Bundle-Localization",
    "Bundle-ManifestVersion",
    "Bundle-Name",
    "Bundle-NativeCode",
    "Bundle-RequiredExecutionEnvironment",
    "Bundle-SubversionURL",
    "Bundle-SymbolicName",
    "Bundle-UUID",
    "Bundle-UpdateLocation",
    "Bundle-Vendor",
    "Bundle-Version",
    "DynamicImport-Package",
    "Export-Package",
    "Export-Service",
    "Fragment-Host",
    "Import-Package",
    "Import-Service",
    "Require-Bundle",
    "Service-Component",
    };

  /**
   * Mapping from attribute key, all lower case, to attribute name
   * with case according to the OSGi specification.
   */
  private static final Hashtable<String,String> osgiAttrNamesMap = new Hashtable<String, String>();
  static {
    for (final String osgiAttrName : osgiAttrNames) {
      osgiAttrNamesMap.put(osgiAttrName.toLowerCase(), osgiAttrName);
    }
  }


  /**
   * If <code>attributePropertyPrefix</code> is set then iterate over
   * all attributes in the main section and set the value for
   * corresponding property to the value of that attribute.
   *
   * The name of the attribute will be the property name without the
   * prefix and the value will be the property value.
   */
  private void updatePropertiesFromMainSectionAttributeValues(Manifest mf)
  {
    if (null!=attributePropertyPrefix) {
      final Project   project      = getProject();
      final Manifest.Section mainS = mf.getMainSection();
      for (@SuppressWarnings("unchecked")
      final Enumeration<String> ae = mainS.getAttributeKeys(); ae.hasMoreElements();) {
        final String key = ae.nextElement();
        final Manifest.Attribute attr = mainS.getAttribute(key);
        // Ensure that the default case is used for OSGi specified attributes
        final String propKey = attributePropertyPrefix
          + (osgiAttrNamesMap.containsKey(key)
             ? osgiAttrNamesMap.get(key) : attr.getName() );
        final String propVal = attr.getValue();
        log("setting '" +propKey +"'='"+propVal+"'.", Project.MSG_VERBOSE);
        project.setProperty(propKey,propVal);
      }
    }
  }

  /**
   * Replace all main section attributes that starts with the
   * specified prefix with an attribute without that prefix,
   * overriding any old definition.
   * @param mf     The manifest to update
   * @param prefix The prefix to match on.
   */
  private void overrideAttributes(Manifest mf, String prefix)
  {
    if (null!=prefix && 0<prefix.length()) {
      final int       prefixLength = prefix.length();
      final Manifest.Section mainS = mf.getMainSection();
      final Vector<String> attrNames = new Vector<String>();
      for (@SuppressWarnings("unchecked")
      final Enumeration<String> ae = mainS.getAttributeKeys(); ae.hasMoreElements();) {
        final String key = ae.nextElement();
        final Manifest.Attribute attr = mainS.getAttribute(key);
        final String attrName = attr.getName();
        if (attrName.startsWith(prefix)) {
          attrNames.add(attrName);
        }
      }
      /* Must do the modification in a separate loop since it modifies
       * the object that the enumeration above iterates over. */
      for (final Object element : attrNames) {
        final String attrName = (String) element;
        final Manifest.Attribute attr = mainS.getAttribute(attrName);
        final String attrVal = attr.getValue();
        mainS.removeAttribute(attrName);
        final String newAttrName = attrName.substring(prefixLength);
        mainS.removeAttribute(newAttrName);
        if (!isPropertyValueEmpty(attrVal)) {
          try {
            final Manifest.Attribute newAttr
              = new Manifest.Attribute(newAttrName,attrVal);
            mainS.addConfiguredAttribute(newAttr);
            log("Overriding '" +newAttrName +"' with value of '"+attrName+"'.",
                Project.MSG_VERBOSE);
          } catch (final ManifestException me) {
            throw new BuildException("overriding of '" +newAttrName
                                     +"' failed: "+me,
                                     me, getLocation());
          }
        }
      }
    }
  }

  /**
   * Remove all kind specific main section attributes.
   *
   * @param mf     The manifest to remove main section attributes from.
   */
  private void removeAttributesForOtherKinds(Manifest mf)
  {
    if (allBundleKinds.size() > 0) {
      final Manifest.Section mainS = mf.getMainSection();

      // Find all attributes to remove.
      final Vector<String> attrNames = new Vector<String>();
      for (@SuppressWarnings("unchecked")
      final Enumeration<String> ae = mainS.getAttributeKeys(); ae
          .hasMoreElements();) {
        final String key = ae.nextElement();
        final Manifest.Attribute attr = mainS.getAttribute(key);
        final String attrName = attr.getName();
        final int prefixEnd = attrName.indexOf('-');
        if (prefixEnd > 0) {
          final String prefix = attrName.substring(0, prefixEnd);
          if (allBundleKinds.contains(prefix)) {
            attrNames.add(attrName);
          }
        }
      }
      // Remove the attributes; must be done outside the loop above since
      // removal modifies the collection that the loop iterates over.
      for (final String attrName : attrNames) {
        mainS.removeAttribute(attrName);
        log("Removing kind specific attribute '" + attrName + "'.",
            Project.MSG_VERBOSE);
      }
    }
  }

  /**
   * If the manifest contains a capability requirement on osgi.ee for the
   * OSGi/Minimum EE then replace the filter part of that requirement with the
   * given replacement value.
   *
   * @param mf
   *          The manifest to replace in.
   */
  private void replaceEEminmum(Manifest mf)
  {
    if (replaceEEmin != null) {
      final Manifest.Section mainS = mf.getMainSection();
      final Manifest.Attribute attr =
        mainS.getAttribute(Constants.REQUIRE_CAPABILITY);
      if (attr != null) {
        final String reqCapValue = attr.getValue();
        if (reqCapValue.contains(OSGI_MINIMUM_EE_NAME)) {
          log("Found '" + Constants.REQUIRE_CAPABILITY
              + "' attribute with requirement on '" + OSGI_MINIMUM_EE_NAME
              + "'.", Project.MSG_DEBUG);

          // Must replace...
          final List<HeaderEntry> hes =
            Util.parseManifestHeader(Constants.REQUIRE_CAPABILITY, reqCapValue,
                                     true, true, false);
          for (final HeaderEntry he : hes) {
            log("Processing header entry '" + he.getKey()
                    + "' with attributes " + he.getAttributes()
                    + " and directives " + he.getDirectives() + ".",
                Project.MSG_DEBUG);
            for (final Entry<String, String> directiveEntry : he
                .getDirectives().entrySet()) {
              if ("osgi.ee".equals(he.getKey())
                  && "filter".equals(directiveEntry.getKey())
                  && directiveEntry.getValue().contains(OSGI_MINIMUM_EE_NAME)) {
                log("Replacing filter '" + directiveEntry.getValue()
                        + "' with '" + replaceEEmin + "' in '" + reqCapValue
                        + "'.", Project.MSG_VERBOSE);
                directiveEntry.setValue(replaceEEmin);
                break;
              }
            }
          }
          attr.setValue(Util.toString(hes));
        }
      }
    }
  }

  /**
   * Add a section to the manifest.
   *
   * @param section the manifest section to be added.
   *
   * @exception ManifestException if the section is not valid.
   */
  public void addConfiguredSection(Manifest.Section section)
    throws ManifestException {
    manifestNested.addConfiguredSection(section);
  }

  /**
   * Special value used to indicate that a Manifest.Attribute with
   * this value shall be weeded out. I.e., not added to the manifest.
   * This value is used as the default value for properties that maps
   * to bundle manifest attributes via the attributePropertyPrefix.
   */
  static protected final String BUNDLE_EMPTY_STRING = "[bundle.emptystring]";

  /**
   * Check if a property value is empty or not.
   *
   * The value is empty if it is <code>null</code>, the empty string
   * or the special value BundleManifestTask.BUNDLE_EMPTY_STRING.
   *
   * @param pval The property value to check.
   * @return <code>true</code> if the value is empty.
   */
  static protected boolean isPropertyValueEmpty( String pval ) {
     return null==pval || "".equals(pval) || BUNDLE_EMPTY_STRING.equals(pval);
  }


  /**
   * Add an attribute to the main section of the manifest.
   * Attributes with the value BUNDLE_EMPTY_STRING are not added.
   *
   * @param attribute the attribute to be added.
   *
   * @exception ManifestException if the attribute is not valid.
   */
  public void addConfiguredAttribute(Manifest.Attribute attribute)
    throws ManifestException {
    if(BUNDLE_EMPTY_STRING.equals(attribute.getValue())) {
      return;
    }
    manifestNested.addConfiguredAttribute(attribute);
  }

  /**
   * Ensure that the named main section attribute ends with the
   * specified suffix.
   * @param mf       The manifest object to work with.
   * @param attrName The name of the attribute to check / update.
   * @param suffix   The required suffix.
   */
  private void ensureAttrEndsWith(final Manifest mf,
                                  final String attrName,
                                  final String suffix){
    final Manifest.Attribute attr = mf.getMainSection().getAttribute(attrName);
    if (null!=attr) {
      final String rhs = attr.getValue();
      if (!rhs.endsWith(suffix)) {
        attr.setValue( rhs +suffix );
      }
    }
  }

  /**
   * Ensure that the first value of the named main section attribute
   * ends with the specified suffix.
   *
   * @param mf       The manifest object to work with.
   * @param attrName The name of the attribute to check / update.
   * @param suffix   The required suffix.
   */
  private void ensureAttrFirstValueEndsWith(final Manifest mf,
                                            final String attrName,
                                            final String suffix)
  {
    final Manifest.Attribute attr = mf.getMainSection().getAttribute(attrName);
    if (null != attr) {
      final String rhs = attr.getValue();
      if (rhs != null && 0 < rhs.length()) {
        final int semiPos = rhs.indexOf(';');
        if (0 < semiPos) {
          // Found manifest attribute with parameter(s) or directive(s)
          final String firstValue = rhs.substring(0, semiPos).trim();
          if (!firstValue.endsWith(suffix)) {
            attr.setValue( firstValue + suffix + rhs.substring(semiPos));
          }
        } else {
          if (!rhs.endsWith(suffix)) {
            attr.setValue( rhs +suffix );
          }
        }
      }
    }
  }

  /**
   * Ensure that the named main section attribute have the given
   * value.
   * @param mf       The manifest object to work with.
   * @param attrName The name of the attribute to check / update.
   * @param value    The required attribute value.
   */
  private void ensureAttrValue(Manifest mf, String attrName, String value){
    Manifest.Attribute ma = mf.getMainSection().getAttribute(attrName);
    if (null==ma) {
      ma = new Manifest.Attribute(attrName,value);
      try {
        mf.getMainSection().addConfiguredAttribute(ma);
      } catch (final ManifestException me) {
        throw new BuildException("ensureAttrValue("+attrName+","
                                 +value +") failed.",
                                 me, getLocation());
      }
    } else {
      ma.setValue(value);
    }
  }


  private final static String DOC_URL_PREFIX
    = "http://www.knopflerfish.org/releases/current/";
  private final static String SVN_URL_PREFIX
    = "https://www.knopflerfish.org/svn/knopflerfish.org/trunk/";

  /**
   * If this is a distribution build (the
   * <code>Knopflerfish-Version</code> attribute is present) then use
   * the version number as replacement for:
   * <ul>
   *   <li>the <code>current</code>-part of a Bundle-DocURL
   *       value that start with {@link #DOC_URL_PREFIX}.
   *   <li>the <code>trunk</code>-part of a Bundle-SubversionURL
   *       value that start with {@link #SVN_URL_PREFIX}.
   * </ul>
   */
  private void replaceTrunkWithVersion(Manifest mf)
  {
    final Manifest.Attribute kfVerAttr
      = mf.getMainSection().getAttribute("Knopflerfish-Version");
    if (null!=kfVerAttr) {
      final String version = kfVerAttr.getValue();
      final boolean isSnapshot = -1<version.indexOf("snapshot");

      final String toReplace   = "/releases/current/";
      final String replacement = isSnapshot
        ? "/snapshots/" +version +"/"
        : ("/releases/" +version +"/");
      final Manifest.Attribute docAttr
        = mf.getMainSection().getAttribute("Bundle-DocURL");
      if (null!=docAttr) {
        final String docURL = docAttr.getValue();
        if (docURL.startsWith(DOC_URL_PREFIX)) {
          final int ix = DOC_URL_PREFIX.indexOf(toReplace);
          final String newDocURL
            = DOC_URL_PREFIX.substring(0,ix)
            +replacement +docURL.substring(ix + toReplace.length());
          docAttr.setValue(newDocURL);
        }
      }

      if (!isSnapshot) {
        final Manifest.Attribute svnAttr
          = mf.getMainSection().getAttribute("Bundle-SubversionURL");
        if (null!=svnAttr) {
          final String svnURL = svnAttr.getValue();
          if (svnURL.startsWith(SVN_URL_PREFIX)) {
            final String newSvnURL
              = SVN_URL_PREFIX.substring(0,SVN_URL_PREFIX.indexOf("trunk/"))
              +"tags/" +version +svnURL.substring(SVN_URL_PREFIX.length()-1);
            svnAttr.setValue(newSvnURL);
          }
        }
      }
    }
  }


  /**
   * Create or update the Manifest when used as a task.
   *
   * @throws BuildException if the manifest cannot be written.
   */
  @Override
  public void execute() throws BuildException {
    if (mode.getValue().equals("update") && manifestTemplateFile==null) {
      throw new BuildException("the template file attribute is required"
                               +" when mode is \"update\"");
    }
    if (mode.getValue().startsWith("template") && manifestTemplateFile==null) {
      throw new BuildException("the template file attribute is required"
                               +" when mode is \"" +mode.getValue() +"\"");
    }


    final Manifest manifestProps = new Manifest();
    if (!mode.getValue().equals("templateOnly")) {
      addAttributesFromProperties(manifestProps);
    }

    final Manifest manifestToWrite  = Manifest.getDefaultManifest();
    Manifest manifestTemplate = null;

    if (null!=manifestTemplateFile && manifestTemplateFile.exists()) {
      FileInputStream   is = null;
      InputStreamReader ir = null;
      try {
        is = new FileInputStream(manifestTemplateFile);
        if (encoding == null) {
          ir = new InputStreamReader(is, "UTF-8");
        } else {
          ir = new InputStreamReader(is, encoding);
        }
        manifestTemplate = new Manifest(ir);
      } catch (final ManifestException me) {
        throw new BuildException("Template manifest " + manifestTemplateFile
                                 + " is invalid", me, getLocation());
      } catch (final IOException ioe) {
        throw new BuildException("Failed to read " + manifestTemplateFile,
                                 ioe, getLocation());
      } finally {
        if (ir != null) {
          try {
            ir.close();
          } catch (final IOException e) {
            // ignore
          }
        }
      }
    }

    try {
      if (mode.getValue().equals("update")) {
        // Resulting manifest based on data from
        // template file + manifest properties + nested data
        if (manifestTemplate != null) {
          manifestToWrite.merge(manifestTemplate);
        }
        manifestToWrite.merge(manifestProps);
        manifestToWrite.merge(manifestNested);
        log("Creating bundle manifets based on '"
            +manifestTemplateFile
            +"' merged with data from"
            +" manifest properties and nested elements.",
            manifestFile==null ? Project.MSG_DEBUG: Project.MSG_VERBOSE);
      }
      if (mode.getValue().equals("replace")) {
        // Resulting manifest based on properties and nested data
        manifestToWrite.merge(manifestProps);
        manifestToWrite.merge(manifestNested);
        log("Creating bundle manifets based on data from"
            +" properties and nested elements.",
            manifestFile==null ? Project.MSG_DEBUG: Project.MSG_VERBOSE);
      }
      if (mode.getValue().startsWith("template")) {
        // Resulting manifest based on template and nested data.
        if (manifestTemplate != null) {
          manifestToWrite.merge(manifestTemplate);
        }
        if (!mode.getValue().equals("templateOnly")) {
          manifestToWrite.merge(manifestNested);
          log("Creating bundle manifets based on '" +manifestTemplateFile
              +"' and nested elements.",
              manifestFile==null ? Project.MSG_DEBUG: Project.MSG_VERBOSE);
        } else {
          log("Creating bundle manifets based on '" +manifestTemplateFile
              +"'.",
              manifestFile==null ? Project.MSG_DEBUG: Project.MSG_VERBOSE);
        }
      }
    } catch (final ManifestException me) {
      throw new BuildException("Manifest is invalid", me, getLocation());
    }

    if (null!=mainAttributesToSkip && 0<mainAttributesToSkip.length()) {
      final StringTokenizer st = new StringTokenizer(mainAttributesToSkip,",");
      while(st.hasMoreTokens()) {
        final String attrName = st.nextToken();
        log("Weeding out '"+attrName+"'.", Project.MSG_VERBOSE);
        manifestToWrite.getMainSection().removeAttribute(attrName);
      }
    }

    if (null!=bundleKind && 0<bundleKind.length()) {
      final String kindUC = bundleKind.toUpperCase();
      final String kindLC = bundleKind.toLowerCase();
      final String suffix = "-"+kindUC;
      ensureAttrEndsWith( manifestToWrite, "Bundle-Name", suffix );
      ensureAttrFirstValueEndsWith( manifestToWrite, "Bundle-SymbolicName",
                                    suffix );
      ensureAttrEndsWith( manifestToWrite, "Bundle-UUID", ":"+kindLC );
      ensureAttrEndsWith( manifestToWrite,
                          "Bundle-Description"," ("+kindUC+")" );
      if ("API".equals(kindUC)) {
        ensureAttrValue(manifestToWrite, "Bundle-Category", kindUC );
      }
      overrideAttributes(manifestToWrite, bundleKind+"-");
    }

    removeAttributesForOtherKinds(manifestToWrite);
    replaceEEminmum(manifestToWrite);
    replaceTrunkWithVersion(manifestToWrite);

    if (null==manifestFile) {
      updatePropertiesFromMainSectionAttributeValues(manifestToWrite);
    } else {
      PrintWriter pw = null;
      try {
        final FileOutputStream   os = new FileOutputStream(manifestFile);
        final OutputStreamWriter ow = new OutputStreamWriter(os, "UTF-8");
        pw = new PrintWriter(ow);
        manifestToWrite.write(pw);
        doVerbose(manifestToWrite);
      } catch (final IOException ioe) {
        throw new BuildException("Failed to write " + manifestFile,
                                 ioe, getLocation());
      } finally {
        if (pw != null) {
          pw.close();
        }
      }
    }
  }

}
