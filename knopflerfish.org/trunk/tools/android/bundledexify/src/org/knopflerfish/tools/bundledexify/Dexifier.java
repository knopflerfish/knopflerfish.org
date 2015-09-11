/*
 * Copyright (c) 2015-2015, KNOPFLERFISH project
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
package org.knopflerfish.tools.bundledexify;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import com.android.dx.cf.direct.DirectClassFile;
import com.android.dx.cf.direct.StdAttributeFactory;
import com.android.dx.cf.iface.ParseException;
import com.android.dx.dex.DexOptions;
import com.android.dx.dex.cf.CfOptions;
import com.android.dx.dex.cf.CfTranslator;
import com.android.dx.dex.file.ClassDefItem;
import com.android.dx.dex.file.DexFile;

public class Dexifier {

  public static final String VERSION = "1.0";

  private static final String BUNDLE_CLASS_PATH = "Bundle-ClassPath";
  private static final String BUNDLE_DEXIFY_PATH = "Bundle-Dexify-Path";
  private static final String REQUIRE_CAPABILITY = "Require-Capability";
  private static final String OSGI_EE_REQUIREMENT = "osgi.ee; filter:=\"";
  private static final String DEFAULT_EE_REQUIREMENT = "(osgi.ee=OSGiMinimum)";
  private static final String CLASS_SUFFIX = ".class";
  private static final String JAR_SUFFIX = ".jar";
  private static final String CLASSES_DEX = "classes.dex";

  private static final int MAX_NUMBER_OF_IDX_PER_DEX = 65000;

  /* Array.newInstance may be added by RopperMachine,
   * ArrayIndexOutOfBoundsException.<init> may be added by EscapeAnalysis */
  private static final int MAX_METHOD_ADDED_DURING_DEX_CREATION = 2;

  /* <primitive types box class>.TYPE */
  private static final int MAX_FIELD_ADDED_DURING_DEX_CREATION = 9;

  /* force creation even if input is up-to-date */
  private boolean force = false;
  /* keep class files in jar */
  private boolean keepClassFiles = false;
  /* keep directory entries in jar */
  private boolean keepDirectories = false;
  /* replace input file with output */
  private boolean replaceOutput = false;
  /* Output files */
  private Hashtable<String,File> output = new Hashtable<String,File>();
  /* Destination directory for output */
  private File destDir = null;
  /* output info */
  private boolean verbose = false;
  /* DEX compile options */
  private CfOptions cfOptions = new CfOptions();
  private DexOptions dexOptions = new DexOptions();


  public File dexify(String bundle) throws IOException {
    File inputFile = new File(bundle);
    File outputFile = getOutput(bundle);
    boolean ok = false;
    info("Processing: " + bundle);
    try {
      JarFile jar = new JarFile(bundle);
      if (isForce() || !upToDate(outputFile, inputFile, jar)) {
        Manifest m = updateManifest(jar.getManifest());
        Set<String> cp = getBundleClasspath(m);
        JarOutputStream outJar = new JarOutputStream(new FileOutputStream(outputFile), m);
        DexFile dexFile = getDexFile();
        for (Enumeration<JarEntry> eje = jar.entries(); eje.hasMoreElements(); ) {
          JarEntry je = eje.nextElement();
          byte [] bytes = null;
          String name = je.getName();
          if (name.endsWith(JAR_SUFFIX)) {
            for (String p : cp) {
              if (p.equals(name)) {
                bytes = dexifyJar(new JarInputStream(jar.getInputStream(je)));
              }
            }
          }
          if (keepEntry(name)) {
            if (bytes == null) {
              bytes  = getEntryBytes(jar, je);
            }
            saveEntry(name, bytes, outJar);
          }
          if (name.endsWith(CLASS_SUFFIX)) {
            String cname = null;
            for (String p : cp) {
              if (p.equals(".") || p.equals("/")) {
                cname  = name;
                break;
              } else if (name.startsWith(p)) {
                cname = name.substring(p.length());
              }
            }
            if (cname != null) {
              if (bytes == null) {
                bytes = getEntryBytes(jar, je);
              }
              addToDexFile(dexFile, cname, bytes);
            }
          }
        }
        saveDexFile(dexFile, 1, outJar);
        outJar.close();
        finalizeOutput(bundle);
        info("Created: " + outputFile);
      } else {
        info("File up-to-date: " + bundle);
      }
      ok = true;
      return outputFile;
    } finally {
      if (!ok) {
        outputFile.delete();
      }
    }
  }

  public boolean isForce() {
    return force;
  }

  public void setForce(boolean force) {
    this.force = force;
  }

  public boolean isKeepClassFiles() {
    return keepClassFiles;
  }

  public void setKeepClassFiles(boolean keepClassFiles) {
    this.keepClassFiles = keepClassFiles;
  }

  public boolean isKeepDirectories() {
    return keepDirectories;
  }

  public void setKeepDirectories(boolean keepDirectories) {
    this.keepDirectories = keepDirectories;
  }

  public boolean isReplaceOutput() {
    return replaceOutput;
  }

  public void setReplaceOutput(boolean replaceOutput) {
    this.replaceOutput = replaceOutput;
  }

  public String getDestDir() {
    return destDir.toString();
  }

  public void setDestDir(String destDir) {
    this.destDir = new File(destDir);
    this.destDir.mkdirs();
  }

  public boolean isVerbose() {
    return verbose;
  }

  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  public int getApiLevel() {
    return dexOptions.targetApiLevel;
  }

  public void setApiLevel(int apiLevel) {
    dexOptions.targetApiLevel = apiLevel;
  }

  private byte[] dexifyJar(JarInputStream ji) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    JarOutputStream outJar = new JarOutputStream(baos);
    DexFile dexFile = getDexFile();
    for (JarEntry je = ji.getNextJarEntry(); je != null; je = ji.getNextJarEntry()) {
      byte [] bytes = null;
      String name = je.getName();
      if (keepEntry(name)) {
        bytes  = getBytes(ji, (int) je.getSize());
        saveEntry(name, bytes, outJar);
      }
      if (name.endsWith(CLASS_SUFFIX)) {
        if (bytes == null) {
          bytes  = getBytes(ji, (int) je.getSize());
        }
        addToDexFile(dexFile, name, bytes);
      } else if (!force && name.equals(CLASSES_DEX)) {
        // Jar already dexified
        return null;
      }
    }
    saveDexFile(dexFile, 1, outJar);
    outJar.close();
    return baos.toByteArray();
  }

  private Set<String> getBundleClasspath(Manifest m) {
    Attributes mainAttributes = m.getMainAttributes();
    String p = mainAttributes.getValue(BUNDLE_DEXIFY_PATH);
    if (p == null) {
      p = mainAttributes.getValue(BUNDLE_CLASS_PATH);
    }
    if (p == null) {
      p = ".";
    }
    Set<String> res = new HashSet<String>();
    for (String e : p.split(",")) {
      res.add(e.trim());
    }
    return res;
  }

  private void addToDexFile(DexFile dexFile, String name, byte[] bytes) {
    DirectClassFile cf = new DirectClassFile(bytes, name, cfOptions.strictNameCheck);
    cf.setAttributeFactory(StdAttributeFactory.THE_ONE);
    cf.getMagic();

    int numMethodIds = dexFile.getMethodIds().items().size();
    int numFieldIds = dexFile.getFieldIds().items().size();
    int constantPoolSize = cf.getConstantPool().size();

    int maxMethodIdsInDex = numMethodIds + constantPoolSize + cf.getMethods().size() +
        MAX_METHOD_ADDED_DURING_DEX_CREATION;
    int maxFieldIdsInDex = numFieldIds + constantPoolSize + cf.getFields().size() +
        MAX_FIELD_ADDED_DURING_DEX_CREATION;

    if ((dexFile.getClassDefs().items().size() > 0)
        && ((maxMethodIdsInDex > MAX_NUMBER_OF_IDX_PER_DEX) ||
            (maxFieldIdsInDex > MAX_NUMBER_OF_IDX_PER_DEX))) {
      throw new RuntimeException("TODO multi dex!");
    }

    try {
      ClassDefItem clazz = CfTranslator.translate(cf, bytes, cfOptions , dexOptions, dexFile);
      synchronized (dexFile) {
        dexFile.add(clazz);
      }
    } catch (ParseException ex) {
      Main.exit("Parse error, " + name, ex);
    }
  }

  private void saveDexFile(DexFile dexFile, int idx, JarOutputStream outJar) throws IOException {
    if (!dexFile.isEmpty()) {
      String cn = "classes" + (idx > 1 ? Integer.toString(idx) : "") + ".dex";
      ZipEntry je = new JarEntry(cn);
      outJar.putNextEntry(je);
      dexFile.writeTo(outJar, null, false);
      outJar.closeEntry();
    }
  }

  private DexFile getDexFile() {
    return new DexFile(dexOptions);
  }

  private void saveEntry(String name, byte[] bytes, JarOutputStream outJar) throws IOException {
    ZipEntry je = new JarEntry(name);
    outJar.putNextEntry(je);
    outJar.write(bytes);
    outJar.closeEntry();
  }

  private byte[] getEntryBytes(JarFile jar, JarEntry je) throws IOException {
    InputStream is = jar.getInputStream(je);
    try {
      return getBytes(is, (int)je.getSize());
    } finally {
      is.close();
    }
  }

  private byte[] getBytes(InputStream is, int ilen) throws IOException {
    byte[] bytes;
    if (ilen >= 0) {
      bytes = new byte[(int)ilen];
      final DataInputStream dis = new DataInputStream(is);
      dis.readFully(bytes);
    } else {
      bytes = new byte[0];
      final byte[] tmp = new byte[81920];
      int len;
      while ((len = is.read(tmp)) > 0) {
        final byte[] oldbytes = bytes;
        bytes = new byte[oldbytes.length + len];
        System.arraycopy(oldbytes, 0, bytes, 0, oldbytes.length);
        System.arraycopy(tmp, 0, bytes, oldbytes.length, len);
      }
    }
    return bytes;
  }

  private boolean keepEntry(String name) {
    if (name.equalsIgnoreCase("META-INF/MANIFEST.MF")) {
      return false;
    }
    if (name.equals(CLASSES_DEX)) {
      return false;
    }
    if (name.endsWith(CLASS_SUFFIX)) {
      return isKeepClassFiles();
    }
    if (name.endsWith("/")) {
      return isKeepDirectories();
    }
    return true;
  }

  private Manifest updateManifest(Manifest manifest) {
    Attributes mainAttributes = manifest.getMainAttributes();
    String reqs = mainAttributes.getValue(REQUIRE_CAPABILITY);
    reqs = getAndroidRequirement(reqs);
    mainAttributes.putValue(REQUIRE_CAPABILITY, reqs);
    Manifest res = new Manifest();
    res.getMainAttributes().putAll(mainAttributes);
    return res;
  }

  private String getAndroidRequirement(String reqs) {
    String ar = "(&(osgi.ee=Android)(version>=" + dexOptions.targetApiLevel + "))";
    String eeReq = extractEERequirementFilter(reqs);
    if (eeReq != null && eeReq.contains("Android")) {
      // We assume we want to keep it if already contains Android
      return reqs;
    } else {
      if (isKeepClassFiles()) {
        if (eeReq == null) {
          return (reqs != null ? reqs + "," : "") + OSGI_EE_REQUIREMENT  + "(&" + ar + DEFAULT_EE_REQUIREMENT + ")\"";
        } else {
          return reqs.replace(eeReq, "(|" + eeReq + ar + ")");
        }
      } else {
        if (eeReq == null) {
          return (reqs != null ? reqs + "," : "") + OSGI_EE_REQUIREMENT  + ar + "\"";
        } else {
          return reqs.replace(eeReq, ar);
        }
      }
    }
  }

  private String extractEERequirementFilter(String reqs) {
    if (reqs != null) {
      int i = reqs.indexOf(OSGI_EE_REQUIREMENT);
      if (i != -1) {
        int start = i + OSGI_EE_REQUIREMENT.length();
        int end = reqs.indexOf('"', start);
        if (end != -1) {
          return reqs.substring(start, end);
        }
        throw new RuntimeException("Failed to parse " + REQUIRE_CAPABILITY + ": " + reqs);
      }
    }
    return null;
  }

  private void info(String msg) {
    if (isVerbose()) {
      System.out.println(msg);
    }
  }

  private boolean upToDate(File outputFile, File inputFile, JarFile jar) {
    if (outputFile.exists()) {
      return inputFile.lastModified() < outputFile.lastModified();
    } else if (isReplaceOutput()) {
      return jar.getEntry(CLASSES_DEX) != null;
    }
    return false;
  }

  private File getOutput(String bundle) throws IOException {
    File res;
    if (destDir == null) {
      if (isReplaceOutput()) {
        res = File.createTempFile("dexify", ".dexjar");
      } else {
        res = new File((bundle.endsWith(".jar") ? bundle.substring(0, bundle.length() - 4) : bundle) + ".dex.jar"); 
      }
    } else {
      res = new File(destDir, new File(bundle).getName());
    }
    output.put(bundle, res);
    return res;
  }

  private void finalizeOutput(String bundle) {
    File out = output.remove(bundle);
    assert(out != null);
    if (destDir == null && isReplaceOutput()) {
      out.renameTo(new File(bundle));
    }
  }

}