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

package x10.util;
import java.util.LinkedList;


/** ThreadSafeQueue this is an implementation of a First In First Out (FIFO)
* data structure.  This class uses synchronization to provide thread safety
* for adding and removing objects to and from the queue.  All of the methods
* in this class are synchronized, therefore temporary thread blocking might
* occur when calling any of these methods.
*
*
* @author Wade Wassenberg
*
* @version 1.0
*/

public class ThreadSafeQueue
{
    
    
    /** queue LinkedList the list of objects in the queue
    *
    */
    
    LinkedList queue;
    
    
    /** ThreadSafeQueue constructs an empty ThreadSafeQueue
    *
    *
    */
    
    public ThreadSafeQueue()
    {
        queue = new LinkedList();
    }
    
    
    /** enqueue adds the specified object to the end of the queue
    *
    * @param element the object to be added to the end of the queue
    *
    */
    
    public synchronized void enqueue(Object element)
    {
        queue.add(element);
        try
        {
            notifyAll();
        }
        catch(IllegalMonitorStateException imse)
        {
        }
    }
    
    
    /** dequeueNextAvailable blocks until the next object becomes available
    * to the queue.  If the queue is not empty, the first object is removed
    * from the queue and returned without blocking.
    *
    * @return Object - the next available object in the queue.
    * @exception InterruptedException if the blocked thread is interrupted
    * before an object becomes available.
    *
    */
    
    public synchronized Object dequeueNextAvailable() throws InterruptedException
    {
        while(queue.size() < 1)
        {
            wait();
        }
        Object element = queue.getFirst();
        queue.removeFirst();
        return(element);
    }
    
    
    /** dequeue removes and returns the first object in the queue without
    * blocking.  If the queue is empty, null is returned.
    *
    * @return Object - the next object in the queue or null if the queue is
    * empty.
    *
    */
    
    public synchronized Object dequeue()
    {
        if(queue.size() < 1)
        {
            return(null);
        }
        else
        {
            Object element = queue.getFirst();
            queue.removeFirst();
            return(element);
        }
    }
    
    
    /** peek returns the next available object in the queue without
    * physically removing the object from the queue.
    *
    * @return Object the next available object in the queue or null
    * if the queue is empty.
    *
    */
    
    public synchronized Object peek()
    {
        if(queue.size() < 1)
        {
            return(null);
        }
        else
        {
            return(queue.getFirst());
        }
    }
    
    
    /** dequeue removes the specified object from the queue
    * if and only if the specified object is the first object
    * in the queue.
    *
    * @param toDequeue the object to dequeue
    *
    */
    
    public synchronized void dequeue(Object toDequeue)
    {
        if(queue.size() > 0)
        {
            if(queue.getFirst() == toDequeue)
            {
                queue.removeFirst();
            }
        }
    }
    
    
    /** empty completely removes all objects that are currently in the
    * queue.
    *
    *
    */
    
    public synchronized void empty()
    {
        while(dequeue() != null);
    }
}