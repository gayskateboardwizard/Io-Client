package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.settings.NumberSetting;
import io.client.settings.StringSetting;
import net.minecraft.client.Minecraft;

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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        int currentTick = (int) mc.level.getGameTime();
        int currentDelay = (int) delaySetting.getValue();

        if (currentTick - lastTick >= currentDelay) {
            mc.player.connection.sendChat(message.getValue());
            lastTick = currentTick;
        }
    }

    public void setMessage(String msg) {
        message.setValue(msg);
    }
}