package org.knopflerfish.bundle.repository.index;

import java.util.List;

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
    System.out.println("CALLED FOR: " + resource.getLocation());
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
        { "Bundle-License",
          IdentityNamespace.CAPABILITY_LICENSE_ATTRIBUTE
        },
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
    capabilities.add(b.buildCapability());   

  }

}
