package com.kirikomp.client;


import com.kirikomp.common.AuthResult;
import com.kirikomp.common.DataPackage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.kirikomp.client.UIutils.showError;
import static com.kirikomp.client.UIutils.updateUI;
import static com.kirikomp.common.AuthResult.Result.OK;
import static java.lang.System.exit;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.stream.Collectors.toList;


public class Controller
        implements Initializable {

    private NetConnection conn;
    private Window parentWin;
    private ServerResponseHandler responseHandler;
    private FileSender fileSender;
    private ExecutorService exec;
    private Runnable authHandler;

    private static final int THREAD_MAX_COUNT = 2;

    @FXML
    VBox rootElem;

    @FXML
    VBox vboxLoginPanel;
    @FXML
    TextField login;
    @FXML
    PasswordField password;

    @FXML
    HBox HboxConnect;
    @FXML
    Button btnConn;
    @FXML
    Button btnDisconn;
    @FXML
    Button btnExit;

    @FXML
    HBox HboxFiles;

    @FXML
    VBox vboxLocalFiles;
    @FXML
    Button btnSendFiles;
    @FXML
    Button btnDeleteLocalFiles;
    @FXML
    ListView<String> lstLocalFiles;


    @FXML
    VBox vboxServerFiles;
    @FXML
    Button btnDownloadFiles;
    @FXML
    Button btnDeleteFilesInCloud;
    @FXML
    ListView<String> lstFilesInCloud;

    //Запрос аутентификации
    public void auth(ActionEvent event) {
        try {
            conn.auth(login.getText(), password.getText());
        } catch (NetConnection.SendDataException e) {
            showError(e.getCause().getMessage());
        }
    }

    //Приватный класс авторизации Runnable
    private class AuthHandler
            implements Runnable {

        @Override
        public void run() {
            try {
                while (!currentThread().isInterrupted()) {
                    DataPackage response = conn.getResponseFromServer();

                    if (response instanceof AuthResult) {
                        AuthResult ar = (AuthResult) response;
                        if (ar.getResult() == OK)
                            break;

                        showError("Ошибка авторизации");
                    }
                }
                initMainPanel();
            } catch (NetConnection.ServerResponseException e) {
                e.printStackTrace();
            }
        }

    }

    //Первичная инициализация с панелью авторизации
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        conn = ClientApp.getNetConnection();

        HboxConnect.setVisible(false);
        HboxConnect.setManaged(false);

        HboxFiles.setVisible(false);
        HboxFiles.setManaged(false);

        authHandler = new Controller.AuthHandler();
        exec = Executors.newSingleThreadExecutor();
        exec.execute(authHandler); // Запускаем обработчик авторизации в отдельном потоке
    }

    //Инициализация панелей работы с файлами после успешной авторизации
    private void initMainPanel() {
        //Скрываем панель авторизации
        vboxLoginPanel.setVisible(false);
        vboxLoginPanel.setManaged(false);
        //Отображаем панели работы с файлами
        HboxConnect.setVisible(true);
        HboxConnect.setManaged(true);

        HboxFiles.setVisible(true);
        HboxFiles.setManaged(true);

        //Инициализируем обработчик ответов сервера
        responseHandler = new ServerResponseHandler();
        //Вешаем callback's на обновление списков файлов в результате действий с файлами
        responseHandler.setFileListToServerActionUI(this::updateFileListFromCloud);
        responseHandler.setFileListToLocalActionUI(this::updateLocalFileList);

        //Инициализируем обработчик отправки файлов
        fileSender = new FileSender();
        exec = newFixedThreadPool(THREAD_MAX_COUNT);
        //Запускаем потоки на отправку и получение
        exec.execute(responseHandler);
        exec.execute(fileSender);
        
        //Скрываем кнопку "Подключится"
        btnConn.managedProperty().bind(btnConn.visibleProperty());
        btnDisconn.managedProperty().bind(btnDisconn.visibleProperty());
        btnConn.setVisible(false);
        btnConn.setDisable(true);

        //Обновляем списки файлов
        updateLocalFileList();
        getFileListFromCloud();
    }

    // Обработчик кнопки "Подключиться"
    public void connectToCloud(ActionEvent event) {
        try {
            conn.open();
            exec.execute(responseHandler);
            exec.execute(fileSender);
            setButtonsState(true);
        } catch (IOException e) {
            showError("Не удалось подключиться");
        }
    }

    // Обработчик кнопки "Отключиться"
    public void disconnectFromCloud(ActionEvent event) {
        try {
            conn.close();
            setButtonsState(false);
        } catch (IOException e) {
            showError("Ошибка обрыва связи");
        }
    }

    //Обработчик кнопки "Выход"
    public void exitFromApp(ActionEvent event) {
        exit(0);
    }

    //Изменение состояния (видимости) кнопок в зависимости от состояния подключения
    private void setButtonsState(boolean connected) {
        btnConn.setVisible(!connected);
        btnConn.setDisable(connected);
        btnDisconn.setDisable(!connected);
        btnDisconn.setVisible(connected);
//
        btnSendFiles.setDisable(!connected);
        btnDownloadFiles.setDisable(!connected);
        btnDeleteFilesInCloud.setDisable(!connected);
    }

    //Обработчик кнопки "Отправить"
    public void sendFiles(ActionEvent event) {
        if (parentWin == null) parentWin = rootElem.getScene().getWindow();

        FileChooser fc = new FileChooser();
        List<File> files = fc.showOpenMultipleDialog(parentWin);

        if (files == null || files.isEmpty())
            return;

        fileSender.addFilesToQueue(files);
    }

    //Обработчик нажатия кнопки "Удалить файлы" (в локальном репозитории)
    public void deleteLocalFiles(ActionEvent event) {
        try {
            MultipleSelectionModel<String> model = lstLocalFiles.getSelectionModel();
            List<String> items = model.getSelectedItems();

            for (String fn : items) {
                Path path = Paths.get(ConfigSingleton.getInstance().STORAGE_DIR + "/" + fn);
                Files.delete(path);
                updateLocalFileList();
            }
        } catch (IOException e) {
            showError(e);
        }
    }

    //Обработчик нажатия кнопки "Скачать файлы" (из сервера в локальный репозиторий)
    public void downloadFiles(ActionEvent event) {
        try {
            MultipleSelectionModel<String> model = lstFilesInCloud.getSelectionModel();
            List<String> items = model.getSelectedItems();
            conn.sendDownloadFilesCommand(items);
        } catch (NetConnection.SendDataException e) {
            showError(e);
        }
    }

    //Обработчик нажатия кнопки "Удалить файлы" (на сервере)
    public void deleteFilesInCloud(ActionEvent event) {
        try {
            MultipleSelectionModel<String> model = lstFilesInCloud.getSelectionModel();
            List<String> items = model.getSelectedItems();
            conn.sendDeleteFilesCommand(items);
        } catch (NetConnection.SendDataException e) {
            showError(e);
        }
    }

    //Получение списка файлов на сервере
    public void getFileListFromCloud() {
        try {
            conn.sendFileListCommand();
        } catch (NetConnection.SendDataException e) {
            showError(e);
        }
    }

    //Обновление списка файлов на сервере
    public void updateFileListFromCloud(List<String> filenames) {
        updateUI(() ->
        {
            List<String> items = lstFilesInCloud.getItems();
            items.clear();
            items.addAll(filenames);
        });
    }

    //Обновление списка файлов в локальном репозитории
    public void updateLocalFileList() {
        updateUI(() ->
        {
            try {
                List<String> fnames = Files.list(Paths.get(ConfigSingleton.getInstance().STORAGE_DIR))
                        .map(x -> x.getFileName().toString())
                        .collect(toList());

                List<String> items = lstLocalFiles.getItems();
                items.clear();
                items.addAll(fnames);
            } catch (IOException e) {
                showError(e);
            }
        });
    }
}