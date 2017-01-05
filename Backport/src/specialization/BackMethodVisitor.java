package specialization;


import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import rt.Opcodes;

import java.util.*;

/**
 *
 * Created by Jefferson Mangue on 12/06/2016.
 */
class BackMethodVisitor extends MethodVisitor {

    // SimpleEntry needed explicitly for java 7.
    private static final HashMap<Integer, List<AbstractMap.SimpleEntry<String, Integer>>> INSTRS = new HashMap<>();

    static {
        INSTRS.put(Opcodes.ARETURN, Arrays.asList(
                new AbstractMap.SimpleEntry<>("I", Opcodes.IRETURN),
                new AbstractMap.SimpleEntry<>("B", Opcodes.IRETURN),
                new AbstractMap.SimpleEntry<>("S", Opcodes.IRETURN),
                new AbstractMap.SimpleEntry<>("C", Opcodes.IRETURN),
                new AbstractMap.SimpleEntry<>("Z", Opcodes.IRETURN),
                new AbstractMap.SimpleEntry<>("J", Opcodes.LRETURN),
                new AbstractMap.SimpleEntry<>("F", Opcodes.FRETURN),
                new AbstractMap.SimpleEntry<>("D", Opcodes.DRETURN)));
        INSTRS.put(Opcodes.ALOAD, Arrays.asList(
                new AbstractMap.SimpleEntry<>("I", Opcodes.ILOAD),
                new AbstractMap.SimpleEntry<>("B", Opcodes.ILOAD),
                new AbstractMap.SimpleEntry<>("S", Opcodes.ILOAD),
                new AbstractMap.SimpleEntry<>("C", Opcodes.ILOAD),
                new AbstractMap.SimpleEntry<>("Z", Opcodes.ILOAD),
                new AbstractMap.SimpleEntry<>("J", Opcodes.LLOAD),
                new AbstractMap.SimpleEntry<>("F", Opcodes.FLOAD),
                new AbstractMap.SimpleEntry<>("D", Opcodes.DLOAD)));
        INSTRS.put(Opcodes.ASTORE, Arrays.asList(
                new AbstractMap.SimpleEntry<>("I", Opcodes.ISTORE),
                new AbstractMap.SimpleEntry<>("B", Opcodes.ISTORE),
                new AbstractMap.SimpleEntry<>("S", Opcodes.ISTORE),
                new AbstractMap.SimpleEntry<>("C", Opcodes.ISTORE),
                new AbstractMap.SimpleEntry<>("Z", Opcodes.ISTORE),
                new AbstractMap.SimpleEntry<>("J", Opcodes.LSTORE),
                new AbstractMap.SimpleEntry<>("F", Opcodes.FSTORE),
                new AbstractMap.SimpleEntry<>("D", Opcodes.DSTORE)));
        INSTRS.put(Opcodes.AALOAD, Arrays.asList(
                new AbstractMap.SimpleEntry<>("I", Opcodes.IALOAD),
                new AbstractMap.SimpleEntry<>("B", Opcodes.IALOAD),
                new AbstractMap.SimpleEntry<>("S", Opcodes.IALOAD),
                new AbstractMap.SimpleEntry<>("C", Opcodes.IALOAD),
                new AbstractMap.SimpleEntry<>("Z", Opcodes.IALOAD),
                new AbstractMap.SimpleEntry<>("J", Opcodes.LALOAD),
                new AbstractMap.SimpleEntry<>("F", Opcodes.FALOAD),
                new AbstractMap.SimpleEntry<>("D", Opcodes.DALOAD)));
        INSTRS.put(Opcodes.DUP, Arrays.asList(
                new AbstractMap.SimpleEntry<>("I", Opcodes.DUP),
                new AbstractMap.SimpleEntry<>("B", Opcodes.DUP),
                new AbstractMap.SimpleEntry<>("S", Opcodes.DUP),
                new AbstractMap.SimpleEntry<>("C", Opcodes.DUP),
                new AbstractMap.SimpleEntry<>("Z", Opcodes.DUP),
                new AbstractMap.SimpleEntry<>("J", Opcodes.DUP2),
                new AbstractMap.SimpleEntry<>("F", Opcodes.DUP),
                new AbstractMap.SimpleEntry<>("D", Opcodes.DUP2)));
        INSTRS.put(Opcodes.AASTORE, Arrays.asList(
                new AbstractMap.SimpleEntry<>("I", Opcodes.IASTORE),
                new AbstractMap.SimpleEntry<>("B", Opcodes.IASTORE),
                new AbstractMap.SimpleEntry<>("S", Opcodes.IASTORE),
                new AbstractMap.SimpleEntry<>("C", Opcodes.IASTORE),
                new AbstractMap.SimpleEntry<>("Z", Opcodes.IASTORE),
                new AbstractMap.SimpleEntry<>("J", Opcodes.LASTORE),
                new AbstractMap.SimpleEntry<>("F", Opcodes.FASTORE),
                new AbstractMap.SimpleEntry<>("D", Opcodes.DASTORE)));
        INSTRS.put(Opcodes.ACONST_NULL, Arrays.asList(
                new AbstractMap.SimpleEntry<>("I", Opcodes.ICONST_0),
                new AbstractMap.SimpleEntry<>("B", Opcodes.ICONST_0),
                new AbstractMap.SimpleEntry<>("S", Opcodes.ICONST_0),
                new AbstractMap.SimpleEntry<>("C", Opcodes.ICONST_0),
                new AbstractMap.SimpleEntry<>("Z", Opcodes.ICONST_0),
                new AbstractMap.SimpleEntry<>("J", Opcodes.LCONST_0),
                new AbstractMap.SimpleEntry<>("F", Opcodes.FCONST_0),
                new AbstractMap.SimpleEntry<>("D", Opcodes.DCONST_0)));
        INSTRS.put(Opcodes.ANEWARRAY, Arrays.asList(
                new AbstractMap.SimpleEntry<>("I", Opcodes.T_INT),
                new AbstractMap.SimpleEntry<>("B", Opcodes.T_BYTE),
                new AbstractMap.SimpleEntry<>("S", Opcodes.T_SHORT),
                new AbstractMap.SimpleEntry<>("C", Opcodes.T_CHAR),
                new AbstractMap.SimpleEntry<>("Z", Opcodes.T_BOOLEAN),
                new AbstractMap.SimpleEntry<>("J", Opcodes.T_LONG),
                new AbstractMap.SimpleEntry<>("F", Opcodes.T_FLOAT),
                new AbstractMap.SimpleEntry<>("D", Opcodes.T_DOUBLE)));
        // TODO case IF_ACMPEQ
        // TODO case IF_ACMPNE
        // TODO case MULTIANEWARRAY ??
    }

