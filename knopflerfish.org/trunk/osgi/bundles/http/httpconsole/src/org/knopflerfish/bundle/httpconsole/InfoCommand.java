/*
 * Copyright (c) 2003, KNOPFLERFISH project
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
  }

  public StringBuffer run(HttpServletRequest request) {
    StringBuffer sb = new StringBuffer();

    long[] bids = Util.getBundleIds(request);

    sb.append("<div class=\"shadow\">Info</div>");
    if(bids.length == 0) {
      sb.append("<div class=\"shadow\">Framework properties</div>");
      Hashtable props = new Hashtable();
      addProp(props, Constants.FRAMEWORK_VENDOR);
      addProp(props, Constants.FRAMEWORK_VERSION);
      addProp(props, Constants.FRAMEWORK_OS_NAME);
      addProp(props, Constants.FRAMEWORK_OS_VERSION);
      addProp(props, Constants.FRAMEWORK_PROCESSOR);
      addProp(props, Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
      
      printDictionary(props, sb);

      sb.append("<div class=\"shadow\">System properties</div>");
      printDictionary(System.getProperties(), sb);
      
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
	printDictionary(headers, sb);
	
      }
    }

    return sb;
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
