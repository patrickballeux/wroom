/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webroom.gui;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import org.schwering.irc.lib.IRCConfig;
import org.schwering.irc.lib.IRCConfigBuilder;
import org.schwering.irc.lib.IRCConnection;
import org.schwering.irc.lib.IRCConnectionFactory;
import org.schwering.irc.lib.IRCEventAdapter;
import org.schwering.irc.lib.IRCUser;
import webroom.WebRoom;
import webroom.engine.Message;
import webroom.engine.Renderer;
import webroom.engine.RoomFile;
import webroom.engine.Sprite;
import webroom.engine.Teleport;
import webroom.engine.Texture;

/**
 *
 * @author patri
 */
public class Browser extends javax.swing.JFrame implements Message {

    private Renderer renderer;
    private TreeMap<String, Teleport> teleports = null;
    private TreeMap<String, URL> sounds = null;
    private TreeMap<String, String> notifications = null;
    private Clip backgroundSound = null;
    private TreeMap<String, URL> downloads;
    private RendererStatus rendStatus;
    private TreeMap<String, URL> medias;
    private TreeMap<String, URL> webpages;
    private TreeMap<String, String> embeded;
    private java.util.prefs.Preferences preferences;
    private TreeMap<String, String> doors;
    private IRCConnection irc;
    private String chatroom;
    private String chatHost;
    private int chatPort;
    private DefaultListModel<String> userModel;
    private boolean requestingUserList = false;
    private ArrayList<Sprite> userSprites = new ArrayList<>();
    private Texture userImage;

    /**
     * Creates new form Browser
     */
    public Browser() {
        initComponents();
        this.setIconImage((new ImageIcon(this.getClass().getResource("logo.png")).getImage()));
        rendStatus = new RendererStatus();
        setTitle("WRoom (v" + WebRoom.VERSION + ")");
        preferences = java.util.prefs.Preferences.userNodeForPackage(this.getClass());
        cboURLs.insertItemAt(preferences.get("homepage", "http://wroom.crombz.com"), 0);
        cboURLs.setSelectedIndex(0);
        userModel = new DefaultListModel<>();
        lstUsers.setModel(userModel);
        txtChat.setEnabled(false);
        userImage = new Texture(getClass().getResource("/webroom/engine/user.png"));

        this.pack();
    }

