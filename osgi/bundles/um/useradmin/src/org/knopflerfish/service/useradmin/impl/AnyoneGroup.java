package org.knopflerfish.service.useradmin.impl;

import org.osgi.service.useradmin.*;

import java.util.*;

public class AnyoneGroup extends GroupImpl {
	
	public AnyoneGroup() {
		super("user.anyone");
	}

	public boolean hasMember(String name, Set visited) {
		return true;
	}
	
	public boolean canBeVisited() {
		return false;
	}
	
	public boolean canBeDeleted() {
		return false;
	}
	
	public boolean addMember(Role role) {
		return false;
	}
	
	public boolean addRequiredMember(Role role) {
		return false;
	}
	
	public boolean removeMember(Role role) {
		return false;
	}
}

