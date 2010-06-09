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
import java.net.*;
import x10.util.LogHandler;


/** ControllerServer is a Network daemon for providing access to a Controller
* over a network.
*
*
* @author Wade Wassenberg
*
* @version 1.0
*/

public class ControllerServer extends Thread
{
    private Controller c;
    private int port;
    private boolean alive;
    
    
    /** ControllerServer constructs a new ControllerServer network daemon
    * which provides access to the specified Controller on the
    * specified port.
    *
    * @param c the Controller to provide network access to.
    * @param port the TCP port that the daemon will listen for connections
    *
    */
    
    public ControllerServer(Controller c, int port)
    {
        this.c = c;
        this.port = port;
    }
    
    
    /** run loops and accepts connections from SocketControllers over
    * a network.  Each connection gets its own ServerDispatchProxy.
    *
    * @see x10.net.ServerDispatchProxy
    * @see x10.net.SocketController
    */
    
    public void run()
    {
        alive = true;
        try
        {
            ServerSocket ss = new ServerSocket(port);
            while(alive)
            {
                try
                {
                    Socket s = ss.accept();
                    ServerDispatchProxy sdp = new ServerDispatchProxy(s, c);
                    c.addUnitListener(sdp);
                }
                catch(Exception e)
                {
                    LogHandler.logException(e, 1);
                    alive = false;
                }
            }
        }
        catch(Exception e)
        {
            LogHandler.logException(e, 1);
        }
    }
}