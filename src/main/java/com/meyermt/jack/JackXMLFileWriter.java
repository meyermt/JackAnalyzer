package com.meyermt.jack;

import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

/**
 * Created by michaelmeyer on 2/27/17.
 */
public class JackXMLFileWriter {

    private final Path outputPath;
    private final static String JACK_EXT = ".jack";
    private final static String XML_EXT = ".xml";

    public JackXMLFileWriter(Path outputPath) {
        this.outputPath = outputPath;
    }


    public void writeDocOut(Map.Entry<String, Document> fileNameToXML) {
        Document doc = fileNameToXML.getValue();
        String fileName = fileNameToXML.getKey().replaceAll(JACK_EXT, XML_EXT);
        String outputDir;
        try {
            if (outputPath.toString().endsWith(JACK_EXT)) {
                outputDir = outputPath.toRealPath(NOFOLLOW_LINKS).getParent().toString();
            } else {
                outputDir = outputPath.toRealPath(NOFOLLOW_LINKS).toString();
            }
            File newOutputFile = Paths.get(outputDir, fileName).toFile();
            writeXmlOutput(newOutputFile, doc);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create new file to write to for file " + fileName, e);
        }
    }

    private void writeXmlOutput(File newOutputFile, Document doc) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(newOutputFile);
            transformer.setOutputProperty("omit-xml-declaration", "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "html");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(source, result);
        } catch (TransformerException e) {
            throw new RuntimeException("Unable to write output xml for file: " + newOutputFile.getName(), e);
        }
    }
}
