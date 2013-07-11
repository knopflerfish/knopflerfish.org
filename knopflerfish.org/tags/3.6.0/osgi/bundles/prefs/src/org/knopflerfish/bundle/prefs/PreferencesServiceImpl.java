/*
 * Copyright (c) 2003-2009, KNOPFLERFISH project
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

package org.knopflerfish.bundle.prefs;

import org.osgi.framework.*;
import org.osgi.service.prefs.*;
import java.util.*;


public class PreferencesServiceImpl implements PreferencesService {

  /**
   * Factory method that creates an actual PrefsStorage instance for
   * the system tree.
   */
  protected PrefsStorage createPrefsStorageSystem()
  {
    return PrefsStorageFile.createPrefsStorageSystem(bundle);
  }

  /**
   * Factory method that creates an actual PrefsStorage instance for
   * a user tree.
   */
  protected PrefsStorage createPrefsStorageUser(String user)
  {
    return PrefsStorageFile.createPrefsStorageUser(bundle, user);
  }

  /**
   * Template method that returns a list with the name of all users
   * that have a preferences tree.
   */
  protected String[] getPrefsStorageUsers()
  {
    return PrefsStorageFile.getPrefsStorageUsers(bundle);
  }

  /**
   * Template method that removes all preferences trees for the
   * specified bundle form the backing store.
   * Called when the bundle is un-installed.
   */
  protected void cleanup()
  {
    PrefsStorageFile.cleanup(bundle);
  }


  final Bundle bundle;

  PrefsStorage systemStorage;

  // String -> PrefsStorage
  Map userStorage = new HashMap();

  protected PreferencesServiceImpl() {
    bundle = null;
  }

  PreferencesServiceImpl(Bundle bundle) {
    this.bundle = bundle;
  }

  public Preferences getSystemPreferences() {
    if (null==systemStorage || systemStorage.isStale()) {
      systemStorage = createPrefsStorageSystem();
    }
    return systemStorage.getNode("", true);
  }

  public Preferences getUserPreferences(String name) {
    synchronized(userStorage) {
      PrefsStorage storage = (PrefsStorage) userStorage.get(name);
      if(storage == null || storage.isStale()) {
        storage   = createPrefsStorageUser(name);
        userStorage.put(name, storage);
      }
      return storage.getNode("", true);
    }
  }

  public String[] getUsers() {
    synchronized(userStorage) {
      return getPrefsStorageUsers();
    }
  }

  public void flush() {
    if (null!=systemStorage) {
      try {
        systemStorage.flush(null);
      } catch (BackingStoreException bse) {
        // Ignore, is logged by the actual storage impl.
      }
    }
    for (Iterator it = userStorage.values().iterator(); it.hasNext(); ) {
      PrefsStorage storage = (PrefsStorage) it.next();
      try {
        storage.flush(null);
      } catch (BackingStoreException bse) {
        // Ignore, is logged by the actual storage impl.
      }
    }
  }
}
