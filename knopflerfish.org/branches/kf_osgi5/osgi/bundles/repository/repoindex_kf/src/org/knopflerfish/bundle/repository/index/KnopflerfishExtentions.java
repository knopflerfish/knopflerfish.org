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
  // <?xml-stylesheet type="text/xsl" href="stylesheet.xsl"?>

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
        // TODO: Add source
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
    /* 
I namespacet "org.knopflerfish.extra" använder repository_desktop nu
följande attribut:

category         värdet på manifestheadern: Bundle-Category
vendor           värdet på manifestheadern: Bundle-Vendor


Och följande om de inte finns med i identity namespacet för en resurs:

IdentityNamespace.CAPABILITY_DESCRIPTION_ATTRIBUTE
IdentityNamespace.CAPABILITY_COPYRIGHT_ATTRIBUTE
IdentityNamespace.CAPABILITY_DOCUMENTATION_ATTRIBUTE
IdentityNamespace.CAPABILITY_LICENSE_ATTRIBUTE

*/  

  }

}
