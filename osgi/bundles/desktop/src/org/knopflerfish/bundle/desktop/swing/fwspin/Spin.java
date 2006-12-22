/*
 * Copyright (c) 2003, KNOPFLERFISH project
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

package org.knopflerfish.bundle.desktop.swing.fwspin;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.CubicCurve2D;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JPanel;

import org.knopflerfish.bundle.desktop.swing.Activator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * @author Erik Wistrand
 */
public class Spin extends JPanel implements Runnable, BundleListener, ServiceListener {

  Thread   runner = null;
  boolean  bRun   = false;

  long delay      = 30;
  long targetTime = 1000 / 30;
  long updateTime = 0;
  long totalTime = 0;

  Dimension  size     = null;

  Image      memImage = null;
  Graphics   memG     = null;

  boolean bShowFrameRate    = false;
  boolean bShowBundleLegend = true;
  boolean use2D             = true;
  boolean bShowBundleInfo   = false;
  boolean bShowHelp         = false;
  boolean bShowDeps         = false;
  boolean bStepSize         = false;

  double deltaA = Math.PI * 1.5;
  double aStep  = Math.PI * 2 / 120;

  Map bundles  = new TreeMap();
  Map services = new HashMap();
  Map active   = new HashMap();

  Object paintLock = services;


  double fontSize = 1.0;

  Object alphaHalf = null;

  String searchString = "";
  boolean bSearchMode = false;

  Console console;

  public KeyListener kl = null;

  Color bgColor   = new Color(20, 20, 80);
  Color textColor = bgColor.brighter().brighter().brighter();

