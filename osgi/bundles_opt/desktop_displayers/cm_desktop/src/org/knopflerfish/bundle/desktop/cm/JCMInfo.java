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
import org.knopflerfish.util.metatype.*;
import org.osgi.service.metatype.*;

public class JCMInfo extends JPanel {
  MetaTypeProvider mtp;
  PIDProvider      pp;

  JPanel main;
  JCMService jcmService;

  public JCMInfo() {
    super(new BorderLayout());

    main = new JPanel(new BorderLayout());
    jcmService = new JCMService();

    add(main, BorderLayout.CENTER);
  }

  JComboBox servicePIDBox = null;
  JComboBox factoryPIDBox = null;

  void setProvider(MetaTypeProvider _mtp, PIDProvider _pp) {
    this.mtp = _mtp;
    this.pp  = _pp;
    main.removeAll();

    if(mtp != null) {
      
      servicePIDBox = null;
      factoryPIDBox = null;
      
      Set services  = pp.getServicePIDs();
      Set factories = pp.getFactoryPIDs();

      String[] servicePIDs = null;
      if(services.size() > 0) {
	servicePIDs = new String[services.size()];
	
	
	int i = 0;
	for(Iterator it = services.iterator(); it.hasNext();) {
	  servicePIDs[i++] = (String)it.next();
	}
	
	servicePIDBox = new JComboBox(servicePIDs);
	servicePIDBox.addActionListener(new ActionListener() {	  
	    public void actionPerformed(ActionEvent ev) {	    
	      int ix = servicePIDBox.getSelectedIndex();
	      if(ix == -1) {
		return;
	      } else {
		String pid = (String)servicePIDBox.getSelectedItem();
		setServiceOCD(pid);
	      }
	    }
	  });
      }
      
      String[] factoryPIDs = null;
      if(factories.size() > 0) {
	factoryPIDs = new String[factories.size()];
	int i = 0;
	for(Iterator it = factories.iterator(); it.hasNext();) {
	  factoryPIDs[i++] = (String)it.next();
	}
	
	factoryPIDBox = new JComboBox(factoryPIDs);
	factoryPIDBox.addActionListener(new ActionListener() {	  
	    public void actionPerformed(ActionEvent ev) {	    
	      int ix = factoryPIDBox.getSelectedIndex();
	      if(ix == -1) {
		return;
	      } else {
		String pid = (String)factoryPIDBox.getSelectedItem();
		setFactoryOCD(pid);
	      }
	    }
	  });
      }
      
      JPanel upperBox = new JPanel(new GridLayout(0, 1));
      
      upperBox.setBorder(BorderFactory.createTitledBorder("Available PIDs"));
      
      if(servicePIDBox != null) {
	upperBox.add(new JLabelled("Services", "", servicePIDBox, 100));
      }
      if(factoryPIDBox != null) {
	upperBox.add(new JLabelled("Factories", "", factoryPIDBox, 100));
      }
      
      main.add(upperBox,   BorderLayout.NORTH);
      main.add(jcmService, BorderLayout.CENTER);

      // Set either the first service or the first factory as displayed
      if(servicePIDs != null) {
	setServiceOCD(servicePIDs[0]);
      } else {
	if(factoryPIDs != null) {
	  setFactoryOCD(factoryPIDs[0]);
	} else {
	  // Neither service nor factory found in provider
	}
      }
    }
    invalidate();
    revalidate();
    repaint();
  }
  
  void setServiceOCD(String pid) {
    ObjectClassDefinition ocd = 
      (ObjectClassDefinition)mtp.getObjectClassDefinition(pid, null);
    
    jcmService.setServiceOCD(ocd);
  }

  void setFactoryOCD(String pid) {
    ObjectClassDefinition ocd = 
      (ObjectClassDefinition)mtp.getObjectClassDefinition(pid, null);

    jcmService.setFactoryOCD(ocd);
  }
}
