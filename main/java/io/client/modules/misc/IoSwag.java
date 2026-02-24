package io.client.modules.misc;

import io.client.modules.templates.Category;
import io.client.modules.templates.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.RadioSetting;
import io.client.settings.StringSetting;
import java.util.List;

public class IoSwag extends Module {
    public final BooleanSetting greentext = new BooleanSetting("AutoGreentext", true);
    private final RadioSetting suffix = new RadioSetting("Suffix", "<IO>");
    private final BooleanSetting useCustomSuffix = new BooleanSetting("UseCustomSuffix", false);
    private final StringSetting customSuffix = new StringSetting("CustomSuffix", "");

    public IoSwag() {
        super("IoSwag", "Show Them", -1, Category.MISC);
        suffix.addOption("<IO>");
        suffix.addOption("<IO on crack!>");
        suffix.addOption("<[Remote Acces Trojan] On Crack!>");
        suffix.addOption("<WK>");
        suffix.addOption("-𝖂𝕶");
        suffix.addOption("木卫一客户端");
        suffix.addOption("木卫一");
        suffix.addOption("イオクライアント");
        suffix.addOption("イオ");
        suffix.addOption(":3");
        suffix.addOption(":D");
        suffix.addOption("UwU");
        suffix.addOption("˚⟡˖ ࣪");
        suffix.addOption(">>>");
        addSetting(suffix);
        addSetting(useCustomSuffix);
        addSetting(customSuffix);
        addSetting(greentext);

    }

    public String getSuffix() {
        if (useCustomSuffix.isEnabled() && !customSuffix.getValue().isBlank()) {
            return customSuffix.getValue();
        }
        return suffix.getSelectedOption();
    }

    public void setCustomSuffix(String value) {
        customSuffix.setValue(value);
        useCustomSuffix.setEnabled(true);
    }

    public void clearCustomSuffix() {
        customSuffix.setValue("");
        useCustomSuffix.setEnabled(false);
    }

    public boolean setPresetSuffix(String option) {
        for (String suffixOption : suffix.getOptions()) {
            if (suffixOption.equalsIgnoreCase(option)) {
                suffix.setSelectedOption(suffixOption);
                useCustomSuffix.setEnabled(false);
                return true;
            }
        }
        return false;
    }

    public List<String> getPresetSuffixes() {
        return suffix.getOptions();
    }

    public boolean isGreentextEnabled() {
        return greentext.isEnabled();
    }

    public void setGreentextEnabled(boolean enabled) {
        greentext.setEnabled(enabled);
    }

}



