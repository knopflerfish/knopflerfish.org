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

import java.util.Map.Entry;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

public class NsToHtmlGeneric
  implements NsToHtml
{

  public NsToHtmlGeneric()
  {
  }

  @Override
  public String getNs()
  {
    return "*";
  }

  @Override
  public String toHTML(Capability capability)
  {
    final StringBuilder sb = new StringBuilder(100);
    sb.append("<table border='0'>\n");
    // Generic HTML presentation
    for (final Entry<String, Object> attr : capability.getAttributes()
        .entrySet()) {
      Util.toHTMLtr13_3(sb, "=", attr.getKey(), attr.getValue());
    }
    for (final Entry<String, String> dir : capability.getDirectives()
        .entrySet()) {
      Util.toHTMLtr13_3(sb, ":=", dir.getKey(), dir.getValue());
    }
    sb.append("</table>\n");
    return sb.toString();
  }

  @Override
  public String toHTML(Requirement requirement)
  {
    final StringBuilder sb = new StringBuilder(100);
    sb.append("<table border='0'>\n");
    for (final Entry<String, String> dir : requirement.getDirectives()
        .entrySet()) {
      Util.toHTMLtr13_3(sb, ":=", dir.getKey(), dir.getValue());
    }
    for (final Entry<String, Object> attr : requirement.getAttributes()
        .entrySet()) {
      Util.toHTMLtr13_3(sb, "=", attr.getKey(), attr.getValue());
    }
    sb.append("</table>\n");
    return sb.toString();
  }

}
