package io.client.mixin;

import com.mojang.brigadier.ParseResults;
import io.client.event.MiscEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandManager.class)
public abstract class CommandsMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    public void performCommand(ParseResults<ServerCommandSource> parseResults, String string, CallbackInfo ci) {
        boolean result = MiscEvents.COMMAND_EXECUTE.invoker().onCommandExecuted(parseResults);
        if (!result)
            ci.cancel();
    }
}

