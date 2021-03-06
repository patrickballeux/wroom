/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webroom.gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.schwering.irc.lib.IRCConfig;
import org.schwering.irc.lib.IRCConfigBuilder;
import org.schwering.irc.lib.IRCConnection;
import org.schwering.irc.lib.IRCConnectionFactory;
import org.schwering.irc.lib.IRCEventAdapter;
import org.schwering.irc.lib.IRCUser;
import webroom.WebRoom;
import webroom.engine.Message;
import webroom.engine.RendererFX;
import webroom.engine.RoomFile;
import webroom.engine.Sprite;
import webroom.engine.Texture;

/**
 *
 * @author patri
 */
public class WebRoomFX extends Application implements Message {

    private BorderPane panel;
    private TextField txtURL;
    private RendererFX renderer;
    private ToolBar topPanel;
    private VBox bottomPanel;
    private RoomFile currentFile;
    private Button btnOpenFile;
    private Stage stage;
    private StackPane root;
    private Label lblMessage;
    private java.util.prefs.Preferences preferences;
    private IRCConnection irc;
    private String chatroom;
    private String chatHost;
    private int chatPort;
    private boolean requestingUserList = false;
    private ArrayList<Sprite> userSprites = new ArrayList<>();
    private Texture userImage;
    private TextField txtChat;
    private Clip bgSound = null;
    private ObservableList<String> itemHistories;
    private ListView<String> listHistories;
    private String userAvatar;

