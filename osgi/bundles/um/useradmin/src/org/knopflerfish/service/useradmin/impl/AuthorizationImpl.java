package org.knopflerfish.service.useradmin.impl;

import org.osgi.service.useradmin.*;

import java.io.*;
import java.util.*;

public class AuthorizationImpl implements Authorization, Serializable {

	private String name;
	private List roles;
	
	public AuthorizationImpl(String name, List roles) {
		this.name  = name;
		this.roles = roles;
	}

	// Authorization methods
	public String getName() {
		return name;
	}
	
	public String[] getRoles() {
		if (roles.isEmpty()) {
			return null;
		}
		
		return (String[])roles.toArray();
	}

	public boolean hasRole(String roleName) {
		return roles.contains(roleName);
	}
	
	// Object methods
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("USER = " + name + ", ROLES = {");
		sb.append(listToString(roles));
		sb.append("}");
		
		return sb.toString();
	}
	
	private StringBuffer listToString(List list) {
		StringBuffer sb = new StringBuffer();
		Iterator it = list.iterator();
		while (it.hasNext()) {
			Object role = it.next();
			sb.append(role);
			if (it.hasNext()) {
				sb.append(", ");
			}
		}
		return sb;
	}
}

