<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <!-- 
       This is a semi-naive JUnit XML formatting stylesheet.
  -->

<xsl:template match="/">

<!--
  <xsl:template match="suite">
    <pre>
      Class: <xsl:value-of select="@class"/>
      Time:  <xsl:value-of select="@time"/>ms
    </pre>
    
    <h4 class="shadow">Test cases</h4>
    <table>
      <tr>
	<th>Name</th>
	<th>Status</th>
      </tr>
      
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
	<td><xsl:value-of select="description"/></td>
      </tr>
    </table>
  </xsl:template>
-->

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

<!-- <LINK href="junit.css" rel="stylesheet" type="text/css"/> -->
<LINK href="css/knopflerfish.css" rel="stylesheet" type="text/css"/>
<LINK href="junit.css" rel="stylesheet" type="text/css"/>

<style type="text/css">
  BODY  {
  background: #fff;
  margin-top:   5px;
  margin-left:  10px;
  margin-right: 10px;
  font-size: 0.8125em;
  font-family: Lucida Grande, Tahoma, Verdana, Helvetica, sans-serif;
  }
</style>
</head>

<body>

  <div id="header" style="width:100%;">
    <div id="header_logo">
      <a href="index.html"><img src="images/kf300_black.png" border="0" alt="knopflerfish logo"/></a>
    </div>
    <div id="header_centerbox">
      <div class="header_centerinfo_top">
        Distribution Documentation
      </div>
      <div class="header_centerinfo_bottom">
        Knopflerfish Integration Tests
      </div>
    </div>
    <div id="header_rightinfo">
      <div class="header_stylish">
        Open Source OSGi Service Platform Maintained by
      </div>
      <a href="http://www.makewave.com">
        <img border="0" alt="Makewave" src="images/makewave_logo.png" style="margin-top: 4px;"/>
      </a>
    </div>
    <!--<div style="clear:both;"></div> -->
    <div id="header_fade">
    </div>
  </div>

  <xsl:for-each select="/junit/testcase">
  <h1 class="kf">JUnit test results for id '<xsl:value-of select="@id"/>'
  - 
<xsl:value-of select="/junit/@date"/>
  </h1>



<a href="index.xml">Test index</a><br/>
<a href="#properties">Properties</a>
<p>
<xsl:value-of select="description"/>
<xsl:choose>
 <xsl:when test="docurl = ''">
 </xsl:when>
 <xsl:otherwise>
  (see also <a href="{docurl}">docs</a>) <br/>
 </xsl:otherwise>
</xsl:choose>
</p>
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
    <a name="{@name}"></a>
    <h2 class="kf">Test Suite: <xsl:value-of select="@name"/></h2>
   <pre>
Class: <xsl:value-of select="@class"/>
Time:  <xsl:value-of select="@time"/>ms
   </pre>

   <h3 class="kf">Test cases</h3>
   <table class="kf">
    <tr>
     <th>Name</th>
     <th>Status</th>
     <th>Reference</th>
     <th>Description</th>
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
      <td>
      	<xsl:for-each select="ref">
	  <a href="#{@refname}"><xsl:value-of select="@refname"/></a><br/>
	</xsl:for-each>
      </td>
      <!-- <a href="#{@ref}"><xsl:value-of select="@ref"/></a></td> -->
      <td><xsl:value-of select="@description"/></td>
     </tr>
    </xsl:for-each>

    <xsl:for-each select="suitecase">
     <tr>
      <td><a href="#{@name}">	<xsl:value-of select="@name"/></a></td>	
      <td>suite</td>
      <td>
	<xsl:for-each select="ref">
	  <a href="#{@refname}"><xsl:value-of select="@refname"/></a><br/>
	</xsl:for-each>
      </td>
      <td><xsl:value-of select="@description"/></td>
     </tr>
    </xsl:for-each>
   </table> 

  </xsl:for-each>

  <xsl:for-each select="testresult">

   <xsl:for-each select="errors">
    <h3 class="kf">Errors</h3>
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
    <h3 class="kf">Failures</h3>
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
  <h3 class="kf"><xsl:value-of select="@name"/></h3>
  <p>
  <a name="{@name}"></a>
  <table class="kf">
   <xsl:for-each select="value">
    <tr>
     <td><xsl:value-of select="@key"/></td>
     <td><xsl:value-of select="text()"/></td>
    </tr>
   </xsl:for-each>
  </table>
  </p>
</xsl:for-each>

</body>
</html>

</xsl:template>

</xsl:stylesheet>
