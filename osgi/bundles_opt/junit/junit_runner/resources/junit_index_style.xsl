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
  Knopflerfish Integraton Tests - Test Index
</title>

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

  <h1 class="kf">Knopflerfish Junit Integration Test Results</h1>
 
<h2 class="kf">Exectuted Test Runs</h2>

<p>
  <table class="kf">
    <tr>
      <th>Name</th>
      <th>Description</th>
      <th>Mode</th>
      <th># of test suites</th>
      <th># of failures</th>
      <th># not executed</th>      
    </tr>
    <xsl:for-each select="/knopflerfish_integration_tests/junit_run">
    <tr>
      <td> <xsl:value-of select="@name"/> </td>
      <td> <xsl:value-of select="@description"/> </td>
      <td> <xsl:value-of select="summary/@testMode" /> </td>
      <td> <xsl:value-of select="summary/@testSuitesExecuted"/></td>
      <td> <xsl:value-of select="summary/@testSuitesFailed"/></td>
      <td> <xsl:value-of select="summary/@testSuitesNotExecuted"/> </td>
    </tr>
    </xsl:for-each>
  </table>
</p>

<h2 class="kf">Test Suites</h2>
<p>
 <table class="kf">
  <tr>
   <th>Test run</th>
   <th>Test id</th>
   <th>Status</th>
   <th># of tests</th>
   <th># of errors</th>
   <th># of failures</th>
   <th>Time (ms)</th>
   <th>Date</th>
  </tr>
  <xsl:for-each select="/knopflerfish_integration_tests/junit_run">
    <xsl:for-each select="junit">
      <xsl:sort select="testcase/@id"/>
      <tr>
	<td> <xsl:value-of select="../@name" /> </td>
	<td>
	  <a href="{../@name}-{testcase/@id}.xml"><xsl:value-of select="testcase/@id"/></a>
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

	<td>
	  <xsl:value-of select="@date"/>
	</td>
      </tr>
    </xsl:for-each>
  </xsl:for-each>
</table>
</p>

<p>
  The raw log from <a href="out.txt">System.out/err</a> (all test runs)
</p>

<p>
  <h3 class="kf">Test Suites Disabled</h3>
  <table>
    <xsl:for-each select="/junit_index/junit_not_executed/suite">
      <xsl:sort select="@name"/>
      <tr>
	<td> <xsl:value-of select="@name"/></td>
      </tr>
     </xsl:for-each>
  </table>
</p>

</body>
</html>


</xsl:template>

</xsl:stylesheet>
