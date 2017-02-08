package value;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Iterator;

/**
 * Created by Fabien GIACHERIO on 08/02/17.
 *
 * Used to transform any VT opcode to the reference's opcode equivalent
 */
public class VTInstructionTransformer extends MethodTransformer {

    public VTInstructionTransformer(MethodTransformer mt) {
        super(mt);
    }

    @Override
    public void transform(MethodNode mn) {
        InsnList insns = mn.instructions;
        Iterator i = insns.iterator();
        while (i.hasNext()) {
            AbstractInsnNode i1 = (AbstractInsnNode) i.next();
            if(isVLOAD0(i1)) {
//                ((VarInsnNode) i1).setOpcode(Opcodes.ALOAD);
            }
        }
        super.transform(mn);
    }

    private static AbstractInsnNode getNext(AbstractInsnNode insn) {
        do {
            insn = insn.getNext();
            if (insn != null && !(insn instanceof LineNumberNode)) {
                break;
            }
        } while (insn != null);
        return insn;
    }

    private static boolean isVLOAD0(AbstractInsnNode i) {
        return i.getOpcode() == Opcodes.VLOAD && ((VarInsnNode) i).var == 0;
    }
}
