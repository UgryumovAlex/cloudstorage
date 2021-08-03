package com.geekbrains.cloudstorage.cloudserver.handlers;


import com.geekbrains.cloudstorage.cloudserver.CloudUserCommand;
import com.geekbrains.cloudstorage.cloudserver.StorageLogic;
import com.geekbrains.cloudstorage.common.FileProcess;
import com.geekbrains.cloudstorage.common.ResponseCommand;
import com.geekbrains.cloudstorage.common.ServerResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

/**
 * Обработчик предназначени для выполнения команд на сервере
 * */
public class CloudStorageHandler extends SimpleChannelInboundHandler<CloudUserCommand> {

    private final String serverStorageUserData = "server" + File.separator + "Storage" + File.separator + "UserData";
    private StorageLogic storageLogic;
    private static String lastCommandWithResponseRequest;
    private static String lastCommandParam;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudUserCommand s) throws Exception {

        try {
            if (storageLogic == null) {
                storageLogic = new StorageLogic(Paths.get(serverStorageUserData + File.separator + s.getUser().getUserDirectory()));
            }

            switch (s.getCommand().getName()) {

                case "ls": //Получение содержимого текущего каталога
                    sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_LIST, storageLogic.getFilesList(), storageLogic.getUserPath()));
                    break;

                case "mkdir": //Создание нового каталога
                    try {
                        if (storageLogic.createDirectory(s.getCommand().getParams().get(0))) {
                            sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_MKDIR_OK, storageLogic.getFilesList(), storageLogic.getUserPath()));
                        }
                    } catch (FileAlreadyExistsException e) {
                        sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_MKDIR_ALREADY_EXISTS));
                    } catch (Exception e) {
                        sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_MKDIR_FAIL));
                    }
                    break;

                case "touch": //Создание нового файла
                    try {
                        if (storageLogic.createFile(s.getCommand().getParams().get(0))) {
                            sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_TOUCH_OK, storageLogic.getFilesList(), storageLogic.getUserPath()));
                        }
                    } catch (FileAlreadyExistsException e) {
                        sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_TOUCH_ALREADY_EXISTS));
                    } catch (Exception e) {
                        sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_TOUCH_FAIL));
                    }

                    break;

                case "cd": //Смена текущего каталога
                    try {
                        storageLogic.changeDirectory(s.getCommand().getParams().get(0));
                        sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_CD_OK, storageLogic.getFilesList(), storageLogic.getUserPath()));
                    } catch (IllegalArgumentException e) {
                        sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_CD_FAIL));
                    }
                    break;

                case "rm": //Удаление каталога. Если он не пустой, то формируется запрос пользователю на подтверждение
                    lastCommandWithResponseRequest = "rm";
                    lastCommandParam = s.getCommand().getParams().get(0);
                    try {
                        if (storageLogic.removeFileOrDirectory(s.getCommand().getParams().get(0))) {
                            sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_RM_OK, storageLogic.getFilesList(), storageLogic.getUserPath()));
                        }
                    } catch (DirectoryNotEmptyException e) {
                        sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_RM_DELETE_DIR));
                    } catch (NoSuchFileException e) {
                        sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_RM_NOT_EXISTS));
                    } catch (IOException e) {
                        sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_RM_FAIL));
                    }
                    break;

                case "N": //Используется при подтверждении удаления непустого каталога. Удаление отменяется
                    if (lastCommandWithResponseRequest != null) {
                        lastCommandWithResponseRequest = null;
                        lastCommandParam = null;
                        sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_RM_OK, storageLogic.getFilesList(), storageLogic.getUserPath()));
                    }
                    break;

                case "Y": //Используется при подтверждении удаления непустого каталога. Каталог удаляется
                    if (lastCommandWithResponseRequest != null && lastCommandWithResponseRequest.equals("rm")) {
                        try {
                            storageLogic.deleteNotEmptyDirectory(lastCommandParam);
                            sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_RM_OK, storageLogic.getFilesList(), storageLogic.getUserPath()));

                        } catch (IOException e) {
                            sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILES_RM_FAIL));
                        }
                        lastCommandWithResponseRequest = null;
                        lastCommandParam = null;
                    }
                    break;

                case "uploadFile": //Загрузка файла с клиента на сервер
                    File fileUp = new File(storageLogic.getCurrentPath() + File.separator + s.getCommand().getParams().get(0));
                    FileProcess fu = new FileProcess(fileUp, Long.parseLong(s.getCommand().getParams().get(1)), "UPLOAD");
                    if (fu.getSize() != 0) {
                        ctx.channel().pipeline().fireUserEventTriggered(fu); //Включаем обработчик FileProcessHandler в режим UPLOAD
                    }
                    break;

                case "downloadFile":
                    File fileDown = new File(storageLogic.getCurrentPath() + File.separator + s.getCommand().getParams().get(0));
                    FileProcess fd = new FileProcess(fileDown, Long.parseLong(s.getCommand().getParams().get(1)), "DOWNLOAD");
                    if (fd.getSize() != 0) {
                        ctx.channel().pipeline().fireUserEventTriggered(fd); //Включаем обработчик FileProcessHandler в режим DOWNLOAD
                    }
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void sendResponse(ChannelHandlerContext ctx, ServerResponse<?> serverResponse) {
        ByteBuf bb = ctx.alloc().heapBuffer();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(serverResponse);
            bb.writeBytes(baos.toByteArray());
            ctx.channel().writeAndFlush(bb);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
