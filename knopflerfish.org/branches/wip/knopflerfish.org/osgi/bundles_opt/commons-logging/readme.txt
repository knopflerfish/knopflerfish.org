This an OSGi log wrapper for the Apache Commons Logging API.

Any bundle importing from the wrapper bundle can simply use

 import org.apache.commons.logging.*;

  ...
  Log log = LogFactory.getLog("my_unique_log_name");

  log.info("log message");


All log calls using the commons logging API will be logged on the 
commons-logging bundle log -- not on the calling bundles log!

The log levels are mapped as:

Commons level    OSGi level

 trace           debug (with extra prefix "/trace")
 debug           debug
 info            info
 warn            warn
 error           error
 fatal           fatal (with extra prefix "/fatal")

The is[Level]Enabled() calls are mapped to match.

Each log entry is logged as

 {log name}/message

Note that this bundle converts calls using the commons logging API into
logs stored by the OSGi log service, not the other way around! This means 
you still need a log implementation to get persistent logs. This bundle
does, however, revert gracefully to stdout of no such service is found.

Having a log implementation using, for example Log4J, is yet to be done.

/E


