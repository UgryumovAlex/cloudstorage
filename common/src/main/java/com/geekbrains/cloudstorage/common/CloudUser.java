package com.geekbrains.cloudstorage.common;

import java.io.Serializable;

public class CloudUser implements Serializable {
    private String login;
    private String password;
    private String userDirectory;

    public CloudUser(String login, String password, String userDirectory) {
        this.login = login;
        this.password = password;
        this.userDirectory = userDirectory;
    }

    public String getUserDirectory() {
        return userDirectory;
    }

    public String getLogin() {
        return login;
    }

    @Override
    public String toString() {
        return "CloudUser{" +
                "login='" + login + '\'' +
                ", password='" + password + '\'' +
                ", userDirectory='" + userDirectory + '\'' +
                '}';
    }
}
