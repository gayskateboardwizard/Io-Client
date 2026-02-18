package io.client.commands;

import io.client.managers.TargetManager;

public class UnfriendCommand implements Command {
    @Override
    public void execute(String[] args) throws Exception {
        if (args.length < 1) {
            CommandManager.INSTANCE.sendMessage("§cUsage: |unfriend <username>");
            return;
        }

        String username = args[0];

        if (!TargetManager.INSTANCE.isFriend(username)) {
            CommandManager.INSTANCE.sendMessage("§e" + username + " §7is not your friend");
        } else {
            TargetManager.INSTANCE.removeFriend(username);
            CommandManager.INSTANCE.sendMessage("§cRemoved §e" + username + " §cfrom friends");
        }
    }
}

