package io.client.settings;

public class StringSetting extends Setting {
    private String value;

    public StringSetting(String name, String defaultValue) {
        super(name);
        this.value = defaultValue;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

