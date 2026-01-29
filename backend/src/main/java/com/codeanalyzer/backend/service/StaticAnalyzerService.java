package com.codeanalyzer.backend.service;

import com.codeanalyzer.backend.model.StructureReport;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.springframework.stereotype.Service;

@Service
public class StaticAnalyzerService {

    public StructureReport analyze(String sourceCode) {
        StructureReport report = new StructureReport();

        try {
            // 1. Parse the code structure
            CompilationUnit cu = StaticJavaParser.parse(sourceCode);

            // 2. Find the Class info
            cu.findFirst(ClassOrInterfaceDeclaration.class).ifPresent(c -> {
                report.setClassName(c.getNameAsString());
                
                // 3. Extract Method Names (Needed to visualize Control Flow later)
                for (MethodDeclaration method : c.getMethods()) {
                    report.getMethodNames().add(method.getNameAsString());
                }

                // 4. Extract Variables (Needed to visualize Memory later)
                for (FieldDeclaration field : c.getFields()) {
                    field.getVariables().forEach(variable -> {
                        report.getFields().add(variable.getNameAsString());
                    });
                }
            });

        } catch (Exception e) {
            System.err.println("Analysis failed: " + e.getMessage());
        }

        return report;
    }
}