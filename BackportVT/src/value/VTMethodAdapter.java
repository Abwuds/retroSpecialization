package value;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.ASM5;

/**
 * Created by Fabien GIACHERIO on 08/02/17.
 *
 * This class is used to chain multiples transformations on method nodes
 */
public class VTMethodAdapter extends MethodVisitor {
    final MethodVisitor writer;
    final String owner;
    final String name;
    final String initDescriptor;
    final int access;
    final String desc;
    LocalVariablesSorter lvs;

    public VTMethodAdapter(int access, String name, String desc, String signature, String[] exceptions, MethodVisitor mv, String owner, String initDescriptor) {
        super(ASM5, new MethodNode(access, name, desc, signature, exceptions));
        this.writer = mv;
        this.owner = owner;
        this.name = name;
        this.initDescriptor = initDescriptor;
        this.access=access;
        this.desc=desc;
    }

    @Override
    public void visitEnd() {
        MethodNode mn = (MethodNode) mv;
        VTMethodVisitor instrTransformer = new VTMethodVisitor(null, lvs, writer, owner, initDescriptor);
        instrTransformer.transform(mn);
    }
}
