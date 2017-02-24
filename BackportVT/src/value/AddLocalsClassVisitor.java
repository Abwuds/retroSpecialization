package value;


import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;

import static value.VTClassVisitor.API;

/**
 * Created by Fabien GIACHERIO on 20/02/17.
 *
 * ClassVisitor which call the AddLocalsMethodVisitor.
 */
public class AddLocalsClassVisitor extends ClassVisitor {

    String owner;

    public AddLocalsClassVisitor(ClassVisitor cv) {
        super(API, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.owner = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
        if (mv != null && !name.equals("<init>")) {
            AddLocalsMethodVisitor at = new AddLocalsMethodVisitor(mv, access, name, desc, signature, exceptions,owner);
            at.aa = new AnalyzerAdapter(owner, access, name, desc, at);
            at.lvs = new LocalVariablesSorter(access, desc, at.aa);
            return at.lvs;
        }
        return mv;
    }
}
