/*
 * Copyright (c) 2005, KNOPFLERFISH project
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

package org.knopflerfish.framework;

/**
 * @author Administrator
 *
 * This class is a helper classes that supply additional information to the
 * component class
 */
public class ComponentReferenceInfo {

  /* component reference name */
  private String referenceName;

  /* Java interface */
  private String interfaceType;

  /* The number of service wich will bind up to this component */
  private String cardinality;

  /*
   * Indicate when this service may be activated and deactivated, (either
   * static or dynamic)
   */
  private String policy;

  /* Selection filter to be used when selecting a desired service */
  private String target;

  /* The name of the component method to call when to bind */
  private String bind;

  /* The name of the component method to call when to unbind */
  private String unbind;

  /**
   * @return Returns the bind.
   */
  public String getBind() {
    return bind;
  }

  /**
   * @param bind The bind to set.
   */
  public void setBind(String bind) {
    this.bind = bind;
  }

  /**
   * @return Returns the cardinality.
   */
  public String getCardinality() {
    return cardinality;
  }

  /**
   * @param cardinality The cardinality to set.
   */
  public void setCardinality(String cardinality) {
    this.cardinality = cardinality;
  }

  /**
   * @return Returns the interfaceType.
   */
  public String getInterfaceType() {
    return interfaceType;
  }

  /**
   * @param interfaceType The interfaceType to set.
   */
  public void setInterfaceType(String interfaceType) {
    this.interfaceType = interfaceType;
  }

  /**
   * @return Returns the policy.
   */
  public String getPolicy() {
    return policy;
  }

  /**
   * @param policy The policy to set.
   */
  public void setPolicy(String policy) {
    this.policy = policy;
  }

  /**
   * @return Returns the referenceName.
   */
  public String getReferenceName() {
    return referenceName;
  }

  /**
   * @param referenceName The referenceName to set.
   */
  public void setReferenceName(String referenceName) {
    this.referenceName = referenceName;
  }

  /**
   * @return Returns the target.
   */
  public String getTarget() {
    return target;
  }

  /**
   * @param target The target to set.
   */
  public void setTarget(String target) {
    this.target = target;
  }

  /**
   * @return Returns the unbind.
   */
  public String getUnbind() {
    return unbind;
  }

  /**
   * @param unbind The unbind to set.
   */
  public void setUnbind(String unbind) {
    this.unbind = unbind;
  }
}
