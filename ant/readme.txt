This directory contains ant related code ant build files.
Ant version 1.7 or higher is required.

 build_example.xml          Example of bundle build.xml

 bundlebuild.xml            ant build file to be imported by
                            build.xml when building a bundle.

 bundlebuild_include.xml    Left for backwards compatibility allowing
                            older build-files to function without
                            change.  ant build file to be included
                            (using an external entity) in build.xml.

 bundletasks.xml            ant build file included by bundlebuild_include.xml
                            Defines and compiles the bundle build tasks.

 html_template              HTML templates for generated bundle
                              docs
 src                        source code for bundle build task 


Note: As of Knopflerfish 2.0 all the properties used to specify the
bundle manifest have been renamed. The new naming scheme gives three
major advantages:

1 The properties can now be specified in a bundle manifest template
  file, named bundle.manifest, that may be created using the
  Knopflerfish Eclipse plug in while their values are still available
  for use in the ant build script.

2 You can now add any manifest header you like to the generated bundle
  manifest (previously there where only support for a fixed predefined
  set of manifest attributes).

3 Easy to remember mapping from ant property name to bundle manifest
  attribute name.


To get the name of the ant property that corresponds to the manifest
attribute named "Xy-Zz" simply add the prefix "bmfa." to it.

E.g.,

  Manifest Attribute name  ant property name
  =======================  =================

  Bundle-Name              bmfa.Bundle-Name
  Bundle-SymbolicName      bmfa.Bundle-SymbolicName
  Bundle-Version           bmfa.Bundle-Version
  Bundle-Classpath         bmfa.Bundle-Classpath

and so on.

The default prefix, "bmfa", is a short hand for Bundle ManiFest Attribute.


To add a non-standard attribute to the generated manifest simple
create a property with a name that starts with "bmfa." followed by the
manifest attribute name.

E.g., the property definition

<property name="bmfa.Main-Class" value="org.knopflerfish.Main"/>

will add the attribute

Main-Class: org.knopflerfish.Main

to all the bundle manifests generated from the build file it was
defined in.

Another method to do this is to create a template bundle manifest file
that contains the main section attribute definition you want. The
template manifest file shall be named "bundle.manifest" and placed in
the same directory as the build.xml file that shall use it.


The format of the manifest template file is that of a normal manifest
file with one exception: Line length must not obey the 72 characters
per line requirement. The character encoding of the template manifest
file is expected to be UTF-8, but you may specify another encoding in
the build.xml file.

The relaxed line length requirement makes it possible to format the
template manifest file in a readable way.

E.g., the Import-Package and Export-Package can be written with one
package per line as in:

Import-Service: org.knopflerfish.service.log.LogService,
 org.osgi.service.cm.ManagedService,
 org.osgi.service.cm.ManagedServiceFactory,
 org.osgi.service.cm.ConfigurationPlugin
Export-Package: org.osgi.service.cm;version=1.2.0,
 org.knopflerfish.shared.cm;version=1.0
