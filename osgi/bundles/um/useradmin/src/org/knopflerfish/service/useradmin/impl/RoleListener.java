package org.knopflerfish.service.useradmin.impl;

import org.osgi.service.useradmin.*;

public interface RoleListener {

	public void roleCreated(Role role);
	public void roleChanged(Role role);
	public void roleRemoved(Role role);
	
}

