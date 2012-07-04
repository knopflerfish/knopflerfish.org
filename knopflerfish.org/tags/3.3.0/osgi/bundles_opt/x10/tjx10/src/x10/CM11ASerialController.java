/*
* Copyright 2002-2004, Wade Wassenberg  All rights reserved.
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

import java.io.*;
import javax.comm.*;
import java.util.StringTokenizer;
import java.util.LinkedList;
import x10.util.ThreadSafeQueue;
import x10.util.LogHandler;


/** CM11ASerialController is an X10 Controller that bridges x10 hardware
* and software by communicating via a SerialPort with the x10
* "CM11A" module.  <BR><BR>
*
* This class requires the javax.comm package from Sun Microsystems.
*
* @author Wade Wassenberg
*
* @version 1.0
*/

public class CM11ASerialController implements Runnable, Controller
{
    
    
    /** OK byte - the x10 "CM11A" protocol OK byte.
    *
    */
    
    public static final byte OK = ((byte) 0x00);
    
    
    /** READY byte - the x10 "CM11A" protocol READY byte.
    *
    */
    
    public static final byte READY = ((byte) 0x55);
    
    
    /** TIME byte - the x10 "CM11A" protocol TIME byte.
    *
    */
    
    public static final byte TIME = ((byte) 0x9B);
    
    
    /** TIME_POLL byte - the x10 "CM11A" protocol TIME_POLL byte.
    *
    */
    
    public static final byte TIME_POLL = ((byte) 0xA5);
    
    
    /** DATA_POLL byte - the x10 "CM11A" protocol DATA_POLL byte.
    *
    */
    
    public static final byte DATA_POLL = ((byte) 0x5A);
    
    
    /** PC_READY byte - the x10 "CM11A" protocol PC_READY byte.
    *
    */
    
    public static final byte PC_READY = ((byte) 0xC3);
    
    private static final Command STOP = new Command("A1", Command.DIM, 0);
    
    private DataInputStream fromX10;
    private DataOutputStream toX10;
    private SerialPort sp;
    private boolean running;
    private byte[] lastAddresses;
    private UnitEventDispatcher dispatcher;
    private ThreadSafeQueue queue;

    /** CM11ASerialController constructs and starts the Controller on the
    * specified comport.  On a Windows based PC, the comport is of the
    * form "COM1".
    *
    * @param comport the communications port in which the "CM11A"
    * module is connected.
    * @exception IOException if an error occurs while trying to connect
    * to the specified Communications Port.
    *
    */
    
    public CM11ASerialController(String comport) throws IOException
    {
        try
        {
            CommPortIdentifier cpi = CommPortIdentifier.getPortIdentifier(comport);
            sp = (SerialPort) cpi.open("JavaX10Controller", 10000);
            sp.setSerialPortParams(4800, SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);
            fromX10 = new DataInputStream(sp.getInputStream());
            toX10 = new DataOutputStream(sp.getOutputStream());
        }
        catch(NoSuchPortException nspe)
        {
            throw new IOException("No Such Port: " + nspe.getMessage());
        }
        catch(PortInUseException piue)
        {
            throw new IOException("Port in use: " + piue.getMessage());
        }
        catch(UnsupportedCommOperationException ucoe)
        {
            throw new IOException("Unsupported comm operation: " + ucoe.getMessage());
        }
        queue = new ThreadSafeQueue();
        lastAddresses = new byte[0];
        dispatcher = new UnitEventDispatcher();
        new Thread(this).start();
    }
    
    
    /** addUnitListener registers the UnitListener for events.
    *
    * @param listener the listener to register for events.
    *
    */
    
    public void addUnitListener(UnitListener listener)
    {
        dispatcher.addUnitListener(listener);
    }
    
    
    /** removeUnitListener unregisters the UnitListener for events.
    *
    * @param listener the listener to remove.
    *
    */
    
    public void removeUnitListener(UnitListener listener)
    {
        dispatcher.removeUnitListener(listener);
    }
    
    
    private byte getChecksum(short packet)
    {
        byte header = (byte) ((packet >> 8) & 0x00FF);
        byte code = (byte) (packet & 0x00FF);
        return((byte) ((header + code) & 0xFF));
    }
    
    private void setInterfaceTime() throws IOException
    {
        toX10.writeByte(TIME);
        toX10.writeByte(0);
        toX10.writeByte(0);
        toX10.writeByte(0);
        toX10.writeByte(0);
        toX10.writeByte(0);
        toX10.writeByte(0);
    }
    
    
    /** addCommand adds a command to the queue to be dispatched.
    *
    * @param command the Command to be dispatched.
    *
    */
    
    public void addCommand(Command command)
    {
        if(queue.peek() != null)
        {
            queue.enqueue(command);
        }
        else
        {
            queue.enqueue(command);
            initiateNextCommand();
        }
    }
    
    
    /** finalize disconnects the serial port connection and closes
    * the Controller.
    *
    *
    */
    
    protected void finalize()
    {
        addCommand(STOP);
        dispatcher.kill();
    }

    private synchronized void doNotify()
    {
        notifyAll();
    }

    private synchronized void doWait(long millis) throws InterruptedException
    {
        wait(millis);
    }

    /** shutdown tells the controller to finish all commands
    * in the queue and then gracefully disconnects the serial 
    * port connection.
    *
    * @param millis the number of milliseconds to wait for a graceful shutdown.
    * @exception OperationTimedOutException thrown if the Controller has not
    * completely shutdown in the amount of time specified.
    * @exception InterruptedException thrown if the thread is unexpectedly interrupted
    *
    */
    
