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

package org.knopflerfish.bundle.httpconsole;

import java.util.*;
import java.net.URL;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.osgi.framework.*;
import org.osgi.service.http.*;

public class ConsoleServlet extends HttpServlet {

  ServiceReference httpSR;

  Command[]        commands;

  // the fime install command is special
  // since it requires a multipart/form-data
  // form encoding. 
  InstallFileCommand installFileCommand;

  static final String LOGIN_USER  = "loginname";
  static final String LOGIN_PWD   = "loginpwd";
  static final String LOGIN_CMD   = "login_cmd";
  static final String LOGOUT_CMD  = "logout_cmd";

  static final String USER_OBJ = ConsoleServlet.class.getName() + ".user";

  boolean bRequireLogin = 
    "true".equals(System.getProperty("org.knopflerfish.httpconsole.requirelogin", "false"));
  
  String adminUser  = 
    System.getProperty("org.knopflerfish.httpconsole.user", "admin");
    
  String adminPwd   = 
    System.getProperty("org.knopflerfish.httpconsole.pwd",  "admin");

  boolean isValidUser(String user, String pwd) {
    return 
      adminUser.equals(user) &&
      adminPwd.equals(pwd);
  }

  
  public ConsoleServlet(ServiceReference httpSR) {
    this.httpSR = httpSR;

    commands = new Command[] {
      new ReloadCommand(),
      installFileCommand = new InstallFileCommand(),
      new IconDialogCommand(new InstallURLCommand()),
      new StartCommand(),
      new StopCommand(),
      new UpdateCommand(),
      new UninstallCommand(),
      new InfoCommand(),
      new HelpCommand(this),
      new StatusCommand(),
      new ServiceInfoCommand(),
    };

  }


  public void doPost(HttpServletRequest  request, 
		     HttpServletResponse response) 
    throws ServletException,
	   IOException 
  {
    // Handle just as GET
    doGet(request, response);
  }

  
  public void doGet(HttpServletRequest  request, 
		     HttpServletResponse response) 
    throws ServletException, IOException {
    if(iconView == null) {
      iconView = new IconView(this);
    }

    if(checkLogin(request, response)) {
      doGet2(request, response);
    }
  }


  

  

  public void doGet2(HttpServletRequest  request, 
		    HttpServletResponse response) 
    throws ServletException, IOException {

    String uapixels = request.getHeader("ua-pixels");

    int width  = Integer.MAX_VALUE;
    int height = Integer.MAX_VALUE;
    if(uapixels != null) {
      int ix = uapixels.indexOf("x");
      if(ix != -1) {
	try {
	  width  = Integer.parseInt(uapixels.substring(0, ix));
	  height = Integer.parseInt(uapixels.substring(ix + 1));
	} catch (Exception ignored) {
	  ignored.printStackTrace();
	}
      }
    }

    /*
    for(Enumeration e = request.getHeaderNames(); e.hasMoreElements();) {
      String key = (String)e.nextElement();
      String val = request.getHeader(key);
      System.out.println(key + ": " + val);
    }
    */

    PrintWriter out = response.getWriter();


    StringBuffer sb = new StringBuffer();

    try {
      handleCommands(request, sb);
    } catch (Exception e) {
      sb.append(Util.toHTML(e));
    }

    if(installFileCommand.installFile2.redir != null) {
      String base  = request.getScheme() + "://" + request.getServerName();
      int    port = request.getServerPort();
      
      if("https".equals(request.getScheme())) {
	if(port != 443) base = base + ":" + port;
      } else {
	if(port != 80) base = base + ":" + port;
      }
      String url = base + installFileCommand.installFile2.redir;
      installFileCommand.installFile2.redir = null;
      response.sendRedirect(url);
      return;
    }

    if(installFileCommand.installFile2.msg != null) {
      sb.append(installFileCommand.installFile2.msg);
      installFileCommand.installFile2.msg = null;
    }

    response.setContentType("text/html");

    if(false) {
      for(Enumeration e = request.getHeaderNames(); e.hasMoreElements();) {
	String key = (String)e.nextElement();
	String val = request.getHeader(key);
	System.out.println("header: " + key + "=" + val);
      }
      
      for(Enumeration e = request.getParameterNames(); e.hasMoreElements();) {
	String key = (String)e.nextElement();
	String val = request.getParameter(key);
	System.out.println("param: " + key + "=" + val);
      }
    }

    printHeader(out);


    Util.formStart(out, installFileCommand.isTrigger(request));
        
    try {
      out.println("<table width=\"100%\" class=\"maintable\">");


      out.println("<tr class=\"toolview\">");
      out.println("<td colspan=\"2\" class=\"toolview\">");
      printToolbar(request, out, width);
      out.println("</td>");
      out.println("</tr>");

      out.println("<tr>");
      out.println("<td class=\"mainview\">");
      printMain(request, out);
      out.println("</td>");

      out.println("<td class=\"maininfo\">");
      if(sb.length() > 0) {
	out.println("<div class=\"cmdresult\">");
	out.println(sb.toString());
	out.println("</div>");
      } else {
	out.println("<div class=\"shadow\">&nbsp;</div>");
      }
      out.println("</td>");      
      out.println("</tr>");

      out.println("<tr class=\"toolview\">");
      out.println("<td colspan=\"2\" class=\"toolview\">");
      printToolbar(request, out, width);
      out.println("</td>");
      out.println("</tr>");

      out.println("<table>");
    } catch (Exception e) {
      out.println(Util.toHTML(e));
    }

    Util.formStop(out);
    printFooter(out);

    try {
    } catch (Exception e) {
      out.println("<pre>");
      e.printStackTrace(out);
      out.println("</pre>");      
    }

  }

  IconView iconView;

  void handleCommands(HttpServletRequest request, 
		      StringBuffer       sb) throws Exception {
    for(int i = 0; i < commands.length; i++) {
      if(commands[i].isTrigger(request)) {
	StringBuffer s = commands[i].run(request);
	sb.append(s.toString());
      }
    }
  }

  void printMain(HttpServletRequest request, PrintWriter out) throws IOException {
    
    iconView.toHTML(request,out);
    
  }


  void printToolbar(HttpServletRequest request, PrintWriter out, 
		    int width) throws IOException {
    
    out.println("<div class=\"toolbar\">");
    
    for(int i = 0; i < commands.length; i++) {

      // break before status info on small screens
      if(commands[i] instanceof StatusCommand) {
	if(width < 300) {
	  out.print("<br/>");
	}
      }
      commands[i].toHTML(request, out);
    }
    
    out.println("</div>");
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

    Object userObj = session.getAttribute(USER_OBJ);
    
    if(userObj != null) {
      return true;
    }
    
    String msg  = null;
    String user = "";
    if(null != request.getParameter(LOGIN_CMD)) {
      user        = request.getParameter(LOGIN_USER);
      String pwd  = request.getParameter(LOGIN_PWD);

      if(isValidUser(user, pwd)) {
	userObj = new Object();
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

}
