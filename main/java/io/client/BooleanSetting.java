package io.client.settings;

public class BooleanSetting extends Setting {
    private boolean enabled;
    private Runnable OnChange;

    public BooleanSetting(String name, boolean defaultValue) {
        super(name);
        this.enabled = defaultValue;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void toggle() {
        this.enabled = !this.enabled;
        if (OnChange != null) {
            OnChange.run();
        }
    }

    public void OnToggle(Runnable OnChange) {
        this.OnChange = OnChange;
    }
}