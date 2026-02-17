package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.CategorySetting;

public class CapeSettings extends Module {
    public final BooleanSetting enabled = new BooleanSetting("Enabled", true);
    public final CategorySetting privacy = new CategorySetting("Privacy");
    public final BooleanSetting anonymousPing = new BooleanSetting("AnonymousPing", false);

    public CapeSettings() {
        super("Capes", "IO cape behavior settings", -1, Category.SETTINGS);
        privacy.addSetting(anonymousPing);
        addSetting(enabled);
        addSetting(privacy);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void toggle() {
    }
}

