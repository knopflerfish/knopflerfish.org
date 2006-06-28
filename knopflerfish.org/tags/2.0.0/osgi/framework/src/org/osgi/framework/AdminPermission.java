/*
 * Copyright (c) 2006, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 *
 * - Neither the name of the KNOPFLERFISH project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * @author Philippe Laporte
 * @author Jan Stein
 */

/* 
 * Copyright (c) OSGi Alliance (2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This copyright applies to the comments and public final static String fields only
 */

package org.osgi.framework;



import java.security.*;

import org.knopflerfish.framework.*;

import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Hashtable;


/**
 * Indicates the caller's authority to perform specific privileged
 * administrative operations on or to get sensitive information about a bundle.
 * The actions for this permission are:
 * 
 * <pre>
 *  Action               Methods
 *  class                Bundle.loadClass
 *  execute              Bundle.start
 *                       Bundle.stop
 *                       StartLevel.setBundleStartLevel
 *  extensionLifecycle   BundleContext.installBundle for extension bundles
 *                       Bundle.update for extension bundles
 *                       Bundle.uninstall for extension bundles
 *  lifecycle            BundleContext.installBundle
 *                       Bundle.update
 *                       Bundle.uninstall
 *  listener             BundleContext.addBundleListener for SynchronousBundleListener
 *                       BundleContext.removeBundleListener for SynchronousBundleListener
 *  metadata             Bundle.getHeaders
 *                       Bundle.getLocation
 *  resolve              PackageAdmin.refreshPackages
 *                       PackageAdmin.resolveBundles
 *  resource             Bundle.getResource
 *                       Bundle.getResources
 *                       Bundle.getEntry
 *                       Bundle.getEntryPaths
 *                       Bundle.findEntries
 *                       Bundle resource/entry URL creation
 *  startlevel           StartLevel.setStartLevel
 *                       StartLevel.setInitialBundleStartLevel 
 * </pre>
 * 
 * <p>
 * The special action "*" will represent all actions.
 * <p>
 * The name of this permission is a filter expression. The filter gives access
 * to the following parameters:
 * <ul>
 * <li>signer - A Distinguished Name chain used to sign a bundle. Wildcards in
 * a DN are not matched according to the filter string rules, but according to
 * the rules defined for a DN chain.</li>
 * <li>location - The location of a bundle.</li>
 * <li>id - The bundle ID of the designated bundle.</li>
 * <li>name - The symbolic name of a bundle.</li>
 * </ul>
 * 
 */

public final class AdminPermission extends Permission {

	/**
	 * The action string <code>class</code> (Value is "class").
	 * @since 1.3
	 */
	public final static String			CLASS				= "class";
	/**
	 * The action string <code>execute</code> (Value is "execute").
	 * @since 1.3
	 */
	public final static String			EXECUTE				= "execute";
	/**
	 * The action string <code>extensionLifecycle</code> (Value is
	 * "extensionLifecycle").
	 * @since 1.3
	 */
	public final static String			EXTENSIONLIFECYCLE	= "extensionLifecycle";
	/**
	 * The action string <code>lifecycle</code> (Value is "lifecycle").
	 * @since 1.3
	 */
	public final static String			LIFECYCLE			= "lifecycle";
	/**
	 * The action string <code>listener</code> (Value is "listener").
	 * @since 1.3
	 */
	public final static String			LISTENER			= "listener";
	/**
	 * The action string <code>metadata</code> (Value is "metadata").
	 * @since 1.3
	 */
	public final static String			METADATA			= "metadata";
	/**
	 * The action string <code>resolve</code> (Value is "resolve").
	 * @since 1.3
	 */
	public final static String			RESOLVE				= "resolve";
	/**
	 * The action string <code>resource</code> (Value is "resource").
	 * @since 1.3
	 */
	public final static String			RESOURCE			= "resource";
	/**
	 * The action string <code>startlevel</code> (Value is "startlevel").
	 * @since 1.3
	 */
	public final static String			STARTLEVEL			= "startlevel";

	
	LDAPExpr ldap;
	
	Bundle bundle;
	
        int actionMask /*= 0*/;
	
	private static final String WILDCARD = "*";
	
