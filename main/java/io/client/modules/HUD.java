package io.client.modules;

import io.client.Category;
import io.client.ClickGuiScreen;
import io.client.Module;
import io.client.ModuleManager;
import io.client.settings.BooleanSetting;
import io.client.settings.RadioSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

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
    private final RadioSetting horizontal;
    private final RadioSetting vertical;

    private int lastFPS = 0;

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

        horizontal = new RadioSetting("Horizontal", "Left");
        horizontal.addOption("Left");
        horizontal.addOption("Right");

        vertical = new RadioSetting("Vertical", "Top");
        vertical.addOption("Top");
        vertical.addOption("Bottom");

        addSetting(showWatermark);
        addSetting(showFPS);
        addSetting(showCoords);
        addSetting(showBiome);
        addSetting(showDirection);
        addSetting(showSpeed);
        addSetting(showPing);
        addSetting(showTime);
        addSetting(showChordTarget);
        addSetting(horizontal);
        addSetting(vertical);
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

    public void render(DrawContext graphics) {
        if (!isEnabled()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        float scale = 1.0f;
        GUIScale guiScale = ModuleManager.INSTANCE.getModule(GUIScale.class);
        if (guiScale != null) {
            scale = guiScale.getScale();
        }

        List<String> lines = new ArrayList<>();

        if (showWatermark.isEnabled()) {
            lines.add("Io Client");
        }

        if (showFPS.isEnabled()) {
            int fps = mc.getCurrentFps();
            if (fps > 0) lastFPS = fps;
            lines.add("FPS: " + lastFPS);
        }

        if (showCoords.isEnabled()) {
            PlayerEntity player = mc.player;
            lines.add(String.format("XYZ: %.1f %.1f %.1f", player.getX(), player.getY(), player.getZ()));
        }

        if (showBiome.isEnabled()) {
            BlockPos pos = mc.player.getBlockPos();
            String biome = mc.world.getBiome(pos).getKey()
                    .map(key -> key.getValue().getPath())
                    .orElse("unknown");
            lines.add("Biome: " + biome);
        }

        if (showDirection.isEnabled()) {
            Direction direction = mc.player.getHorizontalFacing();
            float yaw = mc.player.getYaw() % 360;
            if (yaw < 0) yaw += 360;
            lines.add("Dir: " + String.format("%s [%.1f°]", direction.getId(), yaw));
        }

        if (showSpeed.isEnabled()) {
            double dx = mc.player.getX() - mc.player.lastRenderX;
            double dz = mc.player.getZ() - mc.player.lastRenderZ;
            double speed = Math.sqrt(dx * dx + dz * dz) * 20.0;
            lines.add(String.format("Speed: %.2f m/s", speed));
        }

        if (showPing.isEnabled() && mc.getNetworkHandler() != null) {
            int ping = 0;
            var playerInfo = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (playerInfo != null) ping = playerInfo.getLatency();
            lines.add("Ping: " + ping + "ms");
        }

        if (showTime.isEnabled()) {
            long time = mc.world.getTimeOfDay() % 24000;
            int hours = (int) ((time / 1000 + 6) % 24);
            int minutes = (int) ((time % 1000) * 60 / 1000);
            lines.add(String.format("Time: %02d:%02d", hours, minutes));
        }

        if (showChordTarget.isEnabled() && hasChordTarget) {
            PlayerEntity player = mc.player;
            double dx = chordTargetX - player.getX();
            double dy = chordTargetY - player.getY();
            double dz = chordTargetZ - player.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            double angle = Math.toDegrees(Math.atan2(dz, dx)) - 90;
            double relative = angle - player.getYaw();
            while (relative > 180) relative -= 360;
            while (relative < -180) relative += 360;

            String arrow = getDirectionArrow(relative);
            lines.add(String.format("Target: %.1f %.1f %.1f %s [%.0fm]",
                    chordTargetX, chordTargetY, chordTargetZ, arrow, distance));
        }

        boolean isTop = vertical.isSelected("Top");
        boolean isLeft = horizontal.isSelected("Left");

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        int lineHeight = (int)(10 * scale);
        int startY = isTop ? 2 : screenHeight - (lines.size() * lineHeight + 2);

        graphics.getMatrices().pushMatrix();
        graphics.getMatrices().scale(scale, scale);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int textWidth = mc.textRenderer.getWidth(line);

            int y = isTop ? (startY + i * lineHeight) : (startY + i * lineHeight);
            int x;

            if (isLeft) {
                x = 2;
            } else {
                x = screenWidth - (int)(textWidth * scale) - 2;
            }

            int scaledX = (int) (x / scale);
            int scaledY = (int) (y / scale);

            graphics.drawText(mc.textRenderer, line, scaledX, scaledY, ClickGuiScreen.currentTheme.moduleEnabled, true);
        }

        graphics.getMatrices().popMatrix();
    }
}