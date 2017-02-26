package com.meyermt.jack;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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

    public static void main(String[] args) {

        System.out.println(args[0]);
        JackFileReader reader = new JackFileReader(args[0]);
        Map<String, List<String>> cleanFilesAndLines = reader.readFileOrFiles();

        JackTokenizer tokenizer = new JackTokenizer();
        CompilationEngine engine = new CompilationEngine();
        System.out.println("running");
        cleanFilesAndLines.entrySet().stream()
                .map(tokenizer.tokenize)
                .map(engine.compile)
                .forEach(entry -> writeDocSysOut(entry.getValue()));

    }

    private static void writeDocSysOut(Document doc) {
        // write the content into xml file
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("mainT.xml"));
            //StreamResult result = new StreamResult(System.out);
            transformer.setOutputProperty("omit-xml-declaration", "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(source, result);
        } catch (TransformerException e) {

        }
    }

}
