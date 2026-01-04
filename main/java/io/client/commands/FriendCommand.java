package io.client.commands;

import io.client.TargetManager;

public class FriendCommand implements Command {
    @Override
    public void execute(String[] args) throws Exception {
        if (args.length < 1) {
            CommandManager.INSTANCE.sendMessage("§cUsage: |friend <username>");
            return;
        }

        String username = args[0];

        if (TargetManager.INSTANCE.isFriend(username)) {
            CommandManager.INSTANCE.sendMessage("§e" + username + " §7is already your friend");
        } else {
            TargetManager.INSTANCE.addFriend(username);
            CommandManager.INSTANCE.sendMessage("§aAdded §e" + username + " §ato friends");
        }
    }
}