/*
 * Copyright (c) 2003-2010, KNOPFLERFISH project
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


import java.util.Map;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import java.net.URL;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * This class contains byte code-manipulation functions for
 * patching loaded bundle classes with custom wrapper methods.
 *
 * <p>
 * It uses the ASM library (http://asm.objectweb.org)
 * </p>
 *
 * <p>
 * See the resource file /patches.props for a list of patched
 * methods. This file references code in ClassPatcherWrappers.
 * </p>
 *
 * @see ClassPatcherWrappers
 * @author Erik Wistrand
 */
public class ClassPatcher {

  // Map<BundleClassLoader,ClassPatcher>
  static protected Map               patchers = new HashMap();

  protected        BundleClassLoader classLoader;

  // Dictionary used for LDAP matching on which patches to apply
  // This dictionary will contain all manifest headers, plus
  // current class name, method name etc
  protected Hashtable matchProps = null;

  // property names used in LDAP matching
  public static final String PROP_CLASSNAME    = "classname";
  public static final String PROP_BID          = "bid";
  public static final String PROP_LOCATION     = "location";
  public static final String PROP_METHODNAME   = "methodname";
  public static final String PROP_METHODACCESS = "methodaccess";
  public static final String PROP_METHODDESC   = "methoddesc";

  protected LDAPExpr patchesFilter = null;
  protected boolean  bDumpClasses  = false;

  // MethodInfo -> MethodInfo
  // These are the (per bundle) method wrappers
  // to be applied.
  protected Map      wrappers      = new HashMap();

  FrameworkContext framework;
  protected ClassPatcher(BundleClassLoader classLoader) {
    this.classLoader = classLoader;
    this.framework = classLoader.bpkgs.bg.bundle.fwCtx;
    init();
  }

  static public ClassPatcher getInstance(BundleClassLoader classLoader) {
    synchronized(patchers) {
      ClassPatcher cp = (ClassPatcher)patchers.get(classLoader);
      if(cp == null) {
        cp = new ClassPatcher(classLoader);
        patchers.put(classLoader, cp);
      }
      return cp;
    }
  }


  protected void init() {
    bDumpClasses = framework.props.getBooleanProperty(FWProps.PATCH_DUMPCLASSES_PROP);
    String urlS = classLoader.archive.getAttribute("Bundle-ClassPatcher-Config");

    if(urlS == null || "".equals(urlS)) {
      urlS = framework.props.getProperty(FWProps.PATCH_CONFIGURL_PROP);
    } else if("none".equals(urlS)) {
      urlS = null;
    }

    makeMatchProps();
    if(urlS != null) {
      loadWrappers(urlS);
    }
  }

  // do the actual patching
  public byte[] patch(String className, byte[] classBytes) {

    if(wrappers.size() == 0) {
      return classBytes;
    }

    matchProps.put(PROP_CLASSNAME, className);

    if(patchesFilter != null) {
      boolean b = patchesFilter.evaluate(matchProps, false);
      if(!b) {
        return classBytes;
      }
    }

    try {
      ClassReader  cr    = new ClassReader(classBytes);
      ClassWriter  cw    = new BundleClassWriter(ClassWriter.COMPUTE_MAXS,
                                                 classLoader);
      ClassAdapter trans = new ClassAdapterPatcher(cw,
                                                   className.replace('.', '/'),
                                                   classLoader,
                                                   classLoader.archive.getBundleId(),
                                                   this);

      cr.accept(trans, 0);

      byte[] newBytes = cw.toByteArray();

      if(bDumpClasses) {
        dumpClassBytes(className, newBytes);
      }
      classBytes = newBytes;
    } catch (Exception e) {
      throw new RuntimeException("Failed to patch " + className + "/"
                                 + classLoader +": " +e);
    }
    return classBytes;
  }

  static String PRE    = "patch.";


  protected void loadWrappers(String urlS) {
    URL url = null;

    InputStream is = null;
    try {
      if(urlS.startsWith("!!")) {
        url = ClassPatcher.class.getResource(urlS.substring(2));
      } else if(urlS.startsWith("!")) {
        url = classLoader.getResource(urlS.substring(1));
      } else {
        url = new URL(urlS);
      }
      is = url.openStream();
      loadWrappersFromInputStream(is);
    } catch (Exception e) {
      framework.debug.printStackTrace("Failed to load patches conf from " + url, e);
    } finally {
      try { is.close(); } catch (Exception ignored) { }
    }
  }


