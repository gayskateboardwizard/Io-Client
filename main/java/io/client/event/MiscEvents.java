package io.client.event;

import com.mojang.brigadier.ParseResults;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.command.ServerCommandSource;

public class MiscEvents {
    public static final Event<CommandExecute> COMMAND_EXECUTE = EventFactory.createArrayBacked(CommandExecute.class, (callbacks) -> (results) -> {
        for (CommandExecute event : callbacks) {
            boolean result = event.onCommandExecuted(results);
            if (!result) {
                return false;
            }
        }
        return true;
    });

    @FunctionalInterface
    public interface CommandExecute {
        boolean onCommandExecuted(ParseResults<ServerCommandSource> results);
    }
}