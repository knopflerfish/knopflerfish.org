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

import java.util.Map;

import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.IdentityExpression;
import org.osgi.service.repository.RequirementBuilder;

import org.knopflerfish.bundle.repository.xml.Data;
import org.knopflerfish.bundle.repository.xml.RequirementImpl;

public class RequirementBuilderImpl
  implements RequirementBuilder
{

  private final Data data;

  public RequirementBuilderImpl(String namespace)
  {
    data = new Data(namespace);
  }

  @Override
  public RequirementBuilder addAttribute(String name, Object value)
  {
    data.attributes.put(name, value);
    return this;
  }

  @Override
  public RequirementBuilder addDirective(String name, String value)
  {
    data.directives.put(name, value);
    return this;
  }

  @Override
  public RequirementBuilder setAttributes(Map<String, Object> attributes)
  {
    data.attributes.clear();
    data.attributes.putAll(attributes);
    return this;
  }

  @Override
  public RequirementBuilder setDirectives(Map<String, String> directives)
  {
    data.directives.clear();
    data.directives.putAll(directives);
    return null;
  }

  @Override
  public RequirementBuilder setResource(Resource resource)
  {
    data.resource = resource;
    return this;
  }

  @Override
  public Requirement build()
  {
    return new RequirementImpl(new Data(data));
  }

  @Override
  public IdentityExpression buildExpression()
  {
    return new IdentityExperssionImpl(build());
  }

}
