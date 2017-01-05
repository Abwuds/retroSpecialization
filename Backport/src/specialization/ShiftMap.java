package specialization;

/**
 * Created by Jefferson Mangue on 02/12/2016.
 */
public interface ShiftMap {
    /**
     * Computes the new parameter's index. If the parameter is not already inside the
     * {@link ShiftMap}, it is added to it, taking into account its size {@code isLarge}
     *
     * @param parameter the parameter's index.
     * @return the new offset of the parameter according to the current {@link ShiftMap}'s rules.
     */
    int getNewVariableIndex(int parameter);


    /**
     * @return the shifted method's name.
     */
    String getMethodName();

    String dump();

    void writeHeader(BackMethodVisitor backMethodVisitor);
}
