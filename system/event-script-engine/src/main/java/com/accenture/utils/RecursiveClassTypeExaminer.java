package com.accenture.utils;

import org.objectweb.asm.*;

import java.util.HashSet;
import java.util.Set;

public class RecursiveClassTypeExaminer extends ClassVisitor {

    private final Set<String> usedTypes = new HashSet<>();
    private final Set<String> processedClasses = new HashSet<>();
    private String currentClassName;

    protected RecursiveClassTypeExaminer(int api) {
        super(Opcodes.ASM9);
    }

    private void addType(String descriptor) {
        if (descriptor == null) return;

        Type type = Type.getType(descriptor);
        collectTypes(type);
    }

    private void collectTypes(Type type) {
        switch (type.getSort()) {
            case Type.OBJECT:
                usedTypes.add(type.getClassName());
                break;
            case Type.ARRAY:
                collectTypes(type.getElementType());
                break;
            case Type.METHOD:
                collectTypes(type.getReturnType());
                for (Type argType : type.getArgumentTypes()) {
                    collectTypes(argType);
                }
                break;
        }
    }

    private void processRecursively(String parent){
        if(processedClasses.contains(parent)){ // base case
            return;
        }

        processedClasses.add(parent);

        if(parent.startsWith("java.lang")){
            return;
        }

        //TBC
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces){
        if (superName != null && !superName.equals("java/lang/Object")) { // Keep checking the parent for types
            usedTypes.add(superName.replace('/', '.'));
        }

        if (interfaces != null) {
            for (String i : interfaces) {
                usedTypes.add(i.replace('/', '.'));
            }
        }

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        usedTypes.add(Type.getType(descriptor).getClassName());
        return super.visitField(access, name, descriptor, signature, value);
    }
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        Type methodType = Type.getMethodType(descriptor);
        usedTypes.add(methodType.getReturnType().getClassName());
        for (Type argType : methodType.getArgumentTypes()) {
            usedTypes.add(argType.getClassName());
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }
}
