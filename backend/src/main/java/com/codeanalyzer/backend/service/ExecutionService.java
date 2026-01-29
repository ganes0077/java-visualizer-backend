package com.codeanalyzer.backend.service;

import com.codeanalyzer.backend.model.ExecutionStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@Service
public class ExecutionService {

    @Autowired
    private InstrumentationService instrumentationService;

    public List<ExecutionStep> execute(String sourceCode) {
        Tracer.start(); // Reset
        String modifiedCode = instrumentationService.instrument(sourceCode);
        
        try {
            Path tempDir = Files.createTempDirectory("java_exec");
            Path sourcePath = tempDir.resolve("Main.java");
            Files.write(sourcePath, modifiedCode.getBytes());

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            String classpath = System.getProperty("java.class.path");
            
            int result = compiler.run(null, System.out, System.err, "-cp", classpath, sourcePath.toString());

            if (result != 0) return Collections.emptyList();

            URLClassLoader classLoader = URLClassLoader.newInstance(
                new URL[]{tempDir.toUri().toURL()}, 
                this.getClass().getClassLoader()
            );
            
            Class<?> cls = Class.forName("Main", true, classLoader);
            
            try {
                java.lang.reflect.Method mainMethod = cls.getMethod("main", String[].class);
                String[] args = new String[0];
                mainMethod.invoke(null, (Object) args);
            } catch (Exception e) {
                // CHECK: Is this our safety limit?
                // The actual exception is wrapped in InvocationTargetException, so we check the cause
                Throwable cause = e.getCause();
                if (cause != null && "Trace Limit Exceeded".equals(cause.getMessage())) {
                    System.out.println("⚠️ Infinite Loop Detected! Execution stopped safely.");
                    // We DO NOT return empty list. We return the trace captured SO FAR.
                    return Tracer.getTrace();
                }
                e.printStackTrace();
            }

            return Tracer.getTrace();

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}