  public Spin() {
    super();

    /*
    System.out.println("made spin");
    addFocusListener(new FocusAdapter() {
        public void focusGained(FocusEvent e) {
          System.out.println("got focus");
        }
        public void focusLost(FocusEvent e) {
          System.out.println("lost focus");
        }
      });
    */

    Bundle[] bl = Activator.getTargetBC().getBundles();
    for(int i = 0; bl != null && i < bl.length; i++) {
      bundleChanged(new BundleEvent(BundleEvent.INSTALLED, bl[i]));
    }
    Activator.getTargetBC().addBundleListener(this);

    try {
      ServiceReference [] srl = Activator.getTargetBC().getServiceReferences(null, null);
      for(int i = 0; srl != null && i < srl.length; i++) {
        serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, srl[i]));
      }
      Activator.getTargetBC().addServiceListener(this, null);
    } catch (Exception e) {
      e.printStackTrace();
    }

    initFonts();

    addMouseMotionListener(new MouseMotionAdapter() {
        public void mouseDragged(MouseEvent ev) {
        }
        public void mouseMoved(MouseEvent ev) {
          hotX = ev.getX();
          hotY = ev.getY();
          setActive(ev.getX(), ev.getY());
          if(mouseActive != null && (mouseActive instanceof BX)) {
            BX bx = (BX)mouseActive;
            Activator.desktop.setSelected(bx.b);
          }
        }
      });

    addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent ev) {
          requestFocus();
          hotX = ev.getX();
          hotY = ev.getY();
          setActive(ev.getX(), ev.getY());
          if(mouseActive != null && (mouseActive instanceof BX)) {
            BX bx = (BX)mouseActive;
            Activator.desktop.setSelected(bx.b);
          }
        }
      });

    center = new SpinItem() {
        public void    paint(Graphics g) { };
        public void    paintDependencies(Graphics g) { };
        public void    paintInfo(Graphics g, double x, double y) { };
        public boolean isActive() { return false; }
      };

    addKeyListener(kl = new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
          if(bShowConsole) {
            handleConsole(e);
            repaint();
            return;
          }
          if(bSearchMode) {
            switch(e.getKeyCode()) {
            case KeyEvent.VK_PERIOD: 
              bSearchMode = false;
              break;
            case KeyEvent.VK_DELETE: 
            case KeyEvent.VK_BACK_SPACE: 
              if(searchString.length() > 0) {
                searchString = searchString.substring(0, searchString.length() -1);
                doSearch();
              }
              break;
            case KeyEvent.VK_UNDERSCORE: 
              searchString = searchString + "_";
              doSearch();
            default:
              searchString = (searchString + e.getKeyChar()).toLowerCase();
              doSearch();
            }
            repaint();
            return;
          }
          switch(e.getKeyCode()) {
          case KeyEvent.VK_SLASH: 
          case KeyEvent.VK_PERIOD: 
            bSearchMode = true;
            break;
          case KeyEvent.VK_F: 
            bShowFrameRate = !bShowFrameRate;
            break;
          case KeyEvent.VK_B: 
            bShowBundleLegend = !bShowBundleLegend;
            break;
          case KeyEvent.VK_I: 
            bShowBundleInfo = !bShowBundleInfo;
            break;
          case KeyEvent.VK_0: 
            {
              if(mouseActive != null) {
                final SpinItem item = mouseActive;
                Runnable run = new Runnable() {
                    public void run() {
                      while(deltaA < 0) deltaA += Math.PI * 2;
                      while(deltaA > -item.getAngle()) {
                        deltaA -= Math.PI / 50;
                        bxNeedRecalc = true;
                        sxNeedRecalc = true;
                        repaint();
                        try {
                          Thread.sleep(20);
                        } catch (Exception e) { }
                      }
                      deltaA = -item.getAngle();
                    }
                  };
                Thread t = new Thread(run, "spin thread");
                t.start();
              }
            }
            break;
          case KeyEvent.VK_8: 
            deltaA -= aStep;
            bxNeedRecalc = true;
            sxNeedRecalc = true;
            break;
          case KeyEvent.VK_9: 
            deltaA += aStep;
            bxNeedRecalc = true;
            sxNeedRecalc = true;
            break;
          case KeyEvent.VK_LEFT: 
            step(-1);
            break;
          case KeyEvent.VK_RIGHT: 
            step(1);
            break;
          case KeyEvent.VK_F1: 
          case KeyEvent.VK_H: 
          case KeyEvent.VK_HELP:
            bShowHelp = !bShowHelp;
            break;
          case KeyEvent.VK_D: 
            bShowDeps = !bShowDeps;
            break;
          case KeyEvent.VK_2: 
            use2D = !use2D;
            break;
          case KeyEvent.VK_S: 
            bStepSize = !bStepSize;
            initFonts();
            break;
          case KeyEvent.VK_4: 
            fontSize += .1;
            initFonts();
            break;
          case KeyEvent.VK_3: 
            fontSize = Math.max(.2, fontSize - .1);
            initFonts();
            break;
          case KeyEvent.VK_C: 
            active.clear();
            depVector = null;
            depActive = null;
            break;
          case KeyEvent.VK_TAB:
            stepDependency();
            break;
          case KeyEvent.VK_F2:
            toggleConsole();
            break;
          case KeyEvent.VK_SPACE: 
            if(depActive != null) {
              mouseActive = depActive;
              depActive = null;
              depVector = null;
              active.clear();
            }

            if(mouseActive != null) {
              if(active.containsKey(mouseActive)) {
                active.remove(mouseActive);
              } else {
                active.put(mouseActive, mouseActive);
              }
            }
            break;
          }
          repaint();
        }
      });

  }

  void handleConsole(KeyEvent e) {
  }

  boolean bShowConsole = false;
  int lineCount = 0;

  void toggleConsole() {
    if(console == null) {
      console = new Console(this);
    }

    bShowConsole = !bShowConsole;

    lineCount++;
    console.addLine("count=" + lineCount);
    if(bShowConsole) {
      console.setBounds(10, size.height / 2, 
                        size.width - 10, size.height - 10);
    } else {
    }
    repaint();
  }

  Vector depVector   = null;
  int    depPos      = 0;
  SpinItem depActive = null;

  void stepDependency() {
    if(depVector == null && mouseActive != null) {
      depVector = mouseActive.getNext(SpinItem.DIR_FROM | SpinItem.DIR_TO);
      
      depPos    = 0;
    }
    if(depVector != null && depVector.size() > 0) {
      depPos = (depPos + 1) % depVector.size();
      depActive = (SpinItem)depVector.elementAt(depPos);
      repaint();
    }
  }

  void doSearch() {
    active.clear();
    int hitCount = 0;
    SpinItem hit = null;
    if(searchString.length() > 0) {
      for(Iterator it = bundles.keySet().iterator(); it.hasNext(); ) {
        Long key      = (Long)it.next();
        SpinItem   item = (SpinItem)bundles.get(key);
        
        if(item.toString().toLowerCase().startsWith(searchString)) {
          active.put(item, item);
          hit = item;
          hitCount++;
        }
      }
    }
    mouseActive = hit;
    depActive = null;
  }


  void step(int delta) {
    if(mouseActive != null && (mouseActive instanceof BX)) {
      BX bx = (BX)mouseActive;
      long id = bx.b.getBundleId();

      BX bx2 = getBX(id + delta);
      if(bx2 != null) {
        mouseActive = bx2;
        repaint();
      }
    }
    if(mouseActive != null && (mouseActive instanceof SX)) {
      SX   sx = (SX)mouseActive;
      Long id = (Long)sx.sr.getProperty("service.id");

      SX sx2 = getSX(id.longValue() + delta);
      if(sx2 != null) {
        mouseActive = sx2;
        repaint();
      }
    }
  }

  int fontMin = 1;
  int fontMax = 11;

  Font[] fonts;
  void initFonts() {
    initFonts("Dialog");
  }


  void initFonts(String name) {
    fonts = new Font[fontMax - fontMin + 1];
    int n = 0;
    for(int i = fontMin; i <= fontMax; i++) {
      int h = (int)(fontSize * i);
      if(bStepSize && i < fontMax) {
        h = (int)(fontMin * fontSize);
      }
      Font f = new Font("Dialog", Font.PLAIN, h);
      fonts[n++] = f;
    }
  }
  
  SpinItem mouseActive = null;

  void setActive(int x, int y) {
    double minD = 1000000;
    
    SpinItem nearest = null;

    for(Iterator it = services.keySet().iterator(); it.hasNext(); ) {
      Object key      = it.next();
      SpinItem   item = (SpinItem)services.get(key);

      double dx = x - item.getSX();
      double dy = y - item.getSY();
      
      double d2 = dx * dx + dy * dy;
      if(d2 < minD) {
        minD = d2;
        nearest = item;
      }
    }

    for(Iterator it = bundles.keySet().iterator(); it.hasNext(); ) {
      Long key      = (Long)it.next();
      SpinItem   item = (SpinItem)bundles.get(key);
      
      double dx = x - item.getSX();
      double dy = y - item.getSY();
      
      double d2 = dx * dx + dy * dy;
      if(d2 < minD) {
        minD = d2;
        nearest = item;
      }
    }
    
    if(nearest != null) {
      mouseActive = nearest;
    }
    repaint();
  }


  Font getFont(double k) {
    int i = (int)(fontMin + (fontMax - fontMin) * k);
    i = Math.max(0, Math.min(fonts.length - 1, i));
    return fonts[i];
  }

  public void forceRepaint() {
    bxNeedRecalc = true;
    sxNeedRecalc = true;
    //    System.out.println("forceRepaint");
    repaint();
  }

  boolean bxNeedRecalc = false;
  boolean sxNeedRecalc = false;

  public void bundleChanged(BundleEvent ev) {
    Long    id = new Long(ev.getBundle().getBundleId());
    synchronized(bundles) {
      BX bx = (BX)bundles.get(id);
      
      switch(ev.getType()) {
      case BundleEvent.INSTALLED: 
        if(bx == null) {
          bx = new BX(this, ev.getBundle());
          bundles.put(id, bx);
          bxNeedRecalc = true;
        }
        break;
      case BundleEvent.UNINSTALLED:
        if(bx != null) {
          bundles.remove(id);
          bxNeedRecalc = true;
        }
        break;
      }
    }   
    repaint();
  }

  PackageAdmin pkgAdmin = null;

  public void serviceChanged(ServiceEvent ev) {
    synchronized(services) {
      ServiceReference sr       = ev.getServiceReference();
      String[]         objClass = (String[]) sr.getProperty(Constants.OBJECTCLASS);
      boolean          bRelease = true;
      SX               sx       = (SX)services.get(sr);
     
      boolean          isPAdmin = false;
      if (objClass !=  null) {
        for (int i=0; i<objClass.length; i++) {
          if (PackageAdmin.class.getName().equals(objClass[i])) {
            isPAdmin = true;
          }
        }
      }
      
      switch(ev.getType()) {
      case ServiceEvent.REGISTERED:
        if(isPAdmin) {
          if(pkgAdmin == null) {
            pkgAdmin = (PackageAdmin) Activator.getTargetBC().getService(sr);
            bRelease = false;
          }
        }
        if(sx == null) {
          sx = new SX(this,sr);
          services.put(sr, sx);
          sxNeedRecalc = true;
        }
        break;
      case ServiceEvent.UNREGISTERING:
        if(isPAdmin) {
          if(pkgAdmin != null) {
            pkgAdmin = null;
            bRelease = true;
          }
        }
        if(sx != null) {
          services.remove(sr);
          sxNeedRecalc = true;
        }
        break;
      case ServiceEvent.MODIFIED:
        break;
      }
      if(bRelease) {
        Activator.getTargetBC().ungetService(sr);
      }
    }
    repaint();
  }

  void recalcBundlePositions() {
    if(memG == null) {
      System.out.println("recalc - no memG");
      return;
    }
    synchronized(bundles) {
      int i = 0;
      int n = bundles.size();
      int cx = size.width / 2;
      int cy = size.height / 2;
      for(Iterator it = bundles.keySet().iterator(); it.hasNext(); ) {
        Long    id = (Long)it.next();
        BX bx = (BX)bundles.get(id);

        double a = deltaA + i * Math.PI * 2 / n;

        bx.setPos(cx - 20 + cx * .8 * Math.cos(a), cy + cy * .8 * Math.sin(a));
        bx.setAngle(a);
        i++;
      }
    }
  }

  void recalcServicePositions() {
    if(memG == null) {
      System.out.println("sx recalc - no memG");
      return;
    }
    synchronized(services) {
      int cx = size.width  / 2;
      int cy = size.height / 2;

      for(Iterator it = services.keySet().iterator(); it.hasNext(); ) {
        ServiceReference   sr = (ServiceReference)it.next();
        SX                 sx = (SX)services.get(sr);

        BX bx = (BX)bundles.get(sx.bid);
        if(bx == null) {
          System.out.println("No bundle for " + sx);
          continue;
        }
        double x = Math.random() * 10 + bx.x - (bx.x - cx) * .2;
        double y = Math.random() * 10 + bx.y - (bx.y - cy) * .2;

        sx.setPos(x, y);
      }
    }
  }

  int count = 0;


  public void run() {

    try {
      repaint();
      while(memG == null) {
        Thread.sleep(100);
      }
      while(bRun) {
        
        long startTime = System.currentTimeMillis();
        synchronized(paintLock) {
          updateAll();
          paintAll();
        }
        updateTime = System.currentTimeMillis() - startTime;
        
        long delta = targetTime - updateTime;
        delay = Math.max(2, delta);

        Thread.sleep(delay);
        totalTime = System.currentTimeMillis() - startTime;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void updateAll() {
    count++;
  }


  public void paintAll() {
    synchronized(paintLock) {
      Graphics g = getGraphics();
      
      if(g != null) {
        paint(g);
        g.dispose();
      }
    }
  }

  String indent(int level) {
    StringBuffer sb = new StringBuffer();
    while(level --> 0) sb.append(" ");
    
    return sb.toString();
  }

  void getDeps(StringBuffer sb, Hashtable lines, SpinItem item, int level) {
    if(item == null) return;

    String line = item.toString();
    if(lines.containsKey(line)) {
      return;
    }

    String s = indent(level);
    //    System.out.println(level + ": " + s + item.toString());
    if(!line.equals("System Bundle")) {
      sb.append(s + item.toString() + "\n");
      lines.put(line, line);
    }
    Vector v = item.getNext(SpinItem.DIR_FROM);
    if(v != null) {
      if(level > 3 && v.size() > 0) {
        sb.append(indent(level) + "...\n");
        return;
      }
      for(int i = 0; i < v.size(); i++) {
        SpinItem next = (SpinItem)v.elementAt(i);
        
        getDeps(sb, lines, next, level + 1);
      }
    }
  }

  void paintDeps(Graphics g, int x, int y) {
    SpinItem item = getInfoItem();

    if(item == null) return;

    StringBuffer sb = new StringBuffer();
    getDeps(sb, new Hashtable(), item, 0);


    String name = "";
    if(item instanceof SX) {
      name = "(" +
        BX.shortName(((SX)item).sr.getBundle()) +
        ")\n";
    }

    String s = "Imports from:\n" + name + sb.toString(); 

    paintBox(s, g, Color.white, Color.black, x, y, .8, 200, 200);
  }

  void paintHelp(Graphics g, int x, int y) {
    
    paintBox("F1/H \t- toggle help\n" + 
             "B    \t- toggle legend\n" + 
             "I    \t- toggle detail info\n" + 
             "D    \t- toggle dependencies\n" + 
             "left \t- previous item\n" + 
             "right\t- next item\n" + 
             "space\t- mark item\n" + 
             "C    \t- clear all marked\n" + 
             "Tab  \t- step dependencies\n" +
             "2    \t- toggle Graphics2D\n" + 
             "S    \t- toggle step font size\n" + 
             "3    \t- decrease font size\n" + 
             "4    \t- increase font size\n" + 
             "8    \t- rotate anti-clockwise\n" +
             "9    \t- rotate clockwise\n" +
             ".    \t- start/stop search mode\n",
             g, Color.white, Color.black, x, y);
  }

  void paintBox(String   s, 
                Graphics g, 
                Color    bgCol, 
                Color    fgCol,
                int      x, 
                int      y) {
    paintBox(s, g, bgCol, fgCol, x, y, 1.0, 200, 200);
  }
  

  void paintBox(String   s, 
                Graphics g, 
                Color    bgCol, 
                Color    fgCol,
                int      x, 
                int      y,
                double   size,
                int minWidth, int minHeight) {
    Object oldComp = null;
    if(use2D) {
      Graphics2D g2 = (Graphics2D)g;
      oldComp = g2.getComposite();
      g2.setComposite((AlphaComposite)alphaHalf);
    }

    String[] lines = split(s, "\n");

    int maxCols = 0;
    for(int i = 0; i <lines.length; i++) {
      if(lines[i].length() > maxCols) {
        maxCols = lines[i].length();
      }
    }

    Font font = getFont(size);

    g.setColor(bgCol);
    
    g.fill3DRect(x, y, 
                 Math.max(minWidth, font.getSize() * maxCols / 2 + 30), 
                 Math.max(minHeight, (font.getSize() + 3) * lines.length + 10), 
                 true);


    g.setFont(font);
    
    g.setColor(fgCol);

    x += 10;
    y += font.getSize() + 5;

    int x2 = x + font.getSize() * 8;

    if(use2D) {
      if(oldComp != null) {
        Graphics2D g2 = (Graphics2D)g;
        g2.setComposite((AlphaComposite)oldComp);
      }
    }

    for(int i = 0; i <lines.length; i++) {
      int ix = lines[i].indexOf("\t");
      if(ix != -1) {
        g.drawString(lines[i].substring(0, ix),  x, y);
        g.drawString(lines[i].substring(ix + 1), x+font.getSize()*4, y);
      } else {
        g.drawString(lines[i], x, y);
      }
      y += font.getSize() + 3;
    }
     
  }

  void paintBundles(Graphics g) {
    //    System.out.println("paintBundles()");
    if(bxNeedRecalc) {
      recalcBundlePositions();
      bxNeedRecalc = false;
    }
    synchronized(bundles) {
      for(Iterator it = bundles.keySet().iterator(); it.hasNext(); ) {
        Long    id = (Long)it.next();
        BX bx = (BX)bundles.get(id);
        bx.paint(g);
      }
    }
  }

 /**
   * Splits a string into words, using the <code>StringTokenizer</code> class.
   */
  public static String[] split(String s, String sep) {
    StringTokenizer st = new StringTokenizer(s, sep);
    int ntok = st.countTokens();
    String[] res = new String[ntok];
    for (int i = 0; i < ntok; i++) {
      res[i] = st.nextToken();
    }
    return res;
  }

  void zoom() {
    toScreen(center);

    for(Iterator it = bundles.keySet().iterator(); it.hasNext(); ) {
      Long    id = (Long)it.next();
      BX bx = (BX)bundles.get(id);
      toScreen(bx);
    }
    for(Iterator it = services.keySet().iterator(); it.hasNext(); ) {
      ServiceReference   sr = (ServiceReference)it.next();
      SX                 sx = (SX)services.get(sr);
      toScreen(sx);
    }
  }

  void paintServices(Graphics g) {
    //    System.out.println("paintServices()");
    if(sxNeedRecalc) {
      recalcServicePositions();
      sxNeedRecalc = false;
    }
    synchronized(services) {
      for(Iterator it = services.keySet().iterator(); it.hasNext(); ) {
        ServiceReference   sr = (ServiceReference)it.next();
        SX                 sx = (SX)services.get(sr);
        sx.paint(g);
      }
    }
  }

  boolean isActive(SpinItem item) {
    return 
      active.containsKey(item) 
      || item == mouseActive 
      || item == depActive;
  }

  boolean isPainting = false;

  boolean bAntiAlias = true;

  public void setAntiAlias(boolean b) {
    bAntiAlias = b;
  }

  public boolean getAntiAlias() {
    return bAntiAlias;
  }

  long bgTime, spriteTime, fgTime;

  public void update(Graphics g) {
    paint(g);
  }

  public void paint(Graphics _g) {
    //    System.out.println("paint()");
    if(isPainting) return;
    isPainting = true;

    synchronized(paintLock) {
      zoom();

      if(use2D) {
        if(alphaHalf == null) {
          alphaHalf = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .7f);
        }
      }

      Graphics g = _g;
      
      makeOff(false);
      
      if(memG == null) return;
      
      long start = System.currentTimeMillis();
      paintBg(memG);

      bgTime = System.currentTimeMillis() - start;

      if(use2D) {
        ((Graphics2D)memG).
          setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                           bAntiAlias 
                           ? RenderingHints.VALUE_ANTIALIAS_ON
                           : RenderingHints.VALUE_ANTIALIAS_OFF);
      }

      paintBundles(memG);
      spriteTime = System.currentTimeMillis() - bgTime - start;

      paintServices(memG);
      fgTime = System.currentTimeMillis() - spriteTime - start;
      

      if(bShowBundleLegend) {
        paintBundleStateLegend(memG);
      }

      SpinItem infoItem = getInfoItem();

      if(infoItem != null) {
        if(bShowBundleInfo) {
          infoItem.paintInfo(memG, 10, size.height - 100);
        }
      }

      if(bShowConsole) {
        console.paint(memG);
      }


      if(bShowFrameRate) {
        paintFrameRate(memG);
      }

      if(bShowHelp) {
        paintHelp(memG, size.width/2-100, 50);
      }
      if(bShowDeps) {
        paintDeps(memG, 
                  (hotX > size.width / 2) ? 40 : (size.width / 2), 
                  20);
      }

      if(bSearchMode) {
        paintSearch(memG, 10, size.height - 140);
      }

      if(use2D) {
        ((Graphics2D)memG).
          setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                           RenderingHints.VALUE_ANTIALIAS_OFF);
      }
      
      g.drawImage(memImage, 0, 0, null);
      
    }
    isPainting = false;
  }

  public SpinItem getInfoItem() {
    return depActive != null ? depActive : mouseActive;
  }
  public void paintBundleStateLegend(Graphics g) {
    paintCBox(g, 10, 10, BX.getColor(Bundle.INSTALLED),    "Installed");
    paintCBox(g, 10, 25, BX.getColor(Bundle.ACTIVE),       "Active");
    paintCBox(g, 10, 40, BX.getColor(Bundle.RESOLVED),     "Resolved");
    paintCBox(g, 10, 55, BX.getColor(Bundle.UNINSTALLED),  "Uninstalled");
    paintCBox(g, 10, 70, BX.getColor(Bundle.STOPPING),     "Stopping");
    paintCBox(g, 10, 85, BX.getColor(Bundle.STARTING),     "Starting");

    paintCBox(g, 10, 105, BX.importsFromColor,              "Imports from");
    paintCBox(g, 10, 120, BX.exportsToColor,                "Exports to");



    paintCBox(g, 10, size.height - 20, bgColor,             "F1 - help");    
  }


  public void paintCBox(Graphics g, int x, int y, Color c, String text) {
    int w = 12;
    int h = 12;

    g.setColor(c);
    g.fillRect(x, y, w, h);

    g.setColor(Color.gray);
    g.drawLine(x,   y, x+w, y);
    g.drawLine(x,   y, x,   y+h);

    g.setColor(Color.black);
    g.drawLine(x+w, y,   x+w, y+h);
    g.drawLine(x,   y+h, x+w, y+h);

    g.setColor(textColor);
    g.setFont(getFont(1.0));
    g.drawString(text, x + 30, y + 10);
  }
  
  public void paintFrameRate(Graphics g) {
    g.setColor(Color.black);
    g.drawString("fps: " + (int)(1000.0 / totalTime), 10, size.height - 5);
    g.drawString("delay: " + delay, 90, size.height - 5);
    g.drawString("bg: "      + bgTime, 10, size.height - 20);
    g.drawString("sprites: " + spriteTime, 10, size.height - 35);
    g.drawString("fg: "      + fgTime, 10, size.height - 50);
  }



  public void paintBg(Graphics g) {
    g.setColor(bgColor);
    g.fillRect(0, 0, size.width, size.height);
  }

  void makeOff(boolean bForce) {
    Dimension d = getSize();
    if(d != null) {
      if(bForce || 
         (size == null || d.width != size.width || d.height != size.height)) 
        {
          size = d;
          center.setPos(size.width / 2, size.height / 2);
          bxNeedRecalc = true;
          sxNeedRecalc = true;
          memImage = createImage(d.width, d.height);
          memG  = memImage.getGraphics();
          
        }
    }
  }
        
  public void start() {
    if(runner != null) {
      return;
    }

    runner = new Thread(this, "fwspin");
    bRun = true;
    runner.start();
  }

  public void stop() {
    Activator.getTargetBC().removeBundleListener(this);
    Activator.getTargetBC().removeServiceListener(this);

    if(runner != null) {
      bRun = false;
      try {
        runner.join(5 * 1000);

      } catch (Exception  e) {
        //
      }
    }
    runner = null;
  }

  int hotX;
  int hotY;

  SpinItem center;

  void toScreen(SpinItem item) {
    double sx = item.getX();
    double sy = item.getY();

    double dx = sx - hotX;
    double dy = sy - hotY;

    double d2 = dx * dx + dy * dy;
    
    double d  = Math.sqrt(d2);

    double maxDist = 100;

    if(bStepSize) {
      maxDist = 10;
    }
    
    d = Math.min(d, maxDist);

    double k = d / maxDist;

    k = 1.0  - Math.sqrt(k);

    /*    double m = 100;

    d = Math.max(0.0, Math.min(m, d));
    double k = m / (d + 1) / 10;
    */

    sx += k * dx;
    sy += k * dy;

    item.setSPos((int)sx, (int)sy, k);
  }

  BX getBX(Long id) {
    return (BX)bundles.get(id);
  }

  BX getBX(long id) {
    return getBX(new Long(id));
  }

  SX getSX(long id) {
    for(Iterator it = services.keySet().iterator(); it.hasNext(); ) {
      ServiceReference   sr = (ServiceReference)it.next();
      SX                 sx = (SX)services.get(sr);

      Long sid = (Long)sr.getProperty("service.id");
      if(sid.longValue() == id) {
        return sx;
      }
    }
    return null;
  }

  public void paintSearch(Graphics g, double x, double y) {

    paintBox(searchString, g, Color.white, Color.black, (int)x, (int)y, 
             .8, 300, 30);
  }

}


