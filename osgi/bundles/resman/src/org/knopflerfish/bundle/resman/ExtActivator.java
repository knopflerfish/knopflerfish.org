package org.knopflerfish.bundle.resman;


import org.knopflerfish.framework.BundleClassLoader;
import org.knopflerfish.framework.BundleClassLoaderListener;
import org.knopflerfish.framework.ExtensionContext;
import org.osgi.framework.ServiceRegistration;

import org.knopflerfish.service.resman.ResourceManager;


public class ExtActivator implements BundleClassLoaderListener
{
  static ExtensionContext extCtx;
  private ResourceManagerImpl resman;
  private ServiceRegistration resmanSR;

  public void activate(final ExtensionContext extCtx) {
    ExtActivator.extCtx = extCtx;

    resman = new ResourceManagerImpl();
    resmanSR = extCtx.registerService
      (new String[]{ ResourceManager.class.getName() }, resman, null);

    extCtx.addBundleClassLoaderListener(this);
  }

  public void deactivate(final ExtensionContext extCtx) {
    resmanSR.unregister();
    resmanSR = null;
    final ResourceManagerImpl resmanOld = resman;
    resman = null;
    resmanOld.unmonitor();
  }

  /**
   *
   * @see org.knopflerfish.framework.BundleClassLoaderListener#bundleClassLoaderCreated
   */
  public void bundleClassLoaderCreated(final BundleClassLoader bcl) {
    if (null!=resman) {
      resman.monitor(bcl);
    }
  }

  /**
   *
   * @see org.knopflerfish.framework.BundleClassLoaderListener#bundleClassLoaderClosed
   */
  public void bundleClassLoaderClosed(final BundleClassLoader bcl) {
    if (null!=resman) {
      resman.unmonitor(bcl);
    }
  }


}
