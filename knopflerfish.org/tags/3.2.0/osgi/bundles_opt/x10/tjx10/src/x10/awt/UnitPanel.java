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

import java.awt.Panel;
import x10.*;
import x10.util.ThreadSafeQueue;


/** UnitPanel is the superclass extended by all awt x10 virtual unit
* panels.  This class provides all of the common functionality between
* different awt unit panels.
*
*
* @author Wade Wassenberg
*
* @version 1.0
*/

public abstract class UnitPanel extends Panel implements UnitListener
{
    private Controller controller;
    
    
    /** address String - the X10 device address associated
    * with this panel.
    *
    */
    
    protected String address;
    
    private ThreadSafeQueue commandQueue;
    
    
    /** UnitPanel constructs the UnitPanel shell and registers with
    * the controller, and associates the X10 device address.
    *
    * @param controller the controller to attatch to
    * @param address the device address associated with this panel
    * @exception IllegalArgumentException thrown if the address
    * specified does not follow X10 address rules.
    *
    */
    
    public UnitPanel(Controller controller, String address) throws IllegalArgumentException
    {
        this.controller = controller;
        controller.addUnitListener(this);
        if(Command.isValid(address))
        {
            this.address = address.toUpperCase();
        }
        else
        {
            throw new IllegalArgumentException(address);
        }
        commandQueue = new ThreadSafeQueue();
    }
    
    
    /** addCommand is a convienience method for posting a command from
    * the subclassed UnitPanel to the controller.
    *
    * @param command the command to be posted to the controller.
    *
    */
    
    public void addCommand(Command command)
    {
        commandQueue.enqueue(command);
        controller.addCommand(command);
    }
    
    
    /** on should be implemented by the subclass to handle receiving an
    * on command.  This method will be called when any appropriate "on"
    * event is received.
    *
    *
    */
    
    public abstract void on();
    
    
    /** off should be implemented by the subclass to handle receiveing an
    * off command.  This method will be called when any appropriate "off"
    * event is received.
    *
    *
    */
    
    public abstract void off();
    
    
    /** dim should be implemented by the subclass to handle receiving a
    * dim command.  This method will be called when any appropriate "dim"
    * event is received.
    *
    * @param percent - percentage to dim the module by
    *
    */
    
    public abstract void dim(int percent);
    
    
    /** brighten should be implemented by the subclass to handle receiving a
    * brighten command.  This method will be called when any appropriate "brighten"
    * event is received.
    *
    * @param percent - percentage to brighten the module by
    *
    */
    
    public abstract void brighten(int percent);
    
    
    /* allUnitsOff
    *
    * @param event
    *
    */
    
    public void allUnitsOff(UnitEvent event)
    {
        if(event.getCommand() == commandQueue.peek())
        {
            commandQueue.dequeue();
        }
        else if(Character.toUpperCase(event.getCommand().getHouseCode()) ==
            Character.toUpperCase(address.charAt(0)))
            {
                off();
            }
    }
    
    
    /* allLightsOff
    *
    * @param event
    *
    */
    
    public void allLightsOff(UnitEvent event)
    {
        if(event.getCommand() == commandQueue.peek())
        {
            commandQueue.dequeue();
        }
        else if(Character.toUpperCase(event.getCommand().getHouseCode()) ==
            Character.toUpperCase(address.charAt(0)))
            {
                off();
            }
    }
    
    
    /* allLightsOn
    *
    * @param event
    *
    */
    
    public void allLightsOn(UnitEvent event)
    {
        if(event.getCommand() == commandQueue.peek())
        {
            commandQueue.dequeue();
        }
        else if(Character.toUpperCase(event.getCommand().getHouseCode()) ==
            Character.toUpperCase(address.charAt(0)))
            {
                on();
            }
    }
    
    
    /* unitOn
    *
    * @param event
    *
    */
    
    public void unitOn(UnitEvent event)
    {
        String eventAddress = event.getCommand().getHouseCode() + "" + event.getCommand().getUnitCode();
        if(event.getCommand() == commandQueue.peek())
        {
            commandQueue.dequeue();
        }
        else if(eventAddress.equalsIgnoreCase(address))
        {
            on();
        }
    }
    
    
    /* unitOff
    *
    * @param event
    *
    */
    
    public void unitOff(UnitEvent event)
    {
        String eventAddress = event.getCommand().getHouseCode() + "" + event.getCommand().getUnitCode();
        if(event.getCommand() == commandQueue.peek())
        {
            commandQueue.dequeue();
        }
        else if(eventAddress.equalsIgnoreCase(address))
        {
            off();
        }
    }
    
    
    /* unitDim
    *
    * @param event
    *
    */
    
    public void unitDim(UnitEvent event)
    {
        String eventAddress = event.getCommand().getHouseCode() + "" + event.getCommand().getUnitCode();
        if(event.getCommand() == commandQueue.peek())
        {
            commandQueue.dequeue();
        }
        else if(eventAddress.equalsIgnoreCase(address))
        {
            dim(event.getCommand().getLevel());
        }
    }
    
    
    /* unitBright
    *
    * @param event
    *
    */
    
    public void unitBright(UnitEvent event)
    {
        String eventAddress = event.getCommand().getHouseCode() + "" + event.getCommand().getUnitCode();
        if(event.getCommand() == commandQueue.peek())
        {
            commandQueue.dequeue();
        }
        else if(eventAddress.equalsIgnoreCase(address))
        {
            brighten(event.getCommand().getLevel());
        }
    }
    
    
    /** getController returns the x10.Controller that is responsible for controlling
    * this virtual Unit.
    *
    * @return Controller the Controller that is responsible for controlling the unit
    * associated with this panel.
    *
    */
    
    public Controller getController()
    {
        return(controller);
    }
    
    
    /** getAddress returns the X10 address of this virtual Unit.  (A1 - P16)
    *
    * @return String the X10 address of the unit associated with this panel.
    *
    */
    
    public String getAddress()
    {
        return(address);
    }
}