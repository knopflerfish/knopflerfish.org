package org.knopflerfish.service.useradmin;

import org.knopflerfish.service.useradmin.store.*;

import org.osgi.service.useradmin.*;

import junit.framework.*;

public class AuthorizationTest extends TestCase {
	
	RoleStore store;
	UserAdmin ua;
	
	User elmer, pepe, bugs, daffy;
	Group administrators, family, alarmSystemActivation, userAnyone;

	public AuthorizationTest(String name) {
		super(name);
		try {
			//store = new InMemoryRoleStore();
			store = new PrevalentRoleStore();
			ua = new UserAdminService(null, store);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected void setUp() throws Exception {
		elmer = (User)ua.createRole("Elmer", Role.USER);
		pepe  = (User)ua.createRole("Pepe", Role.USER);
		bugs  = (User)ua.createRole("Bugs", Role.USER);
		daffy = (User)ua.createRole("Daffy", Role.USER);
		
		administrators        = (Group)ua.createRole("Administrators", Role.GROUP);
		family                = (Group)ua.createRole("Family", Role.GROUP);
		alarmSystemActivation = (Group)ua.createRole("AlarmSystemActivation", Role.GROUP);
		
		elmer = (User)ua.getRole("Elmer");
		pepe = (User)ua.getRole("Pepe");
		
		administrators = (Group)ua.getRole("Administrators");
		family = (Group)ua.getRole("Family");
		alarmSystemActivation = (Group)ua.getRole("AlarmSystemActivation");
		userAnyone = (Group)ua.getRole("user.anyone");
		
		store.addMember(family, elmer);
		store.addMember(family, pepe);
		
		store.addMember(alarmSystemActivation, elmer);
		
		/*
		administrators.addMember(elmer);
		administrators.addMember(pepe);
		administrators.addMember(bugs);
		administrators.addRequiredMember(UserAdminService.USER_ANYONE);
		
		family.addMember(elmer);
		family.addMember(pepe);
		family.addMember(daffy);
		family.addRequiredMember(UserAdminService.USER_ANYONE);
		
		// kills it if no cyclic checking
		//family.addRequiredMember(alarmSystemActivation);
		
		//alarmSystemActivation.addRequiredMember(administrators);
		//alarmSystemActivation.addRequiredMember(family);
		alarmSystemActivation.addRequiredMember(UserAdminService.USER_ANYONE);
		alarmSystemActivation.addMember(UserAdminService.USER_ANYONE);
		*/
	}
	
	public void testGroup() {
		System.out.println(administrators.toString());
		System.out.println(family.toString());
		System.out.println(alarmSystemActivation.toString());
		System.out.println(userAnyone.toString());
	}
	
	public void testAuthorization() {
		Authorization auth = ua.getAuthorization(elmer);
		System.out.println(auth.hasRole("AlarmSystemActivation"));
		System.out.println(auth.toString());
	}
	
	public void testGetRoles() throws Exception {
		
		Role[] roles = ua.getRoles(null);
		
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < roles.length; i++) {
			sb.append(roles[i].getName());
			if (i < roles.length - 1) {
				sb.append(", ");
			}
		}
		System.out.println("All roles:  " + sb.toString());
	}
}

