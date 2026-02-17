package io.client.discord;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;

public final class DiscordRpcManager {
    private static final String APPLICATION_ID = System.getProperty("ioClientDiscordAppId", "1473119387337883763");
    private static volatile boolean running;
    private static Thread callbackThread;

    private DiscordRpcManager() {
    }

    public static void init() {
        if (running)
            return;
        running = true;

        DiscordRPC rpc = DiscordRPC.INSTANCE;
        DiscordEventHandlers handlers = new DiscordEventHandlers();
        rpc.Discord_Initialize(APPLICATION_ID, handlers, true, null);

        DiscordRichPresence presence = new DiscordRichPresence();
        presence.startTimestamp = System.currentTimeMillis() / 1000L;
        presence.details = "Playing IO Client";
        presence.state = "Indev";
        presence.largeImageKey = "io";
        presence.largeImageText = "IO Client";
        rpc.Discord_UpdatePresence(presence);

        callbackThread = new Thread(() -> {
            while (running) {
                try {
                    rpc.Discord_RunCallbacks();
                    Thread.sleep(2000L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Throwable ignored) {
                }
            }
        }, "io-client-discord-rpc");
        callbackThread.setDaemon(true);
        callbackThread.start();
    }

    public static void shutdown() {
        if (!running)
            return;
        running = false;
        if (callbackThread != null) {
            callbackThread.interrupt();
            callbackThread = null;
        }
        try {
            DiscordRPC.INSTANCE.Discord_Shutdown();
        } catch (Throwable ignored) {
        }
    }
}
