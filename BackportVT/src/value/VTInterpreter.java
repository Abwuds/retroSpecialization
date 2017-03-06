package value;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;

/**
 * Created by Fabien GIACHERIO on 28/02/17.
 *
 * This interpreter is used to detect the VT on the stack when a VSTORE instruction is reached.
 */
public class VTInterpreter extends BasicInterpreter {

    @Override
    public BasicValue newValue(Type type) {
        if (type == null) {
            return BasicValue.UNINITIALIZED_VALUE;
        }
        if(type.getSort()==Type.VALUE_TYPE) {
            return new BasicValue(type);
        }
        if(type.getSort() == Type.ARRAY){
            VTClass vtClass = Rewriter.vtsLayout.get(type.getClassName().substring(0, type.getClassName().length() - 2));
            if(vtClass !=null) {
                Type type1 = Type.getType("Q" + vtClass.name + ";");
                return new BasicValue(type1);
            }
        }
        return super.newValue(type);
    }

    @Override
    public BasicValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
        if(insn.getOpcode() == VDEFAULT) {
            return newValue(Type.getObjectType(((TypeInsnNode) insn).desc));
        }
        return super.newOperation(insn);
    }

    @Override
    public BasicValue unaryOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
        switch (insn.getOpcode()) {
            case VGETFIELD :
                return newValue(Type.getType(VTClassVisitor.transformVTDesc(((FieldInsnNode) insn).desc)));
            case VRETURN:
                return null;
            default:
                break;
        }
        return super.unaryOperation(insn, value);
    }

    @Override
    public BasicValue copyOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
        return super.copyOperation(insn, value);
    }

    @Override
    public BasicValue binaryOperation(AbstractInsnNode insn, BasicValue value1, BasicValue value2) throws AnalyzerException {
        switch (insn.getOpcode()) {
            case VALOAD :
                return value1;
            case VWITHFIELD :
                return newValue(Type.getType("Q".concat(((FieldInsnNode) insn).owner).concat(";")));
        }
        return super.binaryOperation(insn, value1, value2);
    }
}
