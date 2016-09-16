package rt;

import java.util.Arrays;

/**
 *
 * Created by Jefferson Mangue on 15/07/2016.
 */
public class SubstitutionTableReader {

    private final int header;
    private final byte[] b;
    private final int[] items;

    public static SubstitutionTable read(final byte[] b) {
        return new SubstitutionTableReader(b, 0, 0).accept();
    }

    private SubstitutionTableReader(final byte[] b, final int off, final int len) {
        this.b = b;
        // class version is at readShort(off + 6)
        // parses the constant pool
        items = new int[readUnsignedShort(off + 8)];
        int n = items.length;
        int max = 0;
        int index = off + 10;
        for (int i = 1; i < n; ++i) {
            items[i] = index + 1;
            int size;
            switch (b[index]) {
                case Types.FIELD:
                case Types.METH:
                case Types.IMETH:
                case Types.INT:
                case Types.FLOAT:
                case Types.NAME_TYPE:
                case Types.INDY:
                    size = 5;
                    break;
                case Types.LONG:
                case Types.DOUBLE:
                    size = 9;
                    ++i;
                    break;
                case Types.UTF8:
                    size = 3 + readUnsignedShort(index + 1);
                    if (size > max) {
                        max = size;
                    }
                    break;
                case Types.HANDLE:
                case Types.TYPE_VAR:
                    size = 4;
                    break;
                case Types.PARAMETERIZED_TYPE:
                    size = 7 + readByte(index + 6) * 2;
                    break;
                case Types.METHOD_DESCRIPTOR:
                    size = 4 + readByte(index + 1) * 2;
                    break;
                // case ClassWriter.CLASS:
                // case ClassWriter.STR:
                // case ClassWriter.MTYPE
                default:
                    size = 3;
                    break;
            }
            index += size;
        }
        // the class header information starts just after the constant pool
        header = index;
    }

    public SubstitutionTable accept() {
        int u; // current offset in the class file

        u = getAttributes();
        for (int i = readUnsignedShort(u); i > 0; --i) {
            String attrName = readSubstitutionTableName(u + 2);

            if (SubstitutionTable.NAME.equals(attrName)) {
                return SubstitutionTable.create(this, u + 8, readInt(u + 4));
            }
            u += 6 + readInt(u + 4);
        }
        return null;
    }

    /**
     * Returns the start index of the attribute_info structure of this class.
     *
     * @return the start index of the attribute_info structure of this class.
     */
    private int getAttributes() {
        // skips the header
        int u = header + 8 + readUnsignedShort(header + 6) * 2;
        // skips fields and methods
        for (int i = readUnsignedShort(u); i > 0; --i) {
            for (int j = readUnsignedShort(u + 8); j > 0; --j) {
                u += 6 + readInt(u + 12);
            }
            u += 8;
        }
        u += 2;
        for (int i = readUnsignedShort(u); i > 0; --i) {
            for (int j = readUnsignedShort(u + 8); j > 0; --j) {
                u += 6 + readInt(u + 12);
            }
            u += 8;
        }
        // the attribute_info structure starts just after the methods
        return u + 2;
    }

    private int readByte(final int index) {
        return b[index] & 0xFF;
    }

    private int readUnsignedShort(final int index) {
        byte[] b = this.b;
        return ((b[index] & 0xFF) << 8) | (b[index + 1] & 0xFF);
    }

    public short readShort(final int index) {
        byte[] b = this.b;
        return (short) (((b[index] & 0xFF) << 8) | (b[index + 1] & 0xFF));
    }

    private int readInt(final int index) {
        byte[] b = this.b;
        return ((b[index] & 0xFF) << 24) | ((b[index + 1] & 0xFF) << 16)
                | ((b[index + 2] & 0xFF) << 8) | (b[index + 3] & 0xFF);
    }

    private String readSubstitutionTableName(int index) {
        int item = readUnsignedShort(index);
        if (index == 0 || item == 0) {
            return null;
        }
        return readUTF8SubstitutionTableNameItem(item);
    }

    private String readUTF8SubstitutionTableNameItem(int item) {
        int index = items[item];
        int length = readUnsignedShort(index);
        int testLen = SubstitutionTable.NAME.length();
        if (length > testLen) {
            return null;
        }
        return readUTF(index + 2, length, new char[length]);
    }

    public String readUTF8(int index) {
        int length = readUnsignedShort(index);
        return readUTF(index + 2, length, new char[length]);
    }

    /**
     * Reads UTF8 string in {@link #b b}.
     *
     * @param index
     *            start offset of the UTF8 string to be read.
     * @param utfLen
     *            length of the UTF8 string to be read.
     * @param buf
     *            buffer to be used to read the string. This buffer must be
     *            sufficiently large. It is not automatically resized.
     * @return the String corresponding to the specified UTF8 string.
     */
    private String readUTF(int index, final int utfLen, final char[] buf) {
        int endIndex = index + utfLen;
        byte[] b = this.b;
        int strLength = 0;
        int c;
        char cc = 0;
        int st = 0;

        while (index < endIndex) {
            c = b[index++];
            switch (st) {
                case 0:
                    c = c & 0xFF;
                    if (c < 0x80) { // 0xxxxxxx
                        buf[strLength++] = (char) c;
                    } else if (c < 0xE0 && c > 0xBF) { // 110x xxxx 10xx xxxx
                        st = 1;
                        cc = (char) (c & 0x1F);
                    } else { // 1110 xxxx 10xx xxxx 10xx xxxx
                        st = 2;
                        cc = (char) (c & 0x0F);
                    }
                    break;
                case 1: // byte 2 of 2-byte char or byte 3 of 3-byte char
                    buf[strLength++] = (char) ((cc << 6) | (c & 0x3F));
                    st = 0;
                    break;
                case 2: // byte 2 of 3-byte char
                    cc = (char) ((cc << 6) | (c & 0x3F));
                    st = 1;
                    break;
            }
        }
        return new String(buf, 0, strLength);
    }
}
