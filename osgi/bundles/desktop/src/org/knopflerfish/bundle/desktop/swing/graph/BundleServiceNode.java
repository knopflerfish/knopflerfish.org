/*
 * Copyright (c) 2003-2022, KNOPFLERFISH project
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

import org.knopflerfish.bundle.desktop.swing.Activator;
import org.knopflerfish.bundle.desktop.swing.Util;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

public class BundleServiceNode extends BundleNode  {
  Collection<DefaultLink> inLinks;
  Collection<DefaultLink> outLinks;

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

  public Collection<DefaultLink> getOutLinks() {
    if(outLinks == null) {
      try {
        outLinks = new ArrayList<>();
        ServiceReference<?>[] srl = b.getRegisteredServices();

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
            for (Bundle bundle : bl) {
              String lId =
                  getId() + "/" +
                      "." + sId +
                      "." + b.getBundleId() +
                      "." + bundle.getBundleId();
              String nId =
                  getId() + "/" +
                      lId +
                      "." + bundle.getBundleId();

              String name =
                  "#" + sId + " " + Util.getClassNames(srl[i]);

              DefaultLink link =
                  new DefaultLink(this,
                      new BundleServiceNode(bundle, depth + 1, nId),
                      depth + 1, lId, name);
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



  public Collection<DefaultLink> getInLinks() {
    if(inLinks == null) {
      try {
        inLinks = new ArrayList<>();

        ServiceReference<?>[] srl = Activator.getTargetBC_getServiceReferences();
        for(int i = 0; srl != null && i < srl.length; i++) {
          Bundle[] bl = srl[i].getUsingBundles();
          if (bl == null) {
            continue;
          }
          Color col = Util.rgbInterpolate(baseColor, burnColor, (double) depth / 3);
          col = Util.rgbInterpolate(col, Color.black, .3);

          String sId = srl[i].getProperty("service.id").toString();

          for (Bundle bundle : bl) {
            if (bundle.getBundleId() == b.getBundleId()) {
              String lId =
                  getId() + "/" +
                      "in." + sId +
                      "." + b.getBundleId() +
                      "." + bundle.getBundleId();
              String nId =
                  getId() + "/" +
                      lId +
                      "." + bundle.getBundleId();

              String name =
                  "#" + sId + " " + Util.getClassNames(srl[i]);

              BundleServiceNode node = new BundleServiceNode(srl[i].getBundle(), depth + 1, nId);
              DefaultLink link = new DefaultLink(node,
                  this,
                  depth + 1,
                  lId,
                  name);
              link.setType(-1);
              link.setColor(col);
              inLinks.add(link);
            }
          }
        }
      } catch (Exception e) {
        if (Activator.log != null) {
          Activator.log.error("Failed to get services", e);
        }
      }
    }
    return inLinks;
  }
}