class SX extends SpinItem {
  ServiceReference sr;
  String className;
  String name;
  Long   bid;
  
  static String ignorePrefix = "com.gatespace.";

  Spin spin;

  public SX(Spin spin, ServiceReference sr) {
    this.sr = sr;
    this.spin = spin;

    bid = new Long(sr.getBundle().getBundleId());

    className = ((String[])sr.getProperty(Constants.OBJECTCLASS))[0]; // TODO: Display all objectclasses?
 
    if(className.startsWith(ignorePrefix)) {
      className = className.substring(ignorePrefix.length());
    }

    name = className + "/" + sr.getProperty(Constants.SERVICE_ID);

  }

  public void paint(Graphics g) {
    g.setFont(spin.getFont(fac + (isActive() ? 1.0 : .2)));
    g.setColor(isActive() ? Color.white : Color.gray);
    g.drawString(name, sx, sy);

    if(isActive()) {
      paintDependencies(g);
    }
  }



  public void paintInfo(Graphics g, double x, double y) {

    StringBuffer sb = new StringBuffer();

    sb.append("" + sr + "\n");

    String[] keys = sr.getPropertyKeys();
    for(int i = 0; keys != null && i < keys.length; i++) {
      String key = keys[i];
      Object val = sr.getProperty(key);

      if(val.getClass().isArray()) {
        Object[] vals = (Object[])val;
        if(vals.length > 0) {
          sb.append(keys[i] + " = " + vals[0] + "\n");
          for(int j = 1; j < vals.length; j++) {
            sb.append("   " + vals[j] + "\n");
          }
        }
      } else {
        sb.append(keys[i] + " = " + val + "\n");
      }
    }
    
    spin.paintBox(sb.toString(), g, Color.white, Color.black, (int)x, (int)y, 
                  .8, 300, 300);
  }