    // The name of the front class of the enclosing class.
    private final String frontOwner;
    private final String methodName;
    // The enclosing class name.
    private final String owner;
    private final InvokeAnyAdapter invokeAnyAdapter;
    private final Handle bsmRTBridge;
    private final boolean isStatic;
    private final ShiftMap shiftMap;

    // Variable for the anewarray substitution.
    private boolean isInstallingTypedANewArray;
    private Label end;


    static BackMethodVisitor createBackMethodVisitor(int api, String methodName, String frontOwner, String owner, String descriptor, int methodAccess, MethodVisitor mv) {
        boolean isStatic = (methodAccess & Opcodes.ACC_STATIC) > 0;
        ShiftMap shiftMap = isStatic ? ShiftMapConstantSlots.createStaticMethodShiftMap(methodName, Type.getType(descriptor)):
                ShiftMapConstantSlots.createInstanceMethodShiftMap(methodName, Type.getType(descriptor));
        return new BackMethodVisitor(api, methodName, frontOwner, owner, isStatic, shiftMap, mv);
    }

    @Override
    public void visitCode() {
        super.visitCode();
        if (isStatic) {
            // This is made in the call of super(); for the constructor (which is the only non static
            // back class method).
            writeHeader();

            System.out.println(shiftMap.dump());
        }
    }

