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

import java.net.URL;
import java.awt.event.MouseListener;
import java.awt.event.ActionListener;
import javax.swing.JPopupMenu;

/**
 * Service interface that should be registered into framework by bundles
 * publishing "Tray Icons".
 *
 * <p>
 * As soon as a <tt>TrayIcon</tt> service is registered, the Tray Icon Manager
 * will pick up the service, read the name, icon, menu and start message, and
 * will try to publish the icon in a system dependent manner.
 * </p>
 *
 * <p>
 * </p>
 */
public interface TrayIcon 
  extends ActionListener, 
	  MouseListener
{

  public static final String PROP_NAME 
    = "org.knopflerfish.service.trayicon.name";

  /**
   * Unique id of this tray item.
   */
  public String getId();

  /**
   * Human-readable name of this tray item, displayed as tooltip.
   */
  public String getName();

  /**
   * URL pointing to image icon for the tray item.
   */
  public URL    getImageURL();

  /**
   * Initial start up message, displayed in a tray balloon, 
   * if possible.
   * 
   * @return Short startup message, <tt>null</tt> if no message should be 
   *         displayed.
   */
  public String getStartupMessage();

  /**
   * Get menu for this item. Can be null.
   *
   */
  public JPopupMenu getTrayJPopupMenu();

  /**
   * Called at clicks etc on the tray item.
   */
  public void trayEvent(TrayEvent ev);
}