  Vector getNext(int dir) {

    TreeMap map = new TreeMap();

    if((dir & SpinItem.DIR_FROM) != 0) {
      Bundle[] bl = sr.getUsingBundles();
      for(int i = 0; bl != null && i < bl.length; i++) {
        BX bx = spin.getBX(bl[i].getBundleId());
        if(bx != null) {
          map.put(new Long(bx.b.getBundleId()), bx);
        }
      }
    }

    Vector v = new Vector();
    for(Iterator it = map.keySet().iterator(); it.hasNext(); ) {
      Long key      = (Long)it.next();
      BX   bx       = (BX)map.get(key);
      v.addElement(bx);
    }
    return v;
  }

  public void paintDependencies(Graphics g) {

    BX bx = spin.getBX(sr.getBundle().getBundleId());

    if(bx != null) {
      g.setColor(Color.gray);
      g.drawLine(sx, sy, bx.sx, bx.sy);
    }

    Bundle[] bl = sr.getUsingBundles();
    for(int i = 0; bl != null && i < bl.length; i++) {
      paintUsing(g, bl[i]);
    }

  }

  public boolean isActive() {
    return spin.isActive(this);
  }

  public void paintUsing(Graphics g, Bundle b) {
    BX bx = spin.getBX(b.getBundleId());
    if(bx == null) {
      return;
    }

    double cx = spin.center.getSX();
    double cy = spin.center.getSY();

    g.setColor(BX.exportsToColor);

    if(spin.use2D) {
      CubicCurve2D.Double curve = 
        new CubicCurve2D.Double (sx, sy,
                                 cx, cy, 
                                 cx, cy,
                                 bx.sx, bx.sy);
      
      
      ((Graphics2D)g).draw(curve);
    } else {
      drawSpline(g, 
                 sx, sy,
                 cx, cy, 
                 cx, cy,
                 bx.sx, bx.sy, 5);
    }
  }
  public String toString() {
    return name;
  }
}


