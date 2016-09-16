package specialization;

import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import rt.Opcodes;
import rt.RT;

import java.util.Stack;

/**
 * Created by Jefferson Mangue on 08/07/2016.
 */
public class InvokeAnyAdapter {

    /**
     * Invokedynamic constants.
     */
    private static final Handle BSM_BSM_CREATE_ANY = new Handle(Opcodes.H_INVOKESTATIC, "rt/RT", "bsm_createAny", RT.TYPE_BSM_CREATE_ANY.toMethodDescriptorString(), false);
    public static final String BSM_NAME = "createAny";
    private static final Handle BSM_BSM_CREATE_ANY_NO_LOOKUP = new Handle(Opcodes.H_INVOKESTATIC, "rt/RT", "bsm_createAnyNoLookup", RT.TYPE_NO_LOOKUP_BSM_CREATE_ANY.toMethodDescriptorString(), false);;
    private final String enclosingClass;
    private final String methodName;
    private final InvokeFieldAdapter invokeFieldAdapter;


    /**
     * Enumeration used for the detection of invoke special calls.
     * This enumeration indicates if an eligible NEW opcodes sequence, for the substitution by an invokedynamic
     * has been visited and then the same for DUP opcode. Only a NEW applied on generics is selected to be replaced.
     * Other new have to be considered and ignored since they must not be replaced.
     */
    private enum InvokeSpecialVisited {
        REPLACED_NEW, REPLACED_DUP, IGNORED_NEW, IGNORED_DUP
    }

    // An int is not sufficient. Because a stack level has multiple states possible.
    // First it has the boolean value "dup visited" to detect if the dup has been visited (and skipped) or not.
    // Otherwise all possible dup between the "new" opcode and the "invokespecial" will be skipped. .
    private final Stack<InvokeSpecialVisited> invokeSpecialStack = new Stack<>();

    private final MethodVisitor mv;

    public InvokeAnyAdapter(String enclosingClass, String methodName, InvokeFieldAdapter invokeFieldAdapter, MethodVisitor mv) {
        this.enclosingClass = enclosingClass;
        this.methodName = methodName;
        this.invokeFieldAdapter = invokeFieldAdapter;
        this.mv = mv;
    }

    public boolean visitTypeInsn(int opcode, String type) {
        if (opcode != Opcodes.NEW) {
            return false;
        }

        // Ignoring this new since it does not manipulate generics.
        if (!type.startsWith("$")) {
            invokeSpecialStack.push(InvokeSpecialVisited.IGNORED_NEW);
            return false;
        }

        // Replacing the "NEW" opcode since it will be replaced by invokedynamic.
        invokeSpecialStack.push(InvokeSpecialVisited.REPLACED_NEW);
        return true;
    }

    public boolean visitInsn(int opcode) {
        if (opcode != Opcodes.DUP || invokeSpecialStack.empty()) {
            return false;
        }
        // Replacing the DUP opcode corresponding to a NEW opcode replaced.
        if (InvokeSpecialVisited.REPLACED_NEW.equals(invokeSpecialStack.peek())) {
            invokeSpecialStack.set(invokeSpecialStack.size() - 1, InvokeSpecialVisited.REPLACED_DUP);
            return true;
        }
        // Ignoring the DUP opcode corresponding to a NEW opcode ignored.
        if (InvokeSpecialVisited.IGNORED_NEW.equals(invokeSpecialStack.peek())) {
            invokeSpecialStack.set(invokeSpecialStack.size() - 1, InvokeSpecialVisited.IGNORED_DUP);
        }
        return false;
    }

