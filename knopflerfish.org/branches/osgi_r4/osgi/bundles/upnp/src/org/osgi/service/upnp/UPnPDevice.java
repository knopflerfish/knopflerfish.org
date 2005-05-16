/*
 * $Header: /cvshome/repository/org/osgi/service/upnp/UPnPDevice.java,v 1.11 2002/09/23 15:41:23 hargrave Exp $
 *
 * Copyright (c) The Open Services Gateway Initiative (2002).
 * All Rights Reserved.
 *
 * Implementation of certain elements of the Open Services Gateway Initiative
 * (OSGI) Specification may be subject to third party intellectual property
 * rights, including without limitation, patent rights (such a third party may
 * or may not be a member of OSGi). OSGi is not responsible and shall not be
 * held responsible in any manner for identifying or failing to identify any or
 * all such third party intellectual property rights.
 *
 * This document and the information contained herein are provided on an "AS
 * IS" basis and OSGI DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO ANY WARRANTY THAT THE USE OF THE INFORMATION HEREIN WILL
 * NOT INFRINGE ANY RIGHTS AND ANY IMPLIED WARRANTIES OF MERCHANTABILITY OR
 * FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT WILL OSGI BE LIABLE FOR ANY
 * LOSS OF PROFITS, LOSS OF BUSINESS, LOSS OF USE OF DATA, INTERRUPTION OF
 * BUSINESS, OR FOR DIRECT, INDIRECT, SPECIAL OR EXEMPLARY, INCIDENTIAL,
 * PUNITIVE OR CONSEQUENTIAL DAMAGES OF ANY KIND IN CONNECTION WITH THIS
 * DOCUMENT OR THE INFORMATION CONTAINED HEREIN, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH LOSS OR DAMAGE.
 *
 * All Company, brand and product names may be trademarks that are the sole
 * property of their respective owners. All rights reserved.
 */

package org.osgi.service.upnp;

import java.util.Dictionary;

/**
 * Represents a UPnP device.
 *
 * For each UPnP root and embedded device, an object
 * is registered with the framework under the <tt>UPnPDevice</tt>
 * interface.
 * <p>
 * The relationship between a root device and its embedded
 * devices can be deduced using the
 * <tt>UPnPDevice.CHILDREN_UDN</tt> and
 * <tt>UPnPDevice.PARENT_UDN</tt>
 * service registration properties.
 * <p>
 * The values of the UPnP property names are defined by the
 * UPnP Forum.
 * <p>
 * All values of the UPnP properties are obtained from the device
 * using the device's default locale.<p>
 * If an application wants to query for a set of localized property
 * values, it has to use the method
 * <tt>UPnPDevice.getDescriptions(String locale)</tt>.
 *
 **/

public interface UPnPDevice {

    /*
     * Constants for the UPnP device match scale.
     */

    /**
     * Constant for the UPnP device match scale, indicating
     * a generic match for the device. Value is 1.
     **/
    int MATCH_GENERIC                            = 1;

    /**
     * Constant for the UPnP device match scale, indicating
     * a match with the device type. Value is 3.
     **/
    int MATCH_TYPE                               = 3;

    /**
     * Constant for the UPnP device match scale, indicating
     * a match with the device model. Value is 7.
     **/
    int MATCH_MANUFACTURER_MODEL                 = 7;

    /**
     * Constant for the UPnP device match scale, indicating
     * a match with the device revision. Value is 15.
     **/
    int MATCH_MANUFACTURER_MODEL_REVISION        = 15;

    /**
     * Constant for the UPnP device match scale, indicating
     * a match with the device revision and the serial number.
     * Value is 31.
     **/
    int MATCH_MANUFACTURER_MODEL_REVISION_SERIAL = 31;

    /**
     * Constant for the value of the service property
     * <tt>DEVICE_CATEGORY</tt> used for
     * all UPnP devices. Value is "UPnP".
	 * @see org.osgi.service.device.Constants#DEVICE_CATEGORY
     **/
    String DEVICE_CATEGORY        = "UPnP";


