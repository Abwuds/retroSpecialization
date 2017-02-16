package value;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;

import java.util.List;

/**
 * Created by Fabien GIACHERIO on 16/02/17.
 */
public class VTBasicInterpreter extends BasicInterpreter {

    public final static BasicValue VALUE_TYPE = new BasicValue(null);

    public VTBasicInterpreter() {
        super(ASM5);
    }

    @Override
    public BasicValue newValue(Type type) {
        if(type != null && type.getSort() == Type.VALUE_TYPE) {
            return VTBasicInterpreter.VALUE_TYPE;
        }
        return super.newValue(type);
    }

    @Override
    public BasicValue binaryOperation(AbstractInsnNode insn, BasicValue value1, BasicValue value2) throws AnalyzerException {
        if(insn.getOpcode() == VALOAD) {
            return VTBasicInterpreter.VALUE_TYPE;
        }
        return super.binaryOperation(insn, value1, value2);
    }

    @Override
    public BasicValue unaryOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
        if (insn.getOpcode() == VGETFIELD) {
            return newValue(Type.getType(((FieldInsnNode) insn).desc));
        } else if (insn.getOpcode() == VRETURN) {
            return null;
        }
        return super.unaryOperation(insn, value);
    }

    @Override
    public BasicValue naryOperation(AbstractInsnNode insn, List<? extends BasicValue> values) throws AnalyzerException {
        if(insn.getOpcode() == Opcodes.VNEW) {
            return newValue(Type.getReturnType(((MethodInsnNode)insn).desc));
        }
        return super.naryOperation(insn, values);
    }
}
