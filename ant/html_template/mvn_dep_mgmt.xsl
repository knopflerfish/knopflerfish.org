<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="html"/>

  <!-- 
       This is a naive Maven deployment management to HTML tranformation
       style sheet.
    -->

  <xsl:template match="/">
    <html>
      <xsl:apply-templates/>
    </html>
  </xsl:template>

  <xsl:template match="KF">
    <head>
      <meta http-equiv = "Content-Type"
            content    = "text/html; charset=ISO-8859-1"/>
      
      <meta http-equiv = "CACHE-CONTROL"
            content    = "NO-CACHE"/>
      
      <title>Knopflerfish <xsl:value-of select="@version"/> Maven Artifacts</title>
      <LINK href="http://www.knopflerfish.org/css/knopflerfish2.css" rel="stylesheet" type="text/css"/>
    </head>

    <body>
      <div id="main">
        <a name="top"></a>
        
        <div id="header">
          <div id="header_logo">
            <a href="http://www.knopflerfish.org/index.html"><img src="http://www.knopflerfish.org/images/kf300_black.png" border="0" alt="knopflerfish OSGi logo"/></a>
          </div>
          <div id="header_fade"></div>
        </div>
        <div id="mainblock_mvn">
          <h2>Knopflerfish <xsl:value-of select="@version"/> Maven Artifacts</h2>
          <div style="margin-left: 20px; margin-right: 20px; margin-top: 5px; margin-bottom: 5px;">
            This page contains a listing of all artifacts in the
            maven2 repository that belongs to Knopflerfish
            <xsl:value-of select="@version"/>.<p/>
            
            You may copy the Maven <code>&lt;dependy&gt;</code>
            elements from this file and paste them into the
            <code>&lt;dependencyManagement&gt;</code>-element of the
            <code>settings.xml</code>-file or the top-level pom-file
            for a collection of projects that uses this Knopflerfish
            version.

          </div>
          
          <br clear="all"/>

          <table class="fancy">

            <tr class="fancy">
              <th class="fancy">Group id</th>
              <th class="fancy">Artifact id</th>
              <th class="fancy">Version</th>
            </tr>

	    <xsl:apply-templates select="dependencyManagement/dependencies/dependency"/>

          </table>
          
          <!-- stop -->
          <a class="top" href="#top">top</a>
        </div>
      </div>
    </body>
  </xsl:template>

  <xsl:template match="dependency">
    <tr>
      <td class="fancy"><xsl:value-of select="groupId"/></td>
      <td class="fancy"><xsl:value-of select="artifactId"/></td>
      <td class="fancy"><xsl:value-of select="version"/></td>
    </tr>
  </xsl:template>

</xsl:stylesheet>
