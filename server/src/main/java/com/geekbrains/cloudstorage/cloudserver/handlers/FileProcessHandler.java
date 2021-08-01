package com.geekbrains.cloudstorage.cloudserver.handlers;


import com.geekbrains.cloudstorage.common.FileProcess;
import com.geekbrains.cloudstorage.common.ResponseCommand;
import com.geekbrains.cloudstorage.common.ServerResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.*;


public class FileProcessHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private FileProcess file;
    private Boolean inProcess = false;
    private long bytesProcessed;
    private RandomAccessFile randomAccessFile;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof FileProcess) {
            file = (FileProcess)evt;

            inProcess = true;
            bytesProcessed = 0;

            if (file.getProcessDirection().equals("UPLOAD")) {
                randomAccessFile = new RandomAccessFile(file.getFile(), "rw");

            } else if (file.getProcessDirection().equals("DOWNLOAD")) {
                randomAccessFile = new RandomAccessFile(file.getFile(), "r");
            }
            randomAccessFile.seek(0);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        if (!inProcess) { //Хэндлер первый в списке, если операция с файлом, то прокидываем дальше
            ctx.fireChannelRead(byteBuf.retain());

        } else {
            if (file.getProcessDirection().equals("UPLOAD")) {
                byte[] bytes = new byte[byteBuf.readableBytes()];
                byteBuf.readBytes(bytes);
                bytesProcessed += bytes.length;
                randomAccessFile.write(bytes);

                if (bytesProcessed == file.getSize()) { //Всё считали, заканчиваем
                    randomAccessFile.close();
                    file = null;
                    inProcess = false;

                    sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILE_UPLOAD_SUCCESS));
                }
            }
            if (file.getProcessDirection().equals("DOWNLOAD")) {
                byte[] buffer = new byte[16*1024];
                int read = randomAccessFile.read(buffer);
                ByteBuf bb = ctx.alloc().heapBuffer();
                bb.writeBytes(buffer, 0, read);
                bytesProcessed += read;
                ctx.channel().writeAndFlush(bb);

                if (bytesProcessed == file.getSize()) { //Всё считали, заканчиваем
                    randomAccessFile.close();
                    file = null;
                    inProcess = false;

                    sendResponse(ctx, new ServerResponse<>(ResponseCommand.FILE_DOWNLOAD_SUCCESS));
                }
            }
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

