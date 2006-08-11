package org.knopflerfish.bundle.httpclient_connector;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Date;

import javax.microedition.io.HttpConnection;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.httpclient.util.DateUtil;


/**
 * TODO: 
 * - is uri.getEscapedURIReference() correct?
 * - shouldn't the getHeaderField(int nth) throw an IOException?
 */
class HttpClientConnection implements HttpConnection {
	
  public final static String TIMEOUT        = "org.knopflerfish.httpclient_connector.so_timeout";
	public final static String PROXY_SERVER   = "org.knopflerfish.httpclient_connector.proxy.server";
	public final static String PROXY_PORT     = "org.knopflerfish.httpclient_connector.proxy.port";
	public final static String PROXY_USERNAME = "org.knopflerfish.httpclient_connector.proxy.username";
	public final static String PROXY_PASSWORD = "org.knopflerfish.httpclient_connector.proxy.password";
  public final static String PROXY_REALM    = "org.knopflerfish.httpclient_connector.proxy.realm";
	public final static String PROXY_SCHEME   = "org.knopflerfish.httpclient_connector.proxy.scheme";

	private final static int STATE_SETUP     = 0;
	private final static int STATE_CONNECTED = 1;
	private final static int STATE_CLOSED    = 2;
	
	private int state = STATE_SETUP;
	private boolean requestSent = false; 
	
	private String method = HttpConnection.GET;
	
	private HttpClient client = new HttpClient();
	private Object lock = new Object(); // lock for client.
	
	private URI uri;
	private HttpMethod resCache = null;
	
	private ArrayList iss = new ArrayList(); // All inputs that are associated with this connection
	private OutputWrapper out = null;
	
	HttpClientConnection(String url, int mode, boolean timeouts) throws URIException {
		uri = new URI(url, false); // assume not escaped URIs
		HostConfiguration conf = client.getHostConfiguration();

    String proxyServer = System.getProperty(PROXY_SERVER, System.getProperty("http.proxyHost"));
    if (proxyServer != null) {
      int proxyPort;
      try {
        proxyPort = Integer.parseInt(System.getProperty(PROXY_PORT, System.getProperty("http.proxyPort", "8080")));
        conf.setProxy(proxyServer, proxyPort);
      } catch (NumberFormatException e) {
        throw new RuntimeException("Invalid proxy: " + proxyServer + ":" + System.getProperty(PROXY_PORT));
      }

      String proxyUsername = System.getProperty(PROXY_USERNAME);
      if (proxyUsername != null) {
        client.getState().setProxyCredentials(
            new AuthScope(proxyServer,
                          proxyPort,
                          System.getProperty(PROXY_REALM),
                          System.getProperty(PROXY_SCHEME)),
                          new UsernamePasswordCredentials(proxyUsername, System.getProperty(PROXY_PASSWORD)));
      }
    }

    String timeoutString = System.getProperty(TIMEOUT);
    if (timeoutString != null) {
      try {
        client.getParams().setSoTimeout(Integer.parseInt(timeoutString));
      } catch (NumberFormatException e) {
        throw new RuntimeException("Invalid timeout " +	timeoutString);
      }
    }
	}

	public long getDate() throws IOException {
		HttpMethod res = getResult(true);
		Header head = res.getResponseHeader("Date");
		
		if (head == null) {
			return 0;
		}
		
		try {
			return DateUtil.parseDate(head.getValue()).getTime();
		} catch (DateParseException e) {
			return 0;
		}
	}

	public long getExpiration() throws IOException {
		HttpMethod res = getResult(true);
		Header head = res.getResponseHeader("Expires");
		
		if (head == null) {
			return 0;
		}
		
		try {
			return DateUtil.parseDate(head.getValue()).getTime();
		} catch (DateParseException e) {
			return 0;
		}		
	}

	public String getFile() {
		return uri.getEscapedPath();
	}

	public String getHeaderField(int nth) { // throws IOException TODO: doesn't this one throw exceptions??
		try {
			HttpMethod res = getResult(true);
			Header[] hs = res.getResponseHeaders();

			if (hs.length > nth && nth >= 0) {
				return hs[nth].getValue();
			} else {
				return null;
			}
		} catch (IOException e) {
			return null;
		}
	}

	public String getHeaderField(String key) throws IOException {
		HttpMethod res = getResult(true);
		Header head = res.getResponseHeader(key);
		
		if (head == null) {
			return null;
		}
		
		return head.getValue();
	}

