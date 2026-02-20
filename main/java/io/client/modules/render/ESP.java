package io.client.modules.render;

import io.client.clickgui.screens.ClickGuiScreen;
import io.client.managers.ModuleManager;
import io.client.managers.TargetManager;
import io.client.modules.templates.Category;
import io.client.modules.templates.Module;
import io.client.settings.BooleanSetting;
import io.client.settings.NumberSetting;
import io.client.settings.RadioSetting;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

public class ESP extends Module {
    private final BooleanSetting showHP;
    private final BooleanSetting playersOnly;
    private final BooleanSetting showArmor;
    private final RadioSetting armorPosition;
    private final NumberSetting baseScale;
    private final BooleanSetting disableVanillaNametags;

    public ESP() {
        super("ESP", "Basic heads on ESP", 0, Category.RENDER);

        showHP = new BooleanSetting("ShowHP", true);
        addSetting(showHP);

        playersOnly = new BooleanSetting("PlayersOnly", false);
        addSetting(playersOnly);

        showArmor = new BooleanSetting("ShowArmor", true);
        addSetting(showArmor);

        armorPosition = new io.client.settings.RadioSetting("ArmorPosition", "Bottom");
        armorPosition.addOption("Top");
        armorPosition.addOption("Bottom");
        addSetting(armorPosition);

        baseScale = new NumberSetting("Scale", 1.0f, 0.3f, 3.0f);
        addSetting(baseScale);

        disableVanillaNametags = new BooleanSetting("DisableVanillaNametags", true);
        addSetting(disableVanillaNametags);
    }

    public void render(DrawContext graphics, float partialTicks) {
        if (!isEnabled())
            return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null)
            return;

        int enabledColor = ClickGuiScreen.currentTheme.moduleEnabled;
        int disabledColor = ClickGuiScreen.currentTheme.sliderForeground;

        Camera cam = mc.gameRenderer.getCamera();
        Vec3d camPos = cam.getPos();

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        double baseFov = mc.options.getFov().getValue();
        float fovModifier = 1.0f;

        if (mc.player.isSprinting()) {
            fovModifier += 0.15f;
        }

        if (mc.player.getAbilities().flying) {
            fovModifier += 0.1f;
        }

        float currentFov = (float) (baseFov * fovModifier);
        Matrix4f projection = mc.gameRenderer.getBasicProjectionMatrix(currentFov);
        Matrix4f modelView = new Matrix4f();

        org.joml.Quaternionf rotation = new org.joml.Quaternionf(cam.getRotation());
        rotation.conjugate();
        modelView.rotation(rotation);

        Matrix4f mvp = new Matrix4f(projection);
        mvp.mul(modelView);

