package io.client.settings;

import java.util.ArrayList;
import java.util.List;

public class CategorySetting extends Setting {
    private final String name;
    private final List<Object> settings = new ArrayList<>();
    private boolean expanded = false;

    public CategorySetting(String name) {
        super(name);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public void toggleExpanded() {
        this.expanded = !this.expanded;
    }

    public List<Object> getSettings() {
        return settings;
    }

    public void addSetting(Object setting) {
        settings.add(setting);
    }

    public void removeSetting(Object setting) {
        settings.remove(setting);
    }

    public void clearSettings() {
        settings.clear();
    }
}