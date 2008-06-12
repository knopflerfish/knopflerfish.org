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
package org.knopflerfish.bundle.desktop.swing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public class ErrorMessageDialog extends JDialog {

  private final JDialog THIS = this;

  private boolean showMore = false;
  private boolean showAdvanced = false;

  private JButton okButton;
  private JButton moreButton;
  private JButton advancedButton;

  private JLabel friendLabel;
  private JLabel moreLabel;
  private JTextArea advancedLabel;

  private JPanel friendPanel;
  private JPanel morePanel;
  private JPanel extraPanel;

  public ErrorMessageDialog(Frame owner, String friendlyMessage) {
    this(owner, friendlyMessage, null, null);
  }

  public ErrorMessageDialog(Frame owner, String friendlyMessage, String moreInfo) {
    this(owner, friendlyMessage, moreInfo, null);
  }

  public ErrorMessageDialog(Frame owner, String friendlyMessage, Throwable t) {
    this(owner, friendlyMessage, null, t);
  }

  public ErrorMessageDialog(Frame owner, String friendlyMessage, String moreInfo, Throwable t) {
    this(owner, null, friendlyMessage, moreInfo, t);
  }

  public ErrorMessageDialog(Frame owner, String title, String friendlyMessage, String moreInfo, Throwable t) {
    super(owner, (title == null || title.length() == 0 ? "Error" : title), true);
    getContentPane().setLayout(new BorderLayout());

    if ((moreInfo == null || moreInfo.length() == 0) && t != null) {
      moreInfo = t.getMessage();
    }

    if (friendlyMessage == null || friendlyMessage.length() == 0) {
      friendlyMessage = moreInfo;
      moreInfo = null;
    }
    
    friendLabel = new JLabel(friendlyMessage == null ? "Unknown error" : friendlyMessage);
    friendPanel = new JPanel(new BorderLayout());
    friendPanel.add(friendLabel, BorderLayout.CENTER);
    getContentPane().add(friendPanel, BorderLayout.NORTH);

    okButton = new JButton("OK");
    okButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        THIS.setVisible(false);
      }
    });
    JPanel okPanel = new JPanel(new FlowLayout());
    okPanel.add(okButton);
    getContentPane().add(okPanel, BorderLayout.SOUTH);

    if (moreInfo != null) {
      moreButton = new JButton("More info");
      moreButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          showMore = !showMore;
          moreButton.setText(showMore ? "Less info" : "More info");
          arrange();
        }
      });
      friendPanel.add(moreButton, BorderLayout.EAST);

      moreLabel = new JLabel(moreInfo);
      morePanel = new JPanel(new BorderLayout());
      morePanel.add(moreLabel, BorderLayout.CENTER);

      extraPanel = new JPanel(new BorderLayout());
      extraPanel.add(morePanel, BorderLayout.NORTH);
    }

    if (t != null) {
      advancedButton = new JButton("Advanced");
      advancedButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          showAdvanced = !showAdvanced;
          if (morePanel == null) showMore = showAdvanced;
          advancedButton.setText(showAdvanced ? "Simple" : "Advanced");
          arrange();
        }
      });

      if (morePanel == null) {
        friendPanel.add(advancedButton, BorderLayout.EAST);
      } else {
        morePanel.add(advancedButton, BorderLayout.EAST);
      }

      StringBuffer buf = new StringBuffer(t.toString());
      StackTraceElement[] elements = t.getStackTrace();
      for (int i=0; i<elements.length; i++) {
        buf.append("\n  at ").append(elements[i].toString());
      }
      advancedLabel = new JTextArea(buf.toString());
      Font oldFont = advancedLabel.getFont();
      advancedLabel.setFont(new Font(oldFont.getName(), oldFont.getStyle(), 10));
    }

    arrange();
    setLocation((getOwner().getWidth() - getWidth()) / 2, (getOwner().getHeight() - getHeight()) / 2);
    String friendliness
      = Util.getProperty("org.knopflerfish.desktop.errordialogfriendliness",
                         null);
    if (friendliness != null) {
      if ("more".equals(friendliness)) {
        moreButton.doClick();
      } else if ("advanced".equals(friendliness)) {
        moreButton.doClick();
        advancedButton.doClick();
      }
    }
  }

  private void arrange() {
    if (showMore) {
      if (extraPanel == null) {
        getContentPane().add(advancedLabel, BorderLayout.CENTER);
      } else {
        getContentPane().add(extraPanel, BorderLayout.CENTER);
        if (showAdvanced) {
          extraPanel.add(advancedLabel, BorderLayout.SOUTH);
        } else if (advancedLabel != null) {
          extraPanel.remove(advancedLabel);
        }
      }
    } else if (extraPanel != null) {
      getContentPane().remove(extraPanel);
    } else if (advancedLabel != null) {
      getContentPane().remove(advancedLabel);
    }

    pack();
  }

}
