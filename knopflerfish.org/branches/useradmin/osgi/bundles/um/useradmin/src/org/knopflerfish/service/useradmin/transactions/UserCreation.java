package org.knopflerfish.service.useradmin.transactions;

import org.knopflerfish.service.useradmin.*;

import java.util.*;

public class UserCreation extends RoleTransaction {

	private String name;

	public UserCreation(String name) {
		this.name = name;
	}

	protected Object executeAndQuery(RoleStore store, Date ignored) throws Exception {
		return store.createUser(name);
	}
}

