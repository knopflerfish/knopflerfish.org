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


/** Controller is implemented by any class that can act as an entry
* point for controlling x10 devices.  A Controller must be able to
* distribute added Commands to ALL registered x10 hardware and software
* modules.  A Controller must also handle the addition and removal of
* UnitListeners.
*
*
* @author Wade Wassenberg
*
* @version 1.0
* @see x10.Command
* @see x10.UnitListener
*/

public interface Controller
{
    
    
    /** addUnitListener registers the specified UnitListener to recieve
    * ALL events that occur, whether initiated by hardware or software
    * control modules.
    *
    * @param listener the object to recieve UnitEvent objects.
    * @see x10.UnitEvent
    */
    
    public void addUnitListener(UnitListener listener);
    
    
    /** removeUnitListener unregisters the specified UnitListener.
    * If the specified UnitListener isn't registered, then it is
    * ignored.
    *
    * @param listener the listener to unregister.
    *
    */
    
    public void removeUnitListener(UnitListener listener);
    
    
    /** addCommand adds a Command to the queue to be dispatched to
    * all hardware and software x10 modules.
    *
    * @param command the Command to be queued.
    *
    */
    
    public void addCommand(Command command);
}