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

package org.knopflerfish.bundle.desktop.swing.console;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.knopflerfish.bundle.desktop.swing.Activator;
import org.knopflerfish.bundle.desktop.swing.Util;
import org.osgi.service.log.LogService;

public class SwingIO extends JPanel {
  private static final String ORG_KNOPFLERFISH_FRAMEWORK_MAIN_BOOT_TEXT = "org.knopflerfish.framework.main.bootText";

  private static final long serialVersionUID = 1L;

  JTextArea  text;
  JTextField tfCmd;

  InputStream origIn;
  PrintStream origOut;
  PrintStream origErr;

  TextReader       in;
  PrintStream      out;

  JPanel panel;
  JScrollPane scroll;
  JTextArea  cmdLabel;

  Vector<String> history    = new Vector<>();
  int    historyPos = 0;

  boolean bGrabbed = false;

  int maxLines
    = Util.getIntProperty("org.knopflerfish.desktop.console.maxlines", 5000);

  void setSystemIO() {

    boolean bDebugClass
      = Util.getBooleanProperty("org.knopflerfish.framework.debug.classloader",
                                false);

    if(!bDebugClass) {
      if(!bGrabbed) {
        try {

          ConsoleSwing.log(LogService.LOG_DEBUG, "grabbing system I/O...");

          origIn  = System.in;
          origOut = System.out;
          origErr = System.err;

          //    System.setIn(in);
          System.setOut(new PrefixPrintStream(out, "[stdout] ",
                                              ConsoleSwing.config.multiplexSystemOut ? System.out : null));
          System.setErr(new PrefixPrintStream(out, "[stderr] ",
                                              ConsoleSwing.config.multiplexSystemErr ? System.err : null));

          bGrabbed = true;
          ConsoleSwing.log(LogService.LOG_DEBUG, "...grabbed system I/O");

        } catch (Exception e) {
          ConsoleSwing.log(LogService.LOG_ERROR, "Failed to set IO", e);
          bGrabbed = false;
        }
      }
    }
  }

  void restoreSystemIO() {

    //    synchronized(grabLock)
      {
      if(bGrabbed) {
        ConsoleSwing.log(LogService.LOG_DEBUG, "restoring system I/O...");
        try {
          if(origIn != null) {
            System.setIn(origIn);
          }
          if(origOut != null) {
            System.setOut(origOut);
          }
          if(origIn != null) {
            System.setErr(origErr);
          }
          ConsoleSwing.log(LogService.LOG_DEBUG, "...restored system I/O");
          bGrabbed = false;
        } catch (Exception e) {
          ConsoleSwing.log(LogService.LOG_ERROR, "Failed to restore IO", e);
        }
      }

    }

  }


