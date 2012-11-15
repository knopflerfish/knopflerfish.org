package org.knopflerfish.util.metatype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;

class BundleMetaTypeInformationSnapshot implements MetaTypeInformation {
  private final BundleContext bc;
  private final Bundle bundle;
  ArrayList pids = new ArrayList();
  ArrayList factoryPids = new ArrayList();
  ArrayList locales = new ArrayList();
  HashMap idToMti = new HashMap();
  
  public static BundleMetaTypeInformationSnapshot extractMetatypeInformation(BundleContext bc, Bundle b) {
    return new BundleMetaTypeInformationSnapshot(bc, b).foundInformation();
  }
  
  private BundleMetaTypeInformationSnapshot foundInformation() {
    return pids.isEmpty() && factoryPids.isEmpty() ? null : this;
  }

  private BundleMetaTypeInformationSnapshot(BundleContext bc, Bundle b) {
    this.bc = bc;
    this.bundle = b;
    extractMetaTypeInformation();
 
  }
  
  private void extractMetaTypeInformation() {
    ServiceReference[] srs = bundle.getRegisteredServices();
    for(int i = 0; srs != null && i < srs.length; ++i) {
      extractMetaTypeInformation(srs[i]);
    }
  }

  private void extractMetaTypeInformation(ServiceReference sr) {
    Object s = null;
    try {
      s = bc.getService(sr);
      if(!(s instanceof MetaTypeProvider) && !(s instanceof MetaTypeInformation)) {
        return;
      }
      if(s instanceof ManagedService){
        addSrPropertiesToArrayListAndMapIds(sr, new String[] {Constants.SERVICE_PID, MetaTypeProvider.METATYPE_PID}, pids, s);
        addSrPropertiesToArrayListAndMapIds(sr, new String[] {MetaTypeProvider.METATYPE_FACTORY_PID}, factoryPids, s);
      } else if(s instanceof ManagedServiceFactory) {
        addSrPropertiesToArrayListAndMapIds(sr, new String[] {MetaTypeProvider.METATYPE_PID}, pids, s);
        addSrPropertiesToArrayListAndMapIds(sr, new String[] {Constants.SERVICE_PID, MetaTypeProvider.METATYPE_FACTORY_PID}, factoryPids, s);
      } else {
        addSrPropertiesToArrayListAndMapIds(sr, new String[] {MetaTypeProvider.METATYPE_PID}, pids, s);
        addSrPropertiesToArrayListAndMapIds(sr, new String[] {MetaTypeProvider.METATYPE_FACTORY_PID}, factoryPids, s);
      }
      String[] ls = ((MetaTypeProvider)s).getLocales();
      if(ls != null && ls.length > 0) {
        locales.addAll(Arrays.asList(ls));
      }
    } finally {
      if(s != null) {
        bc.ungetService(sr);
      }
    }
   }

  @Override
  public ObjectClassDefinition getObjectClassDefinition(String id, String locale) {
    Object o = idToMti.get(id);
    if(o instanceof MetaTypeProvider) {
      return ((MetaTypeProvider)o).getObjectClassDefinition(id, locale);
    } else if(o instanceof MetaTypeInformation) {
      return ((MetaTypeInformation)o).getObjectClassDefinition(id, locale);
    } else {
      return null;
    }
  }

  @Override
  public String[] getLocales() {
    return toStringArray(locales);
  }

  @Override
  public String[] getPids() {
    return toStringArray(pids);
  }

  @Override
  public String[] getFactoryPids() {
    return toStringArray(factoryPids);
  }

  @Override
  public Bundle getBundle() {
    return bundle;
  }
  
  private static String[] toStringArray(ArrayList al) {
    return (String[])al.toArray(new String[al.size()]);
  }
  
  private void addSrPropertiesToArrayListAndMapIds(ServiceReference sr, String[] ps, ArrayList al, Object s) {
    for(int i = 0; i < ps.length; ++i) {
      Object o = sr.getProperty(ps[i]);
      if(o == null) {
        continue;
      } else if(o instanceof String) {
        al.add(o);
        idToMti.put(o, s);
      } else if(o instanceof String[]) {
        Collection c = Arrays.asList((String[])o);
        al.addAll(c);
        mapAllIds(c, s);
      } else if(o instanceof Collection) {
        al.addAll((Collection)o);
        mapAllIds((Collection)o, s);
      } else {
        continue;
      }
    }
  }
  private void mapAllIds(Collection c, Object s) {
    Iterator i = c.iterator();
    while(i.hasNext()) {
      idToMti.put(i.next(), s);
    }
  }
}