package value;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Map;

/**
 * Created by Fabien GIACHERIO on 28/02/17.
 *
 *  This consumer represents an instruction transformation.
 *  It is computed during the analysis in the VTMethodVisitor.
 */
public interface VTConsumer {

    void consume(MethodNode mn, MethodVisitor mv, AbstractInsnNode insn, Map<String, Integer> registers);
}
