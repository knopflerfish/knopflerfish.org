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
import javax.swing.*;

import java.net.*;

import org.knopflerfish.util.metatype.*;
import org.osgi.service.metatype.*;


public class CMDisplayer extends DefaultSwingBundleDisplayer {


  static ServiceTracker cmTracker;

  static Icon infoIcon = null;
  public CMDisplayer(BundleContext bc) {
    super(bc, "CM", "Config Admin", true);
    bUseListeners = true;

    if(infoIcon == null) {
      infoIcon     = new ImageIcon(getClass().getResource("/info16x16.png"));
    }
    cmTracker = new ServiceTracker(bc, 
				   ConfigurationAdmin.class.getName(), 
				   null);
    cmTracker.open();
  }

  static ConfigurationAdmin getCA() {
    return (ConfigurationAdmin)CMDisplayer.cmTracker.getService();
  }

  static Configuration getConfig(String pid) {
    try {
      Configuration[] configs = 
	getCA().listConfigurations("(service.pid=" + pid + ")");
      return configs[0];
    }  catch (Exception e) {
      //      e.printStackTrace();
      throw new IllegalArgumentException("No pid=" + pid);
    }
  }


  static boolean configExists(String pid) {
    try {
      Configuration c = getConfig(pid);
      return true;
    }  catch (Exception e) {
      return false;
    }
  }

  public JComponent newJComponent() {
    return new JCMAdmin();
  }

  public void  disposeJComponent(JComponent comp) {
    JCMAdmin cmAdmin = (JCMAdmin)comp;
    cmAdmin.stop();

    super.disposeJComponent(comp);
  }

  void closeComponent(JComponent comp) {
    JCMAdmin cmAdmin = (JCMAdmin)comp;
    cmAdmin.stop();    
  }

  public void showBundle(Bundle b) {
      // NYI
  }


  public void valueChanged(final long bid) {
    super.valueChanged(bid);

    SwingUtilities.invokeLater(new Runnable() {
	public void run() {
	  try {
	    for(Iterator it = components.iterator(); it.hasNext();) {
	      JCMAdmin cmAdmin = (JCMAdmin)it.next();
	      Bundle[] bl = bc.getBundles();
	      
	      for(int i = 0; i < bl.length; i++) {
		if(bundleSelModel.isSelected(bl[i].getBundleId())) {
		  cmAdmin.setBundle(bl[i]);
		}
	      }
	    }
	  } catch (Exception e) {
	    e.printStackTrace();
	  }
	}
      });
  }
  
  public Icon getSmallIcon() {
    return null;
  }

  class JCMAdmin extends JPanel {

    JCMInfo jcmInfo;

    public JCMAdmin() {
      setLayout(new BorderLayout());
      
      jcmInfo = new JCMInfo();
      add(jcmInfo, BorderLayout.CENTER);
    }

    public void setBundle(Bundle b) {
      try {
	MetaTypeInformation mtp = Activator.getMTP(b);
	jcmInfo.setProvider(mtp, b);
      } catch (Exception e) {
	e.printStackTrace();
	Activator.log.error("Failed to get MetaTypeInformation from bundle " + 
			    b.getBundleId(), e);
      }
    }

    public void stop() {
    }
  }
}

