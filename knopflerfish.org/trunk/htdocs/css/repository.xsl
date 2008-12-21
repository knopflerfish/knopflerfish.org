<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!-- 
  This is a naive OBR-XML-to-HTML tranformation style sheet 
  It should really be built from the HTML templates, instead
  of using hard-coded HTML.
  -->

<xsl:key name="category"  match="bundle" use="bundle-category"/>
<xsl:key name="apivendor" match="bundle" use="bundle-apivendor"/>
<xsl:key name="vendor"    match="bundle" use="bundle-vendor"/>

<xsl:template match="/">
  <html>
    <head>
      <meta http-equiv = "Content-Type"
	    content    = "text/html; charset=ISO-8859-1"/>
      
      <meta http-equiv = "CACHE-CONTROL"
	    content    = "NO-CACHE"/>
      
      <meta name       = "description"
	    content    = "{bundles/repository/name}"/>
      
      <meta name       = "title"
	    content    = "{bundles/repository/name}"/>

      <title><xsl:value-of select="bundles/repository/name"/></title>
      <LINK href="@@obr.css.url@@" rel="stylesheet" type="text/css"/>
    </head>

    <body>
      <div id="main">
	<a name="top"></a>
	
	<div id="header">
	  <div id="header_logo">
	    <a href="@@obr.doc.root@@/index.html"><img src="@@obr.doc.root@@/images/kf300_black.png" border="0" alt="knopflerfish OSGi logo"/></a>
	  </div>
	  <div id="header_fade"></div>
	</div>
	<div id="mainblock_obr">
	  <h2><xsl:value-of select="bundles/repository/name"/></h2>
	  <div style="font-style: italic; margin-left: 20px; margin-right: 20px; margin-top: 5px; margin-bottom: 5px;">
	    This is an OBR compatible bundle repository.
	  </div>
	  
	  <!-- start -->
	  <a href="http://www.knopflerfish.org/images/repo_desktop_host.gif"><img src="http://www.knopflerfish.org/images/repo_desktop_host_320.gif" border="0" align="right" alt="Screenshot of the KF bundlerepository, running as a plugin to the desktop"/></a>
	  <p>
	    OBR is a bundle repository format introduced by the <a href="http://oscar-osgi.sourceforge.net/repo/repository.xml">Oscar</a> OSGi project. The format consists
	    of an XML file describing all available bundles and an OSGi OBR bundle which
	    should be installed on an OSGi framework. When started, this OBR bundle can
	    read the XML file, list bundles, and install bundles and their dependencies.
	  </p>

	  <p>
	    The repository URL is <a href="@@obr.dest.url@@/repository.xml">@@obr.dest.url@@/repository.xml</a>.
	  </p>

	  <p>
	    You can find a <a href="http://bundles.osgi.org/rfc-0112_BundleRepository.pdf">RFC-0112</a> compatible repository at <a href="@@obr.dest.url@@/bindex.xml">@@obr.dest.url@@/bindex.xml</a>.
	  </p>

	  <p>
	    This repository is generated on <xsl:value-of select="bundles/repository/date"/>
	  </p>


	  <p>
	    <b>Important</b>: You need either the latest bundlerepository.jar
            from the Oscar distribution, or the KF bundlerepository bundle
            listed below. The KF bundlerepository bundle also integrates
            into the KF desktop and console.
	  </p>

	  <br clear="all"/>

	  <h4>Available bundles, sorted by API specification</h4>

	  <table class="fancy">

	    <tr class="fancy">
	      <th class="fancy">Bundle</th>
	      <th class="fancy">Description</th>
	      <th class="fancy">Version</th>
	      <th class="fancy">Links<br/></th>
	      <th class="fancy">Vendor</th>
	    </tr>

	    <xsl:for-each select="/bundles/bundle/bundle-apivendor[not(. = preceding::bundle-apivendor)]">
	      <xsl:sort select="." order="descending"/>

	      <tr>

		<td class="fancy" style="background-color: #eeeeee; font-weight: bold;" colspan="5">
		  <xsl:choose>
		    <xsl:when test=". = ''">
		      Other
		    </xsl:when>
		    <xsl:otherwise>
		      <xsl:value-of select="."/>
		    </xsl:otherwise>
		  </xsl:choose>
		</td>
	      </tr>
	      
	      <xsl:for-each select="key('apivendor',.)">
		<xsl:sort select="."/>

		<tr>

		  <td class="fancy">
		    <a href="{bundle-updatelocation}"><xsl:value-of select="bundle-name"/></a>
		  </td>

		  <td class="fancy">
		    <xsl:value-of select="bundle-description"/>
		  </td>

		  <td class="fancy">
		    <xsl:value-of select="bundle-version"/>
		  </td>

		  <td class="fancy">
		    <xsl:if test="bundle-docurl != ''">
		      <a href="{bundle-docurl}">docs</a>
		    </xsl:if>

		  </td>
		  <td class="fancy">
		    <xsl:value-of select="bundle-vendor"/>
		  </td>

		</tr>
	      </xsl:for-each>
	    </xsl:for-each>

	  </table>
	  
	  <!-- stop -->
	  <a class="top" href="#top">top</a>
	</div>
      </div>
    </body>
  </html>
</xsl:template>

</xsl:stylesheet>
