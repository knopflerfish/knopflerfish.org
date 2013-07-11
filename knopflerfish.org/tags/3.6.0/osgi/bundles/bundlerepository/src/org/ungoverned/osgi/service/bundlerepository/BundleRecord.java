/*
 * Oscar Bundle Repository
 * Copyright (c) 2004, Richard S. Hall
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *   * Neither the name of the ungoverned.org nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Contact: Richard S. Hall (heavy@ungoverned.org)
 * Contributor(s):
 *
**/
package org.ungoverned.osgi.service.bundlerepository;

import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.*;

/**
 * A simple interface used to hold meta-data about bundles
 * contained in a bundle repository.
**/
public class BundleRecord
{
    public static final String BUNDLE_NAME = "Bundle-Name";
    public static final String BUNDLE_VERSION = "Bundle-Version";
    public static final String BUNDLE_UPDATELOCATION = "Bundle-UpdateLocation";
    public static final String BUNDLE_SOURCEURL = "Bundle-SourceURL";
    public static final String BUNDLE_DOCURL = "Bundle-DocURL";
    public static final String BUNDLE_LICENSEURL = "Bundle-LicenseURL";
    public static final String BUNDLE_DESCRIPTION = "Bundle-Description";
    public static final String BUNDLE_CATEGORY = "Bundle-Category";
    public static final String BUNDLE_VENDOR = "Bundle-Vendor";
    public static final String BUNDLE_CONTACTADDRESS = "Bundle-ContactAddress";
    public static final String BUNDLE_COPYRIGHT = "Bundle-Copyright";
    public static final String BUNDLE_REQUIREDEXECUTIONENVIRONMENT = "Bundle-RequiredExecutionEnvironment";
    public static final String BUNDLE_NATIVECODE = "Bundle-NativeCode";
    public static final String IMPORT_PACKAGE = "Import-Package";
    public static final String EXPORT_PACKAGE = "Export-Package";
    public static final String DYNAMICIMPORT_PACKAGE = "DynamicImport-Package";
    public static final String REQUIRE_SERVICE = "Require-Service";
    public static final String PROVIDE_SERVICE = "Provide-Service";

    private Map m_attrMap = null;
    private Dictionary m_dict = null;

    /**
     * <p>
     * Constructs a bundle record using the values of the supplied
     * map as the meta-data values for the bundle. The supplied map
     * is copied, but its values are not.
     * </p>
     * @param attrMap a map containing attribute-value pairs of meta-data
     *        for a bundle.
    **/
    public BundleRecord(Map attrMap)
    {
        // Create a case insensitive map.
        m_attrMap = new TreeMap(new Comparator() {
            public int compare(Object o1, Object o2)
            {
                return o1.toString().compareToIgnoreCase(o2.toString());
            }
        });
        m_attrMap.putAll(attrMap);
    }

    /**
     * <p>
     * Returns a dictionary object which can be used with
     * <tt>org.osgi.framework.Filter</tt>, for example. The returned
     * dictionary object is a minimum implementation, where only
     * the <tt>get()</tt>, <tt>size()</tt>, and <tt>isEmpty()</tt>
     * methods do anything useful.
     * </p>
     * @return a dictionary object for accessing the bundle record attributes.
    **/
    public synchronized Dictionary getDictionary()
    {
        if (m_dict == null)
        {
            m_dict = new Dictionary() {
                public int size()
                {
                    return m_attrMap.size();
                }

                public boolean isEmpty()
                {
                    return m_attrMap.isEmpty();
                }

                public Enumeration elements()
                {
                    throw new UnsupportedOperationException("Not implemented.");
                }

                public Enumeration keys()
                {
                    throw new UnsupportedOperationException("Not implemented.");
                }

                public Object get(Object key)
                {
                    return m_attrMap.get(key);
                }

                public Object remove(Object key)
                {
                    throw new UnsupportedOperationException("Not implemented.");
                }

                public Object put(Object key, Object value)
                {
                    throw new UnsupportedOperationException("Not implemented.");
                }
            };
        }

        return m_dict;
    }

    /**
     * <p>
     * Returns an array containing all attribute names associated with
     * the bundle record. The return array is a copy and can be freely
     * modified.
     * </p>
     * @return an array containing the attribute names contained in the
     *         bundle record.
    **/
    public String[] getAttributes()
    {
        return (String[]) m_attrMap.keySet().toArray(new String[m_attrMap.size()]);
    }

    /**
     * <p>
     * Returns the value of the specified attribute. If the value is an array,
     * then a copy is returned.
     * </p>
     * @param name the attribute name for which to retrieve its value.
     * @return the value of the specified attribute or <tt>null</tt> if
     *         the specified attribute does not exist.
    **/
    public Object getAttribute(String name)
    {
        Object obj = m_attrMap.get(name);
        // If the value is an array, then make a copy
        // since arrays are mutable.
        if ((obj != null) && obj.getClass().isArray())
        {
            Class clazz = obj.getClass().getComponentType();
            int len = Array.getLength(obj);
            Object copy = Array.newInstance(obj.getClass().getComponentType(), len);
            System.arraycopy(obj, 0, copy, 0, len);
            obj = copy;
        }
        return obj;
    }

    /**
     * <p>
     * Dumps the contents of the bundle record to the specified print stream.
     * </p>
     * @param out the print stream to use for printing.
    **/    
    public void printAttributes(PrintStream out)
    {
        String[] attrs = getAttributes();
        if (attrs != null)
        {
            Arrays.sort(attrs);
            for (int i = 0; (attrs != null) && (i < attrs.length); i++)
            {
                Object obj = getAttribute(attrs[i]);
                if (obj.getClass().isArray())
                {
                    out.println(attrs[i] + ":");
                    for (int j = 0; j < Array.getLength(obj); j++)
                    {
                        out.println("   " + Array.get(obj, j));
                    }
                }
                else
                {
                    out.println(attrs[i] + ": " + getAttribute(attrs[i]));
                }
            }
        }
    }
}
