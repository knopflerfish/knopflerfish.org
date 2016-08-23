package test_fb.I;

import org.osgi.framework.*;
import test_fapi.FragApi;

public class Activator implements BundleActivator
{
  public void start(BundleContext bc)
  {
    try {
      FragApi fo = (FragApi)bc.getBundle().loadClass("test_fb.common.FragObject").newInstance();
      if (!"FRAG".equals(fo.where())) {
        throw new RuntimeException("Unexpected string: " + fo.where());
      }
    } catch (IllegalAccessException ia) {
      throw new RuntimeException("Class not instanciated: " + ia);
    } catch (InstantiationException ie) {
      throw new RuntimeException("Class not instanciated: " + ie);
    } catch (ClassNotFoundException cnfe) {
      throw new RuntimeException("Class not found: " + cnfe);
    }
  }

  public void stop(BundleContext bc)
  {
  }

}