    /**
     * The <tt>UPnP.export</tt> service property is a hint that marks a device
     * to be picked up and exported by the UPnP Service.
     * Imported devices do not have this property set.
     * The registered property requires no value.<p>The UPNP_EXPORT string is "UPnP.export".
     **/
    String UPNP_EXPORT            = "UPnP.export";



    /**
     * Property key for the Unique Device Name (UDN) property.
     * It is the unique identifier of an instance of a
     * <tt>UPnPDevice</tt>.
     * The value of the property is a <tt>String</tt> object of
     * the Device UDN. Value of the key is "UPnP.device.UDN". This property must
     * be set.
     **/
    String UDN               = "UPnP.device.UDN";

    /**
     * Property key for the Unique Device ID property.
     * This property is an alias to <tt>UPnPDevice.UDN</tt>.
     * It is merely provided for reasons of symmetry with the
     * <tt>UPnPService.ID</tt> property.
     * The value of the property is a <tt>String</tt> object of
     * the Device UDN. The value of the key is "UPnP.device.UDN".
     **/
    String ID               = UDN;


    /**
     * Property key for the UPnP Device Type property. Some standard
     * property values are defined by the Universal Plug and Play
     * Forum. The type string also includes a version number as defined
     * in the UPnP specification. This property must be set.<p>
     * For standard devices defined by a UPnP Forum
     * working committee, this must consist of the following components
     * in the given order separated by colons:
     * <ul>
     * <li><tt>urn</tt></li>
     * <li>schemas-upnp-org</li>
     * <li><tt>device</tt></li>
     * <li>a device type suffix</li>
     * <li>an integer device version</li>
     * </ul>
     * For non-standard devices specified by UPnP vendors following
     * components must be specified in the given order separated by colons:
     * <ul>
     * <li><tt>urn</tt></li>
     * <li>an ICANN domain name owned by the vendor</li>
     * <li><tt>device</tt></li>
     * <li>a device type suffix</li>
     * <li>an integer device version</li>
     * </ul>
     * <p>
     * To allow for backward compatibility the UPnP driver must
     * automatically generate additional Device Type property entries
     * for smaller versions than the current one. If for example a device
     * announces its type as version 3, then properties for versions 2
     * and 1 must be automatically generated.
     * <p>
     * In the case of exporting a UPnPDevice, the highest available version
     * must be announced on the network.
     * <p>
     * Syntax Example: <tt>urn:schemas-upnp-org:device:deviceType:v</tt>
     * <p>The value is "UPnP.device.type".
     **/

    String TYPE       = "UPnP.device.type";

    /**
     * Mandatory property key for the device manufacturer's property. The
     * property value holds a String representation of the
     * device manufacturer's name. Value is "UPnP.device.manufacturer".
     **/
    String MANUFACTURER      = "UPnP.device.manufacturer";

    /**
     * Mandatory property key for the device model name. The property value
     * holds a <tt>String</tt> object giving more information about the device model.
     * Value is "UPnP.device.modelName".
     **/

    String MODEL_NAME        = "UPnP.device.modelName";


    /**
     * Mandatory property key for a short user friendly version of the
     * device name. The property value holds a <tt>String</tt> object with the user
     * friendly name of the device. Value is "UPnP.device.friendlyName".
     **/

    String FRIENDLY_NAME     = "UPnP.device.friendlyName";

    /**
     * Optional property key for a URL to the device manufacturers Web site.
     * The value of the property is a <tt>String</tt> object representing the URL.
     * Value is "UPnP.device.manufacturerURL".
     **/
    String MANUFACTURER_URL  = "UPnP.device.manufacturerURL";

    /**
     * Optional (but recommended) property key for a <tt>String</tt> object
     * with a long description of the device for the end user. The value is
     * "UPnP.device.modelDescription".
     **/
    String MODEL_DESCRIPTION = "UPnP.device.modelDescription";