    private void connectToIRC() throws IOException, KeyManagementException, NoSuchAlgorithmException {
        if (WebRoom.NOCHAT) {
            return;
        }
        txtChat.setEnabled(false);
        IRCConfig config;
        IRCConfigBuilder builder = IRCConfigBuilder.newBuilder();
        builder.host(chatHost);
        builder.port(chatPort);
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
                if (userModel.isEmpty()) {
                    requestingUserList = true;
                    irc.doNames(chatroom);
                    txtChat.setEnabled(true);
                } else {
                    renderer.addMessage("# " + user + " has joinded...");
                    userModel.addElement(user.toString());
                    lstUsers.setSelectedValue(user.toString(), true);
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
                renderer.addMessage("# Connected");
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
                renderer.addMessage("# Topic is: " + topic);
            }

            @Override
            public void onReply(int num, String value, String msg) {
                super.onReply(num, value, msg); //To change body of generated methods, choose Tools | Templates.
                if (requestingUserList) {
                    if (msg.contains("/NAMES")) {
                        requestingUserList = false;
                    } else {
                        String[] users = msg.split(" ");
                        for (String s : users) {
                            userModel.addElement(s);
                            Sprite sp = new Sprite();
                            sp.x = 0;
                            sp.y = 0;
                            sp.texture = new Texture(userImage, "<div style='text-align:center;color:white;background-color:#111;'>" + s + "</div>", 0);
                            sp.id = s.replaceFirst("@", "");
                            userSprites.add(sp);
                            System.out.println("Added sprite for usre: " + s);
                        }
                    }
                }
                renderer.addMessage("# " + msg);
            }

            @Override
            public void onNick(IRCUser user, String newNick) {
                super.onNick(user, newNick); //To change body of generated methods, choose Tools | Templates.
                renderer.addMessage("# Nick: " + user + " - " + newNick);
            }

            @Override
            public void onPart(String chan, IRCUser user, String msg) {
                super.onPart(chan, user, msg); //To change body of generated methods, choose Tools | Templates.
                renderer.addMessage("# Leaving: " + user + " - " + msg);
                userModel.removeElement(user.toString());
            }

            @Override
            public void onDisconnected() {
                super.onDisconnected(); //To change body of generated methods, choose Tools | Templates.
                renderer.addMessage("# Disconnected");
                irc = null;
            }

            @Override
            public void onNotice(String target, IRCUser user, String msg) {
                super.onNotice(target, user, msg); //To change body of generated methods, choose Tools | Templates.
                renderer.addMessage("# Notice: " + target + ">" + user + " - " + msg);
            }

            @Override
            public void onError(String msg) {
                super.onError(msg); //To change body of generated methods, choose Tools | Templates.
                renderer.addMessage("# ERROR: " + msg);
            }

            @Override
            public void onQuit(IRCUser user, String msg) {
                super.onQuit(user, msg); //To change body of generated methods, choose Tools | Templates.
                renderer.addMessage("# " + user + " has left - " + msg);
                userModel.removeElement(user.toString());
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
                    double x = Double.parseDouble(msg.split(":")[1].split("x")[0]);
                    double y = Double.parseDouble(msg.split(":")[1].split("x")[1]);
                    for (Sprite s : userSprites) {
                        if (s.id.equals(user.getNick())) {
                            s.x = x;
                            s.y = y;
                            break;
                        }
                    }
                } else {
                    renderer.addMessage(user + "> " + msg);
                    for (Sprite s : userSprites) {
                        if (s.id.equals(user.getNick())) {
                            s.texture = new Texture(userImage, "<div style='font-size:12px;text-align:center;color:white;background-color:#111;'>" + user.getNick() + "<hr>" + msg + "</div>", 0);
                            break;
                        }
                    }
                    lstUsers.setSelectedValue(user.toString(), true);
                }
            }

        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panViewer = new javax.swing.JPanel();
        panStatusBar = new javax.swing.JPanel();
        lblMessage = new javax.swing.JLabel();
        txtChat = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        panNavigation = new javax.swing.JPanel();
        lblURL = new javax.swing.JLabel();
        btnURLGo = new javax.swing.JButton();
        cboURLs = new javax.swing.JComboBox<>();
        scroller = new javax.swing.JScrollPane();
        lstUsers = new javax.swing.JList<>();
        mainMenu = new javax.swing.JMenuBar();
        mnuFiles = new javax.swing.JMenu();
        mnuFikesOpen = new javax.swing.JMenuItem();
        mnuEdit = new javax.swing.JMenu();
        mnuEditSetHomepage = new javax.swing.JMenuItem();
        mnuEditGoToHomepage = new javax.swing.JMenuItem();
        mnuEditSetNickName = new javax.swing.JMenuItem();
        mnuControl = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenuItem7 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("WRoom");
        setPreferredSize(new java.awt.Dimension(800, 600));
        setSize(new java.awt.Dimension(800, 600));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        panViewer.setBackground(new java.awt.Color(102, 102, 102));
        panViewer.setPreferredSize(new java.awt.Dimension(800, 600));
        panViewer.setLayout(new javax.swing.BoxLayout(panViewer, javax.swing.BoxLayout.PAGE_AXIS));

        panStatusBar.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        lblMessage.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblMessage.setText("Welcome to WRoom");

