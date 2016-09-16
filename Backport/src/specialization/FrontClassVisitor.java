package specialization;

import org.objectweb.asm.*;
import rt.Opcodes;


/**
 *
 * Created by Jefferson Mangue on 09/06/2016.
 */
public class FrontClassVisitor extends ClassVisitor implements Opcodes {

    public static final int API = ASM5;
    public static final String BACK_FIELD = "_back__";
    private static final int COMPILER_VERSION = 51;
    private final ClassWriter backClassWriter;
    private final BackClassVisitor backClassVisitor;

    // Created classes names.
    private String frontClassName;
    private String backFactoryName;

    public FrontClassVisitor(ClassVisitor cv) {
        super(API, cv);
        backClassWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        this.backClassVisitor = new BackClassVisitor(API, backClassWriter);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        // At this step, only anyfied classes starting with the token '$'.
        if (!Type.isParameterizedType(name)) {
            frontClassName = name;
            super.visit(COMPILER_VERSION, access, name, signature, superName, interfaces);
            return;
        }
        // The class is anyfied. Cleaning the class frontClassName into the raw frontClassName.
        frontClassName = name;
        String rawName = Type.rawName(name);
        // The inheritance is not handled for anyfied class yet.
        if (superName != null && !superName.equals("java/lang/Object")) {
            throw new IllegalStateException("Not inheritance allowed.");
        }
        super.visit(COMPILER_VERSION, access, rawName, signature, superName, interfaces);
        // Creating the back class inside the any package, by concatenating "_BackFactory".
        // Now creating a back factory class, placed inside the package java/any".
        createBackClassVisitor(COMPILER_VERSION, rawName);
        // Creating an Object field inside the class. It will be used to store the back class at runtime.
        super.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL, BACK_FIELD, "Ljava/lang/Object;", null, null);
        createFrontSpecializationConstructor(rawName);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        // Each fields are moved inside the back class.
        return backClassVisitor.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        // If we have not created a back class, we use a visitor able to transform invocations in compatible invocations.
        if (!hasBackFactory()) { return new InvokeAnyMethodVisitor(frontClassName, api, super.visitMethod(access, name, desc, signature, exceptions), name); }
        // TODO do not redirect static method to back class.
        // We have to turn every method into static method inside the back class.
        return createRetroValhallaMethodVisitor(access, name, desc, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        backClassVisitor.visitEnd(); // Calls the write of the substitutionTable. (I know it should be clearer).
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        super.visitInnerClass(Type.rawName(name), outerName, innerName, access);
    }

    public byte[] getBackFactoryBytes() {
        return backClassWriter.toByteArray();
    }

    public boolean hasBackFactory() {
        return backFactoryName != null;
    }

    public String getBackFactoryName() {
        return backFactoryName;
    }

    private void createBackClassVisitor(int version, String rawName) {
        backClassVisitor.visit(COMPILER_VERSION, Opcodes.ACC_PUBLIC, rawName, null, "java/lang/Object", null);
        backFactoryName = backClassVisitor.getName();
    }

    private void createFrontSpecializationConstructor(String rawName) {
        // Creating the constructor storing the back object.
        MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Ljava/lang/Void;Ljava/lang/Object;)V", null, null);
        FrontMethodVisitor.createFrontSpecializerConstructor(rawName, BACK_FIELD, mv);
    }

    private MethodVisitor createRetroValhallaMethodVisitor(int access, String name, String desc, String signature, String[] exceptions) {
        // The front method created will bridge on the corresponding back method. The back method will receive the code
        // of the current method visited. The current visitor is the front class resulting.
        MethodVisitor fmw = super.visitMethod(access, name, desc, signature, exceptions);
        FrontMethodVisitor.visitFrontMethod(api, frontClassName, name, desc, signature, exceptions, fmw);
        return backClassVisitor.visitMethod(access, name, desc, signature, exceptions);
    }
}