	private static final int CLASS_BIT               = 1;
	private static final int EXECUTE_BIT             = 2;
	private static final int EXTENSIONLIFECYCLE_BIT  = 4;
	private static final int LIFECYCLE_BIT           = 8;
	private static final int LISTENER_BIT            = 16;
	private static final int METADATA_BIT            = 32;
	private static final int RESOLVE_BIT             = 64;
	private static final int RESOURCE_BIT            = 128;
	private static final int STARTLEVEL_BIT          = 256;
	private static final int ALL_BITS                = CLASS_BIT  | EXECUTE_BIT  | 
	                                                   EXTENSIONLIFECYCLE_BIT  | LIFECYCLE_BIT  | 
	                                                   LISTENER_BIT  | METADATA_BIT  | 
	                                                   RESOLVE_BIT  | RESOURCE_BIT  | STARTLEVEL_BIT ;
	 
	/**
	 * Creates a new <code>AdminPermission</code> object that matches all
	 * bundles and has all actions. Equivalent to AdminPermission("*","*");
	 */
	public AdminPermission() {
		this("*", "*"); //$NON-NLS-1$
	}
	
	/**
	 * Create a new AdminPermission.
	 * 
	 * This constructor must only be used to create a permission that is going
	 * to be checked.
	 * <p>
	 * Examples:
	 * 
	 * <pre>
	 * (signer=\*,o=ACME,c=US)   
	 * (&amp;(signer=\*,o=ACME,c=US)(name=com.acme.*)(location=http://www.acme.com/bundles/*))
	 * (id&gt;=1)
	 * </pre>
	 * 
	 * <p>
	 * When a signer key is used within the filter expression the signer value
	 * must escape the special filter chars ('*', '(', ')').
	 * <p>
	 * Null arguments are equivalent to "*".
	 * 
	 * @param filter A filter expression that can use signer, location, id, and
	 *        name keys. A value of &quot;*&quot; or <code>null</code> matches
	 *        all bundle.
	 * @param actions <code>class</code>, <code>execute</code>,
	 *        <code>extensionLifecycle</code>, <code>lifecycle</code>,
	 *        <code>listener</code>, <code>metadata</code>,
	 *        <code>resolve</code>, <code>resource</code>, or
	 *        <code>startlevel</code>. A value of "*" or <code>null</code>
	 *        indicates all actions
	 */
	public AdminPermission(String filter, String actions) {
		super(filter == null ? (filter = WILDCARD) : filter);
		//TODO implement LDAP extensions for location and signer
		try{
			ldap = new LDAPExpr(filter);
		}
		catch(InvalidSyntaxException e){
			//SPECS what to do?
		}
		parseActions(actions);
                bundle = null;
	}

	/**
	 * Creates a new <code>AdminPermission</code> object to be used by the
	 * code that must check a <code>Permission</code> object.
	 * 
	 * @param bundle A bundle
	 * @param actions <code>class</code>, <code>execute</code>,
	 *        <code>extensionLifecycle</code>, <code>lifecycle</code>,
	 *        <code>listener</code>, <code>metadata</code>,
	 *        <code>resolve</code>, <code>resource</code>,
	 *        <code>startlevel</code>
	 * @since 1.3
	 */
	public AdminPermission(Bundle bundle, String actions) {
		super("BID#" + bundle.getBundleId());
		this.bundle = bundle;
                parseActions(actions);
	}
	
	//package only
	AdminPermission(String filter, int actionMask){
		super(filter);
		this.actionMask = actionMask;
                bundle = null;
	}
	
    //package only
	AdminPermission(Bundle bundle, int actionMask){
		super("BID#" + bundle.getBundleId());
		this.actionMask = actionMask;
	}


