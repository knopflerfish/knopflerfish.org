package org.knopflerfish.service.useradmin.transactions;

import org.knopflerfish.service.useradmin.*;

import org.osgi.service.useradmin.*;

import java.util.*;

abstract class GroupTransaction extends RoleTransaction {

	private String name;

	protected GroupTransaction(Group group) {
		name = group.getName();
	}

	protected Object executeAndQuery(RoleStore store, Date timestamp) throws Exception {
		executeAndQuery((Group)store.findRole(name), timestamp);
		return null;
	}

	protected abstract void executeAndQuery(Group group, Date timestamp) throws Exception;

}

