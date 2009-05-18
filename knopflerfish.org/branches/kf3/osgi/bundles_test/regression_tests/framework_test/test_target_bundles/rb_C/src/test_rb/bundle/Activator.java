package test_rb.bundle;

import org.osgi.framework.*;
import test_rb.C.*;

import java.util.*;


public class Activator
  implements BundleActivator
{
  public void start(BundleContext bc)
  {
    C c = new C();
    D d = new D();
    test_rb.D.D dd = new test_rb.D.D();

    Dictionary cDict = new Hashtable();
    cDict.put("test_rb","C.C");
    cDict.put("toString",c.toString());
    bc.registerService(Object.class.getName(), c, cDict);

    Dictionary dDict = new Hashtable();
    dDict.put("test_rb","C.D");
    dDict.put("toString",d.toString());
    bc.registerService(Object.class.getName(), d, dDict);

    Dictionary ddDict = new Hashtable();
    ddDict.put("test_rb","D.D");
    ddDict.put("toString",dd.toString());
    bc.registerService(Object.class.getName(), dd, ddDict);

    System.out.println("Started.");
    System.out.println(c);
    System.out.println(d);
    System.out.println(dd);
  }

  public void stop(BundleContext bc)
  {
    System.out.println("Stoped.");
  }

}
