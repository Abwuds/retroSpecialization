package value;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

/**
 * Created by Fabien GIACHERIO on 07/02/17.
 *
 * Value type ClassVisitor.
 * Transforms a value type written in java 10 by an object reference interpretable by a JVM 8
 */
public class VTClassVisitor extends ClassVisitor {

    public static final int API = ASM5;
    private String className;
    private String initDescriptor;

    //Used to adapt the ClassVisitor's behavior
    private boolean isValue;

    public VTClassVisitor(ClassVisitor cv) {
        super(API, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        System.out.println(name + " : " + (access&(ACC_PRIVATE|ACC_PUBLIC|ACC_FINAL|ACC_PROTECTED|ACC_SUPER)) + " extends " + superName + " {");
        this.className = name;
        this.isValue = (superName != null && superName.equals("java/lang/__Value"));
        if (isValue) {
            // Delete the ACC_VALUE flag. TODO Would be nice to have an opcode.
            super.visit(version, (access&(ACC_PRIVATE|ACC_PUBLIC|ACC_FINAL|ACC_PROTECTED|ACC_SUPER)), name, signature, "java/lang/Object", interfaces);
        } else {
            super.visit(version, access, name, signature, superName, interfaces);
        }
    }

    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        System.out.println("  Field : " + name + " : " + desc + "  -> " + name + " : " + transformVTDesc(desc)+ " " );
        return cv.visitField(access,name,transformVTDesc(desc),signature,value);
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        System.out.println("  Method : " + name + " : " + desc + "  -> " + name + " : " + findAndTransformVtDesc(desc)+ " " );
        if(name.equals("<init>")) {
            this.initDescriptor = desc;
        }
        else if(name.equals("<vminit>")) {
           return null;
        }

        MethodVisitor mv = cv.visitMethod(access,name,findAndTransformVtDesc(desc), signature, exceptions);
        return new VTMethodAdapter(access,name,findAndTransformVtDesc(desc), signature, exceptions, mv, this.className, this.initDescriptor, this.isValue);
    }
    public void visitEnd() {
        System.out.println("}");
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
        String finalDesc = "("+String.valueOf(lDesc)+")"+rDesc;

        return finalDesc;
    }
}
