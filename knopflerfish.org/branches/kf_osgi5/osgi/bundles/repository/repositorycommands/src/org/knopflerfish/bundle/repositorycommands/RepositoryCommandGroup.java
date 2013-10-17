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
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;

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
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
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
    "List bundle resources.",
    "List all bundles that matches <symbolicname>",
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
    requirement.addBundleIdentityFilter();
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
    "Disable selected repository.",
    "Disables a repository so that it won't be used when searching",
    "for resources.",
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
        if (rm.setRepositoryEnabled(ri, false)) {
          out.println("Disabled repository#" + ri.getId());
        }
      }
      return 0;
    }
  }

  //
  // Enable command
  //

  public final static String USAGE_ENABLE
    = "<repository> ...";

  public final static String[] HELP_ENABLE = new String[] {
    "Enable selected repository.",
    "Enables a repository so that it will be used when searching",
    "for resources.",
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
        if (rm.setRepositoryEnabled(ri, true)) {
          out.println("Enabled repository#" + ri.getId());
        }
      }
      return 0;
    }
  }

  //
  // Install bundle command
  //

  public final static String USAGE_INSTALL
    = "[-s] <symbolicname> [<versionRange>]";

  public final static String[] HELP_INSTALL = new String[] {
    "Install bundle resource.",
    "Installs first bundle resource that matches <symbolicname>",
    "and optional <versionRange>.",
    "-s             Persistently start bundle according to activation policy",
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
    requirement.addBundleIdentityFilter();
    List<Capability> cs = getRepositoryManager().findProviders(requirement);
    if (cs.isEmpty()) {
      out.println("No matching bundle found!");
      return 1;
    }
    Bundle b = null;
    try {
      Resource r = cs.get(0).getResource();
      String loc = null;
      try {
        loc = (String) r.getCapabilities(ContentNamespace.CONTENT_NAMESPACE).get(0).getAttributes().get(ContentNamespace.CAPABILITY_URL_ATTRIBUTE);
        b = bc.installBundle(loc);
      } catch (Exception be) {
        if (loc == null) {
          loc = bsn;
        }
        b = bc.installBundle(loc, ((RepositoryContent)r).getContent());
      }
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
    "List repositories.",
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
  // Rank command
  //

  public final static String USAGE_RANK
    = "<rank> <repository> ...";

  public final static String[] HELP_RANK = new String[] {
    "Change repository ranking.",
    "The rank is used to can change the order in which repositories",
    "are searched. Repository with highest ranking is searched first.",
    "<rank>       New rank of repository, must be an integer",
    "<repository> Wildcard name or id of repository"
  };

  public int cmdRank(Dictionary<String,?> opts, Reader in, PrintWriter out,
                     Session session) {
    final String rankStr = (String) opts.get("rank");
    final String [] selection = (String[]) opts.get("repository");
    final SortedSet<RepositoryInfo> repos = getRepoSelection(selection);

    int rank;
    try {
      rank = Integer.parseInt(rankStr);
    } catch (NumberFormatException nfe) {
      out.println("Rank is not an integer.");
      return 1;      
    }
    if (repos.isEmpty()) {
      out.println("No matching repository.");
      return 1;
    } else {
      final RepositoryManager rm = getRepositoryManager();
      for (RepositoryInfo ri : repos) {
        if (rm.setRepositoryRank(ri, rank)) {
          out.println("Changed rank to " + rank + " for repository#" + ri.getId());
        }
      }
      return 0;
    }
  }

  //
  // Show command
  //

  public final static String USAGE_SHOW
    = "[-t] <namespace> [<filter>]";

  public final static String[] HELP_SHOW = new String[] {
    "Show all capabilities and requirements for selected resources.",
    "-t          Terse output, only show namespace attribute.",
    "<namespace> Which namespace to search.",
    "<filter>    OSGi filter expression for selecting resources."
  };

  public int cmdShow(Dictionary<String,?> opts, Reader in, PrintWriter out,
                     Session session) {
    final String namespace = (String) opts.get("namespace");
    final String filterStr = (String) opts.get("filter");

    BasicRequirement requirement;
    requirement = new BasicRequirement(namespace);
    if (filterStr != null) {
      try {
        FrameworkUtil.createFilter(filterStr);
      } catch (InvalidSyntaxException e) {
        out.println("Invalid filter: " + e.getMessage());
        return 1;
      }
      requirement.addDirective("filter", filterStr); 
    }
    List<Capability> cs = getRepositoryManager().findProviders(requirement);
    if (cs.isEmpty()) {
      out.println("No matching resources found!");
      return 0;
    }
    List<Resource> resources = new ArrayList<Resource>();
    for (Capability c : cs) {
      resources.add(c.getResource());
    }
    printResources(out, resources, namespace, opts.get("-t") == null);
    return 0;
  }


  //
  // Private methods
  //

  private void printRepos(PrintWriter out, SortedSet<RepositoryInfo> rs, String heading, boolean verbose) {
    out.println(heading != null ? heading : "E  Id Rank  Description");
    out.println("------------------------");
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

  private void printResources(PrintWriter out, List<Resource> resources, String namespace, boolean verbose) {
    out.println("Resource");
    out.println("======================");
    for (Resource r : resources) {
      final List<Capability> capabilities = r.getCapabilities(namespace);
      Map<String, Object> nsAttrs = capabilities.iterator().next().getAttributes();
      out.println(nsAttrs.get(namespace));
      if (verbose) {
        out.println("---------------");
        out.println("  Capabilities:");
        final List<Capability> caps = new ArrayList<Capability>(r.getCapabilities(null));
        Collections.sort(caps, new Comparator<Capability>() {
          @SuppressWarnings("unchecked")
          @Override
          public int compare(Capability c1, Capability c2) {
            final String ns = c1.getNamespace();
            int res = ns.compareTo(c2.getNamespace());
            if (res == 0) {
              Object a1 = c1.getAttributes().get(ns);
              if (a1 instanceof Comparable) {
                res = ((Comparable<Object>)a1).compareTo(c2.getAttributes().get(ns));
              }
            }
            return res;
          }
        });
        String oldNs = null;
        for (Capability rc : caps) {
          String ns = rc.getNamespace();
          if (!ns.equals(oldNs)) {
            out.println("    Namespace: " + ns);
            oldNs = ns;
          } else {
            out.println("     --");            
          }
          printMap(out, rc.getAttributes(), "       ", " = ");
          printMap(out, rc.getDirectives(), "       ", " := ");
        }
        out.println("  Requirements:");
        final List<Requirement> reqs = new ArrayList<Requirement>(r.getRequirements(null));
        Collections.sort(reqs, new Comparator<Requirement>() {
          @SuppressWarnings("unchecked")
          @Override
          public int compare(Requirement r1, Requirement r2) {
            final String ns = r1.getNamespace();
            int res = ns.compareTo(r2.getNamespace());
            if (res == 0) {
              Object a1 = r1.getAttributes().get(ns);
              if (a1 instanceof Comparable) {
                res = ((Comparable<Object>)a1).compareTo(r2.getAttributes().get(ns));
              }
            }
            return res;
          }
        });
        oldNs = null;
        for (Requirement rr : r.getRequirements(null)) {
          String ns = rr.getNamespace();
          if (!ns.equals(oldNs)) {
            out.println("    Namespace: " + ns);
            oldNs = ns;
          } else {
            out.println("     --");            
          }
          printMap(out, rr.getAttributes(), "       ", " = ");
          printMap(out, rr.getDirectives(), "       ", " := ");
        }
        out.println();
      }
    }
  }

  private void printMap(PrintWriter out, Map<String,?> m, String prefix, String div) {
    for (Entry<String,?> e : m.entrySet()) {
      out.print(prefix);
      out.print(e.getKey());
      out.print(div);
      out.println(e.getValue());
    }
  }


  private boolean isInstalled(Resource r) {
    Map<String, Object> identity = r.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).iterator().next().getAttributes();
    String name = (String) identity.get(IdentityNamespace.IDENTITY_NAMESPACE);
    Version version = (Version) identity.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
    Map<String, Object> content = r.getCapabilities(ContentNamespace.CONTENT_NAMESPACE).iterator().next().getAttributes();
    Bundle lb = bc.getBundle((String)content.get(ContentNamespace.CAPABILITY_URL_ATTRIBUTE));
    if (lb != null && name.equals(lb.getSymbolicName()) && version.equals(lb.getVersion())) {
      return true;
    }
    for (Bundle b : bc.getBundles()) {
      if (name.equals(b.getSymbolicName()) && version.equals(b.getVersion())) {
        return true;
      }
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
