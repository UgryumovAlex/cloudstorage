package com.geekbrains.cloudstorage.cloudserver;

public class CloudUser {
    private String login;
    private String password;
    private String nick;

    private String userDirectory;

    public CloudUser(String login, String password, String nick, String userDirectory) {
        this.login = login;
        this.password = password;
        this.nick = nick;
        this.userDirectory = userDirectory;
    }

    public String getUserDirectory() {
        return userDirectory;
    }

    public String getLogin() {
        return login;
    }

    public String getNick() {
        return nick;
    }

    @Override
    public String toString() {
        return "CloudUser{" +
                "login='" + login + '\'' +
                ", password='" + password + '\'' +
                ", nick='" + nick + '\'' +
                ", userDirectory='" + userDirectory + '\'' +
                '}';
    }
}
