/*
 * Copyright (c) 2003, 2009, KNOPFLERFISH project
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

package org.knopflerfish.tools.jarunpacker;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.*;
import java.net.URL;
import java.io.*;
import java.lang.reflect.*;

import javax.swing.border.*;
import javax.swing.filechooser.*;

public class JWizard extends JFrame implements InstallUI {

  JLabel       pageIcon;
  Icon         defaultIcon;
  JPanel       wizardPanel;
  JPanel       pagePanel;
  JPanel       commandPanel;
  JButton      cancelButton;
  JButton      backButton;
  JButton      forwardButton;
  JButton      finishButton;
  CardLayout   pageCards;
  JLabel       pageDescription;

  Vector       pages    = new Vector();
  JWizardPage  currPage = null;

  Container    container;
  Object       closeLock = new Object();

  boolean      bFinished = false;

  StatusBar    statusBar;

  public JWizard() {
    this("");
  }

  public JWizard(String title) {
    super(title);

    String systemLF = UIManager.getSystemLookAndFeelClassName();
    try {
      UIManager.setLookAndFeel(systemLF);
    } catch (Exception e) {
      System.out.println("Failed to set system l&F: " + systemLF);
    }

    init();
  }

  //  String defIconPath = "/fish200x300.gif";
  String defIconPath = "/knopflerfish_red400pxl.gif";

  public void init() {

    String iconPath = Main.theMain.iconPath;
    if(iconPath == null) {
      iconPath = defIconPath;
    }

    boolean iconLeft = "true".equalsIgnoreCase(Main.theMain.iconLeft);

    URL url = getClass().getResource(iconPath);
    Icon  leftIcon = null;
    if(url != null) {
      leftIcon = new ImageIcon(url);
    } else {
      System.err.println("non-existing image resource: " + iconPath);
      leftIcon = new ImageIcon(getClass().getResource(defIconPath));
    }


    defaultIcon = leftIcon;
    /*
      new MergedIcon(leftIcon,
      boing,
      leftIcon.getIconWidth() - boing.getIconWidth(),
      leftIcon.getIconHeight() - boing.getIconHeight()
      );
    */
    container = getContentPane();

    container.setLayout(new BorderLayout());

    wizardPanel  = new JPanel(new BorderLayout());
    commandPanel = new JPanel(new FlowLayout());

    statusBar = new StatusBar("");
    pageIcon  = new JLabel(defaultIcon);

    //    boing.setComponent(pageIcon);
    //    boing.start();

    try {
      pageIcon.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
    } catch (Throwable t) {
    }

    pagePanel = new JPanel(pageCards = new CardLayout());
    pageDescription = new JLabel();

    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          cancel();
        }
      });

    cancelButton = new JButton(Strings.get("cancel")) {
        {
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent ev) {
                cancel();
              }
            });
        }
      };

    finishButton = new JButton(Strings.get("finish")) {
        {
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent ev) {
                finish();
              }
            });
        }
      };
    backButton = new JButton(Strings.get("back")) {
        {
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent ev) {
                back();
              }
            });
        }
      };
    forwardButton = new JButton(Strings.get("forward")) {
        {
          addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent ev) {
                forward();
              }
            });
        }
      };

    commandPanel.add(cancelButton);
    commandPanel.add(backButton);
    commandPanel.add(forwardButton);
    commandPanel.add(finishButton);

    JPanel pageMain = new JPanel(new BorderLayout());

    pageMain.add(pageDescription, BorderLayout.NORTH);
    pageMain.add(pagePanel,       BorderLayout.CENTER);

    pagePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
    pageMain.setBorder(new EmptyBorder(5, 5, 5, 5));

    wizardPanel.add(pageMain,     BorderLayout.CENTER);
    wizardPanel.add(commandPanel, BorderLayout.SOUTH);

    container.add(pageIcon,    iconLeft ? BorderLayout.WEST : BorderLayout.NORTH);
    container.add(wizardPanel, BorderLayout.CENTER);
    container.add(statusBar,   BorderLayout.SOUTH);
    if (defaultIcon instanceof ImageIcon) {
      Color defColor = container.getBackground();
      container.setBackground(getBgColor(((ImageIcon)defaultIcon).getImage()));
      statusBar.setBackground(defColor);
    }

    Dimension size = new Dimension(550, 300);
    pageMain.setPreferredSize(size);
    setIcon(this, "/kf_");
    pack();
  }

  Color getBgColor(Image img) {
    int[] pixel = new int[1];
    PixelGrabber pg = new PixelGrabber(img, 1, 1, 1, 1, pixel, 0, 1);
    try {
      pg.grabPixels();
    } catch (InterruptedException e) {
      return null;
    }
    if ((pg.getStatus() & ImageObserver.ABORT) != 0) {
      return null;
    }
    return new Color(pixel[0]);
  }

  public void updateProgress(String msg, int perc) {
    statusBar.updateProgress(msg, perc);
  }

  public void startProgress(String msg) {
    statusBar.startProgress(msg);
  }

  public void stopProgress() {
    statusBar.stopProgress();
  }

  public void theEnd() {
    setVisible(false);
  }


  public boolean isExcluded(String name) {
    return false;
  }

  public int askFile(String title,
                     String[] options,
                     int defOpt,
                     String origFile,
                     String newFile,
                     Date   newDate,
                     long   newSize
                     ) {


    JFileReplacePanel fileReplacePanel =
      new JFileReplacePanel(origFile,
                            newFile,
                            newDate,
                            newSize);


    JOptionPane pane = new JOptionPane(fileReplacePanel,
                                       JOptionPane.QUESTION_MESSAGE,
                                       JOptionPane.DEFAULT_OPTION,
                                       null,
                                       options,
                                       options[defOpt]);
    JDialog dialog = pane.createDialog(this, title);

    dialog.show();
    Object selectedValue = pane.getValue();


    int n = -1;
    for(int i = 0; i < options.length; i++) {
      if(options[i].equals(selectedValue)) {
        n = i;
        break;
      }
    }

    if(n == 3) {
      if(!askCancel()) {
        n = 2;
      }
    }

    return n;

    /*

      n = JOptionPane.showOptionDialog(this,
      fileReplacepanel,
      title,
      JOptionPane.DEFAULT_OPTION,
      JOptionPane.QUESTION_MESSAGE,
      null,
      options,
      options[defOpt]);

      return n;
    */
  }



  public int ask(String title,
                 String message,
                 String[] options,
                 int defOpt,
                 String iconFile) {

    Object msgObject = message;

    int n = JOptionPane.showOptionDialog(this,
                                         msgObject,
                                         title,
                                         JOptionPane.DEFAULT_OPTION,
                                         JOptionPane.QUESTION_MESSAGE,
                                         null,
                                         options,
                                         options[defOpt]);

    if(n == 3) {
      if(!askCancel()) {
        n = 2;
      }
    }
    return n;
  }



  void setIcon(JFrame frame, String baseName) {
    String iconName = baseName + "32x32.gif";
    if (System.getProperty( "os.name", "" ).startsWith("Win")) {
      iconName = baseName + "16x16.gif";
    }
    String strURL = iconName;
    try {
      MediaTracker tracker = new MediaTracker(frame);

      URL url = getClass().getResource(strURL);

      if(url != null) {
        Image image = frame.getToolkit().getImage(url);
        tracker.addImage(image, 0);
        tracker.waitForID(0);

        frame.setIconImage(image);
      } else {
      }
    } catch (Exception e) {
    }
  }

  public void start() {
    bFinished = false;
    setPage((JWizardPage)pages.elementAt(0));
    setVisible(true);

    try {
      synchronized(closeLock) {
        closeLock.wait();
      }
    } catch (Exception ignored) {
    }
  }

  public boolean isFinished() {
    return  bFinished;
  }


  void addPage(JWizardPage page) {
    //    System.out.println("addPage " + page.getName());
    pages.addElement(page);

    pagePanel.add(page, page.getName());
    pageCards.addLayoutComponent(page.getName(), page);
  }

  void setPage(JWizardPage page) {
    int ix = pages.indexOf(page);
    //    System.out.println("setPage " + page.getName() + ", ix=" + ix);
    if(page.getIcon() != null) {
      pageIcon.setIcon(page.getIcon());
    } else {
      pageIcon.setIcon(defaultIcon);
    }

    currPage = page;
    pageCards.show(pagePanel, page.getName());

    backButton.setEnabled(page.canStepBack());
    forwardButton.setEnabled(page.canStepForward());
    cancelButton.setEnabled(page.canCancel());
    finishButton.setEnabled(page.canFinish());

    pageDescription.setText(page.getDescription());

    container.invalidate();
  }

  void back() {
    int ix = pages.indexOf(currPage);
    //    System.out.println("back " + currPage.getName() + ", ix=" + ix);
    if(ix >= 0) {
      setPage((JWizardPage)pages.elementAt(ix - 1));
    }
  }

  void forward() {
    int ix = pages.indexOf(currPage);
    //    System.out.println("forward " + currPage.getName() + ", ix=" + ix);
    if(ix < pages.size() - 1) {
      setPage((JWizardPage)pages.elementAt(ix + 1));
    }
  }

  void cancel() {
    if(askCancel()) {
      close();
    }
  }

  void finish() {
    //    System.out.println("finish");
    setPage((JWizardPage)pages.elementAt(pages.size() - 1));
    bFinished = true;
    close();
  }

  void close() {
    synchronized(closeLock) {
      closeLock.notifyAll();
    }
  }

  boolean askCancel() {
    String[] sa = Strings.getArray("cancel_install_array2");

    int n = JOptionPane.showOptionDialog(this,
                                         Strings.get("cancel_install_msg"),
                                         Strings.get("cancel_install_title"),
                                         JOptionPane.YES_NO_OPTION,
                                         JOptionPane.QUESTION_MESSAGE,
                                         null,
                                         sa,
                                         sa[0]);

    return n == 0;
  }
}
