package value;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.FieldNode;

import static org.objectweb.asm.Opcodes.*;

/**
 * Created by Fabien GIACHERIO on 07/02/17.
 *
 * Value type ClassVisitor.
 * Transforms a value type written in java 10 by an object reference interpretable by a JVM 8
 */
public class VTClassVisitor extends ClassVisitor {

    public static final int API = ASM5;
    private String owner;

    //Used to adapt the ClassVisitor's behavior
    private boolean isValue;

    public VTClassVisitor(ClassVisitor cv) {
        super(API, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.owner = name;
        this.isValue = (superName != null && superName.equals("java/lang/__Value"));
        if (isValue) {
            // Delete the ACC_VALUE flag.
            super.visit(version, (access&~(ACC_VALUE)), name, signature, "java/lang/Object", interfaces);
        } else {
            super.visit(version, access, name, signature, superName, interfaces);
        }
    }

    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        return cv.visitField(access,name,transformVTDesc(desc),signature,value);
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv;
        if(name.equals("<vminit>")) {
            addValueConstructor();
            return null;
        }
        else if(name.equals("<init>")) {
            mv = cv.visitMethod((access&~(ACC_PRIVATE))|ACC_PUBLIC,name,findAndTransformVtDesc(desc), signature, exceptions);
        } else {
            mv = cv.visitMethod(access,name,findAndTransformVtDesc(desc), signature, exceptions);
        }
        VTMethodAdapter vtMethodAdapter = new VTMethodAdapter(access, name, findAndTransformVtDesc(desc), signature, exceptions, mv, this.owner);
        vtMethodAdapter.lvs = new LocalVariablesSorter(access, desc, vtMethodAdapter);
        return  vtMethodAdapter.lvs;
    }

    private void addValueConstructor() {
        VTClass vtClass = Rewriter.vtsLayout.get(owner);
        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "<init>", vtClass.initDesc, null, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        int size = 1;
        for(FieldNode fn : vtClass.fields) {
            mv.visitVarInsn(ALOAD, 0);
            switch (fn.desc) {
                case "B":
                case "C":
                case "S":
                case "Z":
                case "I":
                    mv.visitVarInsn(ILOAD, size);
                    break;
                case "J":
                    mv.visitVarInsn(LLOAD, size);
                    break;
                case "F":
                    mv.visitVarInsn(FLOAD, size);
                    break;
                case "D":
                    mv.visitVarInsn(DLOAD, size);
                    break;
                default:
                    mv.visitVarInsn(ALOAD, size);
                    break;
            }
            mv.visitFieldInsn(PUTFIELD, vtClass.name, fn.name, transformVTDesc(fn.desc));
            size += Type.getType(fn.desc).getSize();
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(0,0);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if(desc.equals("Ljvm/internal/value/DeriveValueType;")) {
            //TODO : Enregistrer la classe ?
            return null;
        }
        return super.visitAnnotation(desc, visible);
    }

    static String transformVTDesc(String desc) {
        if(desc.startsWith("Q")){
            return "L" + desc.substring(1, desc.length());
        }
        return desc;
    }

    static String findAndTransformVtDesc(String desc){
        int midIndex = desc.indexOf(')');
        //First char not needed
        char[] lDesc = desc.substring(1, midIndex).toCharArray();
        String rDesc = desc.substring(midIndex+1, desc.length());
        int off=0;
        while(off < lDesc.length) {
            while(off < lDesc.length && (lDesc[off]!='Q' && lDesc[off]!='L' && lDesc[off]!='T')) off++;
            if(off < lDesc.length && lDesc[off]=='Q') lDesc[off]='L';
            while(off < lDesc.length && (lDesc[off]!=';')) off++;
            off++;
        }
        rDesc = transformVTDesc(rDesc);
        return "("+String.valueOf(lDesc)+")"+rDesc;
    }
}
