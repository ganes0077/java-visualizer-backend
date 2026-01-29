package com.codeanalyzer.backend.service;

import com.codeanalyzer.backend.model.ExecutionStep;
import java.util.*;
import java.lang.reflect.Field; // Import Reflection

public class Tracer {
    private static final List<ExecutionStep> trace = new ArrayList<>();
    private static int stackDepth = 0;
    private static final int MAX_STEPS = 1000;

    public static void start() {
        trace.clear();
        stackDepth = 0;
    }

    public static void enterMethod(String methodName) {
        stackDepth++;
    }

    public static void exitMethod() {
        if (stackDepth > 0) stackDepth--;
    }

    public static void snapshot(int lineNo, String methodName, String varName, Object value) {
        if (trace.size() >= MAX_STEPS) throw new RuntimeException("Trace Limit Exceeded");

        Map<String, String> variables = new HashMap<>();
        if (!trace.isEmpty()) {
            variables.putAll(trace.get(trace.size() - 1).getVariables());
        }

        if (varName != null) {
            variables.put(varName, formatValue(value));
        }

        trace.add(new ExecutionStep(lineNo, methodName, variables, stackDepth));
    }

    public static List<ExecutionStep> getTrace() {
        return new ArrayList<>(trace);
    }

    private static String formatValue(Object value) {
        if (value == null) return "null";
        
        // 1. Handle Arrays
        if (value.getClass().isArray()) {
            if (value instanceof Object[]) return Arrays.deepToString((Object[]) value);
            if (value instanceof int[]) return Arrays.toString((int[]) value);
            if (value instanceof double[]) return Arrays.toString((double[]) value);
            if (value instanceof boolean[]) return Arrays.toString((boolean[]) value);
            if (value instanceof char[]) return Arrays.toString((char[]) value);
        }

        // 2. Handle Primitives & Strings (Keep them simple)
        if (value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Character) {
            return String.valueOf(value);
        }

        // 3. Handle OBJECTS (The new Magic 📦)
        // We use Reflection to inspect fields
        try {
            StringBuilder sb = new StringBuilder("{ ");
            Field[] fields = value.getClass().getDeclaredFields();
            boolean first = true;
            
            for (Field f : fields) {
                // Skip weird internal fields (like "this$0" in inner classes)
                if (f.getName().contains("$")) continue;
                
                f.setAccessible(true); // Allow reading private fields
                Object val = f.get(value);
                
                if (!first) sb.append(", ");
                sb.append(f.getName()).append(": ").append(formatValue(val)); // Recurse for nested objects
                first = false;
            }
            sb.append(" }");
            
            // If object has no fields, just return class name
            if (first) return value.toString();
            
            return sb.toString();
            
        } catch (Exception e) {
            return value.toString(); // Fallback if reflection fails
        }
    }
}