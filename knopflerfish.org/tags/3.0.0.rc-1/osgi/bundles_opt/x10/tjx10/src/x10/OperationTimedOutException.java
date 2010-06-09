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


/** OperationTimedOutException is an exception to be thrown when an operation times out.
  * 
  * 
  * @author Wade Wassenberg
  * 
  * @version 1.0
  */

public class OperationTimedOutException extends Exception
{


    /** OperationTimedOutException constructs an OperationTimedOutException with no message
      * 
      * 
      */

    public OperationTimedOutException()
    {
        super();
    }


    /** OperationTimedOutException constructs an OperationTimedOutException with the specified message
      * 
      * @param message the error message associated to the exception.
      * 
      */
  
    public OperationTimedOutException(String message)
    { 
        super(message);
    }
}
