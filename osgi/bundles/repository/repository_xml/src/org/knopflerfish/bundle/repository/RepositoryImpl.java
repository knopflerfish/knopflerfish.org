/*
 * Copyright (c) 2013-2016, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 *
 * - Neither the name of the KNOPFLERFISH project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
import org.osgi.service.repository.ExpressionCombiner;
import org.osgi.service.repository.Repository;
import org.osgi.service.repository.RequirementBuilder;
import org.osgi.service.repository.RequirementExpression;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

import org.knopflerfish.bundle.repository.expression.ExpressionCombinerImpl;
import org.knopflerfish.bundle.repository.expression.ExpressionResolver;
import org.knopflerfish.bundle.repository.expression.RequirementBuilderImpl;


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


  @Override
  public Promise<Collection<Resource>> findProviders(RequirementExpression expression) {
    Deferred<Collection<Resource>> d = new Deferred<Collection<Resource>>();
    try {
      new ExpressionResolver(this, expression, d).start();
    } catch (Exception e) {
      d.fail(e);
    }
    return d.getPromise();
  }


  @Override
  public ExpressionCombiner getExpressionCombiner() {
    return new ExpressionCombinerImpl();
  }


  @Override
  public RequirementBuilder newRequirementBuilder(String namespace) {
    return new RequirementBuilderImpl(namespace);
  }

}
