package com.geekbrains.cloudstorage.cloudserver;

public class CloudUserCommand {
    private CloudUser user;
    private ServerCommand command;

    public CloudUserCommand(CloudUser user, ServerCommand command) {
        this.user = user;
        this.command = command;
    }

    public CloudUser getUser() {
        return user;
    }

    public ServerCommand getCommand() {
        return command;
    }
}