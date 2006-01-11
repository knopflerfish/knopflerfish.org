package org.knopflerfish.bundle.component;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

import org.osgi.util.tracker.ServiceTracker;

public class Reference extends ServiceTracker {
    
  public static final int ONE_TO_ONE   = 0;
  public static final int ZERO_TO_ONE  = 1;
  public static final int ONE_TO_MANY  = 2;
  public static final int ZERO_TO_MANY = 4;

  public static final int STATIC_POLICY  = 0;
  public static final int DYNAMIC_POLICY = 1;

  private int cardinality;
  private int policy;

  public Reference(String refName, Filter filter, 
		   int cardinality, int policy, 
		   String bind, String unbind,
		   BundleContext bc) {
    
    super(bc, filter, null); 
    this.cardinality = cardinality;
    this.policy = policy;
  }

  public Object addingService(ServiceReference ref, 
			      Object service) {
    return super.addingService(ref);

  }

  /* public void modifiedService() */

  public void removedService(ServiceReference ref,
			     Object service) {
    super.removedService(ref, service);
    /* try to remove this service, 
       possibly disabling the component */
  }

  public boolean isSatisfied() {
    return getTrackingCount() > 0 || 
      cardinality == ZERO_TO_ONE || 
      cardinality == ZERO_TO_MANY;
  }

}
