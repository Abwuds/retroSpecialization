package value;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.*;
import static value.VTClassVisitor.transformVTDesc;

/**
 * Created by Fabien GIACHERIO on 21/02/17.
 *
 * This transformer is used to flatten data or rebox it.
 *
 * Case :
 *  -ARETURN : If the return type of the current method is a VT then :
 *              reboxing datas over all VarInsnNode on the stacksize -1 and then remove the VarInsnNode.
 *              break;
 *  -INVOKEVIRTUAL: If the method instruction's owner is a VT then :
 *              reboxing datas over all AbstractInsnNode on the stacksize - (targetMethodArgsSize+1) then :
 *
 *  -INVOKESTATIC: for each args, if it's a VT then :
 *              reboxing datas and removing the VarInsnNode
 *
 *              Finally if the return type is a VT then :
 *              decomposing VT after the storage of this one.
 */
public class RecomposeObjectMethodTransformer extends MethodTransformer{

    private Map<String, Integer> map;
    private String owner;

    public RecomposeObjectMethodTransformer(MethodTransformer mt, Map<String,Integer> map, String owner) {
        super(mt);
        this.map = map;
        this.owner = owner;
    }

    @Override
    public void transform(MethodNode mn) {
        final String returnTypeClassName = Type.getReturnType(mn.desc).getClassName();
        Analyzer<SourceValue> a = new Analyzer<SourceValue>(new VTBasicInterpreter());
        try {
            a.analyze(owner, mn);
            Frame<SourceValue>[] frames = a.getFrames();
            AbstractInsnNode[] insns = mn.instructions.toArray();
            for (int i = 0; i < frames.length; ++i) {
                switch (insns[i].getOpcode()) {
                    case Opcodes.ARETURN:
                        VTClass vt = Rewriter.vtsLayout.get(returnTypeClassName);
                        if(vt!=null) {
                                SourceValue sourceValue = frames[i].getStack(frames[i].getStackSize() - 1);
                                sourceValue.insns.stream().forEach(c -> {
                                    if(c instanceof VarInsnNode) {
                                        VarInsnNode c1 = (VarInsnNode) c;
                                        if(recomposeObject(mn, c, vt, c1.var)) {
                                            mn.instructions.remove(c);
                                        }
                                    }
                                });
                        }
                        break;
                    case Opcodes.INVOKEVIRTUAL:
                    case Opcodes.INVOKESTATIC:
                        MethodInsnNode insn = (MethodInsnNode) insns[i];
                        List<Type> targetMethodArgs = Arrays.stream(Type.getArgumentTypes(insn.desc)).collect(Collectors.toList());
                        VTClass vtOwner = Rewriter.vtsLayout.get(insn.owner);
                        int typePosition = 0;
                        if(insn.getOpcode() == INVOKEVIRTUAL) {
                            if(vtOwner != null){
                                SourceValue sourceValue = frames[i].getStack(frames[i].getStackSize() - (targetMethodArgs.size()+1));
                                boolean recomposed = false;
                                for(AbstractInsnNode c : sourceValue.insns) {
                                    if (recomposeObject(mn, c , vtOwner, 0)) mn.instructions.remove(c);
                                }
                                //TODO ??
//                                if (recomposed) decomposeObject(mn, vtOwner, insns[i], 0);
                            }
                            typePosition = 1;
                        }

                        for(Type t : targetMethodArgs) {
                            VTClass vtTarget = Rewriter.vtsLayout.get(t.getClassName());
                            if(vtTarget != null) {
                                SourceValue sourceValue = frames[i].getStack(frames[i].getStackSize() - (targetMethodArgs.size() - targetMethodArgs.indexOf(t)));
                                for (AbstractInsnNode c : sourceValue.insns) {
                                    if(c instanceof VarInsnNode) {
                                        if (recomposeObject(mn, c, vtTarget, typePosition)) {
                                            mn.instructions.remove(c);
                                        }
                                    }
                                }
                            }
                            typePosition += t.getSize();
                        }

                        VTClass vtReturned = Rewriter.vtsLayout.get(Type.getReturnType(insn.desc).getClassName());
                        if (vtReturned != null) {
                            //TODO A REVOIR
                            if(insns[i].getNext().getOpcode() == ASTORE) {
                                decomposeObject(mn, vtReturned, insns[i].getNext(), ((VarInsnNode)insns[i].getNext()).var);
                            }
                        }
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

    private void decomposeObject(MethodNode mn, VTClass vt, AbstractInsnNode insn, int index) {
        if(map.get(vt.name+vt.fields.get(0).name+String.valueOf(index)) == null) return;
        mn.instructions.insert(insn,new InsnNode(Opcodes.POP));
        vt.fields.stream().forEach(fn -> {
            switch (fn.desc) {
                case "B":
                case "C":
                case "S":
                case "I":
                    mn.instructions.insert(insn,new VarInsnNode(Opcodes.ISTORE, map.get(vt.name+fn.name+String.valueOf(index))));
                    break;
                case "J":
                    mn.instructions.insert(insn,new VarInsnNode(Opcodes.LSTORE, map.get(vt.name+fn.name+String.valueOf(index))));
                    break;
                case "F":
                    mn.instructions.insert(insn,new VarInsnNode(Opcodes.FSTORE, map.get(vt.name+fn.name+String.valueOf(index))));
                    break;
                case "D":
                    mn.instructions.insert(insn,new VarInsnNode(Opcodes.DSTORE, map.get(vt.name+fn.name+String.valueOf(index))));
                    break;
                default:
                    mn.instructions.insert(insn,new VarInsnNode(Opcodes.ASTORE, map.get(vt.name+fn.name+String.valueOf(index))));
                    decomposeObject(mn, Rewriter.vtsLayout.get(Type.getType(fn.desc).getClassName()), insn,map.get(vt.name+fn.name+String.valueOf(index)));
                    break;
            }
            mn.instructions.insert(insn, new FieldInsnNode(Opcodes.GETFIELD, vt.name, fn.name, transformVTDesc(fn.desc)));
            mn.instructions.insert(insn, new InsnNode(Opcodes.DUP));
        });
        mn.instructions.insert(insn, new VarInsnNode(Opcodes.ALOAD, index));


    }

    private boolean recomposeObject(MethodNode mn, AbstractInsnNode insn, VTClass vt, int index) {
        //TODO Should change when all VTs will be flattened. (Not only methods args).
        if(map.get(vt.name+vt.fields.get(0).name+String.valueOf(index)) == null) return false;
        mn.instructions.insert(insn.getPrevious(), new TypeInsnNode(Opcodes.NEW, vt.name));
        mn.instructions.insert(insn.getPrevious(), new InsnNode(Opcodes.DUP));
        for (FieldNode fn : vt.fields) {
            switch (fn.desc) {
                case "B":
                case "C":
                case "S":
                case "I":
                    mn.instructions.insert(insn.getPrevious(), new VarInsnNode(ILOAD, map.get(vt.name + fn.name + String.valueOf(index))));
                    break;
                case "J":
                    mn.instructions.insert(insn.getPrevious(), new VarInsnNode(LLOAD, map.get(vt.name + fn.name + String.valueOf(index))));
                    break;
                case "F":
                    mn.instructions.insert(insn.getPrevious(), new VarInsnNode(FLOAD, map.get(vt.name + fn.name + String.valueOf(index))));
                    break;
                case "D":
                    mn.instructions.insert(insn.getPrevious(), new VarInsnNode(DLOAD, map.get(vt.name + fn.name + String.valueOf(index))));
                    break;
                default:
                    recomposeObject(mn, insn, Rewriter.vtsLayout.get(Type.getType(fn.desc).getClassName()), map.get(vt.name + fn.name + String.valueOf(index)));
                    break;
            }
        }
        mn.instructions.insert(insn.getPrevious(), new MethodInsnNode(Opcodes.INVOKESPECIAL, vt.name, "<init>", vt.initDesc, false));
        return true;
    }
}
