package org.knopflerfish.service.useradmin;

import org.knopflerfish.service.useradmin.store.*;

import org.osgi.framework.*;
import org.osgi.service.useradmin.*;

import java.util.*;

public class Activator implements BundleActivator {
	
	public void start(BundleContext context) throws BundleException {
		RoleStore store = new InMemoryRoleStore();
		UserAdminService userAdmin = new UserAdminService(context, store);
		context.registerService(UserAdmin.class.getName(), userAdmin, new Hashtable());
	}

	public void stop(BundleContext context) throws BundleException {
		// do nothing
	}
}

