package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.ModuleManager;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;

public class Safety extends Module {
    public final NumberSetting healthThreshold = new NumberSetting("Min HP", 8.0F, 1.0F, 20.0F);
    public final BooleanSetting autoReEnable = new BooleanSetting("Re-Enable", true);

    private boolean wasCAEnabled = false;

    public Safety() {
        super("Safety", "Disables CA when low", -1, Category.COMBAT);
        addSetting(healthThreshold);
        addSetting(autoReEnable);
    }

    @Override
    public void onUpdate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float currentHealth = mc.player.getHealth();
        boolean hasTotem = mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING);

        CrystalAura ca = ModuleManager.INSTANCE.getModule(CrystalAura.class);
        if (ca == null) return;

        if (currentHealth <= healthThreshold.getValue() && !hasTotem) {
            if (ca.isEnabled()) {
                wasCAEnabled = true;
                ca.toggle();
            }
        } else {
            if (autoReEnable.isEnabled()) {

                if (wasCAEnabled && !ca.isEnabled()) {
                    ca.toggle();
                    wasCAEnabled = false;
                }
            }
        }
    }

    @Override
    public void onDisable() {

        if (wasCAEnabled) {
            CrystalAura ca = ModuleManager.INSTANCE.getModule(CrystalAura.class);
            if (ca != null && !ca.isEnabled()) {
                ca.toggle();
            }
            wasCAEnabled = false;
        }
        super.onDisable();
    }
}