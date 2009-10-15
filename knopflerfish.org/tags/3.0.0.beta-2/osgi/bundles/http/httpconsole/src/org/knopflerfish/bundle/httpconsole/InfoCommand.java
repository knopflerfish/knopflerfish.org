/*
 * Copyright (c) 2003-2008, KNOPFLERFISH project
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

package org.knopflerfish.bundle.httpconsole;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;

import org.osgi.framework.*;

public class InfoCommand extends IconCommand {
  public InfoCommand() {
    super("cmd_info",
          "Info",
          "Show info for selected bundles",
          Activator.RES_ALIAS + "/info.gif");

    displayFlags = DISPLAY_COMPACTLIST;
  }

  public StringBuffer run(HttpServletRequest request) {
    StringBuffer sb = new StringBuffer();

    long[] bids = Util.getBundleIds(request);

    //    sb.append("<div class=\"shadow\">Info</div>");
    if(bids.length == 0) {
      sb.append("<div class=\"shadow\">Framework properties</div>");
      Hashtable props = new Hashtable();
      addProp(props, Constants.FRAMEWORK_VENDOR);
      addProp(props, Constants.FRAMEWORK_VERSION);
      addProp(props, Constants.FRAMEWORK_OS_NAME);
      addProp(props, Constants.FRAMEWORK_OS_VERSION);
      addProp(props, Constants.FRAMEWORK_PROCESSOR);
      addProp(props, Constants.FRAMEWORK_EXECUTIONENVIRONMENT);

      // There is no way to access the key-set of the framework
      // properties in OSGi R4; use the key set of the system
      // properties as a substitute.
      Dictionary sysProps = System.getProperties();
      for(Enumeration e = sysProps.keys(); e.hasMoreElements(); ) {
        addProp(props, (String) e.nextElement());
      }
      printDictionary(props, sb);

      sb.append("<div class=\"shadow\">System properties</div>");
      printDictionary(sysProps, sb);

    } else {

      for(int i = 0; i < bids.length; i++) {
        Bundle b = Activator.bc.getBundle(bids[i]);
        sb.append("<div class=\"shadow\">");
        sb.append("#" + bids[i] +
                  " " + Util.getName(b) +
                  ", " + Util.getStateString(b.getState()));
        sb.append("</div>");

        Dictionary headers = b.getHeaders();

        sb.append("Location: " + b.getLocation());

        sb.append("<div class=\"shadow\">Manifest</div>");
        printDictionary(headers, sb);

        printServices(sb,
                      "Registered services",
                      b.getRegisteredServices());

        printServices(sb,
                      "Used services",
                      b.getServicesInUse());

      }
    }

    return sb;
  }

  void printServices(StringBuffer sb,
                     String header,
                     ServiceReference[] srl) {

    if(srl == null || srl.length == 0) {
      return;
    }
    sb.append("<div class=\"shadow\">" + header + "</div>");

    sb.append("<table>\n");
    sb.append(" <tr>\n");
    sb.append("  <th>Class</th>");
    sb.append("  <th>Used by</th>");
    sb.append(" </tr>\n");

    for(int i = 0; srl != null && i < srl.length; i++) {
      sb.append(" <tr>\n");

      sb.append("  <td style=\"background: #efefef;\">");
      try {
        StringWriter sw = new StringWriter();
        Util.printObject(new PrintWriter(sw), srl[i].getProperty("objectclass"));

        sb.append("<a href=\"" + Activator.SERVLET_ALIAS +
                  "?service.id=" + srl[i].getProperty("service.id") +
                  "\">");
        sb.append(sw.toString());
        sb.append("</a>");
      } catch (Exception e) {
        sb.append(Util.toHTML(e));
      }
      sb.append("</td>");

      sb.append("  <td style=\"background: #efefef;\">");
      Bundle[] using = findUsingBundles(srl[i]);

      for(int j = 0; j < using.length; j++) {
        sb.append(Util.infoLink(using[j]));
        sb.append(Util.getName(using[j]));
        sb.append("</a><br/>");
      }
      sb.append("  </td>");

      sb.append(" </tr>\n");
    }
    sb.append("</table>\n");
  }

  Bundle[] findUsingBundles(ServiceReference sr) {
    Bundle[] bundles = Activator.bc.getBundles();
    Hashtable set = new Hashtable();
    for(int i = 0; i < bundles.length; i++) {
      Bundle[] using = sr.getUsingBundles();
      for(int j = 0; using != null && j < using.length; j++) {
        if(bundles[i] == using[j]) {
          set.put(bundles[i], "");
        }
      }
    }

    Bundle[] r = new Bundle[set.size()];
    int n = 0;
    for(Enumeration e = set.keys(); e.hasMoreElements(); ) {
      r[n++] = (Bundle)e.nextElement();
    }

    return r;
  }

  void addProp(Hashtable props, String key) {
    props.put(key, Activator.bc.getProperty(key));
  }

  void printDictionary(Dictionary dict, StringBuffer sb) {
    Vector keys = new Vector();
    for(Enumeration e = dict.keys(); e.hasMoreElements(); ) {
      keys.addElement(e.nextElement());
    }
    Util.sort(keys, Util.stringComparator, false);

    sb.append("<table width=\"200\">");
    for(int i = 0; i < keys.size(); i++) {
      String key = (String)keys.elementAt(i);
      String val = (String)dict.get(key);

      val = Util.replace(val, ",", ", ");
      val = Util.replace(val, "/", "/ ");
      val = Util.replace(val, "\\", "\\ ");

      sb.append("<tr>");
      sb.append("<td>" + key + "</td>");
      sb.append("<td>" + val + "</td>");
      sb.append("</tr>");
    }
    sb.append("</table>");
  }
}
