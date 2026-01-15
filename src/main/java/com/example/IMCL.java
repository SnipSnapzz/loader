package com.example;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

public class IMCL extends ClassLoader {
    private final Map<String, byte[]> classDefinitions;
    private final Map<String, byte[]> resourceDefinitions;

    public IMCL(Map<String, byte[]> classDefinitions, Map<String, byte[]> resourceDefinitions) {
        super(ClassLoader.getSystemClassLoader());
        this.classDefinitions = classDefinitions;
        this.resourceDefinitions = resourceDefinitions;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = classDefinitions.get(name);
        if (bytes == null) {
            throw new ClassNotFoundException(name);
        }
        // Load class from byte array
        return defineClass(name, bytes, 0, bytes.length);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        byte[] data = resourceDefinitions.get(name);
        if (data != null) {
            return new ByteArrayInputStream(data);
        }
        // Handle leading slash
        if (name.startsWith("/")) {
            data = resourceDefinitions.get(name.substring(1));
            if (data != null) {
                return new ByteArrayInputStream(data);
            }
        }
        return super.getResourceAsStream(name);
    }
}