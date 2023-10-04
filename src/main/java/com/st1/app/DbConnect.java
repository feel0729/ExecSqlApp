package com.st1.app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DbConnect {
    private static final Logger logger = LogManager.getLogger();
    private static final String myJdbcDriver = "oracle.jdbc.driver.OracleDriver";
    private static String dbName;
    private static String ip;
    private static String port;
    private static String sid;
    private static String user;
    private static String password;
    private static String timeout;

    public static Connection getConnection() {
        logger.trace("DbConnect getConnection ...");
        logger.trace("DbConnect getConnection dbName = " + dbName);
        try {
            Class.forName(myJdbcDriver);
        } catch (ClassNotFoundException e) {
            logger.error("DbConnect Class.forName error = " + e.getMessage());
        } // register OracleDriver class
        String url = "jdbc:oracle:thin:@" + ip + ":" + port + ":" + sid;
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            logger.error("DbConnect DriverManager.getConnection error = " + e.getMessage());
        }
        return null;
    }

    public static boolean hasConnection() {
        logger.trace("DbConnect hasConnection ...");
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        conn = getConnection();
        try {
            ps = conn.prepareStatement("select 1 as num from dual");
        } catch (SQLException e) {
            logger.error("DbConnect hasConnection error = " + e.getMessage());
        }
        try {
            rs = ps.executeQuery();
        } catch (SQLException e) {
            logger.error("DbConnect hasConnection error = " + e.getMessage());
        }
        try {
            if (rs.next()) {
                int num = rs.getInt("num");
                return num == 1;
            }
            if (rs != null) {
                rs.close();
            }
            if (ps != null) {
                ps.close();
            }
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            logger.error("DbConnect hasConnection error = " + e.getMessage());
        }
        return false;
    }

    public static void settingDbProperty(Map<String, String> dbProperty) {
        logger.trace("DbConnect settingDbProperty ...");
        dbName = dbProperty.get("name");
        ip = dbProperty.get("ip");
        port = dbProperty.get("port");
        sid = dbProperty.get("sid");
        user = dbProperty.get("user");
        password = dbProperty.get("password");
        timeout = dbProperty.get("timeout");
    }

    public static String getTimeout() {
        logger.trace("DbConnect getTimeout ...");
        return timeout;
    }
}
