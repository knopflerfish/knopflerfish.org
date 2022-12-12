/*
 * Copyright (c) 2004-2022, KNOPFLERFISH project
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

package org.knopflerfish.bundle.dirdeployer;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.service.cm.ManagedService;

/**
 * Configuration class for the directory deployer.
 *
 * This class first initializes itself with values from system properties. If it
 * is registered as a <tt>ManagedService</tt>, it also handles calls to the
 * <tt>update</tt> method. The <tt>register</tt> method does this registration.
 */
public class Config
  implements ManagedService
{

  // PID string used for service.pid
  static final String PID = "org.knopflerfish.fileinstall";

  // Property names used both as system properties and as CM properties
  static final String PROP_DIRS = PID + ".dir";
  static final String PROP_INTERVAL = PID + ".poll";
  static final String PROP_USE_INITIAL_START_LEVEL = PID + ".use.initial.startlevel";
  static final String PROP_STARTLEVEL = PID + ".startlevel";
  static final String PROP_UNINSTALL = PID + ".uninstallOnStop";
  static final String PROP_USE_FILE_MARKERS = PID + ".filemarkers.use";
  static final String PROP_REGISTER_CONTROLS = PID + ".bundlecontrols.use";

  static final String DEFAULT_DIR = "./load";
  
  static final boolean USE_FILE_MARKERS_DEFAULT = Boolean.FALSE;
  boolean useFileMarkers = USE_FILE_MARKERS_DEFAULT;
  
  static final boolean REGISTER_BUNDLE_CONTROLS_DEFAULT = Boolean.FALSE;
  boolean registerBundleControls = REGISTER_BUNDLE_CONTROLS_DEFAULT;
  

  // directories to scan
  File[] dirs = new File[0];

  // sleep interval for scan thread
  long interval = 1000;

  // if true, uninstall all bundles when scan thread stops
  boolean uninstallOnStop = true;

  // if the initial start level is enough or not.
  boolean useInitialStartLevel = true;

  // start level for installed bundles
  int startLevel = 1;

  // framework registration
  ServiceRegistration<ManagedService> reg;

  public Config()
  {
    // init with default values
    updated(getDefaults());
  }

  void register()
  {
    if (reg != null) {
      return;
    }

    final Dictionary<String, Object> props = new Hashtable<>();
    props.put("service.pid", PID);

    reg = Activator.bc.registerService(ManagedService.class, this, props);

  }

  void unregister()
  {
    if (reg == null) {
      return;
    }

    reg.unregister();
    reg = null;
  }

  public void updated(Dictionary<String,?> props)
  {

    if (props == null) {
      props = getDefaults();
    } else {
      DirDeployerImpl.log("Received a new configuration.");
    }

    final Object dirsValue = props.get(PROP_DIRS);
    if (dirsValue != null) {
      if (dirsValue instanceof String) {
        final String dirPaths = (String) dirsValue;
        final StringTokenizer st = new StringTokenizer(dirPaths, ",");
        dirs = new File[st.countTokens()];

        int i = 0;
        while (st.hasMoreTokens()) {
          final File dir = new File(st.nextToken().trim());
          dirs[i++] = dir;
        }
      } else if (dirsValue instanceof Vector<?>) {
        @SuppressWarnings("unchecked")
        final
        Vector<String> dirsPaths = (Vector<String>) dirsValue;
        dirs = new File[dirsPaths.size()];
        int i = 0;
        for (final String string : dirsPaths) {
          final File dir = new File(string.trim());
          dirs[i++] = dir;
        }
      }
    } else {
      dirs = new File[0];
    }

    final Boolean uiVal = (Boolean) props.get(PROP_USE_INITIAL_START_LEVEL);
    if (uiVal != null) {
      useInitialStartLevel = uiVal;
    }

    final Integer iVal = (Integer) props.get(PROP_STARTLEVEL);
    if (iVal != null) {
      startLevel = iVal;
    }
    if (startLevel<1) {
      startLevel = getFrameworkInitialStartLevel();
    }

    final Long lVal = (Long) props.get(PROP_INTERVAL);
    if (lVal != null) {
      interval = lVal;
    }

    final Boolean uVal = (Boolean) props.get(PROP_UNINSTALL);
    if (uVal != null) {
      uninstallOnStop = uVal;
    }
    
    final Boolean usefm = (Boolean) props.get(PROP_USE_FILE_MARKERS);
    if (usefm != null) {
      useFileMarkers = usefm;
    }
    final Boolean regc = (Boolean) props.get(PROP_REGISTER_CONTROLS);
    if (regc != null) {
      registerBundleControls = regc;
    }
    
    Activator.logger.info("Config values");
    Activator.logger.info(" useFIleMarkers: " + useFileMarkers);
    Activator.logger.info(" registerBundleControls: " + registerBundleControls);
    
  }

  Dictionary<String,?> getDefaults()
  {

    final Dictionary<String,Object> props = new Hashtable<>();

    final Object dirs = Activator.bc.getProperty(PROP_DIRS);
    props.put(PROP_DIRS, null == dirs ? DEFAULT_DIR : dirs);

    Boolean useInitialStartLevel = Boolean.TRUE;
    final String useInitialStartLevelO = Activator.bc.getProperty(PROP_USE_INITIAL_START_LEVEL);
    if (null != useInitialStartLevelO) {
      useInitialStartLevel = Boolean.valueOf(useInitialStartLevelO);
    }
    props.put(PROP_USE_INITIAL_START_LEVEL, useInitialStartLevel);

    final String startLevelO = Activator.bc.getProperty(PROP_STARTLEVEL);
    int startLevel = -1;
    if (null != startLevelO) {
      try {
        startLevel = Integer.parseInt(startLevelO);
      } catch (final NumberFormatException ignored) {
      }
    }
    if (startLevel < 1) {
      startLevel = getFrameworkInitialStartLevel();
    }
    props.put(PROP_STARTLEVEL, startLevel);

    long interval = 1000;
    final String intervalO = Activator.bc.getProperty(PROP_INTERVAL);
    if (null != intervalO) {
      try {
        interval = Long.parseLong(intervalO);
      } catch (final NumberFormatException ignored) {
      }
    }
    props.put(PROP_INTERVAL, interval);

    Boolean uninstallOnStop = Boolean.TRUE;
    final String uninstallOnStopO = Activator.bc.getProperty(PROP_UNINSTALL);
    if (null != uninstallOnStopO) {
      uninstallOnStop = Boolean.valueOf(uninstallOnStopO);
    }
    props.put(PROP_UNINSTALL, uninstallOnStop);
    
    props.put(PROP_USE_FILE_MARKERS, 
              getBoolProperty(Activator.bc, PROP_USE_FILE_MARKERS, USE_FILE_MARKERS_DEFAULT));
    props.put(PROP_REGISTER_CONTROLS, 
              getBoolProperty(Activator.bc, PROP_REGISTER_CONTROLS, REGISTER_BUNDLE_CONTROLS_DEFAULT));
    

    return props;
  }

  /**
   * Ask the start level service for the default start level of bundles
   *
   * @return the initial start level.
   */
  private int getFrameworkInitialStartLevel()
  {
    int startLevel;
    //
    final FrameworkStartLevel fsl =
      Activator.bc.getBundle(0).adapt(FrameworkStartLevel.class);
    if (null != fsl) {
      startLevel = fsl.getInitialBundleStartLevel();
    } else {
      startLevel = 1; // Fallback to the default initial bundle start level
    }
    return startLevel;
  }
  
  private static Boolean getBoolProperty(BundleContext bc, String p, Boolean def) {
    Boolean ret;
    
    final String o = bc.getProperty(p);
    if (null != o) {
      ret = Boolean.valueOf(o);
    } else {
      ret = def;
    }
    
    Activator.logger.info( p + " : " + ret);
    return ret;
    
  }
}
