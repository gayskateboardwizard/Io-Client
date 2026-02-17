package io.client.modules.settings;

import io.client.Category;
import io.client.Module;
import io.client.TargetManager;
import io.client.settings.BooleanSetting;

public class Targets extends Module {

    public Targets() {
        super("Targets", "Configure which entities to target", -1, Category.SETTINGS);

        for (TargetManager.TargetType type : TargetManager.TargetType.values()) {
            BooleanSetting setting = new BooleanSetting(
                    type.getName(),
                    TargetManager.INSTANCE.isTargetEnabled(type)
            );

            setting.OnToggle(() -> {
                if (TargetManager.INSTANCE.isTargetEnabled(type) != setting.isEnabled()) {
                    TargetManager.INSTANCE.toggleTarget(type);
                }
            });

            addSetting(setting);
        }

    }
    @Override
    public boolean isEnabled() {
        return true;
    }


}
