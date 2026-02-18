package io.client.clickgui;

public enum Theme {
    IO("Io", 0xFF242424, 0xFFc71e00, 0xFFAAAAAA, 0xFF1A1A1A, 0x33FFFFFF, 0xFF555555, 0xFF878787),
    GANYMEDE("Ganymede", 0xFF2F2F2F, 0xFFD9D9D9, 0xFFAAAAAA, 0xFF3A3A3A, 0x33FFFFFF, 0xFF555555, 0xFFBBBBBB),
    CALLISTO("Callisto", 0xFF1A2A1A, 0xFF50FF50, 0xFF777777, 0xFF223322, 0x3344FF44, 0xFF228822, 0xFFAAFFAA),
    EUROPA("Europa", 0xFF1A1A2A, 0xFF6666FF, 0xFFAAAAAA, 0xFF1A1A33, 0x334444FF, 0xFF4444FF, 0xFFAAAAFF),
    AMALTHEA("Amalthea", 0xFF2A1A2A, 0xFFCC66FF, 0xFFAAAAAA, 0xFF331A33, 0x33CC66FF, 0xFF884499, 0xFFDDAAFF),
    THEBE("Thebe", 0xFF2A1A0F, 0xFFFF8C00, 0xFFAAAAAA, 0xFF3D2415, 0x33FF8C00, 0xFF8B4513, 0xFFFFAA55),
    METIS("Metis", 0xFF1A1A1A, 0xFFC0C0C0, 0xFF888888, 0xFF252525, 0x33C0C0C0, 0xFF606060, 0xFFD3D3D3),
    ADRASTEA("Adrastea", 0xFF0F1520, 0xFF87CEEB, 0xFFAAAAAA, 0xFF1A2530, 0x3387CEEB, 0xFF4682B4, 0xFFADD8E6),
    FUTURE("Future", 0xFF121212, 0xFF5A8CFF, 0xFF8E8E8E, 0xE6101010, 0x4490AFFF, 0xFF2A2A2A, 0xFF6AA0FF),
    FUTURE_RED("RedFuture", 0xFF700000, 0xFF801010, 0xFF8E8E8E, 0xE63d110e, 0x44700000, 0xFF2A2A2A, 0xFFAA2020),
    TITAN("Titan", 0xFF1F1A12, 0xFFFFC857, 0xFFAAA08E, 0xFF2A2218, 0x33FFC857, 0xFF6B5330, 0xFFFFD98A),
    PHOBOS("Phobos", 0xFF1A1114, 0xFFFF4D6D, 0xFFB38A94, 0xFF26181D, 0x33FF4D6D, 0xFF6D2A3A, 0xFFFF8097),
    DEIMOS("Deimos", 0xFF10161F, 0xFF00D1B2, 0xFF8EA7A3, 0xFF19232F, 0x3300D1B2, 0xFF1F6F66, 0xFF74FFE9),
    TRITON("Triton", 0xFF111A2A, 0xFF4DB8FF, 0xFF95A7C2, 0xFF1A263A, 0x334DB8FF, 0xFF2D5E88, 0xFF8AD4FF);
    public final int titleBar, moduleEnabled, moduleDisabled, panelBackground, hoverHighlight, sliderBackground, sliderForeground;
    private final String name;

    Theme(String name, int titleBar, int moduleEnabled, int moduleDisabled, int panelBackground, int hoverHighlight,
            int sliderBackground, int sliderForeground) {
        this.name = name;
        this.titleBar = titleBar;
        this.moduleEnabled = moduleEnabled;
        this.moduleDisabled = moduleDisabled;
        this.panelBackground = panelBackground;
        this.hoverHighlight = hoverHighlight;
        this.sliderBackground = sliderBackground;
        this.sliderForeground = sliderForeground;
    }

    public String getName() {
        return name;
    }
}


