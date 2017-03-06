package value;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.ACC_STATIC;

/**
 * Created by Fabien GIACHERIO on 20/02/17.
 *
 * Used to mark and register the value types and their layout.
 */
public class FindVTClassTransformer extends ClassTransformer {


    public FindVTClassTransformer(ClassTransformer ct) {
        super(ct);
    }

    @Override
    public void transform(ClassNode cn) {
        if(cn.superName != null && cn.superName.equals("java/lang/__Value")) {
            Optional<String> vmInitDesc = cn.methods.stream().filter(mn -> mn.name.equals("<vminit>")).map(mn -> mn.desc).findAny();
            if(!vmInitDesc.isPresent()) throw new AssertionError("<vminit> method is not present in class " + cn.name);
            List<FieldNode> fieldsWithoutStatic = cn.fields.stream().filter(fn -> ((fn.access & ACC_STATIC) == 0)).collect(Collectors.toList());
            String tmp = vmInitDesc.get();
            //Adding a boolean into the vminit's constructor and removing the return type
            String newInitDescriptor = VTClassVisitor.findAndTransformVtDesc(tmp.substring(0, tmp.indexOf(')')).concat("Z)V"));
            Rewriter.vtsLayout.put(cn.name, new VTClass(cn.name, newInitDescriptor, fieldsWithoutStatic));
        }
        super.transform(cn);
    }
}
