This directory contains bundles handling definition of OSGI Metatype and 
Configuration Management user interaction.


 - kf_metatype

   This bundle defines an XML format for specifying OSGi Metatype
   instances. Also included are implementations classes for
   the interfaces specified in org.osgi.service.metatype

   This bundle, if started,  also exports  service of 
   type org.knopflerfish.util.metatype.SystemMetatypeProvider,
   allowing other bundles to retreive metatype information from
   bundles.

   For XML specification, see package.html or

    http://www.knopflerfish.org/XMLMetatype/


 - cm_desktop

   This bundle registers a Desktop plugin, presenting a Swing UI
   for manipulation of CM dictionaries.

   The bundle uses the SystemMetatypeProvider exported from kf_metatype.


Todo:

 - CM implementation in osgi/bundles/ shouldbe changed to use new
   schema-style storage for data.

