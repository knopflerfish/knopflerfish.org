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
			<link href="http://www.osgi.org/www/osgi.css" type="text/css" rel="stylesheet"/>
		</head>
		<body>
			<h1>
				<xsl:value-of select="@name"/>
			</h1>
		<p>Increment: 	
				<xsl:value-of select="@increment"/></p>

			<table>
				<tr><th width="200px">Link</th><th>Version</th><th>Doc/Src</th><th>Bytes</th><th>Description</th></tr>
				<xsl:apply-templates matcht="r:resource">
				<!-- Don't think this ever worked -->
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
