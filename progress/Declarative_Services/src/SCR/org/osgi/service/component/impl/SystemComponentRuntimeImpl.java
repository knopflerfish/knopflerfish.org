/*
 * @(#)SystemComponentRuntimeImpl.java        1.0 2005/06/28
 *
 * Copyright (c) 2003-2005 Gatespace telematics AB
 * Otterhallegatan 2, 41670,Gothenburgh, Sweden.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of 
 * Gatespace telematics AB. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Gatespace telematics AB.
 */

package org.osgi.service.component.impl;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;

/**
 * This class is the implementation of the declarative service feature. It will
 * locate and bind diffrent types of declared components on demand. It will also
 * listen to BundleEvents rasied within the framework and
 */
public class SystemComponentRuntimeImpl implements BundleListener {
	/** variable holding the bundlecontext */
	private BundleContext bundleContext;

	/** variable holding the custom parser object */
	private CustomParser customParser;

	public SystemComponentRuntimeImpl(BundleContext context) {
		/* assign the bundlecontext */
		bundleContext = context;
		/* add this as a bundle listener */
		bundleContext.addBundleListener(this);
	}

	/**
	 * Listen for BundleEvents from the framework and
	 * creates a ComponentDeclaration 
	 * @throws BundleException
	 * 
	 * @throws IOException
	 */
	public synchronized void bundleChanged(BundleEvent event) {
		if(event.getBundle().getHeaders().get("Service-Component") !=null){
			
			System.out.println("\n\n******************* Parsing Started *****************************");
			/* create the parser */
			customParser = new CustomParser();
			/* parse the document and retrieve a component declaration */
			ComponentDeclaration componentDeclaration = customParser.readXML(event);
			
			/* check if the declaration is satisfied */
			if(isSatisfied(componentDeclaration)){
				
				/* activate the component i.e create an instance and */
				ComponentContext componentContext = createComponentContext(componentDeclaration);
				/* activate the component */
				activateInstance(componentContext);
				
				/* check if the component declares references */
				if(componentDeclaration.getReferenceInfo().size()>0){
					/* if so bind the references declared */
					bindReferences(componentDeclaration,componentContext.getComponentInstance());
				}
				
				/* check if the component is enabled */
				if(componentDeclaration.isAutoEnable()){
					/* register its services */
					registerComponentServices(componentDeclaration,componentContext);
				}else{
					System.out.println("****************** " + event.getBundle().getHeaders().get("Bundle-Name") +
						" is not enabled saving data for later *************************");
					/*
					 * Save the component data here nothing more or less
					 */
				}
				
			} else {
				/*
				 * Save the component data here nothing more or less
				 */
				
				/* print that this bundles component is not satisfied */ 
				System.out.println("****************** " + event.getBundle().getHeaders().get("Bundle-Name") +
						" is not satisfied *************************");
				
			}
				
		}// if(event.getBundle().getHeaders().get("Service-Component") !=null)

	}
	
	
	/**
	 * This method will activate a component using reflection 
	 * Some component declares a activate(ComponentContext context)
	 * method just because they want the ComponentContext. This method will
	 * try to call the method and pass the context as argument. This method may 
	 * fail then a component doesn't declare an activate method.
	 * 
	 * @param ComponentContext the context which should be passed
	 */
	private void activateInstance(ComponentContext componentContext){
		/* get the component instance */
		Object componentInstance = componentContext.getComponentInstance().getInstance();
		/*  create a string representing the method name */
		String methodName = "activate";
		try{
			System.out.println("The instance is:" + componentInstance);
			
			Method method =componentInstance.getClass().getDeclaredMethod(methodName, 
					new Class[]{ComponentContext.class});
			/* set this as accessible */
			method.setAccessible(true);
			/* invoke the method */
			method.invoke(componentInstance,new Object[]{componentContext});
			
		}catch(NoSuchMethodException e){
			System.out.println("************* Component " + componentContext.getProperties().get("component.name") +
					" does not declare an activate method Ignoring **************");
			
		}catch(InvocationTargetException e){
			System.err.println("error in activateInstance:" +e);
		}catch(IllegalAccessException e){
			System.err.println("error in activateInstance:" +e);
		}
		
	}
	
