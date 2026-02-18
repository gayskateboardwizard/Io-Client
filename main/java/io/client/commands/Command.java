package io.client.commands;

public interface Command {
    void execute(String[] args) throws Exception;
}

