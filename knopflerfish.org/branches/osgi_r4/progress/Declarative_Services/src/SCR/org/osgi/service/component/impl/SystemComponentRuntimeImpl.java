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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;

/**
 * This class is the implementation of the declarative service feature. It will
 * locate and bind diffrent types of declared components on demand. It will also
 * listen to BundleEvents rasied within the framework and act after that. 
 * It is right now very hard to get information from the declaration class
 * and that demands, in many cases, nestle loops, for or while statements.
 * The class uses a ComponentDeclarations, BundleContexts, CustomParsers to
 * fulfill its responsibility. It keeps active components in a vector and
 * inactive components in another vector. 
 * 
 * @author Magnus Klack
 */

public class SystemComponentRuntimeImpl implements BundleListener,
		ServiceListener {
	/** variable holding the bundlecontext */
	private BundleContext bundleContext;

	/** variable holding the custom parser object */
	private CustomParser customParser;

	/** variable counting components */
	private long componentCounter = 0;

	/** variable holding active components */
	private Vector activeComponents = new Vector();

	/** variable holding inactive components */
	private Vector inactiveComponents = new Vector();
	
	/**
	 * Constructor for the SCR assigns local variable and 
	 * if there are already active declarative bundles within 
	 * the framework an evaluation of them.
	 *  
	 * @param context the bundle context 
	 * @param alreadyActive an array with already active components
	 */
	public SystemComponentRuntimeImpl(BundleContext context,
			Vector alreadyActive) {
		/* assign the bundlecontext */
		bundleContext = context;
		/* add this as a bundle listener */
		bundleContext.addBundleListener(this);
		/* add the serviceListener */
		bundleContext.addServiceListener(this);

		for (int i = 0; i < alreadyActive.size(); i++) {
			ComponentDeclaration declaration = (ComponentDeclaration) alreadyActive
					.get(i);
			evaluateComponentDeclaration(declaration, false);
		}

	}
	
	/**
	 * this method will be called when SCR is shutting down
	 * it will dispose all active components handled by the
	 * SCR and return to the caller method
	 */
	public void shutdown(){
		
		/* go through all active components */
		for(int i=0;i<activeComponents.size();i++){
			/* get the component */			
			DeclarativeComponent component = (DeclarativeComponent)
												activeComponents.get(i);
			/* disable the component */
			disableComponent(component.getComponentDeclaration().getComponentName(),
					component.getComponentDeclaration().getDeclaringBundle(),
					true);
			
			/* decrease one element is removed */
			i--;
		}
	
	}
	
	/**
	 * this method listens for service changes it will check the type of the
	 * event and perform correct actions depending on the event. More explanations
	 * are attatched within the method. 
	 * 
	 * @param event
	 */
	public synchronized void serviceChanged(ServiceEvent event) {
		/* check if a new service is registered */
		if (event.getType() == ServiceEvent.REGISTERED
				|| event.getType() == ServiceEvent.UNREGISTERING) {
			try {
				/* get all the object classes */
				String[] objectClasses = (String[]) event.getServiceReference()
						.getProperty("objectClass");

				/* go through them */
				for (int i = 0; i < objectClasses.length; i++) {
					/* create a string representing the newly registered service */
					String objectClass = objectClasses[i];

					/*
					 * check if the event is a REGISTERED event check if any
					 * inactive component has this registered event as reference
					 * if so try to evalutate the that component with the
					 * evaluateComponentDeclaration(..) method. if the component
					 * is satisfied,i.e has all dependencies it needs when it
					 * will be activated and its services, if any , will be
					 * registered into the framework.
					 */
					if (event.getType() == ServiceEvent.REGISTERED) {
						/*
						 * go through all inactive components to se if any of
						 * them has a reference to this service
						 */
						for (int j = 0; j < inactiveComponents.size(); j++) {
							/* get the componentDeclaration */
							ComponentDeclaration componentDeclaration = (ComponentDeclaration) inactiveComponents
									.get(j);

							/* get the components references */
							ArrayList references = componentDeclaration
									.getReferenceInfo();
							/* create an iterator */
							Iterator referenceIterator = references.iterator();

							/* iteratre through the reference info objects */
							while (referenceIterator.hasNext()) {
								/* get the reference info object */
								ComponentReferenceInfo referenceInfo = (ComponentReferenceInfo) referenceIterator
										.next();

								/* get the filter */
								String filter = referenceInfo.getTarget();
								/* get the target */
								String eventTarget = (String) event
										.getServiceReference().getProperty(
												"component.name");

								/*
								 * check if the names of the declared interface
								 * and the object class are equal to each other
								 * and that the component declaration is auto
								 * enabled. Also make sure that the targets of
								 * the declaration equals the component name
								 */
								if (referenceInfo.getInterfaceType().equals(
										objectClass)
										&& componentDeclaration.isAutoEnable()) {
									try {
										/*
										 * try to start the component again this
										 * may failed if the component has other
										 * unsatisfied references which aren't
										 * obvious here, but the
										 * evaluateComponentDeclaration(...)
										 * method will take care of it.
										 * therefore no concerns about the
										 * filter is taken here.
										 */
										evaluateComponentDeclaration(
												componentDeclaration, false);
										
										inactiveComponents.remove(componentDeclaration);
										j--;
									} catch (ComponentException e) {
										/* print the error */
										System.out.println(e);
									}

								}// end
								// if(referenceInfo.getInterfaceType().equals(objectClass)
								// &&
								//componentDeclaration.isAutoEnable())

							}// end while (referenceIterator.hasNext())

						}// end for(int j=0;j<inactiveComponents.size();j++)

					}//end if(event.getType()==ServiceEvent.REGISTERED)

					/*
					 * always check all active components no matter if it is a
					 * REGISTERED or UNREGISTERED event if an active component
					 * has a refence to the event perform correct action i.e,
					 * disable the component if its necessary, rebind the
					 * component and call the components activate method.
					 *  
					 */
					for (int j = 0; j < activeComponents.size(); j++) {

						/* check if it is an DeclarativeComponent */
						if (activeComponents.get(j) instanceof DeclarativeComponent) {
							/* create an ImmediateComponent variable */
							DeclarativeComponent component = (DeclarativeComponent) activeComponents
									.get(j);
							/* create a componentDeclaration variable */
							ComponentDeclaration componentDeclaration = component
									.getComponentDeclaration();

							/* get the components references */
							ArrayList references = componentDeclaration
									.getReferenceInfo();
							/* create an iterator */
							Iterator referenceIterator = references.iterator();

							/* iteratre through the reference info objects */
							while (referenceIterator.hasNext()) {
								/* get the reference info object */
								ComponentReferenceInfo referenceInfo = (ComponentReferenceInfo) referenceIterator
										.next();

								/*
								 * check if the name of the interface is the
								 * same as the newly registered. Also check that
								 * the cardinality is 0..n or 1..n. Also ensure
								 * that a bind method is declared If that is the
								 * case the active component should be binded or
								 * rebinded again. This should only be done if
								 * the event is a REGISTERED event
								 */
								if (referenceInfo.getInterfaceType().equals(
										objectClass)
										&& (referenceInfo.getCardinality()
												.equals("0..n") || referenceInfo
												.getCardinality()
												.equals("1..n"))
										&& (event.getType() == ServiceEvent.REGISTERED)) {

									try {
										/*
										 * make sure the context has been
										 * created before trying to bind this
										 * one.
										 */
										if (component.getComponentContext() != null
												|| component
														.getComponentContexts() != null) {

											if (!component
													.getComponentDeclaration()
													.isServiceFactory()) {
												System.out
														.println("\n"
																+ component
																		.getComponentDeclaration()
																		.getComponentName()
																+ "\nhas declared that its instance wants to be notified about all\n"
																+ "registered service of this type therefore an instance of\n"
																+ "this service will be passed to this components bind method\n");

												/* get the service object */
												Object serviceObject = bundleContext
														.getService(event
																.getServiceReference());
												/*
												 * get the instance to be
												 * invoked
												 */
												Object instance = component
														.getComponentContext()
														.getComponentInstance()
														.getInstance();
												/* get the method name */
												String methodName = referenceInfo
														.getBind();
												/* get the interface type */
												String interfaceName = referenceInfo
														.getInterfaceType();
												/* invoke the bind method */
												reInvokeReference(
														serviceObject,
														instance, methodName,
														interfaceName);
												/* invoke the activate method */
												activateInstance(component
														.getComponentContext());
												/*
												 * add the service reference to
												 * the component
												 */
												component.bindReference(event
														.getServiceReference());
											}

											if (component
													.getComponentDeclaration()
													.isServiceFactory()) {

												System.out
														.println("\n"
																+ component
																		.getComponentDeclaration()
																		.getComponentName()
																+ "\nhas declared that its instances wants to be notified about all\n"
																+ "registered service of this type therefore an instance of\n"
																+ "this service will be passed to this components bind method\n");

												Vector contexts = component
														.getComponentContexts();
												/* get the service object */
												Object serviceObject = bundleContext
														.getService(event
																.getServiceReference());

												for (int z = 0; z < contexts
														.size(); z++) {
													/* get the current context */
													ComponentContext currentContext = (ComponentContext) contexts
															.get(z);
													/*
													 * get the instance to be
													 * invoked
													 */
													Object instance = currentContext
															.getComponentInstance()
															.getInstance();
													/* get the method name */
													String methodName = referenceInfo
															.getBind();
													/* get the interface type */
													String interfaceName = referenceInfo
															.getInterfaceType();
													/* invoke the bind method */
													reInvokeReference(
															serviceObject,
															instance,
															methodName,
															interfaceName);
													/*
													 * invoke the activate
													 * method
													 */
													activateInstance(currentContext);
													/*
													 * add the service reference
													 * to the component
													 */
													component
															.bindReference(event
																	.getServiceReference());

												}

											}

										}

									} catch (ComponentException e) {
										System.err
												.println("error in serviceChanged(..) when reInvoking component:\n"
														+ e);
									}

								}

								/*
								 * variable represents if the component is
								 * bounded or not
								 */
								boolean isBounded = false;
								/* make sure the component is not null */
								if (component != null) {
									/* check if the component is bounded */
									isBounded = component.isBoundedTo(event
											.getServiceReference());
								}

								/*
								 * check if the objectclass matches the
								 * interface in declaration also check if its a
								 * UNREGISTERING event if so check if any
								 * component has the unregistered reference as
								 * dependency. if a dependency exists perform
								 * the legal action depending on the policy and
								 * the cardinality
								 */
								if ((event.getType() == ServiceEvent.UNREGISTERING)
										&& isBounded
										&& referenceInfo.getInterfaceType()
												.equals(objectClass)) {

									/*
									 * create a string representing the
									 * cardinality
									 */
									String cardinality = referenceInfo
											.getCardinality();
									/* get the policy */
									String policy = referenceInfo.getPolicy();

									/*
									 * check if it is a static policy. Static
									 * policy will not try to dynamically rebind
									 * the component SCR will instead disable
									 * the component and try to create a new
									 * one.
									 */
									if (policy.equals("static")) {

										/*
										 * make sure this component doesn't
										 * belong to a stopping bundle if that
										 * is the case this reference is in the
										 * same bundle as the stopping bundle.
										 * Don't disable the component it will
										 * be disabled later.
										 */
										if (componentDeclaration
												.getDeclaringBundle()
												.getState() != Bundle.STOPPING) {
											/* disable the component */
											System.out
													.println("disabel "
															+ componentDeclaration
																	.getComponentName()
															+ " because it has a reference to "
															+ objectClass
															+ " with a static policy");

											/* disable the component */
											disableComponent(
													componentDeclaration
															.getComponentName(),
													componentDeclaration
															.getDeclaringBundle(),
													false);
											/* decrease one element is removed */
											j--;

										} else {

											System.out
													.println("****************  "
															+ componentDeclaration
																	.getComponentName()
															+ "'s bundle is stopping "
															+ "this is probably a bundle internal reference *********");
										}

										/*
										 * evaluate the component again if the
										 * component is satisfied it will
										 * restart else it will be saved for
										 * later use TODO check if the
										 * evaluation method should care about
										 * the isAutoEnable attribute
										 */
										try {

											if (componentDeclaration
													.getDeclaringBundle()
													.getState() != Bundle.STOPPING) {
												evaluateComponentDeclaration(
														componentDeclaration,
														false);
											} else {
												System.err
														.println("************** This component's bundle is stopping"
																+ " ignore to restart **********************");
											}

										} catch (Exception e) {
											System.err
													.println("error occured when trying to restart"
															+ componentDeclaration
																	.getComponentName()
															+ " due to:\n"
															+ e.getMessage());
											/* remove the component */
											activeComponents.remove(component);
											/* decrease one element is removed */
											j--;
										}

									}

									/*
									 * dynamic policy is a little bit more
									 * complex it will behave diffrent depending
									 * on the cardinality declared. if the
									 * cardinality is 1..1 and no references are
									 * available then the component declaring
									 * dependency to this unregistered service
									 * cannot work with out a rebind action. If
									 * the cardinality is 0..1 it means it will
									 * be able to work with out this reference
									 * but the standard procedure is to look for
									 * another equal reference and rebind the
									 * component.
									 */
									if (policy.equals("dynamic")
											&& component
													.getComponentDeclaration()
													.getDeclaringBundle()
													.getState() != Bundle.STOPPING) {

										System.out
												.println("*************** Found dynamic reference to:"
														+ component
																.getComponentDeclaration()
																.getComponentName()
														+ " ********************");

										/*
										 * if the cardinality is 1..1 or 0..1
										 * when unbind the old service a try to
										 * locate a new service and bind it to
										 * the service
										 */
										if (cardinality.equals("1..1")
												|| cardinality.equals("0..1")) {

											/*
											 * check if a new service of this
											 * type is available
											 */
											ServiceReference[] serviceReferences = bundleContext
													.getServiceReferences(
															referenceInfo
																	.getInterfaceType(),
															referenceInfo
																	.getTarget());

											/*
											 * declare a new service object set
											 * it to null
											 */
											Object serviceObject = null;

											/* get the old service object */
											Object oldServiceObject = bundleContext
													.getService(event
															.getServiceReference());

											/*
											 * check that the reference is not
											 * null
											 */
											if (serviceReferences != null) {
												/* get the service */
												serviceObject = bundleContext
														.getService(serviceReferences[0]);
											}

											/*
											 * this is the easy task because
											 * those component types do only
											 * have one instance of the
											 * component object
											 */
											if (component instanceof ImmediateComponent
													|| component instanceof CustomDelayedService
													|| component instanceof CustomComponentFactory
													&& (serviceObject != null)) {

												/* get the context */
												ComponentContext componentContext = component
														.getComponentContext();

												/*
												 * ensure that the context isn't
												 * null if so it means that the
												 * service never been requested
												 * and the component is of the
												 * type CustomDelayedService. If
												 * the context is null then
												 * there are no instance(s) to
												 * rebind.
												 */

												if (componentContext != null) {
													/*
													 * create a string
													 * representing the unbind
													 * method
													 */
													String unbindMethod = referenceInfo
															.getUnbind();
													/*
													 * create a string
													 * representing the bind
													 * method
													 */
													String bindMethod = referenceInfo
															.getBind();

													/*
													 * make sure unbind method
													 * is declared
													 */
													if (unbindMethod != null) {
														try {
															/*
															 * unbind the old
															 * service
															 */
															reInvokeReference(
																	oldServiceObject,
																	componentContext
																			.getComponentInstance()
																			.getInstance(),
																	unbindMethod,
																	referenceInfo
																			.getInterfaceType());
														} catch (ComponentException e) {
															System.err
																	.println(e);
														}
													}

													if (bindMethod != null) {
														try {
															/*
															 * bind the old
															 * service
															 */
															reInvokeReference(
																	serviceObject,
																	componentContext
																			.getComponentInstance()
																			.getInstance(),
																	bindMethod,
																	referenceInfo
																			.getInterfaceType());
														} catch (ComponentException e) {
															/* print the error */
															System.err
																	.println(e);
														}
													}

													try {
														/*
														 * invoke the activate
														 * method
														 */
														activateInstance(componentContext);
													} catch (ComponentException e) {
														/* print the error */
														System.err
																.println("error calling activate method due to:\n"
																		+ e);
													}

												}// end
												// if(componentContext!=null){

											} else if (component instanceof CustomComponentServiceFactory
													&& serviceObject != null) {
												/*
												 * this is a service factory get
												 * all contexts
												 */
												Vector contexts = component
														.getComponentContexts();
												/*
												 * create a string representing
												 * the unbind method
												 */
												String unbindMethod = referenceInfo
														.getUnbind();
												/*
												 * create a string representing
												 * the bind method
												 */
												String bindMethod = referenceInfo
														.getBind();

												for (int x = 0; x < contexts
														.size(); x++) {
													/* get the component context */
													ComponentContext componentContext = (ComponentContext) contexts
															.get(x);

													/*
													 * make sure unbind method
													 * is declared
													 */
													if (unbindMethod != null) {
														try {
															/*
															 * unbind the old
															 * service
															 */
															reInvokeReference(
																	oldServiceObject,
																	componentContext
																			.getComponentInstance()
																			.getInstance(),
																	unbindMethod,
																	referenceInfo
																			.getInterfaceType());
														} catch (ComponentException e) {
															System.err
																	.println(e);
														}
													}

													if (bindMethod != null) {
														try {
															/*
															 * bind the new
															 * service
															 */
															reInvokeReference(
																	serviceObject,
																	componentContext
																			.getComponentInstance()
																			.getInstance(),
																	bindMethod,
																	referenceInfo
																			.getInterfaceType());
														} catch (ComponentException e) {
															System.err
																	.println(e);
														}
													}

													try {
														/*
														 * invoke the activate
														 * method
														 */
														activateInstance(componentContext);
													} catch (ComponentException e) {
														/* print the error */
														System.err
																.println("error calling activate method due to:\n"
																		+ e);
													}

												}// end for(int
												// x=0;x<contexts.size();x++)

											}// end else if(component instanceof
											// CustomComponentServiceFactory)

											if (cardinality.equals("1..1")
													&& serviceObject == null) {
												/*
												 * in this case disable the
												 * component it should not work
												 * with the declared cardinality
												 */
												try {
													System.out
															.println("************ disable "
																	+ componentDeclaration
																			.getComponentName()
																	+ " unable to bind new equal reference to component *************");
													/* call the disable method */
													disableComponent(
															componentDeclaration
																	.getComponentName(),
															componentDeclaration
																	.getDeclaringBundle(),
															false);
													/*
													 * decrease one element is
													 * removed
													 */
													j--;
												} catch (ComponentException e) {
													System.err.println(e);
												}

											}// end
											// if(cardinality.equals("1..1") &&
											// serviceObject==null)
										}

										/*
										 * if cardinality is 1..n means that the
										 * component wants all references of the
										 * given type and MUST have at least one
										 * reference bounded. 0..n means that
										 * the component can work with out a
										 * reference and should not be disabled
										 * if no references are available.
										 */
										if (cardinality.equals("1..n")
												|| cardinality.equals("0..n")) {
											/* get all the service references */
											ServiceReference[] newReferences = null;
											/* get the old service object */
											Object oldServiceObject = bundleContext
													.getService(event
															.getServiceReference());
											/*
											 * create a string representing the
											 * unbind method
											 */
											String unbindMethod = referenceInfo
													.getUnbind();
											/*
											 * create a string representing the
											 * bind method
											 */
											String bindMethod = referenceInfo
													.getBind();

											/*
											 * those component types only have
											 * one instance
											 */
											if (component instanceof ImmediateComponent
													|| component instanceof CustomDelayedService
													|| component instanceof CustomComponentFactory) {

												/*
												 * create a componentContext
												 * variable
												 */
												ComponentContext componentContext = component
														.getComponentContext();

												/*
												 * check if component has an
												 * unbind method
												 */
												if (unbindMethod != null) {
													try {
														/*
														 * unbind the old
														 * service
														 */
														reInvokeReference(
																oldServiceObject,
																componentContext
																		.getComponentInstance()
																		.getInstance(),
																unbindMethod,
																referenceInfo
																		.getInterfaceType());
														component
																.unBindReference(event
																		.getServiceReference());
													} catch (ComponentException e) {
														System.err.println(e);
													}
												}

												try {
													/* get the references */
													newReferences = bundleContext
															.getServiceReferences(
																	referenceInfo
																			.getInterfaceType(),
																	referenceInfo
																			.getTarget());
												} catch (InvalidSyntaxException e) {
													System.err
															.println("error getting servicereferences due to:\n"
																	+ e);
												} catch (Exception e) {
													System.err
															.println("error getting servicereferences due to:\n"
																	+ e);
												}

												/*
												 * check this again because the
												 * old references can be the
												 * left
												 */
												if (newReferences != null) {

													for (int x = 0; x < newReferences.length; x++) {
														/*
														 * make sure unbind
														 * method is declared
														 */
														if (bindMethod != null) {
															try {
																/*
																 * check if
																 * already
																 * bounded
																 */
																if (!component
																		.isBoundedTo(newReferences[x])) {
																	/*
																	 * get the
																	 * object
																	 */
																	Object serviceObject = bundleContext
																			.getService(newReferences[x]);
																	/*
																	 * bind the
																	 * old
																	 * service
																	 */
																	reInvokeReference(
																			serviceObject,
																			componentContext
																					.getComponentInstance()
																					.getInstance(),
																			bindMethod,
																			referenceInfo
																					.getInterfaceType());
																}
															} catch (ComponentException e) {
																System.err
																		.println(e);
															}
														}

														try {
															/*
															 * invoke the
															 * activate method
															 */
															activateInstance(componentContext);
														} catch (ComponentException e) {
															System.err
																	.println("error calling activate method due to:\n"
																			+ e);
														}

													}// end for(int
													// x=0;x<newReferences.length;x++)

												}// end if(newReferences!=null)

											} else if (component instanceof CustomComponentServiceFactory) {
												/*
												 * this is even moore tricky
												 * here we need all the
												 * component instances
												 */
												Vector contexts = component
														.getComponentContexts();
												try {
													/* get the references */
													newReferences = bundleContext
															.getServiceReferences(
																	referenceInfo
																			.getInterfaceType(),
																	null);
												} catch (InvalidSyntaxException e) {
													System.err
															.println("error getting servicereferences due to:\n"
																	+ e);
												} catch (Exception e) {
													System.err
															.println("error getting servicereferences due to:\n"
																	+ e);
												}

												if (newReferences != null) {
													for (int m = 0; m < contexts
															.size(); m++) {
														/*
														 * get the current
														 * context
														 */
														ComponentContext componentContext = (ComponentContext) contexts
																.get(m);

														if (unbindMethod != null) {
															try {
																/*
																 * unbind the
																 * old service
																 */
																reInvokeReference(
																		oldServiceObject,
																		componentContext
																				.getComponentInstance()
																				.getInstance(),
																		unbindMethod,
																		referenceInfo
																				.getInterfaceType());
																component
																		.unBindReference(event
																				.getServiceReference());
															} catch (ComponentException e) {
																System.err
																		.println(e);
															}
														}

														for (int x = 0; x < newReferences.length; x++) {

															/*
															 * make sure unbind
															 * method is
															 * declared
															 */
															if (bindMethod != null) {
																try {
																	/*
																	 * make sure
																	 * the
																	 * component
																	 * isnt
																	 * bounded
																	 * to this
																	 * reference
																	 */
																	if (!component
																			.isBoundedTo(newReferences[x])) {
																		Object serviceObject = bundleContext
																				.getService(newReferences[x]);
																		/*
																		 * bind
																		 * the
																		 * old
																		 * service
																		 */
																		reInvokeReference(
																				serviceObject,
																				componentContext
																						.getComponentInstance()
																						.getInstance(),
																				bindMethod,
																				referenceInfo
																						.getInterfaceType());
																	}
																} catch (ComponentException e) {
																	System.err
																			.println(e);
																}
															}

															try {
																/*
																 * invoke the
																 * activate
																 * method
																 */
																activateInstance(componentContext);
															} catch (ComponentException e) {
																System.err
																		.println("error calling activate method due to:\n"
																				+ e);
															}

														}// end for(int
														// x=0;x<newReferences.length;x++)

													}//end for(int m= 0;
													// m<contexts.size();m++)

												}// end if(newReferences!=null)

											}// if(component instanceof
											// CustomComponentServiceFactory)

											/*
											 * check the cardinality if it is
											 * 1..n it should be disabled here
											 * it can't work if no service
											 * references are available
											 */
											if (component
													.getAllBoundedReferences()
													.size() == 0
													&& cardinality
															.equals("1..n")) {
												System.out
														.println("************ disable "
																+ componentDeclaration
																		.getComponentName()
																+ " unable to bind new equal references to component *************");
												disableComponent(
														componentDeclaration
																.getComponentName(),
														componentDeclaration
																.getDeclaringBundle(),
														false);
												/*
												 * decrease one element is
												 * removed
												 */
												j--;

											}// end if(newReferences==null &&
											// cardinality.equals("1..n"))
										}

									} else if (policy.equals("dynamic")
											&& component
													.getComponentDeclaration()
													.getDeclaringBundle()
													.getState() != Bundle.STOPPING) {
										System.err
												.println("************************* "
														+ component
																.getComponentDeclaration()
														+ "'s bundle is stopping this is probably an internal reference"
														+ " **********************");

									}

								}//end
								// if(referenceInfo.getInterfaceType().equals(objectClass)
								// &&
								//event.getType()==ServiceEvent.UNREGISTERING)

							}// end while (referenceIterator.hasNext())

						} else {

							System.err
									.println("error in SystemComponentRuntimeImpl.serviceChanged(..) due to:\n"
											+ "occurance of none DeclarativeComponent type should not be in the vector"
											+ " for active components");
						}

					}// end for(int j=0

				}// end for(int i=0;i<objectClasses.length;i++)

				/* if it is an unregister event */
				if (event.getType() == ServiceEvent.UNREGISTERING) {
					/*
					 * check all component instances wich haven't been requested
					 * yet this is problematic because an instance isn't done by
					 * them yet.
					 */

					for (int x = 0; x < activeComponents.size(); x++) {
						DeclarativeComponent component = (DeclarativeComponent) activeComponents
								.get(x);

						if (!isSatisfied(component.getComponentDeclaration())
								&& component.getComponentDeclaration()
										.getDeclaringBundle().getState() != Bundle.STOPPING) {
							System.out
									.println("******** found an unrequested service component wich is unsatisfied:"
											+ component
													.getComponentDeclaration()
													.getComponentName()
											+ " ****************");
							disableComponent(component
									.getComponentDeclaration()
									.getComponentName(), component
									.getComponentDeclaration()
									.getDeclaringBundle(), false);
						}

					}

				}

			} catch (Exception e) {
				e.printStackTrace();
				System.err
						.println("error in SystemComponentRuntimeImpl.serviceChanged(..) due to:\n"
								+ e);

			} // end try

		}// if(event.getType()==ServiceEvent.REGISTERED ||
		// event.getType()==ServiceEvent.UNREGISTERING)

	}

	/**
	 * Listens for BundleEvents from the framework and creates a
	 * ComponentDeclaration this is usually the main entrance for a bundle
	 * having declarative components. Further the method will parse the declared
	 * xml files and create ComponentDeclarations of the files. If the event is
	 * a stop event then the method will check if the bundle has declarative
	 * components and if they are activated. If that is the case the
	 * configurations will be disabled and unregistered from the framework.
	 * 
	 * @param event the event from the framework
	 */
	public synchronized void bundleChanged(BundleEvent event) {

		if (event.getBundle().getHeaders().get("Service-Component") != null
				&& event.getType() == BundleEvent.STARTED) {

			try {

				/* try to get the XML file */
				String manifestEntry = (String) event.getBundle().getHeaders()
						.get("Service-Component");

				System.out.println("The Declared components are:"
						+ (String) event.getBundle().getHeaders().get(
								"Service-Component"));

				/* split the string */
				String[] manifestEntries = manifestEntry.split(",");
				/* get the bundle location */
				String bundleLocation = event.getBundle().getLocation();

				/* format the string, i.e, remove the 'file:' entry from it */
				String formattedLocation = bundleLocation.substring(5,
						bundleLocation.length());

				/* get the jar file use the formatted location */
				JarFile jarFile = new JarFile(formattedLocation);

				/* go through all declared xml files */
				for (int i = 0; i < manifestEntries.length; i++) {

					/* get the xmlfile located by the manifestEntry */
					ZipEntry zipEntry = jarFile.getEntry(manifestEntries[i]);
					if (zipEntry != null) {
						/* create the parser */
						customParser = new CustomParser();

						System.out.println("\n\n******************* Parsing "
								+ manifestEntries[i]
								+ " *****************************");

						/*
						 * parse the document and retrieve a component
						 * declaration
						 */
						ComponentDeclaration componentDeclaration = customParser
								.readXML(zipEntry, jarFile);

						/* add the declaring bundle to the declaration */
						componentDeclaration.setDeclaraingBundle(event
								.getBundle());

						/* add the path to the xmlfile to declaration */
						componentDeclaration.setXmlFile(manifestEntries[i]);

						try {
							System.out.println("***************** Evaluate "
									+ componentDeclaration.getComponentName()
									+ " ******************* ");

							/*
							 * evaluate the component don't overide the
							 * isAutoEnable() attribute
							 */
							evaluateComponentDeclaration(componentDeclaration,
									false);

						} catch (ComponentException e) {
							/* print the error */
							System.err
									.println("error when evaluating started bundle with component\n"
											+ "The following Exception(s) was thrown:\n"
											+ e);
						}
					} else {
						System.err.println("error can't find:"
								+ manifestEntries[i]);

					}
				}

			} catch (Exception e) {
				System.err.println("error in bundleChanged(..)" + e);

			}

		}// end if (event.getBundle().getHeaders().get("Service-Component")
		// && event.getType() == BundleEvent.STARTED)

		/* check if it is a stop event and if it is a declarative component */
		if (event.getBundle().getHeaders().get("Service-Component") != null
				&& event.getType() == BundleEvent.STOPPED) {

			try {
				synchronized (activeComponents) {

					/* go through all started and satisfied components */
					for (int i = 0; i < activeComponents.size(); i++) {

						try {
							/* create a variable of type DeclarativeComponent */
							DeclarativeComponent component = (DeclarativeComponent) activeComponents
									.get(i);

							/* get the component declaration */
							ComponentDeclaration componentDeclaration = component
									.getComponentDeclaration();

							/*
							 * check if the event bundle matches the declaration
							 * bundle
							 */
							if (componentDeclaration.getDeclaringBundle()
									.equals(event.getBundle())) {

								System.out.println("Disable "
										+ componentDeclaration
												.getComponentName()
										+ " due to BUNDLE STOPPED");
								/* if so disable the component */
								disableComponent(componentDeclaration
										.getComponentName(), event.getBundle(),
										true);

								/*
								 * decrease i because one element has been
								 * removed
								 */
								i--;
								//component=null;

							}
						} catch (IndexOutOfBoundsException e) {
							System.err
									.println("THIS SHOULD NOT HAPPEN WHEN GOING THROUGH ACTIVE");
							/*
							 * the list can become empty before the iteration
							 * therefore catch the exception but do nothing
							 */
						}

					}

				}

				/*
				 * go through all inactive components and if a match occures
				 * remove the component declaration
				 */

				synchronized (inactiveComponents) {
					/* go through all inactive components */
					for (int i = 0; i < inactiveComponents.size(); i++) {
						try {
							/* get the declaration */
							ComponentDeclaration componentDeclaration = (ComponentDeclaration) inactiveComponents
									.get(i);

							/* if there is a match */
							if (componentDeclaration.getDeclaringBundle()
									.equals(event.getBundle())) {
								/* remove the declaration */
								inactiveComponents.remove(i);
								/* decrease one element is removed */
								i--;
							}
						} catch (IndexOutOfBoundsException e) {
							/*
							 * the list can become empty before the iteration
							 * starts therefore catch the exception but do
							 * nothing
							 */
							System.err
									.println("THIS SHOULD NOT HAPPEN WHEN GOING THROUGH INACTIVE");
						}

					}

					System.out
							.println("\n the size of active components array:"
									+ activeComponents.size());
					if (activeComponents.size() > 0) {
						synchronized (activeComponents) {
							for (int i = 0; i < activeComponents.size(); i++) {
								System.out
										.println("Element:"
												+ ((DeclarativeComponent) activeComponents
														.get(i))
														.getComponentDeclaration()
														.getComponentName());
							}
						}
					}

					System.out
							.println("\n the size of inactive components array:"
									+ inactiveComponents.size());

					if (inactiveComponents.size() > 0) {
						synchronized (inactiveComponents) {
							for (int i = 0; i < inactiveComponents.size(); i++) {
								System.out
										.println("Element:"
												+ ((ComponentDeclaration) inactiveComponents
														.get(i))
														.getComponentName());
							}
						}
					}
					//System.out.println("ELEMENT:" +
					// ((ComponentDeclaration)inactiveComponents.get(0)).getComponentName()
					// );
				}
			} catch (Exception e) {
				System.err.println("Error when stopping bundle"
						+ event.getBundle() + " due to:\n" + e);
			}

		}// end if (event.getBundle().getHeaders().get("Service-Component") !=
		// null
		//&& event.getType() == BundleEvent.STOPPED)

	}

	/**
	 * this method will dispose a given component configuration it will search
	 * through all active components and if a match on handed object is found
	 * the method will unbind, deactivate and unregister the services if there
	 * are any.
	 * 
	 * @param componentObject
	 *            the component object to be disposed
	 * @throws ComponentException
	 *             if failed during the process
	 */
	public void disposeComponent(Object componentObject)
			throws ComponentException {
		
		/* go through all active components */
		for (int i = 0; i < activeComponents.size(); i++) {
			/* get the component */
			DeclarativeComponent component = (DeclarativeComponent) activeComponents
					.get(i);

			/*
			 * check if it is an ImmediateComponent or CustomDelayedService
			 * component. Those are quite simular to each other they both have
			 * ONE component instance and ONE componentContext. The only
			 * diffrence is that an CustomDelayedService offers Services to
			 * other.
			 */
			if (component instanceof ImmediateComponent
					|| component instanceof CustomDelayedService) {
				/* get the component element located in the componentContext */
				Object element = component.getComponentContext()
						.getComponentInstance().getInstance();

				/*
				 * check if the component is equal to the passed componentObject
				 * variable
				 */
				if (element.equals(componentObject)) {
					/* we found the component lets dispose it */
					try {
						/* unbind its references */
						invokeReferences(component, component
								.getComponentContext().getComponentInstance(),
								false);
						try {
							inactivateInstance(component.getComponentContext());
							try {

								/* remove the element from the active array */
								activeComponents.remove(component);

								if (component instanceof CustomDelayedService) {
									/* unregister the component */
									component.getServiceRegistration()
											.unregister();
								}

								/* make sure it does not exists already */
								if (!inactiveComponents.contains(component
										.getComponentDeclaration())) {
									inactiveComponents.add(component
											.getComponentDeclaration());
								}

								/* set the element to null */
								element = null;
								/* set the component to null */
								component = null;
								/* sweep the garbage */
								System.gc();

							} catch (Exception e) {
								throw new ComponentException(
										"error unregistering "
												+ component
														.getComponentDeclaration()
														.getComponentName()
												+ " due to:\n" + e, e
												.getCause());
							}
						} catch (ComponentException e) {
							throw new ComponentException(
									"error when deactivating  "
											+ component
													.getComponentDeclaration()
													.getComponentName()
											+ " due to:\n" + e, e.getCause());
						}

					} catch (ComponentException e) {
						throw e;
					}

				}// end if(element.equals(componentObject))

			}// end if(component instanceof ImmediateComponent)

			/*
			 * this case is moore tricky. The service factory do have many
			 * instances and contexts. Those are created when a request for the
			 * service is done. Therefore all contexts and instances have to be
			 * eliminated here.
			 */
			if (component instanceof CustomComponentServiceFactory) {
				/* get the componentContexts */
				Vector contexts = component.getComponentContexts();

				/* go through all the components created contexts */
				for (int j = 0; j < contexts.size(); j++) {
					/* get the component element located in the componentContext */
					Object element = ((ComponentContext) contexts.get(j))
							.getComponentInstance().getInstance();

					/*
					 * check if the component is equal to the passed
					 * componentObject variable
					 */
					if (element.equals(componentObject)) {
						/* we found the component lets dispose it */
						try {
							/* unbind its references */
							invokeReferences(component,
									((ComponentContext) contexts.get(j))
											.getComponentInstance(), false);
							try {
								inactivateInstance((ComponentContext) contexts
										.get(j));
								try {

									/* remove the element from the active array */
									activeComponents.remove(component);
									/* unregister the component */
									component.getServiceRegistration()
											.unregister();

									/* make sure it does not exists already */
									if (!inactiveComponents.contains(component
											.getComponentDeclaration())) {
										inactiveComponents.add(component
												.getComponentDeclaration());
									}

									/* set the element to null */
									element = null;
									/* set the component to null */
									component = null;
									/* sweep the garbage */
									System.gc();

								} catch (Exception e) {
									throw new ComponentException(
											"error unregistering "
													+ component
															.getComponentDeclaration()
															.getComponentName()
													+ " due to:\n" + e, e
													.getCause());
								}
							} catch (ComponentException e) {
								throw new ComponentException(
										"error when deactivating  "
												+ component
														.getComponentDeclaration()
														.getComponentName()
												+ " due to:\n" + e, e
												.getCause());
							}

						} catch (ComponentException e) {
							throw e;
						}

					}// end if(element.equals(componentObject))

				}//for(int j=0;j<contexts.size();j++)

			}// if(component instanceof CustomDelayedService)

		}// end for(int i=0;i<activeComponents.size();i++)

	}

	/**
	 * this method will disable a component or components after a given name. It
	 * will ensure that the component is in the same bundle as the bundle
	 * calling this method. The method cannot just only check if its a
	 * DeclarativeComponent type. It has to check the implementation of the
	 * DeclarativeComponent because it will perfom diffrent actions depending on
	 * the implementation that is to be disabled.
	 * 
	 * 
	 * @param componentName
	 *            the name of the component(s) to be disable
	 * @param requestBundle
	 *            the bundle the requesting component is located in
	 */
	public void disableComponent(String componentName, Bundle requestBundle,
			boolean isStopping) throws ComponentException {

		System.out
				.println("\n****************** disable component was called in SCR ***********");

		/* go through all active components */
		for (int i = 0; i < activeComponents.size(); i++) {
			/* get the object located in the vector */
			Object object = activeComponents.get(i);

			/* check if it is an immediate component */
			if (object instanceof ImmediateComponent) {
				/* Immediate create an immediate component variable */
				ImmediateComponent component = (ImmediateComponent) object;
				/* get the declaration */
				ComponentDeclaration componentDeclaration = component
						.getComponentDeclaration();

				/* check if the name equals the given name */
				if (componentDeclaration.getComponentName().equals(
						componentName)) {

					/*
					 * check that the requesting bundle is in the same bundle as
					 * this component declared int the componentDeclaration
					 */
					if (componentDeclaration.getDeclaringBundle().equals(
							requestBundle)) {
						try {
							/*
							 * unbind the current Immediate commponents
							 * references
							 */
							invokeReferences(component, component
									.getComponentContext()
									.getComponentInstance(), false);

							try {
								/*
								 * inactivate the component use the components
								 * own context
								 */
								inactivateInstance(component
										.getComponentContext());
								/* remove the instance from the vector */
								activeComponents.remove(object);

								/*
								 * add the componentDeclaration to the
								 * inactiveComponents vector
								 */
								inactiveComponents.add(component
										.getComponentDeclaration());
								/* set the object to null */
								object = null;
								/* set the component to null */
								component = null;
								/* sweep the garbage collection no gurantee but */
								System.gc();

							} catch (ComponentException e) {
								/* throw the error */
								throw new ComponentException(
										"error when deactivating "
												+ componentDeclaration
														.getComponentName()
												+ " due to:\n" + e, e
												.getCause());
							}

						} catch (ComponentException e) {
							/* throw the error */
							throw new ComponentException(
									"error when unbinding "
											+ componentDeclaration
													.getComponentName()
											+ " due to:\n" + e, e.getCause());
						}

					}// end
					// if(componentDeclaration.getDeclaringBundle().equals(requestBundle))

				}// end
				// if(componentDeclaration.getComponentName().equals(componentName))

			}// end if(object instanceof ImmediateComponent)

			/* check if CustomDelayedService */
			if (object instanceof CustomDelayedService) {

				/* create an CustomDelayedService variable */
				CustomDelayedService component = (CustomDelayedService) object;
				/* get the component declaration */
				ComponentDeclaration componentDeclaration = component
						.getComponentDeclaration();

				/* check if the name equals the given name */
				if (componentDeclaration.getComponentName().equals(
						componentName)) {
					/* ensure that they are in the same bundle */
					if (componentDeclaration.getDeclaringBundle().equals(
							requestBundle)) {
						try {
							if (component != null) {
								ComponentContext componentContext = component
										.getComponentContext();

								/*
								 * check that the component context is not null
								 * that might happen if this component is
								 * unrequested
								 */
								if (componentContext != null) {
									/* invoke the declared references */
									invokeReferences(component, component
											.getComponentContext()
											.getComponentInstance(), false);

									try {

										/*
										 * just make sure that the service is
										 * registered
										 */
										if (componentDeclaration
												.getDeclaringBundle()
												.getRegisteredServices().length > 0) {

											/*
											 * an Illegal state may occure if a
											 * bundle STOP event causes this and
											 * the component is to be disable is
											 * in the same bundle as the STOPPED
											 * bundle.
											 */
											try {
												component
														.getServiceRegistration()
														.unregister();
											} catch (IllegalStateException e) {
												/*
												 * do nothing the service will
												 * disappear when the bundle is
												 * stopped
												 */
											}
										}

										try {

											/* in activate the component */
											inactivateInstance(component
													.getComponentContext());

											synchronized (activeComponents) {
												/*
												 * remove the object from active
												 * compnents vector
												 */
												activeComponents.remove(object);
											}

											synchronized (inactiveComponents) {
												if (!inactiveComponents
														.contains(component
																.getComponentDeclaration())) {
													/*
													 * add the declaration to
													 * inactive components
													 * vector
													 */
													inactiveComponents
															.add(component
																	.getComponentDeclaration());
												}
											}
											/* set the object to null */
											object = null;
											/* set the component to null */
											component = null;
											/* do a mark and sweep, no gurantee */
											System.gc();

										} catch (ComponentException e) {
											/* throw the error */
											throw new ComponentException(
													"error when deactivating "
															+ componentDeclaration
																	.getComponentName()
															+ " due to:\n" + e,
													e.getCause());
										} catch (Exception e) {
											/* throw the error */
											throw new ComponentException(
													"error when deactivating "
															+ componentDeclaration
																	.getComponentName()
															+ " due to:\n" + e,
													e.getCause());
										}

									} catch (Exception e) {
										/*
										 * this happens if something goes wrong
										 * unregistering the service print the
										 * error
										 */
										e.printStackTrace();
										throw new ComponentException(
												"error unregistering the service for "
														+ componentDeclaration
																.getComponentName()
														+ " due to:\n" + e, e
														.getCause());

									}
								} else {
									/*
									 * this component has never been requested
									 * and there fore no context and instance is
									 * available unregister the service.
									 */
									System.out.println("Unregister for:"
											+ componentDeclaration
													.getComponentName());

									synchronized (activeComponents) {
										/*
										 * remove the object from active
										 * components vector
										 */
										activeComponents.remove(object);
									}

									/*
									 * just make sure that the service is
									 * registered
									 */
									if (componentDeclaration
											.getDeclaringBundle()
											.getRegisteredServices().length > 0) {

										/*
										 * an Illegal state may occure if a
										 * bundle STOP event causes this and the
										 * component is to be disable is in the
										 * same bundle as the STOPPED bundle.
										 */

										try {
											/* unregister the service */
											component.getServiceRegistration()
													.unregister();
										} catch (IllegalStateException e) {
											/*
											 * do nothing this is caused because
											 * the method is called during a
											 * STOP event
											 */
											//System.err.println(e);
										}
									}

									synchronized (inactiveComponents) {
										if (!inactiveComponents
												.contains(component
														.getComponentDeclaration())) {
											if (!isStopping) {
												/*
												 * add the declaration to
												 * inactive components vector
												 */
												inactiveComponents
														.add(component
																.getComponentDeclaration());
											}
										}
									}
									/* set the object to null */
									object = null;
									/* set the component to null */
									component = null;

								}
							}

						} catch (ComponentException e) {
							/* print the error */
							throw new ComponentException(
									"error when unbinding "
											+ componentDeclaration
													.getComponentName()
											+ " due to:\n" + e, e.getCause());
						} catch (Exception e) {
							e.printStackTrace();
							/* print the error */
							throw new ComponentException(
									"error when unbinding "
											+ componentDeclaration
													.getComponentName()
											+ " due to:\n" + e, e.getCause());
						}

					}// end
					// if(componentDeclaration.getDeclaringBundle().equals(requestBundle))
				}

			}// end if(object instanceof CustomDelayedService)

			/* check if it is an CustomComponentServiceFactory */
			if (object instanceof CustomComponentServiceFactory) {
				/* create a CustomComponentServiceFactory variable */
				CustomComponentServiceFactory component = (CustomComponentServiceFactory) object;
				/* get the componend declaration */
				ComponentDeclaration componentDeclaration = component
						.getComponentDeclaration();

				/* check if the name equals the given name */
				if (componentDeclaration.getComponentName().equals(
						componentName)) {
					/* ensure that they are in the same bundle */
					if (componentDeclaration.getDeclaringBundle().equals(
							requestBundle)) {

						/*
						 * get the vector with all component contexts created by
						 * this configuration
						 */
						Vector contexts = component.getComponentContexts();

						/* iterate and disable all instances created */
						for (int j = 0; j < contexts.size(); j++) {
							ComponentContext componentContext = (ComponentContext) contexts
									.get(j);
							try {
								/* unbind the references */
								invokeReferences(component, componentContext
										.getComponentInstance(), false);
								try {
									/* inactivate the component */
									inactivateInstance(componentContext);
									/* remove the context from the orgin vector */
									contexts.remove(componentContext);

									/*
									 * remove the object from active components
									 * vector
									 */
									activeComponents.remove(object);
									/*
									 * check that the component declaration not
									 * already exists here
									 */
									if (!inactiveComponents.contains(component
											.getComponentDeclaration())) {
										/*
										 * add the declaration to inactive
										 * components vector
										 */
										inactiveComponents.add(component
												.getComponentDeclaration());
									}
									/* set the object to null */
									object = null;
									/* set the component to null */
									componentContext = null;
									/* do a mark and sweep, no gurantee */
									System.gc();

								} catch (ComponentException e) {
									/* print the error */
									throw new ComponentException(
											"error when deactivating "
													+ componentDeclaration
															.getComponentName()
													+ " due to:\n" + e, e
													.getCause());
								}

							} catch (ComponentException e) {
								/* print the error */
								throw new ComponentException(
										"error when unbinding "
												+ componentDeclaration
														.getComponentName()
												+ " due to:\n" + e, e
												.getCause());
							}

						} // end for(int j= 0;j<contexts.size();j++)

						try {

							/*
							 * remove the object from active components vector
							 * do this here as well because sometimes the for
							 * statement will be skipped, i.e, the service has
							 * never been requested it is IMPORTANT TO REMOVE
							 * THIS before unregistration. During unregistration
							 * will the SCR check the array again and it should
							 * not find this object
							 */
							activeComponents.remove(object);
							/* unregister the services */
							component.getServiceRegistration().unregister();

							/*
							 * check that the component declaration not already
							 * exists here
							 */
							if (!inactiveComponents.contains(component
									.getComponentDeclaration())) {
								/*
								 * add the declaration to inactive components
								 * vector
								 */
								inactiveComponents.add(component
										.getComponentDeclaration());
							}
							/* set the object to null */
							object = null;

							/* set the component to null */
							component = null;

						} catch (Exception e) {

							/*
							 * this happens if something goes wrong
							 * unregistering the service print the error
							 */
							throw new ComponentException(
									"error unregistering the component service factory "
											+ componentDeclaration
													.getComponentName() + ":"
											+ e, e.getCause());

						}

					}// end
					// if(componentDeclaration.getDeclaringBundle().equals(requestBundle))
				}

			}// end if(object instanceof CustomComponentServiceFactory)

			/* check if it is a component factory */
			if (object instanceof CustomComponentFactory) {
				/* create a CustomComponentFactory */
				CustomComponentFactory component = (CustomComponentFactory) object;
				/* get the component declaration */
				ComponentDeclaration componentDeclaration = component
						.getComponentDeclaration();

				/* check if the name equals the given name */
				if (componentDeclaration.getComponentName().equals(
						componentName)) {
					/* ensure that they are in the same bundle */
					if (componentDeclaration.getDeclaringBundle().equals(
							requestBundle)) {

						try {
							/* remove the object */
							activeComponents.remove(object);

							/* unregister the componentFactory */
							try {
								/* unregister the service */
								component.getServiceRegistration().unregister();
							} catch (IllegalStateException e) {
								/*
								 * might happen if the components bundle is
								 * stopping
								 */
							}

							/* make sure it doesn't already exists in the vector */
							if (!inactiveComponents.contains(component
									.getComponentDeclaration())) {
								/* add the declaration to inactive components */
								inactiveComponents.add(component
										.getComponentDeclaration());
							}
							/* set the component factory to null */
							component = null;

						} catch (Exception e) {
							throw new ComponentException(
									"error unregistering the component factory "
											+ componentDeclaration
													.getComponentName()
											+ " due to:\n" + e, e.getCause());
						}

					}// end
					// if(componentDeclaration.getDeclaringBundle().equals(requestBundle))

				}// end
				// if(componentDeclaration.getComponentName().equals(componentName)

			}// end if (object instanceof CustomComponentFactory)

		}// end (int i=0;i<activeComponents.size();i++)

	}

	/**
	 * this method will enable component(s) with a given name it will also
	 * ensure that the component is in the same bundle as the requesting
	 * component is before it will enable it.
	 * 
	 * @param componentName
	 *            the name of the component(s) to be enabled null enables all in
	 *            the bundle
	 * @param requestBundle
	 *            the bundle the requesting component is located in
	 */
	public void enableComponent(String componentName, Bundle requestBundle)
			throws ComponentException {
		/* go through all component declaration */
		for (int i = 0; i < inactiveComponents.size(); i++) {
			try {
				/* create a componentDeclaration variable */
				ComponentDeclaration componentDeclaration = (ComponentDeclaration) inactiveComponents
						.get(i);

				/* check if the component name is equal to the given name */
				if (componentDeclaration.getComponentName().equals(
						componentName)
						|| componentName == null) {

					/* check if they are in the same bundle */
					if (componentDeclaration.getDeclaringBundle().equals(
							requestBundle)) {
						try {
							/*
							 * evalute the declaration override the
							 * componentdeclarations isAutoEnabled()
							 */
							evaluateComponentDeclaration(componentDeclaration,
									true);

						} catch (ComponentException e) {
							/* throw the error */
							throw e;
						}

					}// end
					// if(componentDeclaration.getDeclaringBundle().equals(requestBundle))

				}// end
				// if(componentDeclaration.getComponentName().equals(componentName)

			} catch (ClassCastException e) {
				throw new ComponentException(
						"error in enableComponent(..) due to:\n" + e, e
								.getCause());
			}// end try

		}//end for(int i=0;i<inactiveComponents.size();i++)

	}

	/**
	 * locates a service with a given name declared in the reference element of
	 * a component declaration. This method will use the internal bundle context
	 * to locate services.
	 * 
	 * @param ServiceName
	 *            the name of the service declared in the XML file
	 * @param componentName
	 *            the name of the requesting component
	 * 
	 * @return object the service object
	 */
	public Object locateService(String serviceName, String componentName)
			throws ComponentException {

		/*
		 * TODO Check if ID is needed here to identify the component split up in
		 * small methods
		 */

		for (int i = 0; i < activeComponents.size(); i++) {

			/* check if the object is an ImmediateComponent */
			if (activeComponents.get(i) instanceof DeclarativeComponent) {
				/* create an ImmediateComponent variable */
				DeclarativeComponent component = (DeclarativeComponent) activeComponents
						.get(i);

				/* check if the names are equal */
				if (component.getComponentDeclaration().getComponentName()
						.equals(componentName)) {

					/* create an component declaration variable */
					ComponentDeclaration componentDeclaration = component
							.getComponentDeclaration();
					/* create an arraylist variable */
					ArrayList references = componentDeclaration
							.getReferenceInfo();
					/* create an iterator */
					Iterator referenceIterator = references.iterator();

					while (referenceIterator.hasNext()) {
						/* create a ComponentReferenceInfo variable */
						ComponentReferenceInfo referenceInfo = (ComponentReferenceInfo) referenceIterator
								.next();

						/*
						 * check if the the declared reference name is equal to
						 * the given 'serviceName'
						 */
						if (referenceInfo.getReferenceName()
								.equals(serviceName)) {

							/* create a string representing the interface name */
							String interfaceName = referenceInfo
									.getInterfaceType();
							/* locate the services */
							ServiceReference serviceReference = bundleContext
									.getServiceReference(interfaceName);

							if (serviceReference != null) {
								return bundleContext
										.getService(serviceReference);

							}

						}// end
						// if(referenceInfo.getReferenceName().equals(serviceName))

					}// end while(referenceIterator.hasNext())

				}// end
				// if(component.getComponentDeclaration().getComponentName()
				//.equals(componentName))

			}// if(activeComponents.get(i) instanceof DeclarativeComponent)

		}// end for(int i=0;i<activeComponents.size();i++)

		return null;
	}

	/**
	 * locates services with a given name declared in the reference element of a
	 * component declaration. This method will use the internal bundle context
	 * to locate services.
	 * 
	 * @param ServiceName
	 *            the name of the service declared in the XML file
	 * @param componentName
	 *            the name of the requesting component
	 * 
	 * @return object the service object
	 */
	public Object[] locateServices(String serviceName, String componentName)
			throws ComponentException {

		/*
		 * TODO Check if ID is needed here to identify the component split up in
		 * small methods
		 */

		for (int i = 0; i < activeComponents.size(); i++) {

			/* check if the object is an ImmediateComponent */
			if (activeComponents.get(i) instanceof DeclarativeComponent) {
				/* create an ImmediateComponent variable */
				DeclarativeComponent component = (DeclarativeComponent) activeComponents
						.get(i);

				/* check if the names are equal */
				if (component.getComponentDeclaration().getComponentName()
						.equals(componentName)) {

					/* create an component declaration variable */
					ComponentDeclaration componentDeclaration = component
							.getComponentDeclaration();
					/* create an arraylist variable */
					ArrayList references = componentDeclaration
							.getReferenceInfo();
					/* create an iterator */
					Iterator referenceIterator = references.iterator();

					while (referenceIterator.hasNext()) {
						/* create a ComponentReferenceInfo variable */
						ComponentReferenceInfo referenceInfo = (ComponentReferenceInfo) referenceIterator
								.next();

						/*
						 * check if the the declared reference name is equal to
						 * the given 'serviceName'
						 */
						if (referenceInfo.getReferenceName()
								.equals(serviceName)) {
							try {
								/*
								 * create a string representing the interface
								 * name
								 */
								String interfaceName = referenceInfo
										.getInterfaceType();
								/* locate the services */
								ServiceReference[] serviceReferences = bundleContext
										.getServiceReferences(interfaceName,
												null);

								/*
								 * create a an array with the same size as the
								 * service references
								 */
								Object[] services = new Object[serviceReferences.length];
								if (serviceReferences != null) {
									/* go through the references */
									for (int j = 0; j < serviceReferences.length; j++) {
										/* get the service */
										Object service = bundleContext
												.getService(serviceReferences[j]);
										/* check that the service is not null */
										if (service != null) {
											/*
											 * assign the service to the current
											 * position in the array
											 */
											services[j] = service;
										} else {
											/*
											 * throw this if service is null
											 * should not happen
											 */
											throw new ComponentException(
													"fatal internal error in locateServices(..)"
															+ " got service reference but no service object");
										}
									}
									/* return the service array */
									return services;

								}
							} catch (InvalidSyntaxException e) {
								/* throw an error if invalid syntax is used */
								throw new ComponentException(e.getMessage(), e
										.getCause());
							}
						}
					}

				}

			}// if(activeComponents.get(i) instanceof DeclarativeComponent)

		}// end for(int i=0;i<activeComponents.size();i++)

		return null;
	}

	/**
	 * this method will evaluate all component declarations create custom
	 * service objects depending on what kind of component the declaration
	 * declares. if the decalaration is not satisfied it will just save the
	 * declaration for later use.
	 * 
	 * @param componentDeclaration
	 *            the componentDeclaration
	 * @param overideEnable
	 *            if the method should over care about the component
	 *            declarations isAutoEnable() attribute.
	 */
	private synchronized void evaluateComponentDeclaration(
			ComponentDeclaration componentDeclaration, boolean overideEnable)
			throws ComponentException {

		/* check if enabled */
		if (componentDeclaration.isAutoEnable() || overideEnable) {
			/* if it is enable when the services have to be registered */
			if (componentDeclaration.getFactory() != null) {

				/* check if the component is satisfied and enabled */
				if (isSatisfied(componentDeclaration)
						&& componentDeclaration.isAutoEnable()) {
					System.out.println("********************** "
							+ " register component factory for:"
							+ componentDeclaration.getComponentName()
							+ " **************");

					try {
						/* register the component factory */
						CustomComponentFactory componentFactory = registerComponentFactory(componentDeclaration);
						/* add the factory to the vector */
						activeComponents.add(componentFactory);

					} catch (ComponentException e) {
						throw e;
					}

				} else {
					/*
					 * check that the vector doesn't contain the
					 * componentDeclaration
					 */
					if (!inactiveComponents.contains(componentDeclaration)) {
						System.out.println("*************** "
								+ componentDeclaration.getComponentName()
								+ " is not satisfied **********");
						/* Save data here */
						inactiveComponents.add(componentDeclaration);
					}
				}

			} else {

				/* this is a immediate or servicefactory component */
				if (isSatisfied(componentDeclaration)) {
					/*
					 * check if it has references ,i.e, if its a delayed
					 * component
					 */
					if (componentDeclaration.getServiceInfo().size() > 0) {

						/*
						 * the tricky thing is that we have to create one
						 * CustomComponent instance for MANY provided interface
						 */
						ArrayList serviceInfos = componentDeclaration
								.getServiceInfo();

						Iterator serviceIterator = serviceInfos.iterator();

						Vector vectorInterfaces = new Vector();

						while (serviceIterator.hasNext()) {
							/* create the info instance */
							ComponentServiceInfo info = (ComponentServiceInfo) serviceIterator
									.next();
							/*
							 * create an arraylist holding the declared
							 * interfaces
							 */
							ArrayList interfaces = info
									.getComponentInterfaces();

							/* create an iterator */
							Iterator interfacesIterator = interfaces.iterator();

							/* iterate through the interface */
							while (interfacesIterator.hasNext()) {
								/*
								 * create a string representing the interface
								 */
								String interfaceName = (String) interfacesIterator
										.next();

								vectorInterfaces.add(interfaceName);
							}//end while(interfacesIterator.hasNext())

						} // end while(serviceIterator.hasNext())

						String[] allInterfaces = new String[vectorInterfaces
								.size()];

						/* copy all objects to the allinterfaces array */
						for (int i = 0; i < vectorInterfaces.size(); i++) {
							allInterfaces[i] = (String) vectorInterfaces.get(i);
						}

						/* check if it is a service factory */
						if (componentDeclaration.isServiceFactory()) {
							System.out
									.println("********************** "
											+ " register component service factory for:"
											+ componentDeclaration
													.getComponentName()
											+ " **************");
							try {

								/*
								 * register service factory component here
								 */
								CustomComponentServiceFactory serviceComponent = registerComponentServiceFactory(
										allInterfaces, componentDeclaration);

								/*
								 * add the service factory to the vector of
								 * active components
								 */
								activeComponents.add(serviceComponent);

							} catch (ComponentException e) {
								/* just throw the error */
								throw e;
							}

						} else {
							System.out.println("********************** "
									+ " register delayed service for:"
									+ componentDeclaration.getComponentName()
									+ " **************");
							try {
								/*
								 * register custom delayed service here
								 */
								DeclarativeComponent delayedComponent = registerDelayedComponent(
										allInterfaces, componentDeclaration);

								/*
								 * add the delayed service to the vector of
								 * active components
								 */
								activeComponents.add(delayedComponent);

							} catch (ComponentException e) {
								e.printStackTrace();
								/* throw the error */
								throw e;
							} catch (Exception e) {
								throw new ComponentException(e.getMessage(), e
										.getCause());
							}

						}

					} else {

						/* this is an immediate component */
						activeComponents.add(new ImmediateComponent(
								componentDeclaration));

					}

				} else {
					System.out.println("********* "
							+ componentDeclaration.getComponentName()
							+ " is not satisfied ************");

					/* save the component data here */
					/*
					 * check that the vector doesn't contain the
					 * componentDeclaration
					 */
					if (!inactiveComponents.contains(componentDeclaration)) {
						/* Save data here */
						inactiveComponents.add(componentDeclaration);
					}

				}

			}

		} else {
			System.out
					.println("**************** autoEnable is false in the component delcaration ********************");
			/* check that the vector doesn't contain the componentDeclaration */
			if (!inactiveComponents.contains(componentDeclaration)) {
				/* Save data here */
				inactiveComponents.add(componentDeclaration);
			}
		} // if(componentDeclaration.isAutoEnable())

	}

	/**
	 * This method will activate a component using reflection Some component
	 * declares a activate(ComponentContext context) method just because they
	 * want the ComponentContext. This method will try to call the method and
	 * pass the context as argument. This method may fail then a component
	 * doesn't declare an activate method. Normally a NoSuchMethodException is
	 * raises, but this has to be ignored.
	 * 
	 * @param ComponentContext
	 *            the context which should be passed
	 */
	private void activateInstance(ComponentContext componentContext)
			throws ComponentException {

		/* get the component instance */
		Object componentInstance = componentContext.getComponentInstance()
				.getInstance();
		/* create a string representing the method name */
		String methodName = "activate";
		try {

			System.out.println("************* trying to invoke "
					+ componentInstance.toString() + ".activate(...) method ");

			/* get the method */
			Method method = componentInstance.getClass().getDeclaredMethod(
					methodName, new Class[] { ComponentContext.class });

			/* set this as accessible */
			method.setAccessible(true);
			/* invoke the method */
			method.invoke(componentInstance, new Object[] { componentContext });

		} catch (NoSuchMethodException e) {
			System.out.println(componentInstance
					+ " does not declare an activate(..) method");
			/* do nothing here */
			//throw new ComponentException(e.getMessage(), e.getCause());
		} catch (InvocationTargetException e) {
			throw new ComponentException(e.getMessage(), e.getCause());
		} catch (IllegalAccessException e) {
			throw new ComponentException(e.getMessage(), e.getCause());
		}

	}

	/**
	 * This method will inactivate a component using reflection Some component
	 * declares a activate(ComponentContext context) method just because they
	 * want the ComponentContext. This method will try to call the method and
	 * pass the context as argument. This method may fail then a component
	 * doesn't declare an activate method.
	 * 
	 * @param ComponentContext
	 *            the context which should be passed
	 */
	private void inactivateInstance(ComponentContext componentContext)
			throws ComponentException {

		/* get the component instance */
		Object componentInstance = componentContext.getComponentInstance()
				.getInstance();
		/* create a string representing the method name */
		String methodName = "deactivate";
		try {

			System.out
					.println("************* trying to invoke "
							+ componentInstance.toString()
							+ ".deactivate(...) method ");

			/* get the method */
			Method method = componentInstance.getClass().getDeclaredMethod(
					methodName, new Class[] { ComponentContext.class });

			/* set this as accessible */
			method.setAccessible(true);
			/* invoke the method */
			method.invoke(componentInstance, new Object[] { componentContext });

		} catch (NoSuchMethodException e) {
			System.out.println(componentInstance
					+ " does not declare an deactivate(..) method");
			/* do nothing here */
			//throw new ComponentException(e.getMessage(), e.getCause());
		} catch (InvocationTargetException e) {
			throw new ComponentException(e.getMessage(), e.getCause());
		} catch (IllegalAccessException e) {
			throw new ComponentException(e.getMessage(), e.getCause());
		}

	}

	/**
	 * this method checks if a ComponentDeclaration is satisfied or not. It will
	 * check that all references/dependencies are available. If they are a
	 * true will be returned other wise false.
	 * 
	 * @param componentDeclaration
	 * @return true if satisfies false otherwise
	 */
	private boolean isSatisfied(ComponentDeclaration componentDeclaration) {

		/* always assume that the component is satisfied */
		boolean isSatisfied = true;

		/* create an arraylist with the components reference infos */
		ArrayList componentReferences = componentDeclaration.getReferenceInfo();

		/* create an iterator */
		Iterator referenceIterator = componentReferences.iterator();

		while (referenceIterator.hasNext()) {
			/* create the a temporary object */
			ComponentReferenceInfo componentReference = (ComponentReferenceInfo) referenceIterator
					.next();

			/* get the policy */
			String policy = componentReference.getPolicy();
			/* get the cardinality */
			String cardinality = componentReference.getCardinality();
			/* get the refered interface */
			String interfaceName = componentReference.getInterfaceType();
			/* get the filter */
			String filter = componentReference.getTarget();

			try {
				/* get the service reference */
				ServiceReference[] reference = bundleContext
						.getServiceReferences(interfaceName, filter);

				/* check cardinality */
				if ((cardinality.equals("1..1") || cardinality.equals("1..n"))
						&& reference == null) {
					/* this component must not be available */
					return false;
				}

			} catch (InvalidSyntaxException e) {
				System.err.println("error when checking if "
						+ componentDeclaration.getComponentName()
						+ " is satisfied check the target attribute in\n"
						+ " in the component declaration");
			}

		}

		return isSatisfied;
	}
	
	/**
	 * this method registers a component factory into the 
	 * framework. A component factory is responsible for
	 * creating new components on demand.
	 * 
	 * @param componentDeclaration the component declaration for the component
	 * @return CustomComponentFactory the factory which has been created
	 * @throws ComponentException if fails to register a component factory
	 */
	private CustomComponentFactory registerComponentFactory(
			ComponentDeclaration componentDeclaration)
			throws ComponentException {

		/* create a property table */
		Dictionary properties = new Hashtable();
		/* put the component factory constant in it */
		properties.put(ComponentConstants.COMPONENT_FACTORY,
				componentDeclaration.getFactory());

		/* put the component name in it */
		properties.put(ComponentConstants.COMPONENT_NAME, componentDeclaration
				.getComponentName());

		/* put the description in it */
		properties.put(Constants.SERVICE_DESCRIPTION,
				"Declarative component factory created by SCR");

		properties.put(Constants.SERVICE_PID,
				"org.osgi.service.component.ComponentFactory");

		/* create a new factory instance */
		CustomComponentFactory factory = new CustomComponentFactory(
				componentDeclaration);

		try {
			/* register the factory as service into the framework */
			ServiceRegistration registration = componentDeclaration
					.getDeclaringBundle().getBundleContext().registerService(
							ComponentFactory.class.getName(), factory,
							properties);

			/* add the service reference */
			factory.setServiceReference(registration.getReference());

			/* add the registration */
			factory.setServiceRegistration(registration);

			return factory;

		} catch (Exception e) {
			throw new ComponentException("Error registering component factory",
					e.getCause());
		}

	}

	/**
	 * this method registers a CustomComponentServiceFactory into the framework
	 * the service factory will always create new instances on demand. 
	 * 
	 * @param interfaceNames a string array with interfaces for this services
	 * @param componentDeclaration the component declaration for this component
	 * @return CustomComponentServiceFactory the service factory object
	 * @throws ComponentException if fails to register 
	 */
	private CustomComponentServiceFactory registerComponentServiceFactory(
			String[] interfaceNames, ComponentDeclaration componentDeclaration)
			throws ComponentException {

		/* create a property table */
		Dictionary properties = new Hashtable();

		/* put the component name in it */
		properties.put(ComponentConstants.COMPONENT_NAME, componentDeclaration
				.getComponentName());

		/* put the description in it */
		properties.put(Constants.SERVICE_DESCRIPTION,
				"Declarative service factory created by SCR");

		/* create a service factory */
		CustomComponentServiceFactory serviceFactory = new CustomComponentServiceFactory(
				interfaceNames, componentDeclaration);

		try {
			/* register the service use the interface name */
			ServiceRegistration registration = componentDeclaration
					.getDeclaringBundle().getBundleContext().registerService(
							interfaceNames, serviceFactory, properties);

			/* add the service reference to the instance */
			serviceFactory.setServiceReference(registration.getReference());
			/* add the service registration to the instance */
			serviceFactory.setServiceRegistration(registration);

			return serviceFactory;

		} catch (Exception e) {
			throw new ComponentException(
					"Error registering component service factory:" + e, e
							.getCause());
		}

	}
	
	/**
	 * this method registers a delayed component into the framework
	 * delayed components declares service(s) and instances will be
	 * created on demand.
	 * 
	 * @param interfaceNames an array with interfaces for this component
	 * @param componentDeclaration the component declaration for this component
	 * @return CustomDelayedService if managed to register else null
	 * @throws ComponentException if fails to register the services declared
	 */
	private CustomDelayedService registerDelayedComponent(
			String[] interfaceNames, ComponentDeclaration componentDeclaration)
			throws ComponentException {

		/* create a property table */
		Dictionary properties = new Hashtable();

		/* put the component name in it */
		properties.put(ComponentConstants.COMPONENT_NAME, componentDeclaration
				.getComponentName());

		/* put the description in it */
		properties.put(Constants.SERVICE_DESCRIPTION,
				"Declarative delayed component service created by SCR");

		/* create a service factory */
		CustomDelayedService delayedService = new CustomDelayedService(
				interfaceNames, componentDeclaration);

		try {

			/* register the delayed service use the interface name */
			ServiceRegistration registration = componentDeclaration
					.getDeclaringBundle().getBundleContext().registerService(
							interfaceNames, delayedService, properties);

			/* ad the service registration to the delayed service */
			delayedService.setServiceRegistration(registration);
			/* add the service reference to the delayed service */
			delayedService.setServiceReference(registration.getReference());

			return delayedService;

		} catch (Exception e) {
			throw new ComponentException(
					"error registering delayed component service:" + e, e
							.getCause());
		}

	}

	/**
	 * this method creates a componentContext with all features within
	 * 
	 * @param componentDeclaration the componentDeclaration
	 * @param componentID the Components ID
	 * @param registration the Registration object if any 
	 * @param properties the properties for this component
	 * @param usingBundle the bundle using the service the component offers
	 * @param requestBundle the bundle requesting the service
	 * @return ComponentContext if creation succeeded else null
	 * @throws ComponentException if fails to create a context
	 */
	private ComponentContext createComponentContext(
			ComponentDeclaration componentDeclaration, long componentID,
			ServiceRegistration registration, Dictionary properties,
			Bundle usingBundle, Bundle requestBundle) throws ComponentException {
		try {

			/* create a new context */
			ComponentContextImpl newContext = new ComponentContextImpl(
					createComponentInstance(componentDeclaration),
					bundleContext, properties, registration, this, usingBundle,
					requestBundle);

			return newContext;

		} catch (Exception e) {
			throw new ComponentException(e);

		}
	}

	/**
	 * This function will use a ComponentDeclaration to create a new
	 * ComponentInstance object
	 * 
	 * @param componentDeclaration
	 *            the component declaration
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
		return new ComponentInstanceImpl(element, this);
	}

	/**
	 * This method will track the dependencies and give them to the declarative
	 * service which this class controlls. Dependencies are kept in the class
	 * ComponentReferenceInfo.The method can be used for unbind as well as bind 
	 * actions.
	 * 
	 * @param componentDeclaration
	 *            the component declaration
	 * @param componentInstance
	 *            the componentInstance
	 * @param bind if its a bind action or not
	 * 
	 * @author Magnus Klack
	 */
	private synchronized void invokeReferences(DeclarativeComponent component,
			ComponentInstance componentInstance, boolean bind)
			throws ComponentException {

		ComponentDeclaration componentDeclaration = component
				.getComponentDeclaration();

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

			if (componentRef.getInterfaceType() != null
					&& componentRef != null
					&& (componentRef.getCardinality().equals("0..1") || componentRef
							.getCardinality().equals("1..1"))) {

				/* get the interface */
				String interfaceName = componentRef.getInterfaceType();
				/* print the service interface name */
				System.out
						.println("service interface name is:" + interfaceName);
				/* get the reference */
				String targetFilter = componentRef.getTarget();
				/* assign a component class object */
				Object componentObject = componentInstance.getInstance();

				/*
				 * create a string representing the method which should be
				 * called, i.e, the bind or unbind method
				 */
				String methodName;

				if (bind) {
					/* get the bind method */
					methodName = componentRef.getBind();
					/* print the bind method */
					System.out.println("The bind-method is:" + methodName);
				} else {
					/* get the bind method */
					methodName = componentRef.getUnbind();
					/* print the bind method */
					System.out.println("The unbind-method is:" + methodName);

				}

				if (methodName != null) {

					try {
						/* get the service references */
						ServiceReference[] serviceReferences = bundleContext
								.getServiceReferences(interfaceName,
										targetFilter);

						/*
						 * check that the reference is there if it isn't then do
						 * nothing. Null may occur if the reference is in the
						 * same bundle as the component is and the bundle is
						 * stopped
						 */
						if (serviceReferences != null) {
							/* print the service reference */
							System.out.println("The service reference is:"
									+ serviceReferences[0]
									+ " with cardinality:"
									+ componentRef.getCardinality());

							/* get the service */
							Object reference = bundleContext
									.getService(serviceReferences[0]);

							if (reference != null) {
								try {
									/* print that we try to invoke the method */
									System.out
											.println("************* Trying to invoke "
													+ methodName
													+ " in class:"
													+ componentDeclaration
															.getImplementation()
													+ " ******************");

									/* get the method */
									Method method = componentObject
											.getClass()
											.getDeclaredMethod(
													methodName,
													new Class[] { Class
															.forName(interfaceName) });

									/* set this as accessible */
									method.setAccessible(true);
									/* invoke the method */
									method.invoke(componentObject,
											new Object[] { reference });

									if (bind) {
										/* bind the declarative component */
										component
												.bindReference(serviceReferences[0]);
									} else if (!bind) {
										/* unbind the declarative component */
										component
												.unBindReference(serviceReferences[0]);
									}

								} catch (NoSuchMethodException e) {
									throw new ComponentException(
											e.getMessage(), e.getCause());
								} catch (IllegalAccessException e) {
									throw new ComponentException(
											e.getMessage(), e.getCause());
								} catch (InvocationTargetException e) {
									throw new ComponentException(
											e.getMessage(), e.getCause());
								} catch (ClassNotFoundException e) {
									throw new ComponentException(
											e.getMessage(), e.getCause());
								}
							} else {
								throw new ComponentException(
										"error getting service "
												+ interfaceName
												+ " in invokeReferences() the service is null");
							}

						} else {
							if (bind
									&& componentRef.getCardinality().equals(
											"1..1")) {
								throw new ComponentException(
										"error getting service "
												+ interfaceName
												+ " in invokeReferences() the reference is null");
							}

						}

					} catch (InvalidSyntaxException e) {
						throw new ComponentException(
								"error getting services due to:\n"
										+ e.getMessage(), e.getCause());

					}
				}//end if(methodName!=null)
			} else if (componentRef.getInterfaceType() != null
					&& componentRef != null
					&& (componentRef.getCardinality().equals("0..n") || componentRef
							.getCardinality().equals("1..n"))) {

				/*
				 * this is more complex the component has declared that all
				 * services available in the framwork should be bounded
				 */

				/* get the interface */
				String interfaceName = componentRef.getInterfaceType();
				/* print the service interface name */
				System.out
						.println("service interface name is:" + interfaceName);
				/* get the reference */
				String targetFilter = componentRef.getTarget();
				/* assign a component class object */
				Object componentObject = componentInstance.getInstance();
				/* create a string representing the method name */
				String methodName = null;

				if (bind) {
					/* get the bind method */
					methodName = componentRef.getBind();
					/* print the bind method */
					System.out.println("The bind-method is:" + methodName);
				} else {
					/* get the bind method */
					methodName = componentRef.getUnbind();
					/* print the bind method */
					System.out.println("The unbind-method is:" + methodName);

				}

				try {

					/* get all available services for this reference */
					ServiceReference[] serviceReferences = bundleContext
							.getServiceReferences(interfaceName, targetFilter);

					/* print the service reference */
					System.out.println("cardinality:"
							+ componentRef.getCardinality());

					if (serviceReferences != null) {
						for (int i = 0; i < serviceReferences.length; i++) {
							Object serviceInstance = bundleContext
									.getService(serviceReferences[i]);
							if (serviceInstance != null) {
								if (methodName != null) {
									try {
										/*
										 * print that we try to invoke the
										 * method
										 */
										System.out
												.println("************* Trying to invoke "
														+ methodName
														+ " in class:"
														+ componentDeclaration
																.getImplementation()
														+ " ******************");

										/* get the method */
										Method method = componentObject
												.getClass()
												.getDeclaredMethod(
														methodName,
														new Class[] { Class
																.forName(interfaceName) });

										/* set this as accessible */
										method.setAccessible(true);
										/* invoke the method */
										method
												.invoke(
														componentObject,
														new Object[] { serviceInstance });

										if (bind) {
											/* bind the declarative component */
											component
													.bindReference(serviceReferences[i]);
										} else if (!bind) {
											/* unbind the declarative component */
											component
													.unBindReference(serviceReferences[i]);
										}

									} catch (NoSuchMethodException e) {
										e.printStackTrace();
										throw new ComponentException(e
												.getMessage(), e.getCause());
									} catch (IllegalAccessException e) {
										e.printStackTrace();
										throw new ComponentException(e
												.getMessage(), e.getCause());
									} catch (InvocationTargetException e) {
										e.printStackTrace();
										throw new ComponentException(e
												.getMessage(), e.getCause());
									} catch (ClassNotFoundException e) {
										e.printStackTrace();
										throw new ComponentException(e
												.getMessage(), e.getCause());
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							}
						}
					}
				} catch (InvalidSyntaxException e) {
					e.printStackTrace();
					throw new ComponentException(
							"error getting multiple services due to:\n"
									+ e.getMessage(), e.getCause());

				} catch (Exception e) {
					throw new ComponentException(
							"error getting multiple services due to:\n"
									+ e.getMessage(), e.getCause());
				}

			}//if (componentRef.getInterfaceType() != null)

		}//end while

	}

	/**
	 * this method will invoke an active component configurations bind or unbind
	 * method and pass the new object to it.
	 * 
	 * @param serviceObject
	 *            the service object to be passed as argument
	 * @param instance
	 *            the target instance which should retrive the serviceObject
	 * @param methodName
	 *            the name of the bind method
	 * @param interfaceName
	 *            the interfaceName
	 */
	private void reInvokeReference(Object serviceObject, Object instance,
			String methodName, String interfaceName) {

		try {
			Method method = instance.getClass().getDeclaredMethod(methodName,
					new Class[] { Class.forName(interfaceName) });

			/* set this as accessible */
			method.setAccessible(true);

			try {
				/* invoke the method */
				method.invoke(instance, new Object[] { serviceObject });
			} catch (IllegalArgumentException e) {
				throw new ComponentException(
						"error in reinvokeReference due to:\n" + e.getMessage(),
						e.getCause());
			} catch (IllegalAccessException e) {
				throw new ComponentException(
						"error in reinvokeReference due to:\n" + e.getMessage(),
						e.getCause());
			} catch (InvocationTargetException e) {
				throw new ComponentException(
						"error in reinvokeReference due to:\n" + e.getMessage(),
						e.getCause());
			}

		} catch (SecurityException e) {
			throw new ComponentException(e.getMessage(), e.getCause());
		} catch (NoSuchMethodException e) {
			throw new ComponentException(e.getMessage(), e.getCause());
		} catch (ClassNotFoundException e) {
			throw new ComponentException(e.getMessage(), e.getCause());
		}

	}

	/**
	 * This class will create new instances of a given class it will use the
	 * class to trace and return an instance of the class.
	 * 
	 * @author Magnus Klack
	 */
	private class ComponentCreator {
		/** a Class representing the component implementation */
		private Class componentClass;

		/**
		 * Constructor creates a class
		 * 
		 * @param className
		 *            the class name
		 */
		protected ComponentCreator(String className) {
			try {
				/* create the class object */
				componentClass = Class.forName(className);
			} catch (ClassNotFoundException e) {
				/* print the error */
				System.err.println("error in ComponentCreator:" + e);

			}

		}

		/**
		 * Creates a new instance of the object uses Proxy technique to trace
		 * object
		 * 
		 * @return
		 */
		protected Object newInstance() {
			try {

				/* creates the class instance */
				Object returnObject = componentClass.newInstance();

				/*
				 * create the return object with tracing feature will give the
				 * object as proxy DO NOT USE THIS FEATURE! IF used then you
				 * have to to invoke methods via the proxy instance
				 */
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

	private class CustomDelayedService implements ServiceFactory,
			DeclarativeComponent {
		/** variable holding the instance */
		private ComponentInstance componentInstance = null;

		/** variable holding the interfacename */
		private String[] interfaceNames;

		/** variable holding the context */
		private ComponentContext componentContext;

		/** variable holding the component name */
		private ComponentDeclaration componentDeclaration;

		/** variable holding the service reference */
		private ServiceReference serviceReference;

		/** variable holding the serviceRegistration */
		private ServiceRegistration serviceRegistration;

		/** variable holding all bounded references */
		private Vector boundedReferences = new Vector();

		/*
		 * @param serviceInterface @param declaration
		 */
		public CustomDelayedService(String[] serviceInterfaces,
				ComponentDeclaration declaration) {

			/* assign the interface name */
			interfaceNames = serviceInterfaces;
			/* assign the component declaration */
			componentDeclaration = declaration;

		}

		public synchronized Object getService(Bundle bundle,
				ServiceRegistration registration) {
			System.out
					.println("************* getService is called in CustomDelayedService *********** ");

			if (componentInstance == null) {
				if (isSatisfied(componentDeclaration)) {
					try {
						/* increase the componentCounter */
						componentCounter++;

						Dictionary properties = componentDeclaration
								.getDeclaredProperties();

						/* put the component Name into the properties table */
						properties.put(ComponentConstants.COMPONENT_NAME,
								componentDeclaration.getComponentName());

						/* put the component id into it */
						properties.put(ComponentConstants.COMPONENT_ID,
								new Long(componentCounter));

						/* put the XML file here */
						properties.put(ComponentConstants.SERVICE_COMPONENT,
								componentDeclaration.getXmlFile());

						/*
						 * create a context request will come from the same
						 * bundle declaring the the component
						 */
						componentContext = createComponentContext(
								componentDeclaration, componentCounter,
								serviceRegistration, properties, null,
								componentDeclaration.getDeclaringBundle());

						try {
							/* bind its references */
							invokeReferences(this, componentContext
									.getComponentInstance(), true);

							try {
								activateInstance(componentContext);

								/* return the component instance */
								return componentContext.getComponentInstance()
										.getInstance();

							} catch (ComponentException e) {
								System.err
										.println("error when activating instance in CustomDelayedService.getService:\n"
												+ e);
							} catch (Exception e) {
								System.err
										.println("error when activating instance in CustomDelayedService.getService:\n"
												+ e);
							}

						} catch (ComponentException e) {
							/* print the error */
							System.err
									.println("error when binding references in CustomDelayedService.getService:\n"
											+ e);
						} catch (Exception e) {
							/* print the error */
							System.err
									.println("error when binding references in CustomDelayedService.getService:\n"
											+ e);
						}

					} catch (ComponentException e) {
						/* print the error */
						System.err
								.println("error when creating ComponentContext in CustomDelayedService.getService:\n"
										+ e);
					} catch (Exception e) {
						/* print the error */
						System.err
								.println("error when creating ComponentContext in CustomDelayedService.getService:\n"
										+ e);
					}

				} else {
					System.err.println("component is no longer satisfied");
				}

			} else {
				/* just return the instance here */
				return componentContext.getComponentInstance().getInstance();
			}

			return null;
		}

		public void ungetService(Bundle bundle,
				ServiceRegistration registration, Object service) {
			System.out
					.println("************* ungetService is called in CustomDelayedService *********** ");

		}

		/**
		 * this method sets the service reference used by this component
		 * 
		 * @param reference
		 *            the service reference
		 */
		public void setServiceReference(ServiceReference reference) {
			serviceReference = reference;

		}

		/**
		 * this method returns the service reference registered in the framework
		 * 
		 * @return SericeReference the serviceReference used by this object
		 */
		public ServiceReference getServiceReference() {
			return serviceReference;
		}

		/**
		 * sets the service registration of the component
		 * 
		 * @param registration
		 *            the service registration
		 */
		public void setServiceRegistration(ServiceRegistration registration) {
			serviceRegistration = registration;
		}

		/**
		 * 
		 * @return the service registration of this object
		 */
		public ServiceRegistration getServiceRegistration() {
			return serviceRegistration;

		}

		/**
		 * this method returns the component declaration for this service
		 * 
		 * @return ComponentDeclaration an instance of the component declaration
		 */
		public ComponentDeclaration getComponentDeclaration() {
			return componentDeclaration;
		}

		/**
		 * this returns the component context for the component
		 * 
		 * @return ComponentContext the context of this component
		 */
		public ComponentContext getComponentContext() {
			return componentContext;
		}

		/**
		 * not used here
		 */
		public Vector getComponentContexts() {
			return null;
		}

		/**
		 * binds a reference/dependency to this component
		 */
		public void binReference(ServiceReference reference) {
			boundedReferences.add(reference);

		}

		/**
		 * checks if this component is bounded to a specific reference
		 * 
		 * @return boolean true if the component is bounded to the reference
		 *         otherwise false
		 */
		public boolean isBoundedTo(ServiceReference reference) {

			return boundedReferences.contains(reference);
		}

		/**
		 * returns all bounded references
		 * 
		 * @return Vector with all boundend references
		 */
		public Vector getAllBoundedReferences() {
			return boundedReferences;
		}

		/**
		 * binds a reference to this component
		 * 
		 * @param ServiceReference
		 *            the reference to be bounded
		 */
		public void bindReference(ServiceReference reference) {
			boundedReferences.add(reference);
		}

		/**
		 * unbinds a reference from this component
		 */
		public void unBindReference(ServiceReference reference) {
			boundedReferences.remove(reference);

		}
	}

	/**
	 * This class represents an immediat component it doesn't have any services
	 * is normally Activated at once, but doesn't have to be. Anyways this
	 * instance
	 * 
	 * @author Magnus Klack
	 */
	private class ImmediateComponent implements DeclarativeComponent {
		/** variable holding the componentDeclaration */
		private ComponentDeclaration componentDeclaration;

		/** variable holding the componentInstance */
		private ComponentContext componentContext;

		/** variable holding the service reference */
		private ServiceReference serviceReference;

		/** variable holding the bounded references * */
		private Vector boundedReferences = new Vector();

		public ImmediateComponent(ComponentDeclaration declaration) {
			componentDeclaration = declaration;

			try {

				/* increase the componentCounter */
				componentCounter++;

				Dictionary properties = componentDeclaration
						.getDeclaredProperties();

				/* put the component Name into the properties table */
				properties.put(ComponentConstants.COMPONENT_NAME,
						componentDeclaration.getComponentName());

				/* put the component id into it */
				properties.put(ComponentConstants.COMPONENT_ID, new Long(
						componentCounter));

				/* put the XML file here */
				properties.put(ComponentConstants.SERVICE_COMPONENT,
						componentDeclaration.getXmlFile());

				/*
				 * create the context in this case a request will come frome the
				 * bundle declaring the component
				 */
				componentContext = createComponentContext(componentDeclaration,
						componentCounter, null, properties, null,
						componentDeclaration.getDeclaringBundle());

				try {
					/* bind the references */
					invokeReferences(this, componentContext
							.getComponentInstance(), true);

					try {
						/* activate the component */
						activateInstance(componentContext);
					} catch (ComponentException e) {
						System.err
								.println("error when activate the component instanne in ImmediateComponent:"
										+ e);
					}

				} catch (ComponentException e) {
					System.err
							.println("error when binding references in ImmediateComponent:"
									+ e);
				}

			} catch (ComponentException e) {
				System.err
						.println("error when creating ComponentContext in ImmediatComponent:"
								+ e);
			}

		}

		public ComponentDeclaration getComponentDeclaration() {
			return componentDeclaration;
		}

		public ComponentContext getComponentContext() {
			return componentContext;
		}

		/**
		 * not used here
		 */
		public ServiceRegistration getServiceRegistration() {
			return null;
		}

		/**
		 * not used here
		 */
		public void setServiceRegistration(ServiceRegistration registration) {
		}

		/**
		 * not used here
		 */
		public void setServiceReference(ServiceReference reference) {
		}

		/**
		 * not used here
		 */
		public ServiceReference getServiceReference() {
			return null;
		}

		/**
		 * not used here
		 */
		public Vector getComponentContexts() {
			return null;
		}

		/**
		 * binds a reference/dependency to this component
		 */
		public void binReference(ServiceReference reference) {
			boundedReferences.add(reference);

		}

		/**
		 * checks if this component is bounded to a specific reference
		 * 
		 * @return boolean true if the component is bounded to the reference
		 *         otherwise false
		 */
		public boolean isBoundedTo(ServiceReference reference) {

			return boundedReferences.contains(reference);
		}

		/**
		 * returns all bounded references
		 * 
		 * @return Vector with all boundend references
		 */
		public Vector getAllBoundedReferences() {
			return boundedReferences;
		}

		/**
		 * binds a reference to this component
		 * 
		 * @param ServiceReference
		 *            the reference to be bounded
		 */
		public void bindReference(ServiceReference reference) {
			boundedReferences.add(reference);
		}

		/**
		 * unbinds a reference from this component
		 */
		public void unBindReference(ServiceReference reference) {
			boundedReferences.remove(reference);

		}
	}

	/**
	 * This class is used by the SCR to register servicefactories in advance for
	 * any component declaration declaring the attribute servicefactory. This
	 * class should not be instaniated if the declaration isn't enable and
	 * satisfied.
	 * 
	 * @author Magnus Klack
	 */
	private class CustomComponentServiceFactory implements ServiceFactory,
			DeclarativeComponent {
		/** variable holding the component declaration */
		private ComponentDeclaration componentDeclaration;

		/** string representing the interface name */
		private String[] interfaceNames;

		/** hashtable holding configurations */
		private Hashtable configurations = new Hashtable();

		/** variable holding the component id */
		private long componentID;

		/** variable holding the service reference */
		private ServiceReference serviceReference;

		/** variable keeping all component context created by this factory */
		private Vector componentContexts = new Vector();

		/** variable holding the Service registration of the object */
		private ServiceRegistration serviceRegistration;

		/** variable holding the bounded references * */
		private Vector boundedReferences = new Vector();

		/**
		 * Constructor for the custom service factory used by the
		 * registerCustomServiceFactory method
		 * 
		 * @param serviceInterface
		 *            the string representation of the interface
		 * @param declaration
		 *            the componentDeclaration
		 */
		public CustomComponentServiceFactory(String[] serviceInterfaces,
				ComponentDeclaration declaration) {

			/* increase the componentCounter */
			componentCounter++;
			/* assign the componentCounter value */
			componentID = componentCounter;

			/* assign the component declaration */
			componentDeclaration = declaration;
			/* assign the interface name */
			interfaceNames = serviceInterfaces;

		}

		/**
		 * This will be called when a new service is required from an external
		 * bundle
		 * 
		 * @param bundle
		 *            the requesting bundle
		 * @param registration
		 *            the serviceRegistration
		 */
		public synchronized Object getService(Bundle bundle,
				ServiceRegistration registration) {

			/* print that getService is called */
			System.out
					.println("********** getService() is called in CustomComponentServiceFactory **************");
			try {
				/* check if the bundle already have a configuration */
				Object componentConfiguration = configurations.get(bundle);

				if (componentConfiguration == null) {

					try {

						Dictionary properties = componentDeclaration
								.getDeclaredProperties();

						/* put the component Name into the properties table */
						properties.put(ComponentConstants.COMPONENT_NAME,
								componentDeclaration.getComponentName());

						/* put the component id into it */
						properties.put(ComponentConstants.COMPONENT_ID,
								new Long(componentCounter));

						/* put the XML file here */
						properties.put(ComponentConstants.SERVICE_COMPONENT,
								componentDeclaration.getXmlFile());

						/*
						 * create a new componentContext request will come from
						 * the same bundle declaring the component
						 */
						ComponentContext componentContext = createComponentContext(
								componentDeclaration, componentID,
								serviceRegistration, properties, bundle,
								componentDeclaration.getDeclaringBundle());

						try {

							/* bind the references */
							invokeReferences(this, componentContext
									.getComponentInstance(), true);

							try {
								/* activate the componentInstance */
								activateInstance(componentContext);

								/* add it to the collection of contexts */
								componentContexts.add(componentContext);

								/* return the instance of the component */
								return componentContext.getComponentInstance()
										.getInstance();

							} catch (ComponentException e) {
								/* print the error */
								System.err
										.println("error when activating instance in CustomComponentServiceFactory:"
												+ e);
							}

						} catch (ComponentException e) {
							/* print the error */
							System.err
									.println("error when binding references in CustomComponentServiceFactory:"
											+ e);
						}

					} catch (ComponentException e) {
						/* print the error */
						System.err
								.println("error when creating component context in CustomComponentServiceFactory:"
										+ e);
					}

					/* put the configuration into the table */
					configurations.put(bundle, componentConfiguration);

				} else {
					/* return the old instance */
					return ((ComponentContext) componentConfiguration)
							.getComponentInstance().getInstance();

				}

			} catch (Exception e) {
				throw new ComponentException(
						"unknown error in CustomServiceFactory.getService(..):\n "
								+ e.getMessage(), e.getCause());

			}

			return null;
		}

		/**
		 * This method will be called when a bundle no longer needs a service
		 * produced by this factory class
		 * 
		 * @param bundle
		 *            the bundle requesting this method
		 * @param registration
		 *            the service registration
		 * @param the
		 *            service object
		 */
		public synchronized void ungetService(Bundle bundle,
				ServiceRegistration registration, Object service) {
			System.out
					.println("********** ungetService() is called in CustomServiceFactory **************");
			/* remove the bundle from the configuration table */
			configurations.remove(bundle);
			/* set the service object to null */
			service = null;

		}

		/**
		 * sets the service reference to this configuration
		 * 
		 * @param reference
		 *            the service reference
		 */
		public void setServiceReference(ServiceReference reference) {
			serviceReference = reference;
		}

		/**
		 * returns the service reference used by this component configuration
		 * 
		 * @return ServiceReference the service reference used by this instance
		 */
		public ServiceReference getServiceReference() {
			return serviceReference;
		}

		/**
		 * sets the service registration of this object
		 * 
		 * @param registration
		 *            the service registration
		 */
		public void setServiceRegistration(ServiceRegistration registration) {
			serviceRegistration = registration;

		}

		/**
		 * returns the service registration used by this object
		 * 
		 * @return ServiceRegistration the service registration this bundle uses
		 */
		public ServiceRegistration getServiceRegistration() {
			return serviceRegistration;
		}

		/**
		 * this method returns the component declaration for the service
		 * 
		 * @return the component declaration
		 */
		public ComponentDeclaration getComponentDeclaration() {
			return componentDeclaration;
		}

		/**
		 * this method returns all created componentContexts created by this
		 * service factory
		 * 
		 * @return Vector containing all componentContexts
		 */
		public Vector getComponentContexts() {
			return componentContexts;
		}

		/**
		 * not used here
		 */
		public ComponentContext getComponentContext() {
			return null;
		}

		/**
		 * binds a reference/dependency to this component
		 */
		public void binReference(ServiceReference reference) {
			boundedReferences.add(reference);

		}

		/**
		 * checks if this component is bounded to a specific reference
		 * 
		 * @return boolean true if the component is bounded to the reference
		 *         otherwise false
		 */
		public boolean isBoundedTo(ServiceReference reference) {

			return boundedReferences.contains(reference);
		}

		/**
		 * returns all bounded references
		 * 
		 * @return Vector with all boundend references
		 */
		public Vector getAllBoundedReferences() {
			return boundedReferences;
		}

		/**
		 * binds a reference to this component
		 * 
		 * @param ServiceReference
		 *            the reference to be bounded
		 */
		public void bindReference(ServiceReference reference) {
			boundedReferences.add(reference);
		}

		/**
		 * unbinds a reference from this component
		 */
		public void unBindReference(ServiceReference reference) {
			boundedReferences.remove(reference);

		}
	}

	/**
	 * This class creates new components when newInstance is called from an
	 * external source. It will also register services in advance for the
	 * declared component
	 * 
	 * @author Magnus Klack
	 */
	private class CustomComponentFactory implements ComponentFactory,
			DeclarativeComponent {
		/** variable holding the declaration * */
		private ComponentDeclaration componentDeclaration;

		/** variable holding the service reference */
		private ServiceReference serviceReference;

		/** variable holding the service registration */
		private ServiceRegistration serviceRegistration;

		/** variable holding the bounded references * */
		private Vector boundedReferences = new Vector();

		/**
		 * constructor for CustomComponentFactory
		 * 
		 * @param declaration
		 *            the declaration
		 */
		public CustomComponentFactory(ComponentDeclaration declaration) {
			/* assign the declaration */
			componentDeclaration = declaration;

		}

		/**
		 * this method will create a new Component of the type which this
		 * instance's component declaration declares. It can be an delayed
		 * component or an immediate component
		 * 
		 * @param Dictionary
		 *            the properties for the new component
		 */
		public ComponentInstance newInstance(Dictionary properties)
				throws ComponentException {

			System.out
					.println("**************** newInstance() is called in CustomComponentFactory ************");

			if (isSatisfied(componentDeclaration)) {
				/* increase the component Counter */
				componentCounter++;

				Dictionary standardProperties = componentDeclaration
						.getDeclaredProperties();

				/* put the component Name into the properties table */
				standardProperties.put(ComponentConstants.COMPONENT_NAME,
						componentDeclaration.getComponentName());

				/* put the component id into it */
				standardProperties.put(ComponentConstants.COMPONENT_ID,
						new Long(componentCounter));

				/* put the XML file here */
				standardProperties.put(ComponentConstants.SERVICE_COMPONENT,
						componentDeclaration.getXmlFile());

				//				/* put the component factory into it */
				//				standardProperties.put(ComponentConstants.COMPONENT_FACTORY,
				//						componentDeclaration.getFactory());

				/*
				 * merge the standard properties usally created with the given
				 * properties
				 */
				Dictionary mergedProperties = componentDeclaration
						.mergeProperties(properties, standardProperties);

				/*
				 * create the context requests will come from the same bundle
				 * declaring the bundle use the declaring bundle in the
				 * component declaration.
				 */
				ComponentContext componentContext = createComponentContext(
						componentDeclaration, componentCounter,
						serviceRegistration, mergedProperties, null,
						componentDeclaration.getDeclaringBundle());

				try {
					/* bind the references declared in component declaration */
					invokeReferences(this, componentContext
							.getComponentInstance(), true);

					try {
						/* activate the component */
						activateInstance(componentContext);

						if (componentDeclaration.getServiceInfo().size() > 0) {

							/* get all services */
							ArrayList services = componentDeclaration
									.getServiceInfo();

							/*
							 * create an iterator to iterate through the
							 * services
							 */
							Iterator iteratorServices = services.iterator();
							/* create the service component */
							CustomDelayedService delayedService = null;
							/* vector holding the intefaces */
							Vector vectorInterfaces = new Vector();

							/* iterate */
							while (iteratorServices.hasNext()) {
								/* create a service info */
								ComponentServiceInfo serviceInfo = (ComponentServiceInfo) iteratorServices
										.next();
								/* get all the interfaces */
								ArrayList interfaces = serviceInfo
										.getComponentInterfaces();
								/* create an iterator variable */
								Iterator iteratorInterfaces = interfaces
										.iterator();

								/* iterate throgh all the services */
								while (iteratorInterfaces.hasNext()) {
									/* get the interface */
									String serviceInterface = (String) iteratorInterfaces
											.next();
									vectorInterfaces.add(serviceInterface);
								}
							}

							/* array holding all service interfaces */
							String[] allInterfaces = new String[vectorInterfaces
									.size()];

							/* copy the vector values to the String array */
							for (int i = 0; i < vectorInterfaces.size(); i++) {
								allInterfaces[i] = (String) vectorInterfaces
										.get(i);
							}

							try {

								/* print that we register a new service */
								System.out
										.println("************* register component factory service "
												+ "for new component instance with name"
												+ componentDeclaration
														.getComponentName()
												+ " the service is:"
												+ allInterfaces
												+ " ******************");

								/* clone the componentDeclaration */
								ComponentDeclaration newDeclaration = (ComponentDeclaration) componentDeclaration
										.getClone();

								/* this is not a factory */
								newDeclaration.setFactory(null);

								/* create a new delayed service */
								delayedService = new CustomDelayedService(
										allInterfaces, newDeclaration);

								/* register the service */
								ServiceRegistration registration = componentDeclaration
										.getDeclaringBundle()
										.getBundleContext().registerService(
												allInterfaces, delayedService,
												mergedProperties);

								/* set the service reference */
								delayedService.setServiceReference(registration
										.getReference());

								/* set the registration */
								delayedService
										.setServiceRegistration(registration);

								/* add delayed service component to the vector */
								activeComponents.add(delayedService);

							} catch (Exception e) {
								/* print the error */
								System.err
										.println("error when register service in component factory:"
												+ e);
								/* throw the exception */
								throw new ComponentException(
										"error when register service in component factory:",
										e.getCause());
							}

						} else {
							/* this is an immediate component */
							ImmediateComponent immediateComponent = new ImmediateComponent(
									componentDeclaration);

							/* add the component to the vector */
							activeComponents.add(immediateComponent);

						}

						/* return the component instance */
						return componentContext.getComponentInstance();

					} catch (ComponentException e) {
						/* print the error */
						System.err
								.println("error when activating instance in CustomComponentFactory"
										+ e);
					}

				} catch (ComponentException e) {
					/* print the error */
					System.err
							.println("error when binding references in CustomComponentFactory"
									+ e);
				}

			} else {
				/* print the error */
				System.err.println("component factory is not satisfied");
				throw new ComponentException(
						"Component factory is not satisfied");

			}

			return null;

		}

		/**
		 * @param reference
		 */
		public void setServiceReference(ServiceReference reference) {
			serviceReference = reference;

		}

		/**
		 * @return the service reference of this component factory
		 */
		public ServiceReference getServiceReference() {
			return serviceReference;
		}

		/**
		 * Sets the service registration for this component factory
		 * 
		 * @param registration
		 */
		public void setServiceRegistration(ServiceRegistration registration) {
			serviceRegistration = registration;
		}

		/**
		 * return the service registration
		 * 
		 * @return ServiceRegistration used by this object
		 */
		public ServiceRegistration getServiceRegistration() {
			return serviceRegistration;
		}

		/**
		 * this return the component delcaration
		 * 
		 * @return ComponentDeclaration
		 */
		public ComponentDeclaration getComponentDeclaration() {
			return componentDeclaration;
		}

		/**
		 * not used here
		 */
		public Vector getComponentContexts() {
			return null;
		}

		/**
		 * not used here
		 */
		public ComponentContext getComponentContext() {
			return null;
		}

		/**
		 * binds a reference/dependency to this component
		 */
		public void binReference(ServiceReference reference) {
			boundedReferences.add(reference);

		}

		/**
		 * checks if this component is bounded to a specific reference
		 * 
		 * @return boolean true if the component is bounded to the reference
		 *         otherwise false
		 */
		public boolean isBoundedTo(ServiceReference reference) {

			return boundedReferences.contains(reference);
		}

		/**
		 * returns all bounded references
		 * 
		 * @return Vector with all boundend references
		 */
		public Vector getAllBoundedReferences() {
			return boundedReferences;
		}

		/**
		 * binds a reference to this component
		 * 
		 * @param ServiceReference
		 *            the reference to be bounded
		 */
		public void bindReference(ServiceReference reference) {
			boundedReferences.add(reference);
		}

		/**
		 * unbinds a reference from this component
		 */
		public void unBindReference(ServiceReference reference) {
			boundedReferences.remove(reference);

		}

	}

	/**
	 * interface for wrapping components and its internal configuration
	 * 
	 * @author Magnus Klack
	 */
	private interface DeclarativeComponent {
		/** gets the service registration */
		public ServiceRegistration getServiceRegistration();

		/** sets the service registration */
		public void setServiceRegistration(ServiceRegistration registration);

		/** returns the component declaration for this component */
		public ComponentDeclaration getComponentDeclaration();

		/** set the servicereference */
		public void setServiceReference(ServiceReference reference);

		/** get the service reference for a specifice component */
		public ServiceReference getServiceReference();

		/** use this for Component Service Factories */
		public Vector getComponentContexts();

		/** used for delayed and immediate components */
		public ComponentContext getComponentContext();

		/** binds a new reference to this component */
		public void bindReference(ServiceReference reference);

		/** check if a component is bounded to a reference */
		public boolean isBoundedTo(ServiceReference reference);

		/** gets all bounded reference */
		public Vector getAllBoundedReferences();

		/** unbinds a reference to this component */
		public void unBindReference(ServiceReference reference);

	}

}