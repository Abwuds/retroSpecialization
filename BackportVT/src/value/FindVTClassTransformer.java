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
            Optional<String> initDesc = cn.methods.stream().filter(mn -> mn.name.equals("<init>")).map(mn -> mn.desc).filter(d -> !d.equals("()V")).findAny();
            if(!initDesc.isPresent()) throw new AssertionError("<init> method (with desc != ()V) is not present in class " + cn.name);
            List<FieldNode> fieldsWithoutStatic = cn.fields.stream().filter(fn -> ((fn.access & ACC_STATIC) == 0)).collect(Collectors.toList());
            Rewriter.vtsLayout.put(cn.name, new VTClass(cn.name, VTClassVisitor.findAndTransformVtDesc(initDesc.get()), fieldsWithoutStatic));
        }
        super.transform(cn);
    }
}