    private BackMethodVisitor(int api, String methodName, String frontOwner, String owner, boolean isStatic, ShiftMap shiftMap, MethodVisitor mv) {
        super(api, mv);
        this.methodName = methodName;
        this.frontOwner = frontOwner;
        this.owner = owner;
        this.isStatic = isStatic;
        this.shiftMap = shiftMap;
        bsmRTBridge = new Handle(Opcodes.H_INVOKESTATIC, owner, BackClassVisitor.BSM_RT_BRIDGE, BackClassVisitor.BSM_RT_BRIDGE_DESC, false);

        InvokeAnyAdapter.InvokeFieldAdapter invokeFieldAdapter = new InvokeAnyAdapter.InvokeFieldAdapter() {
            @Override
            public void getField(String owner, String name, String desc) {
                String returnDescriptor = Type.rawDesc(desc);
                String erasedReturnDescriptor = Type.eraseNotJavaLangReference(returnDescriptor); // Removing parameterized types.
                // Normally inserting the front enclosingClass : "(L" + frontOwner + ";". But instead inserting its Object erasure.
                visitInvokeDynamicInsn(name, "(Ljava/lang/Object;)" + erasedReturnDescriptor, bsmRTBridge,
                        BackClassVisitor.HANDLE_RT_BSM_GET_FIELD, "(L" + owner + ";)" + desc);
            }

            @Override
            public void putField(String owner, String name, String desc) {
                visitInvokeDynamicInsn(name, "(Ljava/lang/Object;" + Type.eraseNotJavaLangReference(desc) + ")V",
                        bsmRTBridge, BackClassVisitor.HANDLE_RT_BSM_PUT_FIELD, "(L" + owner + ";" + desc + ")V");
            }
        };

        invokeAnyAdapter = new InvokeAnyAdapter(owner, methodName, invokeFieldAdapter, this);
    }

    /**
     * Appends the shift header allowing the shift of small sized parameters in the descriptor.
     */
    private void writeHeader() {
        //System.out.println(shiftMap);
        shiftMap.writeHeader(this);
    }

    /**
     * @return the current {@link BackMethodVisitor} enclosing class name.
     */
    public String getOwner() {
        return owner;
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if (opcode == Opcodes.CHECKCAST && isInstallingTypedANewArray) {
            visitTypedTypeInsn(Opcodes.CHECKCAST, "CHECKCAST ON " + type.substring(0, 2), type);
            visitLabel(end);
            isInstallingTypedANewArray = false;
            return;
        }
        if (!invokeAnyAdapter.visitTypeInsn(opcode, type)) {
            super.visitTypeInsn(opcode, type);
            return;
        }
    }

    @Override
    public void visitInsn(int opcode) {
        if (!invokeAnyAdapter.visitInsn(opcode)) {
            super.visitInsn(opcode);
        }
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner,
                                final String name, final String desc, final boolean itf) {

        // From the back class, every method invocation is an invoke dynamic.

        // Virtual invocation
        if (opcode == Opcodes.INVOKEVIRTUAL) {
            // Is a ParameterizedType or an Object.
            String normalizedOwner;
            if (Type.isParameterizedType(owner)) {
                normalizedOwner = owner + ';';/*Type.rawDesc(owner);*/
            } else {
                normalizedOwner = 'L' + owner + ';';
            }
            visitInvokeDynamicInsn(name, Type.eraseNotJavaLangMethod(insertReceiver("Ljava/lang/Object;", desc)), bsmRTBridge,
                    BackClassVisitor.HANDLE_RT_BSM_INVOKE_VIRTUAL_FROM_BACK, insertReceiver(normalizedOwner, desc));
            return;
        }

        // Static invocation
        if (opcode == Opcodes.INVOKESTATIC) {
            // return;
        }

        // After the call of super(); writing down the method's header shifting parameters.
        if (opcode == Opcodes.INVOKESPECIAL) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            writeHeader();
            return;
        }

