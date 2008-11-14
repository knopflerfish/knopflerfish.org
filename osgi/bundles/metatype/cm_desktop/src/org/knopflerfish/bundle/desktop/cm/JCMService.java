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

package org.knopflerfish.bundle.desktop.cm;

import org.osgi.framework.*;
import org.osgi.service.cm.*;
import org.osgi.util.tracker.*;
import java.util.*;
import org.knopflerfish.service.desktop.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.io.*;

import org.knopflerfish.util.metatype.*;
import org.osgi.service.metatype.*;

public class JCMService extends JPanel {
  ObjectClassDefinition ocd;
  String	designatedPid;
  JPanel    main;

  boolean   isService = true;
  boolean   isFactory = false;

  JPanel    propPane;
  JPanel    mainPanel;

  // String -> AttributeDefinition
  Map props = new HashMap();
  String    factoryPid;

  public JCMService() {
    super(new BorderLayout());

    main = new JPanel(new BorderLayout());
    add(main, BorderLayout.CENTER);
  }


  void setServiceOCD(String pid, ObjectClassDefinition ocd) {
    this.ocd = ocd;
    this.designatedPid = pid;
    isService  = ocd != null;
    isFactory  =false;
    lastPID = null;
    updateOCD();

  }

  void setFactoryOCD(String pid, ObjectClassDefinition ocd) {
    this.ocd = ocd;
    this.designatedPid = pid;
    isService = false;
    isFactory = ocd != null;
    lastPID = null;
    updateOCD();
  }

