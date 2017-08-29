/*
 * Copyright (c) 2016, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 *
 * - Neither the name of the KNOPFLERFISH project nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.knopflerfish.bundle.dirdeployer;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Vector;

import org.knopflerfish.service.dirdeployer.DeployedBundleControl.DeployedBundleControlState;

public class MarkerFile
{

  private static final String DODEPLOY_MARKER = ".dodeploy";
  private static final String DEPLOYING_MARKER = ".deploying";
  private static final String DEPLOYED_MARKER = ".deployed";
  private static final String UNDEPLOYED_MARKER = ".undeployed";
  private static final String UNDEPLOYING_MARKER = ".undeploying";
  private static final String FAILED_MARKER = ".failed";

  private static final String[] allMarkers = {
      DODEPLOY_MARKER,
      DEPLOYING_MARKER,
      DEPLOYED_MARKER,
      UNDEPLOYING_MARKER,
      UNDEPLOYED_MARKER,
      FAILED_MARKER
  };
  
  public static boolean isMarkedForDeployment(File f)
  {
    try {
      File markerFile = new File(f.getCanonicalPath() + DODEPLOY_MARKER);
      return markerFile.exists();
    } catch (IOException e) {
      return false;
    }
  }

  public static boolean isMarkerFile(File f)
  {
    String name = f.getName();
    
    for (int i = 0; i < allMarkers.length; i++) {
      if (name.endsWith(allMarkers[i]))
        return true;
    }
    return false;
  }
    
  
  public static boolean setDeployingMarker(File f) throws IOException {
    return markerTransition(f, DODEPLOY_MARKER, DEPLOYING_MARKER);
  }
  
  public static boolean setDeployedMarker(File f) throws IOException {
    return markerTransition(f, DEPLOYING_MARKER, DEPLOYED_MARKER);
  }
  
  public static boolean setUndeployingdMarker(File f) throws IOException {
    return markerTransition(f, DEPLOYED_MARKER, UNDEPLOYING_MARKER);
  }

  public static boolean setUndeployedMarker(File f) throws IOException {
    return markerTransition(f, UNDEPLOYING_MARKER, UNDEPLOYED_MARKER);
  }
  
  public static boolean clearAllMarkers(File f) throws IOException {
    boolean success = true;
    for (int i = 0; i < allMarkers.length; i++) {
      File mf = new File(f.getCanonicalPath() + allMarkers[i]);
      if (mf.exists() && !mf.delete()) {
        Activator.logger.error("Failed to delete: " + mf.getCanonicalPath());
        success = false;
      }
    }
    return success;
  }
  
  public static boolean setFailedMarker(File f) throws IOException {
    clearAllMarkers(f);
    return (new File(f.getCanonicalPath() + FAILED_MARKER)).createNewFile();
    
  }
  
  private static boolean markerTransition(File f, String from, String to) throws IOException {
    File fromMarker = new File(f.getCanonicalPath() + from);
    File toMarker = new File(f.getCanonicalPath() + to);
    
    if (fromMarker.exists()) {
      if (!fromMarker.delete())
        return false;
    }
    boolean success = toMarker.createNewFile();
    if (Activator.logger.doDebug())
      Activator.logger.debug("Changing markers: " + from + " -> " + to);
    return success;
  }

  public static boolean isMarkedAsDeployed(File f) {
    try {
      File markerFile = new File(f.getCanonicalPath() + DEPLOYED_MARKER);
      return markerFile.exists();
    } catch (IOException e) {
      return false;
    }
  }
  
  public static Collection<String> getAllMarkers(File f) throws IOException {
    Vector<String> markers = new Vector<String>();
    
    for (int i = 0; i < allMarkers.length; i++) {
      File mf = new File(f.getCanonicalPath() + allMarkers[i]);
      if (mf.exists())
        markers.add(allMarkers[i]);
    }
    return markers;
  }

  public static DeployedBundleControlState getState(String marker) {
    if (DODEPLOY_MARKER.equals(marker))
      return DeployedBundleControlState.STAGED;
    else if (DEPLOYING_MARKER.equals(marker))
      return DeployedBundleControlState.IS_DEPLOYING;
    else if (DEPLOYED_MARKER.equals(marker))
      return DeployedBundleControlState.DEPLOYED;
    else if (UNDEPLOYING_MARKER.equals(marker))
      return DeployedBundleControlState.IS_UNDEPLOYING;
    else if (UNDEPLOYED_MARKER.equals(marker))
      return DeployedBundleControlState.UNDEPLOYED;
    else if (FAILED_MARKER.equals(marker))
      return DeployedBundleControlState.FAILED;
    else throw new IllegalArgumentException("No such marker: " + marker);

  }
}
