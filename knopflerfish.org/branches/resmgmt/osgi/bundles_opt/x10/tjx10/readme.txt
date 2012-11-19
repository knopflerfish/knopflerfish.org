X10 serial controller wrapper using code from The Java X10 Project. 

  http://x10.homelinux.org

This bundle is GPL licensed and must contain full source. 

All other bundles depending on the tjx10 bundle must also be 
distributed as GPL.

When started and attached to a serial port (using CM), this bundle will
register an

 x10.Controller

service into the framework. This can then be used by other bundles. 
See javadocs for the x10 packages at

 http://x10.homelinux.org/docs/


The bundle is configured using a CM factory with the pid

 org.knopflerfish.tjx10.controllers

which accepts properties:

  port   - serial port module is attached to
  module - module name of X10 controller 
           Supported modules are "cm11a" and "cm17a" (case insensitive)
