package org.knopflerfish.bundle.repository.xml;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Resource;


public class CapabilityImpl implements Capability {
  private final Data d;
  CapabilityImpl(Data d) {
    this.d = d;
  }

  @Override
  public String getNamespace() {
    return d.namespace;
  }

  @Override
  public Map<String, String> getDirectives() {
    return d.directives;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return d.attributes;
  }

  @Override
  public Resource getResource() {
    return d.resource;
  }
}
