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
			StringBuffer sb = new StringBuffer();
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
