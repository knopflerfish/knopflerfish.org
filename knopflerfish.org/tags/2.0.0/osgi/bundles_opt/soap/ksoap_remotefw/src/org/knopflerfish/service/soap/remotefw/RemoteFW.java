/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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
package org.knopflerfish.service.soap.remotefw;

import org.osgi.framework.*;
import java.util.*;

public interface RemoteFW {
  public static final String NULL_STR = "@@NULL@@";

  public void startBundle(long bid);
  public void stopBundle(long bid);
  public void updateBundle(long bid);
  public void uninstallBundle(long bid);
  public long installBundle(String location);

  public long        getBundle();

  public String      getBundleContextProperty(String key);

  // (bid)
  public long[]     getBundles();

  public String     getBundleLocation(long bid);
  public int        getBundleState(long bid);

  // (key, value)
  public Vector       getBundleManifest(long bid);

  // (sid)
  public long[]    getRegisteredServices(long bid);

  // (sid)
  public long[]    getServicesInUse(long bid);

  // (sid, bid)
  public long[]    getServiceReferences(String filter);
  public long[]    getServiceReferences2(String clazz, String filter);

  // (bid)
  public long[]    getUsingBundles(long sid);

  // (bid, type)
  public long[]    getBundleEvents();

  // (sid, type)
  public long[]    getServiceEvents();

  // (bid, type)
  public long[]     getFrameworkEvents();

  // (key, value)
  public Vector getServiceProperties(long sid);

  public int     getStartLevel();
  public void    setStartLevel(int level);
  public void    setBundleStartLevel(long bid, int level);
  public int     getBundleStartLevel(long bid);
  public void    setInitialBundleStartLevel(int level);
  public int     getInitialBundleStartLevel();
  public boolean isBundlePersistentlyStarted(long bid);

  public Vector    getExportedPackage(String name);
  public Vector  getExportedPackages(long bid);
  public void   refreshPackages(long[] bids) ;

  public Vector    getSystemProperties();

  public Vector getLog();
  public Vector getFullLog();

  public void createSession(String name);
  public void abortCommand();
  public void closeSession();
  public void setEscapeChar(char ch);
  public void setInterruptString(String str);
  public String[] setAlias(String key, String[] val);
  public String runCommand(String command);

}

