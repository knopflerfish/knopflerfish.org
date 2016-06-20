package org.knopflerfish.bundle.repository.expression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.AndExpression;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.IdentityExpression;
import org.osgi.service.repository.NotExpression;
import org.osgi.service.repository.OrExpression;
import org.osgi.service.repository.Repository;
import org.osgi.service.repository.RequirementBuilder;
import org.osgi.service.repository.RequirementExpression;
import org.osgi.util.promise.Deferred;

import org.knopflerfish.bundle.repository.xml.RequirementImpl;

public class ExpressionResolver
  extends Thread
{
  final Repository repository;
  final RequirementExpression expression;
  final Deferred<Collection<Resource>> deferred;

  public ExpressionResolver(Repository repository,
                            RequirementExpression expression,
                            Deferred<Collection<Resource>> deferred)
  {
    this.repository = repository;
    this.expression = expression;
    this.deferred = deferred;
  }

  @Override
  public void run() {
    try {
      Collection<Resource> resolved = resolve(expression);
      if (resolved instanceof NegativeCollection) {
        RequirementBuilder rb = repository.newRequirementBuilder(ContentNamespace.CONTENT_NAMESPACE);
        Collection<Resource> rs = getResources(rb.build());
        rs.retainAll(resolved);
        resolved = rs;
      }
      deferred.resolve(resolved);
    } catch (Exception e) {
      deferred.fail(e);
    }
  }

  Collection<Resource> resolve(RequirementExpression expr) {
    if (expr instanceof AndExpression) {
      List<RequirementExpression> reqs = ((AndExpression)expr).getRequirementExpressions();
      Collection<Resource> res = resolve(reqs.get(0));
      for (int i = 1; i < reqs.size(); i++) {
        Collection<Resource> resolved = resolve(reqs.get(i));
        if (!(resolved instanceof NegativeCollection)) {
          Collection<Resource> tmp = res;
          res = resolved;
          resolved = tmp;
        }
        res.retainAll(resolved);
      }
      return res;
    } else if (expr instanceof IdentityExpression) {
      Requirement req = ((IdentityExpression)expr).getRequirement();
      Collection<Resource> res = getResources(req);
      return res;
    } else if (expr instanceof NotExpression) {
      Collection<Resource> resolved = resolve(((NotExpression)expr).getRequirementExpression());
      if (resolved instanceof NegativeCollection) {
        return ((NegativeCollection<Resource>)resolved).negate();
      } else {
        return new NegativeCollection<Resource>(resolved);
      }
    } else if (expr instanceof OrExpression) {
      List<RequirementExpression> reqs = ((OrExpression)expr).getRequirementExpressions();
      Collection<Resource> res = resolve(reqs.get(0));
      for (int i = 1; i < reqs.size(); i++) {
        Collection<Resource> resolved = resolve(reqs.get(i));
        if (resolved instanceof NegativeCollection) {
          Collection<Resource> tmp = res;
          res = resolved;
          resolved = tmp;
        }
        for (Resource r : resolved) {
          if (!res.contains(r)) {
            res.add(r);
          }
        }
      }
      return res;
    } else {
      throw new IllegalArgumentException("Unkown RequirementExpression: " + expr);
    }
  }

  private Collection<Resource> getResources(Requirement req)
  {
    Collection<Capability> cs = repository.findProviders(Collections.singleton(req)).get(req);
    Collection<Resource> res = new ArrayList<Resource>(cs.size());
    for (Capability c : cs) {
      // TODO could resource be null?
      res.add(c.getResource());
    }
    return res;
  }
}