	private void parseActions(String actions){
		StringTokenizer st = new StringTokenizer(actions, ",");
		while(st.hasMoreTokens()){
			String tok = st.nextToken();
			if(tok.equals(CLASS)){
				actionMask |= CLASS_BIT ;
			}
			else if(tok.equals(EXECUTE)){
				actionMask |= EXECUTE_BIT ;
			}
			else if(tok.equals(EXTENSIONLIFECYCLE)){
				actionMask |= EXTENSIONLIFECYCLE_BIT ;
			}
			else if(tok.equals(LIFECYCLE)){
				actionMask |= LIFECYCLE_BIT ;
			}
			else if(tok.equals(LISTENER)){
				actionMask |= LISTENER_BIT ;
			}
			else if(tok.equals(METADATA)){
				actionMask |= METADATA_BIT ;
			}
			else if(tok.equals(RESOLVE)){
				actionMask |= RESOLVE_BIT ;
			}
			else if(tok.equals(RESOURCE)){
				actionMask |= RESOURCE_BIT ;
			}
			else if(tok.equals(STARTLEVEL)){
				actionMask |= STARTLEVEL_BIT ;
			}
			else if(tok.equals("*")){
				actionMask = ALL_BITS ;
			}
			else{
				throw new IllegalArgumentException("Unknown action " + tok);
			}
		}	
	}

	/**
	 * Determines the equality of two <code>AdminPermission</code> objects.
	 * 
	 * @param obj The object being compared for equality with this object.
	 * @return <code>true</code> if <code>obj</code> is equivalent to this
	 *         <code>AdminPermission</code>; <code>false</code> otherwise.
	 */
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (!(obj instanceof AdminPermission)) {
			return false;
		}

		AdminPermission ap = (AdminPermission) obj;
		
		if(bundle == null){
			return ((actionMask == ap.actionMask) && getName().equals(ap.getName()));
		}
		else{
			return (actionMask == ap.actionMask  && bundle == ap.bundle);
		}
	}

	/**
	 * Returns the hash code value for this object.
	 * 
	 * @return Hash code value for this object.
	 */
	public int hashCode() {
		if(bundle == null){
			return getName().hashCode() ^ actionMask;
		}
		else{
			return bundle.hashCode() ^ actionMask;
		}	
	}

	/**
	 * Returns the canonical string representation of the
	 * <code>AdminPermission</code> actions.
	 * 
	 * <p>
	 * Always returns present <code>AdminPermission</code> actions in the
	 * following order: <code>class</code>, <code>execute</code>,
	 * <code>extensionLifecycle</code>, <code>lifecycle</code>,
	 * <code>listener</code>, <code>metadata</code>, <code>resolve</code>,
	 * <code>resource</code>, <code>startlevel</code>.
	 * 
	 * @return Canonical string representation of the
	 *         <code>AdminPermission</code> actions.
	 */
	public String getActions() {
		StringBuffer sb = new StringBuffer();
		
		if((actionMask & CLASS_BIT ) != 0){
			sb.append(CLASS);
			sb.append(',');
		}
		if((actionMask & EXECUTE_BIT ) != 0){
			sb.append(EXECUTE);
			sb.append(',');
		}
		if((actionMask & EXTENSIONLIFECYCLE_BIT ) != 0){
			sb.append(EXTENSIONLIFECYCLE);
			sb.append(',');
		}
		if((actionMask & LIFECYCLE_BIT ) != 0){
			sb.append(LIFECYCLE);
			sb.append(',');
		}
		if((actionMask & LISTENER_BIT ) != 0){
			sb.append(LISTENER);
			sb.append(',');
		}
		if((actionMask & METADATA_BIT ) != 0){
			sb.append(METADATA);
			sb.append(',');
		}
		if((actionMask & RESOLVE_BIT ) != 0){
			sb.append(RESOLVE);
			sb.append(',');
		}
		if((actionMask & RESOURCE_BIT ) != 0){
			sb.append(RESOURCE);
			sb.append(',');
		}
		if((actionMask & STARTLEVEL_BIT ) != 0){
			sb.append(STARTLEVEL);
			sb.append(',');
		}
		
		int length = sb.length();
		if(length > 0){
			sb.deleteCharAt(length - 1);
		}
		return sb.toString();
	}

	private boolean match(final AdminPermission ap){
            // TODO, more efficient impl?
            final Hashtable t = new Hashtable();
            AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                  t.put("id", new Long(ap.bundle.getBundleId()));	
                  t.put("location", ap.bundle.getLocation());
                  t.put("name", ap.bundle.getSymbolicName());
                  return null;
                }
              });
	    //SPECS should it be case sensitive or not?
	    return ldap.evaluate(t, false);
	}
	
	/**
	 * Determines if the specified permission is implied by this object. This
	 * method throws an exception if the specified permission was not
	 * constructed with a bundle.
	 * 
	 * <p>
	 * This method returns <code>true</code> if the specified permission is an
	 * AdminPermission AND
	 * <ul>
	 * <li>this object's filter matches the specified permission's bundle ID,
	 * bundle symbolic name, bundle location and bundle signer distinguished
	 * name chain OR</li>
	 * <li>this object's filter is "*"</li>
	 * </ul>
	 * AND this object's actions include all of the specified permission's
	 * actions.
	 * <p>
	 * Special case: if the specified permission was constructed with "*"
	 * filter, then this method returns <code>true</code> if this object's
	 * filter is "*" and this object's actions include all of the specified
	 * permission's actions
	 * 
	 * @param p The permission to interrogate.
	 * 
	 * @return <code>true</code> if the specified permission is implied by
	 *         this object; <code>false</code> otherwise.
	 * @throws RuntimeException if specified permission was not constructed with
	 *         a bundle or "*"
	 */
	public boolean implies(Permission p) {
		if (!(p instanceof AdminPermission)) {
			return false;
		}
		AdminPermission ap = (AdminPermission) p;
		if(ap.bundle == null && !ap.getName().equals(WILDCARD)){
			throw new RuntimeException("permission not contructed with bundle or *");
		}

                if ((actionMask & ap.actionMask) != ap.actionMask) {
                    return false;
                }
		if (bundle != null) { 
                        return bundle == ap.bundle || ap.getName().equals(WILDCARD);
		} else {
                        return getName().equals(WILDCARD) || match(ap);
		}
	}

	/**
	 * Returns a new <code>PermissionCollection</code> object suitable for
	 * storing <code>AdminPermission</code>s.
	 * 
	 * @return A new <code>PermissionCollection</code> object.
	 */
	public PermissionCollection newPermissionCollection() {
		return new AdminPermissionCollection();
	}
}



