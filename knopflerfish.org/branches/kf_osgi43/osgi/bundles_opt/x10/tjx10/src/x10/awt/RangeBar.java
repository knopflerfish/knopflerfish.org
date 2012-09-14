/*
* Copyright 2002-2003, Wade Wassenberg  All rights reserved.
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*
*/

package x10.awt;

import java.awt.*;
import java.awt.event.*;


/** RangeBar
*
*
* @author Wade Wassenberg
*
* @version 1.0
*/

class RangeBar extends Component implements MouseListener, MouseMotionListener, Runnable, Adjustable
{
    
    
    /** HOLDING int
    *
    * @serial
    */
    
    public static final int HOLDING = 99;
    
    
    /** RELEASED int
    *
    * @serial
    */
    
    public static final int RELEASED = 100;
    
    
    /** minimum int
    *
    * @serial
    */
    
    
    private int minimum;
    
    
    /** maximum int
    *
    * @serial
    */
    
    private int maximum;
    
    
    /** value int
    *
    * @serial
    */
    
    private int value;
    
    
    /** blockValue int
    *
    * @serial
    */
    
    private int blockValue;
    
    
    /** pressed boolean
    *
    * @serial
    */
    
    private boolean pressed;
    
    
    /** held boolean
    *
    * @serial
    */
    
    private boolean held;
    
    
    /** relativeHeldPoint int
    *
    * @serial
    */
    
    private int relativeHeldPoint;
    
    
    /** lastY int
    *
    * @serial
    */
    
    
    private int lastY;
    
    
    /** lastValue int
    *
    * @serial
    */
    
    private int lastValue;
    
    
    /** adjustmentListener AdjustmentListener
    *
    * @serial
    */
    
    private AdjustmentListener adjustmentListener = null;
    
    
    /** visibleAmount int
    *
    * @serial
    */
    
    private int visibleAmount;
    
    
    /** fillColor Color
    *
    * @serial
    */
    
    
    private Color fillColor;
    
    
    /** RangeBar
    *
    * @param minimum
    * @param maximum
    * @param value
    * @param blockValue
    *
    */
    
    public RangeBar(int minimum, int maximum, int value, int blockValue)
    {
        this.minimum = minimum;
        this.maximum = maximum;
        this.value = value;
        this.blockValue = blockValue;
        visibleAmount = 20;
        setVisibleAmount(blockValue);
        pressed = false;
        held = false;
        relativeHeldPoint = 0;
        addMouseListener(this);
        addMouseMotionListener(this);
        
        fillColor = Color.lightGray;
    }
    
    
    /** setFillColor
    *
    * @param color
    *
    */
    
    public void setFillColor(Color color)
    {
        fillColor = color;
    }
    
    
    /** getOrientation
    *
    * @return int
    *
    */
    public int getOrientation()
    {
        return(Adjustable.VERTICAL);
    }
    
    
    /** setMinimum
    *
    * @param min
    *
    */
    
    public void setMinimum(int min)
    {
        minimum = min;
        if(value < min)
        {
            value = min;
        }
        Graphics g = getGraphics();
        if(g != null)
        {
            paint(g);
        }
    }
    
    
    /** setMaximum
    *
    * @param max
    *
    */
    
    public void setMaximum(int max)
    {
        maximum = max;
        if(value > max)
        {
            value = max;
        }
        Graphics g = getGraphics();
        if(g != null)
        {
            paint(g);
        }
    }
    
    
    /** getMinimum
    *
    * @return int
    *
    */
    
    public int getMinimum()
    {
        return(minimum);
    }
    
    
    /** getMaximum
    *
    * @return int
    *
    */
    
    public int getMaximum()
    {
        return(maximum);
    }
    
    
    /** getValue
    *
    * @return int
    *
    */
    
    public int getValue()
    {
        return(value);
    }
    
    
    /** setValue
    *
    * @param val
    *
    */
    
    public void setValue(int val)
    {
        value = val;
        if(value > maximum)
        {
            value = maximum;
        }
        else if(value < minimum)
        {
            value = minimum;
        }
        Graphics g = getGraphics();
        if(g != null)
        {
            paint(g);
        }
    }
    
    
    /** setUnitIncrement
    *
    * @param incr
    *
    */
    
    public void setUnitIncrement(int incr)
    {
    }
    
    
    /** getUnitIncrement
    *
    * @return int
    *
    */
    
    public int getUnitIncrement()
    {
        return(1);
    }
    
    
    /** setBlockIncrement
    *
    * @param block
    *
    */
    
    public void setBlockIncrement(int block)
    {
        blockValue = block;
    }
    
    
    /** getBlockIncrement
    *
    * @return int
    *
    */
    