	/**
	 * this method checks if a ComponentDeclaration is satisfied or not.
	 * It will check that all references/dependencies are available.
	 *   
	 * @param componentDeclaration
	 * @return true if satisfies false otherwise
	 */
	private boolean  isSatisfied(ComponentDeclaration componentDeclaration){
		
		/* always assume that the component is satisfied */
		boolean isSatisfied=true;
		
		
		/* create an arraylist with the components reference infos */
		ArrayList componentReferences  = componentDeclaration.getReferenceInfo();
		
		/* create an iterator */
		Iterator referenceIterator = componentReferences.iterator();
		
		while(referenceIterator.hasNext()){
			/* create the a temporary object */
			ComponentReferenceInfo componentReference = (ComponentReferenceInfo) referenceIterator
					.next();
			
			/* get the policy */
			String policy =  componentReference.getPolicy();
			/* get the cardinality */
			String cardinality= componentReference.getCardinality();
			/* get the refered interface */
			String interfaceName = componentReference.getInterfaceType();
			/* get the service reference */
			ServiceReference reference = bundleContext.getServiceReference(interfaceName);
			
			/* check cardinality */
			if((cardinality.equals("1..1") || cardinality.equals("1..n"))&& reference ==null){
						/* this service must be available */
						return false;
			}
			
		}
		
		
		return isSatisfied;
	}
	
	
	/**
	 * this method registers services declared in the ComponentDeclaration
	 * 
	 * @param componentDeclaration the component declaration
	 * @param componentContext the componentContext
	 */
	private void registerComponentServices(
			ComponentDeclaration componentDeclaration,
			ComponentContext componentContext) {
	
		/* get all the service */
		ArrayList services = componentDeclaration.getServiceInfo();
		/* create an iterator to iterate through the services */
		Iterator iteratorServices = services.iterator();
		
		
		/* iterate */
		while(iteratorServices.hasNext()){
			/* create a service info */
			ComponentServiceInfo serviceInfo = (ComponentServiceInfo)iteratorServices.next();
			/* get all the interfaces */
			ArrayList interfaces = serviceInfo.getComponentInterfaces();
			/* create an iterator variable */
			Iterator iteratorInterfaces = interfaces.iterator();
			
			/* iterate throgh all the services */
			while(iteratorInterfaces.hasNext()){
				
				/* create a temporary string variable */
				String serviceInterface = (String)iteratorInterfaces.next();
		        /* variable holding the componentFactory attribute */
		        String componentFactory = componentDeclaration.getFactory();
		        /* variable holding the service factory attribute */
		        boolean isServiceFactory = serviceInfo.isServiceFactory();
		        
		        if(componentFactory!=null && isServiceFactory){
		        	System.err.println("**************** CHECK IF CAN BE BOTH COMPONENT FACTORY AND SERVICE FACTORY *************");
		        } else {
		        	/* check if it is a single service and register if so */
		        	if(componentFactory==null && !isServiceFactory){
		        		 /*  create the hashtable */
				        Hashtable propsTable = new Hashtable();
				        /* put some properties in the table */
		        		propsTable.put(ComponentConstants.COMPONENT_NAME, componentDeclaration.getComponentName());
		  		        /* print that we register a service */
		  		        System.out.println("SCR registers single instance service for interface:"+ serviceInterface );
		  		        
		  		        /* register the service to the framework use the componentInstance */
		  		        try{       	 		        
		  		        	
		  		        	bundleContext.registerService(serviceInterface
		  			        		,componentContext.getComponentInstance().getInstance(),
		  			                propsTable);
		  		        
		  		        }catch(Exception e){
		  		        	System.err.println("Error registering single service:" + e);
		  		        }
		  		        
		        	}else{
		        		/* check if it is a service factory */
		        		if(serviceInfo.isServiceFactory()){
		        			
		        			
			        	}
		        		
		        		/* check if it is a component factory */
		        		if(componentFactory!=null){
			        	
		        		}
			        
		        		
		        	}
		        }
		        
		        
		        /* add the Constant indicating the service_PID */
		        //propsTable.put(Constants.SERVICE_PID,serviceInterface);
		        /* add the Constant variable and the id to the Hashtable */
		      
		        
			}
        
		}
		

	}

	/**
	 * this method creates a ComponentContext containing
	 * a component instance.
	 * 
	 * @param componentDeclaration the ComponentDeclaration object
	 * @return ComponentContext Object if succeed
	 */
	private ComponentContext createComponentContext(
			ComponentDeclaration componentDeclaration) {
		try {
			
			/* create a dictionary */
			Dictionary props = new Hashtable();
			/* create a property */
			String componentName= componentDeclaration.getComponentName();
			/* put the property into the table */
			props.put("component.name",componentName);
			/* create a new context */
			ComponentContextImpl newContext = new ComponentContextImpl(
					createComponentInstance(componentDeclaration),
					bundleContext, props);

			return newContext;

		} catch (Exception e) {
			System.err.println("Error in createComponentContext():" + e);

		}

		return null;
	}
 
	/**
	 * this function will use a ComponentDeclaration to create a new ComponentInstance object
	 * 
	 * @param componentDeclaration the component declaration
	 * @return ComponentInstance object
	 */
	private ComponentInstance createComponentInstance(
			ComponentDeclaration componentDeclaration) {
			
			/* get assign null to the component name */
			String componentName = null;
			/* get the name of the components implementation */
			componentName = componentDeclaration.getImplementation();
			/* create a new factory object */
			ComponentCreator creator = new ComponentCreator(componentName);
			/* create a new instance */
		    Object element = creator.newInstance();
			
		    /* return the component instance */
		    return new ComponentInstanceImpl(element);
	}

