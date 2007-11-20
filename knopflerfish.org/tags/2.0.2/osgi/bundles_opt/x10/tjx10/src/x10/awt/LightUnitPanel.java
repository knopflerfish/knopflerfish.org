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
import x10.*;


/** LightUnitPanel is an awt-based panel that can be used to control a
* dimmable X10 light module.
*
* <br><br><img src="doc-files/LightUnit.gif">
*
* @author Wade Wassenberg
*
* @version 1.0
*/

public class LightUnitPanel extends UnitPanel implements ActionListener, AdjustmentListener
{
    
    private Label title;
    
    private Button onButton = new Button("On");
    private Button offButton = new Button("Off");
    private boolean isOn;
    private Command onCommand;
    private Command offCommand;
    private RangeBar rangeBar;
    private Label dimValue;
    private int lastValue;
    
    
    /** LightUnitPanel constructs a new awt-based panel for controling a
    * dimmable X10 light module.
    *
    * @param controller the controller to attatch to
    * @param address the device address associated with this panel
    * @exception IllegalArgumentException thrown if the address
    * specified does not follow X10 address rules.
    *
    */
    
    public LightUnitPanel(Controller controller, String address) throws IllegalArgumentException
    {
        this(controller, address, address);
    }
    
    
    /** LightUnitPanel constructs a new awt-based panel for controling a
    * dimmable X10 light module.
    *
    * @param controller the controller to attatch to
    * @param address the device address associated with this panel
    * @param title the title to be displayed at the top of the panel
    * @exception IllegalArgumentException thrown if the address
    * specified does not follow X10 address rules.
    *
    */
    
    public LightUnitPanel(Controller controller, String address, String title) throws IllegalArgumentException
    {
        super(controller, address);
        this.title = new Label(title);
        dimValue = new Label("0");
        setLayout(new BorderLayout());
        rangeBar = new RangeBar(0, 100, 0, 10);
        rangeBar.setFillColor(Color.green);
        rangeBar.setVisibleAmount(10);
        rangeBar.addAdjustmentListener(this);
        add("East", rangeBar);
        Panel north = new Panel();
        north.setLayout(new FlowLayout(FlowLayout.CENTER));
        north.add(this.title);
        add("North", north);
        Panel flip = new Panel();
        flip.setLayout(new GridLayout(2,1));
        flip.add(onButton);
        flip.add(offButton);
        add("Center", flip);
        Panel dimPanel = new Panel();
        dimPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        dimPanel.add(dimValue);
        add("South", dimPanel);
        add("West", new Panel());
        onButton.addActionListener(this);
        offButton.addActionListener(this);
        off();
        onCommand = new Command(address, Command.ON);
        offCommand = new Command(address, Command.OFF);
    }
    
    
    /* on
    *
    *
    */
    
    public void on()
    {
        isOn = true;
        onButton.setBackground(Color.yellow);
        offButton.setBackground(Color.lightGray);
        if(getValue() == 0)
        {
            dimValue.setText("100");
            rangeBar.setValue(0);
            lastValue = 0;
        }
    }
    
    
    /* off
    *
    *
    */
    
    public void off()
    {
        isOn = false;
        onButton.setBackground(Color.lightGray);
        offButton.setBackground(Color.yellow);
        dimValue.setText("0");
        rangeBar.setValue(100);
        lastValue = 100;
    }
    
    
    /* dim
    *
    * @param percent
    *
    */
    
    public void dim(int percent)
    {
        int newValue = Integer.parseInt(dimValue.getText()) - percent;
        if(newValue < 0)
        {
            newValue = 0;
        }
        dimValue.setText("" + newValue);
        rangeBar.setValue(100 - newValue);
        lastValue = 100 - newValue;
    }
    
    
    /* brighten
    *
    * @param percent
    *
    */
    
    public void brighten(int percent)
    {
        int newValue = Integer.parseInt(dimValue.getText()) + percent;
        if(newValue > 100)
        {
            newValue = 100;
        }
        dimValue.setText("" + newValue);
        rangeBar.setValue(100 - newValue);
        lastValue = 100 - newValue;
    }
    
    
    /** actionPerformed handles clicks on the On and Off buttons
    *
    * @param event the ActionEvent describing the source of the action.
    *
    */
    
    public void actionPerformed(ActionEvent event)
    {
        Object source = event.getSource();
        if(source == onButton)
        {
            addCommand(onCommand);
            on();
        }
        else if(source == offButton)
        {
            addCommand(offCommand);
            off();
        }
    }
    
    
    /** adjustmentValueChanged handles adjustments to the range bar
    * for dimming and brightening
    *
    * @param event the AdjustmentEvent associated with the dim or brighten
    *
    */
    
    public void adjustmentValueChanged(AdjustmentEvent event)
    {
        int type = event.getAdjustmentType();
        if(type == RangeBar.HOLDING)
        {
            dimValue.setText("" + (100 - event.getValue()));
        }
        else if(type == RangeBar.RELEASED)
        {
            int newValue = 100 - event.getValue();
            int oldValue = 100 - lastValue;
            if(newValue > oldValue)
            {
                newValue = newValue - oldValue;
                addCommand(new Command(address, Command.BRIGHT, newValue));
                dimValue.setText("" + (100 - event.getValue()));
                lastValue = event.getValue();
            }
            else if(oldValue > newValue)
            {
                newValue = oldValue - newValue;
                addCommand(new Command(address, Command.DIM, newValue));
                dimValue.setText("" + (100 - event.getValue()));
                lastValue = event.getValue();
            }
        }
    }
    
    
    /** getTitle returns the title associated with this panel
    *
    * @return String the title assigned to this panel
    *
    */
    
    public String getTitle()
    {
        return(title.getText());
    }
    
    
    /** setTitle sets the title that appears at the top of this panel
    *
    * @param title the title to appear at the top of this panel
    *
    */
    
    public void setTitle(String title)
    {
        this.title.setText(title);
    }
    
    
    /** getState returns the current state of the associated module.
    *
    * @return boolean true if the associated module is On.. false otherwise.
    *
    */
    
    public boolean getState()
    {
        return(isOn);
    }
    
    
    /** getValue returns the current brightness percentage
    *
    * @return int the current brightness percentage of the associated module.
    *
    */
    
    public int getValue()
    {
        return(100 - lastValue);
    }
}