  public SwingIO() {
    super(new BorderLayout());

    panel = this;

    try {
      text = new JTextArea("", 8, 80);
      text.setEditable(false);

      // See if we're using the knopflerfish framework. If so, grab
      // the boot string from framework properties.
      String bootText = Activator
          .getTargetBC_getProperty(ORG_KNOPFLERFISH_FRAMEWORK_MAIN_BOOT_TEXT);
      if (null!=bootText) {
        bootText += "\n\n";
      } else {
        bootText = "";
      }
      text.setText(bootText +
       "Knopflerfish OSGi console.\n" +
       "Type 'help' for help or 'alias' for a list of common commands\n\n");
      scroll = new JScrollPane(text,
                               JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                               JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

      tfCmd = new JTextField();

      final KeyListener keyL = new KeyAdapter() {
          public void keyPressed(KeyEvent ev) {
            if(ev.getKeyCode() == KeyEvent.VK_UP) {
              if(historyPos > 0) {
                String line = history.elementAt(historyPos-1);
                historyPos--;
                tfCmd.setText(line);
              }
            } else if(ev.getKeyCode() == KeyEvent.VK_DOWN) {
              if(historyPos < history.size()-1) {
                String line = history.elementAt(historyPos+1);
                historyPos++;
                tfCmd.setText(line);
              }
            } else if(ev.getKeyCode() == KeyEvent.VK_ENTER) {
              String line = tfCmd.getText();
              if(!("".equals(line) ||
                   "\n".equals(line) ||
                   "\n\r".equals(line) ||
                   "\r\n".equals(line))) {
                history.addElement(line);
                historyPos = history.size();
              }
              if("clear".equals(line)) {
                clear();
              } else if("quit".equals(line)) {
                org.knopflerfish.bundle.desktop.swing
                  .Activator.desktop.stopFramework();
              } else {
                // Try simple command expansion first
                if(line.startsWith("!") && line.length() > 1) {
                  String s2 = line.substring(1);
                  String bestStr = "";
                  for(int i = 0; i < history.size(); i++) {
                    String s = history.elementAt(i);
                    if(s.startsWith(s2) || s.length() >= bestStr.length()) {
                      bestStr = s;
                    }
                  }
                  if(!"".equals(bestStr)) {
                    line = bestStr;
                  }
                }

                // ..and send to console via inputstream
                String s = line + "\r\n";
                text.append(s);
                showLastLine();
                if(in != null) {
                  in.print(s);
                  in.flush();
                }
              }
              tfCmd.setText("");
            }
          }
        };

      tfCmd.addKeyListener(keyL);

      // move focus away from text output to text input
      // in key press
      text.addKeyListener(new  KeyAdapter() {
          public void keyPressed(KeyEvent ev) {
            int modifiers = ev.getModifiersEx();

            // Don't steal special key events like CTRL-C
            if (modifiers == 0) {
              tfCmd.requestFocus();
            }
          }

        });

      out = new PrintStream(new TextAreaOutputStream(this, text));

      panel.add(scroll, BorderLayout.CENTER);

      JPanel cmdPanel = new JPanel(new BorderLayout());

      cmdLabel = new JTextArea("> ");
      cmdLabel.setEditable(false);
      cmdLabel.setFocusable(false);

      cmdPanel.add(cmdLabel, BorderLayout.WEST);
      cmdPanel.add(tfCmd, BorderLayout.CENTER);

      panel.add(cmdPanel, BorderLayout.SOUTH);

      reinit();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  void clear() {
    text.setText("");
  }

  void showLastLine() {
    SwingUtilities.invokeLater(() -> {
      if (maxLines > 0) {
        int linesToRemove = text.getLineCount() - maxLines;
        int index = 0;
        if (linesToRemove > 0) {
          for (int i=0; i<linesToRemove; i++) {
            index = text.getText().indexOf('\n', index) + 1;
          }
          text.setText(text.getText().substring(index));
        }
      }

      JScrollBar bar = scroll.getVerticalScrollBar();
      if(bar != null) {
        int v = bar.getMaximum();
        bar.setValue(v*2);
      }
    });
  }

  synchronized void reinit() {
    Font font = new Font(ConsoleSwing.config.fontName, Font.PLAIN, ConsoleSwing.config.fontSize);

    text.setBackground(Config.parseColor(ConsoleSwing.config.bgColor));
    text.setForeground(Config.parseColor(ConsoleSwing.config.textColor));
    text.setFont(font);

    tfCmd.setBackground(text.getBackground());
    tfCmd.setForeground(text.getForeground());
    tfCmd.setFont(text.getFont());

    cmdLabel.setBackground(text.getBackground());
    cmdLabel.setForeground(text.getForeground());
    cmdLabel.setFont(text.getFont());
  }

  void start() {
    stop();
    in         = new TextReader();
    tfCmd.setText("");
    tfCmd.setEnabled(true);
    setVisible(true);
    if(ConsoleSwing.config.grabSystemIO) {
      setSystemIO();
    }
  }

  @SuppressWarnings("SameParameterValue")
  void disableInput(String reason) {
    if(ConsoleSwing.config.grabSystemIO) {
      restoreSystemIO();
    }
    tfCmd.setEnabled(false);
    tfCmd.setText(reason);
    if(in != null) {
      in.close();
      in = null;
    }
  }

  void stop() {
    if(ConsoleSwing.config.grabSystemIO) {
      restoreSystemIO();
    }
    setVisible(false);
    if(in != null) {
      in.close();
      in = null;
    }
  }
}
