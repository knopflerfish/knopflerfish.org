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

package org.knopflerfish.bundle.frameworkcommands;

import java.io.PrintWriter;
import java.io.Reader;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.permissionadmin.PermissionInfo;

import org.knopflerfish.service.console.Session;
import org.knopflerfish.service.console.Util;

/**
 * Implementation of framework console commands interacting with the
 * optional {@link PermissionAdmin} and {@link
 * ConditionalPermissionAdmin} services.
 *
 * @author Makewave AB
 */

public class PermissionAdminHelperImpl
 implements PermissionAdminHelper
{
  final BundleContext bc;

  private final PermissionAdmin permissionAdmin;
  private final ConditionalPermissionAdmin condPermAdmin;

  PermissionAdminHelperImpl(BundleContext bc) {
    this.bc = bc;

    // all of these services are framework singleton internal services
    // thus, we take a shortcut and skip the service tracking
    ServiceReference sr =
      bc.getServiceReference(PermissionAdmin.class.getName());
    permissionAdmin = null==sr ? null : (PermissionAdmin) bc.getService(sr);

    sr = bc.getServiceReference(ConditionalPermissionAdmin.class.getName());
    condPermAdmin = null==sr ? null
      : (ConditionalPermissionAdmin) bc.getService(sr);
  }


  //
  // Addpermission command
  //

  public int cmdAddpermission(Dictionary opts, Reader in, PrintWriter out,
                              Session session) {
    if (permissionAdmin == null) {
      out.println("Permission Admin service is not available");
      return 1;
    }
    String loc = null;
    PermissionInfo[] pi;
    String selection = (String) opts.get("-b");
    if (selection != null) {
      Bundle[] b = bc.getBundles();
      Util.selectBundles(b, new String[] { selection });
      for (int i = 0; i < b.length; i++) {
        if (b[i] != null) {
          if (loc == null) {
            loc = b[i].getLocation();
          } else {
            out.println("ERROR! Multiple bundles selected");
            return 1;
          }
        }
      }
      if (loc == null) {
        out.println("ERROR! No matching bundle");
        return 1;
      }
      pi = permissionAdmin.getPermissions(loc);
    } else if (opts.get("-d") != null) {
      pi = permissionAdmin.getDefaultPermissions();
    } else {
      loc = (String) opts.get("-l");
      pi = permissionAdmin.getPermissions(loc);
    }
    PermissionInfo pia;
    try {
      pia = new PermissionInfo((String)opts.get("type"),
                               (String)opts.get("name"),
                               (String)opts.get("actions"));
    } catch (IllegalArgumentException e) {
      out.println("ERROR! " + e.getMessage());
      out.println("PermissionInfo type = " + opts.get("type"));
      out.println("PermissionInfo name = " + opts.get("name"));
      out.println("PermissionInfo actions = " + opts.get("actions"));
      return 1;
    }
    if (pi != null) {
      PermissionInfo[] npi = new PermissionInfo[pi.length + 1];
      System.arraycopy(pi, 0, npi, 0, pi.length);
      pi = npi;
    } else {
      pi = new PermissionInfo[1];
    }
    pi[pi.length - 1] = pia;
    if (loc != null) {
      permissionAdmin.setPermissions(loc, pi);
    } else {
      permissionAdmin.setDefaultPermissions(pi);
    }
    return 0;
  }


  //
  // Deletepermission command
  //

  public int cmdDeletepermission(Dictionary opts, Reader in, PrintWriter out,
                                 Session session) {
    if (permissionAdmin == null) {
      out.println("Permission Admin service is not available");
      return 1;
    }
    String loc = null;
    PermissionInfo[] pi;
    String selection = (String) opts.get("-b");
    if (selection != null) {
      Bundle[] b = bc.getBundles();
      Util.selectBundles(b, new String[] { selection });
      for (int i = 0; i < b.length; i++) {
        if (b[i] != null) {
          if (loc == null) {
            loc = b[i].getLocation();
          } else {
            out.println("ERROR! Multiple bundles selected");
            return 1;
          }
        }
      }
      if (loc == null) {
        out.println("ERROR! No matching bundle");
        return 1;
      }
      pi = permissionAdmin.getPermissions(loc);
    } else if (opts.get("-d") != null) {
      pi = permissionAdmin.getDefaultPermissions();
    } else {
      loc = (String) opts.get("-l");
      pi = permissionAdmin.getPermissions(loc);
    }
    if (pi != null) {
      String type = (String) opts.get("type");
      String name = (String) opts.get("name");
      String actions = (String) opts.get("actions");
      int size = 0;
      for (int i = 0; i < pi.length; i++) {
        if (("*".equals(type) || pi[i].getType().equals(type))
            && ("*".equals(name) || pi[i].getName().equals(name))
            && ("*".equals(actions) || pi[i].getActions().equals(
                                                                 actions))) {
          pi[i] = null;
        } else {
          size++;
        }
      }
      if (size == 0) {
        if (opts.get("-r") != null) {
          pi = null;
        } else {
          pi = new PermissionInfo[0];
        }
      } else {
        PermissionInfo[] npi = new PermissionInfo[size];
        for (int i = pi.length - 1; i >= 0; i--) {
          if (pi[i] != null) {
            npi[--size] = pi[i];
          }
        }
        pi = npi;
      }
      if (loc != null) {
        permissionAdmin.setPermissions(loc, pi);
      } else {
        permissionAdmin.setDefaultPermissions(pi);
      }
    }
    return 0;
  }

  //
  // Permissions command
  //

  public int cmdPermissions(Dictionary opts, Reader in, PrintWriter out,
                            Session session) {
    if (permissionAdmin == null) {
      out.println("Permission Admin service is not available");
      return 1;
    }
    String[] loclist = permissionAdmin.getLocations();
    String[] selection = (String[]) opts.get("selection");
    if (loclist != null && selection != null) {
      Bundle[] b = bc.getBundles();
      Util.selectBundles(b, selection);
      lloop: for (int i = 0; i < loclist.length; i++) {
        for (int j = 0; j < selection.length; j++) {
          if (loclist[i].equals(selection[j])) {
            continue lloop;
          }
        }
        for (int j = 0; j < b.length; j++) {
          if (b[j] != null && loclist[i].equals(b[j].getLocation())) {
            continue lloop;
          }
        }
        loclist[i] = null;
      }
    }

    if (opts.get("-d") != null) {
      out.println("Default permissions");
      showPerms(out, permissionAdmin.getDefaultPermissions());
    }

    if (loclist != null) {
      Bundle[] b = bc.getBundles();
      for (int i = 0; i < loclist.length; i++) {
        if (loclist[i] != null) {
          int j = b.length;
          while (--j >= 0) {
            if (loclist[i].equals(b[j].getLocation())) {
              break;
            }
          }
          out.println("Location: "
                      + loclist[i]
                      + (j >= 0 ? " (Bundle #" + b[j].getBundleId() + ")"
                         : ""));
          showPerms(out, permissionAdmin.getPermissions(loclist[i]));
        }
      }
    }
    return 0;
  }

  public int cmdCondpermission(Dictionary opts,
                               Reader in,
                               PrintWriter out,
                               Session session) {
    if (condPermAdmin == null) {
      out.println("Conditional Permission Admin service is not available");
      return 1;
    }
    String [] names = (String []) opts.get("name");
    Enumeration e;
    if (names != null) {
      Vector cpis = new Vector();
      for (int i = 0; i < names.length; i++ ) {
        ConditionalPermissionInfo cpi
          = condPermAdmin.getConditionalPermissionInfo(names[i]);
        if (cpi != null) {
          cpis.addElement(cpi);
        } else {
          out.println("Didn't find ConditionalPermissionInfo named: "
                      + names[i]);
        }
      }
      e = cpis.elements();
    } else {
      e = condPermAdmin.getConditionalPermissionInfos();
    }
    while (e.hasMoreElements()) {
      // NYI! pretty print
      out.println(e.nextElement().toString());
    }
    return 0;
  }

  public int cmdSetcondpermission(Dictionary opts,
                                  Reader in,
                                  PrintWriter out,
                                  Session session)
  {
    if (condPermAdmin == null) {
      out.println("Conditional Permission Admin service is not available");
      return 1;
    }
    String loc = null;
    Vector /* ConditionInfo */ cis = new Vector();
    Vector /* PermissionInfo */ pis = new Vector();
    String name = (String) opts.get("-name");
    String [] cpis = (String []) opts.get("conditional_permission_info");
    String endChar = null;
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < cpis.length; ) {
      String cpi = cpis[i];
      if (endChar != null) {
        buf.append(cpi);
        i++;
        if (cpi.endsWith(endChar)) {
          try {
            if (endChar == "]") {
              cis.addElement(new ConditionInfo(buf.toString()));
            } else {
              pis.addElement(new PermissionInfo(buf.toString()));
            }
          } catch (IllegalArgumentException e) {
            out.println("ERROR! Failed to instanciate: " + buf.toString()
                + " " + e.getMessage());
            return 1;
          }
          endChar = null;
          buf.setLength(0);
        } else {
          buf.append(' ');
        }
      } else if (cpi.startsWith("[")) {
        endChar = "]";
      } else if (cpi.startsWith("(")) {
        endChar = ")";
      } else {
        out.println("ERROR! Expected start char '(' or '[', got: " + cpi);
        return 1;
      }
    }
    ConditionInfo [] cia = (ConditionInfo []) cis.toArray(new ConditionInfo [cis.size()]);
    PermissionInfo [] pia = (PermissionInfo []) pis.toArray(new PermissionInfo [pis.size()]);
    condPermAdmin.setConditionalPermissionInfo(name, cia, pia);
    return 0;
  }

  private void showPerms(PrintWriter out, PermissionInfo[] pi) {
    final String shift = "    ";
    if (pi == null) {
      out.println(shift + "DEFAULT");
    } else if (pi.length == 0) {
      out.println(shift + "NONE");
    } else {
      for (int i = 0; i < pi.length; i++) {
        out.println(shift + pi[i]);
      }
    }
  }

}