class BX extends SpinItem {
  Bundle b;
  String name;
  Spin   spin;

  public BX(Spin spin, Bundle b) {
    this.b    = b;
    this.spin = spin;

    name = shortName(b);

  }

  public static Color importsFromColor = Color.blue;
  public static Color exportsToColor   = Color.gray;

  public boolean isActive() {
    return spin.isActive(this);
  }


  Vector getNext(int dir) {

    Map map = new TreeMap();

    if(spin.pkgAdmin != null) {

      ExportedPackage[] exp = spin.pkgAdmin.getExportedPackages(b);

      if((dir & SpinItem.DIR_TO) != 0) {
        for(int i = 0; exp != null && i < exp.length; i++) {
          Bundle[] bl =  exp[i].getImportingBundles();
          for(int j = 0; bl != null && j < bl.length; j++) {
            BX bx = spin.getBX(bl[j].getBundleId());
            if(bx != null) {
              map.put(new Long(bx.b.getBundleId()), bx);
            }
          }
        }
      }

      if((dir & SpinItem.DIR_FROM) != 0) {
        for(Iterator it = spin.bundles.keySet().iterator(); it.hasNext(); ) {
          Long key      = (Long)it.next();
          BX   bx       = (BX)spin.bundles.get(key);
          
          exp = spin.pkgAdmin.getExportedPackages(bx.b);
          boolean done = false;
          for(int i = 0; !done && exp != null && i < exp.length; i++) {
            Bundle[] bl =  exp[i].getImportingBundles();
            for(int j = 0; !done && bl != null && j < bl.length; j++) {
              if(bl[j].getBundleId() == b.getBundleId()) {
                map.put(new Long(bx.b.getBundleId()), bx);
                done = true;
              }
            }
          }
        }
      }
    }

    map.remove(new Long(b.getBundleId()));

    Vector v = new Vector();
    for(Iterator it = map.keySet().iterator(); it.hasNext(); ) {
      Long key      = (Long)it.next();
      BX   bx       = (BX)map.get(key);
      v.addElement(bx);
    }

    return v;
  }


