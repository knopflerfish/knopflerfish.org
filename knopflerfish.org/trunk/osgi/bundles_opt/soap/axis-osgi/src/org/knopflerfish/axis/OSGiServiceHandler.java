/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Axis" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.knopflerfish.axis;

import org.apache.axis.AxisFault;
import org.apache.axis.MessageContext;
import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.description.ServiceDesc;
import org.apache.axis.handlers.BasicHandler;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;

import org.osgi.framework.*;
import java.util.Hashtable;
import javax.xml.namespace.QName;



/** An <code>OSGiServiceHandler</code> attempts to find the referenced service
 *  in the OSGi registry, and dynamically define a corresponding SOAP service.
 *
 * @author Lasse Helander (lars-erik.helander@home.se)
 */
public class OSGiServiceHandler extends BasicHandler
{
    protected static Log log =
        LogFactory.getLog(OSGiServiceHandler.class.getName());
	
    private static  org.knopflerfish.axis.ObjectRPCProvider pivotHandler = null;


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
        System.err.println("OSGiServiceHandler.invoke() filter "+filter);
	 
  	   SimpleProvider config = (SimpleProvider) msgContext.getAxisEngine().getConfig();
	   QName serviceQName= new QName("osgi-sr",filter);
	   SOAPService service = config.getService(serviceQName);
        System.err.println("OSGiServiceHandler.invoke() old service "+service);
	 
	   if (service == null) {	 
             if (pivotHandler == null) pivotHandler = new org.knopflerfish.axis.ObjectRPCProvider();
             service = new SOAPService(null,pivotHandler,null);
             ServiceDesc serviceDesc = new ServiceDesc();
	     
	     serviceDesc.setName(path.substring(osgiSRindex));
	     
             BundleContext bContext = org.knopflerfish.bundle.axis.Activator.axisBundle;
             Object serviceObject =  null;
             String sfilter = "("+filter+")";
             ServiceReference srs[] = bContext.getServiceReferences(null,sfilter);
             serviceObject =  bContext.getService(srs[0]);
             serviceDesc.loadServiceDescByIntrospection(serviceObject.getClass());
       
             Hashtable options = new Hashtable();
             options.put("allowedMethods","*");
             options.put("className",serviceObject.getClass().getName());
             options.put("serviceObject",serviceObject);
             service.setOptions(options);
             service.setServiceDescription(serviceDesc);
             config.deployService(serviceQName,service);
        System.err.println("OSGiServiceHandler.invoke() new service class "+serviceObject.getClass());
       
	   }
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
