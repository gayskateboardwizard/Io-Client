package io.client.modules;

import io.client.Category;
import io.client.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.RadioSetting;


public class IoSwag extends Module {
    public final BooleanSetting greentext = new BooleanSetting("AutoGreentext", true);
    private final RadioSetting suffix = new RadioSetting("Suffix", "<IO>");


    public IoSwag() {
        super("IoSwag", "Show Them", -1, Category.MISC);
        suffix.addOption("<IO>");
        suffix.addOption("IO on crack!");
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
        addSetting(greentext);


    }

    public String getSuffix() {
        return suffix.getSelectedOption();
    }

}
