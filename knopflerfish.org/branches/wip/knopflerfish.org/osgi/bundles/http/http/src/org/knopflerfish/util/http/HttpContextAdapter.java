/*
 * Copyright (c) 2003, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 *
 * - Neither the name of the KNOPFLERFISH project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.knopflerfish.util.http;

import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.service.http.HttpContext;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

import org.knopflerfish.service.um.useradmin.ContextualAuthorization;
import org.knopflerfish.service.um.useradmin.PasswdAuthenticator;
import org.knopflerfish.service.um.useradmin.PasswdSession;
import org.knopflerfish.service.log.LogRef;
import org.knopflerfish.util.Base64;

/*
   TODO:
   - Check the servlet 2.3 spec for login and jpassword properties
   - Default value if role == null?
   - Allways set attributes in the session? Both Basic and Digest.
     Would be a good thing if the bundle only have one way to check?
     You allways have to check the header anyway to see if the user logs
     in as someone else.
   - How do the user logout?
 */

/**
 * The HttpContextAdapter is a utility class that can be used to implement
 * UserAdmin authorization for HTTP server resources. The getResource and
 * getMimeType methods have the "default" implementations, see respective
 * method for details.
 *
 * <p>
 *
 * A simple use case is when a certain role is required (the remote user
 * must imply the role) for access to a resource. Use an instance of
 * this class as HttpContext, supply the required role in the
 * constructor. Basic HTTP authentication will be used to authenticate
 * the remote user. If the user can be authenticated and is authorized
 * for the supplied role, access to the resource is granted.
 *
 * </p><p>
 *
 * In situations where specific implementations of getResource and
 * getMimeType are desired, this class can be used as a base class to
 * simplify the authentication and authorization checks. In the following
 * example, getResource will only be called if the remote user has been
 * authenticated with Basic authentication and is member of the UserAdmin
 * group "mygroup".
 * <pre>
 * public class MyContext extends HttpContextAdapter {
 *   public MyContext( BundleContext bc ) {
 *     super( bc, null );
 *   }
 *
 *   public boolean handleSecurity(HttpServletRequest request,
 *                                 HttpServletResponse response)
 *   throws IOException
 *   {
 *     return authenticate( request, response, BASIC, "mygroup" );
 *   }
 *
 *   public URL getResource( String name ) { ... }
 *
 *   public String getMimeType( String name ) { ... }
 * }
 * </pre>
 *
 * As another example, a servlet that uses a login servlet at
 * URL "/login/index.shtml" to authenticate the remote user:
 *
 * <pre>
 * public class MyContext extends HttpContextAdapter {
 *   public boolean handleSecurity(HttpServletRequest request,
 *                                 HttpServletResponse response)
 *   throws IOException
 *   {
 *     if (!authenticate( request, response, SERVLET, "mygroup" )) {
 *       response.sendRedirect( "/login/index.shtml" );
 *       response.flushBuffer();
 *
 *       return false;
 *     }
 *
 *     return true;
 *   }
 * }
 * </pre>
 */

public class HttpContextAdapter implements HttpContext {

  /**
   * Constants for different types of authentication. Basic is
   * basic HTTP authentication as described in RFC 2617.
   */
  public static final int BASIC = 0;
  /*public*/ private static final int DIGEST = 1;  // NIY

  /**
   * Authentication using servlet code. For example, a form with
   * user name and password fields is posted to a servlet that
   * performs authentication and sets the corresponding attributes
   * on the session.
   */
  public static final int SERVLET = 2;

  private static String gosgName;

  private BundleContext bc;
  private String role;
  private LogRef log;
  private ServiceReference pwdSr;
  private PasswdAuthenticator pwd;
  private Object pwdLock = new Object();

  static {
    gosgName = System.getProperty("org.knopflerfish.gosg.name");
    if (gosgName == null) {
      gosgName = "<unknown>";
    }
  }

