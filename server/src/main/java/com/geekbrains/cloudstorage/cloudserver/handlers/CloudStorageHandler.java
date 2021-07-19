package com.geekbrains.cloudstorage.cloudserver.handlers;

import com.geekbrains.cloudstorage.cloudserver.CloudUser;
import com.geekbrains.cloudstorage.cloudserver.CloudUserCommand;
import com.geekbrains.cloudstorage.cloudserver.StorageLogic;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class CloudStorageHandler extends SimpleChannelInboundHandler<CloudUserCommand> {

    private final String serverStorageUserData = "server" + File.separator + "Storage" + File.separator + "UserData";
    private StorageLogic storageLogic;
    private static String lastCommandWithResponseRequest;
    private static String lastCommandParam;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudUserCommand s) throws Exception {
        List<String> result = new ArrayList<>();

        try {
            if (storageLogic == null) {
                storageLogic = new StorageLogic(Paths.get(serverStorageUserData + File.separator + s.getUser().getUserDirectory()));
            }

            switch (s.getCommand().getName()) {

                case "ls":
                    result.add(storageLogic.getFilesList());
                    break;

                case "mkdir":
                    result.add(storageLogic.createDirectory(s.getCommand().getParams().get(0)));
                    break;

                case "touch":
                    result.add(storageLogic.createFile(s.getCommand().getParams().get(0)));
                    break;

                case "cd":
                    storageLogic.changeDirectory(s.getCommand().getParams().get(0));
                    break;

                case "rm":
                    lastCommandWithResponseRequest = "rm";
                    lastCommandParam = s.getCommand().getParams().get(0);
                    result.add(storageLogic.removeFileOrDirectory(s.getCommand().getParams().get(0)));
                    break;

                case "N":
                    if (lastCommandWithResponseRequest != null) {
                        lastCommandWithResponseRequest = null;
                        lastCommandParam = null;
                    }
                    break;

                case "Y":
                    if (lastCommandWithResponseRequest != null && lastCommandWithResponseRequest.equals("rm")) {
                        storageLogic.deleteNotEmptyDirectory(lastCommandParam);
                        lastCommandWithResponseRequest = null;
                        lastCommandParam = null;
                        result.add("deleted\r\n");
                    }
                    break;

                case "copy":
                    storageLogic.copy(s.getCommand().getParams().get(0), s.getCommand().getParams().get(1));
                    result.add("copied\r\n");
                    break;
            }

        } catch (Exception e) {
            result.add(e.getMessage());
        }

        if (result.size() > 0) {
            for (String msg : result) {
                ctx.writeAndFlush(msg);
            }
        }
        ctx.writeAndFlush(getUserWelcome(s.getUser())); //Временно для тестирования через PUTTY

    }

    private String getUserWelcome(CloudUser user) {
        return "\r\n" + user.getLogin() + " " + storageLogic.getUserPath() + " : ";
    }
}
