package com.meyermt.jack;

import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
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

        cleanFilesAndLines.entrySet().stream()
                .map(file -> {
                    JackTokenizer tokenizer = new JackTokenizer();
                    return tokenizer.tokenize(file);
                })
                .peek(doc -> writeDocSysOut(doc.getValue()))
                .map(doc -> {
                    CompilationEngine engine = new CompilationEngine();
                    return engine.compile(doc);
                })
                .forEach(entry -> writeDocSysOut(entry.getValue()));

    }

    public static void writeDocSysOut(Document doc) {
        // write the content into xml file
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("mainT" + counter + ".xml"));
            //StreamResult result = new StreamResult(System.out);
            transformer.setOutputProperty("omit-xml-declaration", "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "html");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(source, result);
            counter++;
        } catch (TransformerException e) {

        }
    }

}
