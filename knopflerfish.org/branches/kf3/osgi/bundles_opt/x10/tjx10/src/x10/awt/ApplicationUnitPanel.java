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
import java.io.IOException;


/** ApplicationUnitPanel is a UnitPanel that can be used to execute external
* programs.
*
*
* @author Wade Wassenberg
*
* @version 1.0
*/

public class ApplicationUnitPanel extends UnitPanel implements ActionListener, Runnable
{
    
    
    private Label title;
    private Button onButton = new Button("On");
    private Button offButton = new Button("Off");
    private boolean isOn;
    private Command onCommand;
    private Command offCommand;
    private String command;
    private Process process;
    
    
    /** ApplicationUnitPanel constructs a new awt-based panel for controlling
    * the execution of an external application/command.
    *
    * @param command the command line for an external application to control
    * @param controller the controller to attatch to
    * @param address the unit address associated with this panel
    * @exception IllegalArgumentException thrown if the address
    * specified does not follow X10 address rules.
    *
    */
    
    public ApplicationUnitPanel(String command, Controller controller, String address) throws IllegalArgumentException
    {
        this(command, controller, address, command);
    }
    
    
    /** ApplicationUnitPanel constructs a new awt-based panel for controlling
    * the execution of an external application/command.
    *
    * @param command the command line for an external application to control
    * @param controller the controller to attatch to
    * @param address the unit address associated with this panel
    * @param title the title to be displayed at the top of the panel
    * @exception IllegalArgumentException thrown if the address
    * specified does not follow X10 address rules.
    *
    */
    
    public ApplicationUnitPanel(String command, Controller controller, String address, String title) throws IllegalArgumentException
    {
        super(controller, address);
        this.title = new Label(title);
        this.command = command;
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
        if(process == null)
        {
            try
            {
                process = Runtime.getRuntime().exec(command);
                new Thread(this).start();
            }
            catch(IOException ioe)
            {
                off();
            }
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
        if(process != null)
        {
            process.destroy();
            process = null;
        }
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
    
    
    /** run executes the wait thread for the application/command that was run
    *
    *
    */
    
    public void run()
    {
        try
        {
            process.waitFor();
            addCommand(offCommand);
            off();
        }
        catch(InterruptedException ie)
        {
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