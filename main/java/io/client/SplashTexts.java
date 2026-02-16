package io.client;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SplashTexts {
    private static final List<String> SPLASHES = Arrays.asList(
            "§d我们正在看着你",
            "§4Io Client",
            "§aDefinitely a meteor skid",
            "§eDo you have hobbies?",
            "§9BLUE!",
            "§e1FvoooTQBythtGXo5MJuhF6wvqbFgYeHkN",
            "§eHave you ever tried heroin?",
            "§l§6<Racial Slur>",
            "§l§o§e188.114.96.0",
            "§e<Funny Message>",
            "§eИо-клиент",
            ""
    );

    private static final String SELECTED_SPLASH = SPLASHES.get(new Random().nextInt(SPLASHES.size()));

    public static String getRandomSplash() {
        return SELECTED_SPLASH;
    }
}