  /**
   * Constructs a http context with a handleSecurity method
   * that requires that the remote user has been authenticated with
   * basic authentication and is authorized for the specified role.
   *
   * @param b a bundle context
   * @param role required role, this can be null when subclassing (role
   * is provided in call to authenticate)
   */
  public HttpContextAdapter( BundleContext b, String role ) {
    this.bc = b;
    this.role = role;
    log = new LogRef( bc );

    // get the PasswdAuthenticator service, add listener
    pwdSr = bc.getServiceReference( PasswdAuthenticator.class.getName() );
    if (pwdSr != null) {
      pwd = (PasswdAuthenticator)bc.getService( pwdSr );
    }
    try {
      bc.addServiceListener( new ServiceListener() {
	  public void serviceChanged( ServiceEvent evt ) {
	    ServiceReference sr = evt.getServiceReference();
	    switch( evt.getType() ) {
	    case ServiceEvent.UNREGISTERING:
	      synchronized (pwdLock) {
		if (sr == pwdSr) {
		  bc.ungetService( pwdSr );
		  pwdSr = null;
		  pwd = null;
		}
	      }
	      break;
	    case ServiceEvent.REGISTERED:
	      if (pwdSr == null) {
		pwdSr = sr;
		pwd = (PasswdAuthenticator)bc.getService( pwdSr );
	      }
	      break;
	    }
	  }
	}, "(objectClass=" + PasswdAuthenticator.class.getName() + ")" );
    }
    catch( InvalidSyntaxException e ) {}
  }

  /**
   * Performs basic HTTP authentication and checks that the remote
   * user is authorized for the role that this adapter was initalized
   * with.
   *
   * @param request the request
   * @param response the response
   * @return true if the remote user has been authenticated and is
   * authorized for the required role
   */
  public boolean handleSecurity( HttpServletRequest request,
				 HttpServletResponse response )
    throws IOException {
    return authenticate( request, response, BASIC, role );
  }

  /**
   * The bundle associated with the bundle context that the adapter
   * was initialized with is used to fetch the named resource.
   *
   * @param name the name of the resource
   * @return an URL to the specified resource, or null if the
   * resource could not be found
   */
  public URL getResource( String name ) {
    Bundle b = bc.getBundle();
    return b.getResource( name );
  }

  /**
   * Leaves it to the HTTP service to determine MIME type, that is
   * null is returned.
   *
   * @param name the resource name
   * @return null
   */
  public String getMimeType( String name ) {
    return null;
  }

