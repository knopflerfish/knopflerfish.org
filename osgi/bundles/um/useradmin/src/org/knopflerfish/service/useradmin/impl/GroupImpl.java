package org.knopflerfish.service.useradmin.impl;

import org.knopflerfish.service.useradmin.*;

import org.osgi.service.useradmin.*;

import java.security.*;
import java.util.*;

public class GroupImpl extends UserImpl implements Group {
	
	protected Set basicMembers    = new HashSet();
	protected Set requiredMembers = new HashSet();
	
	public GroupImpl(String name) {
		super(name);
	}

	public int getType() {
		return Role.GROUP;
	}
	
	public boolean hasMember(String name, Set visited) {
		
		boolean atLeastOneBasicMember = hasBasicMembers(basicMembers, name, visited);
		boolean allRequiredMembers    = hasRequiredMembers(requiredMembers, name, visited);
		
		return atLeastOneBasicMember && allRequiredMembers;
	}
	
	private boolean hasBasicMembers(Set members, String name, Set visited) {
		boolean hasBasic = false;

		if (members.isEmpty()) {
			return false;
		}

		Iterator it = members.iterator();
		while (it.hasNext()) {
			Object role = it.next();
			if (role instanceof GroupImpl) {
				GroupImpl group = (GroupImpl)role;
				if (visited.contains(group)) {
					return false;
				}
				if (group.canBeVisited()) {
					visited.add(group);
				}
				hasBasic = hasBasic || group.hasMember(name, visited);
			} else {
				if (role instanceof UserImpl) {
					hasBasic = hasBasic || ((UserImpl) role).hasMember(name, visited);
				}
			}
		}
		return hasBasic;
	}
	
	private boolean hasRequiredMembers(Set members, String name, Set visited) {
		boolean hasRequired = true;

		if (members.isEmpty()) {
			return true;
		}

		Iterator it = members.iterator();
		while (it.hasNext()) {
			Object role = it.next();
			if (role instanceof GroupImpl) {
				GroupImpl group = (GroupImpl)role;
				if (visited.contains(group)) {
					return false;
				}
				if (group.canBeVisited()) {
					visited.add(group);
				}
				hasRequired = hasRequired && group.hasMember(name, visited);
			} else {
				if (role instanceof UserImpl) {
					hasRequired = hasRequired && ((UserImpl) role).hasMember(name, visited);
				}
			}
		}
		return hasRequired;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("GROUP = " + name + ", BASIC = {");
		sb.append(rolesToString(basicMembers));
		sb.append("}, REQUIRED = {");
		sb.append(rolesToString(requiredMembers));
		sb.append("}");
		
		return sb.toString();
	}
	
	private StringBuffer rolesToString(Collection c) {
		StringBuffer sb = new StringBuffer();
		Iterator it = c.iterator();
		while (it.hasNext()) {
			Role role = (Role)it.next();
			sb.append(role.getName());
			if (it.hasNext()) {
				sb.append(", ");
			}
		}
		return sb;
	}

	// Group methods
	public boolean addMember(Role role) {
		if (checkPermissions) {
			AccessController.checkPermission(UserAdminService.adminPermission);
		}
		
		return basicMembers.add(role);
	}

	public boolean addRequiredMember(Role role) {
		if (checkPermissions) {
			AccessController.checkPermission(UserAdminService.adminPermission);
		}
		
		return requiredMembers.add(role);
	}

	public boolean removeMember(Role role) {
		if (checkPermissions) {
			AccessController.checkPermission(UserAdminService.adminPermission);
		}
		
		boolean wasRemoved = false;
		
		if (basicMembers.remove(role)) {
			wasRemoved = true;
		}
		if (requiredMembers.remove(role)) {
			wasRemoved = true;
		}

		return wasRemoved;
	}

	public Role[] getMembers() {
		Role[] roles = new Role[basicMembers.size()];
		return (Role[])basicMembers.toArray(roles);
	}

	public Role[] getRequiredMembers() {
		Role[] roles = new Role[requiredMembers.size()];
		return (Role[])requiredMembers.toArray(roles);
	}
}

