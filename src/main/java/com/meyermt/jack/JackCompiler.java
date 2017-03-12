package com.meyermt.jack;

import java.util.List;
import java.util.Map;

/**
 * Main driver for the Jack Compiler program. Uses reader to read in all .jack files, then passes to tokenizer and
 * compilation engine to create vm code. Finally, passes all filenames and their vm code lines to the writer.
 * Created by michaelmeyer on 2/24/17.
 */
public class JackCompiler {

    /**
     * The entry point of this application. Takes filename or dir args.
     *
     * @param args a single file filename or a directory with .jack files
     */
    public static void main(String[] args) {

        JackFileReader reader = new JackFileReader(args[0]);
        Map<String, List<String>> cleanFilesAndLines = reader.readFileOrFiles();
        JackVMWriter writer = new JackVMWriter(reader.getInputPath());

        cleanFilesAndLines.entrySet().stream()
                .map(file -> {
                    JackTokenizer tokenizer = new JackTokenizer();
                    return tokenizer.tokenize(file);
                })
                .map(doc -> {
                    CompilationEngine engine = new CompilationEngine();
                    return engine.compile(doc);
                })
                //.forEach(writer::writeDocOut);
                .forEach(writer::writeVMOut);
    }
}
