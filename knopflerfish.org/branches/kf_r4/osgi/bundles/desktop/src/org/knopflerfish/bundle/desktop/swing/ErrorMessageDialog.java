package org.knopflerfish.bundle.desktop.swing;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.FlowLayout;
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
    super(owner, (title == null ? "Error" : title), true);
    getContentPane().setLayout(new BorderLayout());

    friendLabel = new JLabel(friendlyMessage);
    friendPanel = new JPanel(new BorderLayout());
    friendPanel.add(friendLabel, BorderLayout.CENTER);
    getContentPane().add(friendPanel, BorderLayout.NORTH);

    okButton = new JButton("OK");
    okButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        THIS.hide();
      }
    });
    JPanel okPanel = new JPanel(new FlowLayout());
    okPanel.add(okButton);
    getContentPane().add(okPanel, BorderLayout.SOUTH);

    if (moreInfo == null && t != null) {
      moreInfo = t.getMessage();
    }

    if (moreInfo != null) {
      moreButton = new JButton("More info");
      moreButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          showMore = !showMore;
          moreButton.setLabel(showMore ? "Less info" : "More info");
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
          advancedButton.setLabel(showAdvanced ? "Simple" : "Advanced");
          arrange();
        }
      });

      morePanel.add(advancedButton, BorderLayout.EAST);

      StringBuffer buf = new StringBuffer(t.toString());
      StackTraceElement[] elements = t.getStackTrace();
      for (int i=0; i<elements.length; i++) {
        buf.append("\n  at ").append(elements[i].toString());
      }
      advancedLabel = new JTextArea(buf.toString());
    }

    arrange();
    setLocation((getOwner().getWidth() - getWidth()) / 2, (getOwner().getHeight() - getHeight()) / 2);
  }

  private void arrange() {
    if (showMore) {
      getContentPane().add(extraPanel, BorderLayout.CENTER);
      if (showAdvanced) {
        extraPanel.add(advancedLabel, BorderLayout.SOUTH);
      } else if (advancedLabel != null) {
        extraPanel.remove(advancedLabel);
      }
    } else if (extraPanel != null) {
      getContentPane().remove(extraPanel);
    }

    pack();
  }

}