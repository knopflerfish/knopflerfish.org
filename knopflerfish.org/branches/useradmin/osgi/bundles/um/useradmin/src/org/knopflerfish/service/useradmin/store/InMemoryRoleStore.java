package org.knopflerfish.service.useradmin.store;
	
import org.knopflerfish.service.useradmin.*;
import org.knopflerfish.service.useradmin.impl.*;

import org.osgi.service.useradmin.*;

import java.io.*;
import java.util.*;

public class InMemoryRoleStore implements RoleStore, Serializable {

	private Map roles = new HashMap();
	private static final GroupImpl USER_ANYONE = new AnyoneGroup();
	
	private transient RoleListener roleListener;
	
	public InMemoryRoleStore() {
		if (!roles.containsKey(USER_ANYONE.getName())) {
			roles.put(USER_ANYONE.getName(), USER_ANYONE);
		}
	}
    
	public User createUser(String name) {
		User user = new UserImpl(name);
		roles.put(name, user);
		
		if (roleListener != null) roleListener.roleCreated(user);
		return user;
	}
	
	public Group createGroup(String name) {
		Group group = new GroupImpl(name);
		roles.put(name, group);
		
		if (roleListener != null) roleListener.roleCreated(group);
		return group;
	}
	
	public Role findRole(String name) {
		Role role = searchRole(name);
		return role;
	}

	private Role searchRole(String name) {
		return (Role)roles.get(name);
	}

	public Role deleteRole(String name) {
		Role role = findRole(name);
		if (((RoleImpl)role).canBeDeleted()) {
			roles.remove(name);
			if (roleListener != null) roleListener.roleRemoved(role);
			return role;
		}
		return null;
	}
	
	public boolean containsRole(String name) {
		return roles.containsKey(name);
	}
	
	public List roles() {
		List roleList = new ArrayList(roles.values());

		Collections.sort(roleList);

		return roleList;
	}
	
	public void addMember(Group group, Role role) {
		group.addMember(role);
	}
	
	public void addRequiredMember(Group group, Role role) {
		group.addRequiredMember(role);
	}
	
	public void removeMember(Group group, Role role) {
		group.removeMember(role);
	}
	
	public void setRoleListener(RoleListener roleListener) {
		this.roleListener = roleListener;
	}
}