    public int getBlockIncrement()
    {
        return(blockValue);
    }
    
    
    /** setVisibleAmount
    *
    * @param amount
    *
    */
    
    public void setVisibleAmount(int amount)
    {
        if(amount > 0)
        {
            visibleAmount = amount;
            if(amount > maximum)
            {
                visibleAmount = maximum;
            }
        }
    }
    
    
    /** getVisibleAmount
    *
    * @return int
    *
    */
    
    public int getVisibleAmount()
    {
        return(visibleAmount);
    }
    
    
    /** addAdjustmentListener
    *
    * @param listener
    *
    */
    
    public void addAdjustmentListener(AdjustmentListener listener)
    {
        adjustmentListener = AWTEventMulticaster.add(adjustmentListener, listener);
    }
    
    
    /** removeAdjustmentListener
    *
    * @param listener
    *
    */
    
    public void removeAdjustmentListener(AdjustmentListener listener)
    {
        adjustmentListener = AWTEventMulticaster.remove(adjustmentListener, listener);
    }
    
    
    /** processAdjustmentEvent
    *
    * @param event
    *
    */
    
    public void processAdjustmentEvent(AdjustmentEvent event)
    {
        if(adjustmentListener != null)
        {
            adjustmentListener.adjustmentValueChanged(event);
        }
    }
    
    
    /** paintAll
    *
    * @param g
    *
    */
    
    public void paintAll(Graphics g)
    {
        paint(g);
    }
    
    
    /** paint
    *
    * @param g
    *
    */
    
    public void paint(Graphics g)
    {
        Dimension size = getSize();
        double height = size.getHeight();
        double width = size.getWidth();
        g.setColor(Color.gray);
        g.fillRect(0, 0, (int) width, (int) height);
        drawSlide(g, Color.lightGray, Color.white, Color.black, fillColor);
    }
    
    
    /** update
    *
    * @param g
    *
    */
    
    public void update(Graphics g)
    {
        paint(g);
    }
    
    
    /** repaint
    *
    *
    */
    
    public void repaint()
    {
        Graphics g = getGraphics();
        if(g != null)
        {
            paint(g);
        }
    }
    
    
    /** getSlideHeight
    *
    * @return double
    *
    */
    
    private double getSlideHeight()
    {
        double height = getSize().getHeight();
        return((visibleAmount / (0.0 + maximum - minimum)) * height);
    }
    
    
    /** getSlideYMin
    *
    * @return double
    *
    */
    
    private double getSlideYMin()
    {
        Dimension size = getSize();
        return(((value + 0.0) / (maximum - minimum + 0.0)) * (size.getHeight() - getSlideHeight()));
    }
    
    
    /** drawSlide
    *
    * @param g
    * @param back
    * @param top
    * @param bottom
    * @param fillColor
    *
    */
    
    private void drawSlide(Graphics g, Color back, Color top, Color bottom, Color fillColor)
    {
        Dimension size = getSize();
        double height = size.getHeight();
        double width = size.getWidth();
        double slideYMin = getSlideYMin();
        g.setColor(back);
        g.fillRect(0, (int) slideYMin, (int) width, (int) getSlideHeight());
        g.setColor(top);
        g.fillRect(0, (int) slideYMin, 2, (int) getSlideHeight()); //left
        g.fillRect(0, (int) slideYMin, (int) width, 2); //top
        g.setColor(bottom);
        g.fillRect(0, (int) (slideYMin + getSlideHeight() - 2), (int) width, 2); //bottom
        g.fillRect((int) (width - 2), (int) slideYMin, 2, (int) getSlideHeight()); //right
        g.setColor(fillColor);
        int fill = (int) (getSlideYMin() + getSlideHeight() + 1);
        g.fillRect(0, fill, (int) width, ((int) (height - fill)));
    }
    
    
    /** getMaximumSize
    *
    * @return Dimension
    *
    */
    
    public Dimension getMaximumSize()
    {
        return(getMinimumSize());
    }
    
    
    /** getMinimumSize
    *
    * @return Dimension
    *
    */
    
    public Dimension getMinimumSize()
    {
        return(new Dimension(20, 50));
    }
    
    
    /** getPreferredSize
    *
    * @return Dimension
    *
    */
    
    public Dimension getPreferredSize()
    {
        //        return(new Dimension(20, ((int) ((maximum - minimum) * 11.0 / 10.0))));
        return(getMinimumSize());
    }
    
    
    /** run
    *
    *
    */
    