    public void shutdown(long millis) throws OperationTimedOutException, InterruptedException
    {
        if(running)
        {
            try
            {
                finalize();
                doWait(millis);
                if(running)
                {
                    throw new OperationTimedOutException("Timed out while waiting for CM11ASerialController to shutdown");
                }
            }
            catch(InterruptedException ie)
            {
                if(running)
                {
                    throw ie;
                }
            }
        }
    }

    /** shutdownNow shuts down the controller and closes the serial port immediately.
    * shutdown(long) is the preferred method of shutting down the controller, but this
    * method provides an immediate, unclean, non-graceful means to shut down the controller.
    *
    */
    
    public void shutdownNow()
    {
        SerialPort sp = this.sp;
        this.sp = null;
        sp.close();
        dispatcher.kill();
    }

    private synchronized void initiateNextCommand()
    {
        Command nextCommand = (Command) queue.peek();
        if(nextCommand != null)
        {
            try
            {
                toX10.writeShort(nextCommand.getAddress());
                toX10.flush();
            }
            catch(IOException ioe)
            {
                if(sp != null) //shutdownNow was not invoked
                {
                    LogHandler.logException(ioe, 1);
                }
            }
        }
    }
    
    private synchronized void handleChecksum(byte checksum)
    {
        Command nextCommand = (Command) queue.peek();
        if(nextCommand != null)
        {
            if(checksum == getChecksum(nextCommand.getAddress()))
            {
                try
                {
                    toX10.writeByte(OK);
                    toX10.flush();
                    byte ready = (byte) fromX10.readByte();
                    if(nextCommand == STOP)
                    {
                        running = false;
                    }
                    else
                    {
                        if(ready == READY)
                        {
                            toX10.writeShort(nextCommand.getFunction());
                            toX10.flush();
                            if(((byte) fromX10.readByte()) == getChecksum(nextCommand.getFunction()))
                            {
                                toX10.writeByte(OK);
                                toX10.flush();
                                ready = (byte) fromX10.readByte();
                                if(ready == READY)
                                {
                                    dispatcher.dispatchUnitEvent(new UnitEvent((Command) queue.dequeue()));
                                }
                            }
                        }
                    }
                }
                catch(IOException ioe)
                {
                    if(sp != null) //shutdownNow was not invoked
                    {
                        LogHandler.logException(ioe, 1);
                    }
                }
            }
            else
            {
                LogHandler.logMessage("CheckSum: " + Integer.toHexString(checksum), 2);
            }
        }
    }
    
    private synchronized void handleData() throws IOException
    {
        toX10.writeByte(PC_READY);
        int length = fromX10.readByte();
        if((length > 0) && (length < 10))
        {
            byte detail = (byte) fromX10.readByte();
            byte[] data = new byte[length - 1];
            boolean[] isAddr = new boolean[length - 1];
            for(int i = 0; i < data.length; i++)
            {
                data[i] = (byte) fromX10.readByte();
                isAddr[i] = ((detail % 2) == 0);
                detail = ((byte) (detail >> 1));
            }
            for(int i = 0; i < isAddr.length; i++)
            {
                if(isAddr[i])
                {
                    for(int j = i + 1; j < isAddr.length; j++)
                    {
                        if(!isAddr[j])
                        {
                            byte function = Command.getFunction(data[j]);
                            byte level = 0;
                            if((function == Command.DIM) || (function == Command.BRIGHT))
                            {
                                level = data[j+1];
                            }
                            lastAddresses = new byte[j - i];
                            for(int k = i; k < j; k++)
                            {
                                lastAddresses[k - i] = data[k];
                                dispatcher.dispatchUnitEvent(new UnitEvent(new Command(data[k], function, (byte) level)));
                            }
                            if((function == Command.DIM) || (function == Command.BRIGHT))
                            {
                                i = j + 1;
                            }
                            else
                            {
                                i = j;
                            }
                            j = isAddr.length;
                        }
                    }
                }
                else
                {
                    byte function = data[i];
                    int level = 0;
                    switch(Command.getFunction(function))
                    {
                        case Command.ALL_UNITS_OFF :
                        case Command.ALL_LIGHTS_ON :
                        case Command.ALL_LIGHTS_OFF :
                        dispatcher.dispatchUnitEvent(new UnitEvent(new Command(function, function, (byte) 0)));
                        break;
                        case Command.DIM :
                        case Command.BRIGHT : i++; level = data[i];
                        case Command.ON :
                        case Command.OFF :
                        for(int l = 0; l < lastAddresses.length; l++)
                        {
                            dispatcher.dispatchUnitEvent(new UnitEvent(new Command(lastAddresses[l], function, (byte) level)));
                        }
                        break;
                    }
                }
            }
        }
    }
    
    /** run is the thread loop that constantly blocks and reads
    * events off of the serial port from the "CM11A" module.
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
                byte nextByte = (byte) fromX10.readByte();
                switch(nextByte)
                {
                    case TIME_POLL : setInterfaceTime();
                    break;
                    case DATA_POLL : handleData();
                    break;
                    default : handleChecksum(nextByte);
                }
                if(running && (fromX10.available() == 0))
                {
                    initiateNextCommand();
                }
            }
            sp.close();
            notifyAll();
        }
        catch(IOException ioe)
        {
            if(sp != null) //shutdownNow was not invoked
            {
               LogHandler.logException(ioe, 1);
            }
        }
    }
}
