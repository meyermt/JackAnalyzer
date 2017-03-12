package com.meyermt.jack;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.*;
import java.util.stream.Stream;

/**
 * Tokenizes .jack source code into a flat list of XML tokens to be processed by the compilation engine.
 * Created by michaelmeyer on 2/24/17.
 */
public class JackTokenizer {

    private List<String> savedStringLiterals = new ArrayList<>();
    private Document doc;
    private Element rootElement;
    private int savedIndexCounter = 0;

    /**
     * The constant keywords.
     */
    public static final List<String> keywords = Arrays.asList(new String[] {"class", "constructor", "method", "function", "field", "static", "boolean", "var",
                                                                "int", "char", "void", "true", "false", "null", "this", "let", "do", "if", "else", "while", "return"});
    /**
     * The constant symbols.
     */
    public static final List<String> symbols = Arrays.asList(new String[] { "{", "}", "(", ")", "[", "]", ".", ";", ",", "+", "-", "*", "/", "&", "|", "<", ">", "=", "~"});

    /**
     * Instantiates a new Jack tokenizer.
     */
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

    /**
     * Tokenizes source code that is passed in and returns as flat XML file
     *
     * @param jackFileToLines the jack filename mapped to lines of source code
     * @return the jack filename mapped to an XML Document of its source as tokens
     */
    public Map.Entry<String, Document> tokenize(Map.Entry<String, List<String>> jackFileToLines) {
            List<String> unblockedLines = removeBlockComments(jackFileToLines.getValue());
            unblockedLines.stream()
                .flatMap(line -> tokenizeLine(line))
                .filter(line -> !line.equals(""))
                .forEach(token -> processXML(token));
            return new AbstractMap.SimpleEntry<>(jackFileToLines.getKey(), doc);
    };

    /*
        Processes each token that is passed in by identifying which kind of token it is and passing to further processing
     */
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

    /*
        Given a line of source, passes to have literal strings compressed,
        spaces put in between symbols (because there may not be space),
        and then all tokens split on whitespace. This creates stream of tokens.
     */
    private Stream<String> tokenizeLine(String line) {
        line = compressAndSaveStringLiterals(line);
        line = spaceOutSymbols(line);
        String[] tokens = line.split("\\s+");
        return Arrays.asList(tokens).stream();
    }

    /*
        Indentifies string literals, saves their "true form" and compresses
        them into one "word" that can be processed later as one token
     */
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

    /*
        Looks for symbols and makes sure there is space around them
     */
    private String spaceOutSymbols(String line) {
        StringBuilder spacedOutLine = new StringBuilder();
        boolean isStringLiteral = false;
        for (int i = 0; i < line.length(); i++) {
            String s = line.substring(i, i + 1);
            if (s.equals("\"")) {
                isStringLiteral = !isStringLiteral;
            }
            if (symbols.contains(s) && !isStringLiteral) {
                spacedOutLine.append(" ");
                spacedOutLine.append(s);
                spacedOutLine.append(" ");
            } else {
                spacedOutLine.append(s);
            }
        }
        return spacedOutLine.toString();
    }

    /*
        Processes a token identified as a keyword and puts it in xml doc
     */
    private void processKeyword(String token) {
        Element keyword = doc.createElement("keyword");
        keyword.appendChild(doc.createTextNode(" " + token + " "));
        rootElement.appendChild(keyword);
    }

    /*
        Processes a token identified as a symbol and puts it in xml doc
    */
    private void processSymbol(String token) {
        Element symbol = doc.createElement("symbol");
        symbol.appendChild(doc.createTextNode(" " + token + " "));
        rootElement.appendChild(symbol);
    }

    /*
       Processes a token identified as an integer and puts it in xml doc
    */
    private void processInt(String token) {
        Element integerConstant = doc.createElement("integerConstant");
        integerConstant.appendChild(doc.createTextNode(" " + token + " "));
        rootElement.appendChild(integerConstant);
    }

    /*
        Processes a token identified as a String literal and puts it in xml doc.
        Processing involves grabbing the "true" value. Thus, this process does depend
        on ordering to be preserved.
    */
    private void processStringLiteral(String token, int savedStringIndex) {
        Element stringConstant = doc.createElement("stringConstant");
        String saved = savedStringLiterals.get(savedStringIndex);
        stringConstant.appendChild(doc.createTextNode(" " + saved + " "));
        rootElement.appendChild(stringConstant);
    }

    /*
       Processes a token identified as an identifier and puts it in xml doc
    */
    private void processIdentifier(String token) {
        Element identifier = doc.createElement("identifier");
        identifier.appendChild(doc.createTextNode(" " + token + " "));
        rootElement.appendChild(identifier);
    }

    /*
        Goes through the source and looks for block comments it can remove.
        (could add this to reader in future)
     */
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
