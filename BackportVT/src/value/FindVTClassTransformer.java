package value;

import org.objectweb.asm.tree.ClassNode;

/**
 * Created by Fabien GIACHERIO on 20/02/17.
 */
public class FindVTClassTransformer extends ClassTransformer {

    public FindVTClassTransformer(ClassTransformer ct) {
        super(ct);
    }

    @Override
    public void transform(ClassNode cn) {
        if(cn.superName != null && cn.superName.equals("java/lang/__Value")) {
            Rewriter.vts.add(new VTClass(cn.name, cn.fields));
        }
        super.transform(cn);
    }
}
