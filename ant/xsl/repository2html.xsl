<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:r="http://www.osgi.org/xmlns/repository/v1.0.0"
    version="1.0">
  <xsl:output method="html"/>
  
  <xsl:template match="/">
    <html>
      <xsl:apply-templates/>
    </html>
  </xsl:template>
  
  <xsl:template match="r:repository">
    <head>
      <META HTTP-EQUIV="Content-Type"
            CONTENT="text/html; charset=iso-8859-1"/>
      <title>
        <xsl:value-of select="@name"/>
      </title>
      <link href="../../docs/css/knopflerfish.css" rel="stylesheet" type="text/css"/>
      <link href="../../docs/css/kf_man.css" rel="stylesheet" type="text/css"/>
      <style type="text/css">
        DIV.left_hdr {
        width: 225px;
        height: 100%;
        background: #000;
        margin: 0px;
        padding-top: 10px;
        padding-left: 15px;
        color: #fff;
        float:left;
        font-weight: bold;
        }

        DIV.logo_hdr {
        height: 100%;
        background: #fb0b0c;
        margin: 0px 0px 0px 225px;
        padding-top: 10px;
        padding-left: 50px;
        }

        BODY  {
        background: #fff;
        margin-top:   5px;
        margin-left:  10px;
        margin-right: 10px;
        font-size: 0.8125em;
        }
      </style>
    </head>
    <body>
      <div id="header" style="width:100%;">
        <div id="header_logo">
          <a href="index.html"><img src="../../docs/images/kf300_black.png" border="0" alt="knopflerfish logo"/></a>
        </div>
        <div id="header_centerbox">
          <div class="header_centerinfo_top">
            Distribution Documentation
          </div>
          <div class="header_centerinfo_bottom">
            <xsl:value-of select="@name"/>
          </div>
        </div>
        <div id="header_rightinfo">
          <div class="header_stylish">
            Open Source OSGi Service Platform Maintained by
          </div>
          <a href="http://www.makewave.com">
            <img border="0" alt="Makewave" src="../../docs/images/makewave_logo.png" style="margin-top: 4px;"/>
          </a>
        </div>
        <!--<div style="clear:both;"></div> -->
        <div id="header_fade">
        </div>
      </div>

      <p>Increment:     
      <xsl:value-of select="@increment"/></p>

      <table>
        <tr><th width="200px">Link</th><th>Version</th><th>Doc/Src</th><th>Bytes</th><th>Description</th></tr>
        <xsl:apply-templates>
          <xsl:sort select="r:capability[@namespace='org.knopflerfish.extra']/r:attribute[@name='name']/@value"/>
        </xsl:apply-templates>
      </table>
    </body>
  </xsl:template>
  
  <xsl:template match="r:resource">
    <tr>
      <td>
        <a href="{normalize-space(r:capability[@namespace='osgi.content']/r:attribute[@name='url']/@value)}">
          <xsl:value-of select="r:capability[@namespace='org.knopflerfish.extra']/r:attribute[@name='name']/@value"/>
        </a>    
      </td>
      <td><xsl:value-of select="r:capability[@namespace='osgi.identity']/r:attribute[@name='version']/@value"/></td>
      <td>
        <xsl:if test="r:capability[@namespace='org.knopflerfish.extra']/r:attribute[@name='documentation']/@value">
          <a href="{normalize-space(r:capability[@namespace='org.knopflerfish.extra']/r:attribute[@name='documentation']/@value)}">doc</a>
          <xsl:if test="r:capability[@namespace='org.knopflerfish.extra']/r:attribute[@name='source']/@value">/</xsl:if>
        </xsl:if>
        <xsl:if test="r:capability[@namespace='org.knopflerfish.extra']/r:attribute[@name='source']/@value">
          <a href="{normalize-space(r:capability[@namespace='org.knopflerfish.extra']/r:attribute[@name='source']/@value)}">src</a>
        </xsl:if>
      </td>
      <td>
        <xsl:value-of select="r:capability[@namespace='osgi.content']/r:attribute[@name='size']/@value"/>
      </td>
      <td>
        <xsl:value-of select="r:capability[@namespace='org.knopflerfish.extra']/r:attribute[@name='description']/@value"/>
      </td>

    </tr>
    
  </xsl:template>       
</xsl:stylesheet>
