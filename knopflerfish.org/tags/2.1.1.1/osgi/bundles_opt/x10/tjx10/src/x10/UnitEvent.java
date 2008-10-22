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

package x10;

import java.io.Serializable;


/** UnitEvent an event representing a change in state of an
* x10 hardware or software module.
*
*
* @author Wade Wassenberg
*
* @version 1.0
*/

public class UnitEvent implements Serializable
{
    
    
    private Command command;
    
    
    /** UnitEvent constructs a UnitEvent based on the specified
    * Command.
    *
    * @param command the command that generated this event.
    *
    */
    
    public UnitEvent(Command command)
    {
        this.command = command;
    }
    
    
    /** getCommand returns the command that generated this event.
    *
    * @return Command the Command that generated this event.
    *
    */
    
    public Command getCommand()
    {
        return(command);
    }
}