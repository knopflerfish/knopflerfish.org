/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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

/**
 * @author Erik Wistrand
 * @author Philippe Laporte
 */

package org.knopflerfish.util.metatype;

import java.net.URL;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.osgi.util.tracker.ServiceTracker;

import org.knopflerfish.service.log.LogRef;

/**
 * Class which monitors installed bundles for metatype and CM default data.
 *
 * <p>
 * When instantiated, SystemMetatypeProvider will listen for installed bundles
 * and try to extract metatype and CM defaults XML from the bundle jar files.
 * This data will then be available using the <tt>getServicePIDs</tt>,
 * <tt>getFactoryPIDs</tt> and <tt>getObjectClassDefinition</tt> methods.
 * </p>
 */
public class SystemMetatypeProvider
  extends MTP
  implements MetaTypeService
{

  /**
   * Default URL to metatype XML.
   *
   * <p>
   * Value is "!/metatype.xml"
   * </p>
   */
  public static final String METATYPE_RESOURCE = "/metatype.xml";

  /**
   * Default URL to default CM values.
   *
   * <p>
   * Value is "!/cmdefaults.xml"
   * </p>
   */
  public static final String CMDEFAULTS_RESOURCE = "/cmdefaults.xml";

  /**
   * Manifest attribute name specifying metatype XML URL.
   *
   * <p>
   * Value is "Bundle-MetatypeURL"
   * </p>
   */
  public static final String ATTRIB_METATYPEURL = "Bundle-MetatypeURL";

  /**
   * Manifest attribute name specifying CM defaults XML URL.
   *
   * <p>
   * Value is "Bundle-CMDefaultsURL"
   * </p>
   */
  public static final String ATTRIB_CMDEFAULTSURL = "Bundle-CMDefaultsURL";

  final BundleContext bc;
  final LogRef log;

  final Map<Bundle, MetaTypeInformation> providers =
    new Hashtable<Bundle, MetaTypeInformation>();

  ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> cmTracker;

  // Special MTP which tracks CM configuration instances
  MTP cmMTP;

  // Map from PID to OCD
  Map<String, OCD> cmOCDMap = new HashMap<String, OCD>();

  /**
   * Create a SystemMetatypeProvider, using the specified bundle context for
   * listeners.
   */
  public SystemMetatypeProvider(BundleContext bc)
  {
    super("system");
    this.bc = bc;
    log = new LogRef(bc);
  }

  SynchronousBundleListener bl = null;

  /**
   * Start listening for bundles.
   */
  public void open()
  {
    if (bl != null) {
      return;
    }

    bl = new SynchronousBundleListener() {
      public void bundleChanged(BundleEvent ev)
      {
        switch (ev.getType()) {
        case BundleEvent.INSTALLED:
        case BundleEvent.RESOLVED:
        case BundleEvent.UNRESOLVED:
        case BundleEvent.UPDATED:
          // NYI! Reduce the number of loadMTPs by combining U* events.
          // We can't read properties from the system bundle
          if (ev.getBundle().getBundleId() != 0) {
            try {
              loadMTP(ev.getBundle());
            } catch (final Exception e) {
              log.error("Failed to handle bundle "
                        + ev.getBundle().getBundleId(), e);
              // e.printStackTrace(System.out);
            }
          }
          break;
        case BundleEvent.UNINSTALLED:
          final Bundle b = ev.getBundle();
          if (b.getBundleId() != 0) {
            providers.remove(b);
          }
          break;
        }
      }
    };

    bc.addBundleListener(bl);
    // Fake events to catch up...
    final Bundle[] bundles = bc.getBundles();
    for (final Bundle bundle : bundles) {
      bl.bundleChanged(new BundleEvent(BundleEvent.INSTALLED, bundle));
    }

    cmTracker =
      new ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>(
                                                                 bc,
                                                                 ConfigurationAdmin.class,
                                                                 null);
    cmTracker.open();

    // track CM configuration instances as MTPs on the CM bundle.
    cmMTP = new MTP("[CM]") {
      @Override
      public String[] getPids()
      {
        return MTP.toStringArray(getCMServicePIDs());
      }

      @Override
      public String[] getFactoryPids()
      {
        return MTP.toStringArray(getCMFactoryPIDs());
      }

      @Override
      public String[] getLocales()
      {
        return null;
      }

      @Override
      public ObjectClassDefinition getObjectClassDefinition(String pid,
                                                            String locale)
      {
        OCD ocd = cmOCDMap.get(pid);
        if (ocd != null) {
          // cached
          return ocd;
        }

        final ConfigurationAdmin ca = cmTracker.getService();
        if (ca != null) {
          try {
            final Configuration[] configs = ca.listConfigurations(null);
            Configuration conf = null;
            for (int i = 0; configs != null && i < configs.length; i++) {
              if (pid.equals(configs[i].getPid())
                  || pid.equals(configs[i].getFactoryPid())) {
                conf = configs[i];
              }
            }
            if (conf != null) {
              final Dictionary<String, ?> props = conf.getProperties();
              ocd = new OCD(pid, pid, pid + " from CM", props);
              cmOCDMap.put(pid, ocd);
              return ocd;
            } else {
              throw new RuntimeException("No config for pid " + pid);
            }
          } catch (final Exception e) {
            log.error("Failed to get service pid " + pid, e);
            return null;
          }
        } else {
          log.warn("Failed to get CA when getting pid " + pid);
          return null;
        }
      }
    };
  }

  /**
   * Stop listening for bundles.
   */
  public void close()
  {
    if (cmTracker != null) {
      cmTracker.close();
      cmTracker = null;
    }
    if (bl != null) {
      bc.removeBundleListener(bl);
    }
    bl = null;
  }

  /**
   * Explicitly load a metatype provider from a bundle and cache it for later
   * retrieval by {@link #getMTP(Bundle)}.
   *
   * @throws Exception
   *           if loading fails
   */
  public void loadMTP(Bundle b)
      throws Exception
  {
    URL url;

    // try R4 first
    Enumeration<URL> metaTypeFiles;
    if (b.getState() == Bundle.INSTALLED) {
      final Enumeration<String> p =
        b.getEntryPaths(MetaTypeService.METATYPE_DOCUMENTS_LOCATION);
      if (p != null) {
        final Vector<URL> tmp = new Vector<URL>();
        while (p.hasMoreElements()) {
          tmp.addElement(b.getEntry(p.nextElement()));
        }
        metaTypeFiles = tmp.elements();
      } else {
        metaTypeFiles = null;
      }
    } else {
      metaTypeFiles =
        b.findEntries(MetaTypeService.METATYPE_DOCUMENTS_LOCATION, "*", false);
    }
    if (metaTypeFiles != null) {
      final BundleMetaTypeResource bmtr = new BundleMetaTypeResource(b);

      while (metaTypeFiles.hasMoreElements()) {
        url = metaTypeFiles.nextElement();
        bmtr.mergeWith(Loader.loadBMTIfromUrl(bc, b, url));
      }

      bmtr.prepare();

      providers.put(b, bmtr);
    } else {
      // proprietary legacy

      MTP mtp = null;

      String defStr = b.getHeaders().get(ATTRIB_METATYPEURL);

      if (defStr == null || "".equals(defStr)) {
        defStr = METATYPE_RESOURCE;
      }

      if (defStr.startsWith("!")) {
        url = b.getEntry(defStr.substring(1));
      } else if (defStr.startsWith("/")) {
        url = b.getEntry(defStr);
      } else {
        url = new URL(defStr);
      }

      if (url != null) {
        try {
          mtp = Loader.loadMTPFromURL(b, url);
          providers.put(b, mtp);
        } catch (final Exception e) {
          log.info("Failed to load metatype XML from bundle " + b.getBundleId(),
                   e);
          // throw e;
        }
      }

      // defaults are specified in the file itself in R4

      String valStr = b.getHeaders().get(ATTRIB_CMDEFAULTSURL);

      if (valStr == null || "".equals(valStr)) {
        valStr = CMDEFAULTS_RESOURCE;
      }

      if (valStr.startsWith("!")) {
        url = b.getEntry(valStr.substring(1));
      } else if (valStr.startsWith("/")) {
        url = b.getEntry(valStr);
      } else {
        url = new URL(valStr);
      }

      if (url != null) {
        try {
          Loader.loadDefaultsFromURL(mtp, url);
          log.info("Bundle " + b.getBundleId() + ": loaded default values");
        } catch (final Exception e) {
          log.info("Failed to load cm defaults XML from bundle "
                       + b.getBundleId(), e);
          // throw e;
        }
      }
    } // proprietary legacy

  }

  @Override
  public String[] getPids()
  {
    synchronized (providers) {
      final Set<String> set = new TreeSet<String>();
      for (final Entry<Bundle, MetaTypeInformation> entry : providers.entrySet()) {
        final MetaTypeInformation mti = entry.getValue();
        final String[] pids = mti.getPids();
        if (pids!=null) {
          set.addAll(Arrays.asList(pids));
        }
      }
      return MTP.toStringArray(set);
    }
  }

  @Override
  public String[] getFactoryPids()
  {
    synchronized (providers) {
      final Set<String> set = new TreeSet<String>();
      for (final Entry<Bundle, MetaTypeInformation> entry : providers.entrySet()) {
        final MetaTypeInformation mti = entry.getValue();
        final String[] fpids = mti.getFactoryPids();
        if (fpids!=null) {
          set.addAll(Arrays.asList(fpids));
        }
      }
      return MTP.toStringArray(set);
    }
  }

  @Override
  public String[] getLocales()
  {
    return null;
  }

  /**
   * Get a loaded metatype provider, given a bundle.
   *
   * @return Provider if such provider is found, otherwise <tt>null</tt>.
   */
  public MetaTypeInformation getMTP(Bundle b)
  {
    final ServiceReference<?> cmSR = cmTracker.getServiceReference();

    MetaTypeInformation mti = null;

    if (cmSR != null && cmSR.getBundle() == b) {
      mti = cmMTP;
    } else if (b.getBundleId() == 0) {
      mti = this;
    } else {
      mti = providers.get(b);
    }

    return mti;
  }

  /**
   * Get an ObjectClassDefinition given a PID.
   *
   * @return ObjectClassDefinition if PID exists, otherwise <tt>null</tt>.
   */
  @Override
  public ObjectClassDefinition getObjectClassDefinition(String pid,
                                                        String locale)
  {
    synchronized (providers) {
      for (final Entry<Bundle, MetaTypeInformation> entry : providers.entrySet()) {
        final MetaTypeInformation mti = entry.getValue();
        final String[] pids = mti.getPids();
        if (pids !=null && Arrays.asList(pids).contains(pid)) {
          return mti.getObjectClassDefinition(pid, locale);
        }
        final String[] fpids = mti.getFactoryPids();
        if (fpids !=null && Arrays.asList(fpids).contains(pid)) {
          return mti.getObjectClassDefinition(pid, locale);
        }
      }
      return null;
    }
  }

  Set<String> getCMServicePIDs()
  {
    final Set<String> pids = new HashSet<String>();
    final ConfigurationAdmin ca = cmTracker.getService();
    if (ca != null) {
      try {
        final Configuration[] configs = ca.listConfigurations("(service.pid=*)");
        for (int i = 0; configs != null && i < configs.length; i++) {
          if (configs[i].getFactoryPid() == null) {
            pids.add(configs[i].getPid());
          }
        }
      } catch (final Exception e) {
        log.error("Failed to get service pids", e);
      }
    }
    return pids;
  }

  Set<String> getCMFactoryPIDs()
  {
    final Set<String> pids = new HashSet<String>();
    final ConfigurationAdmin ca = cmTracker.getService();
    if (ca != null) {
      try {
        final Configuration[] configs = ca.listConfigurations("(service.pid=*)");
        for (int i = 0; configs != null && i < configs.length; i++) {
          if (configs[i].getFactoryPid() != null) {
            pids.add(configs[i].getFactoryPid());
          }
        }
      } catch (final Exception e) {
        log.error("Failed to get service pids", e);
      }
    }
    return pids;
  }

  public MetaTypeInformation getMetaTypeInformation(Bundle bundle)
  {

    MetaTypeInformation mti;

    mti = providers.get(bundle);
    if (mti != null) {
      return mti;
    }

    return BundleMetaTypeInformationSnapshot.extractMetatypeInformation(bc,
                                                                        bundle);
  }
}
