package com.meyermt.jack;

/**
 * Created by michaelmeyer on 3/8/17.
 */
public class VMProducer {

    private static final String POP_PTR_0 = "pop pointer 0";

    public VMProducer() {
    }

    public static String writeFunction(String subType, String functionName, String className, String varCount, String constructorCount) {
        if (subType.equals("constructor")) {
            return "function " + className + "." + functionName + " 0" + System.lineSeparator() +
                   "push constant " + constructorCount + System.lineSeparator() +
                   "call Memory.alloc 1" + System.lineSeparator() +
                   POP_PTR_0;
        } else if (subType.equals("method")) {
            return "function " + className + "." + functionName + " " + varCount + System.lineSeparator() +
                   "push argument 0" + System.lineSeparator() +
                   POP_PTR_0;
        } else {
            return "function " + className + "." + functionName + " " + varCount;
        }
    }

}
