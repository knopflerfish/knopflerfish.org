package org.knopflerfish.service.useradmin.transactions;

import org.knopflerfish.service.useradmin.*;

import java.util.*;

public class RoleDeletion extends RoleTransaction {

	private String name;

	public RoleDeletion(String name) {
		this.name = name;
	}

	protected Object executeAndQuery(RoleStore role, Date ignored) throws Exception {
		return role.deleteRole(name);
	}
}

