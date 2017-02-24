package value;


import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.util.Map;

import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.PUTFIELD;

/**
 * Created by Fabien GIACHERIO on 20/02/17.
 */
public class ReplaceGetAndPutFieldMethodTransformer extends MethodTransformer {

    private Map<String, Integer> map;
    private String owner;
    private int nbInstrAdded;

    public ReplaceGetAndPutFieldMethodTransformer(MethodTransformer mt, Map<String, Integer> map, String owner, int nbInstrAdded) {
        super(mt);
        this.map = map;
        this.owner = owner;
        this.nbInstrAdded = nbInstrAdded;
    }

    @Override
    public void transform(MethodNode mn) {
        Analyzer<SourceValue> a = new Analyzer<SourceValue>(new VTBasicInterpreter());
        try {
            a.analyze(owner, mn);
            AbstractInsnNode[] insns = mn.instructions.toArray();
            Frame<SourceValue>[] frames = a.getFrames();
            for (int i = nbInstrAdded; i < frames.length; ++i) {
                if(insns[i].getOpcode() == GETFIELD) {
                    FieldInsnNode insn = (FieldInsnNode)insns[i];
                    VTClass vt = Rewriter.vtsLayout.get(insn.owner);
                    if(vt != null) {
                        SourceValue source = frames[i].getStack(frames[i].getStackSize() - 1);
                        source.insns.stream().forEach(s -> {
                            //TODO A revoir si on rencontre un DUP
                            removeFieldInsn(mn, insn, ((VarInsnNode)s).var, s);
                        });
                    }
                } else if(insns[i].getOpcode() == PUTFIELD) {
                    FieldInsnNode insn = (FieldInsnNode)insns[i];
                    VTClass vt = Rewriter.vtsLayout.get(insn.owner);
                    if(vt != null) {
                        SourceValue source = frames[i].getStack(frames[i].getStackSize() - 2);
                        source.insns.stream().forEach(s -> {
                            removeFieldInsn(mn, insn, ((VarInsnNode)s).var, s);
                        });
                    }
                }
            }
        } catch (AnalyzerException e) {
            e.printStackTrace();
        }
        super.transform(mn);
    }

    private void removeFieldInsn(MethodNode mn, FieldInsnNode next, int index, AbstractInsnNode s) {
        Integer var = map.get(next.owner + next.name + String.valueOf(index));
        if (var != null) {
            int targetOpcode;
            switch (next.desc) {
                case "B":
                case "C":
                case "S":
                case "I":
                    targetOpcode = next.getOpcode()==GETFIELD?Opcodes.ILOAD:Opcodes.ISTORE;
                    break;
                case "J":
                    targetOpcode = next.getOpcode()==GETFIELD?Opcodes.LLOAD:Opcodes.LSTORE;
                    break;
                case "F":
                    targetOpcode = next.getOpcode()==GETFIELD?Opcodes.FLOAD:Opcodes.FSTORE;
                    break;
                case "D":
                    targetOpcode = next.getOpcode()==GETFIELD?Opcodes.DLOAD:Opcodes.DSTORE;
                    break;
                default:
                    targetOpcode = next.getOpcode()==GETFIELD?Opcodes.ALOAD:Opcodes.ASTORE;
                    break;
            }
            mn.instructions.set(next, new VarInsnNode(targetOpcode, var));
            mn.instructions.remove(s);
        }
    }
}