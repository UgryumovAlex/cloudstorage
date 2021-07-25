package com.geekbrains.cloudstorage.common;

import java.io.Serializable;

public class ServerResponse<T> implements Serializable {
    private final ResponseCommand responseCommand;
    private final T responseObject;
    private final String currentPath;

    public ServerResponse(ResponseCommand responseCommand) {
        this.responseCommand = responseCommand;
        this.responseObject = null;
        this.currentPath = null;
    }

    public ServerResponse(ResponseCommand responseCommand, T responseObject) {
        this.responseCommand = responseCommand;
        this.responseObject = responseObject;
        this.currentPath = null;
    }

    public ServerResponse(ResponseCommand responseCommand, T responseObject, String currentPath) {
        this.responseCommand = responseCommand;
        this.responseObject = responseObject;
        this.currentPath = currentPath;
    }

    public ResponseCommand getResponseCommand() {
        return responseCommand;
    }

    public T getResponseObject() {
        return responseObject;
    }

    public String getCurrentPath() { return currentPath; }

}