    public void run()
    {
        do
        {
            try
            {
                int y = lastY;
                int sy = (int) getSlideYMin();
                if(y < sy)
                {
                    Graphics g = getGraphics();
                    if(g != null)
                    {
                        drawSlide(g, Color.gray, Color.gray, Color.gray, Color.gray);
                    }
                    value = value - blockValue;
                    if(value < minimum)
                    {
                        value = minimum;
                    }
                    if(g != null)
                    {
                        drawSlide(g, Color.lightGray, Color.white, Color.black, fillColor);
                    }
                }
                else if(y > (sy + getSlideHeight()))
                {
                    Graphics g = getGraphics();
                    if(g != null)
                    {
                        drawSlide(g, Color.gray, Color.gray, Color.gray, Color.gray);
                    }
                    value = value + blockValue;
                    if(value > maximum)
                    {
                        value = maximum;
                    }
                    if(g != null)
                    {
                        drawSlide(g, Color.lightGray, Color.white, Color.black, fillColor);
                    }
                }
                processAdjustmentEvent(new AdjustmentEvent(this, AdjustmentEvent.ADJUSTMENT_FIRST, HOLDING, value));
                Thread.sleep(200);
            }
            catch(InterruptedException ie)
            {
            }
        }while(pressed);
    }
    
    
    /** mousePressed
    *
    * @param event
    *
    */
    
    public void mousePressed(MouseEvent event)
    {
        lastY = event.getY();
        int y = lastY;
        int sy = (int) getSlideYMin();
        if((y >= sy) && (y < (sy + getSlideHeight())))
        {
            pressed = true;
            lastValue = value;
            held = true;
            relativeHeldPoint = y - sy;
            Graphics g = getGraphics();
            if(g != null)
            {
                drawSlide(g, Color.lightGray, Color.black, Color.white, fillColor);
            }
        }
        else
        {
            pressed = true;
            lastValue = value;
            new Thread(this).start();
        }
    }
    
    
    /** mouseReleased
    *
    * @param event
    *
    */
    
    public void mouseReleased(MouseEvent event)
    {
        held = false;
        if(pressed)
        {
            pressed = false;
            Graphics g = getGraphics();
            if(g != null)
            {
                drawSlide(g, Color.lightGray, Color.white, Color.black, fillColor);
            }
            if(lastValue != value)
            {
                processAdjustmentEvent(new AdjustmentEvent(this, AdjustmentEvent.ADJUSTMENT_FIRST, RELEASED, value));
            }
        }
    }
    
    
    /** mouseClicked
    *
    * @param event
    *
    */
    
    public void mouseClicked(MouseEvent event)
    {
    }
    
    
    /** mouseEntered
    *
    * @param event
    *
    */
    
    public void mouseEntered(MouseEvent event)
    {
    }
    
    
    /** mouseExited
    *
    * @param event
    *
    */
    
    public void mouseExited(MouseEvent event)
    {
    }
    
    
    /** mouseDragged
    *
    * @param event
    *
    */
    
    public void mouseDragged(MouseEvent event)
    {
        if(held) //dragging bubble
        {
            Graphics g = getGraphics();
            if(g != null)
            {
                drawSlide(g, Color.gray, Color.gray, Color.gray, Color.gray);
            }
            int y = event.getY();
            double height = getSize().getHeight();
            if((y - relativeHeldPoint) >= (height - getSlideHeight()))
            {
                value = maximum;
                if(y > height)
                {
                    relativeHeldPoint = (int) getSlideHeight();
                }
                else
                {
                    relativeHeldPoint = (int) (y - getSlideYMin());
                }
            }
            else if((y - relativeHeldPoint) <= 0)
            {
                value = minimum;
                if(y < 0)
                {
                    relativeHeldPoint = 0;
                }
                else
                {
                    relativeHeldPoint = y;
                }
            }
            else
            {
                double newTop = y - relativeHeldPoint;
                value = (int) ((newTop / (height - getSlideHeight())) * (maximum - minimum));
            }
            if(g != null)
            {
                drawSlide(g, Color.lightGray, Color.white, Color.black, fillColor);
            }
            processAdjustmentEvent(new AdjustmentEvent(this, AdjustmentEvent.ADJUSTMENT_FIRST, HOLDING, value));
        }
        else // outside bubble
        {
            lastY = event.getY();
        }
    }
    
    
    /** mouseMoved
    *
    * @param event
    *
    */
    
    public void mouseMoved(MouseEvent event)
    {
    }
    
    
    /** main
    *
    * @param args
    *
    */
    
    public static void main(String[] args)
    {
        Frame f = new Frame();
        RangeBar rb = new RangeBar(0, 100, 30, 10);
        f.add(new Panel().add(rb));
        rb.setFillColor(Color.yellow);
        rb.setVisibleAmount(3);
        f.pack();
        f.setVisible(true);
    }
}