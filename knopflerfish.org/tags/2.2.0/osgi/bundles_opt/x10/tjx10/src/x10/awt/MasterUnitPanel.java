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


/** MasterUnitPanel is an awt-based panel that can be used to control all
* connected X10 modules.
*
* <br><br><img src="doc-files/MasterUnit.gif">
*
*
* @author Wade Wassenberg
*
* @version 1.0
*/

public class MasterUnitPanel extends UnitPanel implements ActionListener
{
    
    
    private Label title;
    private Button lightsOnButton = new Button("All Lights On");
    private Button lightsOffButton = new Button("All Lights Off");
    private Button unitsOffButton = new Button("All Units Off");
    private Command lightsOnCommand;
    private Command lightsOffCommand;
    private Command unitsOffCommand;
    
    
    /** MasterUnitPanel constructs a new awt-based panel for controling all
    * connected X10 modules.
    *
    * @param controller the controller to attatch to
    * @param address the house address associated with this panel
    * @exception IllegalArgumentException thrown if the address
    * specified does not follow X10 address rules.
    *
    */
    
    public MasterUnitPanel(Controller controller, String address) throws IllegalArgumentException
    {
        this(controller, address, "Master");
    }
    
    
    /** MasterUnitPanel constructs a new awt-based panel for controling all
    * connected X10 modules.
    *
    * @param controller the controller to attatch to
    * @param address the house address associated with this panel
    * @param title the title to be displayed at the top of the panel
    * @exception IllegalArgumentException thrown if the address
    * specified does not follow X10 address rules.
    *
    */
    
    public MasterUnitPanel(Controller controller, String address, String title) throws IllegalArgumentException
    {
        super(controller, address);
        this.title = new Label(title);
        setLayout(new BorderLayout());
        Panel north = new Panel();
        north.setLayout(new FlowLayout(FlowLayout.CENTER));
        north.add(this.title);
        add("North", north);
        Panel flip = new Panel();
        flip.setLayout(new GridLayout(3,1));
        flip.add(lightsOnButton);
        flip.add(lightsOffButton);
        flip.add(unitsOffButton);
        add("Center", flip);
        add("South", new Panel());
        add("East", new Panel());
        add("West", new Panel());
        lightsOnButton.addActionListener(this);
        lightsOffButton.addActionListener(this);
        unitsOffButton.addActionListener(this);
        lightsOnCommand = new Command(address, Command.ALL_LIGHTS_ON);
        lightsOffCommand = new Command(address, Command.ALL_LIGHTS_OFF);
        unitsOffCommand = new Command(address, Command.ALL_UNITS_OFF);
    }
    
    
    /* on
    *
    *
    */
    
    public void on()
    {
    }
    
    
    /* off
    *
    *
    */
    
    public void off()
    {
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
    
    
    /** actionPerformed handles clicks on the allLightsOn, allLightsOff and allUnitsOff buttons
    *
    * @param event the ActionEvent describing the source of the action.
    *
    */
    
    public void actionPerformed(ActionEvent event)
    {
        Object source = event.getSource();
        if(source == lightsOnButton)
        {
            addCommand(lightsOnCommand);
        }
        else if(source == lightsOffButton)
        {
            addCommand(lightsOffCommand);
        }
        else if(source == unitsOffButton)
        {
            addCommand(unitsOffCommand);
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
}