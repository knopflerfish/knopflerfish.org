package org.knopflerfish.bundle.repository.expression;

import org.osgi.resource.Requirement;
import org.osgi.service.repository.IdentityExpression;

public class IdentityExperssionImpl
  implements IdentityExpression
{
  final Requirement requirement;

  IdentityExperssionImpl(Requirement requirement)
  {
    this.requirement = requirement;
  }

  @Override
  public Requirement getRequirement()
  {
    return requirement;
  }

}