  void updateOCD() {

    main.removeAll();

    props.clear();
    factoryPid  = null;

    if(ocd != null) {
      main.setBorder(JCMInfo.makeBorder(this, designatedPid));
      
      mainPanel = new JPanel(new BorderLayout());
      
      propPane = new JPanel();
      BoxLayout box = new BoxLayout(propPane, BoxLayout.Y_AXIS);
      propPane.setLayout(box);
      
      Dictionary configProps = null;
      
      try {
	Configuration conf = CMDisplayer.getConfig(designatedPid);
	configProps = conf.getProperties();
	//	System.out.println("using conf values");
	
      } catch (Exception e) {
	configProps = new Hashtable();
	//	System.out.println("using default values");
      }

      JButton facdelButton = null;


      AttributeDefinition[] reqAttrs 
	= ocd.getAttributeDefinitions(ObjectClassDefinition.REQUIRED);

      AttributeDefinition[] optAttrs
	= ocd.getAttributeDefinitions(ObjectClassDefinition.OPTIONAL);
      
      addAttribs(propPane, reqAttrs, configProps, "");
      
      if(reqAttrs != null && reqAttrs.length > 0) {
	if(optAttrs != null && optAttrs.length > 0) {
	  //
	}
      }
      
      addAttribs(propPane, optAttrs, configProps, " (optional)");

      JPanel propOuter = new JPanel(new BorderLayout());
      propOuter.add(propPane, BorderLayout.NORTH);
      propOuter.add(new JPanel(), BorderLayout.CENTER);

      JScrollPane scroll = new JScrollPane(propOuter);

      scroll.setPreferredSize(propPane.getPreferredSize());

      JPanel topPanel = new JPanel(new BorderLayout());


      JPanel ctrlPanel = new JPanel(new FlowLayout());

      JButton applyButton = new JButton("Apply");
      applyButton.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent ev) {
	    applyConfig(designatedPid);
	  }
	});
      applyButton.setToolTipText("Applies and stores the configuration changes");

      
      if(isFactory) {
	Configuration[] configs = null;
	try {
	  configs = 
	    CMDisplayer.getCA().listConfigurations("(service.factoryPid=" + 
						   designatedPid + ")");
	} catch (Exception e) {
	}
	
	JButton newButton   = new JButton("New");
	newButton.setToolTipText("Create a new factory configuration");
	
	newButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent ev) {
	      newFactoryConfig(designatedPid);
	    }
	  });
	ctrlPanel.add(newButton);
	
	if(configs != null) { 
	  
	  JButton facapplyButton   = new JButton("Apply");
	  facapplyButton.setToolTipText("Applies the currect changes to the factory config");
	  facapplyButton.addActionListener(new ActionListener() {
	      public void actionPerformed(ActionEvent ev) {
		applyFactoryConfig(factoryPid);
	      }
	    });
	  
	  

	  String[] fpids = new String[configs.length];
	  for(int i = 0; i < configs.length; i++) {
	    fpids[i] = configs[i].getPid();
	  }
	  final JComboBox fbox = new JComboBox(fpids);
	  fbox.addActionListener(new ActionListener() {	  
	      public void actionPerformed(ActionEvent ev) {	    
		int ix = fbox.getSelectedIndex();
		if(ix == -1) {
		  return;
		} else {
		  String pid = (String)fbox.getSelectedItem();
		  showFactoryConfig(pid);
		}
	      }
	    });

	  facdelButton = new JButton("Delete");
	  facdelButton.setToolTipText("Delete the selected factory configuration");
	  facdelButton.addActionListener(new ActionListener() {
	      public void actionPerformed(ActionEvent ev) {
		if(factoryPid == null) {
		  factoryPid = (String)fbox.getSelectedItem();
		}
		deleteFactoryPid(factoryPid);
	      }
	    });
	  
	  ctrlPanel.add(facdelButton);
	
	  ctrlPanel.add(facapplyButton);
	  ctrlPanel.add(fbox);


	  if(lastPID != null) {
	    fbox.setSelectedItem(lastPID);
	    showFactoryConfig(lastPID);
	  }
	}
      } else {
	if(CMDisplayer.configExists(designatedPid)) {
	  JButton delButton   = new JButton("Delete");
	  delButton.setToolTipText("Delete configuration");
	  delButton.addActionListener(new ActionListener() {
	      public void actionPerformed(ActionEvent ev) {
		deleteConfig(designatedPid);
	      }
	    });
	  ctrlPanel.add(applyButton);
	  ctrlPanel.add(delButton);
	  
	} else {
	  JButton createButton   = new JButton("Create");
	  createButton.setToolTipText("Create configuration from values below");
	  createButton.addActionListener(new ActionListener() {
	      public void actionPerformed(ActionEvent ev) {
		createConfig(designatedPid);
	      }
	    });

	  JLabel createInfo = new JLabel("Create from values below");
	  ctrlPanel.add(createButton);
	  ctrlPanel.add(createInfo);
	}
      }
      
      //      ctrlPanel.add(new JLabel(designatedPid), BorderLayout.CENTER);
      
      scroll.setBorder(null);
      mainPanel.add(scroll, BorderLayout.CENTER);
      
      topPanel.add(ctrlPanel, BorderLayout.WEST);

      JPanel icons = new JPanel(new FlowLayout());
      

      JLabel iconLabel = null;

      InputStream iconStream = null;
      try {
	iconStream = ocd.getIcon(16);
	if(iconStream != null) {
	  try {
	    ImageIcon icon = new ImageIcon(loadStream(iconStream));
	    iconLabel = new JLabel(icon);
	    icons.add(iconLabel);
	  } catch (Exception e) {
	    Activator.log.error("Failed to load icon", e);
	  }
	}
      } catch (Exception e) {
	Activator.log.error("Failed to get icon stream", e);
      }

      String desc = ocd.getDescription();
      if(desc != null && !"".equals(desc)) {
	JLabel infoLabel = new JLabel(CMDisplayer.infoIcon);
	String tt = "<html>" + ocd.getDescription() + "</html>";
	infoLabel.setToolTipText(tt);
	icons.add(infoLabel);
	if(iconLabel != null) {
	  iconLabel.setToolTipText(tt);
	}
      }

      topPanel.add(icons, BorderLayout.EAST);

      main.add(topPanel, BorderLayout.NORTH);
      main.add(mainPanel, BorderLayout.CENTER);
    }
    
    invalidate();
    revalidate();
    repaint();
  }

  void addAttribs(JComponent propPane, 
		  AttributeDefinition[] ads,
		  Dictionary configProps,
		  String info) {
    for(int i = 0; ads != null && i < ads.length; i++) {
      AttributeDefinition ad = ads[i];
      
      JLabelled item = null;
      
      try {
	JCMProp jcmProp = new JCMProp(ad, configProps);
	
	props.put(ad.getID(), jcmProp);
	
	String className = AD.getClass(ad.getType()).getName();
	
	if(ad.getCardinality() < 0) {
	  className = "Vector of " + className;
	} else if(ad.getCardinality() > 0) {
	  className = className + "[]";
	}
	item = 
	  new JLabelled(ad.getName(), 
			"<b>" + ad.getName() + "</b>" + info + "<br>" + 
			ad.getDescription() +  "<br>" + 
			" (" + className + ")",
			jcmProp,
			100);
	
      } catch (Exception e) {
	Activator.log.error("Failed to create ui for " + ad, e);
	item = 
	  new JLabelled(ad.getName(), 
			ad.getDescription(),
			new JLabel(e.getMessage()),
			100);
	
      }
      propPane.add(item);
    }
  }

  /**
   * Load a stream into a byte array.
   */
  byte [] loadStream(InputStream is) throws IOException {
    int     bufSize = 1024 * 2;
    byte [] buf     = new byte[bufSize];

    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    BufferedInputStream   in   = null;
    try {
      in = new BufferedInputStream(is);
      int n;
      while ((n = in.read(buf)) > 0) {
	bout.write(buf, 0, n);
      }
      return bout.toByteArray();
    } finally {
      try { in.close(); } catch (Exception ignored) { } 
    }
  }

  void showFactoryConfig(String pid) {
    //    System.out.println("showFactoryConfig " + pid);
    
    try {
      Configuration conf = 
	CMDisplayer.getCA().getConfiguration(pid, null);

      setProps(conf.getProperties());
      factoryPid = pid;
    } catch (Exception e) {
      Activator.log.error("show factory failed pid=" + pid, e);
    }
  }
  
  void deleteFactoryPid(String pid) {
    //    System.out.println("deleteFactoryConfig " + pid);
    try {
      Configuration conf = CMDisplayer.getCA()
	.getConfiguration(pid, null);
      conf.delete();
      lastPID = null;
      updateOCD();
    } catch (Exception e) {
      Activator.log.error("delete factory failed pid=" + pid, e);

    }
  }

  void applyFactoryConfig(String pid) {
    //    System.out.println("applyFactoryConfig " + pid);
    try {
      Dictionary props = getProps();
      
      //      System.out.println("props=" + props);
      try {
	Configuration conf = CMDisplayer.getCA()
	  .getConfiguration(pid, null);
	conf.update(props);
	lastPID = conf.getPid();
	updateOCD();
      } catch (Exception e) {
	Activator.log.error("apply factory failed pid=" + pid, e);
      }
    } catch (Exception e) {
      Activator.log.error("failed to get props pid=" + pid, e);
    }
  }


  void newFactoryConfig(String pid) {
    //    System.out.println("newFactoryConfig " + pid);

    try {
      Dictionary props = getProps();
      
      //      System.out.println("props=" + props);
      try {
	Configuration conf = 
	  CMDisplayer.getCA().createFactoryConfiguration(pid, null);
	lastPID = conf.getPid();
	conf.update(props);
	updateOCD();
      } catch (Exception e) {
	showError("new factory failed pid=" + pid, e);
      }
    } catch (Exception e) {
      Activator.log.error("failed to get props pid=" + pid, e);
    }
  }
  
  void deleteConfig(String pid) {
    //    System.out.println("deleteConfig " + pid);
    lastPID = null;
    try {
      Configuration conf = CMDisplayer.getCA().getConfiguration(pid, null);
      conf.delete();
      updateOCD();
    } catch (Exception e) {
      showError("Delete failed pid=" + pid, e);

    }
  }

  void showError(String msg, Throwable t) {
    Activator.log.error(msg, t);
    JOptionPane.showMessageDialog(this,
				  msg + "\n" + 
				  t.toString(),
				  msg,
				  JOptionPane.ERROR_MESSAGE);
  }

  void createConfig(String pid) {
    //    System.out.println("createConfig " + pid);

    try {
      Dictionary props = getProps();
      
      //      System.out.println("props=" + props);
      try {
	Configuration conf = CMDisplayer.getCA().getConfiguration(pid, null);
	conf.update(props);
	lastPID = pid;
	updateOCD();
      } catch (Exception e) {
	Activator.log.error("Failed to create/update pid=" + pid, e);
      }
    } catch (Exception e) {
      Activator.log.error("Failed to get props for pid=" + pid, e);
    }
  }

  String lastPID = null;

  void applyConfig(String pid) {
    //System.out.println("applyConfig " + pid);
    try {
      Dictionary props = getProps();
      
      //      System.out.println("props=" + props);
      try {
	Configuration conf = CMDisplayer.getCA().getConfiguration(pid, null);
	conf.update(props);
	lastPID = pid;
	updateOCD();
      } catch (Exception e) {
	Activator.log.error("Failed to apply/update pid=" + pid, e);
      }
    } catch (Exception e) {
      Activator.log.error("Failed to get props for pid=" + pid, e);
    }
  }

  void setProps(Dictionary in) {

    //    System.out.println("setProps " + in);

    int errCount = 0;
    for(Iterator it = props.keySet().iterator(); it.hasNext(); ) {
      String  name    = (String)it.next();
      JCMProp jcmProp = (JCMProp)props.get(name);
      try {
	Object val      = in.get(name);

	jcmProp.setValue(val);
	jcmProp.setErr(null);
      } catch (Exception e) {
	errCount++;
	jcmProp.setErr(e.getMessage());
      }
    }

    mainPanel.invalidate();
    mainPanel.revalidate();
    mainPanel.repaint();

  }

  Dictionary getProps() {
    Hashtable out = new Hashtable();

    int errCount = 0;
    for(Iterator it = props.keySet().iterator(); it.hasNext(); ) {
      String  name    = (String)it.next();
      JCMProp jcmProp = (JCMProp)props.get(name);
      try {
	Object val      = jcmProp.getValue();
	
	out.put(name, val);
	jcmProp.setErr(null);
      } catch (Exception e) {
	errCount++;
	jcmProp.setErr(e.getMessage());
	//	System.out.println(name + ": " + e);
	Activator.log.error("Failed to convert value in " + name, e); 
      }
    }

    mainPanel.invalidate();
    mainPanel.revalidate();
    mainPanel.repaint();

    if(errCount > 0) {
      throw new IllegalArgumentException("Failed to convert " + errCount + " values");
    }
    return out;
  }
}
