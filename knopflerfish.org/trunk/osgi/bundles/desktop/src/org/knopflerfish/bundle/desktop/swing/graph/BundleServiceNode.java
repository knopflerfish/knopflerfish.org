package org.knopflerfish.bundle.desktop.swing.graph;


import java.util.*;
import java.awt.geom.Point2D;
import java.awt.Color;
import org.osgi.framework.*;
import org.knopflerfish.bundle.desktop.swing.Util;
import org.knopflerfish.bundle.desktop.swing.Activator;

public class BundleServiceNode extends BundleNode  {
  Collection inLinks;
  Collection outLinks;

  public BundleServiceNode(Bundle b, int depth) {
    this(b, depth, "#" + b.getBundleId() + "." + depth);
  }

  public BundleServiceNode(Bundle b, int depth, String id) {
    super(b, depth, id);
    refresh();
  }
  
  public void refresh() {
    outLinks = null;
    inLinks = null;

  }


  Color baseColor = new Color(255, 220, 255);
  Color burnColor = new Color(255, 255, 50);

  public Collection getOutLinks() {
    if(outLinks == null) {
      try {
        outLinks = new ArrayList();
        ServiceReference[] srl = b.getRegisteredServices();
        
        Color col = Util.rgbInterpolate(baseColor, burnColor, (double)depth/5);
        
        for(int i = 0; srl != null && i < srl.length; i++) {
          Bundle[] bl = srl[i].getUsingBundles();
          String sId = srl[i].getProperty("service.id").toString(); 
          if(bl == null || bl.length == 0) {
            String lId = 
              getId() + "/" + 
              "." + sId + 
              "." + b.getBundleId();
            String nId = 
              getId() + "/" + 
              lId + 
              ".none";
            
            String name = 
              "#" + sId + " " + Util.getClassNames(srl[i]);
            
            DefaultLink link = 
              new DefaultLink(this, 
                              new EmptyNode("none", depth+1, nId),
                              depth+1, lId, name);
            link.setColor(col);
            link.setDetail(srl.length > 20 ? 10 : 0);
            outLinks.add(link);
          } else {
            for(int j = 0; j < bl.length; j++) {
              String lId = 
                getId() + "/" + 
                "." + sId + 
                "." + b.getBundleId() + 
                "." + bl[j].getBundleId();
              String nId = 
                getId() + "/" + 
                lId + 
                "." + bl[j].getBundleId();
              
              String name = 
                "#" + sId + " " + Util.getClassNames(srl[i]);

              DefaultLink link = 
                new DefaultLink(this, 
                                new BundleServiceNode(bl[j], depth+1, nId),
                                depth+1, lId, name);
              link.setColor(col);              
              outLinks.add(link);
              link.setDetail(bl.length * srl.length > 20 ? 10 : 0);
            }          
          }
        }
      } catch (Exception e) {
        Activator.log.error("Failed to get services", e);
      }
    }
    return outLinks;
  }
  


  public Collection getInLinks() {
    if(inLinks == null) {
      try {
        inLinks = new ArrayList();
        
        ServiceReference[] srl = Activator.getTargetBC().getServiceReferences(null, null);
        int nImport = 0;
        for(int i = 0; srl != null && i < srl.length; i++) {
          Bundle[] bl = srl[i].getUsingBundles();
          Color col = Util.rgbInterpolate(baseColor, burnColor, (double)depth/3);
          col = Util.rgbInterpolate(col, Color.black, .3);

          String sId = srl[i].getProperty("service.id").toString(); 

          for(int j = 0; bl != null && j < bl.length; j++) {
            if(bl[j].getBundleId() == b.getBundleId()) {
              String lId = 
                getId() + "/" + 
                "in." + sId + 
                "." + b.getBundleId() + 
                "." + bl[j].getBundleId();
              String nId = 
                getId() + "/" + 
                lId + 
                "." + bl[j].getBundleId();
              
              String name = 
                "#" + sId + " " + Util.getClassNames(srl[i]); 
              
              BundleServiceNode node = new BundleServiceNode(srl[i].getBundle(), depth+1, nId);
              DefaultLink link = new DefaultLink(node,
                                                 this,
                                                 depth+1,
                                                 lId, 
                                                 name);
              link.setType(-1);
              link.setColor(col);
              inLinks.add(link);
            }
          }
        }
      } catch (Exception e) {
        if(Activator.log != null) {
          Activator.log.error("Failed to get services", e);
        }
      }
    }
    return inLinks;
  }  
}
