BEGIN { 
  print "<html>"
  print "<head>"
  print "<title>Knopflerfish OSGi - latest distributions</title>"
  print "<LINK href=\"/css/knopflerfish.css\" rel=\"stylesheet\" type=\"text/css\">"
  print "</head>"
  print "<body>"
  print "<h2>Latest Knopflerfish distribution from trunk</h2>"
  print "<pre>"
} 

{ 
  print $6 " " $7 " " $8 " " $5 " bytes   " "<a href=\"" $9 "\">" $9 "</a>" 
} 

END { 
  print "</pre>"
  print "<p>"
  print "<a href=\"junit_grunt/index.xml\">Latest test results (XML)</a>"
  print "</p>"
  print "<p>"
  print "<a href=\"/\">Back to Knopflerfish Main page</a>"
  print "</p>"
  print "</body>"
  print "</html>"}
