package value;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;

import static org.objectweb.asm.Opcodes.*;

/**
 * Created by Fabien GIACHERIO on 08/02/17.
 *
 * Used to transform any VT opcode to the reference's opcode equivalent
 */
public class VTInstructionTransformer extends MethodTransformer {

    private VTMethodAdapter adapter;


    public VTInstructionTransformer(MethodTransformer mt, VTMethodAdapter adapter) {
        super(mt);
        this.adapter = adapter;
    }

    @Override
    public void transform(MethodNode mn) {
        try {
            Analyzer<SourceValue> a = new Analyzer<SourceValue>(new VTBasicInterpreter());
            a.analyze(adapter.owner, mn);
            Frame<SourceValue>[] frames = a.getFrames();
            AbstractInsnNode[] insns = mn.instructions.toArray();
            for (int i = 0; i < frames.length; ++i) {
                switch (insns[i].getOpcode()) {
                    case VLOAD:
                        ((VarInsnNode) insns[i]).setOpcode(Opcodes.ALOAD);
                        break;
                    case VSTORE:
                        ((VarInsnNode) insns[i]).setOpcode(Opcodes.ASTORE);
                        break;
                    case VNEW:
                        Type[] methodArguments = Type.getArgumentTypes(adapter.initDescriptor);
                        SourceValue sourceValue = frames[i].getStack(frames[i].getStackSize() - methodArguments.length);
                        sourceValue.insns.stream().forEach(c -> {
                            mn.instructions.insert(c.getPrevious(), new TypeInsnNode(Opcodes.NEW, adapter.owner));
                            mn.instructions.insert(c.getPrevious(), new InsnNode(Opcodes.DUP));
                        });
                        mn.instructions.set(insns[i], new MethodInsnNode(Opcodes.INVOKESPECIAL, adapter.owner, "<init>", adapter.initDescriptor, false));
                        break;
                    case VRETURN:
                        mn.instructions.set(insns[i], new InsnNode(Opcodes.ARETURN));
                        break;
                    case VGETFIELD:
                        ((FieldInsnNode) insns[i]).setOpcode(Opcodes.GETFIELD);
                        ((FieldInsnNode) insns[i]).desc = VTClassVisitor.transformVTDesc(((FieldInsnNode) insns[i]).desc);
                        break;
                    case INVOKESTATIC:
                        ((MethodInsnNode) insns[i]).desc = VTClassVisitor.findAndTransformVtDesc(((MethodInsnNode) insns[i]).desc);
                        break;
                    case INVOKESPECIAL:
                        ((MethodInsnNode) insns[i]).desc = VTClassVisitor.findAndTransformVtDesc(((MethodInsnNode) insns[i]).desc);
                        if (((MethodInsnNode) insns[i]).owner.equals("java/lang/__Value")) {
                            ((MethodInsnNode) insns[i]).owner = "java/lang/Object";
                        }
                        break;
                    case INVOKEDIRECT:
                        ((MethodInsnNode) insns[i]).desc = VTClassVisitor.findAndTransformVtDesc(((MethodInsnNode) insns[i]).desc);
                        ((MethodInsnNode) insns[i]).setOpcode(Opcodes.INVOKEVIRTUAL);
                        break;
                    //Appears when a object reference needs to initialize his VT attributes.
                    case PUTFIELD:
                        ((FieldInsnNode) insns[i]).desc = VTClassVisitor.transformVTDesc(((FieldInsnNode) insns[i]).desc);
                        break;
                    //Appears when a object reference needs to get his VT attributes.
                    case GETFIELD:
                        ((FieldInsnNode) insns[i]).desc = VTClassVisitor.transformVTDesc(((FieldInsnNode) insns[i]).desc);
                        break;
                    case VASTORE:
                        mn.instructions.set(insns[i], new InsnNode(Opcodes.AASTORE));
                        break;
                    case VALOAD:
                        mn.instructions.set(insns[i], new InsnNode(Opcodes.AALOAD));
                        break;
                    case VWITHFIELD:
                        ((FieldInsnNode) insns[i]).setOpcode(Opcodes.PUTFIELD);
                        mn.instructions.remove(insns[i].getNext());
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

