package value;

import org.objectweb.asm.tree.FieldNode;

import java.util.List;
import java.util.Objects;

/**
 * Created by Fabien GIACHERIO on 17/02/17.
 *
 * Represents a value type.
 */
public class VTClass {

    final String name;
    final String initDesc;
    final List<FieldNode> fields;

    public VTClass(String name, String initDesc, List<FieldNode> fields) {
        this.name = name;
        this.initDesc = initDesc;
        this.fields = fields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VTClass vtClass = (VTClass) o;
        if(Objects.equals(vtClass.name, name)) return false;
        if(Objects.equals(vtClass.initDesc, initDesc)) return false;
        return Objects.equals(vtClass.fields, fields);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (initDesc != null ? initDesc.hashCode() : 0);
        result = 31 * result + (fields != null ? fields.hashCode() : 0);
        return result;
    }
}
