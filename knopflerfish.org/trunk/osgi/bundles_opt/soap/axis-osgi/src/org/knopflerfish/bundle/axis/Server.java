package org.knopflerfish.bundle.axis;

//import java.util.*;
import java.net.ServerSocket;


public class Server extends org.apache.axis.transport.http.SimpleAxisServer  {


  public void start() throws Exception {
      setServerSocket(new ServerSocket(Integer.getInteger("org.knopflerfish.service.axis.port", 8090).intValue()));
      start(true);
  }
  
  public void stop() throws Exception {
  //      stopped = true;
        getServerSocket().close();
  }
  
}