	/**
	 * this method will create a new component factory and return the instance of it
	 * 
	 * @param componentDeclaration the component declaration
	 * @return the componentFactory object if succeed null else
	 * 
	 * @author Magnus Klack
	 */
	private ComponentFactory createComponentFactory(
			ComponentDeclaration componentDeclaration) {
		return null;
	}

	/**
	 * this method will track the dependencies and give them to the declarative
	 * service which this class controlls. Dependencies are kept in the class
	 * ComponentReferenceInfo.
	 *  
	 * @param componentDeclaration the component declaration
	 * @param componentInstance the componentInstance
	 * 
	 * @author Magnus Klack
	 */
	private void bindReferences(ComponentDeclaration componentDeclaration,
			ComponentInstance componentInstance) {

		System.out
				.println("**************** Tracking references for activation *********************");
		/* create an iterator */
		Iterator it = null;
		/* get the reference info array */
		ArrayList referenceInfo = componentDeclaration.getReferenceInfo();
		/* create the iterator */
		it = referenceInfo.iterator();

		/* iterate while still has next */
		while (it.hasNext()) {

			/* create the a temporary object */
			ComponentReferenceInfo componentRef = (ComponentReferenceInfo) it
					.next();

			if (componentRef.getInterfaceType() != null) {
				/* get the interface */
				String interfaceName = componentRef.getInterfaceType();
				/* print the service interface name */
				System.out.println("service interface name is:" + interfaceName);
				/* get the reference */
				String targetFilter = componentRef.getTarget();
				/* assign a component class object */
				Object componentObject = componentInstance.getInstance();

				/* create a string representing the method which should be
				 * called, i.e, the bind method
				 */
				String methodName = componentRef.getBind();
				/* print the bind method */
				System.out.println("The bind-method is:" + methodName);
						
						/* get the service reference */
						ServiceReference serviceReference = bundleContext.getServiceReference(interfaceName);
						/* get the service */
						Object reference = bundleContext.getService(serviceReference);
												
						try{
							/* print that we try to invoke the method */
							System.out.println("Trying to invoke " + methodName + " in class:" + componentDeclaration.getImplementation());
							/* get the method */
							Method method =componentObject.getClass().getDeclaredMethod(methodName, 
									new Class[]{Class.forName(interfaceName)});
							
							/* set this as accessible */
							method.setAccessible(true);
							/* invoke the method */
							method.invoke(componentObject,new Object[]{reference});
							
						}catch(NoSuchMethodException e){
							System.err.println("error in bindReferences():"  +e);
						}catch(IllegalAccessException e){
							System.err.println("error in bindReferences():" +e);
						}catch(InvocationTargetException e){
							System.err.println("error in bindReferences():" +e);
						}catch(ClassNotFoundException e){
							System.err.println("error in bindReferences():" +e);
						}

			

			} else {
				System.err.println("FATAL ERROR IN bindReferences(): dependency interface not declared");

			}//if (componentRef.getInterfaceType() != null)

		}//end while

	}
	
	/**
	 * This class will create new instances of a given class
	 * it will use the TracingIH class to trace and return an instance 
	 * of the class.
	 * 
	 * @author Magnus Klack
	 *
	 */
	private class ComponentCreator{
		/* a Class representing the component implementation */
		private Class componentClass;
		
		/**
		 * Constructor creates a class 
		 * @param className the class name
		 */
		public ComponentCreator(String className){
			try{
				/* create the class object */
				componentClass=Class.forName(className);
			}catch(ClassNotFoundException e){
				/* print the error */
				System.err.println("Error in ComponentCreator:"+e);
			
			}
			
		}
		
		/**
		 * Creates a new instance of the object 
		 * uses Proxy technique to trace object
		 * @return
		 */
		public Object newInstance() {
			try {
				
				/* creates the class instance */
				Object returnObject = componentClass
						.newInstance();
				
				/* create the return object with tracing feature will give the object as proxy 
				 * DO NOT USE THIS FEATURE! IF used then it is easier to invoke methods via 
				 * the proxy instance
				 **/
				//returnObject=TracingIH.createProxy(
				//		returnObject, new PrintWriter(System.out));
				
				System.out.println("Creating instance:" + returnObject);
				/* return the object */
				return returnObject;
				
			} catch (IllegalAccessException e) {
				System.err
						.println("Error in ComponentCreator  Can't access constructor of:"
								+ componentClass + "\n" + e);
			} catch (InstantiationException e) {
				System.err
						.println("Error in ComponentCreator  Can't create new instance of:"
								+ componentClass + " \n" + e);
			} catch (Exception e) {
				System.err.println("Error in ComponentCreator:");
				e.printStackTrace();
			}
			
			return null;
		}
		
	
	}//end ComponentCreator
	
	
	//private class
	
}


