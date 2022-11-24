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

package org.knopflerfish.bundle.restart_test;

import java.util.Map;
import java.util.Properties;

import org.osgi.framework.Bundle;
import org.osgi.service.startlevel.StartLevel;

class State extends Properties {
  static String B     = "bundle_";
  static String STATE = "_state";
  static String ID    = "_id";
  static String UUID  = "_uuid";
  static String LOC   = "_loc";
  static String STARTLEVEL  = "_startlevel";
  
  static String BUNDLE_UUID  = "Bundle-UUID";

  int count = 0;
  StartLevel sl;

  State(StartLevel sl) {
    super();
    this.sl = sl;
  }

  
  public void addBundle(Bundle b) {
    put(B + count + ID,    "" + b.getBundleId());
    put(B + count + STATE, "" + b.getState());
    put(B + count + UUID,  "" + b.getHeaders().get(BUNDLE_UUID));
    put(B + count + LOC,   "" + b.getLocation());
    try {
      put(B + count + STARTLEVEL, "" + sl.getBundleStartLevel(b));
    } catch (IllegalArgumentException iae) {
      put(B + count + STARTLEVEL, "-1");
    }

    count++;
  }

  void assertBundles(Bundle[] bl) throws Exception {
    for (Map.Entry<Object, Object> entry : this.entrySet()) {
      String key = (String) entry.getKey();

      if (key.endsWith(UUID)) {
	String uuid = (String) entry.getValue();
	//	System.out.println("key=" + key + ", val=" + val);

	int ix = key.indexOf("_", B.length() + 1);
	String n = key.substring(B.length(), ix);

	assertBundle(bl, n, uuid);

      }
    }
  }

  void assertBundle(Bundle[] bl, String n, String uuid) throws Exception {
    System.out.println("assertBundle uuid=" + uuid + ", n=" + n);
    int state   = Integer.parseInt((String)get(B + n + STATE));
    int level   = Integer.parseInt((String)get(B + n + STARTLEVEL));

    for (Bundle bundle : bl) {
      if (uuid.equals(bundle.getHeaders().get(BUNDLE_UUID))) {
        //	System.out.println(get(B + n + STATE));

        if (state != bundle.getState()) {
          throw new Exception("FAILED: bundle UUID=" + uuid +
              ", expected state " + state +
              ", found " + bundle.getState());
        }

        if (state != Bundle.UNINSTALLED) {
          if (level != sl.getBundleStartLevel(bundle)) {
            throw new Exception("FAILED: bundle UUID=" + uuid +
                ", expected level " + level +
                ", found " + sl.getBundleStartLevel(bundle));
          }
        }

        System.out.println("PASSED uuid=" + uuid);
        return;
      }
    }
    if (state == Bundle.UNINSTALLED) {
      System.out.println("PASSED: no UUID since uninstalled");
    } else {
      System.out.println("FAILED: no bundle with UUID=" + uuid);
      throw new Exception("FAILED: no bundle with UUID=" + uuid);
    }
  }
}
