/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webroom.gui;

import java.awt.SplashScreen;
import java.io.File;
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
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JComponent;
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
    private Button btnBackToMap;
    private Button btnOpenFile;
    private Stage stage;
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

    @Override
    public void start(Stage primaryStage) throws KeyManagementException, NoSuchAlgorithmException {
        preferences = java.util.prefs.Preferences.userNodeForPackage(this.getClass());
        userImage = new Texture(getClass().getResource("/webroom/engine/user.png"));
        try {
            panel = new BorderPane();

            // Top Panel
            Button btnBrowse = new Button("Go");
            btnBrowse.setOnAction((event) -> {
                try {
                    loadRoom();
                } catch (IOException ex) {
                    Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
                } catch (KeyManagementException ex) {
                    Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NoSuchAlgorithmException ex) {
                    Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            txtURL = new TextField("http://wroom.crombz.com");
            txtURL.setPrefWidth(400);
            btnBackToMap = new Button("<--");
            btnBackToMap.setDisable(true);
            btnBackToMap.setOnAction((event) -> {
                WebView web = (WebView) panel.getCenter();
                web.getEngine().loadContent("<html><body><</body></html>");
                panel.setCenter(renderer);
                btnBackToMap.setDisable(true);
                renderer.requestFocus();
                renderer.toBack();
            });

            btnOpenFile = new Button("...");
            btnOpenFile.setOnAction((event) -> {
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
                    try {
                        loadRoom();
                    } catch (IOException ex) {
                        Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (KeyManagementException ex) {
                        Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (NoSuchAlgorithmException ex) {
                        Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
            topPanel = new ToolBar(btnBrowse, txtURL, btnOpenFile, btnBackToMap);

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
                    if (irc != null) {
                        irc.doPrivmsg(chatroom, txtChat.getText().trim());
                        lblMessage.setText("Me: " + txtChat.getText().trim());
                        txtChat.setText("");
                        renderer.requestFocus();
                    }
                }
            });

            bottomPanel = new VBox(0, lblMessage, txtChat);

            StackPane root = new StackPane();
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
                if (renderer != null) {
                    renderer.stop();
                }
                if (irc != null) {
                    irc.close();
                    irc = null;
                }
            });

        } catch (MalformedURLException ex) {
            Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void loadRoom() throws MalformedURLException, IOException, KeyManagementException, NoSuchAlgorithmException {
        RoomFile file = new RoomFile(new URL(txtURL.getText()), this);
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
        if (irc == null) {
            connectToIRC();
        } else {
            irc.doJoin(chatroom);
        }
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
            } else {
                mediaTexture.playMedia();
            }
        }
        if (currentFile.getSounds().containsKey(loc)) {
            playSound(currentFile.getSounds().get(loc));
        }
        if (currentFile.getTeleports().containsKey(loc)) {
            txtURL.setText(currentFile.getTeleports().get(loc).base.toString());
            try {
                loadRoom();
            } catch (IOException ex) {
                Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
            } catch (KeyManagementException ex) {
                Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
            }
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
                        } else {
                            s.texture.playMedia();
                        }
                    }
                    break;
                }
            }
        }
        if (currentFile.getWebPages().containsKey(loc)) {
            URL url = currentFile.getWebPages().get(loc);
            if (renderer.isVisible()) {
                WebView web = new WebView();
                btnBackToMap.setDisable(false);
                web.getEngine().load(url.toString());
                web.setOnKeyPressed((event) -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        web.getEngine().loadContent("<html><body></body></html>");
                        panel.setCenter(renderer);
                        renderer.requestFocus();
                        renderer.toBack();
                        btnBackToMap.setDisable(true);
                    }
                });
                panel.setCenter(web);
                web.requestFocus();
                topPanel.toFront();
            }
        }
        if (currentFile.getEmbeded().containsKey(loc)) {
            String content = currentFile.getEmbeded().get(loc);
            if (renderer.isVisible()) {
                WebView web = new WebView();
                btnBackToMap.setDisable(false);
                if (content.startsWith("youtube=")) {
                    content = content.replaceFirst("youtube=", "");
                    content = "<iframe width=\"100%\" height=\"100%\" src='https://www.youtube.com/embed/" + content + "?autoplay=1' frameborder=\"0\" allowfullscreen></iframe>";
                } else if (content.startsWith("vidme=")) {
                    content = content.replaceFirst("vidme=", "");
                    content = "<iframe src=\"https://vid.me/e/" + content + "?stats=1&autoplay=1\" width=\"100%\" height=\"100%\" frameborder=\"0\" allowfullscreen webkitallowfullscreen mozallowfullscreen scrolling=\"no\"></iframe>";
                }
                String html = "<html><body>" + content + "</body></html>";
                web.getEngine().loadContent(html);
                web.setOnKeyPressed((event) -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        web.getEngine().loadContent("<html><body></body></html>");
                        panel.setCenter(renderer);
                        renderer.requestFocus();
                        renderer.toBack();
                        btnBackToMap.setDisable(true);
                    }
                });
                panel.setCenter(web);
                web.requestFocus();
                topPanel.toFront();
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
            irc.doPrivmsg(chatroom, "MOVING TO:" + x + "x" + y);
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

    @Override
    public void onCloseView(JComponent comp) {
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
                try {
                    File down = new File("Downloads");
                    if (!down.exists()) {
                        down.mkdir();
                    }
                    in = url.openStream();
                    String[] parts = url.getFile().split("/");
                    java.io.File file = new java.io.File(down, parts[parts.length - 1]);
                    RandomAccessFile out = new RandomAccessFile(file, "rw");
                    updateLabelMessage("Downloading " + file.getName());
                    byte[] buffer = new byte[65536];
                    int count = in.read(buffer);
                    while (count != -1) {
                        updateLabelMessage("Downloading " + file.getName() + "(" + count + " bytes)");
                        count = in.read(buffer);
                        if (count != -1) {
                            out.write(buffer, 0, count);
                        }
                    }
                    updateLabelMessage("Downloading completed for " + file.getName());
                    out.close();
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(WebRoomFX.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    try {
                        in.close();
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
                    s.texture = new Texture(userImage, "<div style='text-align:center;color:white;background-color:#111;'>" + user.getNick() + "</div>", 0);
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
                            sp.texture = new Texture(userImage, "<div style='text-align:center;color:white;background-color:#111;'>" + s + "</div>", 0);
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
                    for (Sprite s : userSprites) {
                        if (s.id.equals(user.getNick())) {
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
                            s.texture = new Texture(userImage, "<div style='font-size:12px;text-align:center;color:white;background-color:#111;'>" + user.getNick() + "<hr>" + msg + "</div>", 0);
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
}
