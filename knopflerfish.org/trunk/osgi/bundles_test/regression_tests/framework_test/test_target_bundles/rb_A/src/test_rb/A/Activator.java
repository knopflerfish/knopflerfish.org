package test_rb.A;

import org.osgi.framework.*;

public class Activator
  implements BundleActivator
{
  public void start(BundleContext bc)
  {
    System.out.println("Started.");
  }

  public void stop(BundleContext bc)
  {
    System.out.println("Stoped.");
  }

}
