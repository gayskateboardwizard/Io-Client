package io.client.modules.misc;

import com.mojang.authlib.GameProfile;
import io.client.modules.templates.Category;
import io.client.modules.templates.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import io.client.settings.StringSetting;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;

public class FakePlayerModule extends Module {
    private final StringSetting name = new StringSetting("Name", "IO-Fake");
    private final NumberSetting health = new NumberSetting("Health", 20.0f, 1.0f, 100.0f);
    private final BooleanSetting copyInv = new BooleanSetting("CopyInv", true);

    private OtherClientPlayerEntity fakePlayer;
    private int fakeId;

    public FakePlayerModule() {
        super("FakePlayer", "Spawns a client-side fake player for testing.", -1, Category.MISC);
        addSetting(name);
        addSetting(health);
        addSetting(copyInv);
    }

    @Override
    public void onEnable() {
        spawnFakePlayer();
    }

    @Override
    public void onDisable() {
        despawnFakePlayer();
    }

    private void spawnFakePlayer() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        despawnFakePlayer();

        String playerName = name.getValue().isBlank() ? "IO-Fake" : name.getValue();
        fakePlayer = new OtherClientPlayerEntity(mc.world, new GameProfile(UUID.randomUUID(), playerName));
        fakePlayer.copyPositionAndRotation(mc.player);
        fakePlayer.setPose(mc.player.getPose());
        fakePlayer.setYaw(mc.player.getYaw());
        fakePlayer.setPitch(mc.player.getPitch());

        float configuredHealth = health.getValue();
        if (configuredHealth <= 20.0f) {
            fakePlayer.setHealth(configuredHealth);
            fakePlayer.setAbsorptionAmount(0.0f);
        } else {
            fakePlayer.setHealth(20.0f);
            fakePlayer.setAbsorptionAmount(configuredHealth - 20.0f);
        }

        if (copyInv.isEnabled()) {
            fakePlayer.getInventory().clone(mc.player.getInventory());
        }

        fakeId = -1000 - (int) (Math.random() * 100000);
        fakePlayer.setId(fakeId);
        mc.world.addEntity(fakePlayer);
    }

    private void despawnFakePlayer() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || fakePlayer == null) return;

        mc.world.removeEntity(fakeId, Entity.RemovalReason.DISCARDED);
        fakePlayer = null;
        fakeId = 0;
    }
}
