package value;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ASM5;
import static value.VTClassVisitor.transformVTDesc;

/**
 * Created by Fabien GIACHERIO on 20/02/17.
 *
 * This Method visitor adds new locals per VT.
 * It registers the index of the new local into a map provide to the next visitors.
 *
 * Key : valuetype.name + fieldnode.name + indexVT
 */
public class AddLocalsMethodVisitor extends MethodVisitor {

    LocalVariablesSorter lvs;
    AnalyzerAdapter aa;
    private String desc;
    private String owner;
    private int access;

    private Map<String,Integer> map;
    private int maxStack;

    private int nbInstrAdded;

    private MethodVisitor next;

    public AddLocalsMethodVisitor(MethodVisitor mv, int access, String name, String desc, String signature, String[] exceptions, String owner) {
        super(ASM5, new MethodNode(access, name, desc, signature, exceptions));
//        super(API, mv);
        this.desc = desc;
        this.map = new HashMap<>();
        this.next = mv;
        this.owner = owner;
        this.nbInstrAdded =0;
        this.access=access;
    }

    @Override
    public void visitCode() {
        Type[] methodArguments = Type.getArgumentTypes(desc);
        int k = 0;

        //Decomposing the value into his own methods
        VTClass vt = Rewriter.vtsLayout.get(owner);
        if((access&ACC_STATIC)==0 && vt !=null){
            addLocalsForVT(vt, k);
            k=1;
        }
        for (Type t : methodArguments) {
            vt = Rewriter.vtsLayout.get(t.getClassName());
            if(vt != null) {
                addLocalsForVT(vt, k);
            }
            k+=t.getSize();
        }
        super.visitCode();
    }

    private void addLocalsForVT(VTClass vt, int index) {
        mv.visitVarInsn(Opcodes.ALOAD, index);
        nbInstrAdded++;
        final int finalK = index;
        vt.fields.stream().forEach(fn -> {
            mv.visitInsn(Opcodes.DUP);
            int indexNewLocal = lvs.newLocal(Type.getType(transformVTDesc(fn.desc)));
            map.put(vt.name+fn.name+String.valueOf(finalK), indexNewLocal);
            mv.visitFieldInsn(Opcodes.GETFIELD, vt.name, fn.name, transformVTDesc(fn.desc));
            switch (transformVTDesc(fn.desc)) {
                case "B":
                case "C":
                case "S":
                case "I":
                    mv.visitVarInsn(Opcodes.ISTORE, indexNewLocal);
                    break;
                case "J":
                    mv.visitVarInsn(Opcodes.LSTORE, indexNewLocal);
                    break;
                case "F":
                    mv.visitVarInsn(Opcodes.FSTORE, indexNewLocal);
                    break;
                case "D":
                    mv.visitVarInsn(Opcodes.DSTORE, indexNewLocal);
                    break;
                default:
                    mv.visitVarInsn(Opcodes.ASTORE, indexNewLocal);
                    VTClass embbededVT = Rewriter.vtsLayout.get(Type.getType(transformVTDesc(fn.desc)).getClassName());
                    if(embbededVT == null) throw new AssertionError("Error the descriptor " + fn.desc + " should refer to another VT");
                    addLocalsForVT(embbededVT, indexNewLocal);
                    break;
            }
            nbInstrAdded+=3;
        });
        mv.visitInsn(Opcodes.POP);
        nbInstrAdded++;
        maxStack = 3;
    }

    @Override
    public void visitEnd() {
        //TODO A revoir
        MethodNode mn = (MethodNode) mv;
        RecomposeObjectMethodTransformer recomposeObjectMethodTransformer = new RecomposeObjectMethodTransformer(null, map, owner);
        ReplaceGetAndPutFieldMethodTransformer removeGetFieldMethodTransformer = new ReplaceGetAndPutFieldMethodTransformer(recomposeObjectMethodTransformer, map, owner, nbInstrAdded);
        removeGetFieldMethodTransformer.transform(mn);
        mn.accept(next);
        super.visitEnd();
    }

    @Override public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitMaxs(Math.max(this.maxStack, maxStack), maxLocals);
    }
}
