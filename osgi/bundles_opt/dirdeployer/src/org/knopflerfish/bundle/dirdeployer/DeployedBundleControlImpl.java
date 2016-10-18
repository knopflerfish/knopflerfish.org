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

import org.knopflerfish.service.dirdeployer.DeployedBundleControl;
import org.osgi.framework.Bundle;

public class DeployedBundleControlImpl implements DeployedBundleControl
{

 private DeployedBundle deployedBundle;
 private DeployedBundleControlState state;
 private Exception failure;
 private DirDeployerImpl deployer;
 
 public DeployedBundleControlImpl(DirDeployerImpl deployer, DeployedBundle db) {
   this.deployer = deployer;
   this.deployedBundle = db;
   this.state = DeployedBundleControlState.DEPLOYED;
 }
 
 public DeployedBundleControlImpl(DirDeployerImpl deployer, Exception e) {
   this.deployer = deployer;
   this.deployedBundle = null;
   this.failure = e;
   this.state = DeployedBundleControlState.FAILED;
 }
  @Override
  public void undeploy()
  {
    // deployedBundle

  }

  @Override
  public Bundle getBundle()
  {
    if (deployedBundle == null)
      return null;
    return deployedBundle.getBundle();
  }

  @Override
  public DeployedBundleControlState getDeploymentState()
  {
    return state;
  }

}
