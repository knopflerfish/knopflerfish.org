/*
 * Copyright (c) 2003-2004, KNOPFLERFISH project
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

package org.knopflerfish.bundle.desktopawt;

import java.util.Vector;
import java.io.*;

import java.awt.*;
import java.awt.event.*; 

import java.net.URL;

public class ConsoleAWT extends Panel {
  LF lf = LF.getLF();

  TextArea  text;
  TextField tfCmd;

  int p0 = -1;
  int p1 = 0;

  String last = "";

  InputStream origIn;
  PrintStream origOut;
  PrintStream origErr;

  PipedOutputStream textSource;

  TextReader       in;
  PrintStream      out;

  Container panel;
  ScrollPane scroll;
  Label  cmdLabel;

  Vector history    = new Vector();
  int    historyPos = 0;

  StringBuffer lineBuff = new StringBuffer();

  boolean bGrabbed = false;

  static boolean multiplexSystemOut = true;
  static boolean multiplexSystemErr = true;
  static boolean grabSystemIO       = true;

  Dimension pSize = new Dimension(100, 200);
  
  public ConsoleAWT() {

    super();
    setLayout(new BorderLayout());
  
    panel = this;

    log("ConsoleAWT()");
    try {
      text = new TextArea("", 0, 40) {
          public Dimension preferredSize() {
            return getPreferredSize();
          }
          public Dimension getPreferredSize() {
            return pSize;
          }
        };
      text.setSize(pSize);
      text.setEditable(false);
      String bootText = 
	"Knopflerfish OSGi console. Copyright (c) 2004 Knopflerfish.";
    
      // See if we're using the knopflerfish framework. If so, grab
      // the boot string from the startup class
      try {
	Class mainClazz = Class.forName("org.knopflerfish.framework.Main");
	bootText        = (String)mainClazz.getField("bootText").get(null);
      } catch (Throwable e) {
	bootText        = "";
	//	e.printStackTrace();
	// anything else defaults to the std boot text above
      }
      text.setText(bootText + 
		   "\n\n" + 
		   "Type 'help' for help or 'alias' for a list of common commands\n\n");
      //      scroll = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
      
      tfCmd = new TextField();
      
      //      scroll.add(text);

      final KeyListener keyL = new KeyAdapter() {
	  public void keyPressed(KeyEvent ev) {
	    if(ev.getKeyCode() == KeyEvent.VK_UP) {
	      if(historyPos > 0) {
		String line = (String)history.elementAt(historyPos-1);
		historyPos--;
		tfCmd.setText(line);
	      }
	    } else if(ev.getKeyCode() == KeyEvent.VK_DOWN) {
	      if(historyPos < history.size()-1) {
		String line = (String)history.elementAt(historyPos+1);
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
                //		org.knopflerfish.bundle.desktop.swing
                //		  .Activator.desktop.stopFramework();
	      } else {
		// Try simple command expansion first
		if(line.startsWith("!") && line.length() > 1) {
		  String s2 = line.substring(1);
		  String bestStr = "";
		  for(int i = 0; i < history.size(); i++) {
		    String s = (String)history.elementAt(i);
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
	    int modifiers = ev.getModifiers();

	    // Don't steal special key events like CTRL-C
	    if(modifiers == 0) {
	      tfCmd.requestFocus();
	    }
	  }

	});

      out = new PrintStream(new TextAreaOutputStream(this, text));

      GridBagLayout gridbag = new GridBagLayout();
      GridBagConstraints c = new GridBagConstraints();

      panel.add(text,         BorderLayout.CENTER);

      Panel cmdPanel = new Panel(new BorderLayout());

      cmdLabel = new Label("> ");
      cmdPanel.add(cmdLabel,        BorderLayout.WEST);
      cmdPanel.add(tfCmd,           BorderLayout.CENTER);

      panel.add(cmdPanel,        BorderLayout.NORTH);

      final PopupMenu cmdPopupMenu = new PopupMenu();
      cmdPopupMenu.add(new CommandItem("ps -1", true));
      cmdPopupMenu.add(new CommandItem("log", true));
      cmdPopupMenu.add(new CommandItem("/fr refresh", true));
      cmdPopupMenu.add(new CommandItem("obr list", true));
      cmdPopupMenu.add(new CommandItem("start <sel>", false));
      cmdPopupMenu.add(new CommandItem("stop <sel>", false));
      cmdPopupMenu.add(new CommandItem("update <sel>", false));
      cmdPopupMenu.add(new CommandItem("obr install =<sel>", false));
      cmdPopupMenu.add(new CommandItem("obr start =<sel>", false));
      cmdPopupMenu.add(new CommandItem("<sel>", false));

      cmdLabel.addMouseListener(new MouseAdapter() 
        {
          public void mousePressed(MouseEvent e) {
            showPopup(e);
          }
          
          public void mouseReleased(MouseEvent e) {
            showPopup(e);
          }
          
          private void showPopup(MouseEvent e) {
            if (cmdPopupMenu != null) {
              Component comp = e.getComponent();
              cmdPopupMenu.show(comp, 0, comp.getSize().height);
            }
          }
        });
      cmdLabel.add(cmdPopupMenu);


      reinit();
    } catch (Exception e) {
      e.printStackTrace();
    }

  } 

  class CommandItem extends MenuItem {
    public CommandItem(final String cmd, final boolean bDirect) {
      super(cmd);
      addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent ev) {
            String s = cmd;
            String sel = text.getSelectedText();
            if(sel != null) {
              sel = sel.trim();
              s = Util.replace(s, "<sel>", sel);
            }
            if(bDirect) {
              in.print(s + "\r\n");
              in.flush();
            } else {
              tfCmd.setText(s);
            }
          }
        });
    }
  }

  static void log(String msg) {
    log(msg, null);
  }

  static void log(String msg, Exception e) {
    if(false) {
      System.out.println(msg);
      if(e != null) {
        e.printStackTrace();
      }
    }
  }

  void setSystemIO() {

    boolean bDebugClass = "true".equals(System.getProperty("org.knopflerfish.framework.debug.classloader", "false"));

    if(!bDebugClass) {
      if(!bGrabbed) {
	try {
	  
          ConsoleAWT.log("grabbing system I/O...");
	  origIn  = System.in;
	  origOut = System.out;
	  origErr = System.err;
	  
	  //	  System.setIn(in);
	  System.setOut(new PrefixPrintStream(out, "[stdout] ", 
					      ConsoleAWT.multiplexSystemOut ? System.out : null));
	  System.setErr(new PrefixPrintStream(out, "[stderr] ", 
					      ConsoleAWT.multiplexSystemErr ? System.err : null));
	  
	  bGrabbed = true;
	  ConsoleAWT.log("...grabbed system I/O");
	  
	} catch (Exception e) {
	  ConsoleAWT.log("Failed to set IO", e);
	  bGrabbed = false;
	}
      }
    }
  }

  void restoreSystemIO() {

    //    synchronized(grabLock) 
      {
      if(bGrabbed) {
	ConsoleAWT.log("restoring system I/O...");
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
	  ConsoleAWT.log("...restored system I/O");
	  bGrabbed = false;
	} catch (Exception e) {
	  ConsoleAWT.log("Failed to restore IO", e);
	}
      }

    }

  }



  void clear() {
    text.setText("");
  }

  void showLastLine() {
  }

  synchronized void reinit() {
    text.setBackground(lf.bgColor);
    text.setForeground(lf.textColor);
    text.setFont(lf.smallFixedFont);

    tfCmd.setBackground(text.getBackground());
    tfCmd.setForeground(text.getForeground());
    tfCmd.setFont(lf.defaultFixedFont);

    cmdLabel.setBackground(tfCmd.getBackground());
    cmdLabel.setForeground(tfCmd.getForeground());
    cmdLabel.setFont(tfCmd.getFont());
  }

  void start() {
    log("starting ConsoleAWT");
    stop();
    in         = new TextReader();
    setVisible(true);
    if(ConsoleAWT.grabSystemIO) {
      setSystemIO();
    }
    log("started ConsoleAWT");
  }

  void stop() {
    if(ConsoleAWT.grabSystemIO) {
      restoreSystemIO();
    }
    setVisible(false);
    if(in != null) {
      in.close();
      in = null;
    }
  }
}
