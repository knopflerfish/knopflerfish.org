package org.knopflerfish.bundle.desktop.swing;


import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.font.TextAttribute;
import java.util.*;
import java.awt.geom.AffineTransform;
import javax.swing.border.*;
import org.osgi.framework.*;
import java.awt.geom.Point2D;
import org.knopflerfish.bundle.desktop.swing.graph.*;
import java.awt.geom.*;

public abstract class JSoftGraph extends JPanel {

  Point2D mousePoint = new Point2D.Double(-1, -1);
  Point2D dragStart  = new Point2D.Double(-1, -1);

  Object mouseObject = null;

  Node currentNode;

  Map currentLinkMap = new LinkedHashMap();

  Color linkColor = Color.black;
  Color textColor = Color.black;
  Color fillColor  = new Color(180, 180, 255);
  Color fillColor2 = new Color(255, 255, 200);
  Color lineColor  = new Color(150, 150, 255);

  Set selectedNodes = new HashSet();
  Node hoverNode = null;

  Color topColor    =  getBackground();
  Color bottomColor =  Util.rgbInterpolate(topColor, new Color(50, 120, 200), .9);

  double outMinDragStart = 0;
  double outMaxDragStart = 0;
  double outSizeDragStart = 0;

  double inMinDragStart = 0;
  double inMaxDragStart = 0;
  double inSizeDragStart = 0;
  int dragWhere;

  public JSoftGraph() {
    setPreferredSize(new Dimension(400, 400));
    setFocusable(true);
    addKeyListener(new KeyAdapter() {
        public void keyPressed(KeyEvent ev) {
          JSoftGraph.this.keyPressed(ev);
        }
      });

    addMouseListener(new MouseAdapter() {
        public void mouseReleased(MouseEvent ev) {
          dragWhere = 0;
        }

        public void mousePressed(MouseEvent ev) {
          dragStart = new Point2D.Double(ev.getX(), ev.getY());

          dragWhere = 0;

          outMinDragStart = currentNode.getOutMin();
          outMaxDragStart = currentNode.getOutMax();
          outSizeDragStart = outMaxDragStart - outMinDragStart;

          inMinDragStart = currentNode.getInMin();
          inMaxDragStart = currentNode.getInMax();
          inSizeDragStart = inMaxDragStart - inMinDragStart;
        }
        
        public void mouseClicked(MouseEvent ev) {
          requestFocus();
          if(ev.getButton() != 1) {
            return;
          }
          mousePoint = new Point2D.Double(ev.getX(), ev.getY());
          Node node = findNearestNode(mousePoint);
          if(node != null) {
            if(ev.getClickCount() >= 2) {
              nodeClicked(node, ev);
            } else {
              nodeSelected(node, ev);
            }
          } else {
            noneSelected(ev);
          }
        }
      });
    addMouseMotionListener(new MouseMotionAdapter() {
        public void 	mouseDragged(MouseEvent ev) {
          mousePoint = new Point2D.Double(ev.getX(), ev.getY());
          if(dragStart != null && currentNode != null) {

            if(inArcActive || outArcActive) {
              if(dragWhere == 0) {
                if(currentNode.getPoint().getY() < dragStart.getY()) {
                  dragWhere = -1;
                } else {
                  dragWhere = 1;
                }
              }
              if(dragWhere == -1) {
                handleInChange(currentNode, ev);
              } else {
                handleOutChange(currentNode, ev);
              }
            }
            bNeedLayout = true;
            mouseObject = null;
            hoverNode   = null;
            repaint();
          }
        }

        public void 	mouseMoved(MouseEvent ev) {
          mouseObject = null;
          mousePoint = new Point2D.Double(ev.getX(), ev.getY());
          if(currentNode != null) {            
            double dist = mousePoint.distance(currentNode.getPoint());
            if(dist < arcRadius) {
              if(currentNode.getPoint().getY() < ev.getY()) {
                outArcActive = false;
                inArcActive  = true;
              } else {
                outArcActive = true;
                inArcActive  = false;
              }
            } else {
              outArcActive = false;
              inArcActive  = false;
            }
          }
          hoverNode = findNearestNode(mousePoint);
          repaint();
        }
      });
  }

  public void keyPressed(KeyEvent ev) {
  }


  int minArcSize = 3;

