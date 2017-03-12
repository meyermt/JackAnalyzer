package com.meyermt.jack;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

/**
 * Writes all VM documents for each file.
 * Created by michaelmeyer on 2/27/17.
 */
public class JackVMWriter {

    private final Path outputPath;
    private final static String JACK_EXT = ".jack";
    private final static String XML_EXT = ".xml";
    private final static String VM_EXT = ".vm";

    /**
     * Instantiates a new Jack VM file writer.
     *
     * @param outputPath the output path
     */
    public JackVMWriter(Path outputPath) {
        this.outputPath = outputPath;
    }


    /**
     * Write vm lines out to file.
     *
     * @param classToVMLines the file name to vm lines mapping
     */
    public void writeVMOut(Map.Entry<String, List<String>> classToVMLines) {
        String fileName = outputPath.getFileName().toString();
        String className = classToVMLines.getKey();
        List<String> vmCode = classToVMLines.getValue();
        if (outputPath.toString().endsWith(JACK_EXT)) {
            String outputFileName = fileName.replace(JACK_EXT, VM_EXT);
            try {
                String outputDir = outputPath.toRealPath(NOFOLLOW_LINKS).getParent().toString();
                Path outputPath = Paths.get(outputDir, outputFileName);
                Files.write(outputPath, vmCode, Charset.defaultCharset());
            } catch (IOException e) {
                System.out.println("Issue encountered writing output file for: " + outputFileName);
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            String outputFileName = className.concat(VM_EXT);
            try {
                String outputDir = outputPath.toRealPath(NOFOLLOW_LINKS).toString();
                Path outputPath = Paths.get(outputDir, outputFileName);
                Files.write(outputPath, vmCode, Charset.defaultCharset());
            } catch (IOException e) {
                System.out.println("Issue encountered writing output file for: " + outputFileName);
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}
