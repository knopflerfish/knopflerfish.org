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
import java.util.Iterator;

import org.knopflerfish.service.console.Session;
import org.knopflerfish.service.console.Util;
import org.knopflerfish.service.resman.ResourceManager;
import org.knopflerfish.service.resman.BundleMonitor;
import org.knopflerfish.service.resman.BundleRevisionMonitor;

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
    "   id level/state  Name                  Memory(max) "
    +"Threads(max) CPU(max)";
  private static final String USAGE_HEADING_ALL =
    "   id rev level/state  Name                  Memory(max) "
    +"Threads(max) CPU(max)";

  private static final String USAGE_SEP_LINE =
    "   -----------------------------------------------------"
    +"------------------";
  private static final String USAGE_SEP_LINE_ALL =
    "   ---------------------------------------------------------"
    +"------------------";

  public int cmdUsage(final Dictionary opts,
                      final Reader in,
                      final PrintWriter out,
                      final Session session)
  {
    final boolean allBundleRevisions = opts.get("-l") != null;
    final Bundle[] bundles = getBundles((String[]) opts.get("bundle"),
                                        true, true);
    int genStartPos = 0;
    final int genLength = 3;

    out.println(allBundleRevisions ? USAGE_HEADING_ALL : USAGE_HEADING);
    out.println(allBundleRevisions ? USAGE_SEP_LINE_ALL : USAGE_SEP_LINE);

    final StringBuffer sbuf = new StringBuffer(82);
    for (int i = 0; i < bundles.length; i++) {
      final Bundle bundle = bundles[i];
      if (null!=bundle) {
        sbuf.setLength(0);
        sbuf.append(Util.showId(bundle));
        if (allBundleRevisions) {
          genStartPos = sbuf.length();
          sbuf.append(Util.showRight(genLength, "rev"));
          sbuf.append(" ");
        }
        sbuf.append(showState(bundle));
        sbuf.append(Util.showLeft(16, Util.shortName(bundle)));

        final BundleMonitor bmon = resman.getMonitor(bundle);
        if (bmon != null) {
          if (allBundleRevisions) {
            final int monStartPos = sbuf.length();

            for (Iterator it=bmon.getBundleRevisionMonitors(); it.hasNext();) {
              final BundleRevisionMonitor brmon
                = (BundleRevisionMonitor) it.next();
              final String gen
                = Util.showRight(genLength,
                                 String.valueOf(brmon.getBundleGeneration()));
              sbuf.replace(genStartPos, genStartPos+genLength, gen);
              appendBundlerRevisionMonitorData(sbuf, brmon);
              out.println(sbuf);
              // Truncate sbuf
              sbuf.setLength(monStartPos);
            }
          } else {
            appendBundlerRevisionMonitorData(sbuf, bmon);
            out.println(sbuf);
          }
        }
      }
    }
    return 0;
  }

  private void appendBundlerRevisionMonitorData(final StringBuffer sbuf,
                                                final BundleRevisionMonitor brmon)
  {
    String vm = formatValueAndMax((int)brmon.getMemory(),
                                  (int)brmon.getMemoryLimit());
    sbuf.append(Util.showRight(21, vm));

    vm = formatValueAndMax(brmon.getThreadCount(),
                           brmon.getThreadCountLimit());
    sbuf.append(Util.showRight( 9, vm));

    vm = formatValueAndMax(brmon.getCPU(), brmon.getCPULimit());
    sbuf.append(Util.showRight( 9, vm));
  }

  //
  // limit
  //

  public int cmdLimit(Dictionary opts,
                      Reader in,
                      PrintWriter out,
                      Session session)
  {
    int memLimit = 0;
    final String memLimitS = (String) opts.get("-mem");
    if (null!=memLimitS) {
      try {
        memLimit = Integer.parseInt(memLimitS);
        if (memLimit<0) {
          out.println("The given memory limit, "+ memLimit +", is <0.");
          return 1;
        }
      } catch (Exception e) {
        out.println("Failed to read integer from memory limit '"
                    +memLimitS +"': " +e);
        return 1;
      }
    }

    int cpuLimit = 0;
    final String cpuLimitS = (String) opts.get("-cpu");
    if (null!=cpuLimitS) {
      try {
        cpuLimit = Integer.parseInt(cpuLimitS);
        if (cpuLimit>100) {
          out.println("The given CPU limit, "+ cpuLimit +", is >100%");
          return 1;
        }
        if (cpuLimit<0) {
          out.println("The given CPU limit, "+ cpuLimit +", is <0.");
          return 1;
        }
      } catch (Exception e) {
        out.println("Failed to read integer from cpu limit '"
                    +cpuLimitS +"': " +e);
        return 1;
      }
    }

    int threadsLimit = 0;
    final String threadsLimitS = (String) opts.get("-threads");
    if (null!=threadsLimitS) {
      try {
        threadsLimit = Integer.parseInt(threadsLimitS);
        if (threadsLimit<0) {
          out.println("The given thread count limit, "+ threadsLimit
                      +", is <0.");
          return 1;
        }
      } catch (Exception e) {
        out.println("Failed to read integer from threads limit '"
                    +threadsLimitS +"': " +e);
        return 1;
      }
    }


    final Bundle[] bundles = getBundles((String[]) opts.get("bundle"),
                                        true, true);
    for (int i = 0; i < bundles.length; i++) {
      final Bundle bundle = bundles[i];
      if (null!=bundle) {
        final BundleMonitor bmon = resman.getMonitor(bundle);
        if (bmon != null) {
          if (memLimit>0) {
            bmon.setMemoryLimit(memLimit);
          }
          if (cpuLimit>0) {
            bmon.setCPULimit(cpuLimit);
          }
          if (threadsLimit>0) {
            bmon.setThreadCountLimit(threadsLimit);
          }
        }
      }
    }
    return 0;
  }



  ////////////////////////////////////////////////////////////
  //
  // Private helper methods
  //
  ////////////////////////////////////////////////////////////

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
