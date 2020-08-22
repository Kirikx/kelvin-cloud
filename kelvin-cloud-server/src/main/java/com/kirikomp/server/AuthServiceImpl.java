package com.kirikomp.server;


import javax.sql.DataSource;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class AuthServiceImpl implements AuthService {

    private final Connection dbConn;
    private static final String SQL = "SELECT * FROM users WHERE login = ? and password = ?";

    /**
     * Инициализация подключения к DB
     */
    public AuthServiceImpl(DataSource dataSource) throws SQLException {
        this.dbConn = dataSource.getConnection();
    }

    /**
     * Метод авторизации пользователя
     *
     * @param login    Login
     * @param password Password
     * @return Boolean status
     * @throws SQLException
     */
    @Override
    public boolean authorize(String login, String password)
            throws SQLException {
        PreparedStatement ps = dbConn.prepareStatement(SQL);
        ps.setString(1, login);
        ps.setString(2, passToHash(password));

        ResultSet rs = ps.executeQuery();

        return rs.next();
    }

    /**
     * Метод преобразования пароля в Hash
     *
     * @param password Password String
     * @return Password String hash
     */
    private String passToHash(String password) {
        StringBuffer code = new StringBuffer();
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] bytes = password.getBytes();
            byte[] digest = messageDigest.digest(bytes);
            for (byte b : digest) {
                code.append(Integer.toHexString(0x0100 + (b & 0x00FF)).substring(1));
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Exception NoSuchAlgorithmException!");
        }
        return code.toString();
    }
}