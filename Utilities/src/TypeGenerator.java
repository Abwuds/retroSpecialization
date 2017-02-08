import org.objectweb.asm.Opcodes;


/**
 * Created by Jefferson Mangue on 08/02/2017.
 *
 *
 * Allows the generation of type encoding used in {@link org.objectweb.asm.ClassWriter}.
 */
public class TypeGenerator {

    /**
     * The type of instructions without any argument.
     */
    private static final int NOARG_INSN = 0;

    /**
     * The type of instructions with an signed byte argument.
     */
    private static final int SBYTE_INSN = 1;

    /**
     * The type of instructions with an signed short argument.
     */
    private static final int SHORT_INSN = 2;

    /**
     * The type of instructions with a local variable index argument.
     */
    private static final int VAR_INSN = 3;

    /**
     * The type of instructions with an implicit local variable index argument.
     */
    private static final int IMPLVAR_INSN = 4;

    /**
     * The type of instructions with a type descriptor argument.
     */
    private static final int TYPE_INSN = 5;

    /**
     * The type of field and method invocations instructions.
     */
    private static final int FIELDORMETH_INSN = 6;

    /**
     * The type of the INVOKEINTERFACE/INVOKEDYNAMIC instruction.
     */
    private static final int ITFMETH_INSN = 7;

    /**
     * The type of the INVOKEDYNAMIC instruction.
     */
    private static final int INDYMETH_INSN = 8;

    /**
     * The type of instructions with a 2 bytes bytecode offset label.
     */
    private static final int LABEL_INSN = 9;

    /**
     * The type of instructions with a 4 bytes bytecode offset label.
     */
    private static final int LABELW_INSN = 10;

    /**
     * The type of the LDC instruction.
     */
    private static final int LDC_INSN = 11;

    /**
     * The type of the LDC_W and LDC2_W instructions.
     */
    private static final int LDCW_INSN = 12;

    /**
     * The type of the IINC instruction.
     */
    private static final int IINC_INSN = 13;

    /**
     * The type of the TABLESWITCH instruction.
     */
    private static final int TABL_INSN = 14;

    /**
     * The type of the LOOKUPSWITCH instruction.
     */
    private static final int LOOK_INSN = 15;

    /**
     * The type of the MULTIANEWARRAY instruction.
     */
    private static final int MANA_INSN = 16;

    /**
     * The type of the WIDE instruction.
     */
    private static final int WIDE_INSN = 17;

    /**
     * The type of the TYPED instruction.
     */
    private static final int TYPED_INSN = 18;

    public static void main(String[] args) {

        // code to generate the above string
        //
        // NOTE : To handle new Valhalla instructions, this code as been replaced by the
        // code inside test/resources/TypeGenerator#generate.

        int i;
        // SBYTE_INSN instructions
        byte[] b = new byte[231];
        b[Opcodes.NEWARRAY] = SBYTE_INSN;
        b[Opcodes.BIPUSH] = SBYTE_INSN;

//          SHORT_INSN instructions
        b[Opcodes.SIPUSH] = SHORT_INSN;

//          (IMPL)VAR_INSN instructions
        b[Opcodes.RET] = VAR_INSN;
        for (i = Opcodes.ILOAD; i <= Opcodes.ALOAD; ++i) {
            b[i] = VAR_INSN;
        }
        for (i = Opcodes.ISTORE; i <= Opcodes.ASTORE; ++i) {
            b[i] = VAR_INSN;
        }
        for (i = 26; i <= 45; ++i) { // ILOAD_0 to ALOAD_3
            b[i] = IMPLVAR_INSN;
        }
        for (i = 59; i <= 78; ++i) { // ISTORE_0 to ASTORE_3
            b[i] = IMPLVAR_INSN;
        }

        // TYPE_INSN instructions
        b[Opcodes.NEW] = TYPE_INSN;
        b[Opcodes.ANEWARRAY] = TYPE_INSN;
        b[Opcodes.CHECKCAST] = TYPE_INSN;
        b[Opcodes.INSTANCEOF] = TYPE_INSN;

        // (Set)FIELDORMETH_INSN instructions
        for (i = Opcodes.GETSTATIC; i <= Opcodes.INVOKESTATIC; ++i) {
            b[i] = FIELDORMETH_INSN;
        }
        b[Opcodes.INVOKEINTERFACE] = ITFMETH_INSN;
        b[Opcodes.INVOKEDYNAMIC] = INDYMETH_INSN;

        // LABEL(W)_INSN instructions
        for (i = Opcodes.IFEQ; i <= Opcodes.JSR; ++i) {
            b[i] = LABEL_INSN;
        }
        b[Opcodes.IFNULL] = LABEL_INSN;
        b[Opcodes.IFNONNULL] = LABEL_INSN;
        b[200] = LABELW_INSN; // GOTO_W
        b[201] = LABELW_INSN; // JSR_W
        // temporary opcodes used internally by ASM - see Label and
        for (i = 213; i < 231; ++i) {
            b[i] = LABEL_INSN;
        }

        // LDC(_W) instructions
        b[Opcodes.LDC] = LDC_INSN;
        b[19] = LDCW_INSN; // LDC_W
        b[20] = LDCW_INSN; // LDC2_W

        // special instructions
        b[Opcodes.IINC] = IINC_INSN;
        b[Opcodes.TABLESWITCH] = TABL_INSN;
        b[Opcodes.LOOKUPSWITCH] = LOOK_INSN;
        b[Opcodes.MULTIANEWARRAY] = MANA_INSN;
        b[196] = WIDE_INSN; // WIDE


        // New instructions
        b[Opcodes.TYPED] = TYPED_INSN;
        b[Opcodes.VRETURN] = TYPED_INSN;
        b[Opcodes.VGETFIELD] = TYPED_INSN;

        for (i = 0; i < b.length; ++i) {
            System.err.print((char) ('A' + b[i]));
        }
        System.err.println();
    }

}
