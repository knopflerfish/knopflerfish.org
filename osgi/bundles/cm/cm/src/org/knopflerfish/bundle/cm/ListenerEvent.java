package org.knopflerfish.bundle.cm;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;

/**
 * @author js
 */
public class ListenerEvent {

	/**
	 * The service reference to the service we are to send the event to.
	 */
	private final ServiceReference sr;

	/**
	 * The configuration event we want to send.
	 */
	private final ConfigurationEvent event;

	/**
	 * Create a listener event.
	 * @param sr The service reference to the service we are to send the event to.
	 * @param event The configuration event we want to send.
	 */
	public ListenerEvent(ServiceReference sr, ConfigurationEvent event) {
		this.sr = sr;
		this.event = event;
	}

	/**
	 * Send the event to the service.
	 * @param bc The bundle context to use to get the service.
	 */
	public void sendEvent(BundleContext bc) {
		if(sr != null)
		{
			ConfigurationListener configurationListener = (ConfigurationListener) bc.getService(sr);
			if(configurationListener != null)
			{
				configurationListener.configurationEvent(event);
			}
			bc.ungetService(sr);
		}
	}
}
