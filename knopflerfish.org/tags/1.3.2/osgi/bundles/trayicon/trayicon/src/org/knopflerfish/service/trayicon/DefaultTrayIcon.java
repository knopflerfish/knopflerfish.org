/*
 * Copyright (c) 2004, KNOPFLERFISH project
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

package org.knopflerfish.service.trayicon;

import org.osgi.framework.*;

import java.net.URL;
import java.util.Hashtable;

import java.awt.event.MouseListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;

import javax.swing.JPopupMenu;

/**
 * Default implementation of TrayIcon.
 *
 */
public class DefaultTrayIcon implements TrayIcon
{
  String id;
  String name;
  URL imageURL;
  
  ServiceRegistration reg;
  BundleContext bc;
  public DefaultTrayIcon(BundleContext bc,
			 String id, 
			 String name, 
			 URL imageURL) {
    this.bc       = bc;
    this.id       = id;
    this.name     = name;
    this.imageURL = imageURL;
  }

  /**
   * Register this instance as a <tt>TrayIcon</tt> service into the framework.
   *
   * <p>
   * The properties used for registering will contain:
   * <pre>
   *  TrayIcon.PROP_NAME    = getName()
   *  Constants.SERVICE_PID = getId();
   * </pre>
   * </p>
   */
  public void register() {
    if(reg == null) {
      Hashtable props = new Hashtable();
      props.put(PROP_NAME,             getName());
      props.put(Constants.SERVICE_PID, getId());

      reg = bc.registerService(TrayIcon.class.getName(),
			       this, 
			       props);
    }
  }

  /**
   * Unregister this instance from the framework.
   */
  public void unregister() {
    if(reg != null) {
      reg.unregister();
    }
    reg = null;
  }


  /**
   * Returns id set in constructor.
   */
  public String getId() {
    return id;
  }

  /**
   * Returns name set in constructor.
   */
  public String getName() {
    return name;
  }

  /**
   * Returns image URL set in constructor.
   */
  public URL    getImageURL() {
    return imageURL;
  }

  /**
   * Default does nothing.
   */
  public String getStartupMessage() {
    return null;
  }

  /**
   * Default does nothing.
   */
  public void actionPerformed(ActionEvent e) {
  }

  /**
   * Default does nothing.
   */
  public void trayEvent(TrayEvent e) {
  }

  /**
   * Default does nothing.
   */
  public void mouseClicked(MouseEvent e) {
  }

  /**
   * Default does nothing.
   */
  public void mouseEntered(MouseEvent e) {
  }

  /**
   * Default does nothing.
   */
  public void mouseExited(MouseEvent e) {
  }

  /**
   * Default does nothing.
   */
  public void mousePressed(MouseEvent e) {
  }
    
  /**
   * Default does nothing.
   */
  public void mouseReleased(MouseEvent e) {
  }

  /**
   * Default returns <tt>null</tt>
   */
  public JPopupMenu getTrayJPopupMenu() {
    return null;
  }
 }
