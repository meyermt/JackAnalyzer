package com.meyermt.jack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by michaelmeyer on 2/24/17.
 */
public class JackFileReader {

    private final Path inputPath;
    private final static String JACK_EXT = ".jack";

    /**
     * Instantiates a new Jack file reader.
     *
     * @param inputFile the input file
     */
    public JackFileReader(String inputFile) {
        this.inputPath = Paths.get(inputFile);
    }

    /**
     * Gets input path.
     *
     * @return the input path
     */
    public Path getInputPath() {
        return this.inputPath;
    }

    /*
        Reads the file. Will exit the program if IOException encountered or file is not of .jack extension
    */
    public Map<String, List<String>> readFileOrFiles() {
        // if the filename doesn't have the .vm extension we will check if it is a directory and if it has VM files
        if (!inputPath.toString().endsWith(JACK_EXT)) {
            File input = inputPath.toFile();
            if (input.isDirectory()) {
                List<File> vmFiles = Arrays.asList(input.listFiles()).stream()
                        .filter(file -> file.isFile())
                        .filter(file -> file.getAbsolutePath().endsWith(JACK_EXT))
                        .collect(Collectors.toList());
                if (vmFiles.isEmpty()) {
                    System.out.println("Directory specified has no .vm files. Please re-run with a new directory");
                    System.exit(1);
                } else {
                    return vmFiles.stream()
                            .map(file -> tryReadingLines(file.toPath()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                }
            } else {
                System.out.println("Only able to read files with .vm extension or a directory containing .vm files. Please rename and try again.");
                System.exit(1);
            }
        } else {
            Map<String, List<String>> oneFileMap = new HashMap<>();
            Map.Entry<String, List<String>> oneFile = tryReadingLines(inputPath);
            oneFileMap.put(oneFile.getKey(), oneFile.getValue());
            return oneFileMap;
        }
        // can't actually hit this but needed to compile
        return null;
    }

    private Map.Entry<String, List<String>> tryReadingLines(Path filePath) {
        try {
            List<String> fileLines = Files.readAllLines(filePath);
            List<String> cleanFileLines = removeComments(fileLines);
            return new AbstractMap.SimpleImmutableEntry<>(filePath.getFileName().toString(), cleanFileLines);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Unable to read file from: " + filePath);
            System.exit(1);
        }
        return null;
    }

    /*
        Removes blank lines, tabs, and comments from code
    */
    private List<String> removeComments(List<String> fileLines) {
        return fileLines.stream()
                .map(commentful -> commentful.replaceAll("(//.*)", ""))
                // remove tab characters, although in theory there shouldn't be any
                .map(tabful -> tabful.replaceAll("\t", ""))
                // remove blank lines after comment removal in case comment was the whole line
                .filter(line -> !line.equals(""))
                .collect(Collectors.toList());
    }

}