  protected void loadWrappersFromInputStream(InputStream is) throws IOException {
    Properties props = new Properties();
    props.load(is);
    String f = (String)props.get("patches.filter");
    if(f != null) {
      try {
        patchesFilter = new LDAPExpr(f);
      } catch (Exception e) {
        framework.debug.printStackTrace("Failed to set patches filter", e);
      }
    }

    for(Iterator it = props.keySet().iterator(); it.hasNext(); ) {
      String key = (String)it.next();
      if(key.startsWith(PRE)) {
        int ix = key.lastIndexOf(".from");
        if(ix != -1) {
          String id = key.substring(PRE.length(), ix);
          String from = (String)props.get(PRE + id + ".from");
          String to   = (String)props.get(PRE + id + ".to");

          if(to == null) {
            if(framework.debug.patch) {
              framework.debug.println("No key=" + (PRE + id + ".to"));
            }
            continue;
          }

          addPatch(from,
                   to,
                   "true".equals(props.get(PRE + id + ".active")),
                   "true".equals(props.get(PRE + id + ".static")),
                   (String)props.get(PRE + id + ".filter")
                   );
        }
      }
    }
  }

  // Parse <owner>.<name><desc>
  static protected void parseSignature(String sig, String[] r) {
    int dotIx = sig.indexOf(".");
    if(dotIx == -1) {
      throw new IllegalArgumentException("No . in sig=" + sig);
    }
    int descIx = sig.indexOf("(");
    if(descIx == -1) {
      descIx = sig.length();
    }
    r[0] = sig.substring(0, dotIx).trim();
    r[1] = sig.substring(dotIx+1, descIx).trim();
    if(descIx < sig.length()) {
      r[2] = sig.substring(descIx).trim();
    } else {
      r[2] = null;
    }
  }

  protected void addPatch(String from,
                          String to,
                          boolean defActive,
                          boolean bStatic,
                          String filter) {
    String[] r = new String[3];

    parseSignature(from, r);
    String owner = r[0];
    String name  = r[1];
    String desc  = r[2];

    parseSignature(to, r);
    String targetOwner = r[0];
    String targetName = r[1];

    if (!classLoader.isBundlePatch()) {
      return;
    }
    String kpt = framework.props.getProperty("kf.patch." + targetName);
    if (kpt != null ? "false".equalsIgnoreCase(kpt) : !defActive) {
      return;
    }

    MethodInfo mi     = new MethodInfo(owner, name, desc, bStatic);
    if(filter != null) {
      try {
        mi.filter = new LDAPExpr(filter);
      } catch (Exception e) {
        framework.debug.printStackTrace("Bad filter for " + mi, e);
      }
    }
    int    ix0        = desc.lastIndexOf("(");
    int    ix1        = desc.lastIndexOf(")");
    String origArgs   = desc.substring(ix0+1, ix1);
    String retType    = desc.substring(ix1+1);
    String targetDesc;
    if(bStatic) {
      targetDesc = origArgs + "JLjava/lang/Object;";
    } else {
      targetDesc = "Ljava/lang/Object;" + origArgs + "JLjava/lang/Object;";
    }
    targetDesc = "(" + targetDesc + ")" + retType;

    MethodInfo target = new MethodInfo(targetOwner,
                                       targetName,
                                       targetDesc,
                                       false);

    target.key = mi;
    wrappers.put(mi, target);
  }

  protected void dumpInfo() {
    boolean bFirst = true;
    for(Iterator it = wrappers.keySet().iterator(); it.hasNext(); ) {
      MethodInfo from = (MethodInfo)it.next();
      MethodInfo mi   = (MethodInfo)wrappers.get(from);
      if(mi.nPatches > 0) {
        if(bFirst) {
          framework.debug.println("Patches in " + mi.className);
          bFirst = false;
        }
        framework.debug.println(" " + mi.nPatches + " " +
                                (mi.nPatches == 1 ? "occurance " : "occurances") +
                                " of " + from.owner + "." + from.name);
      }
      mi.nPatches = 0;
      mi.className = "";
    }
  }

  MethodInfo findMethodInfo(MethodInfo from) {
    MethodInfo mi = (MethodInfo)wrappers.get(from);
    return mi;
  }


  // Initialize matchProps member with manifest headers +
  // bundle location and id
  protected void makeMatchProps() {
    matchProps = new Hashtable();
    Dictionary d = classLoader.bpkgs.bg.bundle.getHeaders();

    for(Enumeration e = d.keys(); e.hasMoreElements(); ) {
      Object key = e.nextElement();
      Object val = d.get(key);
      matchProps.put(key, val);
    }

    matchProps.put(PROP_LOCATION,  classLoader.archive.getBundleLocation());
    matchProps.put(PROP_BID,       new Long(classLoader.archive.getBundleId()));
  }


