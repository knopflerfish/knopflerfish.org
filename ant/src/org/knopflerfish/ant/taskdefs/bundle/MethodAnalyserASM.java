/*
 * Copyright (c) 2003-2018, KNOPFLERFISH project
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

import org.apache.tools.ant.Project;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;

/**
 * Visitor implementation that populates a BundlePackagesInfo object with data
 * about the method it visits.
 */
public class MethodAnalyserASM extends MethodVisitor {

  // The ClassAnalyserASM instance that this method analyzer is
  // belonging to.
  private final ClassAnalyserASM ca;

  private final String methodName;

  public MethodAnalyserASM(final ClassAnalyserASM ca, final String name) {
    super(Opcodes.ASM7);
    this.ca = ca;
    this.methodName = name;
  }

  @Override
  public void visitAnnotableParameterCount(int parameterCount, boolean visible) {
  }

  @Override
  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    ca.addReferencedType(Type.getType(descriptor));

    return null;
  }

  @Override
  public AnnotationVisitor visitAnnotationDefault() {
    return null;
  }

  @Override
  public void visitAttribute(Attribute attribute) {
  }

  @Override
  public void visitCode() {
    ca.task.log("  method '" + methodName + "'.", Project.MSG_DEBUG);
  }

  @Override
  public void visitEnd() {
  }

  @Override
  public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
    ca.addReferencedType(Type.getObjectType(owner));
    ca.addReferencedType(Type.getType(descriptor));
  }

  @Override
  public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
  }

  @Override
  public void visitIincInsn(int var, int increment) {
  }

  @Override
  public void visitInsn(int opcode) {
  }

  @Override
  public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
    ca.addReferencedType(Type.getType(descriptor));
    // TODO analyze annotation arguments.
    
    return null;
  }

  @Override
  public void visitIntInsn(int opcode, int operand) {
  }

  @Override
  public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
      Object... bootstrapMethodArguments) {
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
  }

  @Override
  public void visitLabel(Label label) {
  }

  @Override
  public void visitLdcInsn(Object value) {
  }

  @Override
  public void visitLineNumber(int line, Label start) {
  }

  @Override
  public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
    ca.addReferencedType(Type.getType(descriptor));
  }

  @Override
  public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end,
      int[] index, String descriptor, boolean visible) {
    return null;
  }

  @Override
  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
  }

  @Override
  public void visitMaxs(int maxStack, int maxLocals) {
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    ca.addReferencedType(Type.getObjectType(owner));

    Type[] argTypes = Type.getArgumentTypes(descriptor);
    for (int i = 0; argTypes != null && i < argTypes.length; i++) {
      ca.addReferencedType(argTypes[i]);
    }
    ca.addReferencedType(Type.getReturnType(descriptor));
  }

  @Override
  public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
    ca.addReferencedType(Type.getType(descriptor));
  }

  @Override
  public void visitParameter(String name, int access) {
  }

  @Override
  public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
    ca.addReferencedType(Type.getType(descriptor));
    // TODO analyze annotation arguments.
    
    return null;
  }

  @Override
  public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
  }

  @Override
  public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
    ca.addReferencedType(Type.getType(descriptor));
    // TODO analyze annotation arguments.
    
    return null;
  }

  @Override
  public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
    if (null != type) {
      ca.addReferencedType(Type.getObjectType(type));
    }
  }

  @Override
  public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
    ca.addReferencedType(Type.getType(descriptor));
    // TODO analyze annotation arguments.
    
    return null;
  }

  @Override
  public void visitTypeInsn(int opcode, String type) {
    ca.addReferencedType(Type.getObjectType(type));
  }

  @Override
  public void visitVarInsn(int opcode, int var) {
  }

}
