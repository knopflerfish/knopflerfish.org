/*
 * Copyright (c) 2003, KNOPFLERFISH project
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

package org.knopflerfish.util.cm;

// OSGi packages

// Gatespace packages

// XML packages (provided by the jaxp bundle)
import com.sun.xml.tree.*;
import com.sun.xml.parser.*;
import org.xml.sax.*;
import org.w3c.dom.*;


/**
 ** An entity resolve that knows System IDs for the meta data DTD
 ** (cfg) and for the cm_data DTD.
 **
 ** @author Gatespace AB
 ** @version $Revision: 1.1.1.1 $
 **/
public class CmResolver extends Resolver{

  public CmResolver() {
    String cm_data_loc = CMDataManager.class
      .getResource( "/" + CMDataManager.CM_DATA_0_1_URI ).toString();
    registerCatalogEntry( CMDataManager.CM_DATA_0_1_ID, cm_data_loc );

    String cfg_1_0_dtd_loc = MetaDataManager.class
      .getResource("/" + MetaDataManager.CFG_1_0_DTD_URI).toString();
    registerCatalogEntry( MetaDataManager.CFG_1_0_DTD_ID, cfg_1_0_dtd_loc);
  }

  /*
  public InputSource resolveEntity(java.lang.String publicId,
				   java.lang.String systemId)
    throws SAXException, java.io.IOException 
  {
    System.err.println("Resolving entity; publicID: '"+publicId
		       +"', systemId: '" +systemId +"'.");
    InputSource is = super.resolveEntity( publicId, systemId );
    System.err.println("InputSource.getSystemId="+is.getSystemId());
    System.err.println("InputSource.getPublicId="+is.getPublicId());
    return is;
  }
  */
}
