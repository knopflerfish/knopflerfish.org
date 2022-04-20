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

package org.knopflerfish.bundle.repository.index;

import java.util.List;

import org.knopflerfish.bundle.repository.index.Util.HeaderEntry;
import org.osgi.framework.Constants;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.service.indexer.Builder;
import org.osgi.service.indexer.Capability;
import org.osgi.service.indexer.Requirement;
import org.osgi.service.indexer.Resource;
import org.osgi.service.indexer.ResourceAnalyzer;

public class KnopflerfishExtentions implements ResourceAnalyzer {

  @Override
  public void analyzeResource(Resource resource, List<Capability> capabilities,
      List<Requirement> requirements) throws Exception {
	System.out.println("REPOINDEX_KF CALLED FOR: " + resource.getLocation());
    if(resource.getManifest() == null) {
      return;
    }

    String[][] mappings = new String[][] {
        { Constants.BUNDLE_NAME,
          "name"
        },
        { Constants.BUNDLE_DESCRIPTION,
          IdentityNamespace.CAPABILITY_DESCRIPTION_ATTRIBUTE
        },
        { Constants.BUNDLE_COPYRIGHT,
          IdentityNamespace.CAPABILITY_COPYRIGHT_ATTRIBUTE
        },
        { Constants.BUNDLE_DOCURL,
          IdentityNamespace.CAPABILITY_DOCUMENTATION_ATTRIBUTE,
        },
        /* Special handling
        { "Bundle-License",
          IdentityNamespace.CAPABILITY_LICENSE_ATTRIBUTE
        },
        */
        { Constants.BUNDLE_CATEGORY,
          "category"
        },
        { Constants.BUNDLE_VENDOR,
          "vendor"
        },
        { "Bundle-Icon",
          "icon"
        },
        {
          "Bundle-SubversionURL",
          "source"
        }
    };
    Builder b = new Builder().setNamespace("org.knopflerfish.extra");
    for(String[] m : mappings) {
      String manifestHeader = resource.getManifest().getMainAttributes().getValue(m[0]);
      if(manifestHeader == null) {
        continue;
      }
      String attributeName = m[1];
      b = b.addAttribute(attributeName, manifestHeader);
    }
    // Special handling of Bundle-License
    String manifestHeader = resource.getManifest().getMainAttributes().getValue("Bundle-License");
    if(manifestHeader != null) {
    	b = b.addAttribute(IdentityNamespace.CAPABILITY_LICENSE_ATTRIBUTE, parseLicenseHeader(manifestHeader));
    }
    
    capabilities.add(b.buildCapability());

  }
  
	String parseLicenseHeader(String a) {
		if (a == null) {
			return null;
		} else {
			StringBuilder sb = new StringBuilder();
			try {
				List<HeaderEntry> lic = Util.parseManifestHeader(
						"Bundle-License", a, true, true, false);
				for (HeaderEntry he : lic) {
					if (sb.length() > 0) {
						sb.append(", ");
					}
					sb.append(he.getKey());
				}
			} catch (IllegalArgumentException iae) {

			}
			return sb.toString();
		}
	}

}
