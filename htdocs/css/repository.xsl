<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!-- 
  This is a naive OBR-XML-to-HTML tranformation style sheet 
  It should really be built from the HTML templates, instead
  of using hard-coded HTML.
  -->

<xsl:template match="/">

<html>


<head>
<meta http-equiv = "Content-Type"
      content    = "text/html; charset=ISO-8859-1"/>

<meta http-equiv = "CACHE-CONTROL"
      content    = "NO-CACHE"/>

<meta name       = "description"
      content    = "{bundles/repository-bundle-name}"/>

<meta name       = "title"
      content    = "{bundles/repository-bundle-name}"/>

<title><xsl:value-of select="bundles/repository-bundle-name"/></title>
<LINK href="http://www.knopflerfish.org/css/knopflerfish.css" rel="stylesheet" type="text/css"/>

</head>

<body>
<a name="top"></a>
<table border="0" cellpadding="0" cellspacing="0">
  <tr>
    <td width="10%">
      <div style="text-align:left; margin-right: 10px; margin-left: 10px; margin-top:10 px;">
      <a href="/index.html"><img src="http://www.knopflerfish.org/images/knopflerfish-small.gif" border="0"/></a><br/>
      <p>
      <a class="navigation_enabled" href="/index.html">Home</a><br/>
      <a class="navigation_enabled" href="/components.html">Contents</a><br/>
      <a class="navigation_enabled" href="/download.html">Download</a><br/>
      <a class="navigation_enabled" href="/changelog.html">Changelog</a><br/>
      <a class="navigation_enabled" href="/contacts.html">Contacts</a><br/>
      <a class="navigation_enabled" href="/programming.html">Programming</a><br/>
      <a class="navigation_enabled" href="/desktop.html#1">Desktop</a><br/>
      <a class="navigation_enabled" href="/osgi-links.html">Links</a><br/>
      <a class="navigation_enabled" href="/history.html">History</a><br/>
      <a class="navigation_enabled"   href="/svn_info.html">Subversion repository</a><br/>

      </p>
      <img src="http://www.knopflerfish.org/images/smallfish.gif" border="0"/>
      <br/>
      </div>
    </td>
    <td width="90%">
      <table class="bodytable">
	<tr>
	  <td class="pageheader">
	    <div style="margin-left: 5px; margin-top: 5px; margin-bottom: 5px;">
	    <h2><xsl:value-of select="bundles/repository-bundle-name"/></h2>
	    <div style="font-style: italic; margin-left: 20px; margin-right: 20px; margin-top: 5px; margin-bottom: 5px;">
	    This is an OBR compatible bundle repository.
	    </div>
	    </div>
	  </td>
	</tr>
        <tr><td height="5"></td></tr>
	<tr>
	  <td class="mainblock">
	    
	    <div style="margin-left: 5px; margin-top: 5px; margin-bottom: 5px; margin-right: 5px;">

<!-- start -->

<p>
OBR is a bundle repository format introduced by the <a href="http://oscar-osgi.sourceforge.net/repo/repository.xml">Oscar</a> OSGi project. The format consists
of an XML file describing all available bundles and an OSGi OBR bundle which
should be installed on an OSGi framework. When started, this OBR bundle can
read the XML file, list bundles, and install bundles and their dependencies.
</p>

<p>
This repository is generated on <xsl:value-of select="bundles/date"/>
</p>

<p>
Note: If you're reading this text, you're looking at the XML file via an
XML stylesheet capable browser. The source for this page is however, the same
XML file as the OBR bundle reads.
</p>
<table>

 <tr>
  <th>Bundle</th>
  <th>Description</th>
  <th></th>
 </tr>

 <xsl:for-each select="bundles/bundle">
 <xsl:sort select="bundle-name"/>

 <tr>

  <td>
   <a href="{bundle-updatelocation}"><xsl:value-of select="bundle-name"/></a>
  </td>

  <td style="width: 300px;">
   <xsl:value-of select="bundle-description"/>
  </td>

  <td>
   <xsl:if test="bundle-docurl != ''">
    <a href="{bundle-docurl}">docs</a>
   </xsl:if>

   <xsl:if test="bundle-subversionurl != ''">
    -
    <a href="{bundle-subversionurl}">subversion</a>
   </xsl:if>

  </td>

 </tr>
 </xsl:for-each>

</table>

<!-- stop -->

	    <a class="top" href="#top">top</a>
	    </div>
	  </td>
	</tr>
      </table>
    </td>
  </tr>
</table>

</body>
</html>

</xsl:template>

</xsl:stylesheet>
