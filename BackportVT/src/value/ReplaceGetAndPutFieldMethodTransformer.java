package value;


import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.Map;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.GETFIELD;

/**
 * Created by Fabien GIACHERIO on 20/02/17.
 */
public class ReplaceGetAndPutFieldMethodTransformer extends MethodTransformer {

    private Map<String, Integer> map;

    public ReplaceGetAndPutFieldMethodTransformer(MethodTransformer mt, Map<String, Integer> map) {
        super(mt);
        this.map = map;
    }

    @Override
    public void transform(MethodNode mn) {
        //TODO Refactor
        AbstractInsnNode[] insns = mn.instructions.toArray();
        for (int i = 0; i < insns.length; ++i) {
            if (insns[i].getOpcode() == ALOAD) {
                if (insns[i].getNext().getOpcode() == GETFIELD) {
                    FieldInsnNode next = (FieldInsnNode) insns[i].getNext();
                    Integer var = map.get(next.owner + next.name);
                    if (var != null) {
                        mn.instructions.remove(next.getPrevious());
                        int targetOpcode;
                        switch (next.desc) {
                            case "B":
                            case "C":
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
                        mn.instructions.set(next, new VarInsnNode(targetOpcode, var));
                    }
                } else if((insns[i].getNext().getNext()!=null) && insns[i].getNext().getNext().getOpcode() == Opcodes.PUTFIELD) {
                    FieldInsnNode next = (FieldInsnNode) insns[i].getNext().getNext();
                    Integer var = map.get(next.owner + next.name);
                    if (var != null) {
                        mn.instructions.remove(insns[i]);
                        int targetOpcode;
                        switch (next.desc) {
                            case "B":
                            case "C":
                            case "S":
                            case "I":
                                targetOpcode = Opcodes.ISTORE;
                                break;
                            case "J":
                                targetOpcode = Opcodes.LSTORE;
                                break;
                            case "F":
                                targetOpcode = Opcodes.FSTORE;
                                break;
                            case "D":
                                targetOpcode = Opcodes.DSTORE;
                                break;
                            default:
                                targetOpcode = Opcodes.ASTORE;
                                break;
                        }
                        mn.instructions.set(next, new VarInsnNode(targetOpcode, var));
                    }
                }
            }
        }
        super.transform(mn);
    }
}