	public long getHeaderFieldDate(String key, long def) throws IOException {
		HttpMethod res = getResult(true);
		Header head = res.getResponseHeader(key);
		
		if (head == null) {
			return def;
		}
		
		try {
			Date date = DateUtil.parseDate(head.getValue());
			return date.getTime();
			
		} catch (DateParseException e) {
			return def;
		}
	}

	public int getHeaderFieldInt(String key, int def) throws IOException {
		HttpMethod res = getResult(true);
		Header head = res.getResponseHeader(key);
		
		if (head == null) {
			return def;
		}
		
		try {
			return Integer.parseInt(head.getValue());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	public String getHeaderFieldKey(int nth) throws IOException {
		HttpMethod res = getResult(true);
		Header[] hs = res.getResponseHeaders();

		if (hs.length > nth && nth >= 0) {
			return hs[nth].getName();
		} else {
			return null;
		}
	}

	public String getHost() {
		try {
			return uri.getHost();
		} catch (URIException e) {
			return null;
		}
	}

	public long getLastModified() throws IOException {
		HttpMethod res = getResult(true);
		Header head = res.getResponseHeader("Last-Modified");
		
		if (head != null) {
			try {
				return DateUtil.parseDate(head.getValue()).getTime();
			} catch (DateParseException e) {
				return 0;
			}
		}
		
		return 0;
	}

	public int getPort() {
		return uri.getPort();
	}

	public String getProtocol() {
		return uri.getScheme();
	}

	public String getQuery() {
		return uri.getEscapedQuery();
	}

	public String getRef() {
		// TODO: this returns the URL?!
		return uri.getEscapedURIReference();
	}

	public String getRequestMethod() {
		return method;
	}

	public String getRequestProperty(String key) {
		try {
			HttpMethod res = getResult();
			Header h = res.getRequestHeader(key);
			if (h != null) {
				return h.getValue();
			}
			
			return null;
		} catch (IOException e) {
			throw new RuntimeException("This is a bug.");
		}
	}

	public int getResponseCode() throws IOException {
		HttpMethod res = getResult(true);
		return res.getStatusCode();
	}

	public String getResponseMessage() throws IOException {
		HttpMethod res = getResult(true);
		return res.getStatusText();
	}

	public String getURL() {
		return uri.getEscapedURI();
	}

	public void setRequestMethod(String method) throws IOException {
		if (!HttpConnection.GET.equals(method) && 
				!HttpConnection.HEAD.equals(method) &&
				!HttpConnection.POST.equals(method)) {
			throw new IllegalArgumentException("method should be one of " +
					HttpConnection.GET + ", " + 
					HttpConnection.HEAD + " and " + 
					HttpConnection.POST);
		}
		
		if (state == STATE_CLOSED) {
			init();
		}
		
		if (state != STATE_SETUP) { 
			throw new ProtocolException("Can't reset method: already connected");
		}
		
		if (out != null && !HttpConnection.POST.equals(method)) { 
			// When an outputstream has been created these calls are ignored.
			return ;
		}
		
		if (this.method != method && resCache != null) {
			// TODO: here we should convert an existing resCache
			throw new RuntimeException("Not yet implemented, as a work around you can always set the request method the first thing you do..");
		}
		
		this.method = method;
	}

	public void setRequestProperty(String key, String value) throws IOException {
		
		if (state == STATE_CLOSED) {
			init();
		}
		
		if (state != STATE_SETUP) {
			throw new IllegalStateException("Already connected");
		}
		
		if (out != null && !HttpConnection.POST.equals(method)) { 
			// When an outputstream has been created these calls are ignored.
			return ;
		}
		
		HttpMethod res = getResult();
		res.setRequestHeader(key, value);
	}

	public String getEncoding() { // throws IOException
		try {
			HttpMethod res = getResult(true);
			Header head = res.getResponseHeader("Content-Encoding");
			
			if (head != null) {
				return head.getValue();
			} 
			
			return null;
		} catch (IOException e) {
			return null;
		}
	}

	public long getLength() {
		try {
			HttpMethod res = getResult(true);
			Header head =  res.getResponseHeader("Content-Length");
			
			if (head == null) {
				return -1;
			}
			
			return Long.parseLong(head.getValue());
		} catch (IOException e) {
			return -1;
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	public String getType() {
		try {
			HttpMethod res = getResult(true);
			Header head = res.getResponseHeader("Content-Type") ;
			
			if (head == null) {
				return null;
			}
			
			return head.getValue();
		} catch (IOException e) {
			return null;
		}
	}

	public DataInputStream openDataInputStream() throws IOException {
		return new DataInputStream(openInputStream());
	}

	public InputStream openInputStream() throws IOException {
		HttpMethod res = getResult(true);
		InputStream is = res.getResponseBodyAsStream();
		
		if (is == null) {
			return null;
		}		
		
		InputWrapper iw = new InputWrapper(is);
		
		synchronized(iss) {
			iss.add(iw);
		}
		
		return iw;
	}

	public void close() throws IOException {
		
		synchronized(iss) {
			for (int i = 0, n = iss.size(); i < n; i++) {
				InputWrapper iw = (InputWrapper)iss.get(i);
				iw.closeStream();
			}
			iss.clear();	
		}
		
		if (out != null) {
			out.close();
			out = null;
		}
		
		state = STATE_CLOSED;
	}

	public DataOutputStream openDataOutputStream() throws IOException {
		return new DataOutputStream(openOutputStream());
	}

	public OutputStream openOutputStream() throws IOException {
		if (requestSent) {
			throw new ProtocolException("The request has already been sent");
		}
		
		if (out == null) {
			out = new OutputWrapper();
		}
		
		setRequestMethod(HttpConnection.POST);
		
		return out;
	}
	
	private HttpMethod getResult() throws IOException {
		return getResult(false);
	}
	
	private HttpMethod getResult(boolean forceSend) throws IOException {
		if (resCache != null) {
			if (forceSend && !requestSent) { 
				// if the method has not been send yet (if we have been working with POST)
				sendRequest();
			}
			
			return resCache;
		}
		
		if (method.equals(HttpConnection.POST)) {
			resCache = new PostMethod(uri.getEscapedURI());
			resCache.setFollowRedirects(false);
			
		} else if (method.equals(HttpConnection.HEAD)) {
			resCache = new HeadMethod(uri.getEscapedURI());
		} else if (method.equals(HttpConnection.GET)) {
			resCache = new GetMethod(uri.getEscapedURI());
				
		} else {
			// hopefully this is unreachable code
			throw new IllegalStateException("Not a valid method " + method);
		}
			
		resCache.setFollowRedirects(false);
		
		if (forceSend) { 
			sendRequest();
			state = STATE_CONNECTED;
		}
		
		return resCache;
	}
	
	private void sendRequest() throws IOException {
		synchronized(lock) {
			if (out != null) {
				if (!(resCache instanceof EntityEnclosingMethod)) {
					System.err.println("Warning: data written to request's body, but not supported");
				} else {
					EntityEnclosingMethod m = (EntityEnclosingMethod) resCache;
					m.setRequestEntity(new ByteArrayRequestEntity(out.getBytes()));
				}
			} 
			
			client.executeMethod(resCache);
			requestSent = true;
		}
	}
	
	private void init() {
		state = STATE_SETUP;
		requestSent = false;
		method = HttpConnection.GET;
		resCache = null;
		//params = new DefaultHttpParams();
		out = null;
	}
	
//	private void convert(HttpMethod target) throws IOException {
//		HttpMethod res = getResult();
//		target.setFollowRedirects(res.getFollowRedirects());
//		target.setParams(res.getParams());
//		target.setPath(res.getPath());
//		target.setQueryString(res.getQueryString());
//		Header[] headers = res.getRequestHeaders();
//		
//		if (headers != null) {
//			for (int i = 0; i < headers.length; i++) {
//				target.setRequestHeader(headers[i]);
//			}
//		}
//		
//		target.setURI(res.getURI());
//	}
	
	private class OutputWrapper extends OutputStream {
		private ByteArrayOutputStream bout = new ByteArrayOutputStream();

		public void write(int b) throws IOException {
			bout.write(b);	
		}

		public void close() throws IOException {
			state = STATE_CONNECTED;
		}
		
		public void flush() throws IOException {
			getResult(true);
		}
		
		byte[] getBytes() {
			return bout.toByteArray();
		}
		
	}
	
	private void callClose() throws IOException {
		close();
	}
	
	private class InputWrapper extends InputStream {

		private InputStream is;
		
		InputWrapper(InputStream is) {
			this.is = is;
		}
		
		public int read() throws IOException {
			try {
				return is.read();
			} catch (IOException t) {
				callClose();
				throw t;
			}
		}
		
		public void close() throws IOException {
			synchronized(iss) {
				iss.remove(this);
			}
			
			is.close();
		}
		
		public void closeStream() throws IOException {
			is.close();
		}
	}

	
}
