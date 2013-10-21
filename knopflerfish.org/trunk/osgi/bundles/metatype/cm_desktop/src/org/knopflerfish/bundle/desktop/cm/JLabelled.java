/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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

package org.knopflerfish.bundle.desktop.cm;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class JLabelled
  extends JPanel
{

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public JLabelled(String text, String tooltip, JComponent main, int labelWidth)
  {
    // This panel is horizontal box.
    setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    setAlignmentX(Component.LEFT_ALIGNMENT);

    // Shorten text to last part after a dot
    // if it's too long
    final FontMetrics fm = main.getFontMetrics(main.getFont());
    if (fm != null) {
      int ix;
      while (-1 != (ix = text.indexOf("."))
             && fm.stringWidth(text) > labelWidth) {
        text = text.substring(ix + 1);
      }
    }

    final JLabel label = new JLabel(text);
    final Dimension size = label.getPreferredSize();
    label.setPreferredSize(new Dimension(labelWidth, size.height + 2));

    if (tooltip != null && !"".equals(tooltip)) {
      label.setToolTipText("<html>" + tooltip + "</html>");
    }

    // Since we are in a scroll pane with glue at the bottom we do not want the
    // components to stretch vertically.
    main.setMaximumSize(new Dimension(Integer.MAX_VALUE, main
        .getPreferredSize().height));
    label.setMaximumSize(label.getPreferredSize());

    add(label);
    add(main);
  }
}
