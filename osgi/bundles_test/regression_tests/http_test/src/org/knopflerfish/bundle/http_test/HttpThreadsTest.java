package org.knopflerfish.bundle.http_test;

import junit.framework.TestCase;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import static junit.framework.Assert.assertTrue;

/**
 * Test for github issue 58 (Http Server: threads created in transactions increase activeCount
 * and server will eventually stop processing requests).
 */
public class HttpThreadsTest extends TestCase {
  private final BundleContext bc;

  // Not used
  public HttpThreadsTest() {
    this.bc = null;
  }

  public HttpThreadsTest(BundleContext bc) {
    this.bc = bc;
  }

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }

  public void runTest() throws Throwable {
    assertNotNull(bc);
    ServiceReference<HttpService> serviceReference = bc.getServiceReference(HttpService.class);
    assertNotNull("Failed to getServiceReference HttpService", serviceReference);
    HttpService httpService = bc.getService(serviceReference);
    assertNotNull("Failed to getService HttpService", httpService);

    String alias = "/threads.html";
    httpService.registerServlet(
        alias, new TestServlet(25, 1000), new Hashtable<>(), new TestHttpContext()
    );

    String host = getHost(serviceReference);
    assertNotNull("No host set on http server registration", host);
    String port = getPort(serviceReference);
    assertNotNull("No port set on http server registration", port);

    System.out.println("connection.max: " + getProperty(serviceReference, "connection.max"));

    final URL url = new URL("http://" + host + ":" + port + alias);
    List<CallerThread> callerThreads = Arrays.asList(
        new CallerThread(url), new CallerThread(url), new CallerThread(url), new CallerThread(url)
    );
    final long startTime = System.currentTimeMillis();
    callerThreads.forEach(CallerThread::delayedStart);
    callerThreads.forEach(CallerThread::joinOrDie);
    final long timeSpent = System.currentTimeMillis() - startTime;
    assertTrue("The calls took " + timeSpent + " ms.", timeSpent < 2000);
    System.out.println("The calls took " + timeSpent + " ms.");

    for (CallerThread callerThread : callerThreads) {
      checkResult(callerThread);
    }
  }

  private void checkResult(CallerThread callerThread) throws Exception {
    if (callerThread.getResult() == null) {
      assertNotNull(callerThread.getError());
      throw callerThread.getError();
    }
  }

  private String getHost(ServiceReference<HttpService> serviceReference) {
    return getProperty(serviceReference, "host");
  }

  private String getPort(ServiceReference<HttpService> serviceReference) {
    return getProperty(serviceReference, "port.http", "openPort");
  }

  private String getProperty(ServiceReference<HttpService> serviceReference, String... propertyNames) {
    for (String propertyName : propertyNames) {
      Object property = serviceReference.getProperty(propertyName);
      if (property != null) {
        return property.toString();
      }
    }
    return null;
  }
}

class CallerThread extends Thread {
  private URL url;
  private String result;
  private Exception error;

  public CallerThread(URL url) {
    this.url = url;
  }

  public void joinOrDie() {
    try {
      join();
    } catch (InterruptedException ignored) {
    }
  }

  public void delayedStart() {
    try {
      sleep(100);
    } catch (InterruptedException ignored) {
    }
    start();
  }

  public String getResult() {
    return result;
  }

  public Exception getError() {
    return error;
  }

  @Override
  public void run() {
    try {
      result = getFromUrl(url);
    } catch (IOException e) {
      error = e;
    }
  }

  private String getFromUrl(URL url) throws IOException {
    StringBuilder output = new StringBuilder();
    try (InputStream is = url.openStream()) {
      byte[] buf = new byte[1024];
      int readBytes;
      int totalBytes = 0;
      while (-1 != (readBytes = is.read(buf))) {
        totalBytes += readBytes;
        output.append(new String(buf, 0, readBytes, Charset.defaultCharset()));
      }
      assertTrue(totalBytes > 0);
    }
    return output.toString();
  }
}

class TestServlet extends HttpServlet {
  private int numberOfThreads;
  private  long sleepTime;

  public TestServlet(int numberOfThreads, long sleepTime) {
    this.numberOfThreads = numberOfThreads;
    this.sleepTime = sleepTime;
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();

    List<TestThread> threads = new ArrayList<>();
    for (int i = 0; i < numberOfThreads; i++) {
      threads.add(new TestThread(sleepTime));
    }
    threads.forEach(Thread::start);
    threads.forEach(TestThread::joinOrDie);

    out.println("<HTML>");
    out.println("<HEAD><TITLE>" + " TestServlet" +  "</TITLE></HEAD>");
    out.println("<BODY>");
    out.println("Test servlet 1 for http_test test bundle <br>");
    out.println("The servlet's URI is: " + request.getRequestURI() + "<br>");
    out.println("The servlet's path is: " + request.getServletPath() + "<br>");
    out.println("The servlet's pathinfo is: " + request.getPathInfo());
    out.println("</BODY>");
    out.println("</HTML>");
  }
}

class TestHttpContext implements HttpContext {
  @Override
  public String getMimeType(String name) {
    return null;
  }

  @Override
  public URL getResource(String path) {
    return null;
  }

  @Override
  public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) {
    return true;
  }
}

class TestThread extends Thread {
  private long sleepTime;

  public TestThread(long sleepTime) {
    this.sleepTime = sleepTime;
  }

  public void joinOrDie() {
    try {
      join();
    } catch (InterruptedException ignored) {
    }
  }

  @Override
  public void run() {
    try {
      sleep(sleepTime);
    } catch (InterruptedException ignored) {
    }
  }
}
