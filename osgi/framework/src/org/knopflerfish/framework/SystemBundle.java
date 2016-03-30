/*
 * Copyright (c) 2003-2016, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
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

package org.knopflerfish.framework;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.FrameworkDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.NativeNamespace;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.startlevel.dto.FrameworkStartLevelDTO;
import org.osgi.framework.wiring.FrameworkWiring;

/**
 * Implementation of the System Bundle object.
 *
 * @see org.osgi.framework.Bundle
 * @author Jan Stein
 * @author Philippe Laporte
 * @author Mats-Ola Persson
 */
public class SystemBundle extends BundleImpl implements Framework {

  /**
   * The file where we store the class path
   */
  private final static String BOOT_CLASSPATH_FILE = "boot_cp";

  /**
   * Export-Package string for system packages
   */
  private String exportPackageString;

  /**
   * Provide-Capability string for the system bundle.
   */
  private String provideCapabilityString;

  /**
   * The event to return to callers waiting in Framework.waitForStop() when the
   * framework has been stopped.
   */
  volatile private FrameworkEvent stopEvent = null;

  /**
   * The thread that performs shutdown of this framework instance.
   */
  private Thread shutdownThread = null;

  /**
   * Lock object
   */
  private final Object lock = new Object();

  /**
   * Marker that we need to restart JVM.
   */
  boolean bootClassPathHasChanged;

  private FrameworkWiringImpl fwWiring;


  /**
   * Construct the System Bundle handle.
   *
   */
  SystemBundle(FrameworkContext fw) {
    super(fw);
  }


  /**
   * Initialize this framework.
   *
   * @see org.osgi.framework.launch.Framework#init()
   */
  @Override
  public void init() throws BundleException {
    init(new FrameworkListener [0]);
  }

  /**
   * Initialize this framework and call listeners.
   *
   * @see org.osgi.framework.launch.Framework#init(FrameworkListener...)
   */
  @Override
  public void init(FrameworkListener... listeners) throws BundleException {
    secure.checkExecuteAdminPerm(this);

    synchronized (lock) {
      waitOnOperation(lock, "Framework.init", true);

      switch (state) {
      case INSTALLED:
      case RESOLVED:
        break;
      case STARTING:
      case ACTIVE:
        return;
      default:
        throw new IllegalStateException("INTERNAL ERROR, Illegal state, " + state);
      }
      doInit(listeners);
    }
  }


  /**
   * Start this framework.
   *
   * @see org.osgi.framework.Framework#start
   */
  @Override
  public void start(int options) throws BundleException {
    List<String> bundlesToStart = null;
    synchronized (lock) {
      waitOnOperation(lock, "Framework.start", true);

      switch (state) {
      case INSTALLED:
      case RESOLVED:
        doInit();
        // Fall through
      case STARTING:
        operation = ACTIVATING;
        break;
      case ACTIVE:
        return;
      default:
        throw new IllegalStateException("INTERNAL ERROR, Illegal state, " + state);
      }
      if (fwCtx.startLevelController == null) {
        bundlesToStart = fwCtx.storage.getStartOnLaunchBundles();
      }
    }

    if (fwCtx.startLevelController != null) {
      // start level open is delayed to this point to
      // correctly work at restart
      fwCtx.startLevelController.open();
    } else {
      // Start bundles according to their autostart setting.
      final Iterator<String> i = bundlesToStart.iterator();
      while (i.hasNext()) {
        final BundleImpl b = (BundleImpl)fwCtx.bundles.getBundle(i.next());
        try {
          final int autostartSetting = b.current().archive.getAutostartSetting();
          // Launch must not change the autostart setting of a bundle
          int option = Bundle.START_TRANSIENT;
          if (Bundle.START_ACTIVATION_POLICY == autostartSetting) {
            // Transient start according to the bundles activation policy.
            option |= Bundle.START_ACTIVATION_POLICY;
          }
          b.start(option);
        } catch (final BundleException be) {
          fwCtx.frameworkError(b, be);
        }
      }
    }
    synchronized (lock) {
      state = ACTIVE;
      operation = IDLE;
      lock.notifyAll();
      fwCtx.listeners.frameworkEvent(new FrameworkEvent(FrameworkEvent.STARTED, this, null));
    }
  }


