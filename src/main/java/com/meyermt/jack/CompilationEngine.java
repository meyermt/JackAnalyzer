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

/**
 * Compiles tokenized elements into fully layered xml components ready to create vm files.
 * Created by michaelmeyer on 2/24/17.
 */
public class CompilationEngine {

    private DocumentBuilder docBuilder;
    private Document doc;
    private Element rootElement;
    private int itemInc = 0;

    private static final List<String> classDecs = Arrays.asList(new String[] {"static", "field"});
    private static final List<String> subroutineDecs = Arrays.asList(new String[] {"constructor", "function", "method"});
    private static final List<String> statementsList = Arrays.asList(new String[] {"let", "if", "while", "do", "return"});
    private static final List<String> ops = Arrays.asList(new String[] {"+", "-", "*", "/", "&", "|", "<", ">", "="});
    private static final List<String> unaryOps = Arrays.asList(new String[] { "~", "-"});

    /**
     * Instantiates a new Compilation engine.
     */
    public CompilationEngine() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docBuilder = docFactory.newDocumentBuilder();
            doc = docBuilder.newDocument();
            rootElement = doc.createElement("class");
            doc.appendChild(rootElement);
        } catch (ParserConfigurationException e) {
            System.out.println("hit an error");
        }
    }

    /* helper method that will get empty xml opening and closing tags on separate lines. Needed for TextComparer */
    private void addLineEndingsToEmptyElements() {
        NodeList nodeList = doc.getElementsByTagName("*");
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).getTextContent().trim().equals("")) {
                nodeList.item(i).setTextContent(" ");
            }
        }
    }

    /**
     * Compiles the code for a map entry consisting of a jack filename and its tokenized contents
     *
     * @param jackFileToDocument the jack filename to document mapping
     * @return a map entry of jack filename to XML Document
     */
    public Map.Entry<String, Document> compile(Map.Entry<String, Document> jackFileToDocument) {
        NodeList nodeList = jackFileToDocument.getValue().getElementsByTagName("*");
        itemInc++; // increment past tokens
        Node node = nodeList.item(itemInc);
        if (node.getNodeName().equals("keyword") && node.getTextContent().trim().equals("class")) {
            compileClass(nodeList);
        } else {
            throw new RuntimeException("Should be processing class element");
        }
        addLineEndingsToEmptyElements();
        return new AbstractMap.SimpleEntry<>(jackFileToDocument.getKey(), doc);
    };

    /*
        helper method that will copy a node from the tokenized xml to the compiled one and increment node
        tokenized node list
     */
    private Element copyNodeAndInc(NodeList nodeList) {
        Node node = nodeList.item(itemInc);
        Element element = doc.createElement(node.getNodeName());
        String textValue = node.getTextContent();
        element.appendChild(doc.createTextNode(textValue));
        itemInc++;
        return element;
    }

    /*
        Compiles the class part of a jack class
     */
    private void compileClass(NodeList nodeList) {
        //root class element already added
        rootElement.appendChild(copyNodeAndInc(nodeList)); // add class
        rootElement.appendChild(copyNodeAndInc(nodeList)); // add class name
        rootElement.appendChild(copyNodeAndInc(nodeList)); // add {
        Node node = nodeList.item(itemInc);
        while (node.getNodeName().equals("keyword") && classDecs.contains(node.getTextContent().trim())) {
            compileClassVarDec(nodeList);
            node = nodeList.item(itemInc);
        }
        while (node.getNodeName().equals("keyword") && subroutineDecs.contains(node.getTextContent().trim())) {
            compileSubRoutine(nodeList);
            node = nodeList.item(itemInc);
        }
        rootElement.appendChild(copyNodeAndInc(nodeList)); // add }
    }

    /*
        Compiles the class variable declaration part of a jack class
     */
    private void compileClassVarDec(NodeList nodeList) {
        Element classVarDec = doc.createElement("classVarDec");
        rootElement.appendChild(classVarDec);
        classVarDec.appendChild(copyNodeAndInc(nodeList)); // add keyword
        classVarDec.appendChild(copyNodeAndInc(nodeList)); // add type
        classVarDec.appendChild(copyNodeAndInc(nodeList)); // add first var name
        Node node = nodeList.item(itemInc);
        while (node.getTextContent().trim().equals(",")) {
            classVarDec.appendChild(copyNodeAndInc(nodeList)); // add the comma symbol
            classVarDec.appendChild(copyNodeAndInc(nodeList)); // must be another term so add it
            node = nodeList.item(itemInc); // either a comma or semi colon
        }
        classVarDec.appendChild(copyNodeAndInc(nodeList)); // add the semi colon
    }

    /*
        Compiles a subroutine declaration, parameter list, and subroutine body
     */
    private void compileSubRoutine(NodeList nodeList) {
        Element subroutineDec = doc.createElement("subroutineDec");
        rootElement.appendChild(subroutineDec);
        subroutineDec.appendChild(copyNodeAndInc(nodeList)); // add keyword
        subroutineDec.appendChild(copyNodeAndInc(nodeList)); // add void or type
        subroutineDec.appendChild(copyNodeAndInc(nodeList)); // add name
        subroutineDec.appendChild(copyNodeAndInc(nodeList)); // add (

        Element parameterList = doc.createElement("parameterList");
        subroutineDec.appendChild(parameterList);

        Node node = nodeList.item(itemInc);
        while (!node.getTextContent().trim().equals(")")) {
            parameterList.appendChild(copyNodeAndInc(nodeList)); // add all params before )
            node = nodeList.item(itemInc);
        }
        subroutineDec.appendChild(copyNodeAndInc(nodeList)); // add )

        Element subroutineBody = doc.createElement("subroutineBody");
        subroutineDec.appendChild(subroutineBody);
        subroutineBody.appendChild(copyNodeAndInc(nodeList)); // add {

        if (nodeList.item(itemInc).getTextContent().trim().equals("var")) {
            compileVarDec(subroutineBody, nodeList);
        }

        compileStatements(subroutineBody, nodeList);
        subroutineBody.appendChild(copyNodeAndInc(nodeList)); // add }
    }

    /*
        Compiles statements that are executed within a subroutine
     */
    private void compileStatements(Element element, NodeList nodeList) {
        String stateNode = nodeList.item(itemInc).getTextContent().trim();
        Element statements = doc.createElement("statements");
        element.appendChild(statements);
        while (statementsList.contains(stateNode)) {

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

    /*
        Compiles return statement at end of subroutine
     */
    private void compileReturn(Element element, NodeList nodeList) {
        Element returnStatement = doc.createElement("returnStatement");
        element.appendChild(returnStatement);
        returnStatement.appendChild(copyNodeAndInc(nodeList)); // add return
        if (!nodeList.item(itemInc).getTextContent().trim().equals(";")) {
            compileExpression(returnStatement, nodeList);
        }
        returnStatement.appendChild(copyNodeAndInc(nodeList)); // add ;
    }

    /*
        Compiles do statement
     */
    private void compileDo(Element element, NodeList nodeList) {
        Element doStatement = doc.createElement("doStatement");
        element.appendChild(doStatement);
        doStatement.appendChild(copyNodeAndInc(nodeList)); // add do
        doStatement.appendChild(copyNodeAndInc(nodeList)); // add sub or class or var
        if (nodeList.item(itemInc).getTextContent().trim().equals("(")) {
            doStatement.appendChild(copyNodeAndInc(nodeList)); // add (
            compileExpressionList(doStatement, nodeList);
            doStatement.appendChild(copyNodeAndInc(nodeList)); // add )
        } else {
            doStatement.appendChild(copyNodeAndInc(nodeList)); // add .
            doStatement.appendChild(copyNodeAndInc(nodeList)); // add sub name
            doStatement.appendChild(copyNodeAndInc(nodeList)); // add (
            compileExpressionList(doStatement, nodeList);
            doStatement.appendChild(copyNodeAndInc(nodeList)); // add )
        }
        doStatement.appendChild(copyNodeAndInc(nodeList)); // add ;
    }

    /*
        Compiles a while statement
     */
    private void compileWhile(Element element, NodeList nodeList) {
        Element whileStatement = doc.createElement("whileStatement");
        element.appendChild(whileStatement);
        whileStatement.appendChild(copyNodeAndInc(nodeList)); // add while
        whileStatement.appendChild(copyNodeAndInc(nodeList)); // add (
        compileExpression(whileStatement, nodeList);
        whileStatement.appendChild(copyNodeAndInc(nodeList)); // add )
        whileStatement.appendChild(copyNodeAndInc(nodeList)); // add {
        compileStatements(whileStatement, nodeList);
        whileStatement.appendChild(copyNodeAndInc(nodeList)); // add }
    }

    /*
        Compiles an expression list
     */
    private void compileExpressionList(Element element, NodeList nodeList) {
        Element expList = doc.createElement("expressionList");
        element.appendChild(expList);
        String parenNode = nodeList.item(itemInc).getTextContent().trim();
        while (!parenNode.equals(")")) {
            compileExpression(expList, nodeList);
            String comNode = nodeList.item(itemInc).getTextContent().trim();
            if (comNode.equals(",")) {
                expList.appendChild(copyNodeAndInc(nodeList));
            }
            parenNode = nodeList.item(itemInc).getTextContent().trim();
        }
    }

    /*
        Compiles an expression
     */
    private void compileExpression(Element statement, NodeList nodeList) {
        Element expression = doc.createElement("expression");
        statement.appendChild(expression);
        compileTerm(expression, nodeList);
        String op = nodeList.item(itemInc).getTextContent().trim();
        while (ops.contains(op)) {
            expression.appendChild(copyNodeAndInc(nodeList)); // add the op
            compileTerm(expression, nodeList);
            op = nodeList.item(itemInc).getTextContent().trim();
        }
    }

    /*
        Compiles a term
     */
    private void compileTerm(Element expression, NodeList nodeList) {
        Element term = doc.createElement("term");
        expression.appendChild(term);
        if (unaryOps.contains(nodeList.item(itemInc).getTextContent().trim())) {
            term.appendChild(copyNodeAndInc(nodeList)); // add unary op
            compileTerm(term, nodeList);
        } else if (nodeList.item(itemInc).getTextContent().trim().equals("(")) {
            term.appendChild(copyNodeAndInc(nodeList)); // add (
            compileExpression(term, nodeList);
            term.appendChild(copyNodeAndInc(nodeList)); // add )
        } else {
            term.appendChild(copyNodeAndInc(nodeList)); // add first part of term
            if (nodeList.item(itemInc).getTextContent().trim().equals("[")) {
                term.appendChild(copyNodeAndInc(nodeList)); // add [
                compileExpression(term, nodeList);
                term.appendChild(copyNodeAndInc(nodeList)); // add ]
            } else if (nodeList.item(itemInc).getTextContent().trim().equals(".")) {
                term.appendChild(copyNodeAndInc(nodeList)); // add .
                term.appendChild(copyNodeAndInc(nodeList)); // add sub name
                term.appendChild(copyNodeAndInc(nodeList)); // add (
                compileExpressionList(term, nodeList);
                term.appendChild(copyNodeAndInc(nodeList)); // add )
            } else if (nodeList.item(itemInc).getTextContent().trim().equals("(")) {
                term.appendChild(copyNodeAndInc(nodeList)); // add sub name
                term.appendChild(copyNodeAndInc(nodeList)); // add (
                compileExpressionList(term, nodeList);
                term.appendChild(copyNodeAndInc(nodeList)); // add )
            }
        }
    }

    /*
        Compiles a let statement
     */
    private void compileLet(Element statements, NodeList nodeList) {
        Element letStatement = doc.createElement("letStatement");
        statements.appendChild(letStatement);
        letStatement.appendChild(copyNodeAndInc(nodeList)); // add let
        letStatement.appendChild(copyNodeAndInc(nodeList)); // add var name
        if (nodeList.item(itemInc).getTextContent().trim().equals("[")) {
            letStatement.appendChild(copyNodeAndInc(nodeList)); // add [
            compileExpression(letStatement, nodeList);
            letStatement.appendChild(copyNodeAndInc(nodeList)); // add ]
        }
        letStatement.appendChild(copyNodeAndInc(nodeList)); // add =
        compileExpression(letStatement, nodeList);
        letStatement.appendChild(copyNodeAndInc(nodeList)); // add ;
    }

    /*
        Compiles an if statement
     */
    private void compileIf(Element subroutineBody, NodeList nodeList) {
        Element ifStatement = doc.createElement("ifStatement");
        subroutineBody.appendChild(ifStatement);
        ifStatement.appendChild(copyNodeAndInc(nodeList)); // add if
        ifStatement.appendChild(copyNodeAndInc(nodeList)); // add (
        compileExpression(ifStatement, nodeList);
        ifStatement.appendChild(copyNodeAndInc(nodeList)); // add )
        ifStatement.appendChild(copyNodeAndInc(nodeList)); // add {
        compileStatements(ifStatement, nodeList);
        ifStatement.appendChild(copyNodeAndInc(nodeList)); // add }
        if (nodeList.item(itemInc).getTextContent().trim().equals("else")) {
            ifStatement.appendChild(copyNodeAndInc(nodeList)); // add if
            ifStatement.appendChild(copyNodeAndInc(nodeList)); // add {
            compileStatements(ifStatement, nodeList);
            ifStatement.appendChild(copyNodeAndInc(nodeList)); // add }
        }
    }

    /*
        Compiles the variable declarations of a subroutine
     */
    private void compileVarDec(Element subroutineBody, NodeList nodeList) {
        Node nodeVar = nodeList.item(itemInc);
        while (nodeVar.getTextContent().trim().equals("var")) {
            Element varDec = doc.createElement("varDec");
            subroutineBody.appendChild(varDec);
            varDec.appendChild(copyNodeAndInc(nodeList)); // add var
            varDec.appendChild(copyNodeAndInc(nodeList)); // add type
            varDec.appendChild(copyNodeAndInc(nodeList)); // add name
            Node nodeCom = nodeList.item(itemInc);
            while (nodeCom.getTextContent().trim().equals(",")) {
                varDec.appendChild(copyNodeAndInc(nodeList)); // add comma
                varDec.appendChild(copyNodeAndInc(nodeList)); // add name
                nodeCom = nodeList.item(itemInc); // either , or ;
            }
            varDec.appendChild(copyNodeAndInc(nodeList));
            nodeVar = nodeList.item(itemInc); // either another var or statement
        }
    }
}