final class AdminPermissionCollection extends PermissionCollection {
	
	private Hashtable permissions;

	private boolean	addedAll;
	private int allMask /* = 0 */;

	public AdminPermissionCollection() {
		permissions = new Hashtable();
		addedAll = false;
	}

	public void add(Permission permission) {
		if (isReadOnly())
			throw new SecurityException("read only PermissionCollection");
		if (!(permission instanceof AdminPermission))
			throw new IllegalArgumentException("invalid permission: " + permission);

		AdminPermission ap = (AdminPermission) permission;
		AdminPermission existing;
		String name = null;
		
		if(ap.bundle != null){
			existing = (AdminPermission) permissions.get(ap.bundle);
		}
		else{
			existing = (AdminPermission) permissions.get(name = ap.getName());
		}
		
		if (existing != null) {
			int oldMask = existing.actionMask;
			int newMask = ap.actionMask;
			if (oldMask != newMask) {
				if(ap.bundle != null){
					permissions.put(ap.bundle, new AdminPermission(ap.bundle, oldMask | newMask));
				}
				else{
					permissions.put(name, new AdminPermission(name, oldMask | newMask));
				}	
			}
		}
		else {
			if(ap.bundle != null){
				permissions.put(ap.bundle, permission);
			}
			else{
				permissions.put(name, permission);
			}	
		}

		if (name != null && name.equals("*")){
			addedAll = true;
			allMask |= ap.actionMask;  
		}    		
	}

	public boolean implies(Permission permission) {
		if (!(permission instanceof AdminPermission))
			return (false);

		AdminPermission ap = (AdminPermission)permission;
		if (addedAll && ((allMask | ap.actionMask) == allMask)){
                    return true;
		}
		if(ap.bundle != null){
			Permission inTable = (Permission) permissions.get(ap.bundle);
			if (inTable == null)
	            return false;
	        
	        return inTable.implies(ap);
		}
		else{
			Enumeration enume = permissions.keys();
			while(enume.hasMoreElements()){
				if(((Permission) permissions.get(enume.nextElement())).implies(permission)){
					return true;
				}
			}
			return false;
		}	  
	}

	public Enumeration elements() {
		return (permissions.elements());
	}
}


