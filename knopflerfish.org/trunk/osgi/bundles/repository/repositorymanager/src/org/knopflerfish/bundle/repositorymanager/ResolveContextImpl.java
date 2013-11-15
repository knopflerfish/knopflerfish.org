package org.knopflerfish.bundle.repositorymanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knopflerfish.service.repositorymanager.RepositoryManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;

public class ResolveContextImpl extends ResolveContext {
	final BundleContext bc;
	final List<Resource> mandatory;
	final List<Resource> optional;
	List<Resource> explicitResources = null;
	Map<Resource, Wiring> wirings = null;

	
	ResolveContextImpl(BundleContext bc, List<Resource> mandatory, List<Resource> optional) {
		this.bc = bc;
		this.mandatory = mandatory;
		this.optional = optional;
	}

	@Override
	public Collection<Resource> getMandatoryResources() {
		return mandatory;
	}

	@Override
	public Collection<Resource> getOptionalResources() {
		return optional;
	}
	
	@Override
	public synchronized List<Capability>  findProviders(Requirement r) {
		List<Capability> providers = new ArrayList<Capability>();
		// Same namespace
		// Only no effective or effective=resolve
		// filter
		// org.osgi.wiring.* mandatory
		// singleton

		Bundle[] bs = bc.getBundles();
		for(Bundle b : bs) {
			BundleRevision br = b.adapt(BundleRevision.class);
	        addToProvidersIfMatching(br, providers, r);
		}
		for(Resource m : mandatory) {
			addToProvidersIfMatching(m, providers, r);
		}
		for(Resource o : optional) {
			addToProvidersIfMatching(o, providers, r);
		}
		if(explicitResources == null) {
			explicitResources = new ArrayList<Resource>();
			for(Capability c : providers) {
				explicitResources.add(c.getResource());	
			}
		}
		ServiceReference<RepositoryManager> sr = bc.getServiceReference(RepositoryManager.class);
		if(sr != null) {
			RepositoryManager rm = bc.getService(sr);
			List<Capability> ps = rm.findProviders(r);
			for(Capability c : ps) {
				providers.add(c);
			}
		}
		return providers;
	}

	private void addToProvidersIfMatching(Resource res, List<Capability> providers, Requirement req) {
		String f = req.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
		Filter filter = null;
		if(f != null) {
		  try {
		    filter = bc.createFilter(f);
		  } catch (InvalidSyntaxException e) {
		    // TODO log filter failure, skip
		    System.err.println("Failed, " + f + ". " + e);
		    return;
		  }
		}
		for(Capability c : res.getCapabilities(req.getNamespace())) {
		  if(filter != null && !filter.matches(c.getAttributes())) {
		    continue;
		  }
		  providers.add(c);
		}
	}

	@Override
	public int insertHostedCapability(List<Capability> caps,
			HostedCapability hc) {
		List<Resource> resources = explicitResources;
		int index = resources.indexOf(hc.getResource());
		for (int i = 0; i < caps.size(); ++i) {
			Capability c = caps.get(i); 
			int otherIndex = resources.indexOf(c.getResource());
			if (otherIndex > index ) {
				caps.add(i, hc);
				return i;
			}
		}
		caps.add(hc);
		return caps.size()-1;
	}

	@Override
	public boolean isEffective(Requirement requirement) {
		String ed = requirement.getDirectives().get(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE);
		return ed == null || Namespace.EFFECTIVE_RESOLVE.equals(ed);
	}

	@Override
	public synchronized Map<Resource, Wiring> getWirings() { 
		if(wirings == null) {
			Map<Resource, Wiring> ws = new HashMap<Resource, Wiring>();
			Bundle[] bs = bc.getBundles();
			for(Bundle b : bs) {
				BundleRevision br = b.adapt(BundleRevision.class);
				ws.put(br, br.getWiring());
			}
			wirings = Collections.unmodifiableMap(ws);
		}
		return wirings;
	}

}
