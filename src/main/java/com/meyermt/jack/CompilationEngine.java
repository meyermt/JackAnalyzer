package com.meyermt.jack;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.*;

/**
 * Compiles tokenized elements into fully runnable vm code.
 * Created by michaelmeyer on 2/24/17.
 */
public class CompilationEngine {

    private DocumentBuilder docBuilder;
    private List<String> vmCode = new ArrayList<>();
    private Document doc;
    private Element rootElement;
    private int itemInc = 0;
    private int constructorFieldCount = 0;
    private String className = "";
    private String maybeSubName = "";
    private int ifInc = 0;
    private int whileInc = 0;
    private SymbolTable classTable = new SymbolTable();

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

    /**
     * Compiles the code for a map entry consisting of a jack filename and its tokenized contents
     *
     * @param jackFileToDocument the jack filename to document mapping
     * @return a map entry of jack filename to XML Document
     */
    public Map.Entry<String, List<String>> compile(Map.Entry<String, Document> jackFileToDocument) {
        NodeList nodeList = jackFileToDocument.getValue().getElementsByTagName("*");
        itemInc++; // increment past tokens
        Node node = nodeList.item(itemInc);
        if (node.getNodeName().equals("keyword") && node.getTextContent().trim().equals("class")) {
            compileClass(nodeList);
        } else {
            throw new RuntimeException("Should be processing class element");
        }
        return new AbstractMap.SimpleEntry<>(className, vmCode);
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

    private String getNodeTextValue(NodeList nodeList) {
        Node node = nodeList.item(itemInc);
        Element element = doc.createElement(node.getNodeName());
        return node.getTextContent().substring(1, node.getTextContent().length() - 1);
    }

    private String getNodeTypeValue(NodeList nodeList) {
        Node node = nodeList.item(itemInc);
        return node.getNodeName();
    }

    /*
        Compiles the class part of a jack class
     */
    private void compileClass(NodeList nodeList) {
        //root class element already added
        rootElement.appendChild(copyNodeAndInc(nodeList)); // add class
        className = getNodeTextValue(nodeList);
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
        String kind = getNodeTextValue(nodeList);
        if (kind.equals("field")) {
            constructorFieldCount++;
        }
        classVarDec.appendChild(copyNodeAndInc(nodeList)); // add keyword
        String type = getNodeTextValue(nodeList);
        classVarDec.appendChild(copyNodeAndInc(nodeList)); // add type
        String name = getNodeTextValue(nodeList);
        classTable.addNewItem(name, type, kind);
        classVarDec.appendChild(copyNodeAndInc(nodeList)); // add first var name
        Node node = nodeList.item(itemInc);
        while (node.getTextContent().trim().equals(",")) {
            if (kind.equals("field")) {
                constructorFieldCount++;
            }
            classVarDec.appendChild(copyNodeAndInc(nodeList)); // add the comma symbol
            name = getNodeTextValue(nodeList);
            classTable.addNewItem(name, type, kind);
            classVarDec.appendChild(copyNodeAndInc(nodeList)); // must be another term so add it
            node = nodeList.item(itemInc); // either a comma or semi colon
        }
        classVarDec.appendChild(copyNodeAndInc(nodeList)); // add the semi colon
    }

    /*
        Compiles a subroutine declaration, parameter list, and subroutine body
     */
    private void compileSubRoutine(NodeList nodeList) {
        SymbolTable subTable = new SymbolTable();
        Element subroutineDec = doc.createElement("subroutineDec");
        rootElement.appendChild(subroutineDec);

        String subType = getNodeTextValue(nodeList);
        if (subType.contains("method")) {
            subTable.addNewItem(className, "class", "argument");
        }
        subroutineDec.appendChild(copyNodeAndInc(nodeList)); // add keyword
        subroutineDec.appendChild(copyNodeAndInc(nodeList)); // add void or type
        String functionName = getNodeTextValue(nodeList);
        subroutineDec.appendChild(copyNodeAndInc(nodeList)); // add name

        subroutineDec.appendChild(copyNodeAndInc(nodeList)); // add (

        Element parameterList = doc.createElement("parameterList");
        subroutineDec.appendChild(parameterList);

        Node node = nodeList.item(itemInc);
        while (!node.getTextContent().trim().equals(")")) {
            if (node.getTextContent().trim().equals(",")) {
                parameterList.appendChild(copyNodeAndInc(nodeList));
            }
            String type = getNodeTextValue(nodeList);
            parameterList.appendChild(copyNodeAndInc(nodeList)); // type// add all params before )
            String name = getNodeTextValue(nodeList);
            subTable.addNewItem(name, type, "argument");
            parameterList.appendChild(copyNodeAndInc(nodeList)); // name
            node = nodeList.item(itemInc);
        }
        subroutineDec.appendChild(copyNodeAndInc(nodeList)); // add )

        Element subroutineBody = doc.createElement("subroutineBody");
        subroutineDec.appendChild(subroutineBody);
        subroutineBody.appendChild(copyNodeAndInc(nodeList)); // add {

        int varCounter = 0;
        if (nodeList.item(itemInc).getTextContent().trim().equals("var")) {
            varCounter = compileVarDec(subroutineBody, nodeList, subTable);
        }
        vmCode.add(VMProducer.writeFunction(subType, functionName, className, Integer.toString(varCounter), Integer.toString(constructorFieldCount)));

        compileStatements(subroutineBody, nodeList, subTable);
        subroutineBody.appendChild(copyNodeAndInc(nodeList)); // add }
    }

    /*
        Compiles statements that are executed within a subroutine
     */
    private void compileStatements(Element element, NodeList nodeList, SymbolTable subTable) {
        String stateNode = nodeList.item(itemInc).getTextContent().trim();
        Element statements = doc.createElement("statements");
        element.appendChild(statements);
        while (statementsList.contains(stateNode)) {

            if (stateNode.equals("let")) {
                compileLet(statements, nodeList, subTable);
            } else if (stateNode.equals("if")) {
                compileIf(statements, nodeList, subTable);
            } else if (stateNode.equals("while")) {
                compileWhile(statements, nodeList, subTable);
            } else if (stateNode.equals("do")) {
                compileDo(statements, nodeList, subTable);
            } else {
                compileReturn(statements, nodeList, subTable);
            }
            stateNode = nodeList.item(itemInc).getTextContent().trim();
        }
    }

    /*
        Compiles return statement at end of subroutine
     */
    private void compileReturn(Element element, NodeList nodeList, SymbolTable subTable) {
        Element returnStatement = doc.createElement("returnStatement");
        element.appendChild(returnStatement);
        returnStatement.appendChild(copyNodeAndInc(nodeList)); // add return
        if (!nodeList.item(itemInc).getTextContent().trim().equals(";")) {
            compileExpression(returnStatement, nodeList, subTable);
        } else {
            vmCode.add("push constant 0");
        }
        vmCode.add("return");
        returnStatement.appendChild(copyNodeAndInc(nodeList)); // add ;
    }

    /*
        Compiles do statement
     */
    private void compileDo(Element element, NodeList nodeList, SymbolTable subTable) {
        Element doStatement = doc.createElement("doStatement");
        int expListCount = 0;
        element.appendChild(doStatement);
        doStatement.appendChild(copyNodeAndInc(nodeList)); // add do
        String maybeClassOrSub = getNodeTextValue(nodeList);
        doStatement.appendChild(copyNodeAndInc(nodeList)); // add sub or class or var
        if (nodeList.item(itemInc).getTextContent().trim().equals("(")) {
            vmCode.add("push pointer 0");
            doStatement.appendChild(copyNodeAndInc(nodeList)); // add (
            expListCount = compileExpressionList(doStatement, nodeList, subTable, expListCount);
            expListCount++; // add 1 for the this
            vmCode.add("call " + className + "." + maybeClassOrSub + " " + expListCount);
            doStatement.appendChild(copyNodeAndInc(nodeList)); // add )
        } else {
            doStatement.appendChild(copyNodeAndInc(nodeList)); // add .
            String sub = getNodeTextValue(nodeList);
            if (subTable.hasItem(maybeClassOrSub, classTable)) {
                vmCode.add("push " + subTable.getKind(maybeClassOrSub, classTable) + " " + subTable.getIndex(maybeClassOrSub, classTable));
                expListCount++;
            }
            doStatement.appendChild(copyNodeAndInc(nodeList)); // add sub name
            doStatement.appendChild(copyNodeAndInc(nodeList)); // add (
            expListCount = compileExpressionList(doStatement, nodeList, subTable, expListCount);
            if (subTable.hasItem(maybeClassOrSub, classTable)) {
                vmCode.add("call " + subTable.getType(maybeClassOrSub, classTable) + "." + sub + " " + expListCount);
            } else {
                vmCode.add("call " + maybeClassOrSub + "." + sub + " " + expListCount);
            }
            doStatement.appendChild(copyNodeAndInc(nodeList)); // add )
        }
        vmCode.add("pop temp 0");
        doStatement.appendChild(copyNodeAndInc(nodeList)); // add ;
    }

    /*
        Compiles a while statement
     */
    private void compileWhile(Element element, NodeList nodeList, SymbolTable subTable) {
        int thisWhile = whileInc;
        whileInc++;
        Element whileStatement = doc.createElement("whileStatement");
        vmCode.add("label WHILE_EXP" + thisWhile);
        element.appendChild(whileStatement);
        whileStatement.appendChild(copyNodeAndInc(nodeList)); // add while
        whileStatement.appendChild(copyNodeAndInc(nodeList)); // add (
        compileExpression(whileStatement, nodeList, subTable);
        whileStatement.appendChild(copyNodeAndInc(nodeList)); // add )
        vmCode.add("not");
        vmCode.add("if-goto WHILE_END" + thisWhile);
        whileStatement.appendChild(copyNodeAndInc(nodeList)); // add {
        compileStatements(whileStatement, nodeList, subTable);
        whileStatement.appendChild(copyNodeAndInc(nodeList)); // add }
        vmCode.add("goto WHILE_EXP" + thisWhile);
        vmCode.add("label WHILE_END" + thisWhile);
    }

    /*
        Compiles an expression list
     */
    private int compileExpressionList(Element element, NodeList nodeList, SymbolTable subTable, int expListCount) {
        Element expList = doc.createElement("expressionList");
        element.appendChild(expList);
        String parenNode = nodeList.item(itemInc).getTextContent().trim();
        while (!parenNode.equals(")")) {
            expListCount++;
            compileExpression(expList, nodeList, subTable);
            String comNode = nodeList.item(itemInc).getTextContent().trim();
            if (comNode.equals(",")) {
                expList.appendChild(copyNodeAndInc(nodeList));
            }
            parenNode = nodeList.item(itemInc).getTextContent().trim();
        }
        return expListCount;
    }

    /*
        Compiles an expression
     */
    private void compileExpression(Element statement, NodeList nodeList, SymbolTable subTable) {
        Element expression = doc.createElement("expression");
        statement.appendChild(expression);
        compileTerm(expression, nodeList, subTable);
        String op = nodeList.item(itemInc).getTextContent().trim();
        while (ops.contains(op)) {
            expression.appendChild(copyNodeAndInc(nodeList)); // add the op
            compileTerm(expression, nodeList, subTable);
            if (op.equals("+")) {
                vmCode.add("add");
            } else if (op.equals("-")) {
                vmCode.add("sub");
            } else if (op.equals("*")) {
                vmCode.add("call Math.multiply 2");
            } else if (op.equals("/")) {
                vmCode.add("call Math.divide 2");
            } else if (op.equals("&")) {
                vmCode.add("and");
            } else if (op.equals("|")) {
                vmCode.add("or");
            } else if (op.equals("<")) {
                vmCode.add("lt");
            } else if (op.equals(">")) {
                vmCode.add("gt");
            } else if (op.equals("=")) {
                vmCode.add("eq");
            }
            op = nodeList.item(itemInc).getTextContent().trim();
        }
    }

    /*
        Compiles a term
     */
    private void compileTerm(Element expression, NodeList nodeList, SymbolTable subTable) {
        Element term = doc.createElement("term");
        int expListCount = 0;
        expression.appendChild(term);
        if (unaryOps.contains(nodeList.item(itemInc).getTextContent().trim())) {
            String unary = getNodeTextValue(nodeList);
            term.appendChild(copyNodeAndInc(nodeList)); // add unary op
            compileTerm(term, nodeList, subTable);
            processUnary(unary);
        } else if (nodeList.item(itemInc).getTextContent().trim().equals("(")) {
            term.appendChild(copyNodeAndInc(nodeList)); // add (
            compileExpression(term, nodeList, subTable);
            term.appendChild(copyNodeAndInc(nodeList)); // add )
        } else {
            String strTerm = getNodeTextValue(nodeList);
            String termType = getNodeTypeValue(nodeList);
            maybeSubName = strTerm;
            term.appendChild(copyNodeAndInc(nodeList)); // add first part of term
            if (nodeList.item(itemInc).getTextContent().trim().equals("[")) {
                term.appendChild(copyNodeAndInc(nodeList)); // add [
                compileExpression(term, nodeList, subTable);
                term.appendChild(copyNodeAndInc(nodeList)); // add ]
                //operations for post-array. should have two terms on stack
                vmCode.add("push " + subTable.getKind(maybeSubName, classTable) + " " + subTable.getIndex(maybeSubName, classTable));
                vmCode.add("add");
                vmCode.add("pop pointer 1");
                vmCode.add("push that 0");
            } else if (nodeList.item(itemInc).getTextContent().trim().equals(".")) {
                term.appendChild(copyNodeAndInc(nodeList)); // add .
                String subName;
                if (subTable.hasItem(maybeSubName, classTable)) {
                    expListCount++;
                    vmCode.add("push " + subTable.getKind(maybeSubName, classTable) + " " + subTable.getIndex(maybeSubName, classTable));
                    subName = subTable.getType(maybeSubName, classTable) + "." + getNodeTextValue(nodeList);
                } else {
                    subName = maybeSubName + "." + getNodeTextValue(nodeList);
                }
                term.appendChild(copyNodeAndInc(nodeList)); // add sub name
                term.appendChild(copyNodeAndInc(nodeList)); // add (
                expListCount = compileExpressionList(term, nodeList, subTable, expListCount);
                term.appendChild(copyNodeAndInc(nodeList)); // add )
                vmCode.add("call " + subName + " " + expListCount);
            } else if (nodeList.item(itemInc).getTextContent().trim().equals("(")) {
                term.appendChild(copyNodeAndInc(nodeList)); // add sub name
                term.appendChild(copyNodeAndInc(nodeList)); // add (
                expListCount = compileExpressionList(term, nodeList, subTable, expListCount);
                term.appendChild(copyNodeAndInc(nodeList)); // add )
                vmCode.add("call " + maybeSubName + " " + expListCount);
            } else {
                // just a term or start to a subroutine call
                if (termType.equals("integerConstant")) {
                    vmCode.add("push constant " + strTerm);
                } else if (termType.equals("stringConstant")) {
                    writeVMForStringConstant(strTerm);
                } else if (termType.equals("keyword")) {
                    if (strTerm.equals("null") || strTerm.equals("false")) {
                        vmCode.add("push constant 0");
                    } else if (strTerm.equals("true")) {
                        vmCode.add("push constant 0");
                        vmCode.add("not");
                    } else if (strTerm.equals("this")) {
                        vmCode.add("push pointer 0");
                    }
                } else if (subTable.hasItem(strTerm, classTable)) {
                    vmCode.add("push " + subTable.getKind(strTerm, classTable) + " " + subTable.getIndex(strTerm, classTable));
                } else {
                    //must be first term in subroutine
                    maybeSubName = strTerm;
                }
            }
        }
    }

    private void processUnary(String unary) {
        if (unary.equals("~")) {
            vmCode.add("not");
        } else if (unary.equals("-")) {
            vmCode.add("neg");
        }
    }

    private void writeVMForStringConstant(String stringConst) {
        int numChars = stringConst.length();
        vmCode.add("push constant " + numChars);
        vmCode.add("call String.new 1");
        for (char s : stringConst.toCharArray()) {
            int unicode = (int) s;
            vmCode.add("push constant " + unicode);
            vmCode.add("call String.appendChar 2");
        }
    }

    /*
        Compiles a let statement
     */
    private void compileLet(Element statements, NodeList nodeList, SymbolTable subTable) {
        Element letStatement = doc.createElement("letStatement");
        statements.appendChild(letStatement);
        letStatement.appendChild(copyNodeAndInc(nodeList)); // add let
        String varName = getNodeTextValue(nodeList);
        letStatement.appendChild(copyNodeAndInc(nodeList)); // add var name
        boolean isArray = false;
        if (nodeList.item(itemInc).getTextContent().trim().equals("[")) {
            isArray = true;
            letStatement.appendChild(copyNodeAndInc(nodeList)); // add [
            compileExpression(letStatement, nodeList, subTable);
            letStatement.appendChild(copyNodeAndInc(nodeList)); // add ]
        }
        String kind = subTable.getKind(varName, classTable);
        String index = subTable.getIndex(varName, classTable);
        if (isArray) {
            vmCode.add("push " + kind + " " + index);
            vmCode.add("add");
        }
        letStatement.appendChild(copyNodeAndInc(nodeList)); // add =
        compileExpression(letStatement, nodeList, subTable);
        letStatement.appendChild(copyNodeAndInc(nodeList)); // add ;
        // afterwards we pop it into whatever the var is
        if (isArray) {
            vmCode.add("pop temp 0");
            vmCode.add("pop pointer 1");
            vmCode.add("push temp 0");
            vmCode.add("pop that 0");
        } else {
            vmCode.add("pop " + kind + " " + index);
        }
    }

    /*
        Compiles an if statement
     */
    private void compileIf(Element subroutineBody, NodeList nodeList, SymbolTable subTable) {
        int thisIf = ifInc;
        ifInc++;
        Element ifStatement = doc.createElement("ifStatement");
        subroutineBody.appendChild(ifStatement);
        ifStatement.appendChild(copyNodeAndInc(nodeList)); // add if
        ifStatement.appendChild(copyNodeAndInc(nodeList)); // add (
        compileExpression(ifStatement, nodeList, subTable);
        vmCode.add("if-goto IF_TRUE" + thisIf);
        vmCode.add("goto IF_FALSE" + thisIf);
        vmCode.add("label IF_TRUE" + thisIf);
        ifStatement.appendChild(copyNodeAndInc(nodeList)); // add )
        ifStatement.appendChild(copyNodeAndInc(nodeList)); // add {
        compileStatements(ifStatement, nodeList, subTable);
        ifStatement.appendChild(copyNodeAndInc(nodeList)); // add }
        if (nodeList.item(itemInc).getTextContent().trim().equals("else")) {
            vmCode.add("goto IF_END" + thisIf);
            vmCode.add("label IF_FALSE" + thisIf);
            ifStatement.appendChild(copyNodeAndInc(nodeList)); // add if
            ifStatement.appendChild(copyNodeAndInc(nodeList)); // add {
            compileStatements(ifStatement, nodeList, subTable);
            ifStatement.appendChild(copyNodeAndInc(nodeList)); // add }
            vmCode.add("label IF_END" + thisIf);
        } else {
            vmCode.add("label IF_FALSE" + thisIf);
        }
    }

    /*
        Compiles the variable declarations of a subroutine
     */
    private int compileVarDec(Element subroutineBody, NodeList nodeList, SymbolTable subTable) {
        Node nodeVar = nodeList.item(itemInc);
        int varCounter = 0;
        while (nodeVar.getTextContent().trim().equals("var")) {
            varCounter++;
            Element varDec = doc.createElement("varDec");
            subroutineBody.appendChild(varDec);
            varDec.appendChild(copyNodeAndInc(nodeList)); // add var
            String type = getNodeTextValue(nodeList);
            varDec.appendChild(copyNodeAndInc(nodeList)); // add type
            String name = getNodeTextValue(nodeList);
            subTable.addNewItem(name, type, "local");
            varDec.appendChild(copyNodeAndInc(nodeList)); // add name
            Node nodeCom = nodeList.item(itemInc);
            while (nodeCom.getTextContent().trim().equals(",")) {
                varCounter++;
                varDec.appendChild(copyNodeAndInc(nodeList)); // add comma
                subTable.addNewItem(getNodeTextValue(nodeList), type, "local");
                varDec.appendChild(copyNodeAndInc(nodeList)); // add name
                nodeCom = nodeList.item(itemInc); // either , or ;
            }
            varDec.appendChild(copyNodeAndInc(nodeList));
            nodeVar = nodeList.item(itemInc); // either another var or statement
        }
        return varCounter;
    }
}
