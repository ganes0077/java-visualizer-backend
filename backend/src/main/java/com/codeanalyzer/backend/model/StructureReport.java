package com.codeanalyzer.backend.model;

import java.util.ArrayList;
import java.util.List;

// This class holds the "Map" of the user's code.
// It tells the frontend what Methods and Variables exist so we can draw the boxes.
public class StructureReport {
    private String className;
    private List<String> methodNames; // To draw the Method Areas
    private List<String> fields;      // To draw the Global Variable table

    public StructureReport() {
        this.methodNames = new ArrayList<>();
        this.fields = new ArrayList<>();
    }

    // Getters and Setters
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public List<String> getMethodNames() { return methodNames; }
    public void setMethodNames(List<String> methodNames) { this.methodNames = methodNames; }
    
    public List<String> getFields() { return fields; }
    public void setFields(List<String> fields) { this.fields = fields; }
}