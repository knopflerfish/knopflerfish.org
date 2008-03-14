
/**
 * MySoapTestServiceCallbackHandler.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.3  Built on : Aug 10, 2007 (04:45:47 LKT)
 */

    package org.knopflerfish.client.axis2_soapobject;

    /**
     *  MySoapTestServiceCallbackHandler Callback class, Users can extend this class and implement
     *  their own receiveResult and receiveError methods.
     */
    public abstract class MySoapTestServiceCallbackHandler{



    protected Object clientData;

    /**
    * User can pass in any object that needs to be accessed once the NonBlocking
    * Web service call is finished and appropriate method of this CallBack is called.
    * @param clientData Object mechanism by which the user can pass in user data
    * that will be avilable at the time this callback is called.
    */
    public MySoapTestServiceCallbackHandler(Object clientData){
        this.clientData = clientData;
    }

    /**
    * Please use this constructor if you don't want to set any clientData
    */
    public MySoapTestServiceCallbackHandler(){
        this.clientData = null;
    }

    /**
     * Get the client data
     */

     public Object getClientData() {
        return clientData;
     }

        
           /**
            * auto generated Axis2 call back method for getName method
            * override this method for handling normal response from getName operation
            */
           public void receiveResultgetName(
                    org.knopflerfish.client.axis2_soapobject.MySoapTestServiceStub.GetNameResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getName operation
           */
            public void receiveErrorgetName(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for add method
            * override this method for handling normal response from add operation
            */
           public void receiveResultadd(
                    org.knopflerfish.client.axis2_soapobject.MySoapTestServiceStub.AddResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from add operation
           */
            public void receiveErroradd(java.lang.Exception e) {
            }
                
               // No methods generated for meps other than in-out
                
           /**
            * auto generated Axis2 call back method for getBean method
            * override this method for handling normal response from getBean operation
            */
           public void receiveResultgetBean(
                    org.knopflerfish.client.axis2_soapobject.MySoapTestServiceStub.GetBeanResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from getBean operation
           */
            public void receiveErrorgetBean(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for addToAll method
            * override this method for handling normal response from addToAll operation
            */
           public void receiveResultaddToAll(
                    org.knopflerfish.client.axis2_soapobject.MySoapTestServiceStub.AddToAllResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from addToAll operation
           */
            public void receiveErroraddToAll(java.lang.Exception e) {
            }
                
           /**
            * auto generated Axis2 call back method for mul method
            * override this method for handling normal response from mul operation
            */
           public void receiveResultmul(
                    org.knopflerfish.client.axis2_soapobject.MySoapTestServiceStub.MulResponse result
                        ) {
           }

          /**
           * auto generated Axis2 Error handler
           * override this method for handling error response from mul operation
           */
            public void receiveErrormul(java.lang.Exception e) {
            }
                


    }
    