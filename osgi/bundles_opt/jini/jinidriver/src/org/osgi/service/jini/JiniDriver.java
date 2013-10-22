/*
 * Copyright (c) 2002 by ProSyst Software AG.,
 * url: http://www.prosyst.com
 * All rights reserved.
 *
 * This source code is owned by ProSyst Software AG.
 *
 * These materials have been contributed  to the Open Services Gateway
 * Initiative (OSGi) as “MEMBER LICENSED MATERIALS” as defined in,
 * and subject to the terms of, the OSGi Member Agreement by and
 * between OSGi and ProSyst Software AG, specifically including but
 * not limited to, the license rights and warranty disclaimers as set
 * forth in Sections 3.2 and 12.1 thereof, and the applicable Statement
 * of Work.
 *
 * All company, brand and product names contained within this document
 * may be trademarks that are the sole property of the respective owners.
 *
 * The above notice must be included on all copies of this document
 * that are made.
 *
 * $Header: /cvshome/repository/org/osgi/service/jini/JiniDriver.java,v 1.13 2003/03/07 08:30:43 pkriens Exp $
 * $Revision: 1.13 $
 */


package org.osgi.service.jini;

/**
 * A basic interface for a Jini Driver.<p>
 * This Driver acts as a bridge between a Jini network (community)
 * and an OSGi framework. Using this driver, OSGi services
 * can be exported to the Jini network, and Jini services from the
 * Jini network can be imported into the OSGi framework.
 * This results in two possible transformations: Jini-to-OSGi and
 * OSGi-to-Jini. The Jini Driver is responsible for these transformations.<p>
 * An OSGi service is a Jini service if it is registered
 * in the framework with the specified properties.<p>
 * <p>In OSGi-to-Jini transformation, the driver registers OSGi services as
 * Jini services in the discovered LUS.<p>In Jini-to-OSGi transformation
 * it registers with the framework all discovered LUS and services in the LUS
 * matching the given template.<p>
 * The Jini Driver can be configured, through a set of properties, to
 * export/import Jini Services.<p>
 * The properties <tt>DEVICE_CATEGORY</tt>, <tt>EXPORT</tt>,
 * <tt>LUS_EXPORT_GROUPS</tt>, <tt>SERVICE_ID</tt>,
 * and <tt>ENTRIES</tt> are service
 * register properties for particular Jini Service (imported or exported).<p>
 *
 * The properties <tt>CM_LUS_IMPORT_GROUPS</tt> and <tt>CM_LUS_EXPORT_GROUPS</tt> are for
 * configuration of the Jini Driver. These properties are kept in
 * the Configuration Management Service defined by OSGi.
 */

public interface JiniDriver {

  /**
   * Constant for the value of the service property
   * <tt>DEVICE_CATEGORY</tt> used by all Jini
   * services.
   *<p><b>Value:</b>
   *   <tt>jini</tt></p>
   * @see org.osgi.service.device.Constants#DEVICE_CATEGORY
   **/
  public static final String DEVICE_CATEGORY = "jini";

	/**
	 * The <tt>Export</tt> service property is a hint that marks an OSGi service
	 * to be picked up and exported by the Jini Driver in the Jini network.
	 * Imported services must not have this property set.
	 * <p>
	 * The property has no value. The name of the property is <tt>jini.export</tt></p>
	 **/
  public static final String EXPORT = "jini.export";

  /**
   * Optional service property, which should contain a string array of
   * the LUS groups that are of interest to the OSGi service.
   * This overrides the property <tt>CM_LUS_EXPORT_GROUPS</tt> of the Jini Driver.
   * If the value of this property is not defined, <tt>CM_LUS_EXPORT_GROUPS</tt>
   * will be used. The name of the property is <tt>jini.lus.export.groups</tt>.
   */

  public static final String LUS_EXPORT_GROUPS = "jini.lus.export.groups";

  /**
   * Optional service property, which should contain a string representation of the Jini service ID. It is used
   * by the Jini Driver when exporting framework service. The driver
   * automatically fills the values of this property
   * when importing the Jini service. The name of the property is <tt>jini.service.id</tt>.
   */

  public static final String SERVICE_ID = "jini.service.id";


  /**
   * Optional service property, which should be an {@link net.jini.core.entry.Entry} array, holding
   * the attributes set of the framework service that represents Jini
   * proxy in the registration with a LUS.
   * The name of the property is <tt>jini.entries</tt>.
   * @see net.jini.core.entry.Entry
   */

  public static final String ENTRIES = "jini.entries";

  /**
   * Optional service property, which should be a string array, containing
   * the groups of LUS, that the driver is interested in, when importing
   * Jini services. The driver discovers only the LUS members of
   * at least one of these groups. It discovers all if the property is
   * null or the property is not defined, and does not perform discovery if
   * the length of the array is zero.
   * If LUS are discovered, which after changing the value of this
   * property are not members of the groups, all registered services from
   * them are unregistered. The name of the property is <tt>jini.lus.import.groups</tt>.
   */

  public static final String CM_LUS_IMPORT_GROUPS = "jini.lus.import.groups";

  /**
   * Optional service property, which should be a string array, containing
   * the LUS groups that the driver is interested in, when exporting
   * framework services to the Jini network. The driver discovers only
   * the LUS, which are members of at least one of these groups. It discovers
   * all if the property is null or the property is not defined, and does not
   * perform discovery if the length of the array is 0. If Jini Lookup Services are
   * discovered, which after changing the value of this
   * property are not members of the groups, the registration of all
   * Jini services from the framework, which are registered with them, is
   * cancelled. The name of the property is <tt>jini.lus.export.groups</tt>.
   */

  public static final String CM_LUS_EXPORT_GROUPS = "jini.lus.export.groups";

/**
   * The Jini Driver is defined as a Service Factory. For every bundle a different set of Service Templates is maintained.
   * This method sets a new set of ServiceTemplates (<tt>net.jini.core.lookup.ServiceTemplate</tt>s) that
   * are used for searching and registering services in the discovered LUS.
   * A service registered in a LUS will be registered in framework if it matches at least
   * one of the templates in one of the sets.<p>
   * The <tt>ServiceTemplate(null, null, null)</tt> matches all services.
   *
   * @param template template to be added.
   **/
  public void setServiceTemplates(net.jini.core.lookup.ServiceTemplate [] template);

  /**
   * Gets the current set of templates that is used for searching and registering
   * services registered in discovered LUS. A service, registered in a LUS, will be
   * registered in the framework if it matches at least one of the templates in this set
   *
   * @return an array containing templates or null if the set of templates is empty.
   **/
  public net.jini.core.lookup.ServiceTemplate[] getServiceTemplates();

}