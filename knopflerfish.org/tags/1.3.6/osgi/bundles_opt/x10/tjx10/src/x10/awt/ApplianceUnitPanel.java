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


/** ApplianceUnitPanel is an awt-based panel that can be used to control an
* X10 appliance module.
*
* <br><br><img src="doc-files/ApplianceUnit.gif">
*
*
* @author Wade Wassenberg
*
* @version 1.0
*/

public class ApplianceUnitPanel extends UnitPanel implements ActionListener
{
    
    
    private Label title;
    private Button onButton = new Button("On");
    private Button offButton = new Button("Off");
    private boolean isOn;
    private Command onCommand;
    private Command offCommand;
    
    
    /** ApplianceUnitPanel constructs a new awt-based panel for controling an
    * X10 Appliance module.
    *
    * @param controller the controller to attatch to
    * @param address the device address associated with this panel
    * @exception IllegalArgumentException thrown if the address
    * specified does not follow X10 address rules.
    *
    */
    
    public ApplianceUnitPanel(Controller controller, String address) throws IllegalArgumentException
    {
        this(controller, address, address);
    }
    
    
    /** ApplianceUnitPanel constructs a new awt-based panel for controling an
    * X10 Appliance module.
    *
    * @param controller the controller to attatch to
    * @param address the device address associated with this panel
    * @param title the title to be displayed at the top of the panel
    * @exception IllegalArgumentException thrown if the address
    * specified does not follow X10 address rules.
    *
    */
    
    public ApplianceUnitPanel(Controller controller, String address, String title) throws IllegalArgumentException
    {
        super(controller, address);
        this.title = new Label(title);
        setLayout(new BorderLayout());
        Panel north = new Panel();
        north.setLayout(new FlowLayout(FlowLayout.CENTER));
        north.add(this.title);
        add("North", north);
        Panel flip = new Panel();
        flip.setLayout(new GridLayout(2,1));
        flip.add(onButton);
        flip.add(offButton);
        add("Center", flip);
        add("South", new Panel());
        add("East", new Panel());
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
    }
    
    
    /* dim
    *
    * @param percent
    *
    */
    
    public void dim(int percent)
    {
    }
    
    
    /* brighten
    *
    * @param percent
    *
    */
    
    public void brighten(int percent)
    {
    }
    
    
    /* allLightsOn
    *
    * @param event
    *
    */
    
    public void allLightsOn(UnitEvent event)
    //Appliances do not respond to AllLightsOn
    {
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
}