package org.knopflerfish.bundle.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

public class RepositoryImpl implements Repository {
  Collection<Resource> rs;
  BundleContext bc;

  RepositoryImpl(BundleContext bc, Collection<Resource> rs) {
    this.bc = bc;
    this.rs = rs;
  }
  
  @Override
  public Map<Requirement, Collection<Capability>> findProviders(
      Collection<? extends Requirement> requirements) {
    HashMap<Requirement, Collection<Capability>> ps = 
        new HashMap<Requirement, Collection<Capability>>();
    for(Requirement req : requirements) {
      ps.put(req, new ArrayList<Capability>());
    }
    for(Resource r : rs) {
      for(Requirement req : requirements) {
        String f = req.getDirectives().get("filter");
        Filter filter = null;
        if(f != null) {
          try {
            filter = bc.createFilter(f);
          } catch (InvalidSyntaxException e) {
            // TODO log filter failure, skip
            System.err.println("Failed, " + f + ". " + e);
            continue;
          }
        }
        for(Capability c : r.getCapabilities(req.getNamespace())) {
          if(filter != null && !filter.matches(c.getAttributes())) {
            continue;
          }
          ps.get(req).add(c);
        }
      }
    }
    return ps;
  }

}
