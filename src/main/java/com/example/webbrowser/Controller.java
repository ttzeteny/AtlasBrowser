package com.example.webbrowser;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.stage.Stage;

import java.awt.*;
import java.util.prefs.Preferences;
import java.util.Optional;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import javafx.stage.FileChooser;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import java.io.File;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.kordamp.ikonli.javafx.FontIcon;
import javax.sound.sampled.Clip;

public class Controller {

    @FXML
    private TabPane tabPane;
    @FXML
    private TextField addressBar;
    @FXML
    private HBox toolbarHBox;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label zoomLabel;
    @FXML
    private Button themeButton;
    @FXML
    private HBox bookmarksHBox;
    @FXML
    private Button saveButton;
    private List<Bookmark> bookmarks = new ArrayList<>();
    private File bookmarksFile;
    private Tab newTabButton;
    private boolean isDarkMode;
    private static final String HOME_PAGE_KEY = "homePage";
    private static final String DEFAULT_HOME_PAGE = "https://www.google.com";
    private static final String DARK_MODE_KEY = "darkMode";

    public void initialize() {

        Preferences prefs = Preferences.userNodeForPackage(getClass());
        isDarkMode = prefs.getBoolean(DARK_MODE_KEY, false);

        Platform.runLater(() -> applyTheme());

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
           if (newTab == newTabButton) {
               createNewTab();
           } else {
               updateUIforTab(newTab);
           }
        });

        Platform.runLater(() -> {

            Node header = tabPane.lookup(".tab-header-area");

            if (header != null) {
                toolbarHBox.translateYProperty().bind(header.boundsInLocalProperty().map(b -> b.getHeight()));
                toolbarHBox.toFront();

                bookmarksHBox.translateYProperty().bind(Bindings.createDoubleBinding(
                        () -> header.getBoundsInLocal().getHeight() + toolbarHBox.getHeight(),
                        header.boundsInLocalProperty(), toolbarHBox.heightProperty()
                ));
                bookmarksHBox.toFront();

                progressBar.translateYProperty().bind(Bindings.createDoubleBinding(
                        () -> header.getBoundsInLocal().getHeight() + toolbarHBox.getHeight() + bookmarksHBox.getHeight(),
                        header.boundsInLocalProperty(), toolbarHBox.heightProperty(), bookmarksHBox.heightProperty(),
                        toolbarHBox.heightProperty()
                ));
                progressBar.toFront();
            }

            loadBookmarks();
        });

        newTabButton = new Tab("+");
        newTabButton.setClosable(false);
        tabPane.getTabs().add(newTabButton);
    }

    @FXML
    public void onAddressBarAction() {
        String url = addressBar.getText();
        WebEngine engine = getCurrentEngine();

        if (engine == null) {
            createNewTab();
            engine = getCurrentEngine();
        }
        loadPageInEngine(engine, url);
    }

    @FXML
    public void onBack() {
        WebEngine engine = getCurrentEngine();
        if (engine != null) {
            WebHistory history = engine.getHistory();
            if(history.getCurrentIndex() > 0) {
                history.go(-1);
            }
        }
    }

    @FXML
    public void onForward() {
        WebEngine engine = getCurrentEngine();
        if (engine != null) {
            WebHistory history = engine.getHistory();
            if(history.getCurrentIndex() < history.getEntries().size() - 1) {
                history.go(1);
            }
        }
    }

    @FXML
    public void onReload() {
        WebEngine currentEngine = getCurrentEngine();
        if (currentEngine != null) {
            currentEngine.reload();
        }
    }

    @FXML
    public void onHistory() {

        WebEngine engine = getCurrentEngine();
        if (engine == null) return;

        WebHistory history = engine.getHistory();
        ObservableList<WebHistory.Entry> entries = history.getEntries();

        ListView<WebHistory.Entry> listView = new ListView<>(entries);

        listView.setCellFactory(param -> new ListCell<WebHistory.Entry>() {
            @Override
            protected void updateItem(WebHistory.Entry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTitle() + " - " + item.getUrl());
                }
            }
        });

        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                WebHistory.Entry selectedEntry = listView.getSelectionModel().getSelectedItem();
                if (selectedEntry != null) {
                    loadPageInEngine(engine, selectedEntry.getUrl());
                }
            }
        });

        Stage historyStage = new Stage();
        historyStage.setTitle("History");

        Scene scene = new Scene(listView, 400, 400);
        historyStage.setScene(scene);
        historyStage.show();
    }

    @FXML
    public void onHome() {
        Preferences prefs = Preferences.userNodeForPackage(getClass());
        String homePage = prefs.get(HOME_PAGE_KEY, DEFAULT_HOME_PAGE);

        WebEngine engine = getCurrentEngine();
        if (engine != null) {
            loadPageInEngine(engine, homePage);
        }
    }

    @FXML
    public void onSettings() {
        Preferences prefs = Preferences.userNodeForPackage(getClass());
        String currentHome = prefs.get(HOME_PAGE_KEY, DEFAULT_HOME_PAGE);

        TextInputDialog dialog = new TextInputDialog(currentHome);
        dialog.setTitle("Settings");
        dialog.setHeaderText("Homepage setting");
        dialog.setContentText("Type url of the new homepage:");

        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            String newUrl = result.get();
            prefs.put(HOME_PAGE_KEY, newUrl);
        }
    }

    @FXML
    private void onSaveBookmark() {
        WebEngine engine = getCurrentEngine();

        if (engine != null) {
            String title = engine.getTitle();
            String url = engine.getLocation();

            if (title != null && url != null) {
                Bookmark bookmark = new Bookmark(title, url);
                bookmarks.add(bookmark);
                saveBookmarksToFile();
                refreshBookmarksBar();
            }
        }
    }

    private void refreshBookmarksBar() {
        bookmarksHBox.getChildren().clear();

        for (Bookmark bookmark : bookmarks) {
            Button btn = new Button(bookmark.getTitle());
                if (btn.getText().length() > 15) {
                    btn.setText(btn.getText().substring(0, 15) + "...");
                }
                FontIcon icon = new FontIcon("fas-bookmark");
                btn.setGraphic(icon);

                btn.setOnAction(e -> {
                    WebEngine engine = getCurrentEngine();
                    if (engine != null) {
                        loadPageInEngine(engine, bookmark.getUrl());
                    }
                });

                ContextMenu cm = new ContextMenu();
                MenuItem deleteItem = new MenuItem("delete");
                deleteItem.setOnAction(e -> {
                    bookmarks.remove(bookmark);
                    saveBookmarksToFile();
                    refreshBookmarksBar();
                });
                cm.getItems().add(deleteItem);
                btn.setContextMenu(cm);

                bookmarksHBox.getChildren().add(btn);
        }
    }

    private void saveBookmarksToFile() {
        try (Writer writer = new FileWriter(bookmarksFile)) {
            Gson gson = new Gson();
            gson.toJson(bookmarks, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadBookmarks() {
        File userDataDir = new File(System.getProperty("user.home"), ".atlasbrowser");
        if (!userDataDir.exists()) userDataDir.mkdirs();

        bookmarksFile = new File(userDataDir, "bookmarks.json");

        if (bookmarksFile.exists()) {
            try (Reader reader = new FileReader(bookmarksFile)) {
                Gson gson = new Gson();
                bookmarks = gson.fromJson(reader, new TypeToken<List<Bookmark>>(){}.getType());
                if (bookmarks == null) bookmarks = new ArrayList<>();

                refreshBookmarksBar();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void onToggleTheme() {
        isDarkMode = !isDarkMode;

        Preferences prefs = Preferences.userNodeForPackage(getClass());
        prefs.putBoolean(DARK_MODE_KEY, isDarkMode);

        applyTheme();
    }

    public void applyTheme() {
        if (addressBar.getScene() == null) return;

        ObservableList<String> stylesheets = addressBar.getScene().getStylesheets();
        stylesheets.clear();

        String cssFile = isDarkMode ? "dark_mode_style.css" : "style.css";

        java.net.URL url = getClass().getResource(cssFile);

        if (url == null) {
            System.out.println("Css file not found");
        } else {
            stylesheets.add(url.toExternalForm());
        }

        FontIcon icon = (FontIcon) themeButton.getGraphic();
        if (isDarkMode) {
            icon.setIconLiteral("fas-sun");
            themeButton.getTooltip().setText("Light mode");
        } else {
            icon.setIconLiteral("fas-moon");
            themeButton.getTooltip().setText("Dark mode");
        }
    }

    @FXML
    public void onZoomIn() {
        WebView webView = getCurrentWebView();
        webView.setZoom(webView.getZoom() + 0.1);
    }

    @FXML
    public void onZoomOut() {
        WebView webView = getCurrentWebView();
        if (webView.getZoom() > 0.2) {
            webView.setZoom(webView.getZoom() - 0.1);
        }
    }

    @FXML
    public void onNewTab() {
        createNewTab();
    }

    private boolean isFile(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.endsWith(".zip") ||
                lower.endsWith(".exe") ||
                lower.endsWith(".msi") ||
                lower.endsWith(".dmg") ||
                lower.endsWith(".rar") ||
                lower.endsWith(".7z");
    }

    private void downloadFile(String urlString) {

        try {
            URL url = new URL(urlString);

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save File");

            String fileName = urlString.substring(urlString.lastIndexOf('/') + 1);

            if (fileName.contains("?")) {
                fileName = fileName.substring(0, fileName.indexOf('?'));
            }
            fileChooser.setInitialFileName(fileName);

            File file = fileChooser.showSaveDialog(addressBar.getScene().getWindow());

            if (file != null) {
                Thread thread = new Thread(() -> {
                   try (BufferedInputStream in = new BufferedInputStream(url.openStream());
                        FileOutputStream fileOutputStream = new FileOutputStream(file)) {

                       byte dataBuffer[] = new byte[1024];
                       int bytesRead;
                       while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                           fileOutputStream.write(dataBuffer, 0, bytesRead);
                       }

                       System.out.println("Download done: " + file.getAbsolutePath());
                   } catch (IOException e) {
                       e.printStackTrace();
                   }
                });
                thread.setDaemon(true);
                thread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createContextMenu(WebView webView) {
        webView.setContextMenuEnabled(false);

        ContextMenu contextMenu = new ContextMenu();

        webView.setOnMousePressed(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                contextMenu.getItems().clear();

                WebEngine engine = getCurrentEngine();

                String script = "var x = document.elementFromPoint(" + event.getX() + ", " + event.getY() + ");" +
                                "if (x) { var link = x.closest('a'); if (link) { link.href; } else { ''; } } else { ''; }";

                Object linkUrlObj = engine.executeScript(script);
                String linkUrl = (linkUrlObj != null) ? linkUrlObj.toString() : "";

                Object selectedObj = engine.executeScript("window.getSelection().toString();");
                String selectedText = (selectedObj != null) ? selectedObj.toString() : "";

                if (!linkUrl.isEmpty()) {
                    MenuItem openNewTab = new MenuItem("Open in new tab");
                    openNewTab.setOnAction(actionEvent -> {
                        createNewTab();
                        loadPageInEngine(getCurrentEngine(), linkUrl);
                    });

                    MenuItem copyLink = new MenuItem("Copy link");
                    copyLink.setOnAction(actionEvent -> {
                        ClipboardContent clipboard = new ClipboardContent();
                        clipboard.putString(linkUrl);
                        Clipboard.getSystemClipboard().setContent(clipboard);
                    });

                    contextMenu.getItems().addAll(openNewTab, copyLink, new SeparatorMenuItem());
                }

                if (!selectedText.isEmpty()) {
                    MenuItem copyText = new MenuItem("Copy text");
                    copyText.setOnAction(actionEvent -> {
                        ClipboardContent clipboard = new ClipboardContent();
                        clipboard.putString(selectedText);
                        Clipboard.getSystemClipboard().setContent(clipboard);
                    });
                    contextMenu.getItems().addAll(copyText, new SeparatorMenuItem());
                }

                MenuItem reload = new MenuItem("Reload");
                reload.setOnAction(actionEvent -> engine.reload());

                MenuItem back = new MenuItem("Back");
                back.setOnAction(actionEvent -> onBack());

                contextMenu.getItems().addAll(reload, back);

                contextMenu.show(webView, event.getScreenX(), event.getScreenY());
            } else {
                contextMenu.hide();
            }
        });
    }

    private WebView getCurrentWebView() {
        Tab selectedTab =  tabPane.getSelectionModel().getSelectedItem();

        if (selectedTab != null && selectedTab.getContent() instanceof StackPane) {
            StackPane root = (StackPane) selectedTab.getContent();

            if (!root.getChildren().isEmpty() && root.getChildren().get(0) instanceof WebView) {
                return (WebView) root.getChildren().get(0);
            }
        }
        return null;
    }

    private WebEngine getCurrentEngine() {
        WebView webView = getCurrentWebView();
        return (webView != null) ? webView.getEngine() : null;
    }

    private void createNewTab() {

        Tab tab = new Tab("New Tab");
        WebView newWebView = new WebView();
        WebEngine newEngine = newWebView.getEngine();

        File userDataDir = new File(System.getProperty("user.home"), ".atlasbrowser/data");

        if (!userDataDir.exists()) {
            userDataDir.mkdirs();
        }

        newEngine.setUserDataDirectory(userDataDir);

        newEngine.locationProperty().addListener((obs, oldLocation, newLocation) -> {
            if (isFile(newLocation)) {
                downloadFile(newLocation);

                Platform.runLater(() -> {
                    if (newEngine.getHistory().getCurrentIndex() == 0) {
                        newEngine.load(null);
                    }
                });
                return;
            }
            if (tabPane.getSelectionModel().getSelectedItem() == tab) {
                addressBar.setText(newLocation);
            }
        });

        newEngine.titleProperty().addListener((obs, oldTitle, newTitle) -> {
            tab.setText(newTitle);
            if (tabPane.getSelectionModel().getSelectedItem() == tab) {
                updateWindowTitle(newTitle);
            }
        });

        newWebView.zoomProperty().addListener((obs, oldZoom, newZoom) -> {
            if (tabPane.getSelectionModel().getSelectedItem() == tab) {
                int percentage = (int)(newZoom.doubleValue() * 100);
                zoomLabel.setText(percentage + "%");
            }
        });

        createContextMenu(newWebView);

        StackPane contentRoot = new StackPane(newWebView);

        Platform.runLater(() -> {
            contentRoot.paddingProperty().bind(Bindings.createObjectBinding(() ->
                            new Insets(
                                    toolbarHBox.getHeight() + bookmarksHBox.getHeight(),
                                    0, 0, 0
                            ),
                    toolbarHBox.heightProperty(), bookmarksHBox.heightProperty()
            ));
        });

        tab.setContent(contentRoot);

        if (tabPane.getTabs().contains(newTabButton)) {
            int insertIndex = tabPane.getTabs().indexOf(newTabButton);
            tabPane.getTabs().add(insertIndex, tab);
        } else {
            tabPane.getTabs().add(tab);
        }

        tabPane.getSelectionModel().select(tab);

        Preferences prefs = Preferences.userNodeForPackage(getClass());
        String homePage = prefs.get(HOME_PAGE_KEY, DEFAULT_HOME_PAGE);
        newEngine.load(homePage);
    }

    private void updateUIforTab(Tab tab) {
        if (tab.getContent() instanceof StackPane) {
            StackPane root = (StackPane) tab.getContent();

            if (!root.getChildren().isEmpty() && root.getChildren().get(0) instanceof WebView) {
                WebView webView = (WebView) root.getChildren().get(0);
                WebEngine engine = webView.getEngine();

                addressBar.setText(engine.getLocation());
                updateWindowTitle(engine.getTitle());

                int percentage = (int)(webView.getZoom() * 100);
                zoomLabel.setText(percentage + "%");

                progressBar.progressProperty().unbind();
                progressBar.visibleProperty().unbind();

                progressBar.visibleProperty().bind(engine.getLoadWorker().runningProperty());
                progressBar.progressProperty().bind(engine.getLoadWorker().progressProperty());
            }
        }
    }

    private void updateWindowTitle(String title) {
        if (addressBar.getScene() != null && addressBar.getScene().getWindow() != null) {
            Stage stage = (Stage) addressBar.getScene().getWindow();
            stage.setTitle("Atlas - " + (title != null ? title : "New Tab"));
        }
    }

    private void loadPageInEngine(WebEngine engine, String input) {

        String url;

        if (input.startsWith("http://") || input.startsWith("https://")) {
            url = input;
        } else if (input.startsWith("www.")) {
            url = "https://" + input;
        } else if (input.contains(".") && !input.contains(" ")) {
            url = "https://" + input;
        } else {
            url = "https://www.google.com/search?q=" + input;
        }

        engine.load(url);
    }
}