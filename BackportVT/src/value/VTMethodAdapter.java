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
    private String className;
    private String initDescriptor;

    public VTMethodAdapter(int access, String name, String desc, String signature, String[] exceptions, MethodVisitor mv, String className, String initDescriptor) {
        super(ASM5, new MethodNode(access, name, desc, signature, exceptions));
        next = mv;
        this.className = className;
        this.initDescriptor = initDescriptor;
    }

    @Override
    public void visitEnd() {
        MethodNode mn = (MethodNode) mv;
        VTInstructionTransformer instrTransformer = new VTInstructionTransformer(null, className, initDescriptor);
        instrTransformer.transform(mn);
        mn.accept(next);
    }
}
