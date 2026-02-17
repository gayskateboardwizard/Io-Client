package io.client.modules.render;

import io.client.Category;
import io.client.Module;
import io.client.TargetManager;
import io.client.settings.BooleanSetting;
import io.client.settings.CategorySetting;
import io.client.settings.NumberSetting;
import io.client.settings.RadioSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;

public class Chams extends Module {
    private final RadioSetting targetMode = new RadioSetting("TargetMode", "Targets");

    private final CategorySetting playerSettings = new CategorySetting("PlayerChams");
    private final CategorySetting hostileSettings = new CategorySetting("HostileChams");
    private final CategorySetting passiveSettings = new CategorySetting("PassiveChams");
    private final CategorySetting friendSettings = new CategorySetting("FriendChams");

    private final BooleanSetting players = new BooleanSetting("Players", true);
    private final BooleanSetting hostiles = new BooleanSetting("Hostiles", true);
    private final BooleanSetting passives = new BooleanSetting("Passives", true);
    private final BooleanSetting ignoreSelf = new BooleanSetting("IgnoreSelf", true);
    private final BooleanSetting friendColor = new BooleanSetting("FriendColor", true);

    private final NumberSetting playerR = new NumberSetting("PlayerR", 255.0f, 0.0f, 255.0f);
    private final NumberSetting playerG = new NumberSetting("PlayerG", 255.0f, 0.0f, 255.0f);
    private final NumberSetting playerB = new NumberSetting("PlayerB", 255.0f, 0.0f, 255.0f);
    private final NumberSetting playerA = new NumberSetting("PlayerA", 255.0f, 0.0f, 255.0f);
    private final NumberSetting friendR = new NumberSetting("FriendR", 0.0f, 0.0f, 255.0f);
    private final NumberSetting friendG = new NumberSetting("FriendG", 170.0f, 0.0f, 255.0f);
    private final NumberSetting friendB = new NumberSetting("FriendB", 255.0f, 0.0f, 255.0f);
    private final NumberSetting friendA = new NumberSetting("FriendA", 255.0f, 0.0f, 255.0f);

    private final NumberSetting hostileR = new NumberSetting("HostileR", 255.0f, 0.0f, 255.0f);
    private final NumberSetting hostileG = new NumberSetting("HostileG", 0.0f, 0.0f, 255.0f);
    private final NumberSetting hostileB = new NumberSetting("HostileB", 0.0f, 0.0f, 255.0f);
    private final NumberSetting hostileA = new NumberSetting("HostileA", 255.0f, 0.0f, 255.0f);

    private final NumberSetting passiveR = new NumberSetting("PassiveR", 0.0f, 0.0f, 255.0f);
    private final NumberSetting passiveG = new NumberSetting("PassiveG", 255.0f, 0.0f, 255.0f);
    private final NumberSetting passiveB = new NumberSetting("PassiveB", 0.0f, 0.0f, 255.0f);
    private final NumberSetting passiveA = new NumberSetting("PassiveA", 255.0f, 0.0f, 255.0f);

    public Chams() {
        super("Chams", "Renders colored entities through walls", -1, Category.RENDER);

        targetMode.addOption("Everything");
        targetMode.addOption("Targets");
        targetMode.addOption("Players");
        addSetting(targetMode);

        playerSettings.addSetting(players);
        playerSettings.addSetting(ignoreSelf);
        playerSettings.addSetting(playerR);
        playerSettings.addSetting(playerG);
        playerSettings.addSetting(playerB);
        playerSettings.addSetting(playerA);

        friendSettings.addSetting(friendColor);
        friendSettings.addSetting(friendR);
        friendSettings.addSetting(friendG);
        friendSettings.addSetting(friendB);
        friendSettings.addSetting(friendA);

        hostileSettings.addSetting(hostiles);
        hostileSettings.addSetting(hostileR);
        hostileSettings.addSetting(hostileG);
        hostileSettings.addSetting(hostileB);
        hostileSettings.addSetting(hostileA);

        passiveSettings.addSetting(passives);
        passiveSettings.addSetting(passiveR);
        passiveSettings.addSetting(passiveG);
        passiveSettings.addSetting(passiveB);
        passiveSettings.addSetting(passiveA);

        addSetting(playerSettings);
        addSetting(friendSettings);
        addSetting(hostileSettings);
        addSetting(passiveSettings);
    }

    public boolean shouldRender(Entity entity) {
        if (!isEnabled()) return false;
        if (entity == null) return false;

        if (entity == net.minecraft.client.MinecraftClient.getInstance().player && ignoreSelf.isEnabled())
            return false;

        String mode = targetMode.getSelectedOption();

        switch (mode) {
            case "Everything" -> {
                if (entity instanceof PlayerEntity) return players.isEnabled();
                if (entity instanceof HostileEntity) return hostiles.isEnabled();
                if (entity instanceof AnimalEntity) return passives.isEnabled();
                return false;
            }
            case "Targets" -> {
                if (!TargetManager.INSTANCE.isValidTarget(entity)) return false;
                if (entity instanceof PlayerEntity) return players.isEnabled();
                if (entity instanceof HostileEntity) return hostiles.isEnabled();
                if (entity instanceof AnimalEntity) return passives.isEnabled();
                return true;
            }
            case "Players" -> {
                return entity instanceof PlayerEntity && players.isEnabled();
            }
            default -> {
                return false;
            }
        }
    }

    public int[] getColor(Entity entity) {
        if (entity instanceof PlayerEntity player) {
            if (friendColor.isEnabled() && TargetManager.INSTANCE.isFriend(player.getName().getString())) {
                return rgba(friendR, friendG, friendB, friendA);
            }
            return rgba(playerR, playerG, playerB, playerA);
        }
        if (entity instanceof HostileEntity) {
            return rgba(hostileR, hostileG, hostileB, hostileA);
        }
        if (entity instanceof AnimalEntity) {
            return rgba(passiveR, passiveG, passiveB, passiveA);
        }
        return rgba(255, 255, 255, 255);
    }

    private static int[] rgba(NumberSetting r, NumberSetting g, NumberSetting b, NumberSetting a) {
        return new int[]{(int) r.getValue(), (int) g.getValue(), (int) b.getValue(), (int) a.getValue()};
    }

    private static int[] rgba(int r, int g, int b, int a) {
        return new int[]{r, g, b, a};
    }
}