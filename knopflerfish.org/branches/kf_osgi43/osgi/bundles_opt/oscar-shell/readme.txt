Oscar shell wrapper for the Knopflerfish console
--------------------------------------------

The oscar-shell bundle wraps all commands defined
by the Oscar Command API

  org.ungoverned.osgi.service.shell.Command

so they become directly available in the Knopflerfish console (including
the KF desktop). This is useful if you need to run bundles developed
for Oscar in the KF framework, but still use the KF console.

All Oscar commands commands are placed in the KF command 
group "oscar".

For details on the KF console API, see

http://www.knopflerfish.org/releases/1.3.0/javadoc/org/knopflerfish/service/console/package-summary.html

Sample session from the KF console:

> enter oscar
oscar>
oscar> help
Commands available via the Oscar shell API
 gc          - Invokes the garbage collector.
 obr         - Oscar bundle repository.
oscar> obr list

Bundle Repository (1.0.1)
Content Handler (1.0.0)
Data Stream Handler (1.0.0)
Device Manager (1.0.0)
Handler Test (1.0.0)
HTTP Admin (1.0.0)
HTTP Driver Locator (1.0.0)
...
