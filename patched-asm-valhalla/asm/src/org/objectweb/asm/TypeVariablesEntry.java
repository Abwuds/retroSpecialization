package org.objectweb.asm;

/**
 * Created by Jefferson Mangue on 01/06/2016.
 */
public class TypeVariablesEntry {
    private final int isAny;
    private final String tVarNameIndex;
    private final String boundIndex;

    public TypeVariablesEntry(int flag, String tVarNameIndex, String boundIndex) {
        this.isAny = flag;
        this.tVarNameIndex = tVarNameIndex;
        this.boundIndex = boundIndex;
    }

    public int getIsAny() {
        return isAny;
    }

    public String gettVarNameIndex() {
        return tVarNameIndex;
    }

    public String getBoundIndex() {
        return boundIndex;
    }

    @Override
    public String toString() {
        return "[TypeVariablesEntry - TvarName idx : " + tVarNameIndex +
                " FLAG : " + isAny + " Bound idx : " + boundIndex + ']';
    }
}
