package org.knopflerfish.service.useradmin;

import org.knopflerfish.service.useradmin.impl.*;

import org.osgi.service.useradmin.*;

public interface MemberStore {
	
	public void addMember(Group group, Role role);
	public void addRequiredMember(Group group, Role role);
	public void removeMember(Group group, Role role);
	
	public void setRoleListener(RoleListener roleListener);

}

