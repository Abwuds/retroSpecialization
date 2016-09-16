package rt;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jefferson Mangue on 20/06/2016.
 */
public class SubstitutionTable {
    public static final String NAME = "SubstitutionTable";
    private final HashMap<Integer, Map.Entry<String, String>> descriptors; // Cache.
    private final int max;

    private SubstitutionTable(HashMap<Integer, Map.Entry<String, String>> descriptors, int max) {
        this.descriptors = descriptors;
        this.max = max;
    }

    public HashMap<Integer, Map.Entry<String, String>> getDescriptors() {
        return descriptors;
    }

    public int getMax() {
        return max;
    }

    public static SubstitutionTable create(SubstitutionTableReader substitutionTableReader, int off, int len) {
        HashMap<Integer, Map.Entry<String, String>> descriptors = new HashMap<>();
        int end = off + len;
        int max = 0;
        while (off < end) {
            int index = substitutionTableReader.readShort(off);
            off += 2;
            String owner = substitutionTableReader.readUTF8(off);
            off += owner.length() + 2;
            String descriptor = substitutionTableReader.readUTF8(off);
            off += descriptor.length() + 2;
            descriptors.put(index, new HashMap.SimpleEntry<>(owner, descriptor));
            if (index > max) {
                max = index;
            }
        }
        return new SubstitutionTable(descriptors, max);
    }

    @Override
    public String toString() {
        return "Max : " + max + "\nDescriptors : " + descriptors;
    }
}
