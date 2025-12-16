/*

    Copyright 2018-2025 Accenture Technology

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

package com.accenture.utils;

import org.objectweb.asm.*;

import java.util.HashSet;
import java.util.Set;

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

    @Override
    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaceNames) {
        if (superName != null && !superName.equals("java/lang/Object")) {
            superClass = superName.replace('/', '.');
            types.add(superClass);
        }

        if (interfaceNames != null) {
            for (String iface : interfaceNames) {
                String ifaceName = iface.replace('/', '.');
                interfaces.add(ifaceName);
                types.add(ifaceName);
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
        Type methodType = Type.getType(descriptor);
        addType(methodType.getReturnType());
        for (Type argType : methodType.getArgumentTypes()) {
            addType(argType);
        }
        return null;
    }
}
