package org.knopflerfish.bundle.desktop.swing.graph;


import java.util.*;
import java.awt.geom.Point2D;
import java.awt.Color;
import org.osgi.framework.*;
import org.knopflerfish.bundle.desktop.swing.Util;
import org.knopflerfish.bundle.desktop.swing.Activator;
import org.knopflerfish.bundle.desktop.swing.PackageManager;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;

public class PackageNode extends BundleNode  {
  Collection inLinks;
  Collection outLinks;

  PackageManager pm;

  boolean bFragments = true;
  boolean bMissing   = true;

  public PackageNode(PackageManager pm, Bundle b, int depth, String id) {
    super(b, depth, id);
    this.pm = pm;
    refresh();
  }
  
  public void refresh() {
    outLinks = null;
    inLinks = null;
  }

  Color baseColor = new Color(250, 120, 120);
  Color burnColor = new Color(255, 255, 50);

  Color baseFragmentColor = new Color(250, 150, 150);
  Color burnFragmentColor = new Color(255, 255, 200);

  public Collection getOutLinks() {
    long t0 = System.currentTimeMillis();
    long t1 = 0;
    long t2 = 0;
    long t3;
    if(outLinks == null) {
      try {
        outLinks = new ArrayList();

        t1 = System.currentTimeMillis();

        Color colA = Util.rgbInterpolate(baseColor, burnColor, (double)depth/3);
        Collection pkgs = pm.getExportedPackages(b);
        for(Iterator it = pkgs.iterator(); it.hasNext(); ) {
          ExportedPackage pkg = (ExportedPackage)it.next();
          Color col = colA;
          if(pkg.isRemovalPending()) {
            col = Util.rgbInterpolate(colA, Color.black, .5);
          }
          Bundle[] bl = pkg.getImportingBundles();
          String sId = pkg.getName() +";" + pkg.getVersion();
          
          if(bl == null || bl.length == 0) {
            StringBuffer lId = new StringBuffer();
            lId.append(getId());
            lId.append("/");
            lId.append("out.");
            lId.append(sId);
            lId.append(".");
            lId.append(Long.toString(b.getBundleId()));
            
            StringBuffer nId = new StringBuffer();
            nId.append(getId());
            nId.append("/");
            nId.append(lId.toString());
            nId.append(".none");
            
            String name = sId.toString();
            DefaultLink link = 
              new DefaultLink(this, new EmptyNode("none", depth+1, nId.toString()),
                              depth+1, lId.toString(), name);
            link.setColor(col);
            outLinks.add(link);              
          } else {
            for(int j = 0; j < bl.length; j++) {
              StringBuffer lId = new StringBuffer();
              lId.append(getId());
              lId.append("/");
              lId.append("out.");
              lId.append(sId);
              lId.append(".");
              lId.append(Long.toString(b.getBundleId()));
              lId.append(".");
              lId.append(Long.toString(bl[j].getBundleId()));
              
              StringBuffer nId = new StringBuffer();
              nId.append(getId());
              nId.append("/");
              nId.append(lId.toString());
              nId.append(".");
              nId.append(Long.toString(bl[j].getBundleId()));
              
              String name = sId.toString();
              
              DefaultLink link = 
                new DefaultLink(this, 
                                new PackageNode(pm, bl[j], depth+1, nId.toString()),
                                depth+1, lId.toString(), name);
              link.setColor(col);
              outLinks.add(link);
            }                        
          }
        }
        
        if(bFragments) {
          Bundle bl[] = pm.getHosts(b) ;
          Color col = Util.rgbInterpolate(baseFragmentColor, burnFragmentColor, (double)depth/3);
          for(int i = 0; i < bl.length; i++) {
            String sId = "fragment:" + b.getBundleId() + ":" + bl[i].getBundleId();
            String lId = 
              getId() + "/" + 
              "fragout." + sId + 
              "." + b.getBundleId() + 
              "." + bl[i].getBundleId();

            String nId = 
              getId() + "/" + 
              lId + 
              "." + bl[i].getBundleId();
            
            String name = "Fragment to " + Util.getBundleName(bl[i]);
            
            Node node;
            node = new PackageNode(pm, bl[i], depth+1, nId);
            
            DefaultLink link =  new DefaultLink(this, node, 
                                                depth+1, lId, name);

            link.setColor(col);
            link.setType(2);
            outLinks.add(link);            
          }
        }
        for(Iterator it = outLinks.iterator(); it.hasNext(); ) {
          DefaultLink link = (DefaultLink)it.next();
          link.setDetail(outLinks.size() > 20 ? 10 : 0);
        }
      } catch (Exception e) {
        Activator.log.error("Failed to get node packages", e);
      }
    }
    t2 = System.currentTimeMillis();
    /*
    System.out.println("getOutLinks " + this + 
                       " t2-t0:" + (t2 - t0) + "ms" + 
                       " t2-t1:" + (t2 - t1) + "ms" + 
                       " t1-t0:" + (t1 - t0) + "ms" + 
                       ""
                       );
    */
    return outLinks;
  }
  

  
  public Collection getInLinks() {
    long t0 = System.currentTimeMillis();
    if(inLinks == null) {
      try {
        inLinks = new ArrayList();


        Collection importedPkgs = pm.getImportedPackages(b);

        Color colA = Util.rgbInterpolate(baseColor, burnColor, (double)depth/5);

        colA = Util.rgbInterpolate(colA, Color.black, .3);
        for(Iterator it = importedPkgs.iterator(); it.hasNext(); ) {
          ExportedPackage pkg = (ExportedPackage)it.next();
          Color col = colA;
          if(pkg.isRemovalPending()) {
            col = Util.rgbInterpolate(col, Color.black, .5);
          }
          Bundle fromB = pkg.getExportingBundle();
          String sId = pkg.getName() +";" + pkg.getVersion();
            
          StringBuffer lId = new StringBuffer();
          lId.append(getId());
          lId.append("/");
          lId.append("in.");
          lId.append(sId);
          lId.append(".");
          lId.append(Long.toString(fromB.getBundleId()));
          lId.append(".");
          lId.append(Long.toString(b.getBundleId()));

          StringBuffer nId = new StringBuffer();
          nId.append(getId());
          nId.append("/");
          nId.append(lId.toString());
          nId.append(Long.toString(fromB.getBundleId()));
                
          String name = sId.toString();
          
          PackageNode node = new PackageNode(pm, fromB, depth+1, 
                                             nId.toString());
          DefaultLink link =  new DefaultLink(node,
                                              this,                             
                                              depth+1, lId.toString(), name);
          link.setType(-1);
          link.setColor(col);
          inLinks.add(link);
        }
        
        if(bFragments) {
          String symName = Util.getHeader(b, "Bundle-SymbolicName");
          // Bundle[] bl = Activator.getBC().getBundles();
          Bundle[] bl = pm.getFragments(b);

          Color col = Util.rgbInterpolate(baseFragmentColor, burnFragmentColor, (double)depth/3);
          col = Util.rgbInterpolate(col, Color.black, .3);
          
          String host = Long.toString(b.getBundleId());
          for(int i = 0; bl != null && i < bl.length; i++) {
            // String host = Util.getHeader(bl[i], "Fragment-Host");
            // if(host != null && host.equals(symName)) 
            {
              String sId = "fragment:" + bl[i].getBundleId() + ":" + host;
              StringBuffer lId = new StringBuffer();
              lId.append(getId());
              lId.append("/");
              lId.append("fragin.");
              lId.append(sId);
              lId.append(".");
              lId.append(Long.toString(bl[i].getBundleId()));
              lId.append(".");
              lId.append(Long.toString(b.getBundleId()));

              StringBuffer nId = new StringBuffer();
              nId.append(getId());
              nId.append("/");
              nId.append(lId.toString());
              nId.append(".");
              nId.append(Long.toString(b.getBundleId()));
              
              String name = "Fragment to " + host;
              
              PackageNode node = new PackageNode(pm, bl[i], 
                                                 depth+1, nId.toString());
              DefaultLink link =  new DefaultLink(node, this, 
                                                  depth+1, lId.toString(), 
                                                  name);

              link.setColor(col);
              link.setType(-2);
              inLinks.add(link);            
            }
          }
        }

        if(bMissing) {
          colA = new Color(150, 150, 150);
          
          Collection missingImports = pm.getMissingImports(b);

          for(Iterator it = missingImports.iterator(); it.hasNext(); ) {
            String pkgName = (String)it.next();
            Color col = colA;
            String sId = pkgName;
            
            StringBuffer lId = new StringBuffer();
            lId.append(getId());
            lId.append("/");
            lId.append("in.");
            lId.append(sId);
            lId.append(".");
            lId.append("missing");
            lId.append(".");
            lId.append(Long.toString(b.getBundleId()));

            StringBuffer nId = new StringBuffer();
            nId.append(getId());
            nId.append("/");
            nId.append(lId.toString());
            nId.append("missing");
                
            String name = sId.toString();
            
            DefaultNode node = new EmptyNode("missing " + pkgName, 
                                             depth+1, 
                                             nId.toString());
            DefaultLink link =  new DefaultLink(node,
                                                this,                             
                                                depth+1, lId.toString(), name);
            link.setType(-3);
            link.setColor(col);
            inLinks.add(link);
          }
        }

        for(Iterator it = inLinks.iterator(); it.hasNext(); ) {
          DefaultLink link = (DefaultLink)it.next();
          link.setDetail(inLinks.size() > 20 ? 10 : 0);
        }
      } catch (Exception e) {
        Activator.log.error("Failed to get services", e);
      }
    }
    long t1 = System.currentTimeMillis();
    // System.out.println("getInLinks " + this + " took " + (t1 - t0) + "ms");

    return inLinks;
  }
  
  public String toString() {
    return 
      "#" + b.getBundleId() + " " + Util.getBundleName(b);    
  }
  
}
