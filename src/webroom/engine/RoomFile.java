/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webroom.engine;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 *
 * @author patri
 */
public class RoomFile {

    private URL base;
    private ArrayList<Texture> textures = new ArrayList<>();
    private TreeMap<String, Teleport> teleports = new TreeMap<>();
    private TreeMap<String, URL> sounds = new TreeMap<>();
    private Texture floor;
    private Texture ceiling;
    private int map[][];
    private Message listener;
    private String title;
    private URL backgroundSound = null;
    private TreeMap<String, String> notification = new TreeMap<>();
    private ArrayList<String> quotes = new ArrayList<>();
    private TreeMap<String, URL> downloads = new TreeMap<>();
    private TreeMap<String, URL> medias = new TreeMap<>();
    private TreeMap<String, String> texts = new TreeMap<>();
    private double startX = -1;
    private double startY = -1;
    private TreeMap<String, URL> webpages = new TreeMap<>();
    private TreeMap<String, String> embeded = new TreeMap<>();
    private TreeMap<String, String> doors = new TreeMap<>();
    private String chatroom = "#wroom";
    private String chatHost = "irc.freenode.net";
    private int chatPort = 6667;

    public RoomFile(URL baseLocation, Message listener) throws IOException {
        this.listener = listener;
        base = baseLocation;
        String content = "";
        String finalURL = base.toString();
        if (!finalURL.endsWith(".wrm")) {
            if (finalURL.endsWith("/")) {
                finalURL += "map.wrm";
            } else {
                finalURL += "/map.wrm";
            }
        } else {
            finalURL = base.toString();
            base = new URL(base.toString().substring(0, base.toString().lastIndexOf("/")));
        }
        InputStream in;
        in = new URL(finalURL).openStream();
        byte[] buffer = new byte[65536 * 4];
        listener.status(Message.Type.NOTIFICATION,"Loading map...");
        int count = in.read(buffer);
        content = new String(buffer, 0, count);
        in.close();
        String[] lines = content.split("\n");
        ArrayList<String> mapLines = new ArrayList<>();
        listener.status(Message.Type.NOTIFICATION,"Parsing map...");
        title = "Undefined...";
        for (String line : lines) {
            System.out.println(line);
            if (line.trim().toLowerCase().startsWith("texture=")) {
                String data = line.split("=")[1];
                if (data.contains(",")) {
                    String[] urls = data.split(",");
                    URL[] urlsObj = new URL[urls.length];
                    for (int i = 0; i < urls.length; i++) {
                        urlsObj[i] = new URL(base.toString() + "/" + urls[i].trim());
                    }
                    Texture t = new Texture(urlsObj);
                    textures.add(t);
                } else {
                    URL file = null;
                    if (data.startsWith("http:")) {
                        file = new URL(data);
                    } else if (data.startsWith("file:")) {
                        file = new URL(data);
                    } else {
                        file = new URL(base.toString() + "/" + data);
                    }
                    Texture t = new Texture(file);
                    textures.add(t);
                }
            } else if (line.trim().toLowerCase().startsWith("floor=")) {
                URL file = new URL(base.toString() + "/" + line.split("=")[1]);
                floor = new Texture(file);
            } else if (line.trim().toLowerCase().startsWith("ceiling=")) {
                URL file = new URL(base.toString() + "/" + line.split("=")[1]);
                ceiling = new Texture(file);
            } else if (line.trim().toLowerCase().startsWith("map=")) {
                mapLines.add(line.split("=")[1].replaceAll(" ", "").trim());
            } else if (line.trim().toLowerCase().startsWith("title=")) {
                title = line.split("=")[1];
            } else if (line.trim().toLowerCase().startsWith("chatroom=")) {
                chatroom = line.split("=")[1].trim();
            } else if (line.trim().toLowerCase().startsWith("server=")) {
                chatHost = line.split("=")[1].trim();
            } else if (line.trim().toLowerCase().startsWith("port=")) {
                chatPort = Integer.parseInt(line.split("=")[1].trim());
            } else if (line.trim().toLowerCase().startsWith("action=")) {
                String[] action = line.substring(line.indexOf("=") + 1).split(",");
                switch (action[0]) {
                    case "teleport":
                        Teleport t = new Teleport();
                        t.base = new URL(action[3].trim());
                        teleports.put(action[1] + "," + action[2], t);
                        break;
                    case "sound":
                        sounds.put(action[1] + "," + action[2], new URL(base.toString() + "/" + action[3].trim()));
                        break;
                    case "message":
                        notification.put(action[1] + "," + action[2], action[3]);
                        break;
                    case "download":
                        downloads.put(action[1] + "," + action[2], new URL(action[3]));
                        break;
                    case "media":
                        medias.put(action[1] + "," + action[2], new URL(action[3]));
                        break;
                    case "browse":
                        webpages.put(action[1] + "," + action[2], new URL(action[3]));
                        break;
                    case "embeded":
                        embeded.put(action[1] + "," + action[2], action[3]);
                        break;
                    case "door":
                        doors.put(action[1] + "," + action[2], action[3]);
                        break;
                }
            } else if (line.trim().toLowerCase().startsWith("backgroundsound=")) {
                backgroundSound = new URL(base.toString() + "/" + line.split("=")[1]);
            } else if (line.trim().toLowerCase().startsWith("quote=")) {
                quotes.add(line.split("=")[1]);
            } else if (line.trim().toLowerCase().startsWith("text=")) {
                String text[] = line.replaceFirst("text=", "").split(",");
                texts.put(text[0] + "," + text[1], text[2]);
            } else if (line.trim().toLowerCase().startsWith("start=")) {
                startX = new Double(line.split("=")[1].split(",")[0]);
                startY = new Double(line.split("=")[1].split(",")[1]);
                startX += 0.5;
                startY += 0.5;
            }
        }
        //convert to map...
        listener.status(Message.Type.NOTIFICATION,"Creating floor...");
        map = new int[mapLines.size()][mapLines.get(0).split(",").length];
        System.out.println("Map size: " + map.length + "x" + map[0].length);
        for (int y = 0; y < mapLines.size(); y++) {
            String[] row = mapLines.get(y).split(",");
            for (int x = 0; x < row.length; x++) {
                map[y][x] = new Integer(row[x].trim());
            }
        }
        listener.status(Message.Type.NOTIFICATION,"Welcome to " + title);
        listener.status(Message.Type.NOTIFICATION,"#MESSAGE=Welcome to " + title);
    }

