<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!-- 
  This is a semi-naive JUnit XML formatting stylesheet.
  -->

<xsl:template match="/">

<html>


<head>
<meta http-equiv = "Content-Type"
      content    = "text/html; charset=ISO-8859-1"/>

<meta http-equiv = "CACHE-CONTROL"
      content    = "NO-CACHE"/>

<meta name       = "description"
      content    = "JUnit test result"/>

<meta name       = "title"
      content    = "JUnit index"/>

<title>
 JUnit index
</title>

<style>
BODY  {
 font-family: Verdana, Arial, Helvetica, sans-serif;
 font-size:11px;
 color:000000;
 text-align:left;
 font-weight:normal;
 background:#ffffff;

 margin-top: 10px;
}

.success_true {
}

.success_false {
 background:#ff7777;
}

.passed {
 background:#ffffff;
}

.failed {
 background:#ff7777;
}

.error {
 background:#ff0000;
}

TD  {
 font-family: Verdana, Arial, Helvetica, sans-serif;
 text-align:left;
 vertical-align:top;
 font-size:11px;
 background: #eeeeee;
}

TD.tableheader  {
 font-family: Verdana, Arial, Helvetica, sans-serif;
 text-align:left;
 vertical-align:top;
 font-size:11px;
 font-weight:bold;
 background: #eeeeee;
}

.errbox {
 background: #eeeeee;
}

TH  {
 font-family: Verdana, Arial, Helvetica, sans-serif;
 font-size:11px;
 color:000000;
 text-align:left;
 vertical-align:top;
 font-weight:bold
}

PRE {
 font-family: Courier New, Courier;
 font-size:11px;
 color:#000000;
 text-align:left;
 font-weight:normal;
}

code {
 font-family: Courier New, Courier;
 font-size:11px;
 color:#000000;
 text-align:left;
 font-weight:normal;
}

H1  {
 font-family: Verdana, Arial, Helvetica, sans-serif;
 font-size:19px;
 font-weight:normal;
 color: #000088;
}

H2  {
 font-family: Verdana, Arial, Helvetica, sans-serif;
 font-size:15px;
 font-weight:normal;
 color: #000088;
}

H3  {
 font-family: Verdana, Arial, Helvetica, sans-serif;
 font-size:13px;
 font-weight:normal;
 color: #000088;
}

H4  {
 font-family: Verdana, Arial, Helvetica, sans-serif;
 font-size:11px;
 color: #000088;
}

H5  {
 font-family: Verdana, Arial, Helvetica, sans-serif;
 font-size:9px;
 color: #000088;
}

.testok {
 background-color: #ffffff;
 font-size:15px;
 font-weight:normal;
 color: #000088;
}

.testfailed {
 background-color: #ffffff;
 background-color: #ffffff;
 font-size:15px;
 font-weight:bold;
 color: #000088;
}

H6  {
 font-family: Verdana, Arial, Helvetica, sans-serif;
 font-size:7px;
 font-weight:bold;
}

dt { font-weight:bold; }

dd { font-weight:normal; }

div.toc1 {  margin-left: 1em; }

div.toc2 {  margin-left: 2em; }

div.toc3 {  margin-left: 3em; }

div.exampleInner pre { margin-left: 1em; margin-top: 0em; margin-bottom: 0em; }
div.exampleOuter {border: 4px double gray; margin: 0em; padding: 0em; }

div.exampleInner { 
 background-color: #d5dee3;
 border-top-width: 4px;
 border-top-style: double;
 border-top-color: #d3d3d3;
 border-bottom-width: 4px;
 border-bottom-style: double;
 border-bottom-color: #d3d3d3;
 padding: 4px; margin: 0em;
}

div.exampleWrapper { margin: 4px;}

div.exampleHeader { font-weight: bold;  margin: 4px; }

div.schemaComp { border: 4px double gray; margin: 0em 1em; padding: 0em; }

div.compHeader { margin: 4px; font-weight: bold; }

span.schemaComp { color: #A52A2A; }                  

div.compBody { 
  border-top-width: 4px;
  border-top-style: double;
  border-top-color: #d3d3d3;
  padding: 4px ; margin: 0em;
}

.discussion {
  padding: 4px; 
  margin: 0em;
  font-style: italic;
  background-color: #eeeeee;
}
                   

</style>

</head>

<body>
<h3>Test results</h3>
 <table>
  <tr>
   <th>Test id</th>
   <th>Status</th>
   <th># of tests</th>
   <th># of errors</th>
   <th># of failures</th>
   <th>Time (ms)</th>
  </tr>
  <xsl:for-each select="/junit_index/junit">
   <tr>
    <td>
     <a href="{testcase/@id}.xml"><xsl:value-of select="testcase/@id"/></a>
    </td>
    <td class="success_{testcase/testresult/@wasSuccessful}">
     <xsl:choose>
      <xsl:when test="testcase/testresult/@wasSuccessful = 'true'">
       passed
      </xsl:when>
      <xsl:otherwise>
       failed
      </xsl:otherwise>
     </xsl:choose>
    </td>

    <td style="text-align: right;">
     <xsl:value-of select="testcase/testresult/@runCount"/>
    </td>

    <td style="text-align: right;">
     <xsl:value-of select="testcase/testresult/@errorCount"/>
    </td>

    <td style="text-align: right;">
     <xsl:value-of select="testcase/testresult/@failureCount"/>
    </td>

    <td style="text-align: right;">
     <xsl:value-of select="testcase/suite/@time"/>
    </td>
   </tr>
 </xsl:for-each>
</table>

</body>
</html>


</xsl:template>

</xsl:stylesheet>
