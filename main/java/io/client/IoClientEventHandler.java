package io.client;

import io.client.commands.CommandManager;
import io.client.modules.combat.OffHand;
import io.client.modules.render.ModuleHUD;
import io.client.modules.render.ArmorHud;
import io.client.modules.render.ESP;
import io.client.modules.misc.IoSwag;
import io.client.modules.settings.ThemeChanger;
import io.client.network.IoUserCapeService;
import io.client.clickgui.ClickGuiModeRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class IoClientEventHandler {

    private static final IoClientEventHandler INSTANCE = new IoClientEventHandler();
    private boolean rightClickPressed = false;

    private IoClientEventHandler() {
    }

    public static IoClientEventHandler getInstance() {
        return INSTANCE;
    }

    public void registerEvents() {

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            return !CommandManager.INSTANCE.handleMessage(message);
        });


        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null)
                return;
            IoUserCapeService.onClientTick(client);
            handleKeys(client);
            handleMouse(client);
            ModuleManager.INSTANCE.onUpdate();
        });


        ClientSendMessageEvents.MODIFY_CHAT.register(message -> {
            IoSwag ioSwag = ModuleManager.INSTANCE.getModule(IoSwag.class);
            String prefix = " ";

            if (ioSwag.isEnabled()
                    && !message.startsWith(".")
                    && !message.startsWith("/")
                    && !message.startsWith("|")) {
                if (ioSwag.greentext.isEnabled()) {
                    prefix = "> ";
                } else {
                    prefix = " ";
                }
                return prefix + message + " " + ioSwag.getSuffix();
            }
            return message;
        });


        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            io.client.modules.render.HUD hud =
                    ModuleManager.INSTANCE.getModule(io.client.modules.render.HUD.class);
            if (hud != null && hud.isEnabled()) {
                hud.render(drawContext);
            }

            ModuleHUD ModuleHUD = ModuleManager.INSTANCE.getModule(ModuleHUD.class);
            if (ModuleHUD != null && ModuleHUD.isEnabled()) {
                ModuleHUD.render(drawContext);
            }

            ESP esp = ModuleManager.INSTANCE.getModule(ESP.class);
            if (esp != null && esp.isEnabled()) {
                esp.render(drawContext, tickDelta.getDynamicDeltaTicks());
            }

            ArmorHud armorHud = ModuleManager.INSTANCE.getModule(ArmorHud.class);
            if (armorHud != null && armorHud.isEnabled()) {
                armorHud.render(drawContext, tickDelta.getDynamicDeltaTicks());
            }
        });
    }

    private void handleMouse(MinecraftClient mc) {
        if (mc.getWindow() == null || mc.currentScreen != null) {
            return;
        }

        long window = mc.getWindow().getHandle();
        boolean rightClickNow = isPressed(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT);

        OffHand offHand = ModuleManager.INSTANCE.getModule(OffHand.class);
        if (offHand != null && offHand.isEnabled()) {
            if (rightClickNow && !rightClickPressed) {
                offHand.setClicking(true);
            } else if (!rightClickNow && rightClickPressed) {
                offHand.setClicking(false);
            }
        }

        rightClickPressed = rightClickNow;
    }

    private void handleKeys(MinecraftClient mc) {

        if (mc.getWindow() == null) {
            return;
        }

        long window = mc.getWindow().getHandle();


        if (mc.currentScreen == null) {
            if (isPressed(window, ClickGuiScreen.clickGuiKey)) {
                if (!KeyManager.INSTANCE.isKeyPressed(ClickGuiScreen.clickGuiKey)) {
                    ThemeChanger themeChanger = ModuleManager.INSTANCE.getModule(ThemeChanger.class);
                    String guiMode = themeChanger != null
                            ? themeChanger.getSelectedClickGuiMode()
                            : ClickGuiModeRegistry.getDefaultModeName();
                    mc.setScreen(ClickGuiModeRegistry.createScreen(guiMode));
                    KeyManager.INSTANCE.addKey(ClickGuiScreen.clickGuiKey);
                }
            } else {
                KeyManager.INSTANCE.removeKey(ClickGuiScreen.clickGuiKey);
            }
        }


        if (mc.currentScreen == null) {
            for (int key = GLFW.GLFW_KEY_SPACE; key <= GLFW.GLFW_KEY_LAST; key++) {
                if (isPressed(window, key)) {
                    if (!KeyManager.INSTANCE.isKeyPressed(key)) {
                        ModuleManager.INSTANCE.onKeyPress(key);
                        KeyManager.INSTANCE.addKey(key);
                    }
                } else {
                    KeyManager.INSTANCE.removeKey(key);
                }
            }
        }



        if (mc.currentScreen instanceof ChatScreen chat) {
            if (chat.getFocused() instanceof TextFieldWidget editBox) {
                if (isPressed(window, GLFW.GLFW_KEY_TAB)) {
                    if (!KeyManager.INSTANCE.isKeyPressed(GLFW.GLFW_KEY_TAB)) {
                        String current = editBox.getText();
                        if (current.startsWith("|")) {
                            String next = CommandManager.INSTANCE.getNextSuggestion(current);
                            if (next != null) {
                                editBox.setText(next);
                            }
                        }
                        KeyManager.INSTANCE.addKey(GLFW.GLFW_KEY_TAB);
                    }
                } else {
                    KeyManager.INSTANCE.removeKey(GLFW.GLFW_KEY_TAB);
                }
            }
        }
    }

    private boolean isPressed(long window, int key) {
        return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }
}