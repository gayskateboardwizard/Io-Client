package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;

public class Chams extends Module {
    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting hostiles = new BooleanSetting("Hostiles", true);
    private final BooleanSetting passives = new BooleanSetting("Passives", true);
    private final BooleanSetting ignoreSelf = new BooleanSetting("IgnoreSelf", true);

    private final NumberSetting playerR = new NumberSetting("PlayerR", 255.0f, 0.0f, 255.0f);
    private final NumberSetting playerG = new NumberSetting("PlayerG", 255.0f, 0.0f, 255.0f);
    private final NumberSetting playerB = new NumberSetting("PlayerB", 255.0f, 0.0f, 255.0f);
    private final NumberSetting playerA = new NumberSetting("PlayerA", 255.0f, 0.0f, 255.0f);

    private final NumberSetting hostileR = new NumberSetting("HostileR", 255.0f, 0.0f, 255.0f);
    private final NumberSetting hostileG = new NumberSetting("HostileG", 0.0f, 0.0f, 255.0f);
    private final NumberSetting hostileB = new NumberSetting("HostileB", 0.0f, 0.0f, 255.0f);
    private final NumberSetting hostileA = new NumberSetting("HostileA", 255.0f, 0.0f, 255.0f);

    private final NumberSetting passiveR = new NumberSetting("PassiveR", 0.0f, 0.0f, 255.0f);
    private final NumberSetting passiveG = new NumberSetting("PassiveG", 255.0f, 0.0f, 255.0f);
    private final NumberSetting passiveB = new NumberSetting("PassiveB", 0.0f, 0.0f, 255.0f);
    private final NumberSetting passiveA = new NumberSetting("PassiveA", 255.0f, 0.0f, 255.0f);

    public Chams() {
        super("Chams", "Renders colored entity outlines through walls", -1, Category.RENDER);

        addSetting(players);
        addSetting(hostiles);
        addSetting(passives);
        addSetting(ignoreSelf);

        addSetting(playerR);
        addSetting(playerG);
        addSetting(playerB);
        addSetting(playerA);

        addSetting(hostileR);
        addSetting(hostileG);
        addSetting(hostileB);
        addSetting(hostileA);

        addSetting(passiveR);
        addSetting(passiveG);
        addSetting(passiveB);
        addSetting(passiveA);
    }

    public boolean shouldRender(Entity entity) {
        if (!isEnabled()) return false;
        if (entity == null) return false;
        if (entity == net.minecraft.client.MinecraftClient.getInstance().player && ignoreSelf.isEnabled()) return false;

        if (entity instanceof PlayerEntity) return players.isEnabled();
        if (entity instanceof HostileEntity) return hostiles.isEnabled();
        if (entity instanceof AnimalEntity) return passives.isEnabled();
        return false;
    }

    public int[] getColor(Entity entity) {
        if (entity instanceof HostileEntity) {
            return rgba(hostileR, hostileG, hostileB, hostileA);
        }
        if (entity instanceof AnimalEntity) {
            return rgba(passiveR, passiveG, passiveB, passiveA);
        }
        return rgba(playerR, playerG, playerB, playerA);
    }

    private static int[] rgba(NumberSetting r, NumberSetting g, NumberSetting b, NumberSetting a) {
        return new int[] { (int) r.getValue(), (int) g.getValue(), (int) b.getValue(), (int) a.getValue() };
    }
}

