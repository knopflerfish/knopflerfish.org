package test_fb.A;

import org.osgi.framework.*;
import test_fapi.FragApi;

public class Activator implements BundleActivator
{
  public void start(BundleContext bc)
  {
    System.out.println("Start, " + (new FragApi()).getVersion());
  }

  public void stop(BundleContext bc)
  {
  }

}
