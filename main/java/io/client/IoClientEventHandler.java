package io.client;

import io.client.commands.CommandManager;
import io.client.modules.ArmorHud;
import io.client.modules.ESP;
import io.client.modules.IoSwag;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class IoClientEventHandler {

    private static final IoClientEventHandler INSTANCE = new IoClientEventHandler();

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
            handleKeys(client);
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
            io.client.modules.HUD hud =
                    ModuleManager.INSTANCE.getModule(io.client.modules.HUD.class);
            if (hud != null && hud.isEnabled()) {
                hud.render(drawContext);
            }
            ESP esp = ModuleManager.INSTANCE.getModule(ESP.class);
            if (esp != null && esp.isEnabled()) {
                esp.render(drawContext, tickDelta.getGameTimeDeltaTicks());
            }
            ArmorHud armorHud = ModuleManager.INSTANCE.getModule(ArmorHud.class);
            if (armorHud != null && armorHud.isEnabled()) {
                armorHud.render(drawContext, tickDelta.getGameTimeDeltaTicks());
            }
        });
    }

    private void handleKeys(Minecraft mc) {

        if (mc.getWindow() == null) {
            return;
        }

        long window = mc.getWindow().getWindow();


        if (mc.screen == null) {
            if (isPressed(window, GLFW.GLFW_KEY_BACKSLASH)) {
                if (!KeyManager.INSTANCE.isKeyPressed(GLFW.GLFW_KEY_BACKSLASH)) {
                    mc.setScreen(new ClickGuiScreen());
                    KeyManager.INSTANCE.addKey(GLFW.GLFW_KEY_BACKSLASH);
                }
            } else {
                KeyManager.INSTANCE.removeKey(GLFW.GLFW_KEY_BACKSLASH);
            }
        }


        if (mc.screen == null) {
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



        if (mc.screen instanceof ChatScreen chat) {
            if (chat.getFocused() instanceof EditBox editBox) {
                if (isPressed(window, GLFW.GLFW_KEY_TAB)) {
                    if (!KeyManager.INSTANCE.isKeyPressed(GLFW.GLFW_KEY_TAB)) {
                        String current = editBox.getValue();
                        if (current.startsWith("|")) {
                            String next = CommandManager.INSTANCE.getNextSuggestion(current);
                            if (next != null) {
                                editBox.setValue(next);
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
