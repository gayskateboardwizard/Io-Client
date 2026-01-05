package io.client;

import io.client.settings.RadioSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class Fullbright extends Module {

    private static final double MAX_GAMMA = 10000000000000000000.0;
    private final Minecraft mc = Minecraft.getInstance();
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
            this.originalGamma = mc.options.gamma().get();
            mc.options.gamma().set(MAX_GAMMA);
            mc.options.save();
        }
    }

    @Override
    public void onDisable() {
        if (mc.player == null || mc.options == null) return;

        if (modeSetting.isSelected("Gamma")) {
            mc.options.gamma().set(this.originalGamma);
            mc.options.save();
        }

        if (modeSetting.isSelected("Potion")) {
            mc.player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    @Override
    public void onUpdate() {
        if (mc.player == null) return;

        if (modeSetting.isSelected("Potion")) {
            if (!mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
                mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0));
            }
        }
    }
}