    @Override
    public void start(Stage primaryStage) throws KeyManagementException, NoSuchAlgorithmException {
        preferences = java.util.prefs.Preferences.userNodeForPackage(this.getClass());
        userAvatar = preferences.get("useravatarurl", "");
        userImage = new Texture(getClass().getResource("/webroom/engine/user.png"));
        panel = new BorderPane();

        // Top Panel
        Button btnBrowse = new Button("Go");
        btnBrowse.setOnAction((event) -> {
            loadRoom();
        });
        txtURL = new TextField("http://wroom.crombz.com");
        txtURL.setPrefWidth(400);
        txtURL.setOnKeyPressed((event) -> {
            if (event.getCode() == KeyCode.ENTER) {
                loadRoom();
                event.consume();
            }
        });

        listHistories = new javafx.scene.control.ListView<>();
        listHistories.setStyle("-fx-font-size:16 px;-fx-border: 25px 25px 25px 25px; -fx-border-color: red;");
        itemHistories = FXCollections.observableArrayList("http://wroom.crombz.com");
        listHistories.setItems(itemHistories);
        listHistories.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        listHistories.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            txtURL.setText(newValue);
            root.getChildren().remove(listHistories);
        });
        listHistories.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue && !newValue) {
                root.getChildren().remove(listHistories);
            }
        });
        loatHistories();

        btnOpenFile = new Button("...");
        btnOpenFile.setOnAction((event) -> {
            if (false && WebRoom.NOCHAT) {
                javafx.stage.FileChooser chooser = new FileChooser();
                chooser.setInitialDirectory(new File("."));
                chooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("WRM files", "wrm"));
                File f = chooser.showOpenDialog(primaryStage);
                if (f != null) {
                    try {
                        txtURL.setText(f.toURI().toURL().toString());
                    } catch (MalformedURLException ex) {
                        Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    loadRoom();
                }
            } else {
                //show history...
                root.getChildren().add(listHistories);
                listHistories.toFront();
                listHistories.setMaxWidth(400);
                listHistories.setMaxHeight(400);
                listHistories.requestFocus();
            }
        });

        topPanel = new ToolBar(btnBrowse, txtURL, btnOpenFile);

        lblMessage = new Label("Welcome!");
        lblMessage.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, FontPosture.REGULAR, 12));
        lblMessage.setTextFill(Color.RED);
        lblMessage.setStyle("-fx-padding:10;-fx-background-color: #ffffff;-fx-border-radius: 10 10 0 0;-fx-background-radius: 10 10 0 0;");
        lblMessage.setAlignment(Pos.CENTER);
        lblMessage.setOpacity(0.9);
        txtChat = new TextField();
        txtChat.setDisable(false);
        txtChat.setOpacity(0.8);
        txtChat.setOnKeyPressed((event) -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (txtChat.getText().startsWith("/avatar")) {
                    userAvatar = txtChat.getText().replaceFirst("/avatar ", "");
                    preferences.put("avatar", userAvatar);
                } else if (txtChat.getText().startsWith("/nick")) {
                    String nick = txtChat.getText().replaceFirst("/nick ", "");
                    preferences.put("nick", nick);
                }
                if (irc != null) {
                    irc.doPrivmsg(chatroom, txtChat.getText().trim());
                    lblMessage.setText("Me: " + txtChat.getText().trim());
                    txtChat.setText("");
                    renderer.requestFocus();
                }
            }
        });

        bottomPanel = new VBox(0, lblMessage, txtChat);

        root = new StackPane();
        root.getChildren().add(panel);
        Scene scene = new Scene(root, 800, 512 + 80);
        stage = primaryStage;
        primaryStage.setMinHeight(512 + 40);
        primaryStage.setMinWidth(800);
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("logo.png")));
        primaryStage.setTitle("WRoom " + WebRoom.VERSION);
        panel.setTop(topPanel);
        renderer = new RendererFX();
        loadRoom();
        panel.setCenter(renderer);
        panel.setBottom(bottomPanel);
        topPanel.toFront();
        renderer.toBack();;
        primaryStage.setScene(scene);
        primaryStage.widthProperty().addListener((observable) -> {
            double scaleX = primaryStage.widthProperty().doubleValue() / renderer.widthProperty().doubleValue();
            double scaleY = (primaryStage.heightProperty().doubleValue() + 50) / renderer.heightProperty().doubleValue();;
            if (scaleX >= scaleY) {
                renderer.setScaleX(scaleX);
                renderer.setScaleY(scaleX);
            } else {
                renderer.setScaleX(scaleY);
                renderer.setScaleY(scaleY);
            }
            topPanel.autosize();
            topPanel.setLayoutX((primaryStage.widthProperty().doubleValue() - topPanel.widthProperty().doubleValue()) / 2);
        });
        primaryStage.heightProperty().addListener((observable) -> {
            double scaleX = primaryStage.widthProperty().doubleValue() / renderer.widthProperty().doubleValue();
            double scaleY = (primaryStage.heightProperty().doubleValue() + 50) / renderer.heightProperty().doubleValue();
            if (scaleX >= scaleY) {
                renderer.setScaleX(scaleX);
                renderer.setScaleY(scaleX);
            } else {
                renderer.setScaleX(scaleY);
                renderer.setScaleY(scaleY);
            }

        });
        primaryStage.show();
        primaryStage.setOnCloseRequest((event) -> {
            saveHistories();
            if (renderer != null) {
                renderer.stop();
            }
            if (irc != null) {
                irc.close();
                irc = null;
            }
        });
        renderer.requestFocus();
    }

    private void loadRoom() {
        try {
            RoomFile file = new RoomFile(new URL(txtURL.getText()), this);
            if (itemHistories.size() > 30) {
                itemHistories.remove(itemHistories.size() - 1);
            }
            if (!itemHistories.contains(txtURL.getText())) {
                itemHistories.add(0, txtURL.getText());
            }
            if (irc != null) {
                irc.doPart(chatroom);
                if (!file.getChatHost().equals(chatHost)) {
                    irc.doQuit();
                    irc = null;
                }
            }
            currentFile = file;
            renderer.stop();
            renderer.start(file, this, userSprites);
            renderer.setScaleX(panel.widthProperty().doubleValue() / renderer.widthProperty().doubleValue());
            renderer.setScaleY(panel.widthProperty().doubleValue() / renderer.widthProperty().doubleValue());
            renderer.requestFocus();
            stage.setTitle("WRoom " + WebRoom.VERSION + " - " + file.getTitle());
            updateLabelMessage("Welcome to " + file.getTitle().trim());
            chatHost = file.getChatHost();
            chatroom = file.getChatroom();
            chatPort = file.getChatPort();
            userSprites.clear();

//            Sprite text = new Sprite();
//            text.id = "Robot";
//            text.x = 1.5;
//            text.y = 1.5;            
//            setUserSpriteMessage(text, "Hi to all", "Robot", "https://pbs.twimg.com/profile_images/512735871548665857/TcE8kqVn.jpeg");
//            userSprites.add(text);
            if (irc == null) {
                connectToIRC();
            } else {
                irc.doJoin(chatroom);
            }
            playBackgroundSound(file.getBackgroundSound());

        } catch (MalformedURLException ex) {
            lblMessage.setText("!!URL is invalid!!");
        } catch (IOException ex) {
            lblMessage.setText("!!Could not load map!!");
            System.err.println(ex.getMessage());
        } catch (Exception ex) {
            lblMessage.setText("!!Something went wrong!!");
            System.err.println(ex.getMessage());
        }
    }

    private void playBackgroundSound(URL url) {
        try {
            if (bgSound != null) {
                bgSound.stop();
                bgSound = null;
            }
            if (url != null) {
                bgSound = AudioSystem.getClip();
                bgSound.open(AudioSystem.getAudioInputStream(url));
                bgSound.start();
                bgSound.loop(Clip.LOOP_CONTINUOUSLY);
            }
        } catch (UnsupportedAudioFileException ex) {
            Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
        } catch (LineUnavailableException ex) {
            Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void setUserSpriteMessage(Sprite s, String msg, String nick, String imageURL) {
        String text;
        text = "<div style='text-align:center;color:white;font-size:24px;'>" + nick + "</div>";
        if (imageURL.length() > 0) {
            text += "<center><img src='" + imageURL + "' width=100 height=100 ></center";
        }
        if (msg.length() > 0) {
            text += "<center><font style='color:white;background-color:#111111;font-size:18px'>" + msg + "</font></center>";
        }
        s.texture = new Texture(userImage, text, 0);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length == 1) {
            if (args[0].equals("NOCHAT")) {
                WebRoom.NOCHAT = true;
            }
        }
        launch(args);
    }

    @Override
    public void OnAction(int x, int y) {
        String loc = x + "," + y;
        Texture mediaTexture = null;
        //Check if Texture has Media
        if (renderer.getMap()[y][x] > 0) {
            Texture t = renderer.getTextures().get(renderer.getMap()[y][x] - 1);
            if (t.hasMedia()) {
                mediaTexture = t;
            }
        }
        if (mediaTexture != null) {
            if (mediaTexture.isMediaPlaying()) {
                mediaTexture.stopMedia();
                playBackgroundSound(currentFile.getBackgroundSound());
            } else {
                mediaTexture.playMedia();
                playBackgroundSound(null);
            }
        }
        if (currentFile.getSounds().containsKey(loc)) {
            playSound(currentFile.getSounds().get(loc));
        }
        if (currentFile.getTeleports().containsKey(loc)) {
            txtURL.setText(currentFile.getTeleports().get(loc).base.toString());
            loadRoom();
            return;
        }
        if (currentFile.getNotifications().containsKey(loc)) {
            if (renderer != null) {
                updateLabelMessage(currentFile.getNotifications().get(loc));
            }
        }
        if (currentFile.getDownloads().containsKey(loc)) {
            try {
                URL dl = currentFile.getDownloads().get(loc);
                downloadFile(dl);
            } catch (IOException ex) {
                Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (currentFile.getMedias().containsKey(loc)) {
            //Find the sprite to play the video
            for (Sprite s : renderer.getSprites()) {
                if (x == (int) s.x && y == (int) s.y) {
                    if (s.texture.hasMedia()) {
                        if (s.texture.isMediaPlaying()) {
                            s.texture.stopMedia();
                            playBackgroundSound(currentFile.getBackgroundSound());
                        } else {
                            s.texture.playMedia();
                            playBackgroundSound(null);
                        }
                    }
                    break;
                }
            }
        }
        if (currentFile.getWebPages().containsKey(loc)) {
            URL url = currentFile.getWebPages().get(loc);
            if (renderer.isVisible()) {
                renderer.pause();
                playBackgroundSound(null);
                MyWebView web = new MyWebView(this, true);
                web.getEngine().load(url.toString());
                root.getChildren().add(web);
                web.toFront();
                web.getViewer().requestFocus();
                web.setScaleX(0.95);
                web.setScaleY(0.85);
            }
        }
        if (currentFile.getEmbeded().containsKey(loc)) {
            String content = currentFile.getEmbeded().get(loc);
            if (renderer.isVisible()) {
                renderer.pause();
                playBackgroundSound(null);
                MyWebView web = new MyWebView(this, false);
                if (content.startsWith("youtube=")) {
                    content = content.replaceFirst("youtube=", "");
                    content = "<iframe width=\"100%\" height=\"100%\" src='https://www.youtube.com/embed/" + content + "?autoplay=1' frameborder=\"0\" allowfullscreen></iframe>";
                } else if (content.startsWith("vidme=")) {
                    content = content.replaceFirst("vidme=", "");
                    content = "<iframe src=\"https://vid.me/e/" + content + "?stats=1&autoplay=1\" width=\"100%\" height=\"100%\" frameborder=\"0\" allowfullscreen webkitallowfullscreen mozallowfullscreen scrolling=\"no\"></iframe>";
                }
                String html = "<html><body>" + content + "</body></html>";
                web.getEngine().loadContent(html);
                root.getChildren().add(web);
                web.toFront();
                web.setScaleX(0.95);
                web.setScaleY(0.85);
                web.getViewer().requestFocus();
            }
        }
        if (currentFile.getDoors().containsKey(loc)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int oldTexture = renderer.getMap()[y][x];
                    System.out.println("old texture: " + oldTexture);
                    Texture t = renderer.getTextures().get(oldTexture - 1);
                    //Find to affect texture...
                    try {
                        renderer.getMap()[y][x] = 0;
                        Thread.sleep(2000);
                        while (x == (int) renderer.getCamera().yPos && y == (int) renderer.getCamera().xPos) {
                            Thread.sleep(500);
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    renderer.getMap()[y][x] = oldTexture;
                    System.out.println("Closing door...");
                }
            }).start();
        }

    }

    @Override
    public void OnTrigger(int x, int y) {
        if (irc != null && irc.isConnected()) {
            if (userAvatar.length() == 0) {
                irc.doPrivmsg(chatroom, "MOVING TO:" + x + "x" + y);
            } else {
                irc.doPrivmsg(chatroom, "MOVING TO:" + x + "x" + y + "x" + userAvatar);
            }
        }
    }

    @Override
    public void OnNotification(int x, int y, String msg) {
        if (renderer != null) {
            updateLabelMessage(msg);
        }
    }

    @Override
    public void OnError(int x, int y, String error) {
    }

    private void playSound(URL file) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Clip clip = AudioSystem.getClip();
                    AudioInputStream inputStream = AudioSystem.getAudioInputStream(file);
                    clip.open(inputStream);
                    clip.start();
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }
        }).start();
    }

    private void downloadFile(URL url) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream in = null;
                RandomAccessFile out = null;
                try {
                    File down = new File("Downloads");
                    if (!down.exists()) {
                        down.mkdir();
                    }
                    in = url.openStream();
                    String[] parts = url.getFile().split("/");
                    java.io.File file = new java.io.File(down, parts[parts.length - 1]);
                    out = new RandomAccessFile(file, "rw");
                    updateLabelMessage("Downloading " + file.getName());
                    byte[] buffer = new byte[65536 * 4];
                    int count = in.read(buffer);
                    while (count != -1) {
                        updateLabelMessage("Downloading " + file.getName());
                        count = in.read(buffer);
                        if (count != -1) {
                            out.write(buffer, 0, count);
                        }
                    }
                    updateLabelMessage("Download complete for " + file.getName());
                } catch (IOException ex) {
                    updateLabelMessage("!! Download error !!");
                    Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    try {
                        if (in != null) {
                            in.close();
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }).start();
    }

    private void connectToIRC() throws IOException, KeyManagementException, NoSuchAlgorithmException {
        if (WebRoom.NOCHAT) {
            return;
        }
        txtChat.setDisable(true);
        IRCConfig config;
        IRCConfigBuilder builder = IRCConfigBuilder.newBuilder();
        builder.host(chatHost);
        builder.port(chatPort);
        userSprites.clear();
        //builder.username(System.getProperty("user.name"));
        //builder.password("secret");
        String nick = preferences.get("nick", "Guest__" + System.currentTimeMillis());
        if (nick.length() == 0) {
            nick = "Guest__" + System.currentTimeMillis();
        }
        userAvatar = preferences.get("avatar", userAvatar);
        //builder.realname(nick);
        builder.nick(nick);
        config = builder.build();
        irc = IRCConnectionFactory.newConnection(config);
        irc.connect();

        irc.addIRCEventListener(new IRCEventAdapter() {
            @Override
            public void onJoin(String chan, IRCUser user) {
                super.onJoin(chan, user); //To change body of generated methods, choose Tools | Templates.
                if (userSprites.isEmpty()) {
                    requestingUserList = true;
                    irc.doNames(chatroom);
                    txtChat.setDisable(false);
                } else {
                    updateLabelMessage("# " + user + " has joinded...");
                    Sprite s = new Sprite();
                    s.x = 0;
                    s.y = 0;
                    setUserSpriteMessage(s, "", user.getNick(), "");
                    s.id = user.getNick();
                    userSprites.add(s);
                }
            }

            @Override
            public void onRegistered() {
                super.onRegistered(); //To change body of generated methods, choose Tools | Templates.
                updateLabelMessage("# Connected");
                irc.doJoin(chatroom);
            }

            @Override
            public void onPing(String ping) {
                super.onPing(ping); //To change body of generated methods, choose Tools | Templates.
                irc.doPong(ping);
            }

            @Override
            public void onTopic(String chan, IRCUser user, String topic) {
                super.onTopic(chan, user, topic); //To change body of generated methods, choose Tools | Templates.
                updateLabelMessage("# Topic is: " + topic);
            }

            @Override
            public void onReply(int num, String value, String msg) {
                super.onReply(num, value, msg); //To change body of generated methods, choose Tools | Templates.
                if (requestingUserList) {
                    if (msg.contains("/NAMES")) {
                        requestingUserList = false;
                        updateLabelMessage("Welcome to " + currentFile.getTitle().trim());
                    } else {
                        String[] users = msg.split(" ");
                        for (String s : users) {
                            Sprite sp = new Sprite();
                            sp.x = 0;
                            sp.y = 0;
                            setUserSpriteMessage(sp, "", s, "");
                            sp.id = s.replaceFirst("@", "");
                            userSprites.add(sp);
                            System.out.println("Added sprite for usre: " + s);
                        }
                    }
                } else {
                    //updateLabelMessage("# " + msg);
                }
            }

            @Override
            public void onNick(IRCUser user, String newNick) {
                super.onNick(user, newNick); //To change body of generated methods, choose Tools | Templates.
                updateLabelMessage("# Nick: " + user + " - " + newNick);
            }

            @Override
            public void onPart(String chan, IRCUser user, String msg) {
                super.onPart(chan, user, msg); //To change body of generated methods, choose Tools | Templates.
                updateLabelMessage("# Leaving: " + user + " - " + msg);
            }

            @Override
            public void onDisconnected() {
                super.onDisconnected(); //To change body of generated methods, choose Tools | Templates.
                updateLabelMessage("# Disconnected");
                irc = null;
            }

            @Override
            public void onNotice(String target, IRCUser user, String msg) {
                super.onNotice(target, user, msg);
                updateLabelMessage("# Notice: " + target + ">" + user + " - " + msg);
            }

            @Override
            public void onError(String msg) {
                super.onError(msg);
                updateLabelMessage("# ERROR: " + msg);
            }

            @Override
            public void onQuit(IRCUser user, String msg) {
                super.onQuit(user, msg); //To change body of generated methods, choose Tools | Templates.
                updateLabelMessage("# " + user + " has left - " + msg);
                Sprite s = null;
                for (Sprite u : userSprites) {
                    if (u.id.equals(user.getNick())) {
                        s = u;
                        break;
                    }
                }
                if (s != null) {
                    userSprites.remove(s);
                }
            }

            @Override
            public void onPrivmsg(String target, IRCUser user, String msg) {
                super.onPrivmsg(target, user, msg); //To change body of generated methods, choose Tools | Templates.
                if (msg.startsWith("MOVING TO:")) {
                    double x = Double.parseDouble(msg.split(":")[1].split("x")[0]) + 0.5;
                    double y = Double.parseDouble(msg.split(":")[1].split("x")[1]) + 0.5;
                    String url = "";
                    if (msg.split(":")[1].split("x").length == 3) {
                        url = msg.split(":")[1].split("x")[2].trim();
                    }
                    for (Sprite s : userSprites) {
                        if (s.id.equals(user.getNick())) {
                            s.imageURL = url;
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    double xDelta = (x - s.x) / 10;
                                    double yDelta = (y - s.y) / 10;
                                    for (int i = 0; i < 10; i++) {
                                        s.x += xDelta;
                                        s.y += yDelta;
                                        try {
                                            Thread.sleep(50);
                                        } catch (InterruptedException ex) {
                                            Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    }
                                    s.x = x;
                                    s.y = y;
                                }
                            }).start();
                            break;
                        }
                    }
                } else {
                    updateLabelMessage(user + "> " + msg);
                    for (Sprite s : userSprites) {
                        if (s.id.equals(user.getNick())) {
                            setUserSpriteMessage(s, msg, s.id, s.imageURL);
                            break;
                        }
                    }
                }
            }
        });
    }

    private void updateLabelMessage(String msg) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                lblMessage.setText(msg);
                lblMessage.layout();
                lblMessage.autosize();
                lblMessage.setLayoutX((stage.widthProperty().doubleValue() - lblMessage.widthProperty().doubleValue()) / 2);
            }
        });

    }

    @Override
    public void onChatRequest() {
        if (irc != null && irc.isConnected()) {
            lblMessage.setText("Chatting...");
            txtChat.requestFocus();
            txtChat.setText("");
        }
    }

    private void loatHistories() {
        RandomAccessFile raf = null;
        try {
            File file = new File("history.txt");
            if (file.exists()) {
                raf = new RandomAccessFile(file, "r");
                while (raf.getFilePointer() < raf.length()) {
                    itemHistories.add(raf.readLine());
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (raf != null) {
                    raf.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private void saveHistories() {
        RandomAccessFile raf = null;
        try {
            File file = new File("history.txt");
            raf = new RandomAccessFile(file, "rw");
            raf.setLength(0);
            for (Object h : itemHistories.toArray()) {
                raf.write((h.toString() + "\r\n").getBytes());
            }
            raf.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                raf.close();
            } catch (IOException ex) {
                Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void onCloseView(Pane pane) {
        MyWebView web = (MyWebView) pane;
        if (web != null) {
            web.getEngine().loadContent("<html><body><</body></html>");
            root.getChildren().remove(web);
            renderer.pause();
            renderer.requestFocus();
        }
    }
}
