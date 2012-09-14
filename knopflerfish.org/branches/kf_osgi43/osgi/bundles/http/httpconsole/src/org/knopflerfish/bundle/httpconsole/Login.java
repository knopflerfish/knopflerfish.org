/*
 * Copyright (c) 2003-2008, KNOPFLERFISH project
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

package org.knopflerfish.bundle.httpconsole;

import java.util.*;
import java.net.URL;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.osgi.framework.*;
import org.osgi.service.http.*;

public class Login {
  static final String LOGIN_USER  = "loginname";
  static final String LOGIN_PWD   = "loginpwd";
  static final String LOGIN_CMD   = "login_cmd";
  static final String LOGOUT_CMD  = "logout_cmd";

  static final String USER_OBJ = ConsoleServlet.class.getName() + ".user";

  boolean bRequireLogin = false;
  String adminUser  = "admin";
  String adminPwd   = "admin";
  long expirationTime = 60 * 10; // time in seconds

  public Login(BundleContext bc) {
    String bRequireLoginS
      = bc.getProperty("org.knopflerfish.httpconsole.requirelogin");
    if (null!=bRequireLoginS && bRequireLoginS.length()>0) {
      bRequireLogin = "true".equals(bRequireLoginS);
    }

    String adminUserS = bc.getProperty("org.knopflerfish.httpconsole.user");
    if (null!=adminUserS && adminUserS.length()>0) {
      adminUser = adminUserS;
    }

    String adminPwdS = bc.getProperty("org.knopflerfish.httpconsole.pwd");
    if (null!=adminPwdS && adminPwdS.length()>0) {
      adminPwd = adminPwdS;
    }

    try {
      String expirationTimeS
        = bc.getProperty("org.knopflerfish.httpconsole.expirationtime");
      if (null!=expirationTimeS && expirationTimeS.length()>0) {
        expirationTime = Long.parseLong(expirationTimeS);
      }
    } catch (Exception e) {
      Activator.log.warn("Bad expiration time", e);
    }
  }

  boolean isValidUser(String user, String pwd) {
    return
      adminUser.equals(user) &&
      adminPwd.equals(pwd);
  }

  /**
   * Check if the use has been logged in. If not, print a login page
   */
  boolean checkLogin(HttpServletRequest  request,
                     HttpServletResponse response) throws IOException {

    if(!bRequireLogin) {
      return true;
    }

    HttpSession session = request.getSession();

    if(null != request.getParameter(LOGOUT_CMD)) {
      session.removeAttribute(USER_OBJ);
    }

    String     msg     = null;
    UserObject userObj = null;

    // Catch ClassCastException since the bundle itself
    // may have been updated since the last request. If this
    // happens, the http server may very well still hold on
    // to a class named "UserObject", but which actually isn't
    // the same class as the new, update bundle expects.
    try {
      userObj = (UserObject)session.getAttribute(USER_OBJ);
    } catch (ClassCastException e) {
      msg = "console updated/session expired";
      session.removeAttribute(USER_OBJ);
    }

    if(userObj != null) {
      if(userObj.hasExpired()) {
        session.removeAttribute(USER_OBJ);
        msg = "Session has expired";
      } else {
        userObj.touch();
        return true;
      }
    }


    String user = "";
    if(null != request.getParameter(LOGIN_CMD)) {
      user        = request.getParameter(LOGIN_USER);
      String pwd  = request.getParameter(LOGIN_PWD);

      if(isValidUser(user, pwd)) {
        userObj = new UserObject();
        session.setAttribute(USER_OBJ, userObj);
        return true;
      }
      msg = "Login failed";
    }

    if(user == null) {
      user = "";
    }
    PrintWriter out = response.getWriter();

    printHeader(out);

    Util.formStart(out, false);

    out.println("<h4 style=\"margin: 3px;\" class=\"shadow\">Log in to HTTP console</h4>");

    out.println("<div style=\"margin: 5px;\">");

    if(msg != null) {
      out.println("<div class=\"loginerror\">");
      out.println(msg);
      out.println("</div>");
    }

    out.println("<table>");

    out.println(" <tr>");
    out.println("  <td>Login name</td>");
    out.println("  <td>");
    out.println("  <input type=\"text\" name=\"" + LOGIN_USER + "\"" +
                " value=\"" + user + "\">");
    out.println("  </td>");
    out.println(" </tr>");

    out.println("  <td>Password</td>");
    out.println("  <td>");
    out.println("  <input type=\"password\" name=\"" + LOGIN_PWD + "\">");
    out.println("  </td>");
    out.println(" </tr>");

    out.println(" <tr>");
    out.println("  <td><input name=\"" + LOGIN_CMD + "\" type=\"submit\" value=\"Login\"></td>");
    out.println("  <td>");
    out.println("  ");
    out.println("  </td>");
    out.println(" </tr>");

    out.println("</table>");

    out.println("</form>");


    out.println("</div>");
    out.println("<div class=\"shadow\">&nbsp;</div>");

    printFooter(out);

    return false;
  }

  void printHeader(PrintWriter out) throws IOException {
    out.println("<html>");
    out.println("<head>");
    out.println("<title>Knopflerfish OSGi console</title>");

    out.println("<LINK href=\"" + Activator.RES_ALIAS + "/console.css\" rel=\"stylesheet\" type=\"text/css\">");
    out.println("</head>");
    out.println("<body>");

  }

  void printFooter(PrintWriter out) throws IOException {
    out.println("</body>");
    out.println("</html>");
  }


  /**
   * Simple object for storing user info in an HttpSession.
   * The HttpSession.setMaxInactiveInterval() could have been
   * used instead of the custom timer...but then it would be
   * hard to give a "session expired" message at timeouts.
   */
  class UserObject {
    long touchTime;

    public UserObject() {
      touch();
    }

    public void touch() {
      touchTime = System.currentTimeMillis();
    }

    public boolean hasExpired() {
      return
        (System.currentTimeMillis() - touchTime) > ( 1000 * expirationTime);
    }
  }
}