  /**
   *
   */
  @Override
  public FrameworkEvent waitForStop(long timeout) throws InterruptedException {
    synchronized (lock) {
      // Already stopped?
      if (((INSTALLED | RESOLVED) & state) == 0) {
        stopEvent = null;
        while (true) {
          final long st = Util.timeMillis();
          try {
            lock.wait(timeout);
            if (stopEvent != null) {
              break;
            }
          } catch (final InterruptedException _ignore) {
          }
          if (timeout > 0) {
            timeout = timeout - (Util.timeMillis() - st);
            if (timeout <= 0) {
              break;
            }
          }
        }
        if (stopEvent == null) {
          return new FrameworkEvent(FrameworkEvent.WAIT_TIMEDOUT, this, null);
        }
      } else if (stopEvent == null) {
        // Return this if stop or update have not been called and framework is
        // stopped.
        stopEvent = new FrameworkEvent(FrameworkEvent.STOPPED, this, null);
      }
      return stopEvent;
    }
  }


  /**
   * Stop this framework.
   *
   * @see org.osgi.framework.Framework#stop
   */
  @Override
  public void stop(int options) throws BundleException {
    secure.checkExecuteAdminPerm(this);
    secure.callShutdown(this, false);
  }


  /**
   * Restart this framework.
   *
   * @see org.osgi.framework.Framework#update
   */
  @Override
  public void update(InputStream in) throws BundleException {
    secure.checkLifecycleAdminPerm(this);
    if (in != null) {
      try {
        in.close();
      } catch (final IOException ignore) {
      }
    }
    secure.callShutdown(this, true);
  }


  /**
   * Uninstall of framework are not allowed.
   *
   * @see org.osgi.framework.Framework#uninstall
   */
  @Override
  public void uninstall() throws BundleException {
    secure.checkLifecycleAdminPerm(this);
    throw new BundleException("Uninstall of System bundle is not allowed",
                              BundleException.INVALID_OPERATION);
  }


  /**
   * The system has all the permissions.
   *
   * @see org.osgi.framework.Bundle#hasPermission
   */
  @Override
  public boolean hasPermission(Object permission) {
    return true;
  }


  /**
   * Get header data.
   *
   * @see org.osgi.framework.Bundle#getHeaders
   */
  @Override
  public Dictionary<String, String> getHeaders() {
    return getHeaders(null);
  }


