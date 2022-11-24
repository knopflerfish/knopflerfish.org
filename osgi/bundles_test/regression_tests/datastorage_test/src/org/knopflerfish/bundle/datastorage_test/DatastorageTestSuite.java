/*
 * Copyright (c) 2018-2022, KNOPFLERFISH project
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

package org.knopflerfish.bundle.datastorage_test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import junit.framework.Assert;
import junit.framework.TestSuite;

import com.google.gson.reflect.TypeToken;

import org.knopflerfish.service.datastorage.DataStorageNode;
import org.knopflerfish.service.datastorage.DataStorageService;
import org.knopflerfish.service.datastorage.JsonStorage;
import org.knopflerfish.service.datastorage.JsonStorageNode;
import org.knopflerfish.service.datastorage.JsonGenericStorageNode;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class DatastorageTestSuite
  extends TestSuite
{
  Bundle bu;
  BundleContext bc;
  ServiceReference<DataStorageService> dataStorageSR;
  DataStorageService storageService;

  public DatastorageTestSuite(BundleContext bc) {
    super ("DataStorageTestSuite");

    this.bc = bc;
    this.bu = bc.getBundle();

    addTest(new Setup());
    addTest(new DataStorageTestCreateRead());
    addTest(new DataStorageNodeTests());
    addTest(new JsonNodeTestsCreate());
    addTest(new JsonNodeTestsRead());
    addTest(new JsonNodeTestsListOfDTO());
    addTest(new Cleanup());
  }


  // Get the Preferences Service.
  class Setup extends FWTestCase {
    public void runTest() {
      assertNotNull("No bundle context...", bc);

      dataStorageSR =   bc.getServiceReference(DataStorageService.class);
      assertNotNull("No datastorage service reference available", dataStorageSR);

      storageService =  bc.getService(dataStorageSR);
      assertNotNull("No datastorage service reference available", storageService);
    }
  }

  // Unget the Preferences Service.
  class Cleanup extends FWTestCase {
    public void runTest() {
      storageService = null;
      if (dataStorageSR != null)
        bc.ungetService(dataStorageSR);
      dataStorageSR = null;
    }
  }

  public class DataStorageTestCreateRead extends FWTestCase {

    public void runTest() {
      assertNotNull(storageService);

      final JsonStorage jsonStorage = storageService.getJsonStorage();
      assertNotNull("User prefs tree 'parent'", jsonStorage);

      JsonStorageNode<SimpleDTO> jsonnode = jsonStorage.getNode("/foo", SimpleDTO.class);
      assertNotNull(jsonnode);
      
      SimpleDTO testDTO = new SimpleDTO();
      testDTO.id = 10;
      testDTO.value = "knopfler";
      
      jsonnode.put(testDTO);
      
      SimpleDTO t2 = jsonnode.get();
      assertEquals(testDTO.id, t2.id);
      assertEquals(testDTO.value, t2.value);
    }
  }

  public class DataStorageNodeTests extends FWTestCase {

    public void runTest() {
      assertNotNull(storageService);

      final JsonStorage jsonStorage = storageService.getJsonStorage();
      assertNotNull("No JsonStorage", jsonStorage);

      JsonStorageNode<EmptyDTO> jn1 = jsonStorage.getNode("/foo/bar", EmptyDTO.class);
      assertNotNull(jn1);
      
      JsonStorageNode<SimpleDTO> jn2 = jsonStorage.getNode("/foo/bar/fish", SimpleDTO.class);
      assertNotNull(jn2);

      JsonStorageNode<SimpleDTO> jn3 = jsonStorage.getNode("/foo/bar/knopfler", SimpleDTO.class);
      assertNotNull(jn3);
      
      assertEquals("knopfler", jn3.getName());
      assertEquals("/foo/bar/knopfler", jn3.getAbsolutePath());
      assertEquals("bar", jn3.getParent().getName());
      
      SimpleDTO testDTO = new SimpleDTO();
      testDTO.id = 30;
      testDTO.value = "makewave";
      jn3.put(testDTO);
      
      SimpleDTO t2 = jn3.get();
      assertEquals(testDTO.id, t2.id);
      assertEquals(testDTO.value, t2.value);
      
      // Test for children and removing them
      HashMap<String, String> expectedChildren = new HashMap<>();
      expectedChildren.put("fish", "fish");
      expectedChildren.put("knopfler", "knopfler");
      testChildren(jn1, expectedChildren);
      
      jsonStorage.removeNode("/foo/bar/knopfler");
      expectedChildren.remove("knopfler");
      testChildren(jn1, expectedChildren);
      
      jsonStorage.removeNode("/foo/bar");
      assertFalse(jsonStorage.nodeExists("/foo/bar"));
      assertFalse(jsonStorage.nodeExists("/foo/bar/fish"));
      
    }
  }
  
  public class JsonNodeTestsCreate extends FWTestCase {

    public void runTest() {
      assertNotNull(storageService);
      
      final JsonStorage jsonStorage = storageService.getJsonStorage();
      assertNotNull("No JsonStorage", jsonStorage);

      JsonStorageNode<EmptyDTO> jn1 = jsonStorage.getNode("/foo/bar", EmptyDTO.class);
      assertNotNull(jn1);
      
      JsonStorageNode<SimpleDTO> jn2 = jsonStorage.getNode("/foo/bar2/simple", SimpleDTO.class);
      assertNotNull(jn2);

      JsonStorageNode<GenericDTO> jn3 = jsonStorage.getNode("/foo/bar2/generic", GenericDTO.class);
      assertNotNull(jn3);
      
      SimpleDTO testDTO = new SimpleDTO();
      testDTO.id = 2017;
      testDTO.value = "simple";
      jn2.put(testDTO);
      
      GenericDTO genericDTO = new GenericDTO();
      genericDTO.id = 1999;
      genericDTO.mappings = new HashMap<>();
      genericDTO.mappings.put("alfa", 1);
      genericDTO.desc = "A little generic DTO";
      
    
      
      jn3.put(genericDTO);
      //  genericDTO, new TypeToken<GenericDTO>() {}.getType());
    }
    
    
  }
  
  public class JsonNodeTestsRead extends FWTestCase {

    public void runTest() {
      assertNotNull(storageService);
      
      final JsonStorage jsonStorage = storageService.getJsonStorage();
      assertNotNull("No JsonStorage", jsonStorage);

      JsonStorageNode<SimpleDTO> jn2 = jsonStorage.getNode("/foo/bar2/simple", SimpleDTO.class);
      assertNotNull(jn2);

      JsonStorageNode<GenericDTO> jn3 = jsonStorage.getNode("/foo/bar2/generic", GenericDTO.class);
      assertNotNull(jn3);
      
      SimpleDTO testDTO = jn2.get();
//      SimpleDTO testDTO = jn2.get(SimpleDTO.class);
      assertEquals(2017, testDTO.id);
      assertEquals("simple", testDTO.value);
      
      GenericDTO genericDTO;
      genericDTO = jn3.get();
      
      assertEquals(1999, genericDTO.id);
      assertEquals("A little generic DTO", genericDTO.desc);
      assertEquals(genericDTO.mappings.get("alfa"), new Integer(1));
      
    }
  }
  
  public class JsonNodeTestsListOfDTO extends FWTestCase {

    public void runTest() {
      assertNotNull(storageService);
      
      final JsonStorage jsonStorage = storageService.getJsonStorage();
      assertNotNull("No JsonStorage", jsonStorage);
      
      Type dtoListType = new TypeToken<ArrayList<SimpleDTO>>() {}.getType();
      
      JsonGenericStorageNode<ArrayList<SimpleDTO>> jnode = jsonStorage.getNode("/foo/dtolist", dtoListType);
      assertNotNull(jnode);

      ArrayList<SimpleDTO> dtolist = new ArrayList<>();
      
      for (int i = 0; i < 5; i++) {
        dtolist.add(getSimpleDTO(i, "value - " + i));
      }

      jnode.put(dtolist);
      
      ArrayList<SimpleDTO> retlist = jnode.get();
      for (SimpleDTO dto : retlist) {
        System.out.println(dto.id + " " + dto.value);
      }
    }
  }
  
  private void testChildren(DataStorageNode node, HashMap<String, String> expectedChildren) {
    
    Collection<String> children = node.childNames();
    int count = 0;
    
    for (String childname : children) {
      count++;
      System.out.println("Checking child: " + childname);
      Assert.assertTrue("Expected child to be present: " + childname, expectedChildren.containsKey(childname));
    }
    Assert.assertEquals("Expected children does not match", count, expectedChildren.size());
  }
  
  private static SimpleDTO getSimpleDTO(int id, String value) {
    SimpleDTO o = new SimpleDTO();
    o.id = id;
    o.value = value;
    return o;
  }
}

class SimpleDTO {
  public int id;
  public String value;
}

class GenericDTO {
  public int id;
  public HashMap<String, Integer> mappings;
  public String desc;
}

class EmptyDTO {
}

