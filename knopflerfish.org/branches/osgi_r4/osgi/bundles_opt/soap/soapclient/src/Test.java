import org.knopflerfish.client.*;
 
public class Test
{
    public static void main(String [] args) throws Exception {
        // Make a service
        RemoteAgentService service = new RemoteAgentServiceLocator();
 
        // Now use the service to get a stub which implements the SDI.
        RemoteAgent port = service.getremoteFW();
 
        // Make the actual call
        long[] bundles = port.getBundles();
	for (int i = 0; i < bundles.length; i++) {
	  String[] svs = port.getServices(bundles[i]);	
          System.out.println("Bundle "+bundles[i]+"  # services "+svs.length);
          for (int j = 0; j < svs.length; j++) {
            System.out.println("                             Service "+svs[j]);
          }
	}
    }
}
