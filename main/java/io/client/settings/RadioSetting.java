package io.client.settings;

import java.util.ArrayList;
import java.util.List;

public class RadioSetting extends Setting {
    private final String name;
    private final List<String> options = new ArrayList<>();
    private String selectedOption;

    public RadioSetting(String name, String defaultOption) {
        super(name);
        this.name = name;
        this.selectedOption = defaultOption;
    }

    @Override
    public String getName() {
        return name;
    }

    public void addOption(String option) {
        options.add(option);
    }

    public List<String> getOptions() {
        return options;
    }

    public String getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(String option) {
        if (options.contains(option)) {
            this.selectedOption = option;
        }
    }

    public boolean isSelected(String option) {
        return selectedOption.equals(option);
    }

    public void cycleNext() {
        int currentIndex = options.indexOf(selectedOption);
        int nextIndex = (currentIndex + 1) % options.size();
        selectedOption = options.get(nextIndex);
    }
}

