/*
 * Copyright (c) 2010-2022, KNOPFLERFISH project
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
package org.knopflerfish.bundle.component;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

class ReferenceDescription
{
  static final int FIELD_ELEM_SERVICE = 0;
  static final int FIELD_ELEM_REFERENCE = 1;
  static final int FIELD_ELEM_SERVICE_OBJECTS = 2;
  static final int FIELD_ELEM_PROPERTIES = 3;
  static final int FIELD_ELEM_TUPLE = 4;

  static final String SCOPE_PROTOTYPE_REQUIRED = "prototype_required";

  final String name;
  final String interfaceName;
  final boolean optional;
  final boolean multiple;
  final boolean dynamic;
  final boolean greedy;
  final String bind;
  final String unbind;
  final String updated;
  final String scope;
  final String field;
  final Boolean fieldUpdate;
  final int fieldCollectionType;
  final Filter targetFilter;

  ReferenceDescription(String name,
                       String interfaceName,
                       boolean optional,
                       boolean multiple,
                       boolean dynamic,
                       boolean greedy,
                       String target,
                       String bind,
                       String unbind,
                       String updated,
                       String scope,
                       String field,
                       Boolean fieldUpdate,
                       int fieldCollectionType)
    throws InvalidSyntaxException
  {
    targetFilter = (target != null) ? FrameworkUtil.createFilter(target) : null;
    this.name = name;
    this.interfaceName = interfaceName;
    this.optional = optional;
    this.multiple = multiple;
    this.dynamic = dynamic;
    this.greedy = greedy;
    this.bind = bind;
    this.unbind = unbind;
    this.updated = updated;
    this.scope = scope;
    this.field = field;
    this.fieldUpdate = fieldUpdate;
    this.fieldCollectionType = fieldCollectionType;
  }

  boolean isFieldUpdate() {
    return fieldUpdate != null && fieldUpdate;
  }

}
