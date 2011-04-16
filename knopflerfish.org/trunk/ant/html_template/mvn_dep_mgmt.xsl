<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="html"/>

  <!-- 
       This is a naive Maven deployment management to HTML tranformation
       style sheet.
    -->

  <xsl:template match="/">
    <html lang="en">
      <xsl:apply-templates/>
    </html>
  </xsl:template>

  <xsl:template match="KF">
    <head>
      <meta http-equiv ="Content-Type"
            content    ="text/html; charset=ISO-8859-1"/>
      
      <meta http-equiv ="CACHE-CONTROL"
            content    ="NO-CACHE"/>
      
      <title>Knopflerfish <xsl:value-of select="@version"/> Maven Artifacts</title>
      <link rel="stylesheet" type="text/css"
	    href="http://www.knopflerfish.org./css/knopflerfish2.1.css"/>
      <link rel="shortcut icon"
	    href="http://www.knopflerfish.org./images/favicon.png"/>
    </head>

    <body>
      <div id="main">
        <a name="top"></a>
        <div id="header">
          <div id="header_logo">
            <a href="http://www.knopflerfish.org/index.html"><img src="http://www.knopflerfish.org./images/kf300_black.png" border="0" alt="Knopflerfish logo"/></a>
          </div>
	  <div id="header_ad">
	    <div class="header_stylish">
	      Open Source OSGi Service Platform Maintained by<br/>
	      <a href="http://www.makewave.com">
		<img style="margin-top: 4px;" alt="Makewave" border="0"
		     src="http://www.knopflerfish.org./images/makewave_logo.png"/>
	      </a>
	    </div>
	  </div>
	  <div class="break"></div>
	  <div id="header_menu">
	    <a class="button_closed" href="http://www.knopflerfish.org./index.html">Home</a>
	  </div>
        </div>

	<div id="leftmenu">
	  <div class="nrow1_closed"><a href="http://www.knopflerfish.org./license.html">License</a></div>
	  <div class="nrow1_closed">&#160;</div>
	  <div class="nrow1_closed"><a href="http://www.knopflerfish.org./mailman/listinfo">Mailing lists </a></div>
	  <div class="nrow1_closed"><a  href="http://sourceforge.net/tracker/?atid=567241&amp;group_id=82798&amp;func=browse">Bug tracker <img border="0" alt="" src="http://www.knopflerfish.org./images/extlink.gif"/></a></div>
	  <div class="nrow1_closed"><a href="https://sourceforge.net/forum/forum.php?forum_id=328005">Forum<img border="0" src="http://www.knopflerfish.org./images/extlink.gif" alt=""/></a></div>
	  <div class="nrow1_closed"><a href="http://knopflerfish.blogspot.com/">Knopflerfish Blog<img border="0" src="http://www.knopflerfish.org./images/extlink.gif" alt=""/></a></div>
	  <div class="makewave">Maintained by<br/>
	    <a href="http://www.makewave.com"><img src="http://www.knopflerfish.org./images/makewave_logo_126x16.gif" alt="Makewave" border="0"/></a><br/>
	    <div class="makewave_2">
	      <a href="http://www.makewave.com/site.en/products/knopflerfish_pro_osgi.shtml">Knopflerfish Pro<img border="0" alt="" src="http://www.knopflerfish.org./images/extlink.gif"/></a><br/>
	      <a href="http://www.makewave.com/site.en/products/osgi_training.shtml">OSGi Training<img border="0" alt="" src="http://www.knopflerfish.org./images/extlink.gif"/></a><br/>
	      <a href="http://www.makewave.com/site.en/products/osgi_expert.shtml">Professional Services<img border="0" alt="" src="http://www.knopflerfish.org./images/extlink.gif"/></a><br/>
	    </div>
	  </div>
	  <div class="makewave">
	    <a href="http://www.twitter.com/knopflerfish">
	      <img src="http://twitter-badges.s3.amazonaws.com/follow_us-b.png"
		   alt="Follow knopflerfish on Twitter" border="0"/>
	    </a>
	  </div>
	</div>

	<div id="mainblock">
          <h2>Knopflerfish <xsl:value-of select="@version"/> Maven Artifacts</h2>

          This page contains a listing of all bundles (artifacts) in
          the maven2 repository that belongs to Knopflerfish
          <xsl:value-of select="@version"/>.<p/>
          
          You may copy the Maven <code>&lt;dependy&gt;</code>
          elements from this file and paste them into the
          <code>&lt;dependencyManagement&gt;</code>-element of the
          <code>settings.xml</code>-file or the top-level pom-file
          for a collection of projects that uses this Knopflerfish
          version.<p/>

          <table class="fancy" cellpadding="12" style="width:100%">
            <tr class="fancy" >
              <th class="fancy">Bundle</th>
              <th class="fancy" width="200px">Description</th>
              <th class="fancy">Group id</th>
              <th class="fancy">Artifact id</th>
              <th class="fancy">Version</th>
            </tr>

	    <xsl:apply-templates select="bundles/bundle"/>

          </table>
        </div>
	<div id="footer">
	  <div id="copyright">
	    Copyright &#169; 2008-@YEAR@ The Knopflerfish Project. All
	    rights reserved.
	  </div>
	</div>
      </div>
    </body>
  </xsl:template>

  <xsl:template match="dependency">
    <tr>
      <td class="fancy">&#160;</td>
      <td class="fancy">&#160;</td>
      <td class="fancy"><xsl:value-of select="groupId"/></td>
      <td class="fancy"><xsl:value-of select="artifactId"/></td>
      <td class="fancy"><xsl:value-of select="version"/></td>
    </tr>
  </xsl:template>

  <xsl:template match="bundle">
    <tr>
      <td class="fancy"><xsl:value-of select="name"/></td>
      <td class="fancy" width="200px"><xsl:value-of select="description"/></td>
      <td class="fancy"><xsl:value-of select="groupId"/></td>
      <td class="fancy"><xsl:value-of select="artifactId"/></td>
      <td class="fancy"><a>
	  <xsl:attribute name="href"><xsl:value-of select="url"/></xsl:attribute>
	  <xsl:value-of select="version"/>
        </a>
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>
