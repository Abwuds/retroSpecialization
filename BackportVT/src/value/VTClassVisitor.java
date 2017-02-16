package value;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.ACC_VALUE;
import static org.objectweb.asm.Opcodes.ASM5;

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
        System.out.println(name + " : " + (access&~(ACC_VALUE)) + " extends " + superName + " {");
        this.className = name;
        this.isValue = (superName != null && superName.equals("java/lang/__Value"));
        if (isValue) {
            // Delete the ACC_VALUE flag.
            super.visit(version, (access&~(ACC_VALUE)), name, signature, "java/lang/Object", interfaces);
        } else {
            super.visit(version, access, name, signature, superName, interfaces);
        }
    }

    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        //TODO Check if our class is a value ?
        System.out.println("  Field : " + name + " : " + desc + "  -> " + name + " : " + transformVTDesc(desc)+ " " );
        return cv.visitField(access,name,transformVTDesc(desc),signature,value);
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if(name.equals("<vminit>")) {
            return null;
        }

        System.out.println("  Method : " + name + " : " + desc + "  -> " + name + " : " + findAndTransformVtDesc(desc)+ " " );
        if(name.equals("<init>")) {
            this.initDescriptor = findAndTransformVtDesc(desc);
        }

        MethodVisitor mv = cv.visitMethod(access,name,findAndTransformVtDesc(desc), signature, exceptions);
        return new VTMethodAdapter(access,name,findAndTransformVtDesc(desc), signature, exceptions, mv, this.className, this.initDescriptor);
    }

    public void visitEnd() {
        System.out.println("}");
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
