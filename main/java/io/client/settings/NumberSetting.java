package io.client.settings;

public class NumberSetting extends Setting {
    private final float min;
    private final float max;
    private float value;

    public NumberSetting(String name, float defaultValue, float min, float max) {
        super(name);
        this.value = defaultValue;
        this.min = min;
        this.max = max;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = Math.max(min, Math.min(max, value));
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }
}