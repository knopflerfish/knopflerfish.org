package org.knopflerfish.service.useradmin.store;

import org.knopflerfish.service.useradmin.*;
import org.knopflerfish.service.useradmin.impl.*;
import org.knopflerfish.service.useradmin.transactions.*;

import org.osgi.service.useradmin.*;
import org.prevayler.*;

import java.util.*;

public class PrevalentRoleStore implements RoleStore {
	
	private final Prevayler prevayler;
	
	public PrevalentRoleStore() throws Exception {
		prevayler = PrevaylerFactory.createPrevayler(new InMemoryRoleStore(), "roleStore");
	}

	public User createUser(String name) {
		User user = null;
		try {
			user = (User)prevayler.execute(new UserCreation(name));
		} catch (Exception e) {
			throw new RuntimeException();
		}
		return user;
	}
	
	public Group createGroup(String name) {
		Group group = null;
		try {
			group = (Group)prevayler.execute(new GroupCreation(name)); 
		} catch (Exception e) {
			throw new RuntimeException();
		}
		return group;
	}

	public Role findRole(String name) {
		return store().findRole(name);
	}

	public Role deleteRole(String name) {
		Role role = null;
		try {
			role = (Role)prevayler.execute(new RoleDeletion(name)); 
		} catch (Exception e) {
			throw new RuntimeException();
		}
		return role;
	}
	
	public boolean containsRole(String name) {
		return store().containsRole(name);
	}

	public List roles() {
		return store().roles();
	}
	
	public void addMember(Group group, Role role) {
		try {
			prevayler.execute(new AddMember(group, role));
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}
	
	public void addRequiredMember(Group group, Role role) {
		try {
			prevayler.execute(new AddRequiredMember(group, role));
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}
	
	public void removeMember(Group group, Role role) {
		try {
			prevayler.execute(new RemoveMember(group, role));
		} catch (Exception e) {
			throw new RuntimeException();
		}
	}

	public void setRoleListener(RoleListener roleListener) {
		store().setRoleListener(roleListener);
	}
	
	private RoleStore store() {
		return (RoleStore)prevayler.prevalentSystem();
	}
}

