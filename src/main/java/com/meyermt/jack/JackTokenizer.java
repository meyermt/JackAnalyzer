package com.meyermt.jack;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by michaelmeyer on 2/24/17.
 */
public class JackTokenizer {

    private List<String> savedStringLiterals = new ArrayList<>();
    private Document doc;
    private Element rootElement;
    private int savedIndexCounter = 0;
    public static final List<String> keywords = Arrays.asList(new String[] {"class", "constructor", "method", "function", "field", "static", "boolean", "var",
                                                                "int", "char", "void", "true", "false", "null", "this", "let", "do", "if", "else", "while", "return"});
    public static final List<String> symbols = Arrays.asList(new String[] { "{", "}", "(", ")", "[", "]", ".", ";", ",", "+", "-", "*", "/", "&", "|", "<", ">", "=", "~"});

    public JackTokenizer() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            doc = docBuilder.newDocument();
            rootElement = doc.createElement("tokens");
            doc.appendChild(rootElement);
        } catch (ParserConfigurationException e) {
            System.out.println("hit an error");
        }
    }

    public Map.Entry<String, Document> tokenize(Map.Entry<String, List<String>> jackFileToLines) {
            List<String> unblockedLines = removeBlockComments(jackFileToLines.getValue());
            unblockedLines.stream()
                .flatMap(line -> tokenizeLine(line))
                .filter(line -> !line.equals(""))
                .forEach(token -> processXML(token));
            return new AbstractMap.SimpleEntry<>(jackFileToLines.getKey(), doc);
    };

    private void processXML(String token) {
        if (keywords.contains(token)) {
            processKeyword(token);
        } else if (symbols.contains(token)) {
            processSymbol(token);
        } else if (token.matches("^-?\\d+$")) {
            processInt(token);
        } else if (token.matches("^\".+\"$")) {
            processStringLiteral(token, savedIndexCounter);
            savedIndexCounter++;
        } else {
            processIdentifier(token);
        }
    }

    private Stream<String> tokenizeLine(String line) {
        line = compressAndSaveStringLiterals(line);
        line = spaceOutSymbols(line);
        String[] tokens = line.split("\\s+");
        return Arrays.asList(tokens).stream();
    }

    private String compressAndSaveStringLiterals(String line) {
        boolean inLiteral = false;
        boolean literalExists = false;
        String savedStringLiteral = "";
        StringBuilder compressedLiteralLine = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '"') {
                literalExists = true;
                if (inLiteral) {
                    inLiteral = false;
                } else {
                    inLiteral = true;
                }
            }
            if (inLiteral && !(line.charAt(i) == '"')) {
                savedStringLiteral += line.charAt(i);
                if (!(line.charAt(i) == ' ')) {
                    compressedLiteralLine.append(line.charAt(i));
                }
            } else {
                compressedLiteralLine.append(line.charAt(i));
            }
        }
        if (literalExists) {
            savedStringLiterals.add(savedStringLiteral);
        }
        return compressedLiteralLine.toString();
    }

    private String spaceOutSymbols(String line) {
        StringBuilder spacedOutLine = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            if (symbols.contains(line.substring(i, i + 1))) {
                spacedOutLine.append(" ");
                spacedOutLine.append(line.substring(i, i + 1));
                spacedOutLine.append(" ");
            } else {
                spacedOutLine.append(line.substring(i, i + 1));
            }
        }
        return spacedOutLine.toString();
    }

    private void processKeyword(String token) {
        Element keyword = doc.createElement("keyword");
        keyword.appendChild(doc.createTextNode(" " + token + " "));
        rootElement.appendChild(keyword);
    }

    private void processSymbol(String token) {
        Element symbol = doc.createElement("symbol");
        symbol.appendChild(doc.createTextNode(" " + token + " "));
        rootElement.appendChild(symbol);
    }

    private void processInt(String token) {
        Element integerConstant = doc.createElement("integerConstant");
        integerConstant.appendChild(doc.createTextNode(" " + token + " "));
        rootElement.appendChild(integerConstant);
    }

    private void processStringLiteral(String token, int savedStringIndex) {
        Element stringConstant = doc.createElement("stringConstant");
        String saved = savedStringLiterals.get(savedStringIndex);
        stringConstant.appendChild(doc.createTextNode(" " + saved + " "));
        rootElement.appendChild(stringConstant);
    }

    private void processIdentifier(String token) {
        Element identifier = doc.createElement("identifier");
        identifier.appendChild(doc.createTextNode(" " + token + " "));
        rootElement.appendChild(identifier);
    }

    private static List<String> removeBlockComments(List<String> blockedLines) {
        List<String> unBlockedLines = new ArrayList<>();
        boolean blockStarted = false;
        String blockPattern = "(\\**)(\\\\*)";
        for (String line : blockedLines) {
            if (line.contains("/*") || blockStarted) {
                if (line.contains("*/")) {
                    blockStarted = false;
                } else {
                    blockStarted = true;
                }
            } else {
                unBlockedLines.add(line);
            }
        }
        return unBlockedLines;
    }
}
