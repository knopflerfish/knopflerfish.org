package org.knopflerfish.framework;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

public class BundleRequirementImpl
  implements BundleRequirement
{

  private final BundleGeneration gen;
  private final String nameSpace;
  private final Map<String,String> directives;
  private final Map<String,Object> attributes;
  private final Filter filter;


  /**
   * Creates a {@link BundleRequirement} from the output of the
   * {@link FrameworkUtil#parseEntries() } applied to the Bundle-Requirement
   * header.
   *
   * @param gen the owning bundle revision.
   * @param tokens the parsed data for this requirement.
   */
  BundleRequirementImpl(BundleGeneration gen, Map<String, Object> tokens)
  {
    this.gen = gen;
    nameSpace = (String) tokens.remove("$key");
    for (final String ns : Arrays
        .asList(new String[] { BundleRevision.BUNDLE_NAMESPACE,
                               BundleRevision.HOST_NAMESPACE,
                               BundleRevision.PACKAGE_NAMESPACE })) {
      if (ns.equals(nameSpace)) {
        throw new IllegalArgumentException("Capability with name-space '" + ns
                                           + "' must not be required in the "
                                           + Constants.REQUIRE_CAPABILITY
                                           + " manifest header.");
      }
    }

    @SuppressWarnings("unchecked")
    final
    Map<String,Object> attrs = Collections.EMPTY_MAP;
    attributes = attrs;

    // Only directives are allowed
    @SuppressWarnings("unchecked")
    final Set<String> directiveNames = (Set<String>) tokens.remove("$directives");

    final Set<String> attributeNames = new HashSet<String>(tokens.keySet());
    attributeNames.removeAll(directiveNames);
    if (!attributeNames.isEmpty()) {
      throw new IllegalArgumentException("Attributes was defined in "
          + Constants.REQUIRE_CAPABILITY + "for name-space " +nameSpace
          +": " + attributeNames);
    }
    final String filterStr = (String) tokens.remove("filter");
    if (null!=filterStr && filterStr.length()>0) {
      try {
        filter = FrameworkUtil.createFilter(filterStr);
        tokens.put("filter", filter.toString());
      } catch (final InvalidSyntaxException ise) {
        final String msg = "Invalid filter '" + filterStr + "' in "
                           + Constants.REQUIRE_CAPABILITY
                           + " for name-space " + nameSpace + ": " + ise;
        throw (IllegalArgumentException)
          new IllegalArgumentException(msg).initCause(ise);
      }
    } else {
      filter = null;
    }
    @SuppressWarnings({ "rawtypes", "unchecked" })
    final
    Map<String,String> res = (Map) tokens;
    directives = Collections.unmodifiableMap(res);
  }

  public String getNamespace()
  {
    return nameSpace;
  }

  public Map<String, String> getDirectives()
  {
    return directives;
  }

  public Map<String, Object> getAttributes()
  {
    return attributes;
  }

  public BundleRevision getRevision()
  {
    return gen.getRevision();
  }

  public boolean matches(BundleCapability capability)
  {
    return null==filter ? true : filter.matches(capability.getAttributes());
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer(40);

    sb.append("[")
    .append(BundleRequirement.class.getName())
    .append(": ")
    .append(nameSpace)
    .append(" directives: ")
    .append(directives.toString())
    .append(" attributes: ")
    .append(attributes.toString())
    .append("]");

    return sb.toString();
  }
}
