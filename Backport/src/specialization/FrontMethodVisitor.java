package specialization;


import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import rt.Opcodes;
import rt.Type;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 *
 * Created by Jefferson Mangue on 12/06/2016.
 */
class FrontMethodVisitor extends MethodVisitor {
    /**
     * Invokedynamic constants.
     */
    private static final String BSM_NAME = "newBackSpecies";
    private static final Handle BSM_NEW_BACK_SPECIES;
    private static final Handle BSM_DELEGATE_CALL;

    static {
        MethodType mtNewBack = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class, String.class, String.class);
        MethodType mtDelegateCall = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class);
        BSM_NEW_BACK_SPECIES = new Handle(Opcodes.H_INVOKESTATIC, "rt/RT", "bsm_newBackSpecies", mtNewBack.toMethodDescriptorString(), false);
        BSM_DELEGATE_CALL = new Handle(Opcodes.H_INVOKESTATIC, "rt/RT", "bsm_delegateBackCall", mtDelegateCall.toMethodDescriptorString(), false);
    }

    //TODO Use an InvokeAnyAdapter too.
    private FrontMethodVisitor(int api, MethodVisitor mv) {
        super(api, mv);
    }

    public static void visitFrontMethod(int api, String frontName, String methodName, String desc, String signature, String[] exceptions, MethodVisitor fmw) {
        Type type = Type.getType(desc);
        // For the front method :
        if (methodName.equals("<init>")) {
            // Creating compatibility constructor.
            visitConstructor(Type.rawName(frontName), frontName, fmw, type);
        } else {
            // Instance methods :
            visitInstanceMethod(Type.rawName(frontName), methodName, fmw, type);
        }
        // TODO handle static method.
    }

    private static void visitConstructor(String frontName, String genericName, MethodVisitor mv, Type type) {
        mv.visitCode();
        visitSuperConstructor(mv);
        mv.visitVarInsn(Opcodes.ALOAD, 0); // PutField on this for the field _back__.

        // Loading constructor arguments.
        loadArguments(mv, type);
        Type[] argumentTypes = Type.eraseNotJavaLangMethod(type).getArgumentTypes();
        String indyDescriptor = Type.getMethodDescriptor(Type.getType("Ljava/lang/Object;"), argumentTypes);
        mv.visitInvokeDynamicInsn(BSM_NAME, indyDescriptor, BSM_NEW_BACK_SPECIES, frontName, genericName);
        mv.visitFieldInsn(Opcodes.PUTFIELD, frontName, FrontClassVisitor.BACK_FIELD, "Ljava/lang/Object;");

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void visitSuperConstructor(MethodVisitor mv) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        // TODO the super.<init> owner is Object for the moment, because the inheritance is not handled.
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    }

    private static void visitInstanceMethod(String frontName, String methodName, MethodVisitor mv, Type type) {
        mv.visitCode();
        // Getting the back field to delegate the call.
        mv.visitLdcInsn(methodName);
        mv.visitVarInsn(Opcodes.ALOAD, 0); // PutField on this for the field _back__.
        mv.visitFieldInsn(Opcodes.GETFIELD, frontName, FrontClassVisitor.BACK_FIELD, "Ljava/lang/Object;");
        // Delegating the call and the arguments.
        mv.visitVarInsn(Opcodes.ALOAD, 0);// PutField on this for the field _back__.
        loadArguments(mv, type);
        // Normally loading the front class descriptor : 'L' + frontName + ';'. But instead its Object erasure.
        String delegateDesc = createDelegateCallDescriptor(type, "Ljava/lang/Object;");
        mv.visitInvokeDynamicInsn("bsm_delegateBackCall", delegateDesc, BSM_DELEGATE_CALL);

        // The return.
        Type returnType = type.getReturnType();
        int sort = returnType.getSort();
        switch(sort) {
            case Type.OBJECT:
            case Type.TYPE_VAR:
            case Type.PARAMETERIZED_TYPE:
                mv.visitInsn(Opcodes.ARETURN);
                break;
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
            case Type.CHAR:
                mv.visitInsn(Opcodes.IRETURN);
                break;
            case Type.FLOAT:
                mv.visitInsn(Opcodes.FRETURN);
                break;
            case Type.LONG:
                mv.visitInsn(Opcodes.LRETURN);
                break;
            case Type.DOUBLE:
                mv.visitInsn(Opcodes.DRETURN);
                break;
            default: // void
                mv.visitInsn(Opcodes.RETURN);
                break;

        }
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static String createDelegateCallDescriptor(Type type, String frontDesc) {
        Type[] argsSrc = type.getArgumentTypes();
        Type[] args = new Type[argsSrc.length + 3];
        args[0] = Type.getType("Ljava/lang/String;"); // method name.
        args[1] = Type.getType("Ljava/lang/Object;"); // front#_back__ field := receiver.
        args[2] = Type.getType(frontDesc); // front receiver.
        // args[2] = Type.getType("[Ljava/lang/Object;"); // receiver.
        System.arraycopy(argsSrc, 0, args, 3, argsSrc.length); // method args.
        return Type.getMethodDescriptor(type.getReturnType(), args);
    }

    private static void loadArguments(MethodVisitor mv, Type type) {
        Type[] argumentTypes = type.getArgumentTypes();
        for (int i = 0; i < argumentTypes.length; i++) {
            Type arg = argumentTypes[i];
            int sort = arg.getSort();
            switch (sort) {
                case Type.BOOLEAN:
                case Type.SHORT:
                case Type.BYTE:
                case Type.INT:
                    mv.visitVarInsn(Opcodes.ILOAD, i + 1);
                    break;
                case Type.LONG:
                    mv.visitVarInsn(Opcodes.LLOAD, i + 1);
                    break;
                case Type.DOUBLE:
                    mv.visitVarInsn(Opcodes.DLOAD, i + 1);
                    break;
                case Type.OBJECT:
                case Type.TYPE_VAR:
                case Type.PARAMETERIZED_TYPE:
                    mv.visitVarInsn(Opcodes.ALOAD, i + 1);
                    // TODO add type String[] under the object type.
                /*case Type.ARRAY:
                    mv.visitVarInsn(Opcodes.ALOAD, i + 1); AALOAD ?*/
                    break;
                default:
                    throw new AssertionError("Type not handled : " + sort);
            }
        }
    }

    static void createFrontSpecializerConstructor(String rawName, String backField, MethodVisitor mv) {
        mv.visitCode();
        visitSuperConstructor(mv);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitFieldInsn(Opcodes.PUTFIELD, rawName, backField, "Ljava/lang/Object;");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
