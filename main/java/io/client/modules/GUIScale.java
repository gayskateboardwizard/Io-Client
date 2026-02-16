package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.clickgui.PanelRenderer;
import io.client.settings.NumberSetting;

public class GUIScale extends Module {
    private final NumberSetting scale;

    public GUIScale() {
        super("GUIScale", "Adjust clickgui size", -1, Category.SETTINGS);

        scale = new NumberSetting("Scale", 1.0f, 0.5f, 2.0f);
        addSetting(scale);

        scale.setValue(1.0f);
        PanelRenderer.setScale(1.0f);
    }

    public float getScale() {
        return scale.getValue();
    }

    @Override
    public void onUpdate() {
        PanelRenderer.setScale(scale.getValue());
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void toggle() {
    }
}