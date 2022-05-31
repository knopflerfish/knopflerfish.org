To get Eclipse 4.3 or later to use -target jsr14 when compiling you
need to:

1) Select a JavaSE 6 runtime for you workspace (the jsr14 option was
   removed in the Java 7 compiler).

2) Edit the workspace configuration file named:

   workspace/.metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.jdt.core.prefs

   Change the targetPlatform, compliance and source lines to:

   org.eclipse.jdt.core.compiler.codegen.targetPlatform=jsr14
   org.eclipse.jdt.core.compiler.compliance=1.6
   org.eclipse.jdt.core.compiler.source=1.6

