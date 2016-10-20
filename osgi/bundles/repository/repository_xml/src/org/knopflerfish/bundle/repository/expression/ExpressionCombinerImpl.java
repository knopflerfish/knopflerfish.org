/*
 * Copyright (c) 2016-2016, KNOPFLERFISH project
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
package org.knopflerfish.bundle.repository.expression;

import java.util.ArrayList;
import java.util.Arrays;

import org.osgi.resource.Requirement;
import org.osgi.service.repository.AndExpression;
import org.osgi.service.repository.ExpressionCombiner;
import org.osgi.service.repository.IdentityExpression;
import org.osgi.service.repository.NotExpression;
import org.osgi.service.repository.OrExpression;
import org.osgi.service.repository.RequirementExpression;

public class ExpressionCombinerImpl
  implements ExpressionCombiner
{

  @Override
  public AndExpression and(RequirementExpression expr1,
                           RequirementExpression expr2)
  {
    ArrayList<RequirementExpression> exprs = new ArrayList<RequirementExpression>(2);
    exprs.add(expr1);
    exprs.add(expr2);
    return new AndExpressionImpl(exprs);
  }

  @Override
  public AndExpression and(RequirementExpression expr1,
                           RequirementExpression expr2,
                           RequirementExpression... moreExprs)
  {
    ArrayList<RequirementExpression> exprs = new ArrayList<RequirementExpression>(2 + moreExprs.length);
    exprs.add(expr1);
    exprs.add(expr2);
    exprs.addAll(Arrays.asList(moreExprs));
    return new AndExpressionImpl(exprs);
  }

  @Override
  public IdentityExpression identity(Requirement req)
  {
    return new IdentityExperssionImpl(req);
  }

  @Override
  public NotExpression not(RequirementExpression expr)
  {
    return new NotExpressionImpl(expr);
  }

  @Override
  public OrExpression or(RequirementExpression expr1,
                         RequirementExpression expr2)
  {
    ArrayList<RequirementExpression> exprs = new ArrayList<RequirementExpression>(2);
    exprs.add(expr1);
    exprs.add(expr2);
    return new OrExpressionImpl(exprs);
  }

  @Override
  public OrExpression or(RequirementExpression expr1,
                         RequirementExpression expr2,
                         RequirementExpression... moreExprs)
  {
    ArrayList<RequirementExpression> exprs = new ArrayList<RequirementExpression>(2 + moreExprs.length);
    exprs.add(expr1);
    exprs.add(expr2);
    exprs.addAll(Arrays.asList(moreExprs));
    return new OrExpressionImpl(exprs);
  }

}
