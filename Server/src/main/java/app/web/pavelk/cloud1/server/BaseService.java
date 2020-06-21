package app.web.pavelk.cloud1.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;

public class BaseService {
    private static Connection connection;
    private static Statement stmt;
    static final Logger rootLogger = LogManager.getRootLogger();

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:Server/cloud1.db");
            stmt = connection.createStatement();
            rootLogger.info("Base connect");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void disconnect() {
        try {
            connection.close();
            rootLogger.info("Base disconnect");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean autRequestBase(String login, String pass) {
        try {
            rootLogger.info("Base requst " + login + " / " + pass);
            ResultSet rs = stmt.executeQuery("SELECT nickname, password FROM main WHERE login = '" + login + "'");
            int myHash = pass.hashCode();
            if (rs.next()) {
                int dbHash = rs.getInt(2);
                if (myHash == dbHash) {
                    rootLogger.info("Base authorization ok");
                    return true;
                } else {
                    rootLogger.info("Base authorization incorrect password");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }
}
