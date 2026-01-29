package com.codeanalyzer.backend.service;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;

// This class tricks the compiler into reading code from a String instead of a file
public class JavaSourceFromString extends SimpleJavaFileObject {
    final String code;

    public JavaSourceFromString(String name, String code) {
        // We give it a fake URI like string:///MyClass.java
        super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
        this.code = code;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return code;
    }
}