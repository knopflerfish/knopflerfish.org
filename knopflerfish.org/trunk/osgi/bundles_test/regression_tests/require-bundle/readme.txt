Forum item 
https://sourceforge.net/forum/forum.php?thread_id=1972848&forum_id=328005


When a bundle uses Require-Bundle along with visibility:=reexport,
does it not automatically re-export all the exported packages of the
required bundle? I hope I don't have to explicitly mention the list of
packages of the required bundles in order to make them available via
the requiring bundle.


To give an example:
 
Bundle-SymbolicName: test.rb_B 
Export-Package: test.rb_B 
 
Bundle-SymbolicName: test.rb_A 
Require-Bundle: test.rb_B;visibility:=reexport 
 
This should automatically result in test.rb_A bundle exporting test.rb_B
package, right?

YES!
