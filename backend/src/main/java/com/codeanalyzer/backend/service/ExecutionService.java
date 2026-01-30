package com.codeanalyzer.backend.service;

import com.codeanalyzer.backend.model.ExecutionStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExecutionService {

    @Autowired
    private InstrumentationService instrumentationService;

    public List<ExecutionStep> execute(String sourceCode) {
        String modifiedCode = instrumentationService.instrument(sourceCode);

        try {
            // 1. Create a Temp Directory
            Path tempDir = Files.createTempDirectory("java_exec");
            
            // 2. Write the User's Main.java
            Path sourcePath = tempDir.resolve("Main.java");
            Files.write(sourcePath, modifiedCode.getBytes());

            // 3. GENERATE MOCK FILES (Tracer & ExecutionStep)
            // These allow the user's code to compile in the temp folder
            createMockFiles(tempDir);

            // 4. Compile
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            int result = compiler.run(null, System.out, System.err, 
                "-cp", tempDir.toString(), 
                sourcePath.toString());

            if (result != 0) return Collections.emptyList();

            // 5. Load and Run
            URLClassLoader classLoader = URLClassLoader.newInstance(
                new URL[]{tempDir.toUri().toURL()}, 
                this.getClass().getClassLoader()
            );

            Class<?> mainClass = Class.forName("Main", true, classLoader);
            Method mainMethod = mainClass.getMethod("main", String[].class);
            
            try {
                mainMethod.invoke(null, (Object) new String[0]);
            } catch (Exception e) {
                // Ignore runtime exceptions from user code
            }

            // 6. RETRIEVE DATA VIA REFLECTION
            Class<?> tracerClass = classLoader.loadClass("com.codeanalyzer.backend.service.Tracer");
            Method getTraceMethod = tracerClass.getMethod("getTrace");
            
            List<?> rawSteps = (List<?>) getTraceMethod.invoke(null);

            // 7. Convert Mock Objects to Real ExecutionStep Objects
            return convertToRealSteps(rawSteps);

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    // --- HELPER METHODS ---

    private void createMockFiles(Path tempDir) throws Exception {
        Path serviceDir = tempDir.resolve("com/codeanalyzer/backend/service");
        Files.createDirectories(serviceDir);

        Path modelDir = tempDir.resolve("com/codeanalyzer/backend/model");
        Files.createDirectories(modelDir);

        // Mock ExecutionStep for the TEMP compilation only
        String executionStepSource = 
            "package com.codeanalyzer.backend.model;\n" +
            "public class ExecutionStep {\n" +
            "    private int line;\n" +
            "    private String method;\n" +
            "    private String variable;\n" +
            "    private String value;\n" +
            "    public ExecutionStep(int line, String method, String variable, String value) {\n" +
            "        this.line = line; this.method = method; this.variable = variable; this.value = value;\n" +
            "    }\n" +
            "    public int getLine() { return line; }\n" +
            "    public String getMethod() { return method; }\n" +
            "    public String getVariable() { return variable; }\n" +
            "    public String getValue() { return value; }\n" +
            "}";
        Files.write(modelDir.resolve("ExecutionStep.java"), executionStepSource.getBytes());

        // Mock Tracer
        String tracerSource = 
            "package com.codeanalyzer.backend.service;\n" +
            "import com.codeanalyzer.backend.model.ExecutionStep;\n" +
            "import java.util.ArrayList;\n" +
            "import java.util.List;\n" +
            "public class Tracer {\n" +
            "    private static List<ExecutionStep> trace = new ArrayList<>();\n" +
            "    public static void start() { trace.clear(); }\n" +
            "    public static void snapshot(int line, String method, String variable, Object value) {\n" +
            "        if (trace.size() > 1000) return;\n" +
            "        trace.add(new ExecutionStep(line, method, variable, String.valueOf(value)));\n" +
            "    }\n" +
            "    public static void enterMethod(String method) {}\n" +
            "    public static void exitMethod() {}\n" +
            "    public static List<ExecutionStep> getTrace() { return trace; }\n" +
            "}";
        Files.write(serviceDir.resolve("Tracer.java"), tracerSource.getBytes());
    }

    private List<ExecutionStep> convertToRealSteps(List<?> rawSteps) {
        List<ExecutionStep> realSteps = new ArrayList<>();
        try {
            for (Object obj : rawSteps) {
                // 1. Get raw data from the temporary object using reflection
                Method getLine = obj.getClass().getMethod("getLine");
                Method getMethod = obj.getClass().getMethod("getMethod");
                Method getVariable = obj.getClass().getMethod("getVariable");
                Method getValue = obj.getClass().getMethod("getValue");

                int line = (int) getLine.invoke(obj);
                String method = (String) getMethod.invoke(obj);
                String variable = (String) getVariable.invoke(obj);
                String value = (String) getValue.invoke(obj);

                // 2. Adapt to YOUR Real Model (Map instead of single fields)
                Map<String, String> variables = new HashMap<>();
                if (variable != null) {
                    variables.put(variable, value);
                }

                // 3. Use YOUR Constructor: ExecutionStep(int line, String method, Map vars, int depth)
                // I am passing '0' for depth since we don't track it in this simple version yet.
                ExecutionStep step = new ExecutionStep(line, method, variables, 0);
                
                realSteps.add(step);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return realSteps;
    }
}