package org.knopflerfish.service.useradmin;

import org.knopflerfish.service.useradmin.impl.*;

import org.osgi.service.useradmin.*;

import java.util.*;

public interface RoleStore extends MemberStore {
	
	public User createUser(String name);
	public Group createGroup(String name);
	public Role findRole(String name);
	public Role deleteRole(String name);
	public boolean containsRole(String name);
	public List roles();
	
	public void setRoleListener(RoleListener roleListener);
}