  /**
   * Dump a byte array to a .class file
   */
  protected void dumpClassBytes(String className, byte[] classBytes) {
    OutputStream os = null;
    try {
      String dirName = framework.props.getProperty(FWProps.PATCH_DUMPCLASSES_DIR_PROP);
      File dir = new File(dirName);

      String classFileName = className.replace('.', '/');
      int ix = classFileName.lastIndexOf("/");
      File file;
      if(ix != -1) {
        String classDir = classFileName.substring(0, ix);
        classFileName = classFileName.substring(ix+1) + ".class";
        dir = new File(dir, classDir);
      }
      dir.mkdirs();
      file = new File(dir, classFileName);
      if(framework.debug.patch) {
        framework.debug.println("dump " + className + " to " + file.getAbsolutePath());
      }
      os = new FileOutputStream(file);
      os.write(classBytes);
      os.flush();
    } catch (Exception e) {
      framework.debug.printStackTrace("Failed to dump class=" + className, e);
    } finally {
      try { os.close(); } catch (Exception ignored) { }
    }
  }
}


class ClassAdapterPatcher extends ClassAdapter {
  BundleClassLoader bcl;
  long              bid;
  String            className;
  String            currentMethodName;
  String            currentMethodDesc;
  int               currentMethodAccess;
  String            currentSuperName;
  String[]          currentInterfaces;
  ClassPatcher      cp;

  // set to true by ReplaceMethodAdapter if BID field needs to be added
  boolean           bNeedBIDField = false;

  // magic name of bundle id field added to all processed classes.
  static final String FIELD_BID = "__kf_asm_bid_" + ClassPatcher.class.hashCode()+ "__";

  FrameworkContext framework;

  ClassAdapterPatcher(ClassVisitor      cv,
                      String            className,
                      BundleClassLoader bcl,
                      long              bid,
                      ClassPatcher      cp) {
    super(cv);
    this.className = className;
    this.bcl       = bcl;
    this.bid       = bid;
    this.cp        = cp;
    this.framework = bcl.bpkgs.bg.bundle.fwCtx;
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    currentSuperName  = superName;
    currentInterfaces = interfaces;
    super.visit(version, access, name, signature, superName, interfaces);
  }


  public void visitEnd() {
    if(bNeedBIDField) {
      // Add a static field containing the bundle id to the processed class.
      // This files is used by the wrapper methods to find the bundle owning
      // the class.
      // The field has the magic/long name defined by FIELD_BID
      // hopefully no-one else uses this
      super.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC,
                       FIELD_BID,
                       "J",
                       null,
                       new Long(bid));
    }

    super.visitEnd();

    if(bcl.bpkgs.bg.bundle.fwCtx.debug.patch) {
      cp.dumpInfo();
    }
  }

  public MethodVisitor visitMethod(int access,
                                   String name,
                                   String desc,
                                   String signature,
                                   String[] exceptions) {
    currentMethodName   = name;
    currentMethodDesc   = desc;
    currentMethodAccess = access;
    MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
    if (mv != null) {
      mv = new ReplaceMethodAdapter(mv, this);
    }
    return mv;
  }
}

class ReplaceMethodAdapter extends MethodAdapter implements Opcodes {
  ClassAdapterPatcher ca;
  FrameworkContext framework;

  public ReplaceMethodAdapter(MethodVisitor mv, ClassAdapterPatcher ca) {
    super(mv);
    this.ca = ca;
    this.framework = ca.framework;
  }

  public void visitMethodInsn(int opcode, String owner, String name, String desc) {
    MethodInfo from0 = new MethodInfo(owner, name, desc, false);
    MethodInfo mi    = ca.cp.findMethodInfo(from0);

    if(mi != null) {
      MethodInfo from = mi.key;

      if(from.filter != null) {
        ca.cp.matchProps.put(ClassPatcher.PROP_METHODNAME, ca.currentMethodName);
        ca.cp.matchProps.put(ClassPatcher.PROP_METHODDESC, ca.currentMethodDesc);
        ca.cp.matchProps.put(ClassPatcher.PROP_METHODACCESS, new Integer(ca.currentMethodAccess));
        if(!from.filter.evaluate(ca.cp.matchProps, false)) {
          super.visitMethodInsn(opcode, owner, name, desc);
          return;
        }
      }

      if(framework.debug.patch) {
        framework.debug.println("patch " + ca.className + "/" + ca.currentSuperName
                                + "." + ca.currentMethodName + "\n "
                                + from0.owner + "." + from.name + "\n "
                                + mi.owner + "." + mi.name);
      }
      wrapMethod(mi);
    } else {
      super.visitMethodInsn(opcode, owner, name, desc);
    }
  }

