/*
 * Created on Aug 14, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.knopflerfish.bundle.util;


import org.osgi.framework.*;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.*;
/**
 * This class implements a simple log client that uses a 
 * standard OsgiLog Service. 
 * init needs to be called with a valid bc and a log service has to
 * be available of course.
 */
public class LogClient 
{
    ServiceTracker m_st;
    
    public synchronized void init(BundleContext bc)
    {
        cleanup();
    	m_st = new ServiceTracker(bc, LogService.class.getName(), null);
    	m_st.open();
    }
    
    public synchronized void cleanup()
    {
        if (m_st == null)
        {
        	return;
        }
        m_st.close();
        m_st = null;
    }
    
    public void deb(String msg)
    {
        doLog(LogService.LOG_DEBUG, msg);
    }
    public void info(String msg)
    {
        doLog(LogService.LOG_INFO, msg);
    }
    public void warn(String msg)
    {
        doLog(LogService.LOG_WARNING, msg);
    }
    public void err(String msg)
    {
        doLog(LogService.LOG_ERROR, msg);
    }


    private LogService getS()
    {
        if (m_st== null)
        {
        	return null;
        }
    	return (LogService) m_st.getService();
    }    
    
    private void doLog(int level, String msg)
    {
       LogService s = getS();
       if (s != null)
       {
       	  s.log(level, msg);
       }
    }
    
}