        if (!invokeAnyAdapter.visitMethodInsn(opcode, owner, name, desc, itf, true)) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

    public String insertReceiver(String receiver, String desc) {
        Type type = Type.getMethodType(desc);
        Type[] argumentTypes = type.getArgumentTypes();
        Type[] args = new Type[argumentTypes.length + 1];
        args[0] = Type.getType(receiver);
        for (int i = 0; i < argumentTypes.length; i++) {
            args[i + 1] = argumentTypes[i];
        }
        return Type.getMethodDescriptor(type.getReturnType(), args);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (!invokeAnyAdapter.visitFieldInsn(opcode, owner, name, desc)) {
            // Erasing all description contained in this class.
            owner = Type.rawName(owner);
            if (owner.equals(this.frontOwner)) {
                owner = this.owner;
            }
            super.visitFieldInsn(opcode, owner, name, Type.eraseNotJavaLangReference(desc));
        }
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        if (bsm.getName().equals("metafactory")) {
            visitInvokeDynamicInsn(name, desc, bsmRTBridge, BackClassVisitor.HANDLE_RT_METAFACTORY, "metafactory__");
            return;
        }
        // Normal operations.
        super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        // Visiting normal local variable.
        // Switching the offset
        boolean isLarge = false;
        switch (opcode) {
            case Opcodes.LLOAD:
            case Opcodes.DLOAD:
            case Opcodes.LSTORE:
            case Opcodes.DSTORE:
                isLarge = true;
                break;
        }
        var = shiftMap.getNewVariableIndex(var);
        superVisitVarInsn(opcode, var);
    }

    /**
     * Allowing to call this method passing through the var shift without being affected.
     */
    public void superVisitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        super.visitLocalVariable(name, desc, signature, start, end, index);
    }

    @Override
    public void visitTypedTypeInsnWithParameter(String name, int typedOpcode, int parameter) {
        // Shifting the parameter according to the shiftMap
        // Local instruction of type LOAD/STORE
        if (typedOpcode != Opcodes.ANEWARRAY && typedOpcode != Opcodes.DUP) {
            // Any parameter get. It is always considered as large slot variable.
            parameter = shiftMap.getNewVariableIndex(parameter);
        }
        visitTypeVarSpecialization(name, typedOpcode, parameter);
    }


    @Override
    public void visitTypedInsn(String name, int typedOpcode) {
        switch (typedOpcode) {
            case Opcodes.ALOAD_0:
            case Opcodes.ALOAD_1:
            case Opcodes.ALOAD_2:
            case Opcodes.ALOAD_3: {
                int parameter = typedOpcode - Opcodes.ALOAD_0;
                visitTypedTypeInsnWithParameter(name, Opcodes.ALOAD, parameter);
                break;
            }
            case Opcodes.ASTORE_0:
            case Opcodes.ASTORE_1:
            case Opcodes.ASTORE_2:
            case Opcodes.ASTORE_3: {
                int parameter = typedOpcode - Opcodes.ASTORE_0;
                visitTypedTypeInsnWithParameter(name, Opcodes.ASTORE, parameter);
                break;
            }
            case Opcodes.DUP:{
                int parameter = typedOpcode - Opcodes.DUP;
                visitTypedTypeInsnWithParameter(name, Opcodes.DUP, parameter);
                break;
            }/*
                visitInsn(Opcodes.DUP2);
                break;*/
            default:
                visitTypedTypeInsnWithParameter(name, typedOpcode, -1);
                break;
        }
    }

    void visitShiftTypeVar(String name, int index, int newIndex) {
        end = new Label();
        List<AbstractMap.SimpleEntry<String, Integer>> tests = INSTRS.get(Opcodes.ALOAD);
        if (tests == null) {
            throw new IllegalArgumentException("Invalid Opcode following TYPED instruction : " + Opcodes.ALOAD);
        }

        for (Map.Entry<String, Integer> test : tests) {
            String key = test.getKey();
            visitLdcInsn(key);
            visitLdcPlaceHolderString(BackClassVisitor.RT_SPECIALIZABLE_DESCRIPTOR_TYPE, name, null, true); // Test on TX.

            Label label = new Label();
            visitJumpInsn(Opcodes.IF_ACMPNE, label);

            int newOpcode = test.getValue();
            // The LOAD.
            superVisitVarInsn(newOpcode, index);
            // The corresponding STORE.
            switch (newOpcode) {
                case Opcodes.ILOAD:
                    superVisitVarInsn(Opcodes.ISTORE, newIndex);
                    break;
                case Opcodes.FLOAD:
                    superVisitVarInsn(Opcodes.FSTORE, newIndex);
                    break;
                case Opcodes.LLOAD:
                    superVisitVarInsn(Opcodes.LSTORE, newIndex);
                    break;
                case Opcodes.DLOAD:
                    superVisitVarInsn(Opcodes.DSTORE, newIndex);
                    break;
                default:
                    throw new IllegalStateException("Load bytecode : " + newOpcode + " not handled.");
            }
            visitJumpInsn(Opcodes.GOTO, end);
            visitLabel(label);
        }

        // If none of them worked, doing the original then.
        printASMMsg("Choosing : " + name + " Type[Object] : " + Opcodes.ALOAD, this);
        superVisitVarInsn(Opcodes.ALOAD, index);
        visitLabel(end);

    }

    /**
     * The sequence of switch on the runtime type.
     */
    private void visitTypeVarSpecialization(String name, int typedOpcode, int parameter) {
        end = new Label();
        List<AbstractMap.SimpleEntry<String, Integer>> tests = INSTRS.get(typedOpcode);
        if (tests == null) {
            throw new IllegalArgumentException("Invalid Opcode following TYPED instruction : " + typedOpcode);
        }

        for (Map.Entry<String, Integer> test : tests) {
            String key = test.getKey();
            visitLdcInsn(key);
            visitLdcPlaceHolderString(BackClassVisitor.RT_SPECIALIZABLE_DESCRIPTOR_TYPE, name, null, true); // Test on TX.

            Label label = new Label();
            visitJumpInsn(Opcodes.IF_ACMPNE, label);

            int newOpcode = test.getValue();
            switch (typedOpcode) {
                case Opcodes.ALOAD:
                case Opcodes.ASTORE:
                    superVisitVarInsn(newOpcode, parameter);
                    break;
                case Opcodes.DUP:
                case Opcodes.AASTORE:
                case Opcodes.AALOAD:
                case Opcodes.ARETURN:
                    visitInsn(newOpcode);
                    break;
                case Opcodes.ANEWARRAY:
                    visitIntInsn(Opcodes.NEWARRAY, newOpcode);
                    break;
                default:
                    // TODO ACONST_NULL, AASTORE, AALOAD.
                    System.err.println("BackMethodVisitor#visitTypedTypeInsnWithParameter : TypedCode not handled : " + typedOpcode);
                    break;
            }
            visitJumpInsn(Opcodes.GOTO, end);
            visitLabel(label);
        }

        // If none of them worked, doing the original then.
        printASMMsg("Choosing : " + name + " Type[Object] : " + typedOpcode, this);
        // TODO Handle MULTIANEWARRAY !!
        if (typedOpcode == Opcodes.ANEWARRAY) {
            isInstallingTypedANewArray = true;
            visitTypedTypeInsn(Opcodes.ANEWARRAY, "ANEWARRAY ON : " + name.substring(0, 2), name);
            // Visiting the following CHECKCAST and visitLabel(end) inside the BackMethodVisitor#visitTypeIns to not
            // Put the end label before the following CHECKCAST, and also retrieve right informations for it.
        } else {
            if (parameter == -1) {
                visitInsn(typedOpcode);
            } else {
                superVisitVarInsn(typedOpcode, parameter);
            }
            visitLabel(end);
        }
    }

    private static void printASMMsg(String msg, MethodVisitor mv) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitLdcInsn(msg);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }
}
