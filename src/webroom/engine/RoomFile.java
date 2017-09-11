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
        listener.OnNotification(0, 0, "Loading map...");
        int count = in.read(buffer);
        content = new String(buffer, 0, count);
        in.close();
        String[] lines = content.split("\n");
        ArrayList<String> mapLines = new ArrayList<>();
        listener.OnNotification(0, 0, "Parsing map...");
        title = "Undefined...";
        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex];
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
                switch (action[0].trim()) {
                    case "teleport":
                        Teleport t = new Teleport();
                        String target = action[3].trim();
                        if (!target.toLowerCase().startsWith("http")) {
                            target = base.toString() + "/" + target;
                        }
                        t.base = new URL(target);
                        teleports.put((action[1] + "," + action[2]).trim(), t);
                        if (action.length == 5) {
                            t.title = action[4];
                        } else {
                            t.title = t.base.toString();
                        }
                        break;
                    case "sound":
                        sounds.put((action[1] + "," + action[2]).trim(), new URL(base.toString() + "/" + action[3].trim()));
                        break;
                    case "message":
                        notification.put((action[1] + "," + action[2]).trim(), action[3]);
                        break;
                    case "download":
                        downloads.put((action[1] + "," + action[2]).trim(), new URL(action[3]));
                        break;
                    case "media":
                        medias.put((action[1] + "," + action[2]).trim(), new URL(action[3]));
                        break;
                    case "browse":
                        webpages.put((action[1] + "," + action[2]).trim(), new URL(action[3]));
                        break;
                    case "embeded":
                        embeded.put((action[1] + "," + action[2]).trim(), action[3]);
                        break;
                    case "door":
                        doors.put((action[1] + "," + action[2]).trim(), "");
                        break;
                }
            } else if (line.trim().toLowerCase().startsWith("backgroundsound=")) {
                backgroundSound = new URL(base.toString() + "/" + line.split("=")[1]);
            } else if (line.trim().toLowerCase().startsWith("text=")) {
                String text[] = line.replaceFirst("text=", "").split(",");
                if (text.length == 3) {
                    //Text is on a single ligne...
                    texts.put(text[0] + "," + text[1], text[2]);
                } else {
                    //Text is on multiple lines....
                    String key = (text[0] + "," + text[1]).trim();
                    String textContent = "";
                    for (int i = lineIndex + 1; i < lines.length; i++) {
                        if (!lines[i].trim().toLowerCase().equals("=text")) {
                            textContent += lines[i].trim();
                        } else {
                            lineIndex = i - 1;
                            break;
                        }
                    }
                    texts.put(key, textContent);
                }
            } else if (line.trim().toLowerCase().startsWith("start=")) {
                startX = new Double(line.split("=")[1].split(",")[0]);
                startY = new Double(line.split("=")[1].split(",")[1]);
                startX += 0.5;
                startY += 0.5;
            }
        }
        //convert to map...
        listener.OnNotification(0, 0, "Creating floor...");
        map = new int[mapLines.size()][mapLines.get(0).split(",").length];
        for (int y = 0; y < mapLines.size(); y++) {
            String[] row = mapLines.get(y).split(",");
            for (int x = 0; x < row.length; x++) {
                map[y][x] = new Integer(row[x].trim());
            }
        }
        listener.OnNotification(0, 0, "Welcome to " + title);
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
