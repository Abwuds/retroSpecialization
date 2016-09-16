package specialization;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.SubstitutionTable;
import rt.RT;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by Jefferson Mangue on 26/05/2016.
 */
public class Rewriter {

    private static final String FOLDER_SPECIALIZATION = "compiled/";
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

    /**
     * Writes the given byte array to the specified path.
     *
     * @param path  the path to write the clazz.
     * @param clazz the byte array representing the clazz.
     * @throws IOException
     */
    private void writeClazzTo(final String path, final byte[] clazz) throws IOException {
        File f = new File(new File(directory), FOLDER_SPECIALIZATION + path);
        if (!f.getParentFile().exists() && !f.getParentFile().mkdirs())
            throw new IOException("Cannot create directory " + f.getParentFile());
        FileOutputStream o = new FileOutputStream(f);
        o.write(clazz);
        o.close();
    }

    private byte[] dump(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            FrontClassVisitor frontClassVisitor = new FrontClassVisitor(cw);
            // TODO do not write this attribute on non modified classes.
            new ClassReader(bytes).accept(frontClassVisitor, new Attribute[]{new SubstitutionTable()}, 0);
            // Writing the back factory if one exists.
            writeBackFactoryClazz(frontClassVisitor);
            // Returning the current class byte array.
            return cw.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void writeBackFactoryClazz(FrontClassVisitor frontClassVisitor) throws IOException {
        if (frontClassVisitor.hasBackFactory()) {
            writeClazzTo(frontClassVisitor.getBackFactoryName() + ".class", frontClassVisitor.getBackFactoryBytes());
        }
    }

    public static void main(final String[] args) throws IOException, URISyntaxException {
        String dir = args[0];
        // ${folder} is the folder parameter not set when launching from the ant script.
        if (args.length < 0 || dir.isEmpty() || dir.equals("${folder}")) {
            throw new IllegalArgumentException("Please provide the directory to rewrite.");
        }
        Rewriter rewriter = new Rewriter(dir);
        rewriter.compileDirectory();
        rewriter.copyRTClazz(computeRTPackageAbsolutePath());
        System.out.println("End");
    }

    private void copyRTClazz(String dir) throws IOException {
        System.out.println(dir);
        File[] files = new File(dir).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File d, String name) {
                return name.endsWith(".class");
            }
        });
        if (files == null) {
            throw new IllegalStateException("Please provide the rt folder next to the Backport jar file. In the directory : " + dir);
        }
        for (File f : files) {
            byte[] bytes = Files.readAllBytes(f.toPath());
            writeClazzTo("rt/" + f.getName(), bytes);
        }
    }

    private static String computeRTPackageAbsolutePath() throws URISyntaxException {
        String path = Rewriter.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        String[] split = path.split("/");
        StringBuilder sb = new StringBuilder("/");
        String sep = "";
        for (int i = 0; i < split.length - 1; i++) {
            String s = split[i];
            sb.append(s).append(sep);
            sep = "/";
        }
        path = sb.toString();
        return path + RT.class.getPackage().getName().replace('.', '/') + "/";
    }
}
