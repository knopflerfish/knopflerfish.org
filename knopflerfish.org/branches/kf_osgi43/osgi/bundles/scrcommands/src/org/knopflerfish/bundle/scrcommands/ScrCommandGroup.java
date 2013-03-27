/*
 * Copyright (c) 2011-2013, KNOPFLERFISH project
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

package org.knopflerfish.bundle.scrcommands;

import java.io.PrintWriter;
import java.io.Reader;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.Reference;
import org.apache.felix.scr.ScrService;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import org.knopflerfish.service.console.CommandGroupAdapter;
import org.knopflerfish.service.console.Session;
import org.knopflerfish.service.console.Util;

/**
 * SCR Commands to be handled by the console.
 *
 */
public class ScrCommandGroup
  extends CommandGroupAdapter
{

  final private static int FORMAT_BRIEF = 0;
  final private static int FORMAT_DYNAMIC = 1;
  final private static int FORMAT_FULL = 2;

  private ScrService scrService = null;
  private BundleContext bc;

  /**
   * Comparator for sorting components in bundle ID order.
   */
  private class OrderBundle
    implements Comparator<Component>
  {
    public int compare(Component c1, Component c2)
    {
      final long bid1 = c1.getBundle().getBundleId();
      final long bid2 = c2.getBundle().getBundleId();
      if (bid1 < bid2) {
        return -1;
      } else if (bid1 == bid2) {
        final long id1 = c1.getId();
        final long id2 = c2.getId();
        if (id1 < id2) {
          return -1;
        } else if (id1 == id2) {
          return 0;
        } else {
          return 1;
        }
      } else {
        return 1;
      }
    }
  }

  /**
   * Comparator for sorting components in component ID order.
   */
  private class OrderId
    implements Comparator<Component>
  {
    public int compare(Component c1, Component c2)
    {
      final long id1 = c1.getId();
      final long id2 = c2.getId();
      final long bid1 = c1.getBundle().getBundleId();
      final long bid2 = c2.getBundle().getBundleId();
      if (id1 < id2) {
        return -1;
      } else if (id1 == id2) {
        if (bid1 < bid2) {
          return -1;
        } else if (bid1 == bid2) {
          return 0;
        } else {
          return 1;
        }
      } else {
        return 1;
      }
    }
  }

  /**
   * Comparator for sorting components in component name order.
   */
  private class OrderName
    implements Comparator<Component>
  {
    public int compare(Component c1, Component c2)
    {
      final int res = c1.getName().compareTo(c2.getName());
      if (res == 0) {
        final long id1 = c1.getId();
        final long id2 = c2.getId();
        if (id1 < id2) {
          return -1;
        } else if (id1 == id2) {
          return 0;
        } else {
          return 1;
        }
      }
      return res;
    }
  }

  /*
   * Default constructor used by SCR.
   */
  public ScrCommandGroup()
  {
    super("scr", "SCR commands");
  }

  /*
   * Component activate
   */
  protected void activate(BundleContext bc, ComponentContext cc)
  {
    this.bc = bc;
    scrService = (ScrService) cc.locateService("ScrService");
  }

  /*
   * Component deactivate
   */
  protected void deactivate()
  {
    scrService = null;
  }

  //
  // Disable command
  //

  public final static String USAGE_DISABLE = "<component> ...";

  public final static String[] HELP_DISABLE =
    new String[] { "Disable specified component(s)",
                  "<componentId>   Id or name of component" };

  public int cmdDisable(final Dictionary<String, ?> opts,
                        final Reader in,
                        final PrintWriter out,
                        final Session session)
  {
    final ScrService scr = scrService;
    int failed = 0;
    if (scr == null) {
      out.println("SCR commands are currently inactive");
      return 1;
    }
    final String[] cids = (String[]) opts.get("component");
    for (final String cid : cids) {
      final Component[] components = getComponents(scr, cid, out);
      if (components != null) {
        for (final Component component : components) {
          component.disable();
        }
      } else {
        failed++;
      }
    }
    return failed > 0 ? 1 : 0;
  }

  //
  // Enable command
  //

  public final static String USAGE_ENABLE = "<component> ...";

  public final static String[] HELP_ENABLE =
    new String[] { "Enable specified component(s)",
                  "<component>   Id or name of component" };

  public int cmdEnable(final Dictionary<String, ?> opts,
                       final Reader in,
                       final PrintWriter out,
                       final Session session)
  {
    final ScrService scr = scrService;
    int failed = 0;
    if (scr == null) {
      out.println("SCR commands are currently inactive");
      return 1;
    }
    final String[] cids = (String[]) opts.get("component");
    for (final String cid : cids) {
      final Component[] components = getComponents(scr, cid, out);
      if (components != null) {
        for (final Component component : components) {
          component.enable();
        }
      } else {
        failed++;
      }
    }
    return failed > 0 ? 1 : 0;
  }

  //
  // List command
  //

  public final static String USAGE_LIST =
    "[-i] [-l] [-n] [-r] [-s] [-u] [<bundle>] ...";

  public final static String[] HELP_LIST =
    new String[] {
                  "List all components for specified bundles.",
                  "If no bundle parameters are given show all components",
                  "Components are shown in bundle id order if no order parameter is given.",
                  "-i         List components in ID order",
                  "-l         Show long version of information",
                  "-n         List components in name order",
                  "-r         List components in reverse order",
                  "-s         Only list satisfied components",
                  "-u         Only list unsatisfied components",
                  "<bundle>   Name or id of bundle" };

  public int cmdList(final Dictionary<String, ?> opts,
                     final Reader in,
                     final PrintWriter out,
                     final Session session)
  {
    final ScrService scr = scrService;
    if (scr == null) {
      out.println("SCR commands are currently inactive");
      return 1;
    }
    final String[] selection = (String[]) opts.get("bundle");
    Comparator<Component> order;
    if (opts.get("-i") != null) {
      order = new OrderId();
    } else if (opts.get("-n") != null) {
      order = new OrderName();
    } else {
      order = new OrderBundle();
    }
    boolean showSatisfied = opts.get("-s") != null;
    boolean showUnsatisfied = opts.get("-u") != null;
    if (!showSatisfied && !showUnsatisfied) {
      showSatisfied = true;
      showUnsatisfied = true;
    }
    final TreeSet<Component> comps = new TreeSet<Component>(order);
    if (selection != null) {
      final Bundle[] b = bc.getBundles();
      Util.selectBundles(b, selection);
      for (int i = 0; i < b.length; i++) {
        if (b[i] != null) {
          final Component[] cs = scr.getComponents(b[i]);
          if (cs != null) {
            for (final Component element : cs) {
              if (isSatisfied(cs[i])) {
                if (!showSatisfied) {
                  continue;
                }
              } else {
                if (!showUnsatisfied) {
                  continue;
                }
              }
              comps.add(element);
            }
          }
        }
      }
    } else {
      final Component[] cs = scr.getComponents();
      if (cs != null) {
        for (final Component element : cs) {
          if (isSatisfied(element)) {
            if (!showSatisfied) {
              continue;
            }
          } else {
            if (!showUnsatisfied) {
              continue;
            }
          }
          comps.add(element);
        }
      }
    }
    final int format = opts.get("-l") != null ? FORMAT_DYNAMIC : FORMAT_BRIEF;
    final boolean reverse = opts.get("-r") != null;
    showInfo(comps, format, reverse, out);
    return 0;
  }

  private boolean isSatisfied(Component c)
  {
    return (c.getState() & (Component.STATE_ACTIVATING | Component.STATE_ACTIVE
                            | Component.STATE_REGISTERED | Component.STATE_FACTORY)) != 0;
  }

  //
  // Show command
  //

  public final static String USAGE_SHOW =
    "[-b] [-f] [-n] [-r] [<component>] ...";

  public final static String[] HELP_SHOW =
    new String[] {
                  "Show all information about specified component(s).",
                  "If no component id parameters are given show all components.",
                  "Components are shown in component id order if no order parameter is given.",
                  "-b           Show components in bundle id order",
                  "-f           Show full information about components, adds static info",
                  "-n           Show components in component name order",
                  "-r           Show components in reverse order",
                  "<component>  Id of component" };

  public int cmdShow(final Dictionary<String, ?> opts,
                     final Reader in,
                     final PrintWriter out,
                     final Session session)
  {
    final ScrService scr = scrService;
    int failed = 0;
    if (scr == null) {
      out.println("SCR commands are currently inactive");
      return 1;
    }
    Comparator<Component> order;
    if (opts.get("-b") != null) {
      order = new OrderBundle();
    } else if (opts.get("-n") != null) {
      order = new OrderName();
    } else {
      order = new OrderId();
    }
    final TreeSet<Component> comps = new TreeSet<Component>(order);
    final String[] cids = (String[]) opts.get("component");
    if (cids != null) {
      for (final String cid : cids) {
        final Component[] components = getComponents(scr, cid, out);
        if (components != null) {
          for (final Component component : components) {
            comps.add(component);
          }
        } else {
          failed++;
        }
      }
    } else {
      final Component[] cs = scr.getComponents();
      if (cs != null) {
        for (final Component element : cs) {
          comps.add(element);
        }
      }
    }
    final int format = opts.get("-f") != null ? FORMAT_FULL : FORMAT_DYNAMIC;
    final boolean reverse = opts.get("-r") != null;
    showInfo(comps, format, reverse, out);

    return failed > 0 ? 1 : 0;
  }

  /*
   * Get component by name or id
   */
  private Component[] getComponents(ScrService scr, String cid, PrintWriter out)
  {
    Component[] components = null;
    try {
      final long id = Long.parseLong(cid);
      final Component c = scr.getComponent(id);
      if (c != null) {
        components = new Component[] { c };
      }
    } catch (final NumberFormatException nfe) {
    }
    if (components == null) {
      components = scr.getComponents(cid);
      if (components == null) {
        out.println("Did not find any component with id or name: " + cid);
      }
    }
    return components;
  }

  /*
   * Show information about component.
   */
  private void showInfo(TreeSet<Component> comps,
                        int format,
                        boolean reverse,
                        PrintWriter out)
  {
    int lenId = 2;
    int lenBid = 3;
    for (final Object element : comps) {
      final Component c = (Component) element;
      int tmp = Long.toString(c.getId()).length();
      if (tmp > lenId) {
        lenId = tmp;
      }
      tmp = Long.toString(c.getBundle().getBundleId()).length();
      if (tmp > lenBid) {
        lenBid = tmp;
      }
    }
    final StringBuffer sb = new StringBuffer();
    sb.append(Util.showRight(lenId, "ID"));
    sb.append(" State        ");
    sb.append(Util.showRight(lenBid, "BID"));
    sb.append(" Name");
    out.println(sb.toString());
    Iterator<Component> ci = comps.iterator();
    if (reverse) {
      final LinkedList<Component> rev = new LinkedList<Component>();
      while (ci.hasNext()) {
        rev.addFirst(ci.next());
      }
      ci = rev.iterator();
    }
    while (ci.hasNext()) {
      final Component c = ci.next();
      sb.setLength(0);
      sb.append(Util.showRight(lenId, Long.toString(c.getId())));
      sb.append(" ");
      String state;
      switch (c.getState()) {
      case Component.STATE_DISABLED:
        state = "DISABLED";
        break;
      case Component.STATE_ENABLING:
        state = "ENABLING";
        break;
      case Component.STATE_ENABLED:
        state = "ENABLED";
        break;
      case Component.STATE_UNSATISFIED:
        state = "UNSATISFIED";
        break;
      case Component.STATE_ACTIVATING:
        state = "ACTIVATING";
        break;
      case Component.STATE_ACTIVE:
        state = "ACTIVE";
        break;
      case Component.STATE_REGISTERED:
        state = "REGISTERED";
        break;
      case Component.STATE_FACTORY:
        state = "FACTORY";
        break;
      case Component.STATE_DEACTIVATING:
        state = "DEACTIVATING";
        break;
      case Component.STATE_DISABLING:
        state = "DISABLING";
        break;
      case Component.STATE_DISPOSING:
        state = "DISPOSING";
        break;
      case Component.STATE_DISPOSED:
        state = "DISPOSED";
        break;
      default:
        state = "UNKNOWN";
        break;
      }
      sb.append(Util.showLeft(13, state));
      sb.append(Util.showRight(lenBid,
                               Long.toString(c.getBundle().getBundleId())));
      sb.append(" ");
      sb.append(c.getName());
      out.println(sb.toString());
      sb.setLength(0);
      sb.append(Util.showRight(lenId, ""));
      sb.append(" > ");
      final int baseLen = sb.length();
      if (format >= FORMAT_DYNAMIC) {
        final String[] services = c.getServices();
        if (services != null) {
          for (final String service : services) {
            sb.setLength(baseLen);
            sb.append("Service: ");
            sb.append(service);
            if (format == FORMAT_FULL && c.isServiceFactory()) {
              sb.append(" (ServiceFactory)");
            }
            out.println(sb.toString());
          }
        } else {
          sb.setLength(baseLen);
          sb.append("No services provided.");
          out.println(sb.toString());
        }
        final Reference[] refs = c.getReferences();
        if (refs != null) {
          for (final Reference ref : refs) {
            sb.setLength(baseLen);
            sb.append(ref.isSatisfied()
              ? "Satisfied reference:   "
              : "Unsatisfied reference: ");
            sb.append(ref.getServiceName());
            if (format == FORMAT_FULL) {
              sb.append(ref.isOptional() ? " [0." : " [1.");
              sb.append(ref.isMultiple() ? ".n]" : ".1]");
              if (ref.isStatic()) {
                sb.append(", static bind");
              } else {
                sb.append(", dynamic bind");
              }
            }
            out.println(sb.toString());
          }
        } else {
          sb.setLength(baseLen);
          sb.append("No referenced services");
          out.println(sb.toString());
        }
        @SuppressWarnings("unchecked")
        final Dictionary<String, ?> props = c.getProperties();
        final TreeSet<String> keys = new TreeSet<String>();
        for (final Enumeration<String> e = props.keys(); e.hasMoreElements();) {
          keys.add(e.nextElement());
        }
        for (final String key : keys) {
          sb.setLength(baseLen);
          sb.append("Property " + key + " = ");
          final Object val = props.get(key);
          if (val.getClass().isArray()) {
            sb.append("[");
            final Class<?> ct = val.getClass().getComponentType();
            if (ct == Boolean.TYPE) {
              final boolean[] vals = (boolean[]) val;
              for (int i = 0; i < vals.length; i++) {
                if (i > 0) {
                  sb.append(", ");
                }
                sb.append(Boolean.toString(vals[i]));
              }
            } else if (ct == Byte.TYPE) {
              final byte[] vals = (byte[]) val;
              for (int i = 0; i < vals.length; i++) {
                if (i > 0) {
                  sb.append(", ");
                }
                sb.append(Byte.toString(vals[i]));
              }
            } else if (ct == Character.TYPE) {
              final char[] vals = (char[]) val;
              for (int i = 0; i < vals.length; i++) {
                if (i > 0) {
                  sb.append(", ");
                }
                sb.append(Character.toString(vals[i]));
              }
            } else if (ct == Double.TYPE) {
              final double[] vals = (double[]) val;
              for (int i = 0; i < vals.length; i++) {
                if (i > 0) {
                  sb.append(", ");
                }
                sb.append(Double.toString(vals[i]));
              }
            } else if (ct == Float.TYPE) {
              final float[] vals = (float[]) val;
              for (int i = 0; i < vals.length; i++) {
                if (i > 0) {
                  sb.append(", ");
                }
                sb.append(Float.toString(vals[i]));
              }
            } else if (ct == Integer.TYPE) {
              final int[] vals = (int[]) val;
              for (int i = 0; i < vals.length; i++) {
                if (i > 0) {
                  sb.append(", ");
                }
                sb.append(Integer.toString(vals[i]));
              }
            } else if (ct == Long.TYPE) {
              final long[] vals = (long[]) val;
              for (int i = 0; i < vals.length; i++) {
                if (i > 0) {
                  sb.append(", ");
                }
                sb.append(Long.toString(vals[i]));
              }
            } else if (ct == Short.TYPE) {
              final short[] vals = (short[]) val;
              for (int i = 0; i < vals.length; i++) {
                if (i > 0) {
                  sb.append(", ");
                }
                sb.append(Short.toString(vals[i]));
              }
            } else {
              final Object[] vals = (Object[]) val;
              for (int i = 0; i < vals.length; i++) {
                if (i > 0) {
                  sb.append(", ");
                }
                sb.append(vals[i].toString());
              }
            }
            sb.append("]");
          } else {
            sb.append(val.toString());
          }
          out.println(sb.toString());
        }
      }
      if (format == FORMAT_FULL) {
        sb.setLength(baseLen);
        if (c.isImmediate()) {
          sb.append("Immediate component");
        } else {
          final String factory = c.getFactory();
          if (factory != null) {
            sb.append("Factory component, name = " + factory);
          } else {
            sb.append("Delayed component");
          }
        }
        if (c.isDefaultEnabled()) {
          sb.append(", default enabled");
        }
        sb.append(", config policy = " + c.getConfigurationPolicy());
        out.println(sb.toString());
      }
    }
  }

}