  void handleOutChange(Node node, MouseEvent ev) {
    double dx = ev.getX() - dragStart.getX();
    double dy = ev.getY() - dragStart.getY();
    

    double min  = node.getOutMin();
    double max  = node.getOutMax();
    double size = max - min;
    int nTotal = node.getOutLinks().size();
    
    if(nTotal > minArcSize) {
      double vx = outMinDragStart - dx / 6;
      size = Math.min(nTotal, outSizeDragStart - dy / 12);
      
      if(size < minArcSize) {
        size = minArcSize;
      }

      if(size+min >= nTotal) {
        double d =(size+min) - nTotal;
        vx = Math.max(0, vx - d);
        if(vx < 0) {
          vx = 0;
        }
        min = vx;
        size = nTotal-min;
      }
      
      if(vx < 0) {
        vx = 0;
      }
      if(vx + size > nTotal) {
        vx = nTotal - size;
      }
      node.setOutMin(vx);
      node.setOutMax(vx + size);
      
    }
  }

  void handleInChange(Node node, MouseEvent ev) {
    double dx = ev.getX() - dragStart.getX();
    double dy = ev.getY() - dragStart.getY();
    

    double min  = node.getInMin();
    double max  = node.getInMax();
    double size = max - min;
    int nTotal = node.getInLinks().size();
    
    if(nTotal > minArcSize) {
      double vx = inMinDragStart + dx / 6;
      size = inSizeDragStart + dy / 12;
      
      if(size < minArcSize) {
        size = minArcSize;
      }
      if(size+min > nTotal) {
        size = nTotal-min;
      }
      
      if(vx < 0) {
        vx = 0;
      }
      if(vx + size > nTotal) {
        vx = nTotal - size;
      }
      node.setInMin(vx);
      node.setInMax(vx + size);
    }
  }

  public void noneSelected(MouseEvent ev) {
  }

  public void nodeClicked(Node node, MouseEvent ev) {
  }

  public void nodeSelected(Node node, MouseEvent ev) {
  }


  public abstract Node makeRootNode();

  public void close() {
  }

  Node findNearestNode(Point2D mp) {
    return findNearestNode(mp, false);
  }

  Node findNearestNode(Point2D mp, boolean bPrint) {
    double minDist = 20;

    if(currentNode != null && currentLinkMap != null && currentLinkMap.size() == 0) {
      Point2D p1 = currentNode.getPoint();
      if(p1 != null) {
        double d = mp.distance(p1);
        if(d <= minDist) {
          return currentNode;
        }
      }
    }

    // Map linkMap = new HashMap();
    // layoutNode(currentNode, linkMap);
    Map linkMap = currentLinkMap != null ? currentLinkMap : Collections.EMPTY_MAP;
    
    Node minNode = null;
    for(Iterator it = linkMap.keySet().iterator(); it.hasNext(); ) {
      Link link = (Link)it.next();
      Node n1 = link.getFrom();
      Node n2 = link.getTo();
      
      Point2D p1 = n1.getPoint();
      if(p1 != null) {
        double d = mp.distance(p1);
        if(d <= minDist) {
          minDist = d;
          minNode = n1;
        }
      }
      Point2D p2 = n2.getPoint();
      if(p2 != null) {
        double d = mp.distance(p2);
        if(d <= minDist) {
          minDist = d;
          minNode = n2;
        }
      }
    }

    if(minNode == null) {
      if(mouseObject != null && (mouseObject instanceof Link)) {
        Link  link = (Link)mouseObject;
        Node node = link.getType() >= 0 ? link.getTo() : link.getFrom();
        if(!(node instanceof EmptyNode)) {
          return node;
        }
      }
    }
    return minNode;
    
  }

  Object lock = new Object();

  static final int STATE_NONE       = 0;
  static final int STATE_PRE        = 1;
  static final int STATE_INITFADE   = 2;
  static final int STATE_SHOWTARGET = 3;
  static final int STATE_FADE       = 4;
  static final int STATE_STOPFADE   = 5;

  javax.swing.Timer timer;
  int               speed = 50;
  int               fadestate = STATE_NONE;
  double            fadeK = 0.0;
  boolean           bDirtyTimer = false;

  void startFade() {
    if(timer != null || fadestate != STATE_NONE) {
      bDirtyTimer = true;
      return;
    }

    
    bDirtyTimer = false;
    fadestate   = STATE_INITFADE;
    timer       = new javax.swing.Timer(speed, timerAction);
    timer.start();    
  }

  int blinkN = 0;

  Color[] blinkColor = new Color[] {
    Color.blue,
    Color.black,   
  };
  Map allLinkMap = new LinkedHashMap();
  
  Map nodeOldMap  = new HashMap();
  Map nodeNewMap  = new HashMap();
  Map nodeCurrMap = new HashMap();

  Node newNode;
  Map  newLinkMap;
  Node oldNode;
  Map  oldLinkMap;

