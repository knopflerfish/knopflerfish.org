package org.knopflerfish.service.useradmin.transactions;

import org.osgi.service.useradmin.*;

import java.util.*;

public class RemoveMember extends GroupTransaction {

	private Role role;

	public RemoveMember(Group group, Role role) {
		super(group);
		this.role = role;
	}

	public void executeAndQuery(Group group, Date timestamp) throws Exception {
		group.removeMember(role);
	}
}