  void wrapMethod(MethodInfo mi) {

    // We really need the BID field, so tell
    // the ClassAdapterPatcher that this is needed.
    ca.bNeedBIDField = true;

    // push the bundle id on the stack since the wrapper expects it
    super.visitFieldInsn(GETSTATIC,
                         ca.className,
                         ClassAdapterPatcher.FIELD_BID,
                         "J");

    if((ca.currentMethodAccess & ACC_STATIC) != 0) {
      // push NULL context object if it's a static context
      super.visitInsn(ACONST_NULL);
    } else {
      // otherwise push context object
      super.visitVarInsn(ALOAD, 0);
    }

    // call wrapper method
    super.visitMethodInsn(INVOKESTATIC, mi.owner, mi.name, mi.desc);

    // some housekeeping for statistics purposes
    mi.className = ca.className;
    mi.nPatches++;
  }
}

/*
      // If we have a virtual match, add code the verifies that
      // the top stack value is an instance of cName.
      // If it is, wrap the call, otherwise keep original.
      //
      // We cannot do the instanceof check at patch time since
      // not all classes are loaded yet and we would quite likely
      // run into loops causing ClassCircularityError
      if(false && from.bVirtual) {
        String cName = from.owner;

        // Test if caller is instanceof cName
        super.visitInsn(DUP);
        super.visitTypeInsn(INSTANCEOF, cName);

        // if not, keep original call
        Label label_NotcName = new Label();
        super.visitJumpInsn(IFEQ, label_NotcName);

        // caller was instance of cName, rewrite using wrapper
        wrapMethod(mi);

        // ...and jump out
        Label label_End = new Label();
        super.visitJumpInsn(GOTO, label_End);

        // caller is not instance of cName, keep original invoke
        super.visitLabel(label_NotcName);
        super.visitMethodInsn(opcode, owner, name, desc);

        // End of instanceof check
        super.visitLabel(label_End);
      } else {
*/

/**
 * Utility class to hold and match on method names.
 */
class MethodInfo {
  String     owner;
  String     name;
  String     desc;
  boolean    bStatic;
  int        nPatches;
  String     className = "";
  MethodInfo key;
  LDAPExpr   filter;

  MethodInfo(String owner,
             String name,
             String desc,
             boolean bStatic) {
    this.owner    = owner;
    this.name     = name;
    this.desc     = desc;
    this.bStatic  = bStatic;
  }

  public int hashCode() {
    return owner.hashCode() + 17 * name.hashCode() + 61 * desc.hashCode();
  }

  public boolean equals(Object obj) {
    if(!(obj instanceof MethodInfo)) {
      return false;
    }
    MethodInfo mi = (MethodInfo)obj;
    return
      owner.equals(mi.owner) &&
      name.equals(mi.name) &&
      desc.equals(mi.desc);
  }

  public String toString() {
    return "MethodInfo[" +
      "owner=" + owner +
      ", name=" + name +
      ", desc=" + desc +
      ", bStatic=" + bStatic +
      ", filter= " + filter +
      "]";
  }
}

/**
 * Custom class writer using a specified class loader for
 * the getCommonSuperClass method.
 */
class BundleClassWriter extends ClassWriter {
  protected ClassLoader classLoader;

  public BundleClassWriter(int flags, ClassLoader classLoader) {
    super(flags);
    this.classLoader = classLoader;
  }

  // The original ASM implementation used the system class loader.
  // This code uses the specified class loader instead. Otherwise
  // it's copied from org.objectweb.asm.ClassWriter.java
  protected String getCommonSuperClass(final String type1, final String type2)
  {
    Class c, d;
    try {
      c = Class.forName(type1.replace('/', '.'), true, classLoader);
      d = Class.forName(type2.replace('/', '.'), true, classLoader);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e.toString());
    }
    if (c.isAssignableFrom(d)) {
      return type1;
    }
    if (d.isAssignableFrom(c)) {
      return type2;
    }
    if (c.isInterface() || d.isInterface()) {
      return "java/lang/Object";
    } else {
      do {
        c = c.getSuperclass();
      } while (!c.isAssignableFrom(d));
      return c.getName().replace('.', '/');
    }
  }
}
