package com.codeanalyzer.backend.model;

import java.util.Map;

// Represents ONE moment in time during execution (like a single frame in a video)
public class ExecutionStep {
    private int lineNo;           // Which line is highlighted?
    private String methodName;    // Which method are we in?
    private Map<String, String> variables; // What are the variable values right now?
    private int stackDepth;       // How high is the stack? (For recursion)

    public ExecutionStep(int lineNo, String methodName, Map<String, String> variables, int stackDepth) {
        this.lineNo = lineNo;
        this.methodName = methodName;
        this.variables = variables;
        this.stackDepth = stackDepth;
    }

    // Getters
    public int getLineNo() { return lineNo; }
    public String getMethodName() { return methodName; }
    public Map<String, String> getVariables() { return variables; }
    public int getStackDepth() { return stackDepth; }
}