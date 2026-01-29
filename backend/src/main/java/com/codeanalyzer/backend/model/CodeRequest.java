package com.codeanalyzer.backend.model;

public class CodeRequest {
    private String sourceCode;

    // Default Constructor (Needed for JSON parsing)
    public CodeRequest() {}

    public CodeRequest(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }
}