package com.meyermt.jack;

import java.util.*;

/**
 * Created by michaelmeyer on 3/7/17.
 */
public class SymbolTable {

    public static final String NAME = "name", TYPE = "type", KIND = "kind", INDEX = "index";
    private List<Map<String, String>> values = new ArrayList<>();
    private int argCounter;
    private int varCounter;
    private int staticCounter;
    private int fieldCounter;

    public SymbolTable() {
    }

    public void addNewItem(String name, String type, String kind) {
        Map<String, String> symbolEntry = new HashMap<>();
        symbolEntry.put(NAME, name);
        symbolEntry.put(TYPE, type);
        if (kind.equals("field")) {
            symbolEntry.put(KIND, "this");
        } else {
            symbolEntry.put(KIND, kind);
        }
        symbolEntry.put(INDEX, getTypeCountAndIncrement(kind));
        values.add(symbolEntry);
    }

    public void printAll() {
        values.stream().forEach(map -> {
            System.out.println("name: " + map.get(NAME) + " type: " + map.get(TYPE)
            + " kind: " + map.get(KIND) + " index: " + map.get(INDEX));
        });
    }

    public boolean hasItem(String item, SymbolTable higherTable) {
        Optional<Boolean> inSub = values.stream()
            .filter(entry -> entry.get("name").equals(item))
            .map(it -> true)
            .findFirst();

        if (higherTable != null && !inSub.isPresent()) {
            return higherTable.hasItem(item, null);
        } else {
            return inSub.orElse(false);
        }
    }

    public String getKind(String varName, SymbolTable higherTable) {
        Optional<String> kind = values.stream()
                .filter(entry -> entry.get("name").equals(varName))
                .map(entry -> entry.get("kind"))
                .findFirst();

        if (higherTable != null && !kind.isPresent()) {
            return higherTable.getKind(varName, null);
        } else {
            return kind.orElseThrow(() -> new RuntimeException("Unable to retrieve kind for: " + varName));
        }
    }

    public String getIndex(String varName, SymbolTable higherTable) {
         Optional<String> index = values.stream()
                .filter(entry -> entry.get("name").equals(varName))
                .map(entry -> entry.get("index"))
                .findFirst();

        if (higherTable != null && !index.isPresent()) {
            return higherTable.getIndex(varName, null);
        } else {
            return index.orElseThrow(() -> new RuntimeException("Unable to retrieve index for: " + varName));
        }
    }

    public String getType(String varName, SymbolTable higherTable) {
        Optional<String> index = values.stream()
                .filter(entry -> entry.get("name").equals(varName))
                .map(entry -> entry.get("type"))
                .findFirst();

        if (higherTable != null && !index.isPresent()) {
            return higherTable.getType(varName, null);
        } else {
            return index.orElseThrow(() -> new RuntimeException("Unable to retrieve type for: " + varName));
        }
    }

    private String getTypeCountAndIncrement(String kind) {
        if (kind.equals("argument")) {
            int current =  argCounter;
            argCounter++;
            return Integer.toString(current);
        } else if (kind.equals("static")) {
            int current =  staticCounter;
            staticCounter++;
            return Integer.toString(current);
        } else if (kind.equals("field")) {
            int current = fieldCounter;
            fieldCounter++;
            return Integer.toString(current);
        } else {
            int current = varCounter;
            varCounter++;
            return Integer.toString(current);
        }
    }
}
