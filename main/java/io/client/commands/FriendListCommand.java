package io.client.commands;

import io.client.managers.TargetManager;

import java.util.Set;

public class FriendListCommand implements Command {
    @Override
    public void execute(String[] args) throws Exception {
        Set<String> friends = TargetManager.INSTANCE.getFriends();

        if (friends.isEmpty()) {
            CommandManager.INSTANCE.sendMessage("§7You do not have any friends :(");
            return;
        }

        CommandManager.INSTANCE.sendMessage("§9Friends §7(" + friends.size() + "):");
        for (String friend : friends) {
            CommandManager.INSTANCE.sendMessage("§7- §e" + friend);
        }
    }
}

