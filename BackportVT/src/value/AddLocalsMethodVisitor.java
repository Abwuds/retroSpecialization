package value;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.ASM5;
import static value.VTClassVisitor.transformVTDesc;

/**
 * Created by Fabien GIACHERIO on 20/02/17.
 */
public class AddLocalsMethodVisitor extends MethodVisitor {

    LocalVariablesSorter lvs;
    AnalyzerAdapter aa;
    private String desc;

    private Map<String,Integer> map;
    private int maxStack;

    private MethodVisitor next;

    public AddLocalsMethodVisitor(MethodVisitor mv, int access, String name, String desc, String signature, String[] exceptions) {
        super(ASM5, new MethodNode(access, name, desc, signature, exceptions));
        this.desc = desc;
        this.map = new HashMap<>();
        this.next = mv;
    }

    @Override
    public void visitCode() {
        for (VTClass vt : Rewriter.vts) {
            Type[] methodArguments = Type.getArgumentTypes(desc);
            int k = 0;
            for (Type t : methodArguments) {
                if (t.getClassName().equals(vt.name)) {
                    mv.visitVarInsn(Opcodes.ALOAD, k);
                    vt.fields.stream().forEach(fn -> {
                        mv.visitInsn(Opcodes.DUP);
                        int index = lvs.newLocal(Type.getType(transformVTDesc(fn.desc)));
                        map.put(vt.name+fn.name, index);
                        mv.visitFieldInsn(Opcodes.GETFIELD, vt.name, fn.name, transformVTDesc(fn.desc));
                        switch (fn.desc) {
                            case "B":
                            case "C":
                            case "S":
                            case "I":
                                mv.visitVarInsn(Opcodes.ISTORE, index);
                                break;
                            case "J":
                                mv.visitVarInsn(Opcodes.LSTORE, index);
                                break;
                            case "F":
                                mv.visitVarInsn(Opcodes.FSTORE, index);
                                break;
                            case "D":
                                mv.visitVarInsn(Opcodes.DSTORE, index);
                                break;
                            default:
                                mv.visitVarInsn(Opcodes.ASTORE, index);
                                break;
                        }
                    });
                    mv.visitInsn(Opcodes.POP);
                    maxStack = 3;
                }
                k+=t.getSize();
            }
        }
        super.visitCode();
    }

    @Override
    public void visitEnd() {
        MethodNode mn = (MethodNode) mv;
        ReplaceGetAndPutFieldMethodTransformer removeGetFieldMethodTransformer = new ReplaceGetAndPutFieldMethodTransformer(null, map);
        removeGetFieldMethodTransformer.transform(mn);
        mn.accept(next);
        super.visitEnd();
    }

    @Override public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitMaxs(Math.max(this.maxStack, maxStack), maxLocals);
    }
}
