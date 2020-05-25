package com.kirikomp.client;

public final class ConfigSingleton {

    private static ConfigSingleton instance;

    public String HOST;
    public int PORT;
    public int MAX_OBJ_SIZE;
    public String STORAGE_DIR;

    private ConfigSingleton() {
    }

    public static ConfigSingleton getInstance() {
        if (instance == null) {
            instance = new ConfigSingleton();
        }
        return instance;
    }

}