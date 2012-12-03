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

  public void activate(ExtensionContext extCtx) {
    this.extCtx = extCtx;

    resman = new ResourceManagerImpl(extCtx);
    resmanSR = extCtx.registerService
      (new String[]{ ResourceManager.class.getName() }, resman, null);

    extCtx.addBundleClassLoaderListener(this);
  }

  public void deactivate(ExtensionContext extCtx) {
    resmanSR.unregister();
    resmanSR = null;
  }

  /**
   *
   * @see org.knopflerfish.framework.BundleClassLoaderListener#bundleClassLoaderCreated
   */
  public void bundleClassLoaderCreated(BundleClassLoader bcl) {
    if (null!=resman) {
      resman.monitor(bcl);
    }
  }


}
