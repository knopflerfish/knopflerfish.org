/*
 * Copyright (c) 2003-2011, KNOPFLERFISH project
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

import java.io.IOException;
import java.util.*;

/**
 * Interface for managing bundle data.
 * 
 * @author Jan Stein
 * @author Philippe Laporte
 * @author Mats-Ola Persson
 * @author Gunnar Ekolin
 */
public interface BundleArchive {

  /**
   * Autostart setting stopped.
   * 
   * @see BundleArchive#setAutostartSetting(String)
   */
  public String AUTOSTART_SETTING_STOPPED = "stopped";

  /**
   * Autostart setting eager.
   * 
   * @see BundleArchive#setAutostartSetting(String)
   */
  public String AUTOSTART_SETTING_EAGER = "eager";

  /**
   * Autostart setting declared activation policy.
   * 
   * @see BundleArchive#setAutostartSetting(String)
   */
  public String AUTOSTART_SETTING_ACTIVATION_POLICY = "activation_policy";


  /**
   * Get an attribute from the manifest of a bundle.
   * 
   * Not localized
   * 
   * @param key Name of attribute to get.
   * @return A string with result or null if the entry doesn't exists.
   */
  String getAttribute(String key);


  /**
   * Get a FileArchive handle to a named Jar file or directory within this
   * archive.
   * 
   * @param path Name of Jar file or directory to get.
   * @return A FileArchive object representing new archive, null if not found.
   */
  FileArchive getFileArchive(String path);


  /**
   * Gets all localization entries from this bundle. Will typically read the
   * file OSGI-INF/bundle_&lt;locale&gt;.properties.
   * 
   * @param localeFile Filename within archive for localization properties.
   * @return null or a mapping of the entries.
   */
  Hashtable getLocalizationEntries(String localeFile);


  /**
   * @returns the (raw/unlocalized) attributes
   */
  HeaderDictionary getUnlocalizedAttributes();


  /**
   * Get bundle generation associated with this bundle archive.
   * 
   * @return BundleGeneration object.
   */
  BundleGeneration getBundleGeneration();


  /**
   * Set bundle generation associated with this bundle archive.
   * 
   * @param BundleGeneration object.
   */
  void setBundleGeneration(BundleGeneration bg);


  /**
   * Get bundle identifier for this bundle archive.
   * 
   * @return Bundle identifier.
   */
  long getBundleId();


  /**
   * Get bundle location for this bundle archive.
   * 
   * @return Bundle location.
   */
  String getBundleLocation();


  /**
   * Get a BundleResourceStream to named entry inside a bundle. Leading '/' is
   * stripped.
   * 
   * @param component Entry to get reference to.
   * @param ix index of sub archives. A postive number is the classpath entry
   *          index. 0 means look in the main bundle.
   * @return BundleResourceStream to entry or null if it doesn't exist.
   */
  BundleResourceStream getBundleResourceStream(String component, int ix);


  /**
   * Returns an Enumeration of all the paths (<code>String</code> objects) to
   * entries within the bundle whose longest sub-path matches the supplied path
   * argument.
   * 
   * @param name
   * @return
   */
  Enumeration findResourcesPath(String path);


  /**
   * Get stored bundle start level.
   */
  int getStartLevel();


  /**
   * Set stored bundle start level.
   */
  void setStartLevel(int level) throws IOException;


  /**
   * Get last modified timestamp.
   */
  long getLastModified();


  /**
   * Set stored last modified timestamp.
   */
  void setLastModified(long timemillisecs) throws IOException;


  /**
   * Get auto-start setting.
   * 
   * @return the autostart setting. "-1" if bundle not started.
   */
  int getAutostartSetting();


  /**
   * Set the auto-start setting.
   * 
   * @param setting the autostart setting to use.
   */
  void setAutostartSetting(int setting) throws IOException;


  /**
   * @return the location of the cached bundle.
   */
  String getJarLocation();


  /**
   * Get certificate chains associated with with bundle archive.
   * 
   * @param onlyTrusted Only return trusted certificates.
   * @return All certificates or null if bundle is unsigned.
   */
  ArrayList getCertificateChains(boolean onlyTrusted);


  /**
   * Mark certificate associated with with bundle archive as trusted.
   * 
   */
  void trustCertificateChain(List trustedChain);


  /**
   * Remove bundle archive from persistent storage.
   */
  void purge();


  /**
   * Close archive and all its open files.
   */
  void close();

}
