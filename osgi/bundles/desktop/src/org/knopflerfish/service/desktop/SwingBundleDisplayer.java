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

package org.knopflerfish.service.desktop;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;

import javax.swing.JComponent;
import javax.swing.Icon;

/**
 *
 * Interface for services wishing to become visible as components in
 * the desktop Swing window.
 *
 * <p>
 * The desktop window can be extended by new components, displaying
 * bundle details. To extend the desktop, create and register an instance
 * of <tt>SwingBundleDisplayer</tt>.
 * </p>
 * <p>
 * In fact, all internal views in the desktop bundle
 * are created by such services.
 * </p>
 *
 * <h3>Service properties</h3>
 *
 * All <tt>SwingBundleDisplayer</tt> should be registered with a set of
 * properties:
 * <ul>
 * <li><tt>SwingBundleDisplayer.PROP_NAME</tt> (String)<br>
 *     Short name of the displayer. Will typically be used
 *     in tab names or menu selections.
 * <li><tt>SwingBundleDisplayer.PROP_DESCRIPTION</tt> (String)<br>
 *     Longer description of the displayer. Will typically be
 *     used as tooltip text.
 * <li><tt>SwingBundleDisplayer.PROP_ISDETAIL</tt> (Boolean)<br>
 *     If <tt>Boolean.TRUE</tt>, the displayer provides components
 *     with high detail level for a single, selected bundles. Detail
 *     displayers are typically shown in the right-hand "info" part
 *     of the desktop window.<br>
 *     If this property is not set, or equals <tt>Boolean.FALSE</tt>,
 *     the displayer can handle a view of all installed bundes. These
 *     displayers are typically shown in the left-hand "bundle" part
 *     of the desktop window.
 * </ul>
 * </p>
 *
 * <p>
 * Additionally, displayers may provide an icon which will be displayed
 * next to the name/description.
 * </p>
 *
 * <p>
 * The bundle selection is described by an <a href="BundleSelectionModel.html"><tt>BundleSelectionModel</tt></a>,
 * and displayer should be prepared to display different content
 * depending on which bundles are selected. To modify the selection
 * (and thus update all other registered displayers), use the
 * <tt>setSelection</tt> and <tt>clearSelection</tt> methods in the
 * <tt>BundleSelectionModel</tt>
 * </p>
 *
 * <h3>Desktop usage of Bundle attributes</h3>
 *
 * When the desktop displays bundle information, some bundle
 * attributes are used. The same method should be used by bundle
 * displayers.
 *
 * <dl>
 * <dt><b>Bundle-Activator</b>
 * <dd>The <tt>Bundle-Activator</tt> is used to select from
 *     the built in "application" or "library" icons. If the
 *     bundle has a bundle activator set, the application icon
 *     is selected, otherwise the library icon.
 *
 * <dt><b>Application-Icon</b>
 * <dd>If <tt>Application-Icon</tt> is set to a resource name
 *     of an icon, this icon is used insead of the built in
 *     application of library icons. The icon is accessed using
 *     the <tt>bundle://[bundle id]</tt> URL scheme.
 * </dl>
 */
public interface SwingBundleDisplayer {

  /**
   * Service Property (String)
   * <p>
   * Value is <tt>org.knopflerfish.service.desktop.displayer.name</tt>
   * </p>
   */
  public final static String PROP_NAME =
    "org.knopflerfish.service.desktop.displayer.name";

  /**
   * Service Property (String)
   * <p>
   * Value is <tt>org.knopflerfish.service.desktop.displayer.description</tt>
   * </p>
   */
  public final static String PROP_DESCRIPTION =
    "org.knopflerfish.service.desktop.displayer.description";

  /**
   * Service Property (Boolean)
   * <p>
   * Value is <tt>org.knopflerfish.service.desktop.displayer.isdetail</tt>
   * </p>
   */
  public final static String PROP_ISDETAIL =
    "org.knopflerfish.service.desktop.displayer.isdetail";


  /**
   * Create the actual component that should be displayed.
   *
   * <p>
   * New components <b>must</b> be created for each <tt>createJComponent</tt>
   * call.
   * </p>
   */
  public JComponent createJComponent();

  /**
   * Perform any necessary cleanup operations.
   *
   * @param comp Component previously created by <tt>createJComponent</tt>
   */
  public void       disposeJComponent(JComponent comp);

  /**
   * @param model Selection model describing which bundles are
   *              selected. The actual bundle list must be managed
   *              by the displayer itself.
   */
  public void       setBundleSelectionModel(BundleSelectionModel model);

  /**
   * Get a large icon (48x48) for the displayer.
   *
   * <p>
   * <tt>null</tt> can be returned if no icon is provided.
   * </p>
   */
  public Icon       getLargeIcon();

  /**
   * Get a smaller icon (22x22) for the displayer.
   *
   * <p>
   * <tt>null</tt> can be returned if no icon is provided.
   * </p>
   */
  public Icon       getSmallIcon();

  /**
   * Allow access to another bundle context than the displayer's
   * own. This might be used to set a remote bundle context.
   *
   * <p>
   * This method might never be called - the displayer should
   * in that case use its own context. The normal case is to start
   * with the displayer's own context and later switch to another
   * context.
   * </p>
   *
   * <p>
   * If <t>setTargetBundleContext</tt> is called, the displayer
   * must update all components with this context.
   * </p>
   */
  public void       setTargetBundleContext(BundleContext bc);

  /**
   * Attempt to show the specified bundle.
   */
  public void showBundle(Bundle b);

}
