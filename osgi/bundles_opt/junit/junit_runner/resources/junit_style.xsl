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
      content    = "JUnit test resuls for id={/junit/testcase/@id}"/>

<title>
 JUnit test resuls for id=<xsl:value-of select="/junit/testcase/@id"/>
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

.passed {
 background:#ffffff;
}

.failed {
 background:#ff7777;
}

.error {
 background:#ff7777;
}

TD  {
 font-family: Verdana, Arial, Helvetica, sans-serif;
 text-align:left;
 vertical-align:top;
 font-size:11px;
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

 

 <xsl:for-each select="/junit/testcase">
  <h3>JUnit test results for id '<xsl:value-of select="@id"/>'</h3>
  <xsl:value-of select="/junit/@date"/>
<br/>
<a href="#properties">Properties</a>
<p>
  <xsl:for-each select="testresult">
   <xsl:choose>
    <xsl:when test="@wasSuccessful = 'true'">
     Passed all tests
    </xsl:when>
    <xsl:otherwise>
     <b>Some tests failed</b>
    </xsl:otherwise>
   </xsl:choose>
   <table>
    <tr>
     <td># of tests</td>
     <td><xsl:value-of select="@runCount"/></td>
    </tr>
    <tr>
     <td># of failures</td>
     <td><xsl:value-of select="@failureCount"/></td>
    </tr>
    <tr>
     <td># of errors</td>
     <td><xsl:value-of select="@errorCount"/></td>
    </tr>
   </table>
  </xsl:for-each>
</p>

  <xsl:for-each select="suite">
   <pre>
Class: <xsl:value-of select="@class"/>
Time:  <xsl:value-of select="@time"/>ms
   </pre>

   <h4>Test cases</h4>
   <table>
    <tr>
     <th>Name</th>
     <th>Status</th>
    </tr>
    <xsl:for-each select="case">
     <tr>
      <td>
<xsl:choose>
 <xsl:when test="@status != 'passed'">
  <a href="#{@name}"><xsl:value-of select="@name"/></a>
 </xsl:when>
 <xsl:otherwise>
  <xsl:value-of select="@name"/>
 </xsl:otherwise>
</xsl:choose>
</td>	
      <td class="{@status}"><xsl:value-of select="@status"/></td>
     </tr>
    </xsl:for-each>
   </table> 

  </xsl:for-each>

  <xsl:for-each select="testresult">

   <xsl:for-each select="errors">
    <h3>Errors</h3>
    <xsl:for-each select="failure">
     <a name="{@failedTestCaseName}"></a>
     <div class="schemaComp">
      <div class="compHeader">
       <span class="schemaComp">
        Error: <xsl:value-of select="@failedTestCaseName"/> - <xsl:value-of select="@exceptionMessage"/>
       </span>
       <div class="compBody">
        <code>
         <xsl:value-of select="trace"/>
        </code>
       </div>
      </div>
     </div>
     <br/>
    </xsl:for-each>
   </xsl:for-each>

   <xsl:for-each select="failures">
    <h3>Failures</h3>
    <xsl:for-each select="failure">
     <a name="{@failedTestCaseName}"></a>
     <div class="schemaComp">
      <div class="compHeader">
       <span class="schemaComp">
        Failure: <xsl:value-of select="@failedTestCaseName"/> - <xsl:value-of select="@exceptionMessage"/>
       </span>
       <div class="compBody">
        <code>
         <xsl:value-of select="trace"/>
        </code>
       </div>
      </div>
     </div>
     <br/>
    </xsl:for-each>
   </xsl:for-each>

  </xsl:for-each>

 </xsl:for-each>

 <a name="properties"></a>
 <xsl:for-each select="/junit/properties">
  <h3><xsl:value-of select="@name"/></h3>
  <a name="{@name}"></a>
  <table>
   <xsl:for-each select="value">
    <tr>
     <td><xsl:value-of select="@key"/></td>
     <td><xsl:value-of select="text()"/></td>
    </tr>
   </xsl:for-each>
  </table>
 </xsl:for-each>

</body>
</html>

</xsl:template>

</xsl:stylesheet>
