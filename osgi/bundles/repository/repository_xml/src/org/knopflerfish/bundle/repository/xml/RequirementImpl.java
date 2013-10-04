package org.knopflerfish.bundle.repository.xml;

import java.util.Map;

import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class RequirementImpl implements Requirement {
  final Data d;
  RequirementImpl(Data d) {
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
  
  public String toString() {
    return "Requirement[\n" + d.toString() +"]\n";
  }
}
