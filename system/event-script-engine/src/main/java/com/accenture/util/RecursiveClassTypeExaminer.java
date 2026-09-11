/*

    Copyright 2018-2026 Accenture Technology

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 */

package com.accenture.util;

import org.objectweb.asm.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Collects every class a class file references — superclass, interfaces, field and
 * method signatures, declared exceptions, AND method-body references (call and field
 * owners, new/cast/instanceof types, lambda and method-reference handles, class
 * literals, catch types) — so the SimplePluginLoader's allowlist gate validates what
 * a plugin actually does, not just its shape.
 */
public class RecursiveClassTypeExaminer extends ClassVisitor {

    private final Set<String> types = new HashSet<>();
    private String superClass;
    private final Set<String> interfaces = new HashSet<>();

    public RecursiveClassTypeExaminer() {
        super(Opcodes.ASM9);
    }

    public Set<String> getTypes() {
        return types;
    }

    public String getSuperClass() {
        return superClass;
    }

    public Set<String> getInterfaces() {
        return interfaces;
    }

    private void addType(Type type) {
        if (type.getSort() == Type.OBJECT) {
            types.add(type.getClassName());
        } else if (type.getSort() == Type.ARRAY) {
            addType(type.getElementType());
        }
    }

    private void addInternalName(String internalName) {
        if (internalName != null) {
            if (internalName.charAt(0) == '[') {
                // an array owner, e.g. String[].clone()
                addType(Type.getType(internalName));
            } else {
                types.add(internalName.replace('/', '.'));
            }
        }
    }

    private void addMethodDescriptor(String descriptor) {
        Type methodType = Type.getMethodType(descriptor);
        addType(methodType.getReturnType());
        for (Type argType : methodType.getArgumentTypes()) {
            addType(argType);
        }
    }

    private void addHandle(Handle handle) {
        addInternalName(handle.getOwner());
        String descriptor = handle.getDesc();
        if (descriptor.startsWith("(")) {
            addMethodDescriptor(descriptor);
        } else {
            addType(Type.getType(descriptor));
        }
    }

    private void addConstant(Object value) {
        if (value instanceof Type type) {
            addType(type);
        } else if (value instanceof Handle handle) {
            addHandle(handle);
        } else if (value instanceof ConstantDynamic constant) {
            addHandle(constant.getBootstrapMethod());
            for (int i = 0; i < constant.getBootstrapMethodArgumentCount(); i++) {
                addConstant(constant.getBootstrapMethodArgument(i));
            }
        }
    }

    @Override
    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaceNames) {
        if (superName != null && !superName.equals("java/lang/Object")) {
            superClass = superName.replace('/', '.');
            types.add(superClass);
        }

        if (interfaceNames != null) {
            for (String iFace : interfaceNames) {
                String iFaceName = iFace.replace('/', '.');
                interfaces.add(iFaceName);
                types.add(iFaceName);
            }
        }
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor,
                                   String signature, Object value) {
        addType(Type.getType(descriptor));
        return null;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                     String signature, String[] exceptions) {
        addMethodDescriptor(descriptor);
        if (exceptions != null) {
            for (String exception : exceptions) {
                addInternalName(exception);
            }
        }
        // a real MethodVisitor: body references count toward the allowlist gate
        return new MethodBodyTypeExaminer();
    }

    private class MethodBodyTypeExaminer extends MethodVisitor {

        private MethodBodyTypeExaminer() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            // NEW, ANEWARRAY, CHECKCAST, INSTANCEOF
            addInternalName(type);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            addInternalName(owner);
            addType(Type.getType(descriptor));
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
                                    boolean isInterface) {
            addInternalName(owner);
            addMethodDescriptor(descriptor);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
                                           Object... bootstrapMethodArguments) {
            // lambdas and method references: the bootstrap plus every handle argument
            addMethodDescriptor(descriptor);
            addHandle(bootstrapMethodHandle);
            for (Object argument : bootstrapMethodArguments) {
                addConstant(argument);
            }
        }

        @Override
        public void visitLdcInsn(Object value) {
            // class literals and constant method handles
            addConstant(value);
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
            addType(Type.getType(descriptor));
        }

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            // type is null for a finally block
            addInternalName(type);
        }
    }
}
