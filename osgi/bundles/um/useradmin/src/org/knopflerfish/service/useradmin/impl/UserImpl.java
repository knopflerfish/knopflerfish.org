package org.knopflerfish.service.useradmin.impl;

import org.osgi.service.useradmin.*;

import java.io.*;
import java.security.*;
import java.util.*;

public class UserImpl extends RoleImpl implements User {
	
	protected SecureCredentials creds = new SecureCredentials(this);

	public UserImpl(String name) {
		super(name);
	}
	
	public boolean hasMember(String name, Set visited) {
		return name.equals(this.name);
	}
	
	public int getType() {
		return Role.USER;
	}

	// User methods
	public Dictionary getCredentials() {
		return creds;
	}

	public boolean hasCredential(String key, Object value) {
		if (checkPermissions) {
			AccessController.checkPermission(new UserAdminPermission(key,
					UserAdminPermission.GET_CREDENTIAL));
		}
		Object val = creds.get(key);
		if (val instanceof byte[] && value instanceof byte[]) {
			return Arrays.equals((byte[]) val, (byte[]) value);
		}
		if (val instanceof String && value instanceof String) {
			return val.equals(value);
		}

		return false;
	}
	
	private class SecureCredentials extends SecureProperties implements Serializable {
		
		UserImpl _user;
		
		protected SecureCredentials(UserImpl user) {
			super(user);
			_user = user;
		}
		
		protected String getChangeAction() {
			return UserAdminPermission.CHANGE_CREDENTIAL;
		}

		public Object get(Object key) {
			if (key instanceof String) {
				if (checkPermissions) {
					AccessController.checkPermission(new UserAdminPermission((String) key,
							UserAdminPermission.GET_CREDENTIAL));
				}
				return super.get(key);
			}
			throw new IllegalArgumentException("The key must be a String, got " + key.getClass());
		}

		public String toString() {
			return "#Credentials#";
		}
	}
}

