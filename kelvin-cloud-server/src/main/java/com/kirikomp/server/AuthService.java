package com.kirikomp.server;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.sql.DriverManager.getConnection;


public class AuthService {

    private static final Connection dbConn;
    private static final String JDBC_DRIVER = "org.sqlite.JDBC";
    private static final String DB_URL = "jdbc:sqlite:kelvindb.db";
    private static final String sql = "SELECT * FROM users WHERE login = ? and password = ?";


    static {
        try {
            Class.forName(JDBC_DRIVER);
            dbConn = getConnection(DB_URL);
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public boolean autorize(String login, String password)
            throws SQLException{
        PreparedStatement ps = dbConn.prepareStatement(sql);

        ps.setString(1, login);
        ps.setString(2, passToHash(password));

        ResultSet rs = ps.executeQuery();

        if (rs.next())
            return true;

        return false;
    }

    private String passToHash(String password) {
        StringBuffer code = new StringBuffer(); //the hash code
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte bytes[] = password.getBytes();
            byte digest[] = messageDigest.digest(bytes); //create code
            for (int i = 0; i < digest.length; ++i) {
                code.append(Integer.toHexString(0x0100 + (digest[i] & 0x00FF)).substring(1));
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Exception NoSuchAlgorithmException!");
        }
        return code.toString();
    }

}