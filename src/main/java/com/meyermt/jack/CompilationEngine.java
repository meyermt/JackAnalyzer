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
    private static final List<String> ops = Arrays.asList(new String[] {"+", "-", "*", "/", "&", "|", "<", ">", "="});
    private static final List<String> unaryOps = Arrays.asList(new String[] { "~", "-"});

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

        compileStatements(subroutineBody, nodeList);
    }

    private void compileStatements(Element element, NodeList nodeList) {
        String stateNode = nodeList.item(itemInc).getTextContent().trim();
        while (statements.contains(stateNode)) {
            Element statements = doc.createElement("statements");
            element.appendChild(statements);

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

    private void compileReturn(Element element, NodeList nodeList) {
        Element returnStatement = doc.createElement("returnStatement");
        element.appendChild(returnStatement);
        returnStatement.appendChild(nodeList.item(itemInc)); // add return
        itemInc++;
        if (nodeList.item(itemInc).getNodeName().equals("identifier")) {
            compileExpression(returnStatement, nodeList);
        }
        returnStatement.appendChild(nodeList.item(itemInc)); // add ;
        itemInc++;
    }

    private void compileDo(Element element, NodeList nodeList) {
        Element doStatement = doc.createElement("doStatement");
        element.appendChild(doStatement);
        doStatement.appendChild(nodeList.item(itemInc)); // add do
        itemInc++;
        doStatement.appendChild(nodeList.item(itemInc)); // add sub or class or var
        itemInc++;
        if (nodeList.item(itemInc).getTextContent().trim().equals("(")) {
            doStatement.appendChild(nodeList.item(itemInc)); // add (
            itemInc++;
            compileExpressionList(doStatement, nodeList);
            doStatement.appendChild(nodeList.item(itemInc)); // add )
            itemInc++;
        } else {
            doStatement.appendChild(nodeList.item(itemInc)); // add .
            itemInc++;
            doStatement.appendChild(nodeList.item(itemInc)); // add sub name
            itemInc++;
            doStatement.appendChild(nodeList.item(itemInc)); // add (
            itemInc++;
            compileExpressionList(doStatement, nodeList);
            doStatement.appendChild(nodeList.item(itemInc)); // add )
            itemInc++;
        }
    }

    private void compileWhile(Element element, NodeList nodeList) {
        Element whileStatement = doc.createElement("whileStatement");
        element.appendChild(whileStatement);
        whileStatement.appendChild(nodeList.item(itemInc)); // add while
        itemInc++;
        whileStatement.appendChild(nodeList.item(itemInc)); // add (
        itemInc++;
        compileExpression(whileStatement, nodeList);
        whileStatement.appendChild(nodeList.item(itemInc)); // add )
        itemInc++;
        whileStatement.appendChild(nodeList.item(itemInc)); // add {
        itemInc++;
        compileStatements(whileStatement, nodeList);
        whileStatement.appendChild(nodeList.item(itemInc)); // add }
        itemInc++;
    }

    private void compileExpressionList(Element element, NodeList nodeList) {
        Element expList = doc.createElement("expressionList");
        element.appendChild(expList);
        String parenNode = nodeList.item(itemInc).getTextContent().trim();
        while (!parenNode.equals(")")) {
            compileExpression(expList, nodeList);
            String comNode = nodeList.item(itemInc).getTextContent().trim();
            while (comNode.equals(",")) {
                compileExpression(expList, nodeList);
                comNode = nodeList.item(itemInc).getTextContent().trim();
            }
        }
    }

    private void compileExpression(Element statement, NodeList nodeList) {
        Element expression = doc.createElement("expression");
        statement.appendChild(expression);
        compileTerm(expression, nodeList);
        String op = nodeList.item(itemInc).getTextContent().trim();
        while (ops.contains(op)) {
            expression.appendChild(nodeList.item(itemInc)); // add the op
            itemInc++;
            compileTerm(expression, nodeList);
        }
    }

    private void compileTerm(Element expression, NodeList nodeList) {
        Element term = doc.createElement("term");
        expression.appendChild(term);
        if (unaryOps.contains(nodeList.item(itemInc).getTextContent().trim())) {
            term.appendChild(nodeList.item(itemInc)); // add unary op
            itemInc++;
            compileTerm(term, nodeList);
        } else if (nodeList.item(itemInc).getTextContent().trim().equals("(")) {
            term.appendChild(nodeList.item(itemInc)); // add (
            itemInc++;
            compileExpression(term, nodeList);
            term.appendChild(nodeList.item(itemInc)); // add )
            itemInc++;
        } else {
            term.appendChild(nodeList.item(itemInc)); // add first part of term
            itemInc++;
            if (nodeList.item(itemInc).getTextContent().trim().equals("[")) {
                compileExpression(term, nodeList);
                term.appendChild(nodeList.item(itemInc)); // add )
                itemInc++;
            } else if (nodeList.item(itemInc).getTextContent().trim().equals(".")) {
                term.appendChild(nodeList.item(itemInc)); // add .
                itemInc++;
                term.appendChild(nodeList.item(itemInc)); // add sub name
                itemInc++;
                term.appendChild(nodeList.item(itemInc)); // add (
                itemInc++;
                compileExpressionList(term, nodeList);
                term.appendChild(nodeList.item(itemInc)); // add )
                itemInc++;
            } else if (nodeList.item(itemInc).getTextContent().trim().equals("(")) {
                term.appendChild(nodeList.item(itemInc)); // add sub name
                itemInc++;
                term.appendChild(nodeList.item(itemInc)); // add (
                itemInc++;
                compileExpressionList(term, nodeList);
                term.appendChild(nodeList.item(itemInc)); // add )
                itemInc++;
            }
        }
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
            itemInc++;
        }
        letStatement.appendChild(nodeList.item(itemInc)); // add =
        itemInc++;
        compileExpression(letStatement, nodeList);
        letStatement.appendChild(nodeList.item(itemInc)); // add ;
    }

    private void compileIf(Element subroutineBody, NodeList nodeList) {
        Element ifStatement = doc.createElement("ifStatement");
        subroutineBody.appendChild(ifStatement);
        ifStatement.appendChild(nodeList.item(itemInc)); // add if
        itemInc++;
        ifStatement.appendChild(nodeList.item(itemInc)); // add (
        itemInc++;
        compileExpression(ifStatement, nodeList);
        ifStatement.appendChild(nodeList.item(itemInc)); // add )
        itemInc++;
        ifStatement.appendChild(nodeList.item(itemInc)); // add {
        itemInc++;
        compileStatements(ifStatement, nodeList);
        ifStatement.appendChild(nodeList.item(itemInc)); // add }
        itemInc++;
        if (nodeList.item(itemInc).getTextContent().trim().equals("else")) {
            ifStatement.appendChild(nodeList.item(itemInc)); // add if
            itemInc++;
            ifStatement.appendChild(nodeList.item(itemInc)); // add {
            itemInc++;
            compileStatements(ifStatement, nodeList);
            ifStatement.appendChild(nodeList.item(itemInc)); // add }
            itemInc++;
        }
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
