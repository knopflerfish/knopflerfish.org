/*
 * Copyright (c) 2003-2013, KNOPFLERFISH project
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

import java.awt.BorderLayout;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.util.tracker.ServiceTracker;

public class CMDisplayer
  extends DefaultSwingBundleDisplayer
{

  static ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> cmTracker;

  static Icon infoIcon = null;

  public CMDisplayer(BundleContext bc)
  {
    super(bc, "CM", "Config Admin", true);
    bUseListeners = true;

    if (infoIcon == null) {
      infoIcon = new ImageIcon(getClass().getResource("/info16x16.png"));
    }
    cmTracker =
      new ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>(
                                                                 bc,
                                                                 ConfigurationAdmin.class,
                                                                 null);
    cmTracker.open();
  }

  static ConfigurationAdmin getCA()
  {
    return CMDisplayer.cmTracker.getService();
  }

  /**
   * Get the configuration object for the given PID.
   *
   * @param pid
   *          The PID to get the configuration for.
   * @return The specified configuration or {@code null} if no such
   *         configuration exists.
   */
  static Configuration getConfig(String pid)
  {
    if (pid != null) {
      try {
        final Configuration[] configs =
          getCA().listConfigurations("(service.pid=" + pid + ")");
        return configs[0];
      } catch (final Exception e) {
      }
    }
    return null;
  }

  static boolean configExists(String pid)
  {
    return getConfig(pid) != null;
  }

  /**
   * Find the PID of the best matching targeted configuration for the given
   * (non-targeted PID) and bundle.
   *
   * @param pid
   *          The PID to find a configuration for.
   * @param bundle
   *          The bundle to find the best configuration for.
   * @return The PID of the best existing targeted configuration or if no such
   *         configuration exists, the given PID.
   */
  static String getBestTargetedPIDWithConfig(final String pid, final Bundle bundle)
  {
    if (bundle == null) {
      return pid;
    }
    // "(|(PID=pid)(pid=PID|BSN)(pid=PID|BSN|Version)(pid=PID|BSN|Version|Location))"
    StringBuffer filter =
      new StringBuffer(
                       (pid.length() * 4 + Constants.SERVICE_PID.length() * 4 + 15) * 2);
    filter.append("(|(").append(Constants.SERVICE_PID).append('=').append(pid)
        .append(")(").append(Constants.SERVICE_PID).append('=').append(pid)
        .append('|').append(bundle.getSymbolicName()).append(")(")
        .append(Constants.SERVICE_PID).append('=').append(pid).append('|')
        .append(bundle.getSymbolicName()).append('|')
        .append(bundle.getVersion().toString()).append(")(")
        .append(Constants.SERVICE_PID).append('=').append(pid).append('|')
        .append(bundle.getSymbolicName()).append('|')
        .append(bundle.getVersion().toString()).append('|')
        .append(bundle.getLocation()).append("))");
    try {
      final Configuration[] configs =
        getCA().listConfigurations(filter.toString());
      if (configs==null) {
        return pid;
      }
      String res = pid;
      for (Configuration cfg : configs) {
        if (res.length()< cfg.getPid().length()) {
          res = cfg.getPid();
        }
      }
      return res;
    } catch (final Exception e) {
    }
    return pid;
  }

  static boolean isCmBundle(final Bundle bundle) {
    final Bundle cmBundle = cmTracker.getServiceReference().getBundle();
    return cmBundle != null ? cmBundle.equals(bundle)  : false;
  }

  static boolean isSystemBundle(final Bundle bundle) {
    return bundle != null && bundle.getBundleId() == 0L;
  }

  @Override
  public JComponent newJComponent()
  {
    JComponent newJComponent = new JCMAdmin();
    // Trigger update of the new JCMAdmin component with the current selection.
    valueChanged(0);
    return newJComponent;
  }

  @Override
  public void disposeJComponent(JComponent comp)
  {
    final JCMAdmin cmAdmin = (JCMAdmin) comp;
    cmAdmin.stop();

    super.disposeJComponent(comp);
  }

  @Override
  void closeComponent(JComponent comp)
  {
    final JCMAdmin cmAdmin = (JCMAdmin) comp;
    cmAdmin.stop();
  }

  @Override
  public void showBundle(Bundle b)
  {
    // NYI
  }

  @Override
  public void valueChanged(final long bid)
  {
    super.valueChanged(bid);

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run()
      {
        try {
          for (final JComponent jComponent : components) {
            final JCMAdmin cmAdmin = (JCMAdmin) jComponent;
            if (bundleSelModel.getSelectionCount() == 0) {
              cmAdmin.setBundle(null);
            } else {
              // This displayer can only handle single selections, if multiple
              // bundles are selected, present one of them.
              final Bundle oldSelection = cmAdmin.getBundle();
              if (oldSelection == null
                  || !bundleSelModel.isSelected(oldSelection.getBundleId())) {
                // Currently displayed bundle is no longer selected, display
                // another selected bundle.
                final long newBundleId = bundleSelModel.getSelected();
                cmAdmin.setBundle(-1 == newBundleId ? (Bundle) null : bc
                    .getBundle(newBundleId));
              }
            }
          }
        } catch (final Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  @Override
  public Icon getSmallIcon()
  {
    return null;
  }

  class JCMAdmin
    extends JPanel
  {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    JCMInfo jcmInfo;
    Bundle bundle;

    public JCMAdmin()
    {
      setLayout(new BorderLayout());

      jcmInfo = new JCMInfo();
      add(jcmInfo, BorderLayout.CENTER);
    }

    public void setBundle(Bundle b)
    {
      if ((bundle !=null && bundle.equals(b)) || (bundle==null && b==null)) {
        // No change of selection!
        return;
      }
      bundle = b;
      if (bundle == null) {
        jcmInfo.setProvider(null, null);
      } else {
        try {
          final MetaTypeInformation mti = Activator.getMTP(bundle);
          jcmInfo.setProvider(mti, bundle);
        } catch (final Exception e) {
          Activator.log.error("Failed to get MetaTypeInformation from bundle "
                              + b.getBundleId() +": " +e.getMessage(), e);
        }
      }
    }

    public Bundle getBundle() {
      return bundle;
    }

    public void stop()
    {
      jcmInfo.stop();
    }

  }
}
