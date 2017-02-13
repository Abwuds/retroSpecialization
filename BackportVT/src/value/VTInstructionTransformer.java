package value;

import jdk.nashorn.internal.codegen.types.Type;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Iterator;
import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;

/**
 * Created by Fabien GIACHERIO on 08/02/17.
 *
 * Used to transform any VT opcode to the reference's opcode equivalent
 */
public class VTInstructionTransformer extends MethodTransformer {

    private final String className;
    private final String initDescriptor;
    private final boolean isValue;

    public VTInstructionTransformer(MethodTransformer mt, String name, String initDescriptor, boolean isValue) {
        super(mt);
        this.className = name;
        this.initDescriptor = initDescriptor;
        this.isValue = isValue;
    }

    @Override
    public void transform(MethodNode mn) {
        InsnList insns = mn.instructions;
        Iterator i = insns.iterator();
        while (i.hasNext()) {
            AbstractInsnNode i1 = (AbstractInsnNode) i.next();
            switch ((i1.getOpcode())) {
                case VLOAD :
                    ((VarInsnNode) i1).setOpcode(Opcodes.ALOAD);
                    break;
                case VSTORE :
                    ((VarInsnNode) i1).setOpcode(Opcodes.ASTORE);
                    break;
                case VNEW :
                    String desc = (isValue)?initDescriptor:((MethodInsnNode) i1).desc;
                    String targetClassName = (isValue)?className:getClassNameFromReturnTypeDescriptor(desc);
                    String targetDescriptor = (isValue)?initDescriptor:(desc.substring(0, desc.indexOf(')')+1))+"V";
                    Type[] methodArguments = Type.getMethodArguments(desc);
                    AbstractInsnNode tmp = i1;
                    for (int k = 0; k < methodArguments.length; k++) {
                        tmp = getInsnNode(tmp, AbstractInsnNode::getPrevious);
                    }
                    insns.insert(getInsnNode(tmp, AbstractInsnNode::getPrevious), new TypeInsnNode(Opcodes.NEW, targetClassName));
                    insns.insert(getInsnNode(tmp, AbstractInsnNode::getPrevious), new InsnNode(Opcodes.DUP));
                    insns.insert(i1, new MethodInsnNode(Opcodes.INVOKESPECIAL, targetClassName, "<init>", targetDescriptor, false));
                    insns.remove(i1);
                    break;
                case VRETURN :
                    insns.insert(i1, new InsnNode(Opcodes.ARETURN));
                    insns.remove(i1);
                    break;
                case VGETFIELD :
                    ((FieldInsnNode) i1).setOpcode(Opcodes.GETFIELD);
                    break;
                case INVOKESTATIC :
                    ((MethodInsnNode) i1).desc = VTClassVisitor.findAndTransformVtDesc(((MethodInsnNode) i1).desc );
                    break;
                case INVOKESPECIAL :
                    ((MethodInsnNode) i1).desc = VTClassVisitor.findAndTransformVtDesc(((MethodInsnNode) i1).desc);
                    if(((MethodInsnNode) i1).owner.equals("java/lang/__Value")){
                        ((MethodInsnNode) i1).owner = "java/lang/Object";
                    }
                    break;
                case INVOKEDIRECT :
                    ((MethodInsnNode) i1).desc = VTClassVisitor.findAndTransformVtDesc(((MethodInsnNode) i1).desc);
                    ((MethodInsnNode) i1).setOpcode(Opcodes.INVOKEVIRTUAL);
                    break;
                    //Appears when a object reference needs to initialize his VT attributes.
                case PUTFIELD :
                    ((FieldInsnNode)i1).desc = VTClassVisitor.transformVTDesc(((FieldInsnNode)i1).desc);
                    break;
                    //Appears when a object reference needs to get his VT attributes.
                case GETFIELD :
                    ((FieldInsnNode) i1).desc = VTClassVisitor.transformVTDesc(((FieldInsnNode) i1).desc);
                default:
                    break;
            }
        }
        super.transform(mn);
    }

    private static AbstractInsnNode getInsnNode(AbstractInsnNode insn, Function<AbstractInsnNode, AbstractInsnNode> fun) {
        do {
            insn = fun.apply(insn);
            if (insn != null && !(insn instanceof LineNumberNode)) {
                break;
            }
        } while (insn != null);
        return insn;
    }

    private static String getClassNameFromReturnTypeDescriptor(String desc) {
        return desc.substring(desc.indexOf(')')+2, desc.length()-1);
    }
}