  Point2D center;

  
  ActionListener timerAction = new ActionListener() {
      public void actionPerformed(ActionEvent ev) {
        switch(fadestate) {
        case STATE_PRE:
          fadestate  = STATE_INITFADE;
          break;
        case STATE_INITFADE:
          initFade();

          fadestate          = STATE_FADE;
          fadeK          = 0.0;
          currentLinkMap = allLinkMap;
          linkColor      = Color.black;
          
          updateFade(currentLinkMap, nodeOldMap, nodeNewMap, fadeK);
          break;
        case STATE_FADE:
          fadeK += .1;
          if(fadeK < 1.0) {
            updateFade(currentLinkMap, nodeOldMap, nodeNewMap, fadeK);
          } else {
            fadestate = STATE_STOPFADE;
          }
          break;
        case STATE_STOPFADE:
          stopFade();
          fadestate = STATE_NONE;
          timer.stop();
          timer = null;
          currentNode = newNode;
          currentNode.refresh();
          bNeedLayout = true;
          break;
        }
        repaint();
      }
    };



  public void refresh() {
    repaint();
  }
  


  void initFade() {
    center = getCenter();
    
    nodeOldMap.clear();
    nodeNewMap.clear();
    
    newNode    = makeRootNode();
    newLinkMap = new TreeMap();
    
    layoutNode(newNode, newLinkMap);
    
    allLinkMap.clear();
    
    Map currNodePos = new HashMap();
    Map newNodePos  = new HashMap();
    
    int nAdds = 0;
    for(Iterator it = currentLinkMap.keySet().iterator(); it.hasNext(); ) {
      Link link = (Link)it.next();
      Node n1 = link.getFrom();
      Node n2 = link.getTo();
      currNodePos.put(n1, n1.getPoint());
      currNodePos.put(n2, n2.getPoint());
      if(!allLinkMap.containsKey(link)) {
        allLinkMap.put(link, link);
        nAdds++;
      }
    }
    
    nAdds = 0;
    for(Iterator it = newLinkMap.keySet().iterator(); it.hasNext(); ) {
      Link link = (Link)it.next();
      Node n1 = link.getFrom();
      Node n2 = link.getTo();
      newNodePos.put(n1, n1.getPoint());
      newNodePos.put(n2, n2.getPoint());
      if(!allLinkMap.containsKey(link)) {
        allLinkMap.put(link, link);
        nAdds++;
      }
    }
    
    
    for(Iterator it = allLinkMap.keySet().iterator(); it.hasNext(); ) {
      Link link = (Link)it.next();
      Node n1 = link.getFrom();
      Node n2 = link.getTo();
      
      if(currNodePos.containsKey(n1)) {
        nodeOldMap.put(n1, (Point2D)currNodePos.get(n1));
      } else {
        nodeOldMap.put(n1, center);
      }
      if(currNodePos.containsKey(n2)) {
        nodeOldMap.put(n2, (Point2D)currNodePos.get(n2));
      } else {
        nodeOldMap.put(n2, center);
      }
      
      if(newNodePos.containsKey(n1)) {
        nodeNewMap.put(n1, (Point2D)newNodePos.get(n1));
      } else {
        nodeNewMap.put(n1, center);
      }
      if(newNodePos.containsKey(n2)) {
        nodeNewMap.put(n2, (Point2D)newNodePos.get(n2));
      } else {
        nodeNewMap.put(n2, center);
      }
    }
    
    
  }
  
  String toString(Object o) {
    Point2D p = (Point2D)o;
    return p != null 
      ? ("[" + (int)p.getX() + ", " + (int)p.getY() + "]")
      : "null";
  }
  
  void updateFade(Map linkMap, Map oldPos, Map newPos, double k) {
    
    for(Iterator it = linkMap.keySet().iterator(); it.hasNext(); ) {
      Link link = (Link)it.next();
      Node node;
      Point2D p1;
      Point2D p2;
      
      node = link.getFrom();
      p1 = (Point2D)oldPos.get(node);
      p2 = (Point2D)newPos.get(node);
      node.setPoint(fade(k, p1, p2, center));
      
      node = link.getTo();            
      p1 = (Point2D)oldPos.get(node);
      p2 = (Point2D)newPos.get(node);
      node.setPoint(fade(k, p1, p2, center));
    }
  }
  
  void stopFade() {
    currentNode    = newNode;
    currentLinkMap = newLinkMap;
  }
  
  Point2D fade(double k, Point2D p1, Point2D p2, Point2D def) {
    if(p1 == null) {
      p1 = def;
    }
    if(p2 == null) {
      p2 = def;
    }
    return new Point2D.Double(p1.getX() + k * (p2.getX() - p1.getX()),
                              p1.getY() + k * (p2.getY() - p1.getY()));
  }
  
  
  boolean bNeedLayout = true;
  
  

  Dimension lastSize;

