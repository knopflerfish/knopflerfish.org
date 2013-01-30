/*
 * Copyright (c) 2003-2012, KNOPFLERFISH project
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
package org.knopflerfish.bundle.desktop.swing.graph;


import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.knopflerfish.bundle.desktop.swing.Activator;
import org.knopflerfish.bundle.desktop.swing.PackageManager;
import org.knopflerfish.bundle.desktop.swing.Util;
import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.ExportedPackage;


public class PackageNode
  extends BundleNode
{
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
    if(outLinks == null) {
      try {
        outLinks = new ArrayList();

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

    return inLinks;
  }

  public String toString() {
    return
      "#" + b.getBundleId() + " " + Util.getBundleName(b);
  }

}
