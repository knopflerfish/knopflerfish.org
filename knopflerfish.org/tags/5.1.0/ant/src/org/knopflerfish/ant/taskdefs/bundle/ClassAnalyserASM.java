/*
 * Copyright (c) 2003-2010, KNOPFLERFISH project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
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

package org.knopflerfish.ant.taskdefs.bundle;


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;


/**
 * Visitor implementation that populates a BundlePackagesInfo object
 * with data about the given class.
 */
public class ClassAnalyserASM
  implements ClassVisitor
{

  /**
   * The name of the bundle activator interface on internal form.
   */
  final static private String BUNDLE_ACTIVATOR
    = "org/osgi/framework/BundleActivator";

  // The BundlePackagesInfo object to populate with data.
  final BundlePackagesInfo bpInfo;

  // The task using this object to provide logging functionality.
  final Task task;

  // The package of the current class.
  String currentPackage = null;

  // The File of the current class.
  File currentClassFile = null;


  public ClassAnalyserASM(final BundlePackagesInfo bpInfo,
                          final Task task)
  {
    this.bpInfo = bpInfo;
    this.task   = task;
  }

  public synchronized void analyseClass(File clsFile)
  {
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(clsFile);
      analyseClass(fis, clsFile.toString());
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (null!=fis) {
        try { fis.close(); } catch (Exception _ec) {}
      }
    }
  }

  public synchronized void analyseClass(InputStream clsIn, String fileName)
  {
    try {
      ClassReader cr = new ClassReader(clsIn);
      currentClassFile = new File(fileName);
      cr.accept(this, 0);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  protected void addReferencedType(final Type type)
  {
    switch (type.getSort()) {
    case Type.OBJECT:
      bpInfo.addReferencedClass(currentPackage, type.getInternalName());
      break;
    case Type.ARRAY:
      addReferencedType(type.getElementType());
      break;
    default:
    }
  }

  /*
   * Visits the header of the class.
   *
   * @param version the class version.
   * @param access the class's access flags (see {@link Opcodes}). This
   *        parameter also indicates if the class is deprecated.
   * @param name the internal name of the class (see
   *        {@link Type#getInternalName() getInternalName}).
   * @param signature the signature of this class. May be <tt>null</tt> if
   *        the class is not a generic one, and does not extend or implement
   *        generic classes or interfaces.
   * @param superName the internal of name of the super class (see
   *        {@link Type#getInternalName() getInternalName}). For interfaces,
   *        the super class is {@link Object}. May be <tt>null</tt>, but
   *        only for the {@link Object} class.
   * @param interfaces the internal names of the class's interfaces (see
   *        {@link Type#getInternalName() getInternalName}). May be
   *        <tt>null</tt>.
   */
  public void visit(int version,
                    int access,
                    String name,
                    String signature,
                    String superName,
                    String[] interfaces)
  {
    task.log("Analysing class '" +name +"'.", Project.MSG_VERBOSE);

    currentPackage = bpInfo.addProvidedClass(name);
    if (null!=superName) {
      addReferencedType(Type.getObjectType(superName));
    }
    for(int i = 0; i < interfaces.length; i++) {
      addReferencedType(Type.getObjectType(interfaces[i]));
      if (BUNDLE_ACTIVATOR.equals(interfaces[i])) {
        bpInfo.addProvidedActivatorClass(name);
      }
    }
  }


  /*
   * Visits the source of the class.
   *
   * @param source the name of the source file from which the class was
   *        compiled. May be <tt>null</tt>.
   * @param debug additional debug information to compute the correspondence
   *        between source and compiled elements of the class. May be
   *        <tt>null</tt>.
   */
  public void visitSource(String source, String debug)
  {
  }


  /*
   * Visits the enclosing class of the class. This method must be called only
   * if the class has an enclosing class.
   *
   * @param owner internal name of the enclosing class of the class.
   * @param name the name of the method that contains the class, or
   *        <tt>null</tt> if the class is not enclosed in a method of its
   *        enclosing class.
   * @param desc the descriptor of the method that contains the class, or
   *        <tt>null</tt> if the class is not enclosed in a method of its
   *        enclosing class.
   */
  public void visitOuterClass(String owner, String name, String desc)
  {
  }


  /*
   * Visits an annotation of the class.
   *
   * @param desc the class descriptor of the annotation class.
   * @param visible <tt>true</tt> if the annotation is visible at runtime.
   * @return a visitor to visit the annotation values, or <tt>null</tt> if
   *         this visitor is not interested in visiting this annotation.
   */
  public AnnotationVisitor visitAnnotation(String desc, boolean visible)
  {
    return null;
  }


  /*
   * Visits a non standard attribute of the class.
   *
   * @param attr an attribute.
   */
  public void visitAttribute(Attribute attr)
  {
  }


  /*
   * Visits information about an inner class. This inner class is not
   * necessarily a member of the class being visited.
   *
   * @param name the internal name of an inner class (see
   *        {@link Type#getInternalName() getInternalName}).
   * @param outerName the internal name of the class to which the inner class
   *        belongs (see {@link Type#getInternalName() getInternalName}). May
   *        be <tt>null</tt> for not member classes.
   * @param innerName the (simple) name of the inner class inside its
   *        enclosing class. May be <tt>null</tt> for anonymous inner
   *        classes.
   * @param access the access flags of the inner class as originally declared
   *        in the enclosing class.
   */
  public void visitInnerClass(String name,
                              String outerName,
                              String innerName,
                              int access)
  {
  }


  /*
   * Visits a field of the class.
   *
   * @param access the field's access flags (see {@link Opcodes}). This
   *        parameter also indicates if the field is synthetic and/or
   *        deprecated.
   * @param name the field's name.
   * @param desc the field's descriptor (see {@link Type Type}).
   * @param signature the field's signature. May be <tt>null</tt> if the
   *        field's type does not use generic types.
   * @param value the field's initial value. This parameter, which may be
   *        <tt>null</tt> if the field does not have an initial value, must
   *        be an {@link Integer}, a {@link Float}, a {@link Long}, a
   *        {@link Double} or a {@link String} (for <tt>int</tt>,
   *        <tt>float</tt>, <tt>long</tt> or <tt>String</tt> fields
   *        respectively). <i>This parameter is only used for static fields</i>.
   *        Its value is ignored for non static fields, which must be
   *        initialized through bytecode instructions in constructors or
   *        methods.
   * @return a visitor to visit field annotations and attributes, or
   *         <tt>null</tt> if this class visitor is not interested in
   *         visiting these annotations and attributes.
   */
  public FieldVisitor visitField(int access,
                                 String name,
                                 String desc,
                                 String signature,
                                 Object value)
  {
    addReferencedType(Type.getType(desc));

    return null;
  }


  /*
   * Visits a method of the class. This method <i>must</i> return a new
   * {@link MethodVisitor} instance (or <tt>null</tt>) each time it is
   * called, i.e., it should not return a previously returned visitor.
   *
   * @param access the method's access flags (see {@link Opcodes}). This
   *        parameter also indicates if the method is synthetic and/or
   *        deprecated.
   * @param name the method's name.
   * @param desc the method's descriptor (see {@link Type Type}).
   * @param signature the method's signature. May be <tt>null</tt> if the
   *        method parameters, return type and exceptions do not use generic
   *        types.
   * @param exceptions the internal names of the method's exception classes
   *        (see {@link Type#getInternalName() getInternalName}). May be
   *        <tt>null</tt>.
   * @return an object to visit the byte code of the method, or <tt>null</tt>
   *         if this class visitor is not interested in visiting the code of
   *         this method.
   */
  public MethodVisitor visitMethod(int access,
                                   String name,
                                   String desc,
                                   String signature,
                                   String[] exceptions)
  {
    Type[] argTypes = Type.getArgumentTypes(desc);
    for (int i=0; argTypes!=null && i<argTypes.length; i++) {
      addReferencedType(argTypes[i]);
    }
    addReferencedType(Type.getReturnType(desc));


    for (int i=0; exceptions!=null && i<exceptions.length; i++) {
      bpInfo.addReferencedClass(currentPackage, exceptions[i]);
    }

    return new MethodAnalyserASM(this, name);
  }


  /*
   * Visits the end of the class. This method, which is the last one to be
   * called, is used to inform the visitor that all the fields and methods of
   * the class have been visited.
   */
  public void visitEnd()
  {
  }

}