        int renderDist = mc.options.getViewDistance().getValue();
        double maxRange = renderDist * 16;

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity living))
                continue;
            if (e == mc.player)
                continue;
            if (playersOnly.isEnabled()) {
                if (!(e instanceof net.minecraft.entity.player.PlayerEntity))
                    continue;
            } else {
                if (!TargetManager.INSTANCE.isValidTarget(e))
                    continue;
            }

            double dist = mc.player.distanceTo(e);
            if (dist > maxRange)
                continue;

            double x = e.lastX + (e.getX() - e.lastX) * partialTicks - camPos.x;
            double y = e.lastY + (e.getY() - e.lastY) * partialTicks + e.getHeight() + 0.2 - camPos.y;
            double z = e.lastZ + (e.getZ() - e.lastZ) * partialTicks - camPos.z;

            Vector4f pos = new Vector4f((float) x, (float) y, (float) z, 1.0f);
            pos.mul(mvp);

            if (pos.w <= 0.0f)
                continue;
            if (pos.z <= 0.0f)
                continue;

            float nx = pos.x / pos.w;
            float ny = pos.y / pos.w;

            if (nx < -1.5f || nx > 1.5f || ny < -1.5f || ny > 1.5f)
                continue;

            int sx = (int) ((nx * 0.5f + 0.5f) * sw);
            int sy = (int) ((1.0f - (ny * 0.5f + 0.5f)) * sh);

            float scale = Math.max(0.5f, Math.min(1.2f, (float) (16.0 / dist))) * baseScale.getValue();

            String name = e.getName().getString();
            String hp = String.format("HP: %.1f", living.getHealth());

            int nameW = mc.textRenderer.getWidth(name);
            int hpW = mc.textRenderer.getWidth(hp);
            int maxW = showHP.isEnabled() ? Math.max(nameW, hpW) : nameW;

            var pose = graphics.getMatrices();
            pose.pushMatrix();
            pose.translate(sx, sy);
            pose.scale(scale, scale);

            int bracketOffset = 6;
            int drawY = -10;

            graphics.drawText(mc.textRenderer, "[", -(maxW / 2) - bracketOffset, drawY, enabledColor, true);
            graphics.drawText(mc.textRenderer, "]", (maxW / 2) + 2, drawY, enabledColor, true);

            graphics.drawText(mc.textRenderer, name, -(nameW / 2), drawY, disabledColor, true);

            if (showHP.isEnabled()) {
                graphics.drawText(mc.textRenderer, hp, -(hpW / 2), drawY + 10,
                        getHealthColor(living.getHealth(), living.getMaxHealth(), living), true);
            }

            if (showArmor.isEnabled() && e instanceof PlayerEntity player) {
                int armorY;
                if (armorPosition.isSelected("Top")) {
                    armorY = drawY - 18;
                } else {
                    armorY = drawY + (showHP.isEnabled() ? 22 : 12);
                }

                ItemStack offhand = player.getOffHandStack();
                ItemStack mainhand = player.getMainHandStack();

                int armorCount = 0;
                for (int i = 0; i < 4; i++) {
                    ItemStack armorStack = player.getInventory().getStack(36 + i);
                    if (!armorStack.isEmpty()) {
                        armorCount++;
                    }
                }

                int totalWidth = armorCount * 16;
                if (!offhand.isEmpty()) totalWidth += 16;
                if (!mainhand.isEmpty()) totalWidth += 16;

                int startX = -(totalWidth / 2);
                int currentX = startX;

                if (!offhand.isEmpty()) {
                    graphics.drawItem(offhand, currentX, armorY);
                    graphics.drawStackOverlay(mc.textRenderer, offhand, currentX, armorY);
                    currentX += 16;
                }

                for (int i = 0; i < 4; i++) {
                    ItemStack armorStack = player.getInventory().getStack(36 + i);
                    if (armorStack.isEmpty()) {
                        continue;
                    }

                    graphics.drawItem(armorStack, currentX, armorY);
                    graphics.drawStackOverlay(mc.textRenderer, armorStack, currentX, armorY);
                    currentX += 16;
                }

                if (!mainhand.isEmpty()) {
                    graphics.drawItem(mainhand, currentX, armorY);
                    graphics.drawStackOverlay(mc.textRenderer, mainhand, currentX, armorY);
                }
            }

            pose.popMatrix();
        }
    }

    private int getHealthColor(float hp, float max, LivingEntity e) {
        float p = hp / max;
        if (e == MinecraftClient.getInstance().player) {
            if (hp > 20.0)
                return 0xFFFF0000;
            if (hp > 15.0)
                return 0xFF00FF00;
            if (hp > 9.0)
                return 0xFFFFAA00;

        } else {
            if (p > 0.6f)
                return 0xFF00FF00;
            if (p > 0.3f)
                return 0xFFFFAA00;
        }

        return 0xFFFF0000;

    }

    public boolean shouldDisableVanillaNametags() {
        return disableVanillaNametags.isEnabled();
    }

    public static boolean shouldHideVanillaPlayerNametags() {
        ESP esp = ModuleManager.INSTANCE.getModule(ESP.class);
        return esp != null && esp.isEnabled() && esp.shouldDisableVanillaNametags();
    }
}

