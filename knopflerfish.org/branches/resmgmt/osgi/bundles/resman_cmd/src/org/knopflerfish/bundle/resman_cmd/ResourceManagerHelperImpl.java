/*
 * Copyright (c) 2012, KNOPFLERFISH project
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

package org.knopflerfish.bundle.resman_cmd;

import java.io.PrintWriter;
import java.io.Reader;
import java.util.Dictionary;

import org.knopflerfish.service.console.Session;
import org.knopflerfish.service.console.Util;
import org.knopflerfish.service.resman.ResourceManager;
import org.knopflerfish.service.resman.BundleMonitor;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.startlevel.StartLevel;


/**
 * Interface hiding the dependency to the {@link ResourceManager}
 * service for resource management console commands
 * interacting with the optional {@link ResourceManager} service.
 *
 * @author Makewave AB
 */

public class ResourceManagerHelperImpl
  implements ResourceManagerHelper
{
  private final BundleContext bc;
  private final ResourceManager resman;
  private final StartLevel startLevel;

  /**
   * Look up the resource management service if available. The service
   * will not change during the lifecycle of the framework instance so
   * no need to track it.
   */
  public ResourceManagerHelperImpl(final BundleContext bc)
  {
    this.bc  = bc;

    ServiceReference sr
      = bc.getServiceReference(ResourceManager.class.getName());
    resman = null==sr ? null : (ResourceManager) bc.getService(sr);

    sr = bc.getServiceReference(StartLevel.class.getName());
    startLevel = null==sr ? null : (StartLevel) bc.getService(sr);
  }



  //
  // Usage
  //

  private static final String USAGE_HEADING =
    "   id  level/state  Name                     Memory(max)   "
    +"Threads(max) CPU(max)";
  private static final String USAGE_SEP_LINE =
    "   --------------------------------------------------------"
    +"---------------------";
  private static final String USAGE_EMPTY_LINE =
    "                                                           "
    +"                     ";

  public int cmdUsage(final Dictionary opts,
                      final Reader in,
                      final PrintWriter out,
                      final Session session)
  {
    out.println(USAGE_HEADING);
    out.println(USAGE_SEP_LINE);

    final Bundle[] bundles = getBundles((String[]) opts.get("bundle"),
                                        true, true);

    for (int i = 0; i < bundles.length; i++) {
      final Bundle bundle = bundles[i];
      if (null!=bundle) {
        final StringBuffer sbuf = new StringBuffer(USAGE_EMPTY_LINE);
        sbuf.insert( 0, Util.showId(bundle) );
        sbuf.insert( 7, showState(bundle) );
        prettyPrint( sbuf, 20, 16, true, Util.shortName(bundle) );

        final BundleMonitor bmon = resman.getMonitor(bundle);
        if (bmon != null) {
          prettyPrint(sbuf, 38, 21, false, formatValueAndMax((int)bmon.getMemory(), (int)bmon.getMemoryLimit()));
          prettyPrint(sbuf, 61, 8, false, formatValueAndMax(bmon.getThreadCount(), bmon.getThreadCountLimit()));
          prettyPrint(sbuf, 71, 8, false, formatValueAndMax(bmon.getCPU(), bmon.getCPULimit()));
        }
        sbuf.setLength(80);
        out.println(sbuf);
      }
    }
    return 0;
  }


  private Bundle[] getBundles(String[] selection, boolean sortNumeric) {
    return getBundles(selection, sortNumeric, false, false);
  }

  private Bundle[] getBundles(String[] selection,
                              boolean sortNumeric,
                              boolean sortStartLevel) {
    return getBundles(selection, sortNumeric, sortStartLevel, false);
  }

  private Bundle[] getBundles(String[] selection,
                              boolean sortNumeric,
                              boolean sortStartLevel,
                              boolean sortTime) {
    Bundle[] b = bc.getBundles();
    Util.selectBundles(b, selection);
    if (sortNumeric) {
      Util.sortBundlesId(b);
    } else {
      Util.sortBundles(b, false);
    }
    if (sortStartLevel) {
      sortBundlesStartLevel(b);
    }
    if (sortTime) {
      Util.sortBundlesTime(b);
    }

    return b;
  }

  /**
   * Sort an array of bundle objects based on their start level All entries
   * with no start level is placed at the end of the array.
   *
   * @param b
   *            array of bundles to be sorted, modified with result
   */
  private void sortBundlesStartLevel(Bundle[] b) {
    int x = b.length;

    for (int l = x; x > 0;) {
      x = 0;
      int p = Integer.MAX_VALUE;
      try {
        p = startLevel.getBundleStartLevel(b[0]);
      } catch (Exception ignored) {
      }
      for (int i = 1; i < l; i++) {
        int n = Integer.MAX_VALUE;
        try {
          n = startLevel.getBundleStartLevel(b[i]);
        } catch (Exception ignored) {
        }
        if (p > n) {
          x = i - 1;
          Bundle t = b[x];
          b[x] = b[i];
          b[i] = t;
        } else {
          p = n;
        }
      }
    }
  }

  /**
   * Show bundle start level and state.
   *
   * @param bundle The bundle to present the state for.
   *
   * @return left aligned bundle state with length 13.
   */
  private String showState(final Bundle bundle) {
    final StringBuffer sb = new StringBuffer(13);

    try {
      sb.append(Integer.toString(startLevel.getBundleStartLevel(bundle)));
      while (sb.length() < 2) {
        sb.insert(0, " ");
      }
    } catch (Exception ignored) {
      sb.append("--");
    }

    sb.append("/");

    switch (bundle.getState()) {
    case Bundle.INSTALLED:
      sb.append("installed");
      break;
    case Bundle.RESOLVED:
      sb.append("resolved");
      break;
    case Bundle.STARTING:
      sb.append("starting");
      break;
    case Bundle.ACTIVE:
      sb.append("active");
      break;
    case Bundle.STOPPING:
      sb.append("stopping");
      break;
    case Bundle.UNINSTALLED:
      sb.append("uninstalled");
      break;
    default:
      sb.append("ILLEGAL <" + bundle.getState() + "> ");
      break;
    }
    while (sb.length() < 13) {
      sb.append(" ");
    }

    return sb.toString();
  }

  /**
   * Insert the given string into the stringbuffer left or right
   * aligend.
   *
   * @param sbuf the stringbuffer to insert into.
   * @param start position to insert the string on.
   * @param len max length of the inserted string.
   * @param leftalig if true left align the insertion.
   * @param s the string to insert.
   */
  private static void prettyPrint(final StringBuffer sbuf,
                                  final int start,
                                  final int len,
                                  final boolean leftalign,
                                  final String s) {
    // System.out.println("Pretty printing: " +s);
    final String s2 = s.substring(0, Math.min(len,s.length()));

    if (leftalign) {
      sbuf.insert(start, s2);
    } else {
      sbuf.insert(start+len-s2.length(), s2);
    }
  }

  /**
   * Build a string like <em>val(max)</em>.
   *
   * @param val the current value.
   * @param max the limit.
   *
   * @return String on the form <em>val(max)</em>.
   */
  static String formatValueAndMax(int val, int max) {
    final StringBuffer sbuf = new StringBuffer(22);
    sbuf.append(val);
    sbuf.append("(");
    sbuf.append(max);
    sbuf.append(")");

    return sbuf.toString();
  }

}
