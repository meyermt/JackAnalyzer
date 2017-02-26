package com.meyermt.jack;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by michaelmeyer on 2/24/17.
 */
public class CompilationEngine {

    private Document doc;
    private Element rootElement;
    private int itemInc = 0;

    private static final List<String> classDecs = Arrays.asList(new String[] {"static", "field"});
    private static final List<String> subroutineDecs = Arrays.asList(new String[] {"constructor", "function", "method"});
    private static final List<String> statements = Arrays.asList(new String[] {"let", "if", "while", "do", "return"});

    public CompilationEngine() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            doc = docBuilder.newDocument();
            rootElement = doc.createElement("class");
            doc.appendChild(rootElement);
        } catch (ParserConfigurationException e) {
            System.out.println("hit an error");
        }
    }

    public Function<Map.Entry<String, Document>, Map.Entry<String, Document>> compile = (jackFileToDocument) -> {
        System.out.println("in compile class");
        NodeList nodeList = jackFileToDocument.getValue().getElementsByTagName("*");
        Node node = nodeList.item(itemInc);
        if (node.getNodeName().equals("keyword") && node.getTextContent().trim().equals("class")) {
            compileClass(nodeList);
        } else {
            throw new RuntimeException("Should be processing class element");
        }
        return new AbstractMap.SimpleEntry<>(jackFileToDocument.getKey(), doc);
    };

    private void compileClass(NodeList nodeList) {
        //root class element already added
        rootElement.appendChild(nodeList.item(itemInc)); // add keyword
        itemInc++;
        rootElement.appendChild(nodeList.item(itemInc)); // add identifier
        itemInc++;
        rootElement.appendChild(nodeList.item(itemInc)); // add symbol
        itemInc++;
        Node node = nodeList.item(itemInc);
        while (node.getNodeName().equals("keyword") && classDecs.contains(node.getTextContent().trim())) {
            compileClassVarDec(nodeList);
        }
        while (node.getNodeName().equals("keyword") && subroutineDecs.contains(node.getTextContent().trim())) {
            compileSubRoutine(nodeList);
        }
    }

    private void compileClassVarDec(NodeList nodeList) {
        Element classVarDec = doc.createElement("classVarDec");
        rootElement.appendChild(classVarDec);
        classVarDec.appendChild(nodeList.item(itemInc)); // add keyword
        itemInc++;
        classVarDec.appendChild(nodeList.item(itemInc)); // add first identifier of possibly many
        itemInc++;
        Node node = nodeList.item(itemInc);
        while (node.getTextContent().trim().equals(",")) {
            classVarDec.appendChild(nodeList.item(itemInc)); // add the comma symbol
            itemInc++;
            classVarDec.appendChild(nodeList.item(itemInc)); // must be another term so add it
            itemInc++;
            node = nodeList.item(itemInc); // either a comma or semi colon
        }
        classVarDec.appendChild(node); // add the semi colon
        itemInc++;
    }

    private void compileSubRoutine(NodeList nodeList) {
        Element subroutineDec = doc.createElement("subroutineDec");
        rootElement.appendChild(subroutineDec);
        subroutineDec.appendChild(nodeList.item(itemInc)); // add keyword
        itemInc++;
        subroutineDec.appendChild(nodeList.item(itemInc)); // add void or type
        itemInc++;
        subroutineDec.appendChild(nodeList.item(itemInc)); // add name
        itemInc++;
        subroutineDec.appendChild(nodeList.item(itemInc)); // add (

        Element parameterList = doc.createElement("parameterList");
        subroutineDec.appendChild(parameterList);

        itemInc++;
        Node node = nodeList.item(itemInc);
        while (!node.getTextContent().trim().equals(")")) {
            parameterList.appendChild(nodeList.item(itemInc)); // add all params before )
            itemInc++;
            node = nodeList.item(itemInc);
        }
        subroutineDec.appendChild(node); // add )
        itemInc++;
        Element subroutineBody = doc.createElement("subroutineBody");
        subroutineDec.appendChild(subroutineBody);
        subroutineBody.appendChild(nodeList.item(itemInc)); // add {
        itemInc++;
        if (nodeList.item(itemInc).getTextContent().trim().equals("var")) {
            Element varDec = doc.createElement("varDec");
            subroutineBody.appendChild(varDec);
            compileVarDec(varDec, nodeList);
        }

        String stateNode = nodeList.item(itemInc).getTextContent().trim();
        while (statements.contains(stateNode)) {
            Element statements = doc.createElement("statements");
            subroutineBody.appendChild(statements);

            if (stateNode.equals("let")) {
                compileLet(statements, nodeList);
            } else if (stateNode.equals("if")) {
                compileIf(statements, nodeList);
            } else if (stateNode.equals("while")) {
                compileWhile(statements, nodeList);
            } else if (stateNode.equals("do")) {
                compileDo(statements, nodeList);
            } else {
                compileReturn(statements, nodeList);
            }

            stateNode = nodeList.item(itemInc).getTextContent().trim();
        }
    }

    private void compileExpression(Element statements, NodeList nodeList) {

    }

    private void compileLet(Element statements, NodeList nodeList) {
        Element letStatement = doc.createElement("letStatement");
        statements.appendChild(letStatement);
        letStatement.appendChild(nodeList.item(itemInc)); // add let
        itemInc++;
        letStatement.appendChild(nodeList.item(itemInc)); // add var name
        itemInc++;
        if (nodeList.item(itemInc).getTextContent().trim().equals("[")) {
            letStatement.appendChild(nodeList.item(itemInc)); // add [
            itemInc++;
            compileExpression(letStatement, nodeList);
            letStatement.appendChild(nodeList.item(itemInc)); // add ]
        }

    }

    private void compileIf(Element subroutineBody, NodeList nodeList) {

    }

    private void compileVarDec(Element varDec, NodeList nodeList) {
        Node nodeVar = nodeList.item(itemInc);
        while (nodeVar.getTextContent().trim().equals("var")) {
            varDec.appendChild(nodeList.item(itemInc)); // add var
            itemInc++;
            varDec.appendChild(nodeList.item(itemInc)); // add name
            itemInc++;
            Node nodeCom = nodeList.item(itemInc);
            while (nodeCom.getTextContent().trim().equals(",")) {
                varDec.appendChild(nodeList.item(itemInc)); // add comma
                itemInc++;
                nodeCom = nodeList.item(itemInc); // either , or ;
            }
            varDec.appendChild(nodeCom);
            itemInc++;
            nodeVar = nodeList.item(itemInc); // either another var or statement
        }
    }

    private void compileParameterList() {

    }
}
