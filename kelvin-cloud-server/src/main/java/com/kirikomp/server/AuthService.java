package com.kirikomp.server;

import java.sql.SQLException;

public interface AuthService {
    boolean authorize(String login, String password) throws SQLException;
}
