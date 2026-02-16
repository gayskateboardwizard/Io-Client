package io.client;

import io.client.settings.RadioSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class Fullbright extends Module {

    private static final double MAX_GAMMA = 10000000000000000000.0;
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private double originalGamma;
    private RadioSetting modeSetting;

    public Fullbright() {
        super("FullBright", "Brights up the entire world", -1, Category.RENDER);


        modeSetting = new RadioSetting("Mode", "Gamma");
        modeSetting.addOption("Gamma");
        modeSetting.addOption("Potion");
        addSetting(modeSetting);
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.options == null) return;

        if (modeSetting.isSelected("Gamma")) {
            this.originalGamma = mc.options.getGamma().getValue();
            mc.options.getGamma().setValue(MAX_GAMMA);
            mc.options.write();
        }
    }

    @Override
    public void onDisable() {
        if (mc.player == null || mc.options == null) return;

        if (modeSetting.isSelected("Gamma")) {
            mc.options.getGamma().setValue(this.originalGamma);
            mc.options.write();
        }

        if (modeSetting.isSelected("Potion")) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) return;

        if (modeSetting.isSelected("Potion")) {
            if (!mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, -1, 0));
            }
        }
    }
}