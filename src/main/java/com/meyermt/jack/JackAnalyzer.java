package com.meyermt.jack;

import java.util.List;
import java.util.Map;

/**
 * Created by michaelmeyer on 2/24/17.
 */
public class JackAnalyzer {

    static int counter = 0;

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
