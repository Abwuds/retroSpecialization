package specialization;


import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import rt.Opcodes;
import rt.RT;

/**
 *
 * Created by Jefferson Mangue on 12/06/2016.
 */
class InvokeAnyMethodVisitor extends MethodVisitor {

    private final InvokeAnyAdapter invokeAnyAdapter;

    InvokeAnyMethodVisitor(String owner, int api, MethodVisitor mv, String methodName) {
        super(api, mv);

        InvokeAnyAdapter.InvokeFieldAdapter invokeFieldAdapter = new InvokeAnyAdapter.InvokeFieldAdapter() {
            @Override
            public void getField(String owner, String name, String desc) {
                String eraseDesc = Type.eraseNotJavaLangReference(Type.rawDesc(desc));
                // Normally inserting the front enclosingClass : "(L" + frontOwner + ";". But instead inserting its Object erasure.
                Handle bsm = new Handle(Opcodes.H_INVOKESTATIC, "rt/RT", "bsm_getFieldNoLookup",
                        RT.TYPE_NO_LOOKUP_BSM_GETFIELD.toMethodDescriptorString(), false);
                // TODO remove the + erasedDesc which is wrong since when calling from this invoke context, we manipulate not erased types.
                visitInvokeDynamicInsn(name, "(Ljava/lang/Object;)" + eraseDesc, bsm, "(L" + owner + ";)" + desc);
            }

            @Override
            public void putField(String owner, String name, String desc) {
                Handle bsm = new Handle(Opcodes.H_INVOKESTATIC, "rt/RT", "bsm_putFieldNoLookup",
                        RT.TYPE_NO_LOOKUP_BSM_PUTFIELD.toMethodDescriptorString(), false);
                visitInvokeDynamicInsn(name, "(Ljava/lang/Object;" + Type.eraseNotJavaLangReference(desc) + ")V", bsm,
                        "(L" + owner + ";" + desc + ")V");
            }
        };
        invokeAnyAdapter = new InvokeAnyAdapter(owner, methodName, invokeFieldAdapter, this);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if (!invokeAnyAdapter.visitTypeInsn(opcode, type)) {
            super.visitTypeInsn(opcode, type);
        }
    }

    @Override
    public void visitInsn(int opcode) {
        if (!invokeAnyAdapter.visitInsn(opcode)) {
            super.visitInsn(opcode);
        }
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (!invokeAnyAdapter.visitFieldInsn(opcode, owner, name, desc)) {
            super.visitFieldInsn(opcode, owner, name, desc);
        }
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner,
                                final String name, final String desc, final boolean itf) {
        if (!invokeAnyAdapter.visitMethodInsn(opcode, owner, name, desc, itf, false)) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }
}