    /**
     * Optional (but recommended) property key for a <tt>String</tt>  class
     * typed property holding the model number of the device. Value is
     * "UPnP.device.modelNumber".
     **/
    String MODEL_NUMBER      = "UPnP.device.modelNumber";

    /**
     * Optional property key for a <tt>String</tt> typed property holding
     * a string representing the URL to the Web site for this model. Value
     * is "UPnP.device.modelURL".
     **/

    String MODEL_URL         = "UPnP.device.modelURL";

    /**
     * Optional (but recommended) property key for a <tt>String</tt> typed property holding
     * the serial number of the device. Value is "UPnP.device.serialNumber".
     **/

    String SERIAL_NUMBER     = "UPnP.device.serialNumber";

    /**
     * Optional property key for a <tt>String</tt> typed property holding
     * the Universal Product Code (UPC) of the device. Value is "UPnP.device.UPC".
     **/
    String UPC               = "UPnP.device.UPC";


    /**
     * Optional (but recommended) property key for a <tt>String</tt> typed property holding
     * a string representing the URL to a device representation Web page. Value is
     * "UPnP.presentationURL".
     **/
    String PRESENTATION_URL  = "UPnP.presentationURL";

    /**
     * The property key that must be set for all embedded
     * devices. It contains the UDN of the parent device. The property
     * is not set for root devices. The value is "UPnP.device.parentUDN".
     **/
    String PARENT_UDN        = "UPnP.device.parentUDN";

    /**
     * The property key that must be set for all devices
     * containing other embedded devices.<p> The value is an array of
     * UDNs for each of the device's children (<tt>String[]</tt>). The array contains
     * UDNs for the immediate descendants only. </p>
     *<p>
     * If an embedded device in turn contains embedded devices,
     * the latter are not included in the array.
     *</p>
     * The UPnP Specification does not encourage more than two levels of
     * nesting.
     *<p>
     * The property is not set if the device does not contain embedded
     * devices.
     *<p>
     * The property is of type <tt>String[]</tt>. Value is
	 * "UPnP.device.childrenUDN"
     **/
    String CHILDREN_UDN      = "UPnP.device.childrenUDN";



    /**
     * Locates a specific service by its service id.
     *
     * @param serviceId The service id
     * @return The requested service or null if not found.
     **/
    UPnPService getService(String serviceId);

    /**
     * Lists all services provided by this device.
     *
     * @return Array of services or <tt>null</tt> if no services are available.
     **/
    UPnPService[] getServices();

    /**
     * Lists all icons for this device in a given locale.
     *
     * The UPnP specification allows a device to present different icons
     * based on the client's locale.
     *
     * @param locale A language tag as defined by RFC 1766 and maintained
     *               by ISO 639. Examples include "<tt>de</tt>",
     *               "<tt>en</tt>" or "<tt>en-US</tt>".
     *               The default locale of the device is specified by passing a <tt>null</tt>
     *               argument.
     *
     * @return Array of icons or null if no icons are available.
     **/
    UPnPIcon[] getIcons(String locale);

    /**
     * Get a set of localized UPnP properties.
     *
     * The UPnP specification allows a device to present different device properties
     * based on the client's locale.
     * The properties used to register the UPnPDevice service in the OSGi
     * registry are based on the device's default locale.
     * To obtain a localized set of the properties, an application can use
     * this method.
     * <p>
     * Not all properties might be available in all locales.
     * This method does <b>not</b> substitute missing properties with their
     * default locale versions.
     * <p>
     * @param locale A language tag as defined by RFC 1766 and maintained
     *               by ISO 639. Examples include "<tt>de</tt>",
     *               "<tt>en</tt>" or "<tt>en-US</tt>".
     *               The default locale of the device is specified by passing a <tt>null</tt>
     *               argument.
     * @return Dictionary mapping property name Strings to property value Strings
     *
     **/

    Dictionary getDescriptions(String locale);
}
