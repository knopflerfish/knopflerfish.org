package org.knopflerfish.service.useradmin;

import org.knopflerfish.service.useradmin.impl.*;

import org.osgi.framework.*;
import org.osgi.service.useradmin.*;

import java.security.*;
import java.util.*;

public class UserAdminService implements UserAdmin, ServiceFactory, ServiceListener, RoleListener {
	
	public static final UserAdminPermission adminPermission
		= new UserAdminPermission(UserAdminPermission.ADMIN, null);
	
	protected ServiceReference reference;
	protected RoleStore store;
	protected List listeners = new ArrayList();
	
	BundleContext bundleContext;
	
	UserAdminService(BundleContext context, RoleStore store) {
		bundleContext = context;
		this.store = store;
	}

	private void init(ServiceReference sr) {
		this.reference = sr;
		// Listen for UserAdminListeners, collect those already registered
		try {
			String clazz = UserAdminListener.class.getName();
			bundleContext.addServiceListener(this, "(objectClass=" + clazz + ")");
			ServiceReference[] srs = bundleContext.getServiceReferences(clazz, null);
			if (srs != null) {
				for (int i = 0; i < srs.length; i++) {
					listeners.add(srs[i]);
				}
			}
		} catch (InvalidSyntaxException ex) {
		}
	}

	void sendEvent(int type, Role role) {
		UserAdminEvent event = new UserAdminEvent(reference, type, role);
		
		Iterator it = listeners.iterator();
		while (it.hasNext()) {
			ServiceReference reference = (ServiceReference)it.next();
			UserAdminListener ual =(UserAdminListener)bundleContext.getService(reference);
			if (ual != null) {
				ual.roleChanged(event);
			}
			bundleContext.ungetService(reference);
		}
	}

	// ServiceFactory methods
	public synchronized Object getService(Bundle bundle, ServiceRegistration registration) {
		// Factory is only used to be able to register and then later
		// get hold of our own ServiceReference before any bundle have
		// a chance to use the service.
		if (reference == null) {
			// initialize ourself with the service reference the first time
			// the service is used by a bundle
			init(registration.getReference());
		}
		return this;
	}

	public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
		// do nothing
	}
	
	// RoleListener methods
	public void roleCreated(Role role) {
		sendEvent(UserAdminEvent.ROLE_CREATED, role);
	}
	public void roleChanged(Role role) {
		sendEvent(UserAdminEvent.ROLE_CHANGED, role);
	}
	public void roleRemoved(Role role) {
		sendEvent(UserAdminEvent.ROLE_REMOVED, role);
	}

	// UserAdmin methods
	public Role createRole(String name, int type) {
		if (System.getSecurityManager() != null) {
			AccessController.checkPermission(adminPermission);
		}
		
		if (store.containsRole(name)) {
			return null;
		}

		Role role = null;
		
		switch (type) {
			case Role.USER:
				role = store.createUser(name);
				break;
			case Role.GROUP:
				role = store.createGroup(name);
				break;
			default:
				throw new IllegalArgumentException("UserAdminService: Unknown type '" + type + "'.");
		}

		sendEvent(UserAdminEvent.ROLE_CREATED, role);
		((RoleImpl)role).addRoleListener(this);

		return role;
	}

	public boolean removeRole(String name) {
		if (System.getSecurityManager() != null) {
			AccessController.checkPermission(adminPermission);
		}
		
		Role deletedRole = store.deleteRole(name);
		
		if (deletedRole != null) {
			sendEvent(UserAdminEvent.ROLE_REMOVED, deletedRole);
			return true;
		}

		return false;
	}

	public Role getRole(String name) {
		return store.findRole(name);
	}

	public Role[] getRoles(String filter) throws InvalidSyntaxException {
		List results = new ArrayList();
		Iterator it = store.roles().iterator();
		while (it.hasNext()) {
			RoleImpl role = (RoleImpl) it.next();
			if (filter == null || LDAPQuery.query(filter, role.getProperties())) {
				results.add(role);
			}
		}
		Role[] roles = new Role[results.size()];
		return (Role[])results.toArray(roles);
	}

	public User getUser(String key, String value) {
		User[] users = getUsers(key, value);

		return users.length == 1 ? users[0] : null;
	}

	private User[] getUsers(String key, String value) {
		List results = new ArrayList();
		Iterator it = store.roles().iterator();
		while (it.hasNext()) {
			Object o = it.next();
			if (o instanceof UserImpl) {
				UserImpl user = (UserImpl)o;
				Object content = user.getProperties().get(key);
				if (content instanceof String && value.equals(content)) {
					results.add(user);
				}
			}
		}
		User[] users = new User[results.size()];
		return (User[])results.toArray(users);
	}

	public Authorization getAuthorization(User user) {
		
		String userName;
		if (user == null) {
			userName = "user.anyone";
		} else {
			userName = user.getName();
		}
		
		List roles = new ArrayList();
		
		Iterator it = store.roles().iterator();
		while (it.hasNext()) {
			RoleImpl role = (RoleImpl)it.next();
			if (role.hasMember(userName, new HashSet())) {
				roles.add(role.getName());
			}
		}
		return new AuthorizationImpl(userName, roles);
	}

	// ServiceListener methods
	public void serviceChanged(ServiceEvent event) {
		ServiceReference sr = event.getServiceReference();
		switch (event.getType()) {
			case ServiceEvent.REGISTERED :
				listeners.add(sr);
				break;
			case ServiceEvent.UNREGISTERING :
				break;
		}
	}
}

