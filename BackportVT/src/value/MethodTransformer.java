package value;

import org.objectweb.asm.tree.MethodNode;

/**
 * Created by Fabien GIACHERIO on 08/02/17.
 *
 * Used to compose method transformers easily
 */
public class MethodTransformer {
    protected MethodTransformer mt;

    public MethodTransformer(MethodTransformer mt) {
        this.mt = mt;
    }

    /**
     * Transform a method node
     * @param mn The method node that needs to be transformed
     */
    public void transform(MethodNode mn) {
        if (mt != null) {
            mt.transform(mn);
        }
    }
}