  public void paintDependencies(Graphics g) {
    if(spin.pkgAdmin == null) return;

    ExportedPackage[] exp = spin.pkgAdmin.getExportedPackages(b);

    g.setColor(exportsToColor);

    for(int i = 0; exp != null && i < exp.length; i++) {
      Bundle[] bl =  exp[i].getImportingBundles();
      for(int j = 0; bl != null && j < bl.length; j++) {
        BX bx = spin.getBX(bl[j].getBundleId());
        if(bx != null) {
          paintUsing(g, bx);
        }
      }
    }

    g.setColor(importsFromColor);

    for(Iterator it = spin.bundles.keySet().iterator(); it.hasNext(); ) {
      Long key      = (Long)it.next();
      BX   bx       = (BX)spin.bundles.get(key);

      exp = spin.pkgAdmin.getExportedPackages(bx.b);
      boolean done = false;
      for(int i = 0; !done && exp != null && i < exp.length; i++) {
        Bundle[] bl =  exp[i].getImportingBundles();
        for(int j = 0; !done && bl != null && j < bl.length; j++) {
          if(bl[j].getBundleId() == b.getBundleId()) {
            paintUsing(g, bx);
            done = true;
          }
        }
      }
    }
  }


  public void paintInfo(Graphics g, double x, double y) {

    StringBuffer sb = new StringBuffer();

    sb.append(b.getLocation() + ", id=" + b.getBundleId() + "\n");

    Dictionary headers = b.getHeaders();

    for(Enumeration e = headers.keys(); e.hasMoreElements(); ) {
      String key = (String)e.nextElement();
      String val = (String)headers.get(key);

      sb.append(key + ": " + val + "\n");
    }
    spin.paintBox(sb.toString(), g, Color.white, Color.black, (int)x, (int)y, 
                  .8, 300, 300);
  }