  /**
   * Get header data.
   *
   * @see org.osgi.framework.Bundle#getHeaders
   */
  @SuppressWarnings("deprecation")
  @Override
  public Dictionary<String, String> getHeaders(String locale) {
    secure.checkMetadataAdminPerm(this);
    final Hashtable<String, String> headers = new Hashtable<String, String>();
    headers.put(Constants.BUNDLE_SYMBOLICNAME, getSymbolicName());
    headers.put(Constants.BUNDLE_NAME, location);
    headers.put(Constants.EXPORT_PACKAGE, exportPackageString);
    headers.put(Constants.BUNDLE_VERSION, getVersion().toString());
    headers.put(Constants.BUNDLE_MANIFESTVERSION, "2");
    headers.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT,
        fwCtx.props.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT));
    headers.put("Bundle-Icon", "icon.png;size=32,icon64.png;size=64");
    headers.put(Constants.BUNDLE_VENDOR, "Knopflerfish");
    headers.put(Constants.BUNDLE_DESCRIPTION, "Knopflerfish System Bundle");
    headers.put(Constants.PROVIDE_CAPABILITY, provideCapabilityString);
    return headers;
  }


  /**
   * Get bundle data. Get resources from bundle or fragment jars.
   *
   * @see org.osgi.framework.Bundle#findEntries
   */
  @Override
  public Enumeration<URL> findEntries(String path,
                                      String filePattern,
                                      boolean recurse)
  {
    // TODO, What should system bundle return?
    return null;
  }

  /**
   *
   */
  @Override
  public URL getEntry(String name) {
    if (secure.okResourceAdminPerm(this)) {
      return getClassLoader().getResource(name);
    }
    return null;
  }


  /**
   *
   */
  @Override
  public Enumeration<String> getEntryPaths(String path) {
    return null;
  }

  // Don't delegate to BundleImp since Bundle adaptations may not
  // apply to the SystemBundle.
  @Override
  @SuppressWarnings("unchecked")
  public <A> A adapt(Class<A> type)
  {
    secure.checkAdaptPerm(this, type);
    Object res = null;
    if (FrameworkWiring.class.equals(type)) {
      res = fwWiring;
    } else if (FrameworkStartLevel.class.equals(type)) {
      if (fwCtx.startLevelController != null) {
        res = fwCtx.startLevelController.frameworkStartLevel();
      }
    } else if (Framework.class.equals(type)) {
      res = this;
    } else if (FrameworkStartLevelDTO.class.equals(type)) {
      if (fwCtx.startLevelController != null) {
        res = fwCtx.startLevelController.frameworkStartLevel().getDTO();
      }
    } else if (FrameworkDTO.class.equals(type)) {
      res = getFrameworkDTO();
    } else {
      // TODO filter which adaptation we can do?!
      res = adaptSecure(type);
    }
    return (A) res;
  }


  //
  // Package method
  //

  /**
   * Get class loader for this bundle.
   *
   * @return System Bundle classloader.
   */
  @Override
  ClassLoader getClassLoader() {
    return getClass().getClassLoader();
  }


  /**
   * Set system bundle state to stopping
   */
  void systemShuttingdown(final boolean restart) throws BundleException {
  }


  /**
   * Shutting down is done.
   */
  void systemShuttingdownDone(final FrameworkEvent fe) {
    synchronized (lock) {
      if (state != INSTALLED) {
        state = RESOLVED;
        operation = IDLE;
        lock.notifyAll();
      }
      stopEvent = fe;
    }
  }


  /**
   * Adds an bundle as an extension that will be included in the boot class path
   * on restart.
   * @throws BundleException Should not happened.
   */
  void attachExtension(BundleGeneration extension) throws BundleException  {
    if (extension.isBootClassPathExtension()) {
      // if we attach during startup, we assume that bundle is in BCP.
      if (getClassLoader() == null) {
        current().attachFragment(extension);
      } else {
        throw new UnsupportedOperationException(
            "Bootclasspath extension can not be dynamicly activated");
      }
    } else {
      try {
        addClassPathURL(new URL("file:" + extension.archive.getJarLocation()));
        current().attachFragment(extension);
        handleExtensionActivator(extension);
      } catch (final Exception e) {
        throw new UnsupportedOperationException(
            "Framework extension could not be dynamicly activated, " + e);
      }
    }
  }


  /**
   * Reads all localization entries that affects this bundle (including its
   * host/fragments)
   *
   * @param locale locale == "" the bundle.properties will be read o/w it will
   *          read the files as described in the spec.
   * @param localization_entries will append the new entries to this dictionary
   * @param baseName the basename for localization properties, <code>null</code>
   *          will choose OSGi default
   */
  void readLocalization(String locale,
                        Hashtable<String, String> localization_entries,
                        String baseName)
  {
    @SuppressWarnings("unchecked")
    final Vector<BundleGeneration> fragments = (Vector<BundleGeneration>)current().fragments.clone();
    if (fragments == null) {
      // NYI! read localization from framework.
      // There is no need for this now since it isn't used.
      return;
    }
    if (baseName == null) {
      baseName = Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
    }
    if (!locale.equals("")) {
      locale = "_" + locale;
    }
    while (true) {
      final String l = baseName + locale + ".properties";
      for (int i = fragments.size() - 1; i >= 0; i--) {
        final BundleGeneration bg = fragments.get(i);
        final Hashtable<String, String> tmp = bg.archive.getLocalizationEntries(l);
        if (tmp != null) {
          localization_entries.putAll(tmp);
          return;
        }
      }
      final int pos = locale.lastIndexOf('_');
      if (pos == -1) {
        break;
      }
      locale = locale.substring(0, pos);
    }
  }


  /**
   *
   */
  void initSystemBundle() {
    bundleContext = new BundleContextImpl(this);
    final StringBuffer sp = new StringBuffer(
        fwCtx.props.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES));
    if (sp.length() == 0) {
      // Try the system packages file
      addSysPackagesFromFile(sp, fwCtx.props.getProperty(FWProps.SYSTEM_PACKAGES_FILE_PROP), null);
      if (sp.length() == 0) {
        // Try the system packages base property.
        sp.append(fwCtx.props.getProperty(FWProps.SYSTEM_PACKAGES_BASE_PROP));

        if (sp.length() == 0) {
          if (FWProps.androidApiLevel >= 0) {
            addSysPackagesFromFile(sp, "android_packages.txt", new Version(FWProps.androidApiLevel, 0, 0));
          } else {
            // use default set of packages.
            String jver = fwCtx.props.getProperty(FWProps.SYSTEM_PACKAGES_VERSION_PROP);

            Version jv;
            if (jver == null) {
              jv = new Version(FWProps.javaVersionMajor, FWProps.javaVersionMinor, 0);
            } else {
              try {
                jv = new Version(jver);
              } catch (IllegalArgumentException _ignore){
                if (fwCtx.debug.framework) {
                  fwCtx.debug.println("No built in list of Java packages to be exported "
                      + "by the system bundle for JRE with version '" + jver
                      + "', using the list for 1.7.");
                }
                jv = new Version(1,7,0);
              }
            }
            addSysPackagesFromFile(sp, "packages.txt", jv);
          }
        } else {
          if (sp.charAt(sp.length() - 1) == ',') {
            sp.deleteCharAt(sp.length() - 1);
          }
        }
        addSysPackagesFromFile(sp, "exports", null);
      }
    }
    final String extraPkgs = fwCtx.props.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
    if (extraPkgs.length() > 0) {
      sp.append(",").append(extraPkgs);
    }
    exportPackageString = sp.toString();

    sp.setLength(0);
    sp.append(fwCtx.props.getProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES));
    if (sp.length()==0) {
      // Derive osgi.ee capabilities from the EE header.
      addSysCapabilitiesFromEE(sp);
    }
    // Add in extra system capabilities
    final String epc = fwCtx.props.getProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA);
    if (epc.length()>0) {
      if (sp.length()>0) {
        sp.append(',');
      }
      sp.append(epc);
    }
    provideCapabilityString = sp.toString();

    final BundleGeneration gen = new BundleGeneration(this, exportPackageString,
                                                      provideCapabilityString);
    generations.add(gen);
    gen.bpkgs.registerPackages();
    try {
      gen.bpkgs.resolvePackages(null);
    } catch (final BundleException _ignore) {
      // Shouldn't happend, hooks not active;
    }
    gen.setWired();
    fwWiring = new FrameworkWiringImpl(fwCtx);
  }


  /**
   *
   */
  void uninitSystemBundle() {
    bundleContext.invalidate();
    bundleContext = null;
    if (!bootClassPathHasChanged) {
      for (final BundleGeneration bg : fwCtx.bundles.getFragmentBundles(current())) {
        if (bg.isBootClassPathExtension() && bg.bundle.extensionNeedsRestart()) {
          bootClassPathHasChanged = true;
          break;
        }
      }
    }
    exportPackageString = null;
    provideCapabilityString = null;
    generations.clear();
    fwWiring = null;
  }


  BundleCapabilityImpl getNativeCapability() {
    return current().getDeclaredCapabilities().get(NativeNamespace.NATIVE_NAMESPACE).get(0);
  }

  //
  // Private methods
  //

  /**
   *
   */
  private void doInit(FrameworkListener... listeners) throws BundleException {
    state = STARTING;
    bootClassPathHasChanged = false;
    fwCtx.init(listeners);
  }


  /**
   * Read a file with package names and add them to a stringbuffer. The file is
   * searched for in the current working directory, then on the class path.
   * Each line can have a java version guard at the end with format <tt>!VersionRange</tt>.
   *
   * @param sp Buffer to append the exports to. Same format as the
   *          Export-Package manifest header.
   * @param sysPkgFile Name of the file to load packages to be exported from.
   * @param guard Version to test version guarded lines against.
   */
  private void addSysPackagesFromFile(StringBuffer sp, String sysPkgFile, Version guard) {
    if (null == sysPkgFile || 0 == sysPkgFile.length()) {
      return;
    }

    if (fwCtx.debug.resolver) {
      fwCtx.debug.println("Will add system packages from file " + sysPkgFile);
    }

    URL url = null;
    final File f = new File(new File(sysPkgFile).getAbsolutePath());

    if (!f.exists() || !f.isFile()) {
      url = SystemBundle.class.getResource(sysPkgFile);
      if (null == url) {
        url = SystemBundle.class.getResource("/" + sysPkgFile);
      }
      if (null == url) {
        if (fwCtx.debug.resolver) {
          fwCtx.debug.println("Could not add system bundle package exports from '" + sysPkgFile
              + "', file not found.");
        }
      }
    }
    BufferedReader in = null;
    try {
      Reader reader = null;
      String source = null;

      if (null == url) {
        reader = new FileReader(f);
        source = f.getAbsolutePath().toString();
      } else {
        reader = new InputStreamReader(url.openStream());
        source = url.toString();
      }
      in = new BufferedReader(reader);
      if (fwCtx.debug.resolver) {
        fwCtx.debug.println("\treading from " + source);
      }

      String line;
      for (line = in.readLine(); line != null; line = in.readLine()) {
        line = line.trim();
        if (line.length() > 0 && !line.startsWith("#")) {
          int idx = line.lastIndexOf('!');
          if (idx != -1) {
            try {
              if (new VersionRange(line.substring(idx +1)).includes(guard)) {
                line = line.substring(0, idx);
              } else {
                // Not in version range skip.
                continue;
              }
            } catch (IllegalArgumentException _ignore) { }
          }
          if (sp.length() > 0) {
            sp.append(",");
          }
          sp.append(line);
        }
      }
    } catch (final IOException e) {
      throw new IllegalArgumentException("Failed to read " + sysPkgFile + ": " + e);
    } finally {
      try {
        in.close();
      } catch (final Exception _ignore) {
      }
    }
  }


  /**
   * Create bundle capabilities in the {@code osgi.ee} name-space for all
   * execution environments that the framework supports.
   *
   * This method converts all BREEs defined using the framework environment
   * property {@code org.osgi.framework.executionenvironment} into
   * {@code osgi.ee}-capabilities.
   *
   * <p>A Bundle Required Execution Environment often on a from that matches:
   * <pre>
   * bree'  ::= n1 ( '-' v )? ( '/' n2 ( '-' v )? )?
   * </pre>
   * If it matches the BRWW will be transformed to an osgi.ee
   * capability with attributes as below:
   * <pre>
   *   osgi.ee = n1 ('/' n2 ) ; version:List&lt;Version&gt; = "v"
   *   osgi.ee = &lt;ee name&gt;
   * </pre>
   * If it does not match the {@code osgi.ee} attribute is set to the original
   * BREE value.
   *
   * BREE definitions for Java standard edition uses different names, "J2SE" and
   * "JavaSE" depending on the Java major version, but in
   * {@code osgi.ee}-capabilities the name shall be "JavaSE" independent of Java
   * release.
   *
   * @param sp string buffer with all framework provided capabilities to append
   * to.
   */
  private void addSysCapabilitiesFromEE(StringBuffer sp)
  {
    final Map<String, SortedSet<Version>> eeNameVersions
      = new HashMap<String, SortedSet<Version>>();
    @SuppressWarnings("deprecation")
    final String fwEE = fwCtx.props
        .getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);

    if (fwEE!=null && fwEE.length()>0) {
      final String[] ees = Util.splitwords(fwEE, ",");
      for(final String ee : ees) {
        final String[] n1n2 = Util.splitwords(ee, "/");
        switch (n1n2.length) {
        case 1:
          final String[] nv = Util.splitwords(ee, "-");
          if (nv.length==2) {
            try {
              final Version v = new Version(nv[1]);
              addSysCapabilityEE(eeNameVersions, nv[0], v);
            } catch (final Exception e) {
              addSysCapabilityEE(eeNameVersions, ee, null);
            }
          } else {
            addSysCapabilityEE(eeNameVersions, ee, null);
          }
          break;
        case 2:
          final String[] n1v = Util.splitwords(n1n2[0], "-");
          final String[] n2v = Util.splitwords(n1n2[1], "-");
          try {
            final Version v1 = n1v.length==2 ? new Version(n1v[1]) : null;
            final Version v2 = n2v.length==2 ? new Version(n2v[1]) : null;
            final String n = n1v[0] +"/" +n2v[0];
            if (v1 != null && v2 != null && v1.equals(v2)) {
              addSysCapabilityEE(eeNameVersions, n, v1);
            } else if (v1!=null && v2==null) {
              addSysCapabilityEE(eeNameVersions, n, v1);
            } else if (v1==null && v2!=null) {
              addSysCapabilityEE(eeNameVersions, n, v2);
            } else {
              addSysCapabilityEE(eeNameVersions, ee, null);
            }
          } catch (final Exception e) {
            addSysCapabilityEE(eeNameVersions, ee, null);
          }
          break;
        default:
          addSysCapabilityEE(eeNameVersions, ee, null);
        }
      }
    }
    addSysCapabilityForEE(sp, eeNameVersions);
  }


  private void addSysCapabilityEE(final Map<String, SortedSet<Version>> eeNameVersions,
                                  String name,
                                  final Version v)
  {
    if ("J2SE".equals(name)) {
      name = "JavaSE";
    }
    SortedSet<Version> versions = eeNameVersions.get(name);
    if (versions==null) {
      versions = new TreeSet<Version>();
      eeNameVersions.put(name, versions);
    }
    if (v !=null) {
      versions.add(v);
    }
  }


  private void addSysCapabilityForEE(final StringBuffer sb,
                                     final Map<String, SortedSet<Version>> eeNameVersions)
  {
    for (final Entry<String, SortedSet<Version>> entry : eeNameVersions
        .entrySet()) {
      if (sb.length() > 0) {
        sb.append(',');
      }
      sb.append(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
      sb.append(';');
      sb.append(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
      sb.append('=');
      sb.append(entry.getKey());
      if (!entry.getValue().isEmpty()) {
        sb.append(";version:List<Version>=\"");
        for (final Version v : entry.getValue()) {
          sb.append(v.toString());
          sb.append(',');
        }
        sb.setCharAt(sb.length() - 1, '"');
      }
    }
  }

  /**
   * This method start a thread that stop this Framework, stopping all started
   * bundles.
   *
   * <p>
   * If the framework is not started, this method does nothing. If the framework
   * is started, this method will:
   * <ol>
   * <li>Set the state of the FrameworkContext to <i>inactive</i>.</li>
   * <li>Suspended all started bundles as described in the
   * {@link Bundle#stop(int)} method except that the persistent state of the
   * bundle will continue to be started. Reports any exceptions that occur
   * during stopping using <code>FrameworkErrorEvents</code>.</li>
   * <li>Disable event handling.</li>
   * </ol>
   * </p>
   *
   */
  void shutdown(final boolean restart) {
    synchronized (lock) {
      boolean wasActive = false;
      switch (state) {
      case Bundle.INSTALLED:
      case Bundle.RESOLVED:
        shutdownDone(false);
        break;
      case Bundle.ACTIVE:
        wasActive = true;
        // Fall through
      case Bundle.STARTING:
        if (shutdownThread == null) {
          try {
            final boolean wa = wasActive;
            shutdownThread = new Thread(fwCtx.threadGroup, "Framework shutdown") {
              @Override
              public void run() {
                shutdown0(restart, wa);
              }
            };
            shutdownThread.setDaemon(false);
            shutdownThread.start();
          } catch (final Exception e) {
            systemShuttingdownDone(new FrameworkEvent(FrameworkEvent.ERROR, this, e));
          }
        }
        break;
      case Bundle.STOPPING:
        // Shutdown already inprogress
        break;
      }
    }
  }


  /**
   * Stop this FrameworkContext, suspending all started contexts. This method
   * suspends all started contexts so that they can be automatically restarted
   * when this FrameworkContext is next launched.
   *
   * <p>
   * If the framework is not started, this method does nothing. If the framework
   * is started, this method will:
   * <ol>
   * <li>Set the state of the FrameworkContext to <i>inactive</i>.</li>
   * <li>Stop all started bundles as described in the {@link Bundle#stop(int)}
   * method except that the persistent state of the bundle will continue to be
   * started. Reports any exceptions that occur during stopping using
   * <code>FrameworkErrorEvents</code>.</li>
   * <li>Disable event handling.</li>
   * </ol>
   * </p>
   *
   */
  private void shutdown0(final boolean restart, final boolean wasActive) {
    try {
      synchronized (lock) {
        waitOnOperation(lock, "Framework." + (restart ? "update" : "stop"), true);
        operation = DEACTIVATING;
        state = STOPPING;
      }
      fwCtx.listeners.bundleChanged(new BundleEvent(BundleEvent.STOPPING, this));
      if (wasActive) {
        stopAllBundles();
        saveClasspaths();
      }
      synchronized (lock) {
        fwCtx.uninit();
        shutdownThread = null;
        shutdownDone(restart);
      }
      if (restart) {
        if (wasActive) {
          start();
        } else {
          init();
        }
      }
    } catch (final Exception e) {
      shutdownThread = null;
      systemShuttingdownDone(new FrameworkEvent(FrameworkEvent.ERROR, this, e));
    }

  }


  /**
   * Tell system bundle shutdown finished.
   */
  private void shutdownDone(boolean restart) {
    int t;
    if (bootClassPathHasChanged) {
      t = FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED;
    } else {
      t = restart ? FrameworkEvent.STOPPED_UPDATE : FrameworkEvent.STOPPED;
    }
    systemShuttingdownDone(new FrameworkEvent(t, this, null));
  }


  /**
   * Stop and unresolve all bundles.
   */
  private void stopAllBundles() {
    if (fwCtx.startLevelController != null) {
      fwCtx.startLevelController.shutdown();
    }

    // Stop all active bundles, in reverse bundle ID order
    // The list will be empty when the start level service is in use.
    final List<BundleImpl> activeBundles = fwCtx.bundles.getActiveBundles();
    for (int i = activeBundles.size() - 1; i >= 0; i--) {
      final BundleImpl b = activeBundles.get(i);
      try {
        if (((Bundle.ACTIVE | Bundle.STARTING) & b.getState()) != 0) {
          // Stop bundle without changing its autostart setting.
          b.stop(Bundle.STOP_TRANSIENT);
        }
      } catch (final BundleException be) {
        fwCtx.frameworkError(b, be);
      }
    }

    final List<BundleImpl> allBundles = fwCtx.bundles.getBundles();

    // Set state to INSTALLED and purge any unrefreshed bundles
    for (final BundleImpl bundleImpl : allBundles) {
      final BundleImpl b = bundleImpl;
      if (b.getBundleId() != 0) {
        b.setStateInstalled(false);
        b.purge();
      }
    }
  }


  private void saveClasspaths() {
    final StringBuffer bootClasspath = new StringBuffer();
    for (final BundleGeneration bundleGeneration : fwCtx.bundles.getFragmentBundles(current())) {
      final BundleGeneration ebg = bundleGeneration;
      final String path = ebg.archive.getJarLocation();
      if (ebg.isBootClassPathExtension()) {
        if (bootClasspath.length() > 0) {
          bootClasspath.append(File.pathSeparator);
        }
        bootClasspath.append(path);
      }
    }

    // Post processing to handle boot class extension
    try {
      final File bcpf = new File(Util.getFrameworkDir(fwCtx), BOOT_CLASSPATH_FILE);
      if (bootClasspath.length() > 0) {
        saveStringBuffer(bcpf, bootClasspath);
      } else {
        bcpf.delete();
      }
    } catch (final IOException e) {
      if (fwCtx.debug.errors) {
        fwCtx.debug.println("Could not save classpath " + e);
      }
    }
  }


  private void saveStringBuffer(File f, StringBuffer content) throws IOException {
    PrintStream out = null;
    try {
      out = new PrintStream(new FileOutputStream(f));
      out.println(content.toString());
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }


  private void addClassPathURL(URL url) throws Exception {
    final ClassLoader cl = getClassLoader();
    Method m = Util.getMethod(cl.getClass(), "addURL", new Class[] { URL.class });
    if (m != null) {
      m.invoke(cl, new Object[] { url });
    } else {
      throw new NoSuchMethodException("addURL");
    }
  }


  /**
   * If the extension has an extension activator header process it.
   *
   * @param extension the extension bundle to process.
   */
  private void handleExtensionActivator(final BundleGeneration extension) {
    String extActivatorName =
      extension.archive.getAttribute("Extension-Activator");
    extActivatorName = null!=extActivatorName ? extActivatorName.trim() : null;

    if (null!=extActivatorName && extActivatorName.length()>0) {
      fwCtx.log("Activating extension: " + extension.symbolicName
                + ":" +extension.version + " using: " +extActivatorName);
      fwCtx.activateExtension(extension);
    }
  }


  private FrameworkDTO getFrameworkDTO() {
    FrameworkDTO res = new FrameworkDTO();
    res.bundles = new ArrayList<BundleDTO>();
    for (BundleImpl bi : fwCtx.bundles.getBundles()) {
      res.bundles.add(bi.getDTO());
    }
    res.properties = fwCtx.props.getFWProperties();
    res.services = new ArrayList<ServiceReferenceDTO>();
    for (ServiceRegistrationImpl sri : fwCtx.services.getAllRegistered()) {
      ServiceReferenceDTO srdto = sri.getDTO();
      if (srdto != null) {
        res.services.add(srdto);
      }
    }
    return res;
  }

}
