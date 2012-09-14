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
import java.io.*;
import java.net.*;
import x10.util.*;


/** SocketController is a Client-side virtual Controller object that
* connects to a ControllerServer via TCP socket.
*
*
* @author Wade Wassenberg
*
* @version 1.0
*/

public class SocketController implements Runnable, Controller
{
    
    private UnitEventDispatcher dispatcher;
    private ThreadSafeQueue queue;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private String host;
    private int port;
    private boolean running;
    
    
    /** SocketController constructs a SocketController that automatically
    * connects to the ControllerServer on the specified host and port.
    *
    * @param host the hostname or IP address of the server that is hosting
    * the ControllerServer.
    * @param port the port that the ControllerServer is listening on.
    * @exception IOException thrown if an error occurs setting up the streams
    * to/from the ControllerServer
    * @exception SocketException thrown if an error occurs connecting to the
    * ControllerServer
    *
    */
    
    public SocketController(String host, int port) throws IOException, SocketException
    {
        this.host = host;
        this.port = port;
        Socket s = new Socket(host, port);
        ois = new ObjectInputStream(s.getInputStream());
        oos = new ObjectOutputStream(s.getOutputStream());
        queue = new ThreadSafeQueue();
        dispatcher = new UnitEventDispatcher();
        new Thread(this).start();
    }
    
    
    /** addUnitListener registers the specified UnitListener to recieve
    * UnitEvents
    *
    * @param listener the listener to register
    *
    */
    
    public void addUnitListener(UnitListener listener)
    {
        dispatcher.addUnitListener(listener);
    }
    
    
    /** removeUnitListener unregisters the specified UnitListener.
    *
    * @param listener the listener to remove.
    *
    */
    
    public void removeUnitListener(UnitListener listener)
    {
        dispatcher.removeUnitListener(listener);
    }
    
    
    /** addCommand adds a Command to be queued and then dispatched to
    * the ControllerServer
    *
    * @param command the Command to be queued.
    *
    */
    
    public void addCommand(Command command)
    {
        LogHandler.logMessage("enqueueing command", 2);
        if(queue.peek() != null)
        {
            queue.enqueue(command);
        }
        else
        {
            queue.enqueue(command);
            initiateNextCommand();
        }
        dispatcher.dispatchUnitEvent(new UnitEvent(command));
    }
    
    
    private synchronized void initiateNextCommand()
    {
        Command nextCommand = (Command) queue.dequeue();
        if(nextCommand != null)
        {
            try
            {
                oos.writeObject(nextCommand);
                oos.flush();
                LogHandler.logMessage("command written to server", 2);
            }
            catch(IOException ioe)
            {
                LogHandler.logException(ioe, 1);
            }
        }
    }
    
    
    /** run loops receiving events from the ControllerServer and then
    * dispatching the events to locally registered UnitListeners.
    *
    *
    */
    
    public void run()
    {
        running = true;
        dispatcher.start();
        try
        {
            while(running)
            {
                LogHandler.logMessage("blocking read...", 2);
                UnitEvent nextEvent = (UnitEvent) ois.readObject();
                LogHandler.logMessage("event recieved from server", 2);
                dispatcher.dispatchUnitEvent(nextEvent);
                if(running)
                {
                    initiateNextCommand();
                }
            }
        }
        catch(Exception e)
        {
            LogHandler.logException(e, 1);
        }
        dispatcher.kill();
    }
}