  public void paintUsing(Graphics g, BX bx) {

    double cx = spin.center.getSX();
    double cy = spin.center.getSY();

    if(spin.use2D) {
      CubicCurve2D.Double curve = 
        new CubicCurve2D.Double (sx, sy,
                                 cx, cy, 
                                 cx, cy,
                                 bx.sx, bx.sy);
      
      ((Graphics2D)g).draw(curve);
    } else {
      drawSpline(g, sx, sy,
                 cx, cy, 
                 cx, cy,
                 bx.sx, bx.sy, 5);
    }
    
  }


  static Color installedColor   = Color.gray;
  static Color uninstalledColor = Color.gray.darker();
  static Color resolvedColor    = new Color(90, 90, 255);
  static Color activeColor      = Color.yellow;
  static Color stoppingColor    = Color.red;
  static Color startingColor    = new Color(255, 0, 255);

  public static Color getColor(int state) {
    switch(state) {
    case Bundle.INSTALLED:
      return installedColor;
    case Bundle.ACTIVE:
      return activeColor;
    case Bundle.RESOLVED:
      return resolvedColor;
    case Bundle.UNINSTALLED:
      return uninstalledColor;
    case Bundle.STARTING:
      return startingColor;
    case Bundle.STOPPING: 
      return stoppingColor;
    } 
    
    return Color.black;
  }

  public void paint(Graphics g) {
    g.setFont(spin.getFont(fac + (isActive() ? 1.0 : .8)));

    if(isActive()) {
      g.setColor(Color.white);
    } else {
      g.setColor(getColor(b.getState()));
    }
    g.drawString(name, sx, sy);

    if(isActive()) {
      paintDependencies(g);
    }
  }

  public static String shortName(Bundle b) {
    String s = b.getLocation();
    int ix = s.lastIndexOf("/");
    if(ix == -1) ix = s.lastIndexOf("\\");
    if(ix != -1) {
      s = s.substring(ix + 1);
    }
    if(s.endsWith(".jar")) s = s.substring(0, s.length() - 4);
    return s;
  }

  public String toString() {
    return name;
  }
}







