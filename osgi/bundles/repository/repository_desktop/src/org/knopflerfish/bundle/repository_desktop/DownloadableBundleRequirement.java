/*
 * Copyright (c) 2013-2022, KNOPFLERFISH project
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

package org.knopflerfish.bundle.repository_desktop;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;

/**
 * Requirement selecting downloadable bundles and fragments.
 * @author ekolin
 *
 */
public class DownloadableBundleRequirement
  implements Requirement
{
  /**
   * See Provisioning API, {@code org.osgi.service.provisioning.ProvisioningService}.
   */
  static final String MIME_BUNDLE = "application/vnd.osgi.bundle";
  static final String MIME_BUNDLE_ALT = "application/x-osgi-bundle";

  Map<String,String> directives = new HashMap<>();
  Map<String,Object> attributes = new HashMap<>();

  public DownloadableBundleRequirement()
  {
    final String filter =
      "(|(" + ContentNamespace.CAPABILITY_MIME_ATTRIBUTE + "=" + MIME_BUNDLE
          + ")(" + ContentNamespace.CAPABILITY_MIME_ATTRIBUTE + "="
          + MIME_BUNDLE_ALT + "))";
    directives.put(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
  }

  @Override
  public String getNamespace()
  {
    return ContentNamespace.CONTENT_NAMESPACE;
  }

  @Override
  public Map<String, String> getDirectives()
  {
    return Collections.unmodifiableMap(directives);
  }

  @Override
  public Map<String, Object> getAttributes()
  {
    return Collections.unmodifiableMap(attributes);
  }

  @Override
  public Resource getResource()
  {
    return null;
  }

}
