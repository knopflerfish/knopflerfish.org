package org.knopflerfish.service.useradmin.transactions;

import org.osgi.service.useradmin.*;

import java.util.*;

public class AddMember extends GroupTransaction {

	private Role role;

	public AddMember(Group group, Role role) {
		super(group);
		this.role = role;
	}

	public void executeAndQuery(Group group, Date timestamp) throws Exception {
		group.addMember(role);
	}
}

