package io.client.modules;

import io.client.Category;
import io.client.ClickGuiScreen;
import io.client.Module;
import io.client.TargetManager;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ESP extends Module {
    private final BooleanSetting showHP;
    private final BooleanSetting playersOnly;
    private final NumberSetting baseScale;
    private int enabledColor = 0xFFc71e00;
    private int disabledColor = 0xFFFFFFFF;

    public ESP() {
        super("ESP", "Basic heads on ESP", 0, Category.RENDER);

        showHP = new BooleanSetting("ShowHP", true);
        addSetting(showHP);

        playersOnly = new BooleanSetting("PlayersOnly", false);
        addSetting(playersOnly);

        baseScale = new NumberSetting("Scale", 1.0f, 0.3f, 3.0f);
        addSetting(baseScale);

        loadColors();
    }


    private void loadColors() {
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
                disabledColor = 0xFF878787;
                break;
            case "GANYMEDE":
            case "Ganymede":
                enabledColor = 0xFFD9D9D9;
                disabledColor = 0xFFBBBBBB;
                break;
            case "CALLISTO":
            case "Callisto":
                enabledColor = 0xFF50FF50;
                disabledColor = 0xFFAAFFAA;
                break;
            case "EUROPA":
            case "Europa":
                enabledColor = 0xFF6666FF;
                disabledColor = 0xFFAAAAFF;
                break;
            default:
                enabledColor = 0xFFc71e00;
                disabledColor = 0xFF878787;
                break;
        }
    }


    @Override
    public void onUpdate() {
        if (ClickGuiScreen.opened) {
            enabledColor = ClickGuiScreen.currentTheme.moduleEnabled;
            disabledColor = ClickGuiScreen.currentTheme.sliderForeground;
        }
    }

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 camPos = cam.getPosition();

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        double baseFov = mc.options.fov().get();
        float fovModifier = 1.0f;

        if (mc.player.isSprinting()) {
            fovModifier += 0.15f;
        }

        if (mc.player.getAbilities().flying) {
            fovModifier += 0.1f;
        }

        float currentFov = (float) (baseFov * fovModifier);
        Matrix4f projection = mc.gameRenderer.getProjectionMatrix(currentFov);
        Matrix4f modelView = new Matrix4f();

        org.joml.Quaternionf rotation = new org.joml.Quaternionf(cam.rotation());
        rotation.conjugate();
        modelView.rotation(rotation);

        Matrix4f mvp = new Matrix4f(projection);
        mvp.mul(modelView);

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity living)) continue;
            if (e == mc.player) continue;
            if (playersOnly.isEnabled()) {
                if (!(e instanceof net.minecraft.world.entity.player.Player)) continue;
            } else {
                if (!TargetManager.INSTANCE.isValidTarget(e)) continue;
            }

            double dist = mc.player.distanceTo(e);
            if (dist > 64.0) continue;

            double x = e.xo + (e.getX() - e.xo) * partialTicks - camPos.x;
            double y = e.yo + (e.getY() - e.yo) * partialTicks + e.getBbHeight() + 0.2 - camPos.y;
            double z = e.zo + (e.getZ() - e.zo) * partialTicks - camPos.z;

            Vector4f pos = new Vector4f((float) x, (float) y, (float) z, 1.0f);
            pos.mul(mvp);

            if (pos.w <= 0.0f) continue;
            if (pos.z <= 0.0f) continue;

            float nx = pos.x / pos.w;
            float ny = pos.y / pos.w;

            if (nx < -1.5f || nx > 1.5f || ny < -1.5f || ny > 1.5f) continue;

            int sx = (int) ((nx * 0.5f + 0.5f) * sw);
            int sy = (int) ((1.0f - (ny * 0.5f + 0.5f)) * sh);

            float scale = Math.max(0.5f, Math.min(1.2f, (float) (16.0 / dist))) * baseScale.getValue();

            String name = e.getName().getString();
            String hp = String.format("HP: %.1f", living.getHealth());

            int nameW = mc.font.width(name);
            int hpW = mc.font.width(hp);
            int maxW = showHP.isEnabled() ? Math.max(nameW, hpW) : nameW;

            var pose = graphics.pose();
            pose.pushMatrix();
            pose.translate(sx, sy);
            pose.scale(scale, scale);

            int bracketOffset = 6;
            int drawY = -10;

            graphics.drawString(mc.font, "[", -(maxW / 2) - bracketOffset, drawY, enabledColor, true);
            graphics.drawString(mc.font, "]", (maxW / 2) + 2, drawY, enabledColor, true);

            graphics.drawString(mc.font, name, -(nameW / 2), drawY, disabledColor, true);

            if (showHP.isEnabled()) {
                graphics.drawString(mc.font, hp, -(hpW / 2), drawY + 10,
                        getHealthColor(living.getHealth(), living.getMaxHealth(), living), true);
            }

            pose.popMatrix();
        }
    }

    private int getHealthColor(float hp, float max, LivingEntity e) {
        float p = hp / max;
        if (e == Minecraft.getInstance().player) {
            if (hp > 20.0) return 0xFFFF0000;
            if (hp > 15.0) return 0xFF00FF00;
            if (hp > 9.0) return 0xFFFFAA00;

        } else {
            if (p > 0.6f) return 0xFF00FF00;
            if (p > 0.3f) return 0xFFFFAA00;
        }

        return 0xFFFF0000;


    }
}