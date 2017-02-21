package value;

import org.objectweb.asm.tree.FieldNode;

import java.util.List;

/**
 * Created by Fabien GIACHERIO on 17/02/17.
 */
public class VTClass {

    final String name;
    final List<FieldNode> fields;

    public VTClass(String name, List<FieldNode> fields) {
        this.name = name;
        this.fields = fields;
    }
}
