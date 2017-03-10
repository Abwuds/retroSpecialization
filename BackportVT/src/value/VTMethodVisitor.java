package value;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.*;
import static value.VTClassVisitor.transformVTDesc;

/**
 * Created by Fabien GIACHERIO on 08/02/17.
 *
 * Used to transform any VT opcode to the reference's opcode equivalent
 */
public class VTMethodVisitor extends MethodTransformer {

    private final Map<AbstractInsnNode, VTConsumer> transformations = new HashMap<>();
    private LocalVariablesSorter lvs;
    private MethodVisitor writer;

    private Map<String,Integer> registers = new HashMap<>();
    private String owner;

    public VTMethodVisitor(MethodTransformer mt, LocalVariablesSorter lvs, MethodVisitor writer, String owner) {
        super(mt);
        this.lvs = lvs;
        this.writer = writer;
        this.owner = owner;
    }

    @Override
    public void transform(MethodNode mn) {
        AbstractInsnNode[] insns = mn.instructions.toArray();
        Arrays.stream(insns).forEach(i -> transformations.put(i, VTMethodVisitor::defaultVTConsumer));

        try {
            analyze(mn);
        } catch (AnalyzerException e) {
            throw new IllegalStateException(e);
        }

        //Populate registers
        populateRegisters(mn);

        System.out.println("Rewriting ... " + owner+ " - " + mn.name + ":" + mn.desc);
        for (int i = 0; i < insns.length; ++i) {
            VTConsumer vtConsumer = transformations.getOrDefault(insns[i], VTMethodVisitor::defaultVTConsumer);
            vtConsumer.consume(mn, writer, insns[i], registers);
        }

        //Will be compute by the class writer
        writer.visitMaxs(0, 0);
        super.transform(mn);
    }

    private void populateRegisters(MethodNode mn) {
        VTClass vt = Rewriter.vtsLayout.get(owner);
        if(!mn.name.equals("<init>")) {
            //Decomposing the value into his own methods
            if((mn.access&ACC_STATIC)==0 && vt !=null){
                addLocalsForVT(writer, vt, 0);
            }
            int k =((mn.access&ACC_STATIC)==0)?1:0;
            Type[] methodArguments = Type.getArgumentTypes(mn.desc);
            for (Type t : methodArguments) {
                vt = Rewriter.vtsLayout.get(t.getClassName());
                if(vt != null) {
                    addLocalsForVT(writer, vt, k);
                }
                k+=t.getSize();
            }
        }
    }

    private static void defaultVTConsumer(MethodNode mn, MethodVisitor mv, AbstractInsnNode insn, Map<String, Integer> registers) {
        switch (insn.getOpcode()) {
            case VLOAD:
                mv.visitVarInsn(ALOAD, ((VarInsnNode) insn).var);
                break;
            case VSTORE:
                mv.visitVarInsn(ASTORE, ((VarInsnNode) insn).var);
                break;
            case VRETURN:
                mv.visitInsn(ARETURN);
                mn.instructions.set(insn, new InsnNode(Opcodes.ARETURN));
                break;
            case VGETFIELD:
                FieldInsnNode fieldInsnNode = (FieldInsnNode) insn;
                mv.visitFieldInsn(GETFIELD, fieldInsnNode.owner, fieldInsnNode.name, VTClassVisitor.transformVTDesc(fieldInsnNode.desc));
                break;
            case INVOKESTATIC:
                MethodInsnNode methodInsnNode = (MethodInsnNode) insn;
                mv.visitMethodInsn(INVOKESTATIC, methodInsnNode.owner, methodInsnNode.name, VTClassVisitor.findAndTransformVtDesc(methodInsnNode.desc), methodInsnNode.itf);
                break;
            case INVOKESPECIAL:
                methodInsnNode = (MethodInsnNode) insn;
                mv.visitMethodInsn(INVOKESPECIAL, methodInsnNode.owner.equals("java/lang/__Value")?"java/lang/Object":methodInsnNode.owner, methodInsnNode.name, VTClassVisitor.findAndTransformVtDesc(methodInsnNode.desc), methodInsnNode.itf);
                break;
            case INVOKEVIRTUAL:
            case INVOKEDIRECT:
                methodInsnNode = (MethodInsnNode) insn;
                mv.visitMethodInsn(INVOKEVIRTUAL, methodInsnNode.owner, methodInsnNode.name, VTClassVisitor.findAndTransformVtDesc(methodInsnNode.desc), methodInsnNode.itf);
                break;
            //Appears when a object reference needs to initialize his VT attributes.
            case PUTFIELD:
                fieldInsnNode = (FieldInsnNode) insn;
                mv.visitFieldInsn(PUTFIELD, fieldInsnNode.owner, fieldInsnNode.name, VTClassVisitor.transformVTDesc(fieldInsnNode.desc));
                break;
            //Appears when a object reference needs to get his VT attributes.
            case GETFIELD:
                fieldInsnNode = (FieldInsnNode) insn;
                mv.visitFieldInsn(GETFIELD, fieldInsnNode.owner, fieldInsnNode.name, VTClassVisitor.transformVTDesc(fieldInsnNode.desc));
                break;
            case GETSTATIC:
                fieldInsnNode = (FieldInsnNode) insn;
                mv.visitFieldInsn(GETSTATIC, fieldInsnNode.owner, fieldInsnNode.name, VTClassVisitor.transformVTDesc(fieldInsnNode.desc));
                break;
            case PUTSTATIC:
                fieldInsnNode = (FieldInsnNode) insn;
                mv.visitFieldInsn(PUTSTATIC, fieldInsnNode.owner, fieldInsnNode.name, VTClassVisitor.transformVTDesc(fieldInsnNode.desc));
                break;
            case VASTORE:
                mv.visitInsn(AASTORE);
                break;
            case VALOAD:
                mv.visitInsn(AALOAD);
                break;
            case VWITHFIELD:
                fieldInsnNode = (FieldInsnNode) insn;
                mv.visitFieldInsn(PUTFIELD, fieldInsnNode.owner, fieldInsnNode.name, VTClassVisitor.transformVTDesc(fieldInsnNode.desc));
                break;
            default:
                insn.accept(mv);
                break;
        }
    }

