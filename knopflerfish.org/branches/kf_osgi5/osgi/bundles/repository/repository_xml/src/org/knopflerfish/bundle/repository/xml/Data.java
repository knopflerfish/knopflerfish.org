package org.knopflerfish.bundle.repository.xml;

import java.util.Map;

import org.osgi.resource.Resource;

public class Data {
  String namespace;
  Map<String, String> directives;
  Map<String, Object> attributes;
  Resource resource;
}
