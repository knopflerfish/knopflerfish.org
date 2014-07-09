package test_fb.A;

import org.osgi.framework.*;
import test_fapi.FragApi;

public class Activator implements BundleActivator
{

  public void start(BundleContext bc)
  {
    bc.registerService(FragApi.class.getName(), new FragService(), null);
  }

  public void stop(BundleContext bc)
  {
  }

}
