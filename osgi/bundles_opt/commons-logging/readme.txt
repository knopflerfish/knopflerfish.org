This a OSGi log wrapper for the Apache Commons Logging API.

A bundle can simple use

 import org.apache.commons.logging.*;

  ...
  Log log = LogFactory.getLog("my_unique_log_name");

  log.info("log message");


All logging using the commons logging API will be logged on the 
commons-logging bundle log.

The log levels are mapped as:

Commons level    OSGi level

 trace           debug
 debug           debug
 info            info
 warn            warn
 error           error
 fatal           fatal


