package org.knopflerfish.service.useradmin.impl;

import org.osgi.service.useradmin.*;

import java.io.*;
import java.security.*;
import java.util.*;

public abstract class RoleImpl implements Role, Serializable, Comparable {
	
	protected String name;
	protected SecureProperties props = new SecureProperties(this);
	
	protected transient boolean checkPermissions = System.getSecurityManager() != null;
	private transient Set listeners;

	public RoleImpl(String name) {
		this.name = name;
	}
	
	abstract public boolean hasMember(String name, Set visited);
	
	public boolean canBeVisited() {
		return true;
	}
	
	public boolean canBeDeleted() {
		return true;
	}
	
	public boolean equals(Object o) {
		return ((Role)o).getName().equals(name);
	}
	
	public int hashCode() {
		return name.hashCode();
	}
	
	public String toString() {
		return name;
	}

	// Role methods
	public String getName() {
		return name;
	}

	public int getType() {
		return Role.ROLE;
	}

	public Dictionary getProperties() {
		return props;
	}
	
	public int compareTo(Object o) {
		return name.compareTo(((Role)o).getName());
	}
	
	public void addRoleListener(RoleListener listener) {
		listeners().add(listener);
	}

	public void removeRoleListener(RoleListener listener) {
		listeners().remove(listener);
	}
	
	private Set listeners() {
		if (listeners == null) listeners = new HashSet();
		return listeners;
	}
	
	private void notifyListeners(Role role) {
		Iterator it = listeners().iterator();
		while (it.hasNext()) {
			((RoleListener)it.next()).roleChanged(role);
		}
	}
	
	protected class SecureProperties extends Dictionary implements Serializable {
		
		protected RoleImpl  _role;
		protected Hashtable _properties = new Hashtable();
		
		protected SecureProperties(RoleImpl role) {
			_role = role;
		}

		public Enumeration elements() {
			return _properties.elements();
		}
		
		// No security check for properties
		public Object get(Object key) {
			return _properties.get(key);
		}

		public boolean isEmpty() {
			return _properties.isEmpty();
		}

		public Enumeration keys() {
			return _properties.keys();
		}

		public int size() {
			return _properties.size();
		}

		public Object remove(Object key) {
			if (key instanceof String) {
				if (checkPermissions) {
					AccessController.checkPermission(new UserAdminPermission((String) key,
							UserAdminPermission.CHANGE_PROPERTY));
				}
				Object res = _properties.remove(key);
				notifyListeners(_role);
				return res;
			}
			throw new IllegalArgumentException("The key must be a String, got " + key.getClass());
		}

		public Object put(Object key, Object value) {
			if (key instanceof String) {
				if (checkPermissions) {
					AccessController.checkPermission(new UserAdminPermission((String) key,
							UserAdminPermission.CHANGE_PROPERTY));
				}
				Object res;
				// value of type byte[] or String is ok
				if (value instanceof byte[]) {
					res = _properties.put(key, ((byte[]) value).clone());
				} else if (value instanceof String) {
					res = _properties.put(key, value);
				} else
					throw new IllegalArgumentException("The value must be of type byte[]"
							+ " or String,  got " + value.getClass());
				notifyListeners(_role);

				return res;
			}
			throw new IllegalArgumentException("The key must be a String, got " + key.getClass());
		}

		public String toString() {
			return "#Properties#";
		}
	}
}

