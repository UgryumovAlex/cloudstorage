package com.geekbrains.cloudstorage.cloudserver.handlers;

import com.geekbrains.cloudstorage.cloudserver.CloudUser;
import com.geekbrains.cloudstorage.cloudserver.CloudUserCommand;
import com.geekbrains.cloudstorage.cloudserver.ServerCommand;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.sql.*;

public class CloudAuthHandler extends SimpleChannelInboundHandler<ServerCommand> {

    private Boolean user_authenticated = false;
    private CloudUser cloudUser;

    private Connection connection;
    private PreparedStatement preparedStatement;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ServerCommand s) throws Exception {
        if ("auth".equals(s.getName())) {
            if (authenticateUser( s.getParams().get(0), s.getParams().get(1) ) ) {
                ctx.writeAndFlush("USER_AUTHENTICATED");
            } else {
                ctx.writeAndFlush("INCORRECT_LOGIN_OR_PASSWORD");
            }

        } else if ("reg".equals( s.getName() ) ){
            if (registerUser(s.getParams().get(0), s.getParams().get(1), s.getParams().get(2))) {
                cloudUser = getUser(s.getParams().get(0), s.getParams().get(1));
                user_authenticated = true;
            }
            ctx.writeAndFlush("USER_REGISTERED");

        } else if ("disconnect".equals( s.getName() ) ){
            cloudUser = null;
            user_authenticated = false;
            ctx.writeAndFlush("USER_DISCONNECTED");

        } else {
            if (user_authenticated) {
                ctx.fireChannelRead(new CloudUserCommand(cloudUser, s));
            } else {
                ctx.writeAndFlush("YOU_NEED_TO_BE_AUTHENTICATED");
            }
        }
    }

    private Boolean authenticateUser(String login, String password) {
        cloudUser = getUser(login, password);
        if (cloudUser != null) {
            user_authenticated = true;
        }
        return user_authenticated;
    }

    private CloudUser getUser(String login, String password) {
        try {
            connect();
            if (!connection.isClosed()) {
                preparedStatement = connection.prepareStatement("SELECT login, password, nickname FROM users WHERE login= ? and password=?;");
                preparedStatement.setString(1, login);
                preparedStatement.setString(2, password);
                ResultSet rs = preparedStatement.executeQuery();
                while (rs.next()) {
                    return new CloudUser(
                                          rs.getString("login"),
                                          rs.getString("password"),
                                          rs.getString("nickname"),
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

    private Boolean registerUser(String login, String password, String nick) {
        try {
            connect();
            if (!connection.isClosed()) {
                preparedStatement = connection.prepareStatement("SELECT count(id) as usrs FROM users WHERE login= ? and nickname=?;");
                preparedStatement.setString(1, login);
                preparedStatement.setString(2, nick);

                int userFound = 0;
                ResultSet rs = preparedStatement.executeQuery();
                while (rs.next()) {
                    userFound = rs.getInt("usrs");
                }
                if (userFound > 0) {
                    return false;
                } else {
                    preparedStatement = connection.prepareStatement("INSERT INTO users (login, password, nickname) values (?, ?, ?);");
                    preparedStatement.setString(1, login);
                    preparedStatement.setString(2, password);
                    preparedStatement.setString(3, nick);
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

    private void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:server/storage/config/cloudUsers.db");
    }

    private void disconnect() throws SQLException {
        preparedStatement.close();
        connection.close();
    }
}
