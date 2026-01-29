package com.codeanalyzer.backend.service;

import com.codeanalyzer.backend.model.SyntaxResult;
import org.springframework.stereotype.Service;

import javax.tools.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CompilerService {

    public SyntaxResult validateSyntax(String sourceCode) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("Java Compiler not found! Check your JDK.");
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        // SMART FIX: Try to find the class name to name the file correctly
        String className = "Main";
        Pattern pattern = Pattern.compile("class\\s+(\\w+)");
        Matcher matcher = pattern.matcher(sourceCode);
        if (matcher.find()) {
            className = matcher.group(1);
        }

        // Create the virtual file with the correct name
        JavaFileObject file = new JavaSourceFromString(className, sourceCode);
        Iterable<? extends JavaFileObject> compilationUnits = Collections.singletonList(file);

        JavaCompiler.CompilationTask task = compiler.getTask(
                null, null, diagnostics, null, null, compilationUnits
        );

        boolean success = task.call();

        List<String> errorMessages = new ArrayList<>();
        
        // FILTER: Only show actual Errors, ignore Warnings/Notes
        for (Diagnostic<? extends JavaFileObject> error : diagnostics.getDiagnostics()) {
            if (error.getKind() == Diagnostic.Kind.ERROR) {
                String errorMsg = "Line " + error.getLineNumber() + ": " + error.getMessage(Locale.ENGLISH);
                errorMessages.add(errorMsg);
            }
        }

        // If we found errors, success is false.
        if (!errorMessages.isEmpty()) {
            return new SyntaxResult(false, errorMessages);
        }

        return new SyntaxResult(true, null);
    }
}