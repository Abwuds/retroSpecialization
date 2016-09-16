package specialization;


import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import rt.Opcodes;

import java.util.*;

/**
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
        INSTRS.put(Opcodes.ALOAD_0, Arrays.asList(
                new AbstractMap.SimpleEntry<>("I", Opcodes.ILOAD),
                new AbstractMap.SimpleEntry<>("B", Opcodes.ILOAD),
                new AbstractMap.SimpleEntry<>("S", Opcodes.ILOAD),
                new AbstractMap.SimpleEntry<>("C", Opcodes.ILOAD),
                new AbstractMap.SimpleEntry<>("Z", Opcodes.ILOAD),
                new AbstractMap.SimpleEntry<>("J", Opcodes.LLOAD),
                new AbstractMap.SimpleEntry<>("F", Opcodes.FLOAD),
                new AbstractMap.SimpleEntry<>("D", Opcodes.DLOAD)));
        INSTRS.put(Opcodes.ALOAD_1, Arrays.asList(
                new AbstractMap.SimpleEntry<>("I", Opcodes.ILOAD),
                new AbstractMap.SimpleEntry<>("B", Opcodes.ILOAD),
                new AbstractMap.SimpleEntry<>("S", Opcodes.ILOAD),
                new AbstractMap.SimpleEntry<>("C", Opcodes.ILOAD),
                new AbstractMap.SimpleEntry<>("Z", Opcodes.ILOAD),
                new AbstractMap.SimpleEntry<>("J", Opcodes.LLOAD),
                new AbstractMap.SimpleEntry<>("F", Opcodes.FLOAD),
                new AbstractMap.SimpleEntry<>("D", Opcodes.DLOAD)));
        INSTRS.put(Opcodes.ALOAD_2, Arrays.asList(
                new AbstractMap.SimpleEntry<>("I", Opcodes.ILOAD),
                new AbstractMap.SimpleEntry<>("B", Opcodes.ILOAD),
                new AbstractMap.SimpleEntry<>("S", Opcodes.ILOAD),
                new AbstractMap.SimpleEntry<>("C", Opcodes.ILOAD),
                new AbstractMap.SimpleEntry<>("Z", Opcodes.ILOAD),
                new AbstractMap.SimpleEntry<>("J", Opcodes.LLOAD),
                new AbstractMap.SimpleEntry<>("F", Opcodes.FLOAD),
                new AbstractMap.SimpleEntry<>("D", Opcodes.DLOAD)));
        INSTRS.put(Opcodes.ALOAD_3, Arrays.asList(
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
        INSTRS.put(Opcodes.ASTORE_0, Arrays.asList(
                new AbstractMap.SimpleEntry<>("I", Opcodes.ISTORE),
                new AbstractMap.SimpleEntry<>("B", Opcodes.ISTORE),
                new AbstractMap.SimpleEntry<>("S", Opcodes.ISTORE),
                new AbstractMap.SimpleEntry<>("C", Opcodes.ISTORE),
                new AbstractMap.SimpleEntry<>("Z", Opcodes.ISTORE),
                new AbstractMap.SimpleEntry<>("J", Opcodes.LSTORE),
                new AbstractMap.SimpleEntry<>("F", Opcodes.FSTORE),
                new AbstractMap.SimpleEntry<>("D", Opcodes.DSTORE)));
        INSTRS.put(Opcodes.ASTORE_1, Arrays.asList(
                new AbstractMap.SimpleEntry<>("I", Opcodes.ISTORE),
                new AbstractMap.SimpleEntry<>("B", Opcodes.ISTORE),
                new AbstractMap.SimpleEntry<>("S", Opcodes.ISTORE),
                new AbstractMap.SimpleEntry<>("C", Opcodes.ISTORE),
                new AbstractMap.SimpleEntry<>("Z", Opcodes.ISTORE),
                new AbstractMap.SimpleEntry<>("J", Opcodes.LSTORE),
                new AbstractMap.SimpleEntry<>("F", Opcodes.FSTORE),
                new AbstractMap.SimpleEntry<>("D", Opcodes.DSTORE)));
        INSTRS.put(Opcodes.ASTORE_2, Arrays.asList(
                new AbstractMap.SimpleEntry<>("I", Opcodes.ISTORE),
                new AbstractMap.SimpleEntry<>("B", Opcodes.ISTORE),
                new AbstractMap.SimpleEntry<>("S", Opcodes.ISTORE),
                new AbstractMap.SimpleEntry<>("C", Opcodes.ISTORE),
                new AbstractMap.SimpleEntry<>("Z", Opcodes.ISTORE),
                new AbstractMap.SimpleEntry<>("J", Opcodes.LSTORE),
                new AbstractMap.SimpleEntry<>("F", Opcodes.FSTORE),
                new AbstractMap.SimpleEntry<>("D", Opcodes.DSTORE)));
        INSTRS.put(Opcodes.ASTORE_3, Arrays.asList(
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

    // Variable for the anewarray substitution.
    private boolean isInstallingANewArray;
    private Label end;

    BackMethodVisitor(int api, String methodName, String frontOwner, String owner, MethodVisitor mv) {
        super(api, mv);
        this.methodName = methodName;
        this.frontOwner = frontOwner;
        this.owner = owner;
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
     * @return the current {@link BackMethodVisitor} enclosing class name.
     */
    public String getOwner() {
        return owner;
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if (opcode == Opcodes.CHECKCAST && isInstallingANewArray) {
            visitTypedTypeInsn(Opcodes.CHECKCAST, "CHECKCAST ON " + type.substring(0, 2), type);
            visitLabel(end);
            isInstallingANewArray = false;
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
            if (Type.isParameterizedType(owner)) { normalizedOwner = Type.rawDesc(owner); }
            else { normalizedOwner = 'L' + owner + ';'; }
            visitInvokeDynamicInsn(name, Type.eraseNotJavaLangMethod(insertReceiver("Ljava/lang/Object;", desc)), bsmRTBridge,
                    BackClassVisitor.HANDLE_RT_BSM_INVOKE_SPECIAL_FROM_BACK, insertReceiver(normalizedOwner, desc));
            return;
        }

        // Static invocation
        if (opcode == Opcodes.INVOKESTATIC) {

           // return;
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
        for (int i = 0; i < argumentTypes.length; i++) { args[i + 1] = argumentTypes[i]; }
        return Type.getMethodDescriptor(type.getReturnType(), args);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (!invokeAnyAdapter.visitFieldInsn(opcode, owner, name, desc)) {
            // Erasing all description contained in this class.
            owner = Type.rawName(owner);
            if (owner.equals(this.frontOwner)) { owner = this.owner; }
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

    private void noReplacedTyped(String name, int typedOpcode) {
        super.visitTypedInsn(name, typedOpcode);
        // TODO replace this by the switch of typed opcode.
        if (typedOpcode <= Opcodes.ALOAD_0 || typedOpcode <= Opcodes.ALOAD_3) {
            visitVarInsn(Opcodes.ALOAD, typedOpcode - Opcodes.ALOAD_0);
            return;
        }
        visitInsn(typedOpcode);
    }

    @Override
    public void visitTypedInsn(String name, int typedOpcode) {
        // noReplacedTyped(name, typedOpcode);

        end = new Label();
        List<AbstractMap.SimpleEntry<String, Integer>> tests = INSTRS.get(typedOpcode);
        if (tests == null) {
            throw new IllegalArgumentException("Invalid Opcode following TYPED instruction : " + typedOpcode);
        }

        for (Map.Entry<String, Integer> test : tests) {
            String key = test.getKey();
            int newOpcode = test.getValue();
            visitLdcInsn(key);
            visitLdcTypedString("Type test on : " + name.substring(0, 2), name); // Test on TX.

            // TODO '==' instead of equals.
            visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
            Label label = new Label();
            visitJumpInsn(Opcodes.IFEQ, label);
            switch (typedOpcode) {
                case 42: // Opcodes.ALOAD_0:
                case 43: // Opcodes.ALOAD_1:
                case 44: // Opcodes.ALOAD_2:
                case 45: // Opcodes.ALOAD_3:
                    visitVarInsn(newOpcode, typedOpcode - Opcodes.ALOAD_0);
                    break;
                case 75: //Opcodes.ASTORE_0:
                case 76: //Opcodes.ASTORE_1:
                case 77: //Opcodes.ASTORE_2:
                case 78: //Opcodes.ASTORE_3:
                    visitVarInsn(newOpcode, typedOpcode - Opcodes.ASTORE_0);
                    break;
                case 83: // Opcodes.AASTORE:
                case 50: // Opcodes.AALOAD:
                    visitInsn(newOpcode);
                    break;
                case 176: // Opcodes.ARETURN:
                    visitInsn(newOpcode);
                    break;
                case 189: // Opcodes.ANEWARRAY:
                    visitIntInsn(Opcodes.NEWARRAY, newOpcode);
                    break;
                default:
                    // TODO ACONST_NULL, AASTORE, AALOAD.
                    System.err.println("BackMethodVisitor#visitTypedInsn : TypedCode not handled : " + typedOpcode);
                    break;
            }
            visitJumpInsn(Opcodes.GOTO, end);
            visitLabel(label);
        }

        // If none of them worked, doing the original then.
        printASMMsg("Choosing : " + name + " Type[Object] : " + typedOpcode, this);
        // TODO MULTIANEWARRAY !!
        if (typedOpcode == Opcodes.ANEWARRAY) {
            isInstallingANewArray = true;
            visitTypedTypeInsn(Opcodes.ANEWARRAY, "ANEWARRAY ON : " + name.substring(0, 2), name);
            // Visiting the following CHECKCAST and visitLabel(end) inside the BackMethodVisitor#visitTypeIns to not
            // Put the end label before the following CHECKCAST, and also retrieve right informations for it.
        } else {
            visitInsn(typedOpcode);
            visitLabel(end);
        }
    }

    public static void printASMMsg(String msg, MethodVisitor mv) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitLdcInsn(msg);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }

}
