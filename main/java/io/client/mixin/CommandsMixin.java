package io.client.mixin;

import com.mojang.brigadier.ParseResults;
import io.client.event.MiscEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public abstract class CommandsMixin {
    @Inject(method = "performCommand(Lcom/mojang/brigadier/ParseResults;Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    public void performCommand(ParseResults<CommandSourceStack> parseResults, String string, CallbackInfo ci) {
        boolean result = MiscEvents.COMMAND_EXECUTE.invoker().onCommandExecuted(parseResults);
        if (!result)
            ci.cancel();
    }
}