/* Copyright (c) 2003-2022, KNOPFLERFISH project
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.util.Arrays;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class ErrorMessageDialog extends JDialog {

  private static final long serialVersionUID = 1L;

  private boolean showMore = false;
  private boolean showAdvanced = false;

  private JButton moreButton;
  private JButton advancedButton;

  private JComponent advancedLabel;

  private JPanel friendPanel;
  private JPanel morePanel;
  private JPanel extraPanel;

  public ErrorMessageDialog(Frame owner,
                            String friendlyMessage)
  {
    this(owner, null, friendlyMessage, null, null);
  }

  public ErrorMessageDialog(Frame owner,
                            String friendlyMessage,
                            String moreInfo)
  {
    this(owner, null, friendlyMessage, moreInfo, null);
  }

  public ErrorMessageDialog(Frame owner,
                            String friendlyMessage,
                            Throwable t)
  {
    this(owner, null, friendlyMessage, null, t);
  }

  public ErrorMessageDialog(Frame owner,
                            String friendlyMessage,
                            String moreInfo,
                            Throwable t)
  {
    this(owner, null, friendlyMessage, moreInfo, t);
  }

  public ErrorMessageDialog(Frame owner,
                            String title,
                            String friendlyMessage,
                            String moreInfo,
                            Throwable throwable)
  {
    super(owner, (title == null || title.length() == 0 ? "Error" : title), true);
    getContentPane().setLayout(new BorderLayout());

    if ((moreInfo == null || moreInfo.length() == 0) && throwable != null) {
      moreInfo = throwable.getMessage();
    }

    if (friendlyMessage == null || friendlyMessage.length() == 0) {
      friendlyMessage = moreInfo;
      moreInfo = null;
    }

    addFriendlyMessage(friendlyMessage);

    addOkButton();

    if (moreInfo != null) {
      addMoreInfo(moreInfo);
    }

    if (throwable != null) {
      addThrowable(throwable);
    }

    arrange();
    setLocationRelativeTo(getOwner());

    String friendliness = Util.getProperty("org.knopflerfish.desktop.errordialogfriendliness", null);
    if (friendliness != null) {
      if ("more".equals(friendliness)) {
        moreButton.doClick();
      } else if ("advanced".equals(friendliness)) {
        moreButton.doClick();
        advancedButton.doClick();
      }
    }
  }

  private void addThrowable(Throwable throwable) {
    advancedButton = new JButton("Advanced");
    advancedButton.addActionListener(e -> {
      showAdvanced = !showAdvanced;
      if (morePanel == null) showMore = showAdvanced;
      advancedButton.setText(showAdvanced ? "Simple" : "Advanced");
      arrange();
    });

    if (morePanel == null) {
      friendPanel.add(advancedButton, BorderLayout.EAST);
    } else {
      final Box box = Box.createVerticalBox();
      box.add(advancedButton);
      box.add(Box.createVerticalGlue());
      morePanel.add(box, BorderLayout.EAST);
    }

    final StringBuilder buf = new StringBuilder(200);
    Throwable cause = throwable;
    while (null!=cause) {
      if (buf.length()>0) {
        buf.append("\nCaused by ");
      }
      buf.append(cause.toString());

      final StackTraceElement[] elements = cause.getStackTrace();
      Arrays.stream(elements).forEach(element -> buf.append("\n  at ").append(element.toString()));
      cause = cause.getCause();
    }
    JTextArea advancedTextArea = new JTextArea(buf.toString());
    final Font oldFont = advancedTextArea.getFont();
    final Font newFont = new Font(oldFont.getName(), oldFont.getStyle(), 10);
    advancedTextArea.setFont(newFont);
    advancedLabel = new JScrollPane(advancedTextArea);
  }

  private void addOkButton() {
    JButton okButton = new JButton("OK");
    okButton.addActionListener(e -> setVisible(false));
    JPanel okPanel = new JPanel(new FlowLayout());
    okPanel.add(okButton);
    getContentPane().add(okPanel, BorderLayout.SOUTH);
  }

  private void addFriendlyMessage(String friendlyMessage) {
    JLabel friendLabel = new JLabel(friendlyMessage == null ? "Unknown error" : friendlyMessage);
    friendPanel = new JPanel(new BorderLayout());
    friendPanel.add(friendLabel, BorderLayout.CENTER);
    getContentPane().add(friendPanel, BorderLayout.NORTH);
  }

  private void addMoreInfo(String moreInfo) {
    moreButton = new JButton("More info");
    moreButton.addActionListener(e -> {
      showMore = !showMore;
      moreButton.setText(showMore ? "Less info" : "More info");
      arrange();
    });
    friendPanel.add(moreButton, BorderLayout.EAST);

    // Make html out of long texts to enable line breaking
    if (moreInfo.length()>80 && !moreInfo.startsWith("<html>")) {
      StringBuilder sb = new StringBuilder(moreInfo.length() + 40);
      sb.append("<html><p>");
      for (int i = 0; i < moreInfo.length(); i++) {
        char c = moreInfo.charAt(i);
        if (c == '&') {
          sb.append("&amp;");
        } else if (c == '<') {
          sb.append("&lt;");
        } else if (c == '>') {
          sb.append("&gt;");
        } else if (c == '\n') {
          sb.append("<br/>");
        } else {
          sb.append(c);
        }
      }
      sb.append("</p></html>");
      moreInfo = sb.toString();
    }
    JLabel moreLabel = new JLabel(moreInfo);
    morePanel = new JPanel(new BorderLayout());
    morePanel.add(moreLabel, BorderLayout.CENTER);

    extraPanel = new JPanel(new BorderLayout());
    extraPanel.add(morePanel, BorderLayout.NORTH);
  }

  private void arrange() {
    if (showMore) {
      if (extraPanel == null) {
        getContentPane().add(advancedLabel, BorderLayout.CENTER);
      } else {
        getContentPane().add(extraPanel, BorderLayout.CENTER);
        if (showAdvanced) {
          extraPanel.add(advancedLabel, BorderLayout.CENTER);
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

    // Ensure that the dialog is smaller than the owning window.
    final Dimension ownerSize = getOwner().getSize();
    final Dimension mySize = getSize();
    boolean sizeChanged = false;

    double newWidth = mySize.getWidth();
    if (ownerSize.getWidth()<newWidth) {
      newWidth = ownerSize.getWidth() - 10.0;
      sizeChanged = true;
    }
    double newHeight = mySize.getHeight();
    if (ownerSize.getHeight()<newHeight) {
      newHeight = ownerSize.getHeight() - 10.0;
      sizeChanged = true;
    }
    if (sizeChanged) {
      mySize.setSize(newWidth, newHeight);
      setSize(mySize);
    }
    validate();
  }

}
