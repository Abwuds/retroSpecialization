package value;

import org.objectweb.asm.tree.ClassNode;

import java.util.List;
import java.util.Optional;

/**
 * Created by Fabien GIACHERIO on 20/02/17.
 *
 * Used to mark and register the value types and their layout.
 */
public class FindVTClassTransformer extends ClassTransformer {

    private List<String> vts;

    public FindVTClassTransformer(ClassTransformer ct, List<String> vts) {
        super(ct);
        this.vts = vts;
    }

    @Override
    public void transform(ClassNode cn) {
        if(cn.superName != null && cn.superName.equals("java/lang/__Value")) {
            Optional<String> initDesc = cn.methods.stream().filter(mn -> mn.name.equals("<init>")).map(mn -> mn.desc).filter(d -> !d.equals("()V")).findAny();
            if(!initDesc.isPresent()) throw new AssertionError("<init> method (with desc != ()V) is not present in class " + cn.name);
            Rewriter.vtsLayout.put(cn.name, new VTClass(cn.name, VTClassVisitor.findAndTransformVtDesc(initDesc.get()), cn.fields));
        }
        super.transform(cn);
    }
}