    private void analyze(MethodNode mn) throws AnalyzerException {
        AbstractInsnNode[] insns = mn.instructions.toArray();

        analyseBySourceValue(mn, insns);
        analyseByBasicValue(mn, insns);
    }

    private void analyseBySourceValue(MethodNode mn, AbstractInsnNode[] insns) throws AnalyzerException {
        Analyzer<SourceValue> a = new Analyzer<SourceValue>(new SourceInterpreter());
        a.analyze(owner, mn);
        Frame<SourceValue>[] frames = a.getFrames();
        for (int i = 0; i < frames.length; ++i) {
            switch (insns[i].getOpcode()) {
                case VNEW:
                    SourceValue sourceValue = frames[i].getStack(0);
                    MethodInsnNode methodInsnNode = (MethodInsnNode) insns[i];
                    VTClass vtTargetInsn = Rewriter.vtsLayout.get(Type.getReturnType(methodInsnNode.desc).getClassName());
                    if(sourceValue!=null) {
                        for(AbstractInsnNode k : sourceValue.insns) {
                            VTConsumer consumer = transformations.get(k);
                            transformations.put(k, (mn1, mv, insn, registers) -> {
                                mv.visitTypeInsn(NEW, owner);
                                mv.visitInsn(DUP);
                                if(consumer!=null) {
                                    consumer.consume( mn1, mv, insn, registers);
                                }
                            });
                        }
                        transformations.put(insns[i], (mn1, mv, insn, registers) -> {
                            //Add a default value for the extra boolean parameter of the constructor
                            mv.visitInsn(ACONST_NULL);
                            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, "<init>", vtTargetInsn.initDesc, false);
                        });
                    } else {
                        transformations.put(insns[i], (mn1, mv, insn, registers1) -> {
                            mv.visitTypeInsn(NEW, owner);
                            mv.visitInsn(DUP);
                            //Add a default value for the extra boolean parameter of the constructor
                            mv.visitInsn(ACONST_NULL);
                            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, "<init>", vtTargetInsn.initDesc, false);
                        });
                    }
                    break;
                case VGETFIELD:
                    sourceValue = frames[i].getStack(frames[i].getStackSize()-1);
                    if(sourceValue != null) {
                        for(AbstractInsnNode source : sourceValue.insns) {
                            if(source.getOpcode() == VLOAD) {
                                FieldInsnNode tmp = (FieldInsnNode)insns[i];
                                VarInsnNode varInsnNode = (VarInsnNode) source;
                                transformations.put(varInsnNode, (mn1, mv, insn, registers1) -> {
                                    //Do nothing in order to remove varInsn;
                                });
                                transformations.put(insns[i], (mn1, mv, insn, registers) -> {
                                    VTClass vt = Rewriter.vtsLayout.get(VTClassVisitor.transformVTDesc(tmp.owner));
                                    if(vt != null) {
                                        int targetOpcode;
                                        switch (VTClassVisitor.transformVTDesc(tmp.desc)) {
                                            case "B":
                                            case "C":
                                            case "Z":
                                            case "S":
                                            case "I":
                                                targetOpcode = Opcodes.ILOAD;
                                                break;
                                            case "J":
                                                targetOpcode = Opcodes.LLOAD;
                                                break;
                                            case "F":
                                                targetOpcode = Opcodes.FLOAD;
                                                break;
                                            case "D":
                                                targetOpcode = Opcodes.DLOAD;
                                                break;
                                            default:
                                                targetOpcode = Opcodes.ALOAD;
                                                break;
                                        }
                                        mv.visitVarInsn(targetOpcode, registers.get(vt.name + tmp.name + varInsnNode.var));
                                    }
                                });
                            }
                        }
                    } else {
                        transformations.put(insns[i], (mn1, mv, insn, registers) -> {
                            FieldInsnNode fieldInsnNode = (FieldInsnNode) insn;
                            mv.visitFieldInsn(GETFIELD, fieldInsnNode.owner, fieldInsnNode.name, VTClassVisitor.transformVTDesc(fieldInsnNode.desc));
                        });
                    }
                    break;
                case VRETURN :
                    final String returnTypeClassName = Type.getReturnType(mn.desc).getClassName();
                    VTClass vt = Rewriter.vtsLayout.get(returnTypeClassName);
                    sourceValue = frames[i].getStack(frames[i].getStackSize()-1);
                    sourceValue.insns.stream().forEach(c -> {
                        if(c instanceof VarInsnNode) {
                            transformations.put(c, (mn1, mv, insn, registers) -> {
                                VarInsnNode c1 = (VarInsnNode) c;
                                recomposeObject(mv, vt, c1.var);
                            });
                        }
                    });
                    transformations.put(insns[i], (mn1, mv, insn, registers1) -> {
                        mv.visitInsn(ARETURN);
                    });
                    break;
                case INVOKEDIRECT:
                case INVOKEVIRTUAL:
                    methodInsnNode = (MethodInsnNode) insns[i];
                    List<Type> targetMethodArgs = Arrays.stream(Type.getArgumentTypes(methodInsnNode.desc)).collect(Collectors.toList());
                    // InvokeVirtual
                    final VTClass vtOwner = Rewriter.vtsLayout.get(methodInsnNode.owner);
                    if(vtOwner != null){
                        sourceValue = frames[i].getStack(0);
                        for(AbstractInsnNode c : sourceValue.insns) {
                            if(c instanceof VarInsnNode) {
                                VTConsumer vtConsumer = transformations.get(c);
                                transformations.put(c, (mn1, mv, insn, registers) -> {
                                    if(isDecomposed(vtOwner, ((VarInsnNode) c).var)) {
                                        recomposeObject(mv, vtOwner, ((VarInsnNode) c).var);
                                    } else {
                                        vtConsumer.consume(mn1, mv, insn, registers);
                                    }
                                });
                            } else {
                                //TODO Do something
//                                throw new IllegalStateException("VarInsnNode required");
                            }
                        }
                    }
                    for (Type t : targetMethodArgs) {
                        VTClass vtTarget = Rewriter.vtsLayout.get(t.getClassName());
                        if (vtTarget != null) {
                            SourceValue sc = frames[i].getStack(targetMethodArgs.indexOf(t)+1);
                            sc.insns.forEach(c -> {
                                if(c instanceof VarInsnNode) {
                                    VTConsumer vtConsumer = transformations.get(c);
                                    transformations.put(c, (mn1, mv, insn, registers1) -> {
                                        if(isDecomposed(vtTarget, ((VarInsnNode) c).var)){
                                            recomposeObject(mv, vtTarget, ((VarInsnNode) c).var);
                                        }
                                        else {
                                            vtConsumer.consume(mn1, mv, insn, registers1);
                                        }
                                    });
                                } else {
                                    //TODO Do something
//                                    throw new IllegalStateException("VarInsnNode required");
                                }
                            });
                        }
                    }
                    break;
                case INVOKESTATIC :
                    methodInsnNode = (MethodInsnNode) insns[i];
                    targetMethodArgs = Arrays.stream(Type.getArgumentTypes(methodInsnNode.desc)).collect(Collectors.toList());
                    for (Type t : targetMethodArgs) {
                        VTClass vtTarget = Rewriter.vtsLayout.get(t.getClassName());
                        if (vtTarget != null) {
                            SourceValue sc = frames[i].getStack(targetMethodArgs.indexOf(t));
                            sc.insns.forEach(c -> {
                                if(c instanceof VarInsnNode) {
                                    VTConsumer vtConsumer = transformations.get(c);
                                    transformations.put(c, (mn1, mv, insn, registers1) -> {
                                        int var = ((VarInsnNode) c).var;
                                        if(isDecomposed(vtTarget, var)) {
                                            recomposeObject(mv, vtTarget, var);
                                        } else {
                                            vtConsumer.consume(mn1, mv, insn, registers1);
                                        }
                                    });
                                } else {
                                    //TODO Do something
//                                    throw new IllegalStateException("VarInsnNode required");
                                }
                            });
                        }
                    }
                    break;
                case VWITHFIELD:
                    FieldInsnNode insn = (FieldInsnNode) insns[i];
                    SourceValue sc = frames[i].getStack(0);
                    int targetIndex=0;
                    for(AbstractInsnNode tmp : sc.insns) {
                        if(tmp instanceof VarInsnNode) {
                            targetIndex = ((VarInsnNode) tmp).var;
                            transformations.put(tmp, (mn1, mv, insn1, registers1) -> {
                                // Do nothing in order to remove varInsn
                            });
                        }//TODO Do something
                    }
                    final int finalIndex = targetIndex;
                    VTClass vtTarget = Rewriter.vtsLayout.get(insn.owner);
                    transformations.put(insns[i], (mn1, mv, insn1, registers1) -> {
                       switch (VTClassVisitor.transformVTDesc(insn.desc)) {
                           case "B":
                           case "C":
                           case "Z":
                           case "S":
                           case "I":
                               mv.visitVarInsn(ISTORE, registers1.get(vtTarget.name+ insn.name+ finalIndex));
                               break;
                           case "J":
                               mv.visitVarInsn(LSTORE, registers1.get(vtTarget.name+ insn.name+ finalIndex));
                               break;
                           case "F":
                               mv.visitVarInsn(FSTORE, registers1.get(vtTarget.name+ insn.name+ finalIndex));
                               break;
                           case "D":
                               mv.visitVarInsn(DSTORE, registers1.get(vtTarget.name+ insn.name+ finalIndex));
                               break;
                           default:
                               mv.visitVarInsn(ASTORE, registers1.get(vtTarget.name+ insn.name+ finalIndex));
                               break;
                       }
                        //TODO I think that a recomposition is not necessary, we should remove the ASTORE instruction.
                       recomposeObject(mv, vtTarget, finalIndex);
                    });
                    break;
                case VDEFAULT :
                    TypeInsnNode typeInsnNode = (TypeInsnNode) insns[i];
                    VTClass vtClass = Rewriter.vtsLayout.get(typeInsnNode.desc);
                    transformations.put(insns[i], (mn1, mv, insn1, registers1) -> {
                       mv.visitTypeInsn(NEW, typeInsnNode.desc);
                       mv.visitInsn(DUP);
                       mv.visitMethodInsn(Opcodes.INVOKESPECIAL, vtClass.name, "<init>", "()V", false);
                    });
                    break;
                default :
                    break;
            }
        }
    }

    private void analyseByBasicValue(MethodNode mn, AbstractInsnNode[] insns) throws AnalyzerException {
        Analyzer<BasicValue> a2 = new Analyzer<>(new VTInterpreter());
        a2.analyze(owner, mn);

        Frame<BasicValue>[] frames2 = a2.getFrames();
        for(int i=0; i< insns.length; i++) {
            if(insns[i].getOpcode() == VSTORE) {
                VarInsnNode varInsnNode = (VarInsnNode) insns[i];
                BasicValue stackValue = frames2[i].getStack(frames2[i].getStackSize()-1);
                final int k = i;
                transformations.put(insns[i], ((mn1, mv, insn, registers1) -> {
                    VTClass vt = Rewriter.vtsLayout.get(stackValue.getType().getClassName());
                    mv.visitVarInsn(ASTORE, ((VarInsnNode) insn).var);
                    if(vt != null) {
                        if (!isDecomposed(vt, varInsnNode.var)) {
                            addLocalsForVT(mv, vt, varInsnNode.var);
                        } else {
                            decomposeObject(mv, vt, varInsnNode.var);
                        }
                    }
                }));
            }
        }
    }

    private void decomposeObject(MethodVisitor mv, VTClass vt, int index) {
        mv.visitVarInsn(ALOAD, index);
        vt.fields.forEach(fn -> {
            mv.visitInsn(DUP);
            mv.visitFieldInsn(Opcodes.GETFIELD, vt.name, fn.name, transformVTDesc(fn.desc));
            switch (fn.desc) {
                case "B":
                case "C":
                case "S":
                case "Z":
                case "I":
                    mv.visitVarInsn(Opcodes.ISTORE, registers.get(vt.name+fn.name+String.valueOf(index)));
                    break;
                case "J":
                    mv.visitVarInsn(Opcodes.LSTORE, registers.get(vt.name+fn.name+String.valueOf(index)));
                    break;
                case "F":
                    mv.visitVarInsn(Opcodes.FSTORE, registers.get(vt.name+fn.name+String.valueOf(index)));
                    break;
                case "D":
                    mv.visitVarInsn(Opcodes.DSTORE, registers.get(vt.name+fn.name+String.valueOf(index)));
                    break;
                default:
                    mv.visitVarInsn(Opcodes.ASTORE, registers.get(vt.name+fn.name+String.valueOf(index)));
                    decomposeObject(mv, Rewriter.vtsLayout.get(Type.getType(fn.desc).getClassName()), registers.get(vt.name+fn.name+String.valueOf(index)));
                    break;
            }
        });
        mv.visitInsn(POP);
    }

    private void recomposeObject(MethodVisitor mv, VTClass vt, int index) {
        mv.visitTypeInsn(NEW, vt.name);
        mv.visitInsn(DUP);
        for (FieldNode fn : vt.fields) {
            switch (fn.desc) {
                case "B":
                case "C":
                case "S":
                case "I":
                case "Z":
                    mv.visitVarInsn(ILOAD,registers.get(vt.name + fn.name + String.valueOf(index)));
                    break;
                case "J":
                    mv.visitVarInsn(LLOAD,registers.get(vt.name + fn.name + String.valueOf(index)));
                    break;
                case "F":
                    mv.visitVarInsn(FLOAD,registers.get(vt.name + fn.name + String.valueOf(index)));
                    break;
                case "D":
                    mv.visitVarInsn(DLOAD,registers.get(vt.name + fn.name + String.valueOf(index)));
                    break;
                default:
                    recomposeObject(mv, Rewriter.vtsLayout.get(Type.getType(fn.desc).getClassName()), registers.get(vt.name + fn.name + String.valueOf(index)));
                    break;
            }
        }
        //Add a default value for the extra boolean parameter of the constructor
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, vt.name, "<init>", vt.initDesc, false);
    }

    private void addLocalsForVT(MethodVisitor mv, VTClass vt, int index) {
        mv.visitVarInsn(ALOAD, index);
        final int finalK = index;
        vt.fields.stream().forEach(fn -> {
            mv.visitInsn(DUP);
            int indexNewLocal = lvs.newLocal(Type.getType(transformVTDesc(fn.desc)));
            registers.put(vt.name+fn.name+String.valueOf(finalK), indexNewLocal);
            mv.visitFieldInsn(GETFIELD, vt.name, fn.name, transformVTDesc(fn.desc));
            switch (transformVTDesc(fn.desc)) {
                case "B":
                case "C":
                case "S":
                case "I":
                case "Z":
                    mv.visitVarInsn(ISTORE, indexNewLocal);
                    break;
                case "J":
                    mv.visitVarInsn(LSTORE, indexNewLocal);
                    break;
                case "F":
                    mv.visitVarInsn(FSTORE, indexNewLocal);
                    break;
                case "D":
                    mv.visitVarInsn(DSTORE, indexNewLocal);
                    break;
                default:
                    mv.visitVarInsn(ASTORE, indexNewLocal);
                    VTClass embbededVT = Rewriter.vtsLayout.get(Type.getType(transformVTDesc(fn.desc)).getClassName());
                    if(embbededVT == null) throw new AssertionError("Error the descriptor " + fn.desc + " should refer to another VT");
                    addLocalsForVT(mv, embbededVT, indexNewLocal);
                    break;
            }
        });
        mv.visitInsn(POP);
    }

    private boolean isDecomposed(VTClass vt, int index) {
        return vt.fields.isEmpty()?false:(registers.get(vt.name+vt.fields.get(0).name+String.valueOf(index))!=null);
    }
}