  public void paintComponent(Graphics _g) {
    Graphics2D  g = (Graphics2D)_g;
    Dimension size = getSize();
    
    if(lastSize == null || lastSize.width != size.width || lastSize.height != size.height) {
      bNeedLayout = true;
    }
    lastSize = size;

    if(bNeedLayout) {
      currentLinkMap.clear();
      layoutNode(currentNode, currentLinkMap);
      bNeedLayout = false;
    }
    
    Point2D center = getCenter();

    if(isOpaque()) {
      g.setColor(getBackground());
      g.fillRect(0, 0, size.width, size.height);

      
      float y0 = (float)(0.8 * center.getY());
      float y1 = (float)(size.height);
      GradientPaint painter = new GradientPaint(0.0f, y0,
                                                topColor,
                                                0.0f, y1,
                                                bottomColor);
      Paint oldP = g.getPaint();
      g.setPaint(painter);
      g.fill(new Rectangle2D.Double(0, y0, size.width, y1-y0));
      
      g.setPaint(oldP);

    }

    if(bPaintRootName) {
      if(currentNode != null) {
        paintMirror(g, new Color(20, 20, 100), currentNode.toString(), 10, 20, 1.5, false);
      }
    }

    paintLinkMap(g, currentLinkMap);

    if(currentNode != null && currentLinkMap != null && currentLinkMap.size() == 0) {
      currentNode.setPoint(center);
      paintNode(g, currentNode);
    }

    if(label != null) {
      paintMirror(g, new Color(20, 20, 100), label, 10, size.height - 20, 1.5, true);
    }
    
    if(hoverNode != null) {
      setToolTipText(hoverNode.toString());
    } else if(mouseObject != null) {
      setToolTipText(mouseObject.toString());
    } else {
      setToolTipText(null);
    }
  }
  
  String label = null;
  public void setLabel(String s) {
    label = s;
    repaint();
  }

  boolean bPaintRootName = false;
  public void setPaintRootName(boolean b) {
    bPaintRootName = true;
    repaint();
  }


  Map darkColors = new HashMap();
  Set paintedNodes = new HashSet();
  
  void paintLinkMap(Graphics2D g, Map linkMap) {
    paintedNodes.clear();
    Point2D center = getCenter();    
    if(linkMap == null) {
      return;
    }
    
    g.setColor(linkColor);
    Util.setAntialias(g, true);
    
    // paint links
    for(Iterator it = linkMap.keySet().iterator(); it.hasNext(); ) {
      Link link = (Link)it.next();
      Color c1 = fillColor;
      Color c2 = lineColor;
      if(link instanceof DefaultLink) {
        DefaultLink dl = (DefaultLink)link;
        c1 = dl.getColor();
        c2 = (Color)darkColors.get(c1);
        if(c2 == null) {
          c2 = Util.rgbInterpolate(c1, Color.black, .5);
          darkColors.put(c1, c2);
        }
      }
      if(link.getType() < 0) {
        c2 = null;
      }

      paintLink(g, link, c1, c2, true);
    }


    // paint link mirror image
    if(fadestate != STATE_FADE) {
      Composite oldComp = g.getComposite();
      AffineTransform oldTrans = g.getTransform();
      g.scale(1.0, -.5);
      g.translate(center.getY()*.5, -3.1*center.getY());
      g.shear(-0.5, 0);
      g.setComposite(alphaSome);
      int n = 0;
      int n2 = 0;
      Color c1 = Util.rgbInterpolate(bottomColor, Color.black, .1);
      for(Iterator it = linkMap.keySet().iterator(); it.hasNext(); ) {
        Link link = (Link)it.next();
        
        if(link.getType() >= 0) {
          paintLink(g, link, c1, null, false);
          n++;
        }
        n2++;
      }
      g.setTransform(oldTrans);
      g.setComposite(oldComp);
    }

    // paint mouse object
    if(mouseObject instanceof Link) {
      Link link = (Link)mouseObject;
      paintLink(g, link, fillColor2, lineColor, false);
      // paintLinkText(g, link, false);
      // paintLinkInfo(g, link);
    }

    // paint nodes

    for(Iterator it = linkMap.keySet().iterator(); it.hasNext(); ) {
      Link link = (Link)it.next();
      if(!paintedNodes.contains(link.getFrom())) {
        paintedNodes.add(link.getFrom());
        paintNode(g, link.getFrom());
      }
      if(!paintedNodes.contains(link.getTo())) {
        paintedNodes.add(link.getTo());
        paintNode(g, link.getTo());
      }
    }


    Util.setAntialias(g, false);
  }


  void paintNode(Graphics2D g, Node node) {
    
  }

  Color arcOutCol1 = new Color(100, 100, 255, 255);
  Color arcOutCol2 = new Color(200, 200, 255, 255);

  Color arcInCol1 = new Color(100, 100, 255, 255);
  Color arcInCol2 = new Color(200, 200, 255, 255);

