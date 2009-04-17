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

package org.knopflerfish.bundle.desktop.swing;

import java.util.Hashtable;
import java.util.Dictionary;
import java.util.Enumeration;

/**
 * Utility class for string localization.
 *
 * Conventions:
 * <pre>
 * button_xx      String for xx to be placed on a graphical button 
 * menu_xx        String for xx to be placed in a window menu
 * tooltip_xx     String for tooltip xx text
 * str_xx         Generic string for xx
 * </pre>
 */
public class Strings {
  
  /**
   * Lookup table for strings.
   */
  private static Hashtable strings;

  /**
   * Initialize strings hashtable.
   */
  static {
    strings = new Hashtable() {
	{
	  put("frame_title",       "Knopflerfish OSGi desktop ($(2) $(1))");
	  put("menu_quit",         "Quit");
	  put("menu_openbundles",  "Open bundle(s)...");
	  put("menu_save",         "Save deploy archive...");
	  put("menu_file",         "File");
	  put("menu_edit",         "Edit");
	  put("menu_view",         "View");
	  put("menu_bundles",      "Bundles");
	  put("menu_about",        "About");
	  put("menu_help",         "Help");
	  put("menu_view_bundles",   "Bundles");
	  put("menu_view_console",   "Console");
	  put("menu_view_info",      "Bundle info");
	  put("menu_view_toolbar",   "Toolbar");
	  put("menu_view_statusbar", "Statusbar");
	  put("menu_view_log",       "Log window");
	  put("menu_lookandfeel",    "Look and Feel");
	  put("tt_html_back",        "Back to previous bundle");
	  put("tt_html_fwd",         "Forward to next bundle");
	  put("tt_stopbundle",       "Stop Bundle");
	  put("tt_startbundle",      "Start Bundle");
	  put("tt_updatebundle",     "Update Bundle");
	  put("tt_uninstallbundle",  "Uninstall Bundle");

	  put("menu_tips",      "Show tips");
	  put("prev_tip",       "Previous tip");
	  put("next_tip",       "Next tip");

	  put("item_stopbundles",       "Stop");
	  put("item_startbundles",      "Start");
	  put("item_updatebundles",     "Update");
	  put("item_uninstallbundles",  "Uninstall");

	  put("menu_remotefw",          "Remote framework...");
	  put("remote_connect_msg",     "Enter address to remote framework");
	  put("remote_connect_title",   "Connect to remote framework");

	  put("menu_openbundleurl",        "Add bundle URL...");
	  put("dialog_addbundleurl_msg",   "Enter bundle URL");
	  put("dialog_addbundleurl_title", "Add bundle URL");

	  put("bundleinfo_startup", 
	      "<html>Select on or more bundles to " + 
	      "view detail information</html>");

	  put("q_stopframework",     
	      "Are you sure you want stop the entire framework?\n" + 
	      "All bundles will be stopped.");
	  
	  put("msg_stopframework",   "Stop framework");
	  
	  put("q_stopdesktop",     
	      "Do you want to stop the desktop bundle?\n" + 
	      "This will also close the desktop window.");
	  
	  put("msg_stopdesktop",   "Stop desktop");

	  put("cancel", "Cancel");
	  put("yes",    "Yes");
	  put("no",     "No");
	  put("ok",     "OK");
	  put("close",  "Close");

	  put("local",  "Local");

	  put("fmt_q_stopdesktop",
	      "Are you sure you want to stop $(1)?\n" + 
	      "This will close the desktop window.");
	  

	  put("q_uninstallbundle",
	      "Are you sure you want to uninstall the bundle $(1)?");


	  put("msg_uninstallbundle", "Uninstall bundle");

	  put("menu_refreshbundles", "Refresh bundle packages");
	  
	  put("str_fwinfo", "Framework info");
	  put("str_about", "About");
	  put("str_abouttext", 
	      "Knopflerfish OSGi desktop, version $(1)\n" + 
	      "(c) 2004 Knopflerfish.\n\n" + 
	      "See\n" + 
	      "  http://www.knopflerfish.org\n" + 
	      "for more information" + 
	      "\n\n" + 
	      "This release uses LGPL icons from the\n" +
	      "Noia icon collection by Carlitus, see\n" +
	      " http://www.carlitus.net"
	      );

	  put("item_selectall",       "Select all");
	  put("item_unselectall",     "Unselect all");
	  put("item_clear_console",   "Clear console");

	  put("startlevel.label",     "Start level: ");
	  put("menu_startlevel",      "Start level");
	  put("nostartlevel.label",   "");
	}
      };
  }

  /**
   * Get a string given a key.
   *
   * @param key Key to lookup
   * @return if key is present return matching value, otherwise return key
   */
  public static String get(String key) {
    if(strings.containsKey(key)) {
      return (String)strings.get(key);
    } else {
      return key;
    }
  }

  /**
   * Format a string with argument.
   *
   * @param key Key to lookup using <code>get(key)<code>. 
   *            The resulting string is used as source string for 
   *            argument substitution.
   *            <code>$(1)</code> is replaced with <code>arg1</code>
   * @param arg1 Replacement string.
   */
  public static String fmt(String key, Object arg1) {
    return replace(get(key), "$(1)", 
		   arg1 != null ? arg1.toString() : "null");
  }

  /**
   * Format a string with arguments.
   *
   * @param key Key to lookup using <code>get(key)<code>. 
   *            The resulting string is used as source string for 
   *            argument substitution.
   *            <code>$(1)</code> is replaced with <code>arg1</code><br>
   *            <code>$(2)</code> is replaced with <code>arg2</code>
   * @param arg1 Replacement string.
   * @param arg2 Replacement string.
   */
  public static String fmt(String key, Object arg1, Object arg2) {
    return replace(fmt(key, arg1), "$(2)", 
		   arg2 != null ? arg2.toString() : "null");
  }


/**
   * Replace occurances of substrings.
   *
   * @param s  Source string.
   * @param v1 String to be replaced with <code>v2</code>.
   * @param v2 String replacing <code>v1</code>. 
   * @return Modified string. If any of the input strings are <tt>null</tt>,
   *         the source string will be returned unmodified.
   */
  public static String replace(final String s, 
			       final String v1, 
			       final String v2) {
    
    // return quick when nothing to do
    if(s == null 
       || v1 == null 
       || v2 == null 
       || v1.length() == 0 
       || v1.equals(v2)) {
      return s;
    }

    int ix       = 0;
    int v1Len    = v1.length(); 
    int n        = 0;

    // count number of occurances to be able to correctly size
    // the resulting output char array
    while(-1 != (ix = s.indexOf(v1, ix))) {
      n++;
      ix += v1Len;
    }

    // No occurances at all, just return source string
    if(n == 0) {
      return s;
    }

    // Set up an output char array of correct size
    int     start  = 0;
    int     v2Len  = v2.length();
    char[]  r      = new char[s.length() + n * (v2Len - v1Len)];
    int     rPos   = 0;

    // for each occurance, copy v2 where v1 used to be
    while(-1 != (ix = s.indexOf(v1, start))) {
      while(start < ix) r[rPos++] = s.charAt(start++);
      for(int j = 0; j < v2Len; j++) {
	r[rPos++] = v2.charAt(j);
      }
      start += v1Len;
    }

    // ...and add all remaining chars
    ix = s.length(); 
    while(start < ix) r[rPos++] = s.charAt(start++);
    
    // ..ouch. this hurts.
    return new String(r);
  }


}
