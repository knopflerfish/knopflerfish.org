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

import java.util.Set;
import java.util.TreeSet;

import javax.swing.JComponent;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class ClosureHTMLDisplayer
  extends DefaultSwingBundleDisplayer
{

  public ClosureHTMLDisplayer(BundleContext bc)
  {
    super(bc, "Closure", "Shows bundle closure", true);

  }

  @Override
  public JComponent newJComponent()
  {
    return new JHTML(this);
  }

  @Override
  public void valueChanged(long bid)
  {
    final Bundle[] bl = Activator.desktop.getSelectedBundles();

    for (final JComponent jComponent : components) {
      final JHTML comp = (JHTML) jComponent;
      comp.valueChanged(bl);
    }
  }

  static class JHTML extends JHTMLBundle
  {
    private static final long serialVersionUID = 1L;

    JHTML(DefaultSwingBundleDisplayer displayer)
    {
      super(displayer);
    }

    @Override
    public void updateView(Bundle[] bl)
    {
      final StringBuilder sb = new StringBuilder("<html>\n");

      if (bl == null || bl.length == 0) {
        setCurrentBID(-1);

        sb.append("<html>\n");
        sb.append("<table border=0>\n");
        sb.append("<tr><td bgcolor=\"#eeeeee\">");
        startFont(sb, "-1");
        sb.append(getNoBundleSelectedHeader());
        sb.append("</font>\n");
        sb.append("</td>\n");
        sb.append("</tr>\n");
        sb.append("</table>\n");

        startFont(sb);
        sb.append(getNoBundleSelectedText());
        sb.append("</font>\n" + "</p>\n" + "</html>");
      } else {

        setCurrentBID(bl[0].getBundleId());

        sb.append("<table border=0 width=\"100%\">\n");
        sb.append("<tr><td width=\"100%\" bgcolor=\"#eeeeee\">");
        startFont(sb, "-1");
        for (int i = 0; i < bl.length; i++) {
          sb.append(getBundleSelectedHeader(bl[i]));
          if (i < bl.length - 1) {
            sb.append("<br>");
          }
        }
        sb.append("</font>\n");
        sb.append("</td>\n");
        sb.append("</tr>\n");

        sb.append("<tr><td bgcolor=\"#ffffff\">");
        sb.append(bundleInfo(bl).toString());
        sb.append("</td>\n");
        sb.append("</tr>\n");
        sb.append("</table>\n");
      }

      sb.append("\n</html>");

      setHTML(sb.toString());
    }

    @Override
    public StringBuilder bundleInfo(Bundle target)
    {
      return new StringBuilder("---- " + target);
    }

    public StringBuilder bundleInfo(Bundle[] targets)
    {
      final StringBuilder sb = new StringBuilder();

      startFont(sb);
      final Set<Bundle> closure = new TreeSet<>(Util.bundleIdComparator);

      for (final Bundle target : targets) {
        int state = target.getState();
        if (state != Bundle.INSTALLED && state != Bundle.UNINSTALLED ) {
          closure.addAll(Util.getClosure(target, null));
        } else {
          sb.append("Bundle #");
          sb.append(target.getBundleId());
          sb.append(" is in ");
          sb.append(state == Bundle.INSTALLED ? "INSTALLED" : "UNINSTALLED");
          sb.append(" state, closure not available");
          return sb;
        }
      }

      boolean containsUninstalled = false;
      if (closure.size() == 0) {
        sb.append("No dependencies");
      } else {
        sb.append("<b>Dependencies via capabilities and services</b><br>");
        for (final Bundle depB : closure) {
          sb.append("&nbsp;&nbsp;");
          if (depB.getState() != Bundle.UNINSTALLED) {
            Util.bundleLink(sb, depB);
          } else {
            sb.append("<b>UNINSTALLED</b>, ");
            sb.append(Util.getBundleName(depB));
            containsUninstalled  = true;
          }
          sb.append("<br>");
        }
      }

      sb.append("<br>");

      // Add xarsg info if we seem to be running knopflerfish
      if (targets.length > 0 && (targets[0].getClass().getName().contains("knopflerfish"))) {
        if (!containsUninstalled) {
          final String xargs =
              Util.getXARGS(null, closure).toString();
          sb.append("<hr>");
          startFont(sb);
          sb.append("<b>Suggested startup .xargs file</b><br>\n");
          sb.append("</font>");

          sb.append("<pre>");
          sb.append("<font size=\"-2\">");
          // sb.append(Text.replace(xargs, "\n", "<br>"));
          sb.append(xargs);
          sb.append("</font>");
          sb.append("</pre>");
        } else {
          sb.append("<hr>");
          startFont(sb);
          sb.append("<b>Suggested startup .xargs not available when closure contains uninstalled bundles</b><br>\n");
          sb.append("</font>");         
        }
      }

      sb.append("</font>");

      return sb;
    }

  }

}
