package io.client;

import io.client.settings.Setting;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    private final String name;
    private final String description;
    private final Category category;
    private int key;
    private boolean enabled;
    private boolean extended = false;

    private List<Setting> settings = new ArrayList<>();

    public Module(String name, String description, int key, Category category) {
        this.name = name;
        this.description = description;
        this.key = key;
        this.category = category;
        this.enabled = false;
    }

    public void toggle() {
        this.enabled = !this.enabled;
        if (this.enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }


    public void onUpdate() {
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    protected void addSetting(Setting setting) {
        this.settings.add(setting);
    }

    public List<Setting> getSettings() {
        return settings;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isExtended() {
        return extended;
    }

    public void setExtended(boolean extended) {
        this.extended = extended;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}