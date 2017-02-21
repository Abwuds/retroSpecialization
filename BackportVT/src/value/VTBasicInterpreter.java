package value;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.util.List;

/**
 * Created by Fabien GIACHERIO on 16/02/17.
 */
public class VTBasicInterpreter extends SourceInterpreter {

    public VTBasicInterpreter() {
        super(ASM5);
    }

    @Override
    public SourceValue newValue(Type type) {
        return super.newValue(type);
    }

    @Override
    public SourceValue newOperation(AbstractInsnNode insn) {
        return super.newOperation(insn);
    }

    @Override
    public SourceValue copyOperation(AbstractInsnNode insn, SourceValue value) {
        return super.copyOperation(insn, value);
    }

    @Override
    public SourceValue unaryOperation(AbstractInsnNode insn, SourceValue value) {
        return super.unaryOperation(insn, value);
    }

    @Override
    public SourceValue binaryOperation(AbstractInsnNode insn, SourceValue value1, SourceValue value2) {
        return super.binaryOperation(insn, value1, value2);
    }

    @Override
    public SourceValue ternaryOperation(AbstractInsnNode insn, SourceValue value1, SourceValue value2, SourceValue value3) {
        return super.ternaryOperation(insn, value1, value2, value3);
    }

    @Override
    public SourceValue naryOperation(AbstractInsnNode insn, List<? extends SourceValue> values) {
        return super.naryOperation(insn, values);
    }

    @Override
    public void returnOperation(AbstractInsnNode insn, SourceValue value, SourceValue expected) {
        super.returnOperation(insn, value, expected);
    }

    @Override
    public SourceValue merge(SourceValue d, SourceValue w) {
        return super.merge(d, w);
    }

}
