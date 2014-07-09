/*
 * Copyright (c) 2013-2013, KNOPFLERFISH project
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

package org.knopflerfish.service.repositorymanager;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.repository.Repository;

public class RepositoryInfo implements Comparable<RepositoryInfo> {

  final private long id;
  final private int rank;
  final ServiceReference<Repository> sr;

  public RepositoryInfo(ServiceReference<Repository> sr) {
    this.id = ((Long)sr.getProperty(Constants.SERVICE_ID)).longValue();
    Object r = sr.getProperty(Constants.SERVICE_RANKING);
    if (r != null && r instanceof Integer) {
      this.rank = ((Integer)r).intValue();
    } else {
      this.rank = 0;
    }
    this.sr = sr;
  }

  public RepositoryInfo(RepositoryInfo old, int rank) {
    this.id = old.id;
    this.rank = rank;
    this.sr = old.sr;
  }

  public long getId() {
    return id;
  }

  public int getRank() {
    return rank;
  }

  public Object getProperty(String prop) {
    return sr.getProperty(prop);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (id ^ (id >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null)
      return false;
    if (getClass() != o.getClass())
      return false;
    RepositoryInfo rio = (RepositoryInfo) o;
    if (id != rio.id || rank != rio.rank)
      return false;
    return true;
  }

  @Override
  public int compareTo(RepositoryInfo o) {
    if (equals(o)) {
      return 0;
    }
    if (rank != o.rank) {
      return o.rank - rank;
    } else {
      return id < o.id ? -1 : 1;
    }
  }

  public ServiceReference<Repository> getServiceReference() {
    return sr;
  }

  @Override
  public String toString() {
    return "RepositoryInfo [id=" + id + ", rank=" + rank + "]";
  }

}