        txtChat.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtChatKeyPressed(evt);
            }
        });

        jLabel1.setText("Chat");

        javax.swing.GroupLayout panStatusBarLayout = new javax.swing.GroupLayout(panStatusBar);
        panStatusBar.setLayout(panStatusBarLayout);
        panStatusBarLayout.setHorizontalGroup(
            panStatusBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panStatusBarLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panStatusBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblMessage, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(panStatusBarLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtChat)))
                .addContainerGap())
        );
        panStatusBarLayout.setVerticalGroup(
            panStatusBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panStatusBarLayout.createSequentialGroup()
                .addGroup(panStatusBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtChat, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblMessage)
                .addContainerGap())
        );

        panNavigation.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        lblURL.setText("URL");

        btnURLGo.setText("Go");
        btnURLGo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnURLGoActionPerformed(evt);
            }
        });

        cboURLs.setEditable(true);
        cboURLs.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "http://wroom.crombz.com" }));
        cboURLs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cboURLsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panNavigationLayout = new javax.swing.GroupLayout(panNavigation);
        panNavigation.setLayout(panNavigationLayout);
        panNavigationLayout.setHorizontalGroup(
            panNavigationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panNavigationLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblURL)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cboURLs, 0, 646, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnURLGo)
                .addContainerGap())
        );
        panNavigationLayout.setVerticalGroup(
            panNavigationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panNavigationLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panNavigationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblURL)
                    .addComponent(btnURLGo)
                    .addComponent(cboURLs, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        scroller.setMaximumSize(new java.awt.Dimension(152, 32767));
        scroller.setMinimumSize(new java.awt.Dimension(152, 23));

        lstUsers.setBackground(new java.awt.Color(0, 0, 0));
        lstUsers.setFont(new java.awt.Font("Arial", 1, 11)); // NOI18N
        lstUsers.setForeground(new java.awt.Color(0, 255, 0));
        lstUsers.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        lstUsers.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lstUsers.setMaximumSize(new java.awt.Dimension(150, 9999));
        lstUsers.setMinimumSize(new java.awt.Dimension(150, 80));
        lstUsers.setPreferredSize(new java.awt.Dimension(150, 80));
        scroller.setViewportView(lstUsers);

        mnuFiles.setText("File");

        mnuFikesOpen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        mnuFikesOpen.setText("Open");
        mnuFikesOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuFikesOpenActionPerformed(evt);
            }
        });
        mnuFiles.add(mnuFikesOpen);

        mainMenu.add(mnuFiles);

        mnuEdit.setText("Edit");

        mnuEditSetHomepage.setText("Set as Homepage");
        mnuEditSetHomepage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditSetHomepageActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditSetHomepage);

        mnuEditGoToHomepage.setText("Go to Hopepage");
        mnuEditGoToHomepage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditGoToHomepageActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditGoToHomepage);

        mnuEditSetNickName.setText("Set Nickname");
        mnuEditSetNickName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuEditSetNickNameActionPerformed(evt);
            }
        });
        mnuEdit.add(mnuEditSetNickName);

        mainMenu.add(mnuEdit);

        mnuControl.setText("Control");

        jMenuItem1.setText("Move Foward - UP/W");
        mnuControl.add(jMenuItem1);

        jMenuItem2.setText("Move Bacward: DOWN/S");
        mnuControl.add(jMenuItem2);

        jMenuItem3.setText("Move Left: LEFT/A");
        mnuControl.add(jMenuItem3);

        jMenuItem4.setText("Move Right: RIGHT/D");
        mnuControl.add(jMenuItem4);

        jMenuItem5.setText("Run: SHIFT + Fowward/Backward");
        mnuControl.add(jMenuItem5);

        jMenuItem6.setText("Strafe: CTRL + Left/Right");
        mnuControl.add(jMenuItem6);

        jMenuItem7.setText("Action: SPACE/Mouse Double-click");
        mnuControl.add(jMenuItem7);

        mainMenu.add(mnuControl);

        setJMenuBar(mainMenu);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(panViewer, javax.swing.GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scroller, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(panNavigation, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(panStatusBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(panNavigation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panViewer, javax.swing.GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(scroller, javax.swing.GroupLayout.DEFAULT_SIZE, 373, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panStatusBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void mnuFikesOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuFikesOpenActionPerformed
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getName().endsWith(".wrm") || f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "*.wrm (WRoom file)";
            }
        });
        chooser.showOpenDialog(this);
        if (chooser.getSelectedFile() != null) {
            try {
                cboURLs.removeItem(chooser.getSelectedFile().toURI().toURL().toString());
                cboURLs.insertItemAt(chooser.getSelectedFile().toURI().toURL().toString(), 0);
                cboURLs.setSelectedIndex(0);
                btnURLGo.doClick();
            } catch (MalformedURLException ex) {
                Logger.getLogger(Browser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_mnuFikesOpenActionPerformed

    private void btnURLGoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnURLGoActionPerformed
        try {
            if (cboURLs.getSelectedItem() == null) {
                cboURLs.setSelectedIndex(0);
            }
            if (cboURLs.getSelectedItem() != null) {
                URL url = new URL(cboURLs.getSelectedItem().toString());
                loadRoom(url);
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(Browser.class.getName()).log(Level.SEVERE, null, ex);
        }

    }//GEN-LAST:event_btnURLGoActionPerformed

    private void cboURLsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cboURLsActionPerformed
        btnURLGo.doClick();
    }//GEN-LAST:event_cboURLsActionPerformed

    private void mnuEditSetHomepageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuEditSetHomepageActionPerformed
        if (cboURLs.getSelectedItem() != null) {
            preferences.put("homepage", cboURLs.getSelectedItem().toString());
            try {
                preferences.sync();
            } catch (BackingStoreException ex) {
                Logger.getLogger(Browser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_mnuEditSetHomepageActionPerformed

    private void mnuEditGoToHomepageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuEditGoToHomepageActionPerformed
        cboURLs.removeItem(preferences.get("homepage", "http://wroom.crombz.com"));
        cboURLs.insertItemAt(preferences.get("homepage", "http://wroom.crombz.com"), 0);
        cboURLs.setSelectedIndex(0);
    }//GEN-LAST:event_mnuEditGoToHomepageActionPerformed

    private void txtChatKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtChatKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            if (irc != null && irc.isConnected()) {
                String msg = txtChat.getText();
                if (!msg.startsWith("/")) {
                    irc.doPrivmsg(chatroom, msg);
                    renderer.addMessage("Me>" + msg);
                }
                txtChat.setText("");
            }
        }
    }//GEN-LAST:event_txtChatKeyPressed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        if (irc != null && irc.isConnected()) {
            irc.doQuit();
        }
    }//GEN-LAST:event_formWindowClosing

    private void mnuEditSetNickNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuEditSetNickNameActionPerformed
        String nick = preferences.get("nick", "Guest__" + System.currentTimeMillis());
        dlgNickInput d = new dlgNickInput(nick, this, true);
        d.setVisible(true);
        preferences.put("nick", d.getNick());
        if (irc != null && irc.isConnected()) {
            irc.doNick(nick);
        }
        try {
            preferences.sync();
        } catch (BackingStoreException ex) {
            Logger.getLogger(Browser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_mnuEditSetNickNameActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnURLGo;
    private javax.swing.JComboBox<String> cboURLs;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem7;
    private javax.swing.JLabel lblMessage;
    private javax.swing.JLabel lblURL;
    private javax.swing.JList<String> lstUsers;
    private javax.swing.JMenuBar mainMenu;
    private javax.swing.JMenu mnuControl;
    private javax.swing.JMenu mnuEdit;
    private javax.swing.JMenuItem mnuEditGoToHomepage;
    private javax.swing.JMenuItem mnuEditSetHomepage;
    private javax.swing.JMenuItem mnuEditSetNickName;
    private javax.swing.JMenuItem mnuFikesOpen;
    private javax.swing.JMenu mnuFiles;
    private javax.swing.JPanel panNavigation;
    private javax.swing.JPanel panStatusBar;
    private javax.swing.JPanel panViewer;
    private javax.swing.JScrollPane scroller;
    private javax.swing.JTextField txtChat;
    // End of variables declaration//GEN-END:variables

    private boolean isAlreadyLoading = false;

    private void loadRoom(URL url) {
        if (!isAlreadyLoading) {
            txtChat.setEnabled(false);
            isAlreadyLoading = true;
            Message listener = this;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (renderer != null) {
                        panViewer.remove(renderer);
                        renderer.stop();

                    } else {
                        panViewer.removeAll();
                    }
                    rendStatus.resetStatus();
                    rendStatus.updateStatus("Reaching " + url.toString());
                    panViewer.add(rendStatus, BorderLayout.CENTER);
                    panViewer.updateUI();
                    rendStatus.updateStatus("Loading " + url.toString());
                    try {
                        RoomFile f = new RoomFile(url, listener);
                        teleports = f.getTeleports();
                        if (irc != null) {
                            irc.doPart(chatroom);
                            userModel.removeAllElements();
                            if (!f.getChatHost().equals(chatHost)) {
                                irc.doQuit();
                                irc = null;
                            }
                        }
                        chatroom = f.getChatroom();
                        chatHost = f.getChatHost();
                        chatPort = f.getChatPort();
                        rendStatus.updateStatus("Setting up the renderer...");
                        String id = url.toString().hashCode() + "";
                        userSprites.clear();
                        renderer = new Renderer(f, listener, userSprites);
                        if (irc == null) {
                            connectToIRC();
                        } else {
                            irc.doJoin(chatroom);
                        }

                        rendStatus.updateStatus("Loading sounds...");
                        sounds = f.getSounds();
                        medias = f.getMedias();
                        doors = f.getDoors();
                        webpages = f.getWebPages();
                        embeded = f.getEmbeded();
                        downloads = f.getDownloads();
                        notifications = f.getNotifications();
                        rendStatus.updateStatus("Starting...");
                        panViewer.remove(rendStatus);
                        panViewer.add(renderer, BorderLayout.CENTER);
                        renderer.start();
                        renderer.addMessage("Welcome to " + f.getTitle());
                        if (backgroundSound != null) {
                            backgroundSound.stop();
                            backgroundSound.close();
                        }
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                URL sound = f.getBackgroundSound();
                                if (sound != null) {
                                    try {
                                        backgroundSound = AudioSystem.getClip();
                                        AudioInputStream inputStream = AudioSystem.getAudioInputStream(sound);
                                        backgroundSound.open(inputStream);
                                        backgroundSound.start();
                                        backgroundSound.loop(Clip.LOOP_CONTINUOUSLY);
                                    } catch (Exception e) {
                                        System.err.println(e.getMessage());
                                    }
                                }
                            }
                        }).start();
                        if (panViewer != null) {
                            panViewer.updateUI();
                        }
                        repaint();
                        lblMessage.setText("Welcome to " + f.getTitle());
                    } catch (Exception ex) {
                        rendStatus.updateStatus("Could not load WRoom... " + ex.getMessage());
                        ex.printStackTrace();
                    }
                    isAlreadyLoading = false;
                }
            }).start();
        }
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
                    lblMessage.setText("Downloading " + file.getName());
                    byte[] buffer = new byte[65536];
                    int count = in.read(buffer);
                    while (count != -1) {
                        lblMessage.setText("Downloading " + file.getName() + "(" + count + " bytes)");
                        count = in.read(buffer);
                        if (count != -1) {
                            out.write(buffer, 0, count);
                        }
                    }
                    lblMessage.setText("Downloading completed for " + file.getName());
                    out.close();
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(Browser.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        Logger.getLogger(Browser.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }).start();
    }

    @Override
    public void OnAction(int x, int y) {
        String loc = x + "," + y;
        Texture mediaTexture = null;

        //Check if Texture has Media
        if (renderer.getMap()[y][x] > 0) {
            Texture t = renderer.getTextures().get(renderer.getMap()[y][x] - 1);
            System.out.println("testing texture " + renderer.getMap()[y][x]);
            if (t.hasMedia()) {
                mediaTexture = t;
                System.out.println("Texture has media");
            }
        }

        if (mediaTexture != null) {
            if (mediaTexture.isMediaPlaying()) {
                mediaTexture.stopMedia();
            } else {
                mediaTexture.playMedia();
            }
        }
        if (sounds.containsKey(loc)) {
            playSound(sounds.get(loc));
        }
        if (teleports.containsKey(loc)) {
            cboURLs.removeItem(teleports.get(loc).base.toString());
            cboURLs.insertItemAt(teleports.get(loc).base.toString(), 0);
            cboURLs.setSelectedIndex(0);
            btnURLGo.doClick();
        }
        if (notifications.containsKey(loc)) {
            if (renderer != null) {
                renderer.addMessage(notifications.get(loc));
            }
        }
        if (downloads.containsKey(loc)) {
            try {
                URL dl = downloads.get(loc);
                downloadFile(dl);
            } catch (IOException ex) {
                Logger.getLogger(Browser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (medias.containsKey(loc)) {
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
        if (webpages.containsKey(loc)) {
            try {
                if (backgroundSound != null) {
                    backgroundSound.stop();
                }
            } catch (Exception ex) {
            }
            WebPanel web = new WebPanel(webpages.get(loc), this, getBounds());
            panViewer.remove(renderer);
            if (irc != null && irc.isConnected()) {
                irc.doAway("Browsing web at " + webpages.get(loc));
            }
            panViewer.add(web, BorderLayout.CENTER);
            web.setSize(getWidth(), getHeight() * 2 / 3);
            web.setVisible(true);
            web.requestFocus();
        }
        if (embeded.containsKey(loc)) {
            try {
                if (backgroundSound != null) {
                    backgroundSound.stop();
                }
            } catch (Exception ex) {
            }
            WebPanel web = new WebPanel(embeded.get(loc), this, getBounds());
            if (irc != null && irc.isConnected()) {
                irc.doAway("Watching video online: " + webpages.get(loc));
            }
            panViewer.remove(renderer);
            panViewer.add(web, BorderLayout.CENTER);
            web.setSize(getWidth(), getHeight() * 2 / 3);
            web.setVisible(true);
            web.requestFocus();
            panViewer.updateUI();
        }
        System.out.println("Opening door at " + loc);
        if (doors.containsKey(loc)) {
            System.out.println("Opening door at " + loc);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("Opening door at " + loc);
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
                        Logger.getLogger(Browser.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    renderer.getMap()[y][x] = oldTexture;
                    System.out.println("Closing door...");
                }
            }).start();
        }
    }

    @Override
    public void OnTrigger(int x, int y
    ) {
        if (irc != null && irc.isConnected()) {
            irc.doPrivmsg(chatroom, "MOVING TO:" + x + "x" + y);
        }
    }

    @Override
    public void OnNotification(int x, int y, String msg
    ) {
        rendStatus.updateStatus(msg);
    }

    @Override
    public void OnError(int x, int y, String error
    ) {
    }

    @Override
    public void onCloseView(JComponent comp
    ) {
        try {
            if (comp instanceof VideoPanel) {
                VideoPanel media = (VideoPanel) comp;
                media.stop();
                panViewer.remove(media);
                panViewer.add(renderer, BorderLayout.CENTER);
                if (irc != null && irc.isConnected()) {
                    irc.doAway();
                }
                renderer.requestFocus();
                if (backgroundSound != null) {
                    backgroundSound.start();
                    backgroundSound.loop(Clip.LOOP_CONTINUOUSLY);
                }
            } else if (comp instanceof WebPanel) {
                WebPanel web = (WebPanel) comp;
                web.stop();
                panViewer.remove(web);
                panViewer.add(renderer, BorderLayout.CENTER);
                if (irc != null && irc.isConnected()) {
                    irc.doAway();
                }
                renderer.requestFocus();
                if (backgroundSound != null) {
                    backgroundSound.start();
                    backgroundSound.loop(Clip.LOOP_CONTINUOUSLY);
                }
            }
        } catch (Exception ex) {
        }
        panViewer.updateUI();
    }
}
