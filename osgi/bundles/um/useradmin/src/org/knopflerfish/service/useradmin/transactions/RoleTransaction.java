package org.knopflerfish.service.useradmin.transactions;

import org.knopflerfish.service.useradmin.*;

import org.prevayler.*;

import java.util.*;

public abstract class RoleTransaction implements TransactionWithQuery {

	public Object executeAndQuery(Object role, Date timestamp) throws Exception {
		return executeAndQuery((RoleStore)role, timestamp);
	}

	protected abstract Object executeAndQuery(RoleStore role, Date timestamp) throws Exception;
}