    public boolean visitMethodInsn(final int opcode, final String owner, final String name, final String desc,
                                   final boolean itf, boolean eraseInvoke) {
        // Calling directly the back field of a virtual method call on a parameterized type.
        // TODO do the same for back method but with ERASURE.
        if (opcode == Opcodes.INVOKEVIRTUAL && Type.isParameterizedType(owner)) {
            String inlinedBackCallDesc = createInlinedBackCallDescriptor(Type.getType(desc), "Ljava/lang/Object;");
            Handle bsm_inlinedBackCall = new Handle(Opcodes.H_INVOKESTATIC, "rt/RT", "bsm_inlinedBackCall", RT.BSMS_TYPE.toMethodDescriptorString(), false);
            mv.visitInvokeDynamicInsn(name, inlinedBackCallDesc, bsm_inlinedBackCall);
            // Case handled.
            return true;
        }

        // TODO replace INVOKEVIRTUAL inside the back context by an invokedynamic of bsm_invokeSpecial.


        if (opcode != Opcodes.INVOKESPECIAL) {
            // Writing the call inside the class. Case not handled.
            return false;
        }

        // Detect "new" call to substitute it by an invokedynamic.
        if (!invokeSpecialStack.empty()) {
            InvokeSpecialVisited top = invokeSpecialStack.peek();
            if (InvokeSpecialVisited.REPLACED_DUP.equals(top)) {

                // Erasing the arguments. Because we are invoking a constructor of the back which are always erased.
                // But we preserve the good return type to type correctly if no full erasure are requested
                // (when invoking from a front code or a normal class code, typing normally.
                Type type = Type.eraseNotJavaLangMethod(Type.getMethodType(desc));
                Type returnType = Type.getType(owner);
                if (eraseInvoke) { returnType = Type.eraseNotJavaLangReference(returnType); }
                String methodDescription = Type.getMethodType(returnType, type.getArgumentTypes()).toString();
                // The name has to be <init>, but this is not a valid bsm identifier because of "<" and ">".
                mv.visitInvokeDynamicInsn(BSM_NAME, methodDescription, BSM_BSM_CREATE_ANY_NO_LOOKUP, owner);
                invokeSpecialStack.pop();
                // Case handled.
                return true;
            }
            // IGNORED_DUP. Popping the current stack level.
            invokeSpecialStack.pop();
        }
        // Writing method call.
        return false;
    }

    public boolean visitFieldInsn(int opcode, String owner, String name, String desc) {
        // Regular field enclosingClass (not parameterized type or so).
        if (!Type.isParameterizedType(owner)) {
            // If the enclosing class is the field's enclosingClass, then its a regular field visited.
           // if (!this.enclosingClass.equals(enclosingClass)) {
                return false; // super.visitFieldInsn(opcode, enclosingClass, name, Type.rawDesc(desc));
            //}
        }

        owner = Type.rawName(owner);
        // When the field owner is parameterized and we are inside its constructor, performing regular field operations.
        if (getBackRepresentation(owner).equals(enclosingClass) && methodName.equals("<init>")) {
            return false; // super.visitFieldInsn(opcode, enclosingClass, name, Type.eraseNotJavaLangReference(desc));
        }

        // Otherwise, we always perform an invoke dynamic to retrieve the field.
        if (opcode == Opcodes.GETFIELD) {
            // Every getfield/putfield in method which are not <init>, is transformed in a getfield/putfield
            // on the back field. To do so, we pass the logic to an invokedynamic which will
            // get the field value or push the value in the field contained inside the back class.
            invokeFieldAdapter.getField(owner, name, desc);
            return true;
        }

        if (opcode == Opcodes.PUTFIELD) {
            invokeFieldAdapter.putField(owner, name, desc);
            return true;
        }

        // Neither TYPE_GETFIELD nor PUTFIELD.
        // super.visitFieldInsn(opcode, enclosingClass, name, Type.eraseNotJavaLangReference(desc));
        return false;
    }

    public static String getBackRepresentation(String owner) {
        return BackClassVisitor.ANY_PACKAGE + Type.rawName(owner) + BackClassVisitor.BACK_FACTORY_NAME;
    }

    private static String createInlinedBackCallDescriptor(Type type, String frontDesc) {
        Type[] argsSrc = type.getArgumentTypes();
        int argsLength = argsSrc.length;
        Type[] args = new Type[argsLength + 1];
        args[0] = Type.getType(frontDesc); // front to perform front#getField:_back__ and delegate it the call.
        System.arraycopy(argsSrc, 0, args, 1, argsLength); // method args.
        return Type.getMethodDescriptor(type.getReturnType(), args);
    }


    interface InvokeFieldAdapter {
        void getField(String owner, String name, String desc);
        void putField(String owner, String name, String desc);
    }
}
