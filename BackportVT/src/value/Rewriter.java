package value;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.objectweb.asm.Opcodes.ASM5;

/**
 * Created by Fabien GIACHERIO on 06/02/17.
 */
public class Rewriter {
    private static final String FOLDER_VALUE = "backportVT_result/";

    /**
     * The directory visited
     */
    private final String directory;

    /**
     * @param directory the working directory to walk.
     */
    private Rewriter(String directory) {
        this.directory = directory;
    }

    /**
     * Compiles all the classes present inside the working directory.
     *
     * @throws IOException TODO walk recursively.
     */
    public void compileDirectory() throws IOException {
        File[] files = new File(directory).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".class");
            }
        });
        if (files == null) {
            throw new IllegalStateException("Can not find the folder : " + directory);
        }
        for (File f : files) {
            compileClazz(f.toPath());
        }
    }

    /**
     * Compile the class at the given path according to the rewriting rules.
     *
     * @param path the path of the file to compileDirectory.
     * @throws IOException
     */
    private void compileClazz(final Path path) throws IOException {
        System.out.println("Writing class : " + path);
        writeClazzTo(path.getFileName().toString(), dump(path));
    }

    private byte[] dump(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            ClassNode cn = new ClassNode(ASM5);
            VTClassVisitor vtClassVisitor = new VTClassVisitor(cw);
            new ClassReader(bytes).accept(cn, 0);
//            AddReferenceMethodTransformer addReferenceMethodTransformer = new AddReferenceMethodTransformer(null);
//            addReferenceMethodTransformer.transform(cn);
            cn.accept(vtClassVisitor);

//

            // Returning the current class byte array.
            return cw.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Writes the given byte array to the specified path.
     *
     * @param path  the path to write the clazz.
     * @param clazz the byte array representing the clazz.
     * @throws IOException
     */
    private void writeClazzTo(final String path, final byte[] clazz) throws IOException {
        File f = new File(new File(directory), FOLDER_VALUE + path);
        if (!f.getParentFile().exists() && !f.getParentFile().mkdirs())
            throw new IOException("Cannot create directory " + f.getParentFile());
        FileOutputStream o = new FileOutputStream(f);
        o.write(clazz);
        o.close();
    }

    public static void main(final String[] args) throws IOException, URISyntaxException {
        if (args.length == 0) {
            throw new IllegalArgumentException("Please provide the directory to rewrite : -Dfolder=path");
        }
        String dir = args[0];
        // ${folder} is the folder parameter not set when launching from the ant script.
        System.out.println(dir);
        if (dir.isEmpty() || dir.equals("${folder}")) {
            throw new IllegalArgumentException("Please provide the directory to rewrite.");
        }
        if (dir.contains("-Dfolder=")) {
            String[] split = dir.split("-Dfolder=");
            if (split.length != 2) {
                throw new IllegalArgumentException("Please provide the directory to rewrite : -Dfolder=path");
            }
            dir = split[1];
        }
        System.out.println(dir);
        Rewriter rewriter = new Rewriter(dir);
        System.out.println("Directory : " + dir);
        rewriter.compileDirectory();
    }
}
