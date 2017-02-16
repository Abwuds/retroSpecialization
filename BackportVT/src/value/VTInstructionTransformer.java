package value;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

/**
 * Created by Fabien GIACHERIO on 08/02/17.
 *
 * Used to transform any VT opcode to the reference's opcode equivalent
 */
public class VTInstructionTransformer extends MethodTransformer {

    private final String owner;
    private final String initDescriptor;

    private Map<String,Integer> map;

    public VTInstructionTransformer(MethodTransformer mt, String name, String initDescriptor) {
        super(mt);
        this.owner = name;
        this.initDescriptor = initDescriptor;
        this.map = new HashMap<>();
    }

    @Override
    public void transform(MethodNode mn) {
        Analyzer<BasicValue> a =
                new Analyzer<BasicValue>(new VTBasicInterpreter());
        try {
            a.analyze(owner, mn);
            Frame<BasicValue>[] frames = a.getFrames();
            AbstractInsnNode[] insns = mn.instructions.toArray();
            for (int i = 0; i < frames.length; ++i) {
                switch (insns[i].getOpcode()) {
                    case VLOAD :
                        ((VarInsnNode) insns[i]).setOpcode(Opcodes.ALOAD);
                        break;
                    case VSTORE :
                        ((VarInsnNode) insns[i]).setOpcode(Opcodes.ASTORE);
                        break;
                    case VNEW :
                        String desc = initDescriptor;
                        String targetClassName = owner;
                        String targetDescriptor = initDescriptor;
                        org.objectweb.asm.Type[] methodArguments = org.objectweb.asm.Type.getArgumentTypes(desc);
                        int k=i;
                        while (frames[k].getStackSize() != (frames[i].getStackSize()-methodArguments.length)) {
                            k--;
                        }
                        mn.instructions.insert(insns[k].getPrevious(), new TypeInsnNode(Opcodes.NEW, targetClassName));
                        mn.instructions.insert(insns[k].getPrevious(), new InsnNode(Opcodes.DUP));
                        mn.instructions.set(insns[i],  new MethodInsnNode(Opcodes.INVOKESPECIAL, targetClassName, "<init>", targetDescriptor, false));
                        mn.maxStack+=2;
                        break;
                    case VRETURN :
                        mn.instructions.set(insns[i], new InsnNode(Opcodes.ARETURN));
                        break;
                    case VGETFIELD :
                        ((FieldInsnNode) insns[i]).setOpcode(Opcodes.GETFIELD);
                        break;
                    case INVOKESTATIC :
                        ((MethodInsnNode) insns[i]).desc = VTClassVisitor.findAndTransformVtDesc(((MethodInsnNode) insns[i]).desc );
                        break;
                    case INVOKESPECIAL :
                        ((MethodInsnNode) insns[i]).desc = VTClassVisitor.findAndTransformVtDesc(((MethodInsnNode) insns[i]).desc);
                        if(((MethodInsnNode) insns[i]).owner.equals("java/lang/__Value")){
                            ((MethodInsnNode) insns[i]).owner = "java/lang/Object";
                        }
                        break;
                    case INVOKEDIRECT :
                        ((MethodInsnNode) insns[i]).desc = VTClassVisitor.findAndTransformVtDesc(((MethodInsnNode) insns[i]).desc);
                        ((MethodInsnNode) insns[i]).setOpcode(Opcodes.INVOKEVIRTUAL);
                        break;
                    //Appears when a object reference needs to initialize his VT attributes.
                    case PUTFIELD :
                        ((FieldInsnNode)insns[i]).desc = VTClassVisitor.transformVTDesc(((FieldInsnNode)insns[i]).desc);
                        break;
                    //Appears when a object reference needs to get his VT attributes.
                    case GETFIELD :
                        ((FieldInsnNode) insns[i]).desc = VTClassVisitor.transformVTDesc(((FieldInsnNode) insns[i]).desc);
                        break;
                    case VASTORE :
                        mn.instructions.set(insns[i], new InsnNode(Opcodes.AASTORE));
                        break;
                    case VALOAD :
                        mn.instructions.set(insns[i], new InsnNode(Opcodes.AALOAD));
                        break;
                    default:
                        break;
                }
            }
        } catch (AnalyzerException e) {
            e.printStackTrace();
        }
        super.transform(mn);
    }
}
