package rt;

/**
 *
 * Created by Jefferson Mangue on 15/07/2016.
 */
class Types {

    /**
     * The type of instructions without any argument.
     */
    static final int NOARG_INSN = 0;

    /**
     * The type of instructions with an signed byte argument.
     */
    static final int SBYTE_INSN = 1;

    /**
     * The type of instructions with an signed short argument.
     */
    static final int SHORT_INSN = 2;

    /**
     * The type of instructions with a local variable index argument.
     */
    static final int VAR_INSN = 3;

    /**
     * The type of instructions with an implicit local variable index argument.
     */
    static final int IMPLVAR_INSN = 4;

    /**
     * The type of instructions with a type descriptor argument.
     */
    static final int TYPE_INSN = 5;

    /**
     * The type of field and method invocations instructions.
     */
    static final int FIELDORMETH_INSN = 6;

    /**
     * The type of the INVOKEINTERFACE/INVOKEDYNAMIC instruction.
     */
    static final int ITFMETH_INSN = 7;

    /**
     * The type of the INVOKEDYNAMIC instruction.
     */
    static final int INDYMETH_INSN = 8;

    /**
     * The type of instructions with a 2 bytes bytecode offset label.
     */
    static final int LABEL_INSN = 9;

    /**
     * The type of instructions with a 4 bytes bytecode offset label.
     */
    static final int LABELW_INSN = 10;

    /**
     * The type of the LDC instruction.
     */
    static final int LDC_INSN = 11;

    /**
     * The type of the LDC_W and LDC2_W instructions.
     */
    static final int LDCW_INSN = 12;

    /**
     * The type of the IINC instruction.
     */
    static final int IINC_INSN = 13;

    /**
     * The type of the TABLESWITCH instruction.
     */
    static final int TABL_INSN = 14;

    /**
     * The type of the LOOKUPSWITCH instruction.
     */
    static final int LOOK_INSN = 15;

    /**
     * The type of the MULTIANEWARRAY instruction.
     */
    static final int MANA_INSN = 16;

    /**
     * The type of the WIDE instruction.
     */
    static final int WIDE_INSN = 17;

    /**
     * The type of the TYPED instruction.
     */
    static final int TYPED_INSN = 18;

    /**
     * The type of CONSTANT_Class constant pool items.
     */
    static final int CLASS = 7;

    /**
     * The type of CONSTANT_Fieldref constant pool items.
     */
    static final int FIELD = 9;

    /**
     * The type of CONSTANT_Methodref constant pool items.
     */
    static final int METH = 10;

    /**
     * The type of CONSTANT_InterfaceMethodref constant pool items.
     */
    static final int IMETH = 11;

    /**
     * The type of CONSTANT_String constant pool items.
     */
    static final int STR = 8;

    /**
     * The type of CONSTANT_Integer constant pool items.
     */
    static final int INT = 3;

    /**
     * The type of CONSTANT_Float constant pool items.
     */
    static final int FLOAT = 4;

    /**
     * The type of CONSTANT_Long constant pool items.
     */
    static final int LONG = 5;

    /**
     * The type of CONSTANT_Double constant pool items.
     */
    static final int DOUBLE = 6;

    /**
     * The type of CONSTANT_NameAndType constant pool items.
     */
    static final int NAME_TYPE = 12;

    /**
     * The type of CONSTANT_Utf8 constant pool items.
     */
    static final int UTF8 = 1;

    /**
     * The type of CONSTANT_MethodType constant pool items.
     */
    static final int MTYPE = 16;

    /**
     * The type of CONSTANT_MethodHandle constant pool items.
     */
    static final int HANDLE = 15;

    /**
     * The type of CONSTANT_InvokeDynamic constant pool items.
     */
    static final int INDY = 18;

    /**
     * The type of CONSTANT_TypeVar constant pool items.
     */
    static final int TYPE_VAR = 19;

    /**
     * The type of CONSTANT_ParameterizedType constant pool items.
     */
    static final int PARAMETERIZED_TYPE = 20;

    /**
     * The type of CONSTANT_ArrayType constant pool items.
     */
    static final int ARRAY_TYPE = 21;

    /**
     * The type of CONSTANT_Descriptor constant pool items.
     */
    static final int METHOD_DESCRIPTOR = 22;

    /**
     * The base value for all CONSTANT_MethodHandle constant pool items.
     * Internally, ASM store the 9 variations of CONSTANT_MethodHandle into 9
     * different items.
     */
    static final int HANDLE_BASE = 20;

    static final int TYPE_NORMAL = 30;

    static final int TYPE_UNINIT = 31;

    static final int TYPE_MERGED = 32;

    /**
     * The type of BootstrapMethods items. These items are stored in a special
     * class attribute named BootstrapMethods and not in the constant pool.
     */
    static final int BSM = 33;
}
