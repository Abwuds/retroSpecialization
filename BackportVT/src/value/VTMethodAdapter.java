package value;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.ASM5;

/**
 * Created by Fabien GIACHERIO on 08/02/17.
 *
 * This class is used to chain multiples transformations on method nodes
 */
public class VTMethodAdapter extends MethodVisitor {
    MethodVisitor next;

    public VTMethodAdapter(int access, String name, String desc, String signature, String[] exceptions, MethodVisitor mv) {
        super(ASM5, new MethodNode(access, name, desc, signature, exceptions));
        next = mv;
    }

    @Override
    public void visitEnd() {
        MethodNode mn = (MethodNode) mv;
        VTInstructionTransformer instrTransformer = new VTInstructionTransformer(null);
        instrTransformer.transform(mn);
        /*TestTransformer tt = new TestTransformer(null);
        tt.transform(mn);*/
        mn.accept(next);
    }
}
