package org.knopflerfish.bundle.repository.xml;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.RepositoryContent;

public class ResourceImpl implements Resource, RepositoryContent {
  ArrayList<Capability> cs = new ArrayList<Capability>();
  ArrayList<Capability> content = new ArrayList<Capability>();
  ArrayList<Requirement> rs = new ArrayList<Requirement>();

  @Override
  public List<Capability> getCapabilities(String namespace) {
    return cs;
  }

  @Override
  public List<Requirement> getRequirements(String namespace) {
    // TODO Auto-generated method stub
    return rs;
  }

  void addReq(RequirementImpl req) {
    rs.add(req);
    
  }

  void addCap(CapabilityImpl cap) {
    cs.add(cap);
    if(cap.getNamespace().equalsIgnoreCase(ContentNamespace.CONTENT_NAMESPACE)) {
      content.add(cap);
    }
  }

  boolean hasContent() {
    return !content.isEmpty();
  }
  
  @Override
  public InputStream getContent() {
    try {
      Capability c = content.get(0);
      c.getAttributes().get("url");
      return new URL((String)c.getAttributes().get("url")).openStream();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
}