  void paintNodeStart(Graphics2D g, Node node) {
    if(node.getDepth() > 0) {
      return;
    }

    paintNodeOutArc(g, node);
    paintNodeInArc(g, node);
  }

  int arcRadius = 40;

  boolean outArcActive = false;
  boolean inArcActive = false;

  void paintNodeOutArc(Graphics2D g, Node node) {
    double outMin    = node.getOutMin();
    double outMax    = node.getOutMax();
    double outSize   = outMax - outMin;
    
    int nTotal    = node.getOutLinks().size();
    
    if(outSize < nTotal) {
      
      Composite oldComp = g.getComposite();
      
      Composite alpha = outArcActive ? alphaHalf : alphaSome;        
      g.setComposite(alpha);
      
      double aStart = 180.0 * outMin / nTotal;
      double aSize  = Math.max(2, 180.0 * outSize / nTotal);
      
      int rx = arcRadius;
      int ry = (int)(rx * .9);
      
      
      Util.setAntialias(g, true);
      
      // background arc
      g.setColor(arcOutCol1);
      g.fillArc((int)(0 - rx), 
                (int)(0 - ry),
                rx * 2, ry * 2,
                0, 180);
      
      // g.setComposite(alpha);
      // size arc
      g.setColor(arcOutCol2);
      g.fillArc((int)(0 - rx), 
                (int)(0 - ry),
                rx * 2, ry * 2,
                (int)aStart, (int)aSize);
      g.setComposite(oldComp);
      Util.setAntialias(g, false);
    }
  }
  
  void paintNodeInArc(Graphics2D g, Node node) {
    double inMin    = node.getInMin();
    double inMax    = node.getInMax();
    double inSize   = inMax - inMin;
    
    int nTotal    = node.getInLinks().size();
    
    if(inSize < nTotal) {
      
      Composite oldComp = g.getComposite();
      
      Composite alpha = inArcActive ? alphaHalf : alphaSome;
      
      g.setComposite(alpha);
      
      double aStart = 180.0 * inMin / nTotal;
      double aSize  = Math.max(2, 180.0 * inSize / nTotal);
      
      int rx = 40;
      int ry = (int)(rx * .8);
      
      Util.setAntialias(g, true);
      
      // background arc
      g.setColor(arcInCol1);
      g.fillArc((int)(0 - rx), 
                (int)(0 - ry),
                rx * 2, ry * 2,
                180, 180);
      
      // g.setComposite(alphaHalf);
      // size arc
      g.setColor(arcInCol2);
      g.fillArc((int)(0 - rx), 
                (int)(0 - ry),
                rx * 2, ry * 2,
                180 + (int)aStart, (int)aSize);
      g.setComposite(oldComp);
      Util.setAntialias(g, false);
    }
  }


  void paintLinkText(Graphics2D g, Link link, boolean bClip) {
    Dimension size = getSize();
    Node n1 = link.getFrom();
    Node n2 = link.getTo();
    
    
    Point2D p1 = n1.getPoint();
    Point2D p2 = n2.getPoint();
    
    if(p1 != null && p2 != null) {
      Color c1 = Color.white;
      Color c2 = Color.black;
      
      double cx = (p1.getX() + p2.getX()) / 2;
      double cy = (p1.getY() + p2.getY()) / 2;
      
      // double mx = cx - mousePoint.getX();
      // double my = cy - mousePoint.getY();
      // double dist = Math.sqrt(mx * mx + my * my);
      
      double k = 1.0 / (1 + link.getDepth());        
      if(link.getDepth() < 10 && size.width > 50) {
        float textScale = Math.min(1f, (float)(k * 1.8));
        
        AffineTransform trans = g.getTransform();
        
        String s = link.toString();

        FontMetrics fm = g.getFontMetrics();
        
        Rectangle2D r = fm.getStringBounds(s, g);
        
        
        
        int pad = 2;
        
        double left = (cx-r.getWidth()/2) - pad;
        double top  = cy - (r.getHeight()/2) -pad;
        double w    = r.getWidth() + 2 * pad;
        double h    = r.getHeight() + 2 * pad;
        
        g.translate(left, top);
        g.scale(textScale, textScale);
        
        g.setColor(c1);
        g.drawString(s, 0, 0);
        g.setColor(c2);
        g.drawString(s, 1, 1);

        // paintString(g, link.toString(), 0, 0, bClip);
        
        g.setTransform(trans);
        
        /*
        g.translate((int)p2.getX(), (int)p2.getY());
        g.scale(textScale, textScale);
        
        paintString(g, n2.toString(), 0, 0, bClip);
        */

        g.setTransform(trans);
      }
    }
  }

  static Color txtColor = new Color(100, 100, 100);

