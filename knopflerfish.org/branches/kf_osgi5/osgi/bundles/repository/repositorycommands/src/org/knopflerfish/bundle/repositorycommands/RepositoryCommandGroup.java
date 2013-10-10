/*
 * Copyright (c) 2013-2013, KNOPFLERFISH project
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

package org.knopflerfish.bundle.repositorycommands;

import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.knopflerfish.service.console.CommandGroupAdapter;
import org.knopflerfish.service.console.Session;
import org.knopflerfish.service.console.Util;
import org.knopflerfish.service.repositorymanager.BasicRequirement;
import org.knopflerfish.service.repositorymanager.RepositoryInfo;
import org.knopflerfish.service.repositorymanager.RepositoryManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.VersionRange;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.RepositoryContent;
import org.osgi.util.tracker.ServiceTracker;


/**
 * Console commands for interaction with OSGI repositories.
 *
 */

public class RepositoryCommandGroup
  extends CommandGroupAdapter
{
  final BundleContext bc;

  final private ServiceTracker<RepositoryManager, RepositoryManager> repoMgrTracker;


  RepositoryCommandGroup(BundleContext bc) {
    super("repository", "Repository commands");
    this.bc = bc;
    repoMgrTracker = new ServiceTracker<RepositoryManager, RepositoryManager>(bc,
        RepositoryManager.class.getName(), null);
    repoMgrTracker.open();
  }


  //
  // Add repository command
  //

  public final static String USAGE_ADD
    = "[-r #rank#] <url>";

  public final static String[] HELP_ADD = new String[] {
    "Add a XML based repository.",
    "-r #rank#  Set rank explicitly",
    "<url>      URL for repository file"
  };

  public int cmdAdd(Dictionary<String,?> opts, Reader in, PrintWriter out,
                    Session session) {
    final String url = (String) opts.get("url");
    final String rank = (String) opts.get("-r");
    Hashtable<String, Object> p = new Hashtable<String, Object>();
    if (rank != null) {
      try {
        p.put(Constants.SERVICE_RANKING, Integer.parseInt(rank));
      } catch (NumberFormatException nfe) {
        out.println("Rank not a number!");
        return 1;
      }
    }
    RepositoryInfo ri;
    try {
      ri = getRepositoryManager().addXmlRepository(url, p);
    } catch (Exception e) {
      out.println("Failed to add repository: " + e);
      return 1;
    }
    out.println("New repository created, id=" + ri.getId());
    return 0;
  }

  //
  // Bundle command
  //

  public final static String USAGE_BUNDLE
    = "[-l] [<symbolicname> [<versionRange>]]";

  public final static String[] HELP_BUNDLE = new String[] {
    "Find bundle resources all ot all that matches <symbolicname>",
    "and <versionRange>.",
    "-l             Verbose output",
    "<symbolicname> Bundle symbolic name to match",
    "<versionRange> Optional bundle version range"
  };

  public int cmdBundle(Dictionary<String,?> opts, Reader in, PrintWriter out,
                       Session session) {
    final boolean verbose = (opts.get("-l") != null);
    final String bsn = (String) opts.get("symbolicname");
    final String ver = (String) opts.get("versionRange");
    BasicRequirement requirement;
    if (bsn != null) {
      requirement = new BasicRequirement(IdentityNamespace.IDENTITY_NAMESPACE, bsn);
    } else {
      requirement = new BasicRequirement(IdentityNamespace.IDENTITY_NAMESPACE);
    }
    if (ver != null) {
      requirement.addVersionRangeFilter(new VersionRange(ver)); 
    }
    requirement.addBundleTypeFilter();
    List<Resource> resources = new ArrayList<Resource>();
    for (Capability c : getRepositoryManager().findProviders(requirement)) {
      resources.add(c.getResource());
    }
    if (resources.isEmpty()) {
      out.println("No bundles found!");
      return 1;
    } else {
      printBundleResources(out, resources, verbose);
    }
    return 0;
  }

  //
  // Disable command
  //

  public final static String USAGE_DISABLE
    = "<repository> ...";

  public final static String[] HELP_DISABLE = new String[] {
    "Disable repository, so that it won't be used when searching for resources",
    "<repository> Wildcard name or id of repository"
  };

  public int cmdDisable(Dictionary<String,?> opts, Reader in, PrintWriter out,
                        Session session) {
    final String [] selection = (String[]) opts.get("repository");
    final SortedSet<RepositoryInfo> repos = getRepoSelection(selection);

    if (repos.isEmpty()) {
      out.println("No matching repository.");
      return 1;
    } else {
      final RepositoryManager rm = getRepositoryManager();
      for (RepositoryInfo ri : repos) {
        rm.setRepositoryEnabled(ri, false);
      }
      printRepos(out, repos, "Disabled repositories", false);
      return 0;
    }
  }

  //
  // Enable command
  //

  public final static String USAGE_ENABLE
    = "<repository> ...";

  public final static String[] HELP_ENABLE = new String[] {
    "Enable repository, so they will be used when searching for resources",
    "<repository> Wildcard name or id of repository"
  };

  public int cmdEnable(Dictionary<String,?> opts, Reader in, PrintWriter out,
                       Session session) {
    final String [] selection = (String[]) opts.get("repository");
    final SortedSet<RepositoryInfo> repos = getRepoSelection(selection);

    if (repos.isEmpty()) {
      out.println("No matching repository.");
      return 1;
    } else {
      final RepositoryManager rm = getRepositoryManager();
      for (RepositoryInfo ri : repos) {
        rm.setRepositoryEnabled(ri, true);
      }
      printRepos(out, repos, "Enabled repositories:", false);
      return 0;
    }
  }

  


  //
  // Install bundle command
  //

  public final static String USAGE_INSTALL
    = "[-s] <symbolicname> [<versionRange>]";

  public final static String[] HELP_INSTALL = new String[] {
    "Install bundle resource that matches <symbolicname>",
    "and <versionRange>.",
    "-s             Persistently start bundle(s) according to activation policy",
    "<symbolicname> Bundle symbolic name to match",
    "<versionRange> Optional bundle version range"
  };

  public int cmdInstall(Dictionary<String,?> opts, Reader in, PrintWriter out,
                        Session session) {
    final String bsn = (String) opts.get("symbolicname");
    final String ver = (String) opts.get("versionRange");
    BasicRequirement requirement;
    requirement = new BasicRequirement(IdentityNamespace.IDENTITY_NAMESPACE, bsn);
    if (ver != null) {
      requirement.addVersionRangeFilter(new VersionRange(ver)); 
    }
    List<Capability> cs = getRepositoryManager().findProviders(requirement);
    if (cs.isEmpty()) {
      out.println("No matching bundle found!");
      return 1;
    }
    Bundle b = null;
    try {
      Capability c = cs.get(0);
      String loc = bsn;
      b = bc.installBundle(loc, ((RepositoryContent)c.getResource()).getContent());
      out.println("Installed: " + showBundle(b));
      if (opts.get("-s") != null) {
        b.start(Bundle.START_ACTIVATION_POLICY);
        out.println("Started: " + showBundle(b));
      }
    } catch (final BundleException e) {
      Throwable t = e;
      while (t instanceof BundleException
             && ((BundleException) t).getNestedException() != null) {
        t = ((BundleException) t).getNestedException();
      }
      if (b != null) {
        out.println("Couldn't start bundle: " + showBundle(b)
                    + " (due to: " + t + ")");
      } else {
        out.println("Couldn't install bundle (due to: " + t + ")");
        t.printStackTrace(out);
      }
      return 1;
    }

    return 0;
  }

  //
  // List command
  //

  public final static String USAGE_LIST
    = "[-l] [<repository>]";

  public final static String[] HELP_LIST = new String[] {
    "List repositories",
    "-l           Verbose output",
    "<repository> Name or id of repository"
  };

  public int cmdList(Dictionary<String,?> opts, Reader in, PrintWriter out,
                        Session session) {
    final String [] selection = (String[]) opts.get("repository");
    final SortedSet<RepositoryInfo> repos = getRepoSelection(selection);
    final boolean verbose = (opts.get("-l") != null);

    if (repos.isEmpty()) {
      if (selection != null) {
        out.println("No matching repository.");
      } else {
        out.println("No repositories found.");
      }
      return 1;
    } else {
      printRepos(out, repos, null, verbose);
      return 0;
    }
  }

  //
  // Private methods
  //

  private void printRepos(PrintWriter out, SortedSet<RepositoryInfo> rs, String heading, boolean verbose) {
    out.println(heading != null ? heading : "E  Id Rank  Description");
    out.println("----------------------");
    final RepositoryManager rm = getRepositoryManager();
    for (RepositoryInfo ri : rs) {
      final String desc = (String) ri.getProperty(Constants.SERVICE_DESCRIPTION);
      final long id = ri.getId();
      out.print(rm.isEnabled(ri) ? '*' : ' ');
      out.print(Util.showRight(4, Long.toString(id)));
      out.print(" ");
      out.print(Util.showRight(4, Integer.toString(ri.getRank())));
      out.print("  ");
      out.println(desc);
      if (verbose) {
        out.println();
      }
    }
  }

  private void printBundleResources(PrintWriter out, List<Resource> resources, boolean verbose) {
    out.println("I Bundle resource");
    out.println("- --------------------");
    for (Resource r : resources) {
      Map<String, Object> identity = r.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).iterator().next().getAttributes();
      out.print(isInstalled(r) ? "* " : "  ");
      out.print(identity.get(IdentityNamespace.IDENTITY_NAMESPACE));
      out.print(", version=");
      out.println(identity.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE));
      if (verbose) {
        Map<String, Object> content = r.getCapabilities(ContentNamespace.CONTENT_NAMESPACE).iterator().next().getAttributes();
        out.println("    Type: " + identity.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));
        out.println("    URL:  " + content.get(ContentNamespace.CAPABILITY_URL_ATTRIBUTE));
        out.println("    Size: " + content.get(ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE));
      }
    }
  }

  private boolean isInstalled(Resource r) {
    Map<String, Object> content = r.getCapabilities(ContentNamespace.CONTENT_NAMESPACE).iterator().next().getAttributes();
    Bundle b = bc.getBundle((String)content.get(ContentNamespace.CAPABILITY_URL_ATTRIBUTE));
    if (b != null) {
      // TODO, check that we have correct bundle
      return true;
    } else {
      // TODO, scan for bundle
      
    }
    return false;
  }

  private RepositoryManager getRepositoryManager() {
    RepositoryManager rm = repoMgrTracker.getService();
    if (rm != null) {
      return rm;
    } else {
      throw new IllegalStateException("Repository Manager not available.");
    }
  }

  private SortedSet<RepositoryInfo> getRepoSelection(String[] selection) {
    SortedSet<RepositoryInfo> res = getRepositoryManager().getAllRepositories();
    if (selection != null) {
      for (Iterator<RepositoryInfo> iterator = res.iterator(); iterator.hasNext();) {
        RepositoryInfo ri = iterator.next();
        final String desc = (String) ri.getProperty(Constants.SERVICE_DESCRIPTION);
        final long id = ri.getId();
        boolean match = false;
        for (String s : selection) {
          try {
            if (Long.parseLong(s) != id) {
              continue;
            }
          } catch (NumberFormatException _noNum) {
            if (desc == null || desc.indexOf(s) == -1) {
              continue;
            }
          }
          match  = true;
          break;
        }
        if (!match) {
          iterator.remove();
        }
      }
    }
    return res;
  }

  private String showBundle(Bundle b) {
    return Util.shortName(b) + " (#" + b.getBundleId() + ")";
  }

}
