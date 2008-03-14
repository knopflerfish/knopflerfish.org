package org.knopflerfish.axis2.impl;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.util.Loader;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.deployment.repository.util.ArchiveReader;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisConfigurator;
import org.apache.axis2.transport.http.HTTPConstants;

import org.apache.axis2.deployment.*;

import javax.servlet.ServletConfig;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.knopflerfish.service.log.LogRef;

/**
 * Processes the init parameters for the AxisServlet.
 * This allows the location of the axis2.xml and the module repository
 * to be different from the default locations.
 * The init parameters support alternate file, or URL values for both of these.
 */
public class OSGiConfigurator
  extends DeploymentEngine implements AxisConfigurator
{

  private static final LogRef log = Activator.log;
  private ServletConfig config;

  /**
   * The name of the init parameter (axis2.xml.path) that can be used
   * to override the default location for the axis2.xml file. When
   * both this init parameter, and the axis2.xml.url init parameters
   * are not specified in the axis servlet init-parameter, the default
   * location of ${app}/WEB-INF/conf/axis2.xml is used.
   *
   * The value of this path is interpreted as a file system absolute path.
   * This parameter takes precedence over the axis2.xml.url init parameter.
   */
  public static final String PARAM_AXIS2_XML_PATH = "axis2.xml.path";


  /**
   * The name of the init parameter (axis2.xml.url) that when
   * specified indicates the axis2.xml should be loaded using the URL
   * specified as the value of this init parameter. If the
   * axis2.xml.path init parameter is present, this init parameter has
   * no effect.
   */
  public static final String PARAM_AXIS2_XML_URL = "axis2.xml.url";


  /**
   * The name of the init parameter (axis2.repository.path) that when
   * specified indicates the path to the
   */
  public static final String PARAM_AXIS2_REPOSITORY_PATH = "axis2.repository.path";


  /**
   * The name of the init parameter (axis2.repository.url) that when specified indicates the url to be used
   */
  public static final String PARAM_AXIS2_REPOSITORY_URL = "axis2.repository.url";


  public static final String  BUNDLE_AXIS2_CONFIGURATION_RESOURCE = "/WEB-INF/conf/axis2.xml";

  /**
   * Default constructor for configurator.
   *
   * This determines the axis2.xml file to be used from the init
   * parameters for the AxisServlet in the web.xml.
   * The order of initialization is according the the following precedence:
   * <ul>
   * <li>If the parameter axis2.xml.path is present, the value is
   *     webapp relative path to be used as the location to the axis2.xml
   *     file.
   * <li>Otherwise, if the parameter axis2.xml.url is present, the URL
   *     is used as the location to the axis2.xml file.
   * <li>Otherwise, when both of the above init parameters are not
   *     present, file is attempted to be loaded from
   *     &lt;repo&gt;/WEB-INF/axis2.xml.
   * <li> When none of the above could be found, the axis2.xml is
   *      loaded from the classpath resource, the value of
   *      DeploymenConstants.AXIS2_CONFIGURATION_RESOURCE.
   * </ul>
   *
   * @param servletConfig the ServletConfig object from the
   *                      AxisServlet. This method is called from the
   *                      init() of the AxisServlet.
   */
  public OSGiConfigurator(ServletConfig servletConfig)
    throws DeploymentException
  {
    try {
      this.config = servletConfig;
      InputStream axis2Stream = null;

      log.debug("use bundle axis config "
               + BUNDLE_AXIS2_CONFIGURATION_RESOURCE + " from classpath");
      axis2Stream =
        Loader.getResourceAsStream(BUNDLE_AXIS2_CONFIGURATION_RESOURCE);


      log.debug("axis2Stream=" + axis2Stream);

      axisConfig = populateAxisConfiguration(axis2Stream);
      axisConfig.setSystemClassLoader(Activator.class.getClassLoader());

      if(axis2Stream != null){
        axis2Stream.close();
      }
      Parameter param = new Parameter();
      param.setName(Constants.Configuration.ARTIFACTS_TEMP_DIR);
      param.setValue(config.getServletContext().getAttribute("javax.servlet.context.tempdir"));

      try {
        axisConfig.addParameter(param);
      } catch (AxisFault axisFault) {
        log.error(axisFault.getMessage(), axisFault);
      }

      // Load module repository from /WEB-INF/modules
      URL url = this.getClass().getResource("/");
      if (url != null) {
        log.debug("start: load repository from "+url);
        loadRepositoryFromURL(url);
        log.debug("done:  load repository from "+url);
      } else {
        log.warn("Could not find /");
      }

      // when the module is an unpacked war file,
      // we can set the web location path in the deployment engine.
      // This will let us
      String webpath = config.getServletContext().getRealPath("");
      if (webpath != null && !"".equals(webpath)) {
        log.debug("setting web location string: " + webpath);
        File weblocation = new File(webpath);
        setWebLocationString(weblocation.getAbsolutePath());
      } // if webpath not null

    } catch (DeploymentException e) {
      log.error(e.getMessage(), e);
      throw e;
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    axisConfig.setConfigurator(this);
  }

  public AxisConfiguration getAxisConfiguration() throws AxisFault {
    return axisConfig;
  }

  /**
   * Gets the axis configuration object by loading the repository.
   * The order of initialization is according the the following precedence:
   * <ul>
   * <li>If the parameter axis2.repository.path is present, this folder is used as the location to the repository.
   * <li>Otherwise, if the parameter axis2.repository.url is present, the URL is used as the location to the repository.
   * <li>Otherwise, when both of the above init parameters are not present, the web applications WEB-INF folder is used as the folder for the repository.
   * </ul>
   *
   * @return the instance of the AxisConfiguration object that reflects the repository according to the rules above.
   * @throws AxisFault when an error occurred in the initialization of the AxisConfiguration.
   */
  public AxisConfiguration xgetAxisConfiguration() throws AxisFault {
    try {
      String repository = null;

      if (repository == null) {
        repository = config.getInitParameter(PARAM_AXIS2_REPOSITORY_PATH);
        if (repository != null) {
          loadRepository(repository);
          log.debug("loaded repository from path: " + repository);
        }
      }

      if (repository == null) {
        repository = config.getInitParameter(PARAM_AXIS2_REPOSITORY_URL);
        if (repository != null) {
          loadRepositoryFromURL(new URL(repository));
          log.debug("loaded repository from url: " + repository);
        }
      }

      if (repository == null) {
        if (config.getServletContext().getRealPath("") != null) {
          // this is an unpacked war file
          repository = config.getServletContext().getRealPath("/WEB-INF");
        }
        if (repository != null) {
          loadRepository(repository);
          log.debug("loaded repository from /WEB-INF folder (unpacked war)");
        }
      }
      if (repository == null) {
        URL url = this.getClass().getResource("/");
        if (url != null) {
          repository = url.toString();
          loadRepositoryFromURL(url);
          log.debug("loaded repository from "+url);
        }
      }


      if (repository == null) {
        loadFromClassPath();
        log.debug("loaded repository from classpath");
      }

    } catch (Exception ex) {
      log.error(ex + ": loading repository from classpath", ex);
      loadFromClassPath();
    }
    axisConfig.setConfigurator(this);
    return axisConfig;
  }

  public void loadServices() {
    log.debug("loadServices()");
  }

  //To engage globally listed modules
  public void engageGlobalModules() throws AxisFault {
    engageModules();
  }

  public void setConfigContext(ConfigurationContext configContext) {
    super.setConfigContext(configContext);

    // setting ServletContext into configctx
    configContext.setProperty(HTTPConstants.MC_HTTP_SERVLETCONTEXT,
                              config.getServletContext());
    Parameter servletConfigParam = new Parameter();
    servletConfigParam.setName(HTTPConstants.HTTP_SERVLETCONFIG);
    servletConfigParam.setValue(config);
    try {
      configContext.getAxisConfiguration().addParameter(servletConfigParam);
    } catch (AxisFault axisFault) {
      log.error(axisFault.getMessage(), axisFault);
    }
  }
}
