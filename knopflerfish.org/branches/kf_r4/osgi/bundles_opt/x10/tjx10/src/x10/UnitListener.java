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


/** UnitListener is the interface implemented by objects that wish
* to recieve UnitEvents.  This interface is typically implemented
* by software-based x10 modules.  A registered UnitListener will
* recieve ALL events for ALL housecodes and unitcodes.  It is up
* to the implementing class to filter out the events for specific
* housecodes and unitcodes if desired.
*
*
* @author Wade Wassenberg
*
* @version 1.0
*/

public interface UnitListener
{
    
    
    /** allUnitsOff is called when an x10 All Units Off event occurs.
    *
    * @param event the UnitEvent that is dispatched.
    *
    */
    
    public void allUnitsOff(UnitEvent event);
    
    
    /** allLightsOff is called when an x10 All Lights Off event occurs.
    *
    * @param event the UnitEvent that is dispatched.
    *
    */
    
    public void allLightsOff(UnitEvent event);
    
    
    /** allLightsOn is called when an x10 All Lights On event occurs.
    *
    * @param event the UnitEvent that is dispatched.
    *
    */
    
    public void allLightsOn(UnitEvent event);
    
    
    /** unitOn is called when an x10 On event occurs.
    *
    * @param event the UnitEvent that is dispatched.
    *
    */
    
    public void unitOn(UnitEvent event);
    
    
    /** unitOff is called when an x10 Off event occurs.
    *
    * @param event the UnitEvent that is dispatched.
    *
    */
    
    public void unitOff(UnitEvent event);
    
    
    /** unitDim is called when an x10 Dim event occurs.
    *
    * @param event the UnitEvent that is dispatched.
    *
    */
    
    public void unitDim(UnitEvent event);
    
    
    /** unitBright is called when an x10 Bright event occurs.
    *
    * @param event the UnitEvent that is dispatched.
    *
    */
    
    public void unitBright(UnitEvent event);
}