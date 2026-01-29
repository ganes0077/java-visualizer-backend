package com.codeanalyzer.backend.model;

import java.util.List;

public class SyntaxResult {
    private boolean isSuccess;
    private List<String> errors;

    // Constructor
    public SyntaxResult(boolean isSuccess, List<String> errors) {
        this.isSuccess = isSuccess;
        this.errors = errors;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}