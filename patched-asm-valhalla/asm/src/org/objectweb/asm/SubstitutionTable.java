package org.objectweb.asm;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * Created by Jefferson Mangue on 20/06/2016.
 */
public class SubstitutionTable extends Attribute {
    public static final String NAME = "SubstitutionTable";

    private final ByteVector vector;
    private final HashMap<Integer, Map.Entry<String, String>> descriptors; // Cache.

    public SubstitutionTable() {
        super(NAME);
        vector = new ByteVector();
        descriptors = new HashMap<Integer, Map.Entry<String, String>>();
    }


    public void putUTF8(int index, String owner, String descriptor) {
        descriptors.put(index, new HashMap.SimpleEntry<String, String>(descriptor, owner));
        vector.putShort(index);
        vector.putUTF8(owner);
        vector.putUTF8(descriptor);
    }

    public boolean contains(int index) {
        return descriptors.containsKey(index);
    }

    public boolean contains(String descriptor, String owner) {
        // TODO test on owner AND descriptor since x/Object is replaced by t/Object.
        return descriptors.containsValue(new HashMap.SimpleEntry<String, String>(descriptor, owner));
    }

    public int get(String desc, String owner) {
        // Since the contains test using index is used inside a loop inside ClassWriter#get method.
        // The index has been chosen to be the key.
        AbstractMap.SimpleEntry<String, String> entry = new HashMap.SimpleEntry<String, String>(desc, owner);
        for (Map.Entry<Integer,  Map.Entry<String, String>> e : descriptors.entrySet()) {
            if (e.getValue().equals(entry)) {
                return e.getKey();
            }
        }
        throw new IllegalArgumentException("No key : " + entry);
    }

    @Override
    public String toString() {
        return descriptors.toString();
    }

    public boolean isEmpty() {
        return descriptors.isEmpty();
    }

    public byte[] getByteArray() {
        return vector.data;
    }


    @Override
    protected Attribute read(ClassReader cr, int off, int len, char[] buf, int codeOff, Label[] labels) {
        return super.read(cr, off, len, buf, codeOff, labels);
    }

    @Override
    protected ByteVector write(ClassWriter cw, byte[] code, int len, int maxStack, int maxLocals) {
        return vector;
    }
}
