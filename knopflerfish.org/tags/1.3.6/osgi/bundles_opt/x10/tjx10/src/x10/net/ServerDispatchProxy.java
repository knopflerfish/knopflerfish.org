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

package x10.net;

import x10.*;
import x10.util.ThreadSafeQueue;
import java.io.*;
import java.net.*;
import x10.util.LogHandler;


/** ServerDispatchProxy is the server-side proxy which dispatches events
* to and recieves commands from the SocketController assigned to this
* object.  A new ServerDispatchProxy is constructed for each SocketController
* that connects to a ControllerServer.
*
*
* @author Wade Wassenberg
*
* @version 1.0
*/

public class ServerDispatchProxy extends Thread implements UnitListener
{
    
    private Controller c;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    
    
    /** s Socket the Socket connection to the client SocketController
    *
    */
    
    Socket s;
    
    private boolean alive;
    private ThreadSafeQueue commandQueue;
    
    
    /** ServerDispatchProxy constructs a new ServerDispatchProxy for
    * the specified Controller to the SocketController on the other
    * end of the specified Socket.
    *
    * @param s the Socket to the connected SocketController
    * @param c the Controller that is being shared across the network
    * with the SocketController.
    * @exception IOException if an error occurs obtaining streams to/from
    * the connected SocketController.
    *
    */
    
    public ServerDispatchProxy(Socket s, Controller c) throws IOException
    {
        this.s = s;
        this.c = c;
        oos = new ObjectOutputStream(s.getOutputStream());
        ois = new ObjectInputStream(s.getInputStream());
        commandQueue = new ThreadSafeQueue();
        start();
    }
    
    
    /** run reads Commands from the SocketController and propogates
    * them along to the local Controller object.
    *
    *
    */
    
    public void run()
    {
        LogHandler.logMessage("ServerDispatchProxy running", 2);
        alive = true;
        while(alive)
        {
            try
            {
                LogHandler.logMessage("blocking read...", 2);
                Command nextCommand = (Command) ois.readObject();
                LogHandler.logMessage("Command recieved from client", 2);
                commandQueue.enqueue(nextCommand);
                c.addCommand(nextCommand);
            }
            catch(IOException ioe)
            {
                LogHandler.logException(ioe, 1);
                alive = false;
            }
            catch(ClassNotFoundException cnfe)
            {
                LogHandler.logException(cnfe, 1);
            }
        }
        c.removeUnitListener(this);
    }
    
    
    /** dispatchEvent sends all events received locally to the
    * SocketController that is connected.
    *
    * @param event the event to dispatch to the SocketController
    *
    */
    
    private synchronized void dispatchEvent(UnitEvent event)
    {
        if(event.getCommand() == commandQueue.peek())
        {
            commandQueue.dequeue();
        }
        else
        {
            try
            {
                oos.writeObject(event);
                oos.flush();
                LogHandler.logMessage("Event written to client", 2);
            }
            catch(Exception e)
            {
                LogHandler.logException(e, 1);
            }
        }
    }
    
    
    /** allUnitsOff recieves the UnitListener event to be dispatched
    * to the SocketController
    *
    * @param event the event to be dispatched
    *
    */
    
    public void allUnitsOff(UnitEvent event)
    {
        dispatchEvent(event);
    }
    
    
    /** allLightsOff recieves the UnitListener event to be dispatched
    * to the SocketController
    *
    * @param event the event to be dispatched
    *
    */
    
    public void allLightsOff(UnitEvent event)
    {
        dispatchEvent(event);
    }
    
    
    /** allLightsOn recieves the UnitListener event to be dispatched
    * to the SocketController
    *
    * @param event the event to be dispatched
    *
    */
    
    public void allLightsOn(UnitEvent event)
    {
        dispatchEvent(event);
    }
    
    
    /** unitOn recieves the UnitListener event to be dispatched
    * to the SocketController
    *
    * @param event the event to be dispatched
    *
    */
    
    public void unitOn(UnitEvent event)
    {
        dispatchEvent(event);
    }
    
    
    /** unitOff recieves the UnitListener event to be dispatched
    * to the SocketController
    *
    * @param event the event to be dispatched
    *
    */
    
    public void unitOff(UnitEvent event)
    {
        dispatchEvent(event);
    }
    
    
    /** unitDim recieves the UnitListener event to be dispatched
    * to the SocketController
    *
    * @param event the event to be dispatched
    *
    */
    
    public void unitDim(UnitEvent event)
    {
        dispatchEvent(event);
    }
    
    
    /** unitBright recieves the UnitListener event to be dispatched
    * to the SocketController
    *
    * @param event the event to be dispatched
    *
    */
    
    public void unitBright(UnitEvent event)
    {
        dispatchEvent(event);
    }
}