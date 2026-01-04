package io.client.modules;

import io.client.Category;
import io.client.ClickGuiScreen;
import io.client.Module;
import io.client.settings.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class HUD extends Module {
    private final BooleanSetting showFPS;
    private final BooleanSetting showCoords;
    private final BooleanSetting showBiome;
    private final BooleanSetting showDirection;
    private final BooleanSetting showSpeed;
    private final BooleanSetting showPing;
    private final BooleanSetting showTime;
    private final BooleanSetting showWatermark;
    private final BooleanSetting showChordTarget;

    private int lastFPS = 0;
    private int enabledColor;
    private int disabledColor;

    private double chordTargetX = 0;
    private double chordTargetY = 0;
    private double chordTargetZ = 0;
    private boolean hasChordTarget = false;

    public HUD() {
        super("HUD", "Displays information", 0, Category.RENDER);

        showWatermark = new BooleanSetting("Watermark", true);
        showFPS = new BooleanSetting("FPS", true);
        showCoords = new BooleanSetting("Coordinates", true);
        showBiome = new BooleanSetting("Biome", false);
        showDirection = new BooleanSetting("Direction", false);
        showSpeed = new BooleanSetting("Speed", false);
        showPing = new BooleanSetting("Ping", false);
        showTime = new BooleanSetting("Time", false);
        showChordTarget = new BooleanSetting("Chord Target", false);

        addSetting(showWatermark);
        addSetting(showFPS);
        addSetting(showCoords);
        addSetting(showBiome);
        addSetting(showDirection);
        addSetting(showSpeed);
        addSetting(showPing);
        addSetting(showTime);
        addSetting(showChordTarget);

        loadColors();
    }

    public void setChordTarget(double x, double y, double z) {
        this.chordTargetX = x;
        this.chordTargetY = y;
        this.chordTargetZ = z;
        this.hasChordTarget = true;
    }

    public void clearChordTarget() {
        this.hasChordTarget = false;
    }

    public boolean hasChordTarget() {
        return hasChordTarget;
    }

    public double getChordTargetX() {
        return chordTargetX;
    }

    public double getChordTargetY() {
        return chordTargetY;
    }

    public double getChordTargetZ() {
        return chordTargetZ;
    }

    private void loadColors() {
        switch (ClickGuiScreen.currentTheme) {
            case IO, GANYMEDE, CALLISTO, EUROPA:
                enabledColor = ClickGuiScreen.currentTheme.moduleEnabled;
                disabledColor = ClickGuiScreen.currentTheme.moduleDisabled;
                break;
        }
        if (ClickGuiScreen.currentTheme != null) {
            File themeFile = new File(Minecraft.getInstance().gameDirectory, "io/modules.cfg");
            String activeTheme = "Io";

            if (themeFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(themeFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("Themes:setting:")) {
                            String[] parts = line.split(":");
                            if (parts.length == 4 && Boolean.parseBoolean(parts[3])) {
                                activeTheme = parts[2];
                                break;
                            }
                        }
                    }
                } catch (IOException ignored) {
                }
            }

            switch (activeTheme) {
                case "IO":
                case "Io":
                    enabledColor = 0xFFc71e00;
                    disabledColor = 0xFFAAAAAA;
                    break;
                case "GANYMEDE":
                case "Ganymede":
                    enabledColor = 0xFFD9D9D9;
                    disabledColor = 0xFFAAAAAA;
                    break;
                case "CALLISTO":
                case "Callisto":
                    enabledColor = 0xFF50FF50;
                    disabledColor = 0xFF777777;
                    break;
                case "EUROPA":
                case "Europa":
                    enabledColor = 0xFF6666FF;
                    disabledColor = 0xFFAAAAAA;
                    break;
                default:
                    enabledColor = 0xFFc71e00;
                    disabledColor = 0xFFAAAAAA;
                    break;
            }
        }
    }

    @Override
    public void onUpdate() {
        if (ClickGuiScreen.opened) {
            enabledColor = ClickGuiScreen.currentTheme.moduleEnabled;
        }
    }

    private String getDirectionArrow(double angle) {
        if (angle >= -22.5 && angle < 22.5) return "↑";
        if (angle >= 22.5 && angle < 67.5) return "↗";
        if (angle >= 67.5 && angle < 112.5) return "→";
        if (angle >= 112.5 && angle < 157.5) return "↘";
        if (angle >= 157.5 || angle < -157.5) return "↓";
        if (angle >= -157.5 && angle < -112.5) return "↙";
        if (angle >= -112.5 && angle < -67.5) return "←";
        return "↖";
    }

    public void render(GuiGraphics graphics) {
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        int y = 2;

        if (showWatermark.isEnabled()) {
            graphics.drawString(mc.font, "Io Client", 2, y, enabledColor, true);
            y += 10;
        }

        if (showFPS.isEnabled()) {
            int fps = mc.getFps();
            if (fps > 0) lastFPS = fps;
            graphics.drawString(mc.font, "FPS: " + lastFPS, 2, y, enabledColor, true);
            y += 10;
        }

        if (showCoords.isEnabled()) {
            Player player = mc.player;
            graphics.drawString(mc.font,
                    String.format("XYZ: %.1f %.1f %.1f", player.getX(), player.getY(), player.getZ()),
                    2, y, enabledColor, true);
            y += 10;
        }

        if (showBiome.isEnabled()) {
            BlockPos pos = mc.player.blockPosition();
            String biome = mc.level.getBiome(pos).unwrapKey()
                    .map(key -> key.location().getPath())
                    .orElse("unknown");
            graphics.drawString(mc.font, "Biome: " + biome, 2, y, enabledColor, true);
            y += 10;
        }

        if (showDirection.isEnabled()) {
            Direction direction = mc.player.getDirection();
            float yaw = mc.player.getYRot() % 360;
            if (yaw < 0) yaw += 360;
            graphics.drawString(mc.font,
                    "Dir: " + String.format("%s [%.1f°]", direction.getName(), yaw),
                    2, y, enabledColor, true);
            y += 10;
        }

        if (showSpeed.isEnabled()) {
            double dx = mc.player.getX() - mc.player.xOld;
            double dz = mc.player.getZ() - mc.player.zOld;
            double speed = Math.sqrt(dx * dx + dz * dz) * 20.0;
            graphics.drawString(mc.font, String.format("Speed: %.2f m/s", speed), 2, y, enabledColor, true);
            y += 10;
        }

        if (showPing.isEnabled() && mc.getConnection() != null) {
            int ping = 0;
            var playerInfo = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (playerInfo != null) ping = playerInfo.getLatency();
            graphics.drawString(mc.font, "Ping: " + ping + "ms", 2, y, enabledColor, true);
            y += 10;
        }

        if (showTime.isEnabled()) {
            long time = mc.level.getDayTime() % 24000;
            int hours = (int) ((time / 1000 + 6) % 24);
            int minutes = (int) ((time % 1000) * 60 / 1000);
            graphics.drawString(mc.font, String.format("Time: %02d:%02d", hours, minutes), 2, y, enabledColor, true);
            y += 10;
        }

        if (showChordTarget.isEnabled() && hasChordTarget) {
            Player player = mc.player;
            double dx = chordTargetX - player.getX();
            double dy = chordTargetY - player.getY();
            double dz = chordTargetZ - player.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            double angle = Math.toDegrees(Math.atan2(dz, dx)) - 90;
            double relative = angle - player.getYRot();
            while (relative > 180) relative -= 360;
            while (relative < -180) relative += 360;

            String arrow = getDirectionArrow(relative);

            graphics.drawString(mc.font,
                    String.format("Target: %.1f %.1f %.1f %s [%.0fm]",
                            chordTargetX, chordTargetY, chordTargetZ, arrow, distance),
                    2, y, enabledColor, true);
            y += 10;
        }
    }
}