  static void paintMirror(Graphics2D g, Color c, String s, int x, int y, double k, boolean bMirror) {
    Composite oldComp = g.getComposite();
    AffineTransform oldTrans = g.getTransform();


    Util.setAntialias(g, true);
    g.translate(x, y);
    g.scale(k, k);
    
    g.setColor(c);
    g.drawString(s, 0, 0);

    if(bMirror) {
      g.setComposite(alphaSome);
      
      g.setTransform(oldTrans);
      g.translate(x, y);
      g.scale(k, -k*1.2);
      g.shear(-0.25, 0);
      
      g.setColor(c);
      g.drawString(s, 0, 0);
    }




    g.setTransform(oldTrans);
    g.setComposite(oldComp);
    Util.setAntialias(g, false);
  }

  static void paintString(Graphics2D g, String s, int cx, int cy, boolean bClip) {
    if(s.trim().length() == 0) {
      return;
    }

    if(bClip) {
      int max = 12;
      if(s.length() > 12) {
        int ix = s.lastIndexOf(".");
        if(ix != -1 && ix > 5 && s.length() - ix < 20) {
          s = s.substring(0, 5) + "..." + s.substring(ix+1);
        } else {
          s = s.substring(0, max/2) + "..." + s.substring(s.length() - max/2);
        }
      }
    }

    FontMetrics fm = g.getFontMetrics();

    Rectangle2D r = fm.getStringBounds(s, g);



    int pad = 2;

    int left = (int)(cx-r.getWidth()/2) - pad;
    int top  = cy - (int)(r.getHeight()/2) -pad;
    int w    = (int)r.getWidth() + 2 * pad;
    int h    = (int)r.getHeight() + 2 * pad;
    
    g.setColor(Color.white);
    g.fillRect(left+1, top+1, w-1, h-1);

    g.setColor(Color.gray);

    g.drawLine(left+1, top,   left+w-2, top);       // top
    g.drawLine(left+1, top+h, left+w-2, top+h);     // bottom
    g.drawLine(left,   top+1, left,     top+h-2);   // left
    g.drawLine(left+w, top+1, left+w,   top+h-2);   // right
    
    g.setColor(txtColor);
    
    g.drawString(s, left + pad, top + pad + fm.getMaxAscent());

  }

  void paintLink(Graphics2D g, Link link, Color fillColor, Color lineColor, boolean bFind) {
    Node n1 = link.getFrom();
    Node n2 = link.getTo();
    
    Point2D p1 = n1.getPoint();
    Point2D p2 = n2.getPoint();
    
    if(false && n2.equals(currentNode)) {
      drawLine(g, 
               link, 
               (int)p1.getX(),
               (int)p1.getY(),
               (int)p2.getX(),
               (int)p2.getY(),
               fillColor,
               fadestate == STATE_FADE ? null : lineColor,
               bFind);
    } else {
      if(p1 != null && p2 != null) {
        Stroke oldStroke = g.getStroke();
        
        double k = 1.0 / (1 + link.getDepth());
        g.setStroke(new BasicStroke((float)k));
        drawArrow(g,  
                  link,
                  (int)p1.getX(),
                  (int)p1.getY(),
                  (int)p2.getX(),
                  (int)p2.getY(),
                  fillColor,
                  lineColor,
                  bFind);
        
        
        g.setStroke(oldStroke);
      }
    }
  }


  public Point2D getCenter() {
    Dimension size = getSize();
    
    double cx = size.width / 2;
    double cy = .68 * size.height;
    return new Point2D.Double(cx, cy);
  }

  public void layoutNode(Node node,
                         Map linkMap) {
    Dimension size = getSize();

    double r  = Math.min(size.width, size.height) * .37;

    if(r < 10) {
      return;
    }
    long t0 = System.currentTimeMillis();
    layoutNode(node, linkMap, r, getCenter(), 10, 0.0);
    long t1 = System.currentTimeMillis();
  }

  int maxDepth = 10;
  public void setMaxDepth(int d) {
    maxDepth = d;
    repaint();
  }

  int maxLinks = 20;