    public String getChatroom() {
        return chatroom;
    }

    public String getChatHost() {
        return chatHost;
    }

    public int getChatPort() {
        return chatPort;
    }

    public TreeMap<String, String> getDoors() {
        return doors;
    }

    public TreeMap<String, URL> getMedias() {
        return medias;
    }

    public TreeMap<String, String> getEmbeded() {
        return embeded;
    }

    public TreeMap<String, URL> getWebPages() {
        return webpages;
    }

    public double getStartX() {
        return startX;
    }

    public double getStartY() {
        return startY;
    }

    public TreeMap<String, URL> getDownloads() {
        return downloads;
    }

    public TreeMap<String, String> getTexts() {
        return texts;
    }

    public String getTitle() {
        return title;
    }

    public TreeMap<String, String> getNotifications() {
        return notification;
    }

    public TreeMap<String, Teleport> getTeleports() {
        return teleports;
    }

    public ArrayList<String> getQuotes() {
        return quotes;
    }

    public URL getBackgroundSound() {
        return backgroundSound;
    }

    public TreeMap<String, URL> getSounds() {
        return sounds;
    }

    public ArrayList<Texture> getTextures() {
        return textures;
    }

    public Texture getFloor() {
        return floor;
    }

    public Texture getCeiling() {
        return ceiling;
    }

    public int[][] getMap() {
        return map;
    }
}