  /**
   * A convenience method to authenticate the remote user.
   *
   * @param request the request to authenticate. Will after the call
   * contain AUTHORIZATION (an Authorization object), REMOTE_USER
   * (a String) and AUTHENTICATION_TYPE (also String) if the headers of
   * the request contain the correct information to authenticate the
   * remote user (Basic HTTP authentication) or if the session contained an
   * Authorization object (Servlet), regardless of the remote user being
   * authorized as the specified role or not).
   * @param response for authentication type BASIC: if
   * autentication information from the client is not satisfactory
   * (missing or wrong user/password or user not authorized) the
   * response will be modified to contain the information necessary to
   * require Basic authorization from the client. The realm will be
   * <code>[platform name] + " " + [required role]</code>.
   * <p>
   * For authentication type SERVLET: response will not be modified,
   * it is up the caller to modify the response (if the return value
   * of the call indicate that authentication failed) to contain
   * suitable information, for exampel a "Please log in" page or a
   * redirect to the URL of a login servlet.</p>
   * @param auth_type desired authorization method, can be BASIC or SERVLET.
   * If BASIC, authentication is requested with the Unauthorized response.
   * SERVLET assumes that login is handled by a login servlet that
   * attaches an Authorization object on the session attribute AUTHORIZATION.
   * @param role role that the remote user is required to be authorized as,
   * will be forwarded to the client as realm for the authentication
   * (for auth type BASIC).
   *
   * @return true if a remote user has been authenticated and is authorized
   * for the specified role, otherwise false.
   */
  protected boolean authenticate( HttpServletRequest request,
				  HttpServletResponse response,
				  int auth_type,
				  String role ) throws IOException
  {
    Authorization auth = null;
    if( auth_type == BASIC || auth_type == DIGEST ) {
      String authHeader = request.getHeader("authorization");
      int index;
      if (authHeader != null && (index = authHeader.indexOf(' ')) != -1 ) {
	String remoteUser = null;
	String remotePasswd = null;
	String scheme = authHeader.substring( 0, index ).toLowerCase();
	String creds = authHeader.substring( index+1 );
	if (auth_type == BASIC && scheme.equals( "basic" )) {
	  creds = new String(Base64.decode(creds));
	  index = creds.indexOf(':');
	  if (index != -1) {
	    remoteUser = creds.substring(0, index);
	    remotePasswd = creds.substring(index + 1);
	  }
	}
	else if (auth_type == DIGEST && scheme.equals( "digest" )) {
	  // TBD
	  System.err.println( "Digest not supported yet!" );
	}
	if (remoteUser != null) {
	  synchronized( pwdLock ) {
	    if (pwd != null) {
	      PasswdSession ps = pwd.createSession();
	      ps.setUsername( remoteUser );
	      ps.setPassword( remotePasswd );
	      ContextualAuthorization ca = ps.getAuthorization();
	      if (ca != null) {
		String authMethod = "passwd"; //TBD
		String inputPath = request.getScheme();
		ca.setIPAMContext(inputPath, authMethod);
		auth = ca;
		// user authenticated ok, set request attributes
		request.setAttribute( REMOTE_USER, ca.getName() );
		request.setAttribute( AUTHENTICATION_TYPE, scheme );
		request.setAttribute( AUTHORIZATION, ca );
	      }
	      else {
		// username or password wrong
		if (log.doInfo()) log.log( LogService.LOG_INFO,
		      "Authentication failed for user " + remoteUser + ".");
	      }
	    }
	    else {
	      // no password authenticator
	      if (log.doWarn()) log.log( LogService.LOG_WARNING,
		    "Authentication failed, PasswdAuthenticator not " +
		    "available." );
	    }
	  } // synchronized uaLock
	}
	else {
	  // auth_type not matching header or could not extract name, password
	  if (log.doWarn()) log.log( LogService.LOG_WARNING,
		"Authentication failed, wrong auth type or malformed auth " +
				     "info.");
	}
      }
      else {
	// auth_type is basic or digest, no auth info in request
	if (log.doDebug()) log.log( LogService.LOG_DEBUG,
	      "Request without Basic or Digest authentication header.");
      }
    }
    else if (auth_type == SERVLET) {
      HttpSession session = request.getSession();
      auth = (Authorization)session.getAttribute(AUTHORIZATION);
      if (auth != null ) {
	request.setAttribute( REMOTE_USER, auth.getName() );
	request.setAttribute( AUTHENTICATION_TYPE,
			      session.getAttribute(AUTHENTICATION_TYPE) );
	request.setAttribute( AUTHORIZATION, auth );
      }
      else {
	// No auth info in session
	if (log.doDebug()) log.log( LogService.LOG_DEBUG,
	      "Request without authorization in session.");
      }
    }

    if (auth != null) {
      if (auth.hasRole( role )) {
	if (log.doInfo()) log.log( LogService.LOG_INFO,
	      "User " +auth.getName()+ " was authenticated ok and " +
	      "authorized as "+ role + ".");
	return true;
      }
      else {
	if (log.doInfo()) log.log( LogService.LOG_INFO,
	      "User " +auth.getName()+ " does not impy " +role+ ".");
      }
    }
    if (auth_type == BASIC | auth_type == DIGEST) {
      String scheme = auth_type == BASIC ? "BASIC " : "DIGEST ";
      String realm = role + " at " + gosgName;
      response.setHeader("WWW-Authenticate", scheme + "realm=\"" +realm+ "\"");
      response.sendError( HttpServletResponse.SC_UNAUTHORIZED );
      response.flushBuffer();
      if (log.doDebug()) log.log( LogService.LOG_DEBUG,
	    "Response set to WWW-Authenticate:"+scheme+ "realm=\"" +realm+
	    "\"");
    }

    return false;
  }
}
