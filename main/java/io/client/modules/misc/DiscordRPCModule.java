package io.client.modules.misc;

import io.client.discord.DiscordRpcManager;
import io.client.modules.templates.Category;
import io.client.modules.templates.Module;

public class DiscordRPCModule extends Module {
    public DiscordRPCModule() {
        super("DiscordRPC", "Toggle Discord Rich Presence", -1, Category.MISC);
    }

    @Override
    public void onEnable() {
        DiscordRpcManager.init();
    }

    @Override
    public void onDisable() {
        DiscordRpcManager.shutdown();
    }
}
