
package org.knopflerfish.axis;

import org.apache.axis.AxisFault;
import org.apache.axis.MessageContext;
import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.description.ServiceDesc;
import org.apache.axis.handlers.BasicHandler;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.deployment.wsdd.WSDDDeployment;
import org.apache.axis.deployment.wsdd.WSDDDocument;
import org.apache.axis.deployment.wsdd.WSDDService;
import org.apache.axis.WSDDEngineConfiguration;
import org.apache.axis.transport.http.HTTPConstants;
import org.apache.axis.utils.XMLUtils;
import org.apache.axis.enum.Scope;
import org.apache.commons.logging.Log;

import org.osgi.framework.*;
import java.util.Hashtable;
import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;



/** An <code>OSGiServiceHandler</code> attempts to find the referenced service
 *  in the OSGi registry, and dynamically define a corresponding SOAP service.
 *
 * @author Lasse Helander (lars-erik.helander@home.se)
 */
public class OSGiServiceHandler extends BasicHandler
{
    protected static Log log =
        LogFactory.getLog(OSGiServiceHandler.class.getName());
	
    private String serviceWSDD(String serviceName, String className) {
       return "<service name=\""+serviceName+"\" provider=\"java:RPC\">" +
                "<parameter name=\"allowedMethods\" value=\"*\"/>" +
                "<parameter name=\"className\" value=\""+className+"\"/>" +
              "</service>";
    }	

    private String deploymentWSDD(String body) {
      return "<deployment" + 
                " xmlns=\"http://xml.apache.org/axis/wsdd/\""+
                " xmlns:java=\"http://xml.apache.org/axis/wsdd/providers/java\">"+
		body+
             "</deployment>";	    
    }
    
    private WSDDService findService(WSDDDeployment deployment,String name) {
      WSDDService[] ws = deployment.getServices(); 
      for (int i = 0; i < ws.length; i++) {
        WSDDService s = ws[i];
	if (ws[i].getServiceDesc().getName().equals(name)) return ws[i];
      }
      return null;
    }


    public void invoke(MessageContext msgContext) throws AxisFault
    {
        System.err.println("Enter: OSGiServiceHandler::invoke");
        log.debug("Enter: OSGiServiceHandler::invoke");

        // If there's already a targetService then just return.

         if (msgContext.getService() != null) return;
	 
	 String path = (String) msgContext.getProperty(HTTPConstants.MC_HTTP_SERVLETPATHINFO);
        System.err.println("OSGiServiceHandler.invoke() path " +path);
	 
	 int osgiSRindex = path.indexOf("osgi-sr/");
	 
        // If there's no "osgi-sr" in the path then just return.

	 if (osgiSRindex < 0) return;
        System.err.println("OSGiServiceHandler.invoke() index "+osgiSRindex);
	 try {
	   String filter = path.substring(osgiSRindex+8);	
	   String serviceName = "OSGi-SR:"+filter;	 
  	   WSDDEngineConfiguration config =  (WSDDEngineConfiguration) msgContext.getAxisEngine().getConfig();
	   WSDDDeployment deployment = config.getDeployment();
	   WSDDService ws = findService(deployment,serviceName); 
	   SOAPService service = null;
	   	  	   
	 
	   if (ws == null) {	 
             BundleContext bContext = org.knopflerfish.bundle.axis.Activator.axisBundle;
             Object serviceObject =  null;
             String sfilter = "("+filter+")";
             ServiceReference srs[] = bContext.getServiceReferences(null,sfilter);
             serviceObject =  bContext.getService(srs[0]);
	     
	     String addDoc = deploymentWSDD(serviceWSDD(serviceName,serviceObject.getClass().getName()));
	     WSDDDocument doc = new WSDDDocument(XMLUtils.newDocument(new ByteArrayInputStream(addDoc.getBytes())));
             doc.deploy(deployment);
             msgContext.getAxisEngine().refreshGlobalOptions();
	     
             deployment = config.getDeployment();
	     ws = findService(deployment,serviceName); 
        System.err.println("OSGiServiceHandler.invoke() QName "+ws.getQName());
	    	     
	     service = deployment.getService(ws.getQName());
             service.setOption("scope",Scope.APPLICATION_STR);	
	     msgContext.getAxisEngine().getApplicationSession().set(serviceName,serviceObject);

        System.err.println("OSGiServiceHandler.invoke() new service class "+serviceObject.getClass());
       
	   }
	     service = deployment.getService(ws.getQName());
        System.err.println("OSGiServiceHandler.invoke() new service "+service);
	    msgContext.setService(service);
	 } catch (Exception e) {e.printStackTrace();throw AxisFault.makeFault(e);}

        log.debug("Exit: OSGiServiceHandler::invoke");
        System.err.println("Exit: OSGiServiceHandler::invoke");
    }

    public void generateWSDL(MessageContext msgContext) throws AxisFault {
        invoke(msgContext);
    }
}
