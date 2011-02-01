package test_fb.A;

import org.osgi.framework.*;
import test_fapi.FragApi;
import test_fb.common.FragObject;

public class FragService implements ServiceFactory
{

  public Object getService(Bundle b, ServiceRegistration sr) {
    return new FragObject();
  }

  public void ungetService(Bundle b, ServiceRegistration sr, Object s) {
  }

}
