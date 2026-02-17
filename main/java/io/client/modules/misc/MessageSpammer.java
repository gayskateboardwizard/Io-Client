package io.client.modules.misc;

import io.client.Category;
import io.client.Module;
import io.client.settings.NumberSetting;
import io.client.settings.StringSetting;
import net.minecraft.client.MinecraftClient;

public class MessageSpammer extends Module {
    private final NumberSetting delaySetting = new NumberSetting("Tick delay", 200, 1, 1200);
    private final StringSetting message = new StringSetting("Message", "I am a spam message");
    private int lastTick = 0;

    public MessageSpammer() {
        super("ChatSpammer", "Sends a friendly message ever so often", -1, Category.MISC);
        addSetting(delaySetting);
        addSetting(message);
    }

    @Override
    public void onUpdate() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        int currentTick = (int) mc.world.getTime();
        int currentDelay = (int) delaySetting.getValue();

        if (currentTick - lastTick >= currentDelay) {
            mc.player.networkHandler.sendChatMessage(message.getValue());
            lastTick = currentTick;
        }
    }

    public void setMessage(String msg) {
        message.setValue(msg);
    }
}
