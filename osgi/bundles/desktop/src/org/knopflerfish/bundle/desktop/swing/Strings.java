/*
 * Copyright (c) 2003-2022, KNOPFLERFISH project
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for string localization.
 *
 * Conventions:
 * <pre>
 * button_xx      String for xx to be placed on a graphical button
 * menu_xx        String for xx to be placed in a window menu
 * tooltip_xx     String for tool-tip xx text
 * str_xx         Generic string for xx
 * </pre>
 */
public class Strings {

  /**
   * Lookup table for strings.
   */
  private static Map<String, String> strings;

  /**
   * Initialize strings map.
   */
  static {
    strings = new HashMap<String, String>() {
        private static final long serialVersionUID = 1L;
        {
          put("frame_title",       "Knopflerfish OSGi desktop ($(2) $(1))");
          put("menu_quit",         "Quit");
          put("menu_openbundles",  "Open Bundle File...");
          put("menu_save",         "Save Deploy Archive...");
          put("menu_file",         "File");
          put("menu_edit",         "Edit");
          put("menu_view",         "View");
          put("menu_bundles",      "Bundles");
          put("menu_about",        "About");
          put("menu_help",         "Help");
          put("menu_view_bundles",   "Bundles");
          put("menu_view_console",   "Console");
          put("menu_view_info",      "Bundle Info");
          put("menu_view_toolbar",   "Toolbar");
          put("menu_view_statusbar", "Statusbar");
          put("menu_view_log",       "Log Window");
          put("menu_lookandfeel",    "Look and Feel");
          put("menu_errordialog",     "Error Dialog");
          put("menu_errordialog_use", "Use Error Dialog");
          put("menu_errordialoglevel",          "Default Display");
          put("menu_errordialoglevel_normal",   "Normal");
          put("menu_errordialoglevel_more",     "More");
          put("menu_errordialoglevel_advanced", "Advanced");

          put("tt_html_back",        "Back to previous bundle");
          put("tt_html_fwd",         "Forward to next bundle");

          put("menu_tips",      "Show tips");
          put("prev_tip",       "Previous tip");
          put("next_tip",       "Next tip");

          put("item_resolvebundles",    "Resolve Selected Bundles");
          put("item_resolvebundles.descr",
              "Resolves un-resolved selected bundles. If called with no "
              +"selection, try to resolve all un-resolved bundles.");
          put("item_startbundles",      "Start Selected Bundles");
          put("item_stopbundles",       "Stop Selected Bundles");
          put("item_updatebundles",     "Update Selected Bundles");
          put("item_refreshbundles",    "Refresh Selected Bundles");
          put("item_refreshbundles.descr",
              "Refresh bundles re-wires required capabilites for the selected "
              +"bundles, if the selection is empty refresh all bundles that "
              +"are marked as pending removal by the framework.");
          put("item_uninstallbundles",  "Uninstall Selected Bundles");

          put("item_resolvebundle",    "Resolve Bundle ");
          put("item_startbundle",      "Start Bundle ");
          put("item_stopbundle",       "Stop Bundle ");
          put("item_updatebundle",     "Update Bundle ");
          put("item_refreshbundle",    "Refresh Bundle ");
          put("item_refreshbundle.descr",
              "Refresh bundle re-wires required capabilites for the bundle.");
          put("item_uninstallbundle",  "Uninstall Bundle ");

          put("menu_remotefw",          "Remote framework...");
          put("remote_connect_msg",     "Enter address to remote framework");
          put("remote_connect_title",   "Connect to remote framework");

          put("menu_openbundleurl",        "Open Bundle Location...");
          put("dialog_addbundleurl_msg",   "Enter bundle location");
          put("dialog_addbundleurl_title", "Open Bundle Location");

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

          put("str_fwinfo", "Framework info");
          put("str_about", "About");
          put("str_abouttext",
              "Knopflerfish OSGi desktop, version $(1)\n" +
              "Framework: $(2) $(3)\n\n" +
              "$(4)\n\n" +
              "See https://www.knopflerfish.org for more information.\n\n" +
              "This release uses icons from Tango Desktop\n" +
              "Project released to the public domain, see\n" +
              "http://tango.freedesktop.org/Tango_Desktop_Project"
              );

          put("item_selectall",       "Select all");
          put("item_unselectall",     "Unselect all");
          put("item_clear_console",   "Clear console");

          put("menu_startOptions", "     Options");
          put("menu_startOptions.descr","Options to use when starting bundles");
          put("start_option_transient", "Transient");
          put("start_option_transient.descr",
              "Start bundle without marking it as persistently started.");
          put("start_option_eager",     "Eager");
          put("start_option_policy",    "Policy");
          put("start_option_policy.descr",
              "Start bundle according to its start policy."
              +" If unchecked the bundle will be eagerly started.");

          put("menu_stopOptions", "     Options");
          put("menu_stopOptions.descr","Options to use when stoping bundles");
          put("stop_option_transient", "Transient");
          put("stop_option_transient.descr",
              "Stop bundle without marking it as not persistently started.");

          put("startlevel.label",     "Start level: ");
          put("startlevel.label.descr", "Change the active start level of the Framework.");
          put("startlevel.descr",  "Sets startlevel for the selected bundle.");
          put("startlevel.noSel",  "No bundle selected");
          put("menu_startlevel",      "Start level");
          put("nostartlevel.label",   "");

          put("str_checkupdate",      "Check for update");
          put("item_newwindow",      "New window");
          put("str_services",      "Services");
          put("str_packages",      "Packages");
          put("item_newgraphwindow",      "New graph window");
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
      return strings.get(key);
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

  public static String fmt(String key, Object arg1, Object arg2, Object arg3) {
    return replace(fmt(key, arg1, arg2), "$(3)",
                   arg3 != null ? arg3.toString() : "null");
  }

  public static String fmt(String key, Object arg1, Object arg2, Object arg3, Object arg4) {
    return replace(fmt(key, arg1, arg2, arg3), "$(4)",
                   arg4 != null ? arg4.toString() : "null");
  }

/**
   * Replace occurrences of substrings.
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
    final int v1Len    = v1.length();
    int n        = 0;

    // count number of occurrences to be able to correctly size
    // the resulting output char array
    while(-1 != (ix = s.indexOf(v1, ix))) {
      n++;
      ix += v1Len;
    }

    // No occurrences at all, just return source string
    if(n == 0) {
      return s;
    }

    // Set up an output char array of correct size
    int     start  = 0;
    final int     v2Len  = v2.length();
    final char[]  r      = new char[s.length() + n * (v2Len - v1Len)];
    int     rPos   = 0;

    // for each occurrence, copy v2 where v1 used to be
    while(-1 != (ix = s.indexOf(v1, start))) {
      while(start < ix) {
        r[rPos++] = s.charAt(start++);
      }
      for(int j = 0; j < v2Len; j++) {
        r[rPos++] = v2.charAt(j);
      }
      start += v1Len;
    }

    // ...and add all remaining chars
    ix = s.length();
    while(start < ix) {
      r[rPos++] = s.charAt(start++);
    }

    // ..ouch. this hurts.
    return new String(r);
  }

  /**
   * Group string in words, replace the word separator with a given
   * replacement string.
   * <p>
   * Citation chars may be used to group words with embedded word separators.
   * </p>
   *
   * @param s
   *            String to transform.
   * @param separators
   *            separators to replace. Any character in separators are
   *            treated as a word separator.
   * @param replacement
   *            the string to replace the word separator with.
   * @param citChar
   *            Citation character used for grouping words with embedded
   *            separators. Typically '"'.
   */
  public static String replaceWordSep(String s,
                                      String separators,
                                      String replacement,
                                      char citChar)
  {
    boolean bCit = false; // true when inside citation chars.
    boolean bSep = false; // true when inside separator chars.
    final StringBuilder buf = new StringBuilder();
    int i = 0;

    while (i < s.length()) {
      final char c = s.charAt(i);

      if (bCit || separators.indexOf(c) == -1) {
        // Build up word until we breaks on either a citation char or
        // separator
        if (c == citChar) {
          bCit = !bCit;
          buf.append(c);
        } else {
          if (bSep) {
            buf.append(replacement);
            bSep = !bSep;
          }
          buf.append(c);
        }
        i++;
      } else {
        // found separator outside of citation
        bSep = true;

        // and skip separators so we start clean on a word or citation
        // char
        while ((i < s.length())
               && (-1 != separators.indexOf(s.charAt(i)))) {
          i++;
        }
      }
    }
    return buf.toString();
  }

  /**
   * Split string in words, given word separator chars.
   * Citation chars may be used to group words with embedded word separators.
   *
   * @param s
   *            String to transform.
   * @param separators
   *            separators to split at. Any character in separators are
   *            treated as a word separator.
   * @param citChar
   *            Citation character used for grouping words with embedded
   *            separators. Typically '"'.
   */
  public static List<String> splitWordSep(final String s,
                                          final String separators,
                                          final char citChar)
  {
    final ArrayList<String> res = new ArrayList<String>();
    final StringBuilder buf = new StringBuilder(s.length());
    boolean bCit = false; // true when inside citation chars.
    boolean bSep = false; // true when inside separator chars.
    int i = 0;

    while (i < s.length()) {
      final char c = s.charAt(i);

      if (bCit || separators.indexOf(c) == -1) {
        // Build up word until we breaks on either a citation char or
        // separator
        if (c == citChar) {
          bCit = !bCit;
          buf.append(c);
        } else {
          if (bSep) {
            res.add(buf.toString());
            buf.setLength(0);
            bSep = !bSep;
          }
          buf.append(c);
        }
        i++;
      } else {
        // found separator outside of citation
        bSep = true;

        // and skip separators so we start clean on a word or citation
        // char
        while ((i < s.length())
               && (-1 != separators.indexOf(s.charAt(i)))) {
          i++;
        }
      }
    }
    res.add(buf.toString());
    buf.setLength(0);
    return res;
  }
}
