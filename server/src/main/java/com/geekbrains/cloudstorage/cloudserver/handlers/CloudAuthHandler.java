package com.geekbrains.cloudstorage.cloudserver.handlers;

import com.geekbrains.cloudstorage.cloudserver.CloudUserCommand;
import com.geekbrains.cloudstorage.cloudserver.ServerCommand;
import com.geekbrains.cloudstorage.common.CloudUser;
import com.geekbrains.cloudstorage.common.ResponseCommand;
import com.geekbrains.cloudstorage.common.ServerResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.sql.*;
/**
 * Обработчик для аутентификации и регистрации пользователя.
 * Реализованы методы аутентификации, регистрации и отключения. При успешной регистрации происходит
 * автоматическая аутентификация. Если пользователь подключен, то к команде, пришедшей с обработчика
 * CommandValidateHandler добавляется информация о пользователе и она далее отправляется в следующий
 * обработчик CloudStorageHandler.
 * */
public class CloudAuthHandler extends SimpleChannelInboundHandler<ServerCommand> {

    private Boolean user_authenticated = false;
    private CloudUser cloudUser;

    private Connection connection;
    private PreparedStatement preparedStatement;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ServerCommand s) throws Exception {

        if ("auth".equals(s.getName())) {
            if (authenticateUser( s.getParams().get(0), s.getParams().get(1) ) ) {
                sendResponse(ctx, new ServerResponse<>(ResponseCommand.AUTH_OK, cloudUser));
                System.out.println("USER_AUTHENTICATED\r\n");
            } else {
                sendResponse(ctx, new ServerResponse<>(ResponseCommand.AUTH_FAIL));
                System.out.println("INCORRECT_LOGIN_OR_PASSWORD\r\n");
            }

        } else if ("reg".equals( s.getName() ) ){
            if (registerUser(s.getParams().get(0), s.getParams().get(1))) {
                cloudUser = getUser(s.getParams().get(0), s.getParams().get(1));
                user_authenticated = true;
                sendResponse(ctx, new ServerResponse<>(ResponseCommand.REG_OK, cloudUser));
                System.out.println("USER_REGISTERED\r\n");
            } else {
                user_authenticated = false;
                sendResponse(ctx, new ServerResponse<>(ResponseCommand.REG_FAIL));
                System.out.println("REGISTRATION_ERROR\r\n");
            }


        } else if ("disconnect".equals( s.getName() ) ){
            cloudUser = null;
            user_authenticated = false;
            sendResponse(ctx, new ServerResponse<>(ResponseCommand.AUTH_OUT));
            System.out.println("USER_DISCONNECTED\r\n");

        } else {
            if (user_authenticated) {
                ctx.fireChannelRead(new CloudUserCommand(cloudUser, s));
            } else {
                sendResponse(ctx, new ServerResponse<>(ResponseCommand.AUTH_REQUIRED, null));
            }
        }
    }

    /**
     * Аутентификация пользователя
     * */
    private Boolean authenticateUser(String login, String password) {
        cloudUser = getUser(login, password);
        if (cloudUser != null) {
            user_authenticated = true;
        } else {
            user_authenticated = false;
        }
        return user_authenticated;
    }

    /**
     * Получение информации о пользователе из БД по логину и паролю
     * */
    private CloudUser getUser(String login, String password) {
        try {
            connect();
            if (!connection.isClosed()) {
                preparedStatement = connection.prepareStatement("SELECT login, password FROM users WHERE login= ? and password=?;");
                preparedStatement.setString(1, login);
                preparedStatement.setString(2, password);
                ResultSet rs = preparedStatement.executeQuery();
                while (rs.next()) {
                    return new CloudUser(
                                          rs.getString("login"),
                                          rs.getString("password"),
                                          rs.getString("login")
                                        );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Регистрация пользователя. Новый пользователь прописывается в БД. Логин должен быть уникальным.
     * */
    private Boolean registerUser(String login, String password) {
        try {
            connect();
            if (!connection.isClosed()) {
                preparedStatement = connection.prepareStatement("SELECT count(id) as usrs FROM users WHERE login= ?;");
                preparedStatement.setString(1, login);

                int userFound = 0;
                ResultSet rs = preparedStatement.executeQuery();
                while (rs.next()) {
                    userFound = rs.getInt("usrs");
                }
                if (userFound > 0) {
                    return false;
                } else {
                    preparedStatement = connection.prepareStatement("INSERT INTO users (login, password) values (?, ?);");
                    preparedStatement.setString(1, login);
                    preparedStatement.setString(2, password);
                    if (preparedStatement.executeUpdate() > 0) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    /**
     * Подключение к БД с пользователями
     * */
    private void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:server/storage/config/cloudUsers.db");
    }

    /**
     * Отключение от БД
     * */
    private void disconnect() throws SQLException {
        preparedStatement.close();
        connection.close();
    }

    /**
     * Ответ сервера клиенту.
     * */
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
