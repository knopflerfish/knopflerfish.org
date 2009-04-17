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

package org.knopflerfish.bundle.desktop.swing;

import org.osgi.framework.*;

import org.knopflerfish.service.desktop.*;

import javax.swing.*;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class TimeLineDisplayer extends DefaultSwingBundleDisplayer {

  ListSelectionModel model;

  Desktop            desktop;

  // Long (time) -> BundleEvent
  SortedMap bundleEvents = new TreeMap();

  // Bundle -> (Long (time) -> ServiceEvent)
  Map bundleToServiceEvents = new HashMap();
  
  public TimeLineDisplayer(BundleContext bc) {
    super(bc, "Time line", "Time line of bundle events", false);

    desktop = Activator.desktop;
  }


  public void bundleChanged(BundleEvent ev) {
    super.bundleChanged(ev);

    Bundle b = ev.getBundle();
    
    FWEvent fwe = new FWEvent(b, ev.getType());
    bundleEvents.put(new Long(fwe.getId()), fwe);
    
    repaintComponents();
  }

  public void serviceChanged(ServiceEvent ev) {
    ServiceReference sr = ev.getServiceReference();
    
    FWEvent fwe = new FWEvent(sr, ev.getType());

    Map serviceEvents = (Map)bundleToServiceEvents.get(sr.getBundle());
    if(serviceEvents == null) {
      serviceEvents = new TreeMap(); 
      bundleToServiceEvents.put(sr.getBundle(), serviceEvents);
    }

    serviceEvents.put(new Long(fwe.getId()), fwe);

    repaintComponents();
  }



  void clear() {
    bundleEvents.clear();
    bundleToServiceEvents.clear();
  }

  public JComponent newJComponent() {
    JTimeLine tl = new JTimeLine();
    tl.reloadAll(false);

    return tl;
  }


  JScrollPane  scroll;

  class JTimeLine extends JPanel {

    JBundleGraph graph;  
    
    public JTimeLine() {
      setLayout(new BorderLayout());
      
      setBackground(Color.white);
      
      graph  = new JBundleGraph();      

      scroll = new JScrollPane(graph);

      add(scroll, BorderLayout.CENTER);
      
      JToolBar cmdPanel = new JToolBar(JToolBar.VERTICAL);
      cmdPanel.setFloatable(false);

      cmdPanel.add(new JButton() { {
	setIcon(desktop.reloadIcon);
	setToolTipText("Reload events");
	addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent ev) {
	      reloadAll(true);
	    }
	  });
      }});
      cmdPanel.add(new JToolBar.Separator());

      
      cmdPanel.add(new JButton() { {
	setIcon(desktop.magPlusIcon);
	setToolTipText("Zoom in");
	addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent ev) {
	      Point p = scroll.getViewport().getViewPosition();
	      graph.doZoom((int)(p.x * zoomK),
			       (int)(p.y * zoomK),
			   zoomK, zoomK);
	    }
	  });
      }});
      
      cmdPanel.add(new JButton() { {
	setIcon(desktop.magMinusIcon);
	setToolTipText("Zoom out");
	addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent ev) {
	      Point p = scroll.getViewport().getViewPosition();
	      graph.doZoom((int)(p.x / zoomK),
			   (int)(p.y / zoomK),
			   1/zoomK, 1/zoomK);
	    }
	  });
      }});
      
      cmdPanel.add(new JButton() { {
	setIcon(desktop.magFitIcon);
	setToolTipText("Zoom out all");
	addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent ev) {
	      graph.doZoomOutAll();
	    }
	  });
      }});



      
      add(cmdPanel, BorderLayout.WEST);      
    }

    void reloadAll(boolean bAsk) {
      int n = 0;
      if(bAsk) {
	Object[] options = {Strings.get("yes"), 
			    Strings.get("cancel")};
	
	
	n =JOptionPane
	  .showOptionDialog(Activator.desktop.frame,
			    Strings.get("This will clear all old events and reload with current framework status"),
			    Strings.get("Reload all events?"),
			    JOptionPane.YES_NO_OPTION,
			    JOptionPane.QUESTION_MESSAGE,
			    null,
			    options,
			    options[1]);
      }

      if(n == 0) {
	clear();
	getAllBundles();
	getAllServices();
	invalidate();
      }

    }

    double zoomK = 1.1;
    
    // set any currently hilited bundle by in paintBundles()
    Bundle hiliteBundle = null;
    int    hiliteBundleIx = -1;
    
    
    // The component actually drawing the time line
    class JBundleGraph extends JPanel  {
      
      double zoomFac = 1.0;
      
      int mouseX = 0;
      int mouseY = 0;
      
      int mouseDragX = 0;
      int mouseDragY = 0;
      boolean bIsDragging = false;
      
      public JBundleGraph() {
	
	addMouseMotionListener(new MouseMotionListener() {
	    public void mouseMoved(MouseEvent ev) {
	      saveMousePos(ev);
	      repaint();
	    }
	    public void mouseDragged(MouseEvent ev) {
	      bIsDragging = true;
	      mouseDragX = ev.getX();
	      mouseDragY = ev.getY();
	      repaint();
	    }
	  });
	
	
	addMouseListener(new MouseAdapter() {
	    public void mousePressed(MouseEvent ev) {
	      saveMousePos(ev);
	    }
	    
	    public void mouseReleased(MouseEvent ev) {
	      bIsDragging = false;
	      mouseDragX = ev.getX();
	      mouseDragY = ev.getY();

	      zoomTo(mouseX, mouseY, mouseDragX, mouseDragY);
	      repaint();
	    }
	    
	    
	    public void mouseClicked(MouseEvent ev) {
	      saveMousePos(ev);
	      double k = zoomK;
	      if((ev.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
		bundleSelModel.clearSelection();
		if(hiliteBundle != null) {
		  bundleSelModel.setSelected(hiliteBundle.getBundleId(), true);
		}
	      } else {
		double kx = 1.0;
		double ky = 1.0;
		if((ev.getModifiers() & MouseEvent.ALT_MASK) != 0) {
		  kx = k;
		}
		if((ev.getModifiers() & MouseEvent.CTRL_MASK) != 0) {
		  ky = k;
		}
		if((ev.getModifiers() & MouseEvent.SHIFT_MASK) != 0) {
		  kx = 1/ kx;
		  ky = 1/ ky;
		}
		doZoom(ev.getX(), ev.getY(), kx, ky);
	      }
	    }
	  });
      }
      
      void saveMousePos(MouseEvent ev) {
	mouseX = ev.getX();
	mouseY = ev.getY();
      }
      
      void doZoom(int left, int top, double kx, double ky) {
	
	JViewport viewPort = scroll.getViewport();
	
	Dimension size         = getSize();
	Dimension viewSize     = viewPort.getExtentSize();
	
	double w = Math.min(10000, size.width * kx);
	double h = Math.min(10000, size.height * ky);
	Dimension newSize  = new Dimension((int)w, (int)h);

	Rectangle rect = new Rectangle(left, top,
				       viewSize.width,
				       viewSize.height);

	setPreferredSize(newSize);
	revalidate();

	scrollRectToVisible(rect);
	revalidate();
      }

      void zoomTo(int x1, int y1, int x2, int y2) {
	
	int dx = Math.abs(x2 - x1);
	int dy = Math.abs(y2 - y1);
	
	if(dx <= 15 || dy <= 15) {
	  return;
	}

	Dimension size     = getSize();

	double kx = (double)size.width / dx;
	double ky = (double)size.height / dy;

	doZoom((int)(x1 * kx), (int)(y1 * ky), kx, ky);
      }
      
      void doZoomOutAll() {
	
	JViewport viewPort = scroll.getViewport();
	
	Dimension size     = getSize();
	Dimension newSize  = viewPort.getExtentSize();
	
	setPreferredSize(newSize);
	revalidate();
      }
      
      protected void paintComponent(Graphics g) {
	try {
	  Dimension size = getSize();
	  
	  if(isOpaque()) {
	    g.setColor(bgColor);
	    g.fillRect(0,0,size.width, size.height);
	  }
	  paintAll(g);
	} catch (Exception e) {
	  Activator.log.error("Failed to paint", e);
	}
      }
      
      public void paintAll(Graphics _g) {
	try { 
	  Graphics2D g = (Graphics2D)_g;
	  
	  
	  Dimension size = getSize();
	  
	  
	  g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
			     RenderingHints.VALUE_ANTIALIAS_ON);
	  
	  paintBundles(g);
	  paintNearest(g);
	  
	  g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
			     RenderingHints.VALUE_ANTIALIAS_OFF);
	  
	  paintDrag(g);
	} catch (Exception e) {
	  e.printStackTrace();
	}
      }
      
      void paintDrag(Graphics2D g) {
	if(!bIsDragging) {
	  return;
	}
	
	g.setColor(Color.black);
	int x = mouseX;
	int y = mouseY;
	
	int w = mouseDragX - mouseX;
	int h = mouseDragY - mouseY;
	
	if(w < 0) { x += w; w = -w; }
	if(h < 0) { y += h; h = -h; }
	
	g.drawRect(x, y, w, h); 
      }
      
      Color bundleLineColor           = new Color(200,200,200);
      Color bundleSelectedLineColor   = new Color(0,0,0);
      Color bundleHiliteColor         = new Color(230,230,230);
      Color bundleNameColor           = new Color(100,100,100);
      Color bundleEventColor          = new Color(0,  0,  0);
      Color bundleTextColor           = new Color(0,0,0);
      Color bgColor                   = new Color(255,255,255);
      
      int hiliteHeight        = 12;
      

      void paintNearest(Graphics g) {
	FWEvent fwe = getNearest(mouseX, mouseY);

	if(fwe != null) {
	  setToolTipText(fwe.toString());
	  g.setColor(Color.black);
	  g.drawArc(fwe.x - 10, fwe.y - 11,
		    16, 16, 
		    0, 360);
	} else {
	  setToolTipText(null);
	}
      }

      
      FWEvent getNearest(int x, int y) {
	double   bestDist  = Double.MAX_VALUE;
	FWEvent  bestEvent = null;

	for(Iterator itb = bundleToServiceEvents.keySet().iterator(); itb.hasNext();) {
	  Bundle b = (Bundle)itb.next();
	  Map serviceEvents = (Map)bundleToServiceEvents.get(b);

	  for(Iterator it = serviceEvents.keySet().iterator(); it.hasNext(); ) {
	    Long        key  = (Long)it.next();
	    FWEvent     fwe  = (FWEvent)serviceEvents.get(key);
	    
	    double dx    = x - fwe.x;
	    double dy    = y - fwe.y;
	    double dist2 = dx * dx + dy * dy;
	    
	    if(dist2 <= bestDist) {
	      bestDist  = dist2;
	      bestEvent = fwe;
	    }
	  }
	}

	for(Iterator it = bundleEvents.keySet().iterator(); it.hasNext(); ) {
	  Long        key  = (Long)it.next();
	  FWEvent     fwe  = (FWEvent)bundleEvents.get(key);

	  int dx = x - fwe.x;
	  int dy = y - fwe.y;
	  int dist2 = dx * dx + dy * dy;
	  

	  if(dist2 <= bestDist) {
	    bestDist  = dist2;
	    bestEvent = fwe;
	  }
	}

	return bestEvent;
      }
	    

      void paintServices(Graphics g, Bundle b, 
			 long first, 
			 int offsetX, 
			 int offsetY,
			 double kx, double ky) {
	Map serviceEvents = (Map)bundleToServiceEvents.get(b);

	if(serviceEvents == null) {
	  return;
	}

	for(Iterator it = serviceEvents.keySet().iterator(); it.hasNext(); ) {
	  Long        key  = (Long)it.next();
	  FWEvent     fwe  = (FWEvent)serviceEvents.get(key);
	  long        time = fwe.getTime();
	 
	  ServiceReference sr = fwe.getServiceReference();
	  Bundle           b2 = sr.getBundle();
	  if(b2 != null && b.getBundleId() == b2.getBundleId()) {
	    int y = (int)(b.getBundleId() * ky) + offsetY;
	    int x = (int)((time - first) * kx) + offsetX;

	    g.setColor(Color.black);

	    fwe.setPos(x, y + 3);
	    
	    g.drawLine(fwe.x, fwe.y, fwe.x, fwe.y - 3);

	  }
	}
      }

      void paintBundles(Graphics g) {

	Bundle[] bundles = getBundleArray();
	
	if(bundleEvents.size() == 0) {
	  return;
	}
	
	Dimension size = getSize();
	
	int offsetY = 10;
	int offsetX = 10;
	
	
	long first = ((FWEvent)bundleEvents.get(bundleEvents.firstKey())).getTime();
	long last = ((FWEvent)bundleEvents.get(bundleEvents.lastKey())).getTime();
	
	if(last == first) {
	  last = first + 1;
	}
	
	
	long diff = last - first;
	
	double ky = (size.height - offsetY * 2) / (double)bundles.length;
	double kx = (size.width - offsetX * 2) / (double)diff;
	
	int bundleH = (int)((size.height - offsetY/2) / bundles.length);

	Point viewPoint = scroll.getViewport().getViewPosition();

	
	int detailLevel = 0;
	if(bundleH < 12) {
	  detailLevel = 0;
	} else if(bundleH < 20) {
	  detailLevel = 1;
	} else {
	  detailLevel = 2;
	}
	
	int imageSize = 12;
	
	Font font = getSizedFont((bundleH - 10) / 40.0);
	
	
	hiliteBundle   = null;
	hiliteBundleIx = -1;
	
	for(int i = 0; i < bundles.length; i++) {
	  Bundle b = bundles[i];
	  
	  int y = (int)(b.getBundleId() * ky + offsetY);
	  
	  if(mouseY > y - bundleH/2 && mouseY < y + bundleH/2) {
	    hiliteBundle = b;
	    hiliteBundleIx = i;
	    
	    g.setColor(bundleHiliteColor);
	    g.fillRect(offsetX, 
		       y,
		       size.width - offsetX,
		       hiliteHeight);
	  }
	  
	  if(bundleSelModel.isSelected(b.getBundleId())) {
	    g.setColor(bundleSelectedLineColor);
	  } else {
	    g.setColor(bundleLineColor);
	  }
	  g.drawLine(offsetX, 
		     y,
		     size.width - offsetX,
		     y);
	  
	  g.setFont(font);
	  g.setColor(bundleNameColor);
	  
	  g.drawString(Util.getBundleName(b),
		       offsetX,
		       y + font.getSize() + 1);

	  paintServices(g, b, first, offsetX, offsetY, kx, ky);
	}
	
	
	for(Iterator it = bundleEvents.keySet().iterator(); it.hasNext(); ) {
	  Long        key  = (Long)it.next();
	  FWEvent     fwe  = (FWEvent)bundleEvents.get(key);
	  long        time = fwe.getTime();
	  
	  int y = (int)(fwe.getBundle().getBundleId() * ky) + offsetY;
	  int x = (int)((time - first) * kx) + offsetX;
	  
	  fwe.setPos(x, y);

	  g.setColor(bundleEventColor);
	  
	  ImageIcon icon = desktop.getBundleEventIcon(fwe.getType());
	  
	  if(icon != null) {
	    if(imageSize == -1) {
	      g.drawImage(icon.getImage(), x, y, null);
	    } else {
	      g.drawImage(icon.getImage(), 
			  x-10, y-11, 16, 16, null);
	    }
	  } else {
	    g.drawLine(x, y, x, y-3);
	    String s = Util.bundleEventName(fwe.getType());
	    g.drawString(s, x, y);
	  }
	}
	
      }
    }
  }

  static int idCount = 0;
  
  class FWEvent implements Comparable {
    
    long time;
    int  id;
    
    BundleEvent      bundleEvent;
    Bundle           bundle;
    ServiceReference sr;
    int              eventType;
    int              x = -1;
    int              y = -1;
      
    FWEvent          ref;
    
    
    public FWEvent(Bundle b, FWEvent ref) {
      this.id        = idCount++;
      this.time      = System.currentTimeMillis();
      this.bundle    = b; 
      this.ref       = ref;
    }
    
    public FWEvent(Bundle b, int eventType) {
      this.id        = idCount++;
      this.time      = System.currentTimeMillis();
      this.bundle    = b;
      this.eventType = eventType;
    }
    
    public FWEvent(ServiceReference sr, int eventType) {
      this.id        = idCount++;
      this.time      = System.currentTimeMillis();
      this.sr        = sr;
      this.eventType = eventType;
    }
    
    public void setPos(int x, int y) {
      this.x = x;
      this.y = y;
    }

    public Bundle getBundle() {
      return bundle;
    }
    
    public ServiceReference getServiceReference() {
      return sr;
    }
    
    public int getType() {
      return eventType;
    }
    
    public boolean isBundle() {
      return bundle != null && ref == null;
    }
    
    public boolean isSR() {
      return sr != null;
    }
    
    public boolean isRef() {
      return ref != null;
    }
    
    public long getTime() {
      return time;
    }
    
    public int getId() {
      return id;
    }
    
    public int compareTo(Object other) {
      FWEvent e = (FWEvent)other;
      
      return (int)(id - e.id);
    }
    
    public void paint(Graphics2D g, int x, int y) {
      
    }
    
    public String toString() {
      return toString(false);
    }

    public String toString(boolean bCoord) {
      StringBuffer sb = new StringBuffer();
      

      
      if(isBundle()) {
	sb.append(Util.getBundleName(getBundle()));
	sb.append(" ");
	sb.append(Util.bundleEventName(getType()));
      } else if(isSR()) {
	sb.append("#" + getServiceReference().getProperty(Constants.SERVICE_ID));
	String[] sl = (String[])getServiceReference().getProperty(Constants.OBJECTCLASS);
	sb.append(" ");
	for(int i = 0; i < sl.length; i++) {
	  sb.append(sl[i]);
	  if(i < sl.length - 1) {
	    sb.append("/");
	  }
	}
	sb.append(" ");
	sb.append(Util.serviceEventName(getType()));
      }

      sb.append(" " + (new Date(time)).toString());
      if(bCoord) {
	sb.append(" (" + x + ", " + y + ")");
      }
      return sb.toString();
    }
  } 

  
  Font[] fonts = null;
  
  Font getSizedFont(double size) {
    int min = 5; 
    int max = 19;
      
    if(fonts == null) {
      fonts = new Font[10];
      
      for(int i = 0; i < fonts.length; i++) {
	double k = (double)i / fonts.length;
	fonts[i]  = new Font("dialog", 
			     Font.PLAIN, 
			     (int)(min + k * (max - min)));  
      }
    }
    
    int n = (int)(size * (fonts.length - 1));
    n = Math.max(0, Math.min(n, fonts.length - 1));
    
    return fonts[n];
    
  }
}
