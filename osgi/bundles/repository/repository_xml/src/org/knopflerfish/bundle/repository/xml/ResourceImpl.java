package org.knopflerfish.bundle.repository.xml;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.RepositoryContent;

public class ResourceImpl implements Resource, RepositoryContent {
  List<Capability> cs = new ArrayList<Capability>();
  List<Requirement> rs = new ArrayList<Requirement>();

  @Override
  public List<Capability> getCapabilities(String namespace) {
    List<Capability> result = cs;
    if(namespace != null) {
      result = new ArrayList<Capability>();
      for(Capability c : cs) {
        if(namespace.equalsIgnoreCase(c.getNamespace())) {
          result.add(c);
        }
      }
    }
    return Collections.unmodifiableList(result);
  }

  @Override
  public List<Requirement> getRequirements(String namespace) {
    List<Requirement> result = rs;
    if(namespace != null) {
      result = new ArrayList<Requirement>();
      for(Requirement r : rs) {
        if(namespace.equalsIgnoreCase(r.getNamespace())) {
          result.add(r);
        }
      }
    }
    return Collections.unmodifiableList(result);
  }

  void addReq(RequirementImpl req) {
    rs.add(req);
    
  }

  void addCap(CapabilityImpl cap) {
    cs.add(cap);
  }

  boolean hasContent() {
    return !getCapabilities(ContentNamespace.CONTENT_NAMESPACE).isEmpty();
  }
  
  @Override
  public InputStream getContent() {
    try {
      Capability c = getCapabilities(ContentNamespace.CONTENT_NAMESPACE).get(0);
      return new URL((String)c.getAttributes().get(ContentNamespace.CAPABILITY_URL_ATTRIBUTE)).openStream();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
  
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("Resource[\n");
    for(Capability c : cs) {
      sb.append(c.toString());
    }
    for(Requirement r : rs) {
      sb.append(r.toString());
    }
    sb.append("]\n");
    return sb.toString();
  }
}
