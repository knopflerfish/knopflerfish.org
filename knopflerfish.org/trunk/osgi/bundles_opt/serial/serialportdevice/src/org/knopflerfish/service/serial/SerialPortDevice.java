/**
 * Copyright (c) 2001 Gatespace AB. All Rights Reserved.
 *
 * $Header: /cvs/gs/gosg/gatespace_bundles/serialport/serialport_api/public_src/SerialPortDevice.java,v 1.2 2001/05/13 09:18:38 tommy Exp $
 * $Revision: 1.2 $
 */

package org.knopflerfish.service.serial;

import org.osgi.service.device.*;

import javax.comm.*;

/**
 * Service wrapping a javax.comm.SerialPort and enabling
 * it to participate in DeviceManager match()/attach() process.
 *
 * @version $Revision: 1.2 $
 */
public interface SerialPortDevice extends Device {
  public static final int MATCH_VENDOR_PRODUCT_REVISION = 4;
  public static final int MATCH_VENDOR_PRODUCT          = 3;
  public static final int MATCH_CLASS                   = 2;
  public static final int MATCH_GENERIC                 = 1;

  /**
   * Get the serial port connected to this device.
   *
   * Caller is responsible for calling releaseSerialPort
   * when done.
   *
   * @return SerialPort object.
   */
  SerialPort allocateSerialPort();

  /**
   * Release the allocated serial port.
   */
  void releaseSerialPort();

}
