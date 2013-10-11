package org.knopflerfish.bundle.repository_desktop;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;

/**
 * Requirement selecting downloadable bundles and fragments.
 * @author ekolin
 *
 */
public class BundleRequirement
  implements Requirement
{
  /**
   * See Provisioning API, {@code org.osgi.service.provisioning.ProvisioningService}.
   */
  static final String MIME_BUNDLE = "application/vnd.osgi.bundle";
  static final String MIME_BUNDLE_ALT = "application/x-osgi-bundle";

  Map<String,String> directives = new HashMap<String, String>();
  Map<String,Object> attributes = new HashMap<String, Object>();

  public BundleRequirement()
  {
    final String filter =
      "(|(" + ContentNamespace.CAPABILITY_MIME_ATTRIBUTE + "=" + MIME_BUNDLE
          + ")(" + ContentNamespace.CAPABILITY_MIME_ATTRIBUTE + "="
          + MIME_BUNDLE_ALT + "))";
    directives.put(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter);
  }

  @Override
  public String getNamespace()
  {
    return ContentNamespace.CONTENT_NAMESPACE;
  }

  @Override
  public Map<String, String> getDirectives()
  {
    return Collections.unmodifiableMap(directives);
  }

  @Override
  public Map<String, Object> getAttributes()
  {
    return Collections.unmodifiableMap(attributes);
  }

  @Override
  public Resource getResource()
  {
    return null;
  }

}
