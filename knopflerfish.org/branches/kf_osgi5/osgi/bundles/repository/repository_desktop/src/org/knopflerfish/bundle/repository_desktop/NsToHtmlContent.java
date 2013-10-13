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
import java.util.Map.Entry;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.ContentNamespace;

public class NsToHtmlContent
  implements NsToHtml
{

  public NsToHtmlContent()
  {
  }

  @Override
  public String getNs()
  {
    return ContentNamespace.CONTENT_NAMESPACE;
  }

  @Override
  public String toHTML(Capability capability)
  {
    // mime, url, size, sha

    // Make a modifiable clone of the attributes.
    final Map<String, Object> attrs =
      new HashMap<String, Object>(capability.getAttributes());

    final StringBuffer sb = new StringBuffer(50);

    Object val;

    val = attrs.remove(ContentNamespace.CAPABILITY_URL_ATTRIBUTE);
    if (val != null) {
      sb.append(Util.toHTML(val)).append("<br>");
    }

    val = attrs.remove(ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE);
    if (val != null) {
      sb.append("Size: ").append(val).append("&nbsp;bytes.<br>");
    }

    val = attrs.remove(ContentNamespace.CONTENT_NAMESPACE);
    if (val != null) {
      sb.append("SHA-256: ").append(val).append("<br>");
    }

    val = attrs.remove(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE);
    if (val != null) {
      sb.append("Content-Type: ").append(val).append("<br>");
    }

    if (!attrs.isEmpty()) {
      sb.append("Other attributes: ");
      sb.append(attrs);
    }

    return sb.toString();
  }

  @Override
  public String toHTML(Requirement requirement)
  {
    final StringBuffer sb = new StringBuffer(100);
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
