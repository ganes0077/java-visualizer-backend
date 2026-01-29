package com.codeanalyzer.backend.controller;

import com.codeanalyzer.backend.model.StructureReport;
import com.codeanalyzer.backend.model.CodeRequest;
import com.codeanalyzer.backend.model.SyntaxResult;
import com.codeanalyzer.backend.service.CompilerService;
import com.codeanalyzer.backend.service.ExecutionService;
import com.codeanalyzer.backend.service.StaticAnalyzerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CodeAnalysisController {

    @Autowired private CompilerService compilerService;
    @Autowired private StaticAnalyzerService analyzerService;
    @Autowired private ExecutionService executionService; // New!

    @PostMapping("/analyze")
    public Map<String, Object> analyzeCode(@RequestBody CodeRequest request) {
        String code = request.getSourceCode();
        
        // 1. Check Syntax
        SyntaxResult syntaxResult = compilerService.validateSyntax(code);
        Map<String, Object> response = new HashMap<>();
        response.put("syntax", syntaxResult);

        if (syntaxResult.isSuccess()) {
            // 2. Static Analysis
            StructureReport structure = analyzerService.analyze(code);
            response.put("structure", structure);
            
            // 3. Dynamic Execution (The Trace)
            // Only run if it's a class named Main for now to be safe
            if (code.contains("class Main")) {
                 response.put("trace", executionService.execute(code));
            }
        }

        return response;
    }
}