package com.meyermt.jack;

import java.util.List;
import java.util.Map;

/**
 * Main driver for the Jack Analyzer program. Uses reader to read in all .jack files, then passes to tokenizer and
 * compilation engine to create xml. Finally, passes all filenames and their XML documents to the writer.
 * Created by michaelmeyer on 2/24/17.
 */
public class JackAnalyzer {

    /**
     * The entry point of this application. Takes filename or dir args.
     *
     * @param args a single file filename or a directory with .jack files
     */
    public static void main(String[] args) {

        JackFileReader reader = new JackFileReader(args[0]);
        Map<String, List<String>> cleanFilesAndLines = reader.readFileOrFiles();
        JackXMLFileWriter writer = new JackXMLFileWriter(reader.getInputPath());

        cleanFilesAndLines.entrySet().stream()
                .map(file -> {
                    JackTokenizer tokenizer = new JackTokenizer();
                    return tokenizer.tokenize(file);
                })
                .map(doc -> {
                    CompilationEngine engine = new CompilationEngine();
                    return engine.compile(doc);
                })
                .forEach(writer::writeDocOut);
    }
}