  public void layoutNode(Node   node,
                         Map    linkMap,
                         double r,
                         Point2D center,
                         int    detail,
                         double a0
                         ) {
    try {
      double rx = r / 2 * .8;
      double ry = rx;

      if(detail > maxDepth) {
        Collection outLinks = node.getOutLinks();

        if(node.getOutMax() == 0) {
          node.setOutMax(Math.min(maxLinks, outLinks.size()));
        }
        
        double nMax = node.getOutMax() - node.getOutMin();
        
        int range = outLinks.size();
        if(nMax < range) {
          range = (int)(nMax + 1);
        }

        int _n = 0;
        int n = 0;
        
        double frac = node.getOutMin() - Math.floor(node.getOutMin());
        a0 = -Math.PI * frac / range;

        for(Iterator it = outLinks.iterator(); it.hasNext(); ) {
          Link link = (Link)it.next();
          if(_n >= Math.floor(node.getOutMin()) && 
             _n <= Math.floor(node.getOutMax())) {
            double a = a0 + Math.PI + Math.PI * n / range + Math.PI / nMax/2;
            
            double x = center.getX() + r * Math.cos(a);
            double y = center.getY() + r * Math.sin(a);
            
            link.getFrom().setPoint(center);
            link.getTo().setPoint(new Point2D.Double(x, y));
            
            linkMap.put(link, link);
            
            layoutNode(link.getTo(),
                       linkMap, 
                       r * .4,
                       link.getTo().getPoint(),
                       detail - 1,
                       0);
            n++;
          }
          _n++;
        }
      }


      if(detail > 9) {
        Collection inLinks = node.getInLinks();

        if(node.getInMax() == 0) {
          node.setInMax(Math.min(maxLinks, inLinks.size()));
        }
        
        double nMax = node.getInMax() - node.getInMin();
        
        int range = inLinks.size();
        if(nMax < range) {
          range = (int)(nMax + 1);
        }

        int _n = 0;
        int n = 0;

        int nImport = inLinks.size();
        double inrx = r*1;
        double inry = r*.60;

        double frac = node.getInMin() - Math.floor(node.getInMin());
        a0 = -Math.PI * frac / range;

        for(Iterator it = inLinks.iterator(); it.hasNext(); ) {
          Link link = (Link)it.next();
          if(_n >= Math.floor(node.getInMin()) && 
             _n <= Math.floor(node.getInMax())) {
            double a = a0 + 2*Math.PI + Math.PI * n / range + Math.PI / range/2;
            
            double x = center.getX() + inrx * Math.cos(a);
            double y = center.getY() + inry * Math.sin(a);
            
            DefaultNode n1 = (DefaultNode)link.getFrom();
            Node n2 = link.getTo();
            
            n1.setSize(2 + 5 * (1 + Math.sin(a)));
            n1.setPoint(new Point2D.Double(x, y));
            n2.setPoint(center);
            
            
            linkMap.put(link, link);
            n++;
          }
          _n++;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  

  static AlphaComposite alphaHalf 
    = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .7f);

  static AlphaComposite alphaSome
    = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .2f);

  public static String[] split(String s, String sep) {
    StringTokenizer st = new StringTokenizer(s, sep);
    int ntok = st.countTokens();
    String[] res = new String[ntok];
    for (int i = 0; i < ntok; i++) {
      res[i] = st.nextToken();
    }
    return res;
  }
  
  Color boxCol = new Color(200, 200, 200);

  void paintBox(Graphics2D g, 
                String s, 
                int x, 
                int y) {
    paintBox(g, s, boxCol, Color.black, x, y + 3, 1, 100, 10, false, false);
  }

  public static final Stroke SOLID_STROKE = new BasicStroke(1f);

  double arrowK = 1.0;
  double arrowDK = 0.01;

  public void drawArrow(Graphics2D g, 
                        Link link,
                        int x1, int y1, 
                        int x2, int y2,
                        Color fillColor,
                        Color lineColor) {
    drawArrow(g, link, x1, y1, x2, y2, fillColor, lineColor, true);
  }

  public static double dist(double x1, double y1, 
                            double x2, double y2,
                            double mx, double my) {
    double _x1 = Math.min(x1, x2);
    double _x2 = Math.max(x1, x2);
    double _y1 = Math.min(y1, y2);
    double _y2 = Math.max(y1, y2);
    if(mx >= _x1 && mx <= _x2 && 
       my >= _y1 && my <= _y2) {
      return Line2D.ptLineDist(x1, y1, x2, y2, mx, my);
    }
    return Double.MAX_VALUE;
  }


  Map strokeMap = new HashMap();
  Stroke getStroke(int depth) {
    Integer i = new Integer(depth);
    Stroke s = (Stroke)strokeMap.get(i);
    if(s == null) {
      s = new BasicStroke((float)(5.0 / (depth + 1)));
      strokeMap.put(i, s);
    }
    return s;
  }

  Stroke fatStroke = new BasicStroke((float)5);

  public void drawArrow(Graphics2D g, 
                        Link link,
                        int x1, int y1, 
                        int x2, int y2,
                        Color fillColor,
                        Color lineColor,
                        boolean bFind) {

    if((link.getDetail() >= 25 || link.getDepth() > 3)) {
      double dist = 10;
      if(bFind && mousePoint != null) {
        dist = dist(x1, y1, x2, y2, 
                           mousePoint.getX(),  mousePoint.getY());
        if(dist <= 2) {
          mouseObject = link;
        }
      }
      g.setColor(fillColor);
      g.drawLine(x1, y1, x2, y2);
      return;
    }
    Stroke oldStroke = g.getStroke();
    Util.setAntialias(g, true);
    try {
      int dx = x2 - x1;
      int dy = y2 - y1;
      double len = Math.sqrt(dx * dx + dy * dy);

      double aDir=Math.atan2(dy, dx);
      AffineTransform trans = g.getTransform();

      int type = link.getType();
      
      g.translate(x1, y1);
      g.rotate(aDir);

      GeneralPath path = new GeneralPath();
      
      double k = 1.0;

      if(Math.abs(type) == 2) {
        k = 0;
      }

      Shape s1 = new CubicCurve2D.Double(0, len/20, 
                                         len/4, k* len/4,
                                         3*len/4, k * -len/5,
                                         len, 0);

      path.append(s1, false);

      Shape s2 = new CubicCurve2D.Double(len, 0, 
                                         3*len/4, k * -len/4,
                                         len/4, k * len/6,
                                         0, -len/20);

      path.append(s2, true);
      path.closePath();

      if(bFind) {
        try {
          if(path.contains(g.getTransform().inverseTransform(mousePoint, null))) {
            mouseObject =  link;
          }
        } catch (Exception e) {
          throw new RuntimeException("Failed to transform", e);
        }
      }
      g.setColor(fillColor);
      g.fill(path);

      if(lineColor != null) {
        if(len > 20) {
          
          g.setColor(Util.rgbInterpolate(fillColor, Color.red, .3));
          g.setStroke(getStroke(link.getDepth()));
          g.draw(s2);
        }
      }

      
      g.setStroke(oldStroke);
      if(lineColor != null) {
        g.setColor(lineColor);
        g.draw(path);
      }



      // g.drawLine(0, 0, (int)len, 0);
      
      g.setTransform(trans);

    } finally {
      g.setStroke(oldStroke);
    }
  }

  public void drawLine(Graphics2D g, 
                       Link link,
                       int x1, int y1, 
                       int x2, int y2,
                       Color fillColor,
                       Color lineColor,
                       boolean bFind) {
    
    Stroke oldStroke = g.getStroke();
    try {
      int dx = x2 - x1;
      int dy = y2 - y1;
      double len = Math.sqrt(dx * dx + dy * dy);

      double aDir=Math.atan2(dy, dx);
      AffineTransform trans = g.getTransform();

      g.translate(x1, y1);
      g.rotate(aDir);

      GeneralPath path = new GeneralPath();
      
      double k = 1.0;

      Polygon p1 = new Polygon();
      
      Node n1 = link.getFrom();
      
      int h = (int)n1.getSize();
      p1.addPoint(0, h);
      p1.addPoint((int)len, 0);
      p1.addPoint(0, -h);
      path.append(p1, false);

      path.closePath();

      if(bFind) {
        try {
          if(path.contains(g.getTransform().inverseTransform(mousePoint, null))) {
            mouseObject =  link;
          }
        } catch (Exception e) {
          throw new RuntimeException("Failed to transform", e);
        }
      }
      g.setColor(fillColor);
      g.fill(path);

      if(lineColor != null) {
        g.setColor(lineColor);
        g.draw(path);
      }
      // g.drawLine(0, 0, (int)len, 0);
      
      g.setTransform(trans);

    } finally {
      g.setStroke(oldStroke);
    }
  }

  void paintBox(Graphics2D g, 
                String   s,                 
                Color    bgCol, 
                Color    fgCol,
                int      x, 
                int      y,
                double   size,
                int minWidth, int minHeight,
                boolean bCenterX,
                boolean bCenterY) {
    Object oldComp = g.getComposite();
    g.setComposite((AlphaComposite)alphaHalf);
    
    String[] lines = split(s, "\n");
    
    int maxCols = 0;
    for(int i = 0; i <lines.length; i++) {
      if(lines[i].length() > maxCols) {
        maxCols = lines[i].length();
      }
    }

    Font font = g.getFont();

    g.setColor(bgCol);
    
    int w = Math.max(minWidth, font.getSize() * maxCols / 2 + 30);
    int h = Math.max(minHeight, (font.getSize() + 3) * lines.length + 10);

    if(bCenterX) {
      x -= w/2;
    }
    if(bCenterY) {
      y -= h/2;
    }

    g.fill3DRect(x, y, 
                 w, h,                 
                 true);


    g.setColor(fgCol);

    x += 10;
    y += font.getSize() + 5;

    int x2 = x + font.getSize() * 8;

    g.setComposite((AlphaComposite)oldComp);

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
}

