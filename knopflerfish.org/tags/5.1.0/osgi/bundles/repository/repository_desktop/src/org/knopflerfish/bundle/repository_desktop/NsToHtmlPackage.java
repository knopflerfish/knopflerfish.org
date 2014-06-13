/*
 * Copyright (c) 2013-2013, KNOPFLERFISH project
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

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

public class NsToHtmlPackage
  implements NsToHtml
{

  private void appendVersionAndResolutionDirective(final StringBuffer sb,
                                                   final Requirement requirement)
  {
    final String filter =
      requirement.getDirectives().get(Constants.FILTER_DIRECTIVE);
    Util.appendVersion(sb, filter, Constants.VERSION_ATTRIBUTE);

    Util.appendResolution(sb, requirement);
  }

  public NsToHtmlPackage()
  {
  }

  @Override
  public String getNs()
  {
    return BundleRevision.PACKAGE_NAMESPACE;
  }

  @Override
  public String toHTML(Capability capability)
  {
    final StringBuffer sb = new StringBuffer(50);

    // Make a modifiable clone of the capability attributes.
    final Map<String, Object> attrs
      = new HashMap<String, Object>(capability.getAttributes());

    sb.append(attrs.remove(BundleRevision.PACKAGE_NAMESPACE));

    final Version version = (Version) attrs
        .remove(Constants.VERSION_ATTRIBUTE);
    if (version!=null) {
      sb.append("&nbsp;");
      sb.append(version);
    }

    attrs.remove(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
    attrs.remove(Constants.BUNDLE_VERSION_ATTRIBUTE);
    if (!attrs.isEmpty()) {
      sb.append("&nbsp;");
      sb.append(attrs);
    }

    return sb.toString();
  }

  @Override
  public String toHTML(Requirement requirement)
  {
    final StringBuffer sb = new StringBuffer(50);
    final String filter = requirement.getDirectives().get("filter");
    final String pkgName =
      Util.getFilterValue(filter, BundleRevision.PACKAGE_NAMESPACE);
    if (pkgName != null) {
      sb.append(pkgName);
      appendVersionAndResolutionDirective(sb, requirement);
    } else {
      sb.append(filter);
    }

    return sb.toString();
  }

}
