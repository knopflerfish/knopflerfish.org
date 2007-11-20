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

<LINK href="junit.css" rel="stylesheet" type="text/css"/>

</head>

<body>
<h3 class="shadow">Test results</h3>


 <table>
  <tr>
   <th>Test id</th>
   <th>Status</th>
   <th># of tests</th>
   <th># of errors</th>
   <th># of failures</th>
   <th>Time (ms)</th>
   <th>Date</th>
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

    <td>
     <xsl:value-of select="@date"/>
    </td>

   </tr>
 </xsl:for-each>
</table>

<p>
 <a href="out.txt">System.out/err</a>
</p>

</body>
</html>


</xsl:template>

</xsl:stylesheet>
