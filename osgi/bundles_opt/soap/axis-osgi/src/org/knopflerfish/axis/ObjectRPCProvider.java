

package org.knopflerfish.axis;

import org.apache.axis.AxisFault;
import org.apache.axis.Constants;
import org.apache.axis.MessageContext;
import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.providers.java.RPCProvider;


import org.apache.commons.logging.Log;

import org.apache.axis.Handler;
import javax.xml.rpc.holders.IntHolder;



/**
 * Support RPC on specific object. Prior object must have been assigned
 * to SOAPService option
 *
 * @author Lasse Helander (lars-erik.helander@home.se)
 */
public class ObjectRPCProvider extends RPCProvider {
    protected static Log log =
            LogFactory.getLog(ObjectRPCProvider.class.getName());


    /**
     * Get the service object whose method actually provides the service.
     * Lookup object in MessageContext property (RPCObject)
     */
 
   public Object getServiceObject (MessageContext msgContext,
                                    Handler service,
                                    String clsName,
                                    IntHolder scopeHolder)
        throws Exception
    {
        Object preparedObject = msgContext.getService().getOption("serviceObject");
        return preparedObject;
 
    }

}
