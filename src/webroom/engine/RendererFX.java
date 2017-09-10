/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webroom.engine;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.TouchPoint;
import javafx.scene.paint.Color;

/**
 *
 * @author patri
 */
public class RendererFX extends Canvas implements Runnable {

    private boolean running;
    private int[] pixels;
    private ArrayList<Texture> textures;
    private Texture floor;
    private Texture ceiling;
    private Camera camera;
    private Screen screen;
    private Thread thread;
    private TreeMap<String, Teleport> teleports;
    private int[][] map;
    private long startTime = System.currentTimeMillis();
    private ArrayList<String> userMessages = new ArrayList<>();
    private ArrayList<Sprite> sprites;
    private ArrayList<Sprite> userSprites;
    private Message listener;
    private int deltaSteps = 0;
    private int deltaStepsDir = 1;
    private static final int STEPHEIGHT = 3;
    
        private java.util.prefs.Preferences preferences;


    public RendererFX(RoomFile file, Message listener, ArrayList<Sprite> userSprites) {
        this.userSprites = userSprites;
        textures = file.getTextures();
        floor = file.getFloor();
        ceiling = file.getCeiling();
        map = file.getMap();
        this.listener = listener;
        double startX = file.getStartX();
        double startY = file.getStartY();
        if (startX == -1) {
            System.out.println("Finding start entry point...");
            for (int y = 0; y < map.length; y++) {
                for (int x = 0; x < map[y].length; x++) {
                    if (map[y][x] == 0) {
                        startX = x;
                        startY = y;
                        break;
                    }
                    if (startX != 0) {
                        break;
                    }
                }
            }
            startX += 0.5;
            startY += 0.5;
        }
        this.teleports = file.getTeleports();
        Texture teleporticon = new Texture(getClass().getResource("/webroom/gui/teleport.png"));
        Texture mediaicon = new Texture(getClass().getResource("spritemediaplay.png"));
        Texture webicon = new Texture(getClass().getResource("spriteweblink.png"));
        sprites = new ArrayList<>();
        for (String key : teleports.keySet()) {
            Sprite s = new Sprite();
            double x = Integer.parseInt(key.split(",")[0]);
            double y = Integer.parseInt(key.split(",")[1]);
            x += 0.5;
            y += 0.5;
            s.texture = new Texture(teleporticon, "<div style='text-align:center;font-size:24px;color:#111111;border: 2px solid #111111;background-color:WHITE;'>" + teleports.get(key).title + "</div>", 0);
            s.x = x;
            s.y = y;
            s.distance = 0;
            sprites.add(s);
        }
        for (String key : file.getMedias().keySet()) {
            Sprite s = new Sprite();
            double x = Integer.parseInt(key.split(",")[0]);
            double y = Integer.parseInt(key.split(",")[1]);
            x += 0.5;
            y += 0.5;
            Texture media = new Texture(file.getMedias().get(key), true);
            s.texture = media;
            s.x = x;
            s.y = y;
            s.distance = 0;
            sprites.add(s);
        }
        for (String key : file.getWebPages().keySet()) {
            Sprite s = new Sprite();
            double x = Integer.parseInt(key.split(",")[0]);
            double y = Integer.parseInt(key.split(",")[1]);
            x += 0.5;
            y += 0.5;
            s.texture = new Texture(webicon, "<div style='background-color:white;border:2px solid #0000FF;color:#111111;text-align:center;'>" + file.getWebPages().get(key).toString() + "</div>", 0);
            s.x = x;
            s.y = y;
            s.distance = 0;
            sprites.add(s);
        }
        for (String key : file.getEmbeded().keySet()) {
            Sprite s = new Sprite();
            double x = Integer.parseInt(key.split(",")[0]);
            double y = Integer.parseInt(key.split(",")[1]);
            x += 0.5;
            y += 0.5;
            s.texture = mediaicon;
            s.x = x;
            s.y = y;
            s.distance = 0;
            sprites.add(s);
        }
        for (String key : file.getTexts().keySet()) {
            int x = Integer.parseInt(key.split(",")[0]);
            int y = Integer.parseInt(key.split(",")[1]);
            try {
                if (map[y][x] != 0) {
                    Texture t = new Texture(textures.get(map[y][x] - 1), file.getTexts().get(key), 0);
                    textures.add(t);
                    map[y][x] = textures.size();
                } else {
                    Texture t = new Texture(file.getTexts().get(key), 0);
                    textures.add(t);
                    map[y][x] = textures.size();
                }
            } catch (Exception ex) {
                System.err.println("Error loaging texture... : " + ex.getMessage());
            }
        }
        for (String key : file.getDoors().keySet()) {
            int x = Integer.parseInt(key.trim().split(",")[0]);
            int y = Integer.parseInt(key.trim().split(",")[1]);
            Texture texture = textures.get(map[y][x] - 1);
            textures.add(new Texture(texture, "<center>Door</center>", 0));
            map[y][x] = textures.size();
        }
        camera = new Camera(startX, startY, 1, 0, 0, -.66, listener);
        //screen = new Screen(map, textures, getWidth() - STEPHEIGHT, getHeight() - STEPHEIGHT, floor, ceiling, sprites,userSprites);
        int height = 512;
        int width = height * 4 / 3;
        pixels = new int[width * height];
        screen = new Screen(map, textures, width, height, floor, ceiling, sprites, userSprites);
        thread = new Thread(this);
        //addKeyListener(camera);
        setOnKeyPressed((event) -> {
            requestFocus();
            switch (event.getCode()) {
                case LEFT:
                case A:
                    camera.left = true;
                    break;
                case RIGHT:
                case D:
                    camera.right = true;
                    break;
                case DOWN:
                case S:
                    camera.back = true;
                    break;
                case UP:
                case W:
                    camera.forward = true;
                    break;
                case SPACE:
                    sendOnAction(0);
                    break;
                case CONTROL:
                    camera.strafe = true;
                    break;
                case SHIFT:
                    camera.runFactor = 1.8;
                    break;
            }

            event.consume();
        });

        setOnMouseClicked((event) -> {
            requestFocus();
            if (event.getClickCount() == 2) {
                sendOnAction(event.getX());
            }
            event.consume();
        });

        setOnKeyReleased((event) -> {
            switch (event.getCode()) {
                case LEFT:
                case A:
                    camera.left = false;
                    break;
                case RIGHT:
                case D:
                    camera.right = false;
                    break;
                case DOWN:
                case S:
                    camera.back = false;
                    break;
                case UP:
                case W:
                    camera.forward = false;
                    break;
                case CONTROL:
                    camera.strafe = false;
                    break;
                case SHIFT:
                    camera.runFactor = 1;
                    break;
                case C:
                    listener.onChatRequest();
                    break;
            }
            event.consume();
        });
        setOnTouchPressed((event) -> {
            lastTouched = event.getTouchPoint();
            event.consume();
        });
        setOnTouchReleased((event) -> {
            camera.left = false;
            camera.right = false;
            camera.forward = false;
            camera.back = false;
            event.consume();
        });

        setOnTouchStationary((event) -> {
            if (lastTouched.getX() + 30 < event.getTouchPoint().getX()) {
                camera.right = true;
                camera.left = false;
            }
            if (lastTouched.getX() - 30 > event.getTouchPoint().getX()) {
                camera.left = true;
                camera.right = false;
            }
            if (lastTouched.getY() + 30 < event.getTouchPoint().getY()) {
                camera.forward = false;
                camera.back = true;
            }
            if (lastTouched.getY() - 30 > event.getTouchPoint().getY()) {
                camera.back = false;
                camera.forward = true;
            }
            event.consume();
        });
        setFocused(true);
        setWidth(width);
        setHeight(height);
        setCache(true);
        running = true;
        requestFocus();
    }

    TouchPoint lastTouched;

    private void sendOnAction(double cameraX) {
        double rayDirX = camera.xDir + camera.xPlane * cameraX;
        double rayDirY = camera.yDir + camera.yPlane * cameraX;
        //Map position
        int mapX = (int) camera.xPos;
        int mapY = (int) camera.yPos;
        //length of ray from current position to next x or y-side
        double sideDistX;
        double sideDistY;
        //Length of ray from one side to next in map
        double deltaDistX = Math.sqrt(1 + (rayDirY * rayDirY) / (rayDirX * rayDirX));
        double deltaDistY = Math.sqrt(1 + (rayDirX * rayDirX) / (rayDirY * rayDirY));
        //Direction to go in x and y
        int stepX, stepY;
        boolean hit = false;//was a wall hit
        //Figure out the step direction and initial distance to a side
        if (rayDirX < 0) {
            stepX = -1;
            sideDistX = (camera.xPos - mapX) * deltaDistX;
        } else {
            stepX = 1;
            sideDistX = (mapX + 1.0 - camera.xPos) * deltaDistX;
        }
        if (rayDirY < 0) {
            stepY = -1;
            sideDistY = (camera.yPos - mapY) * deltaDistY;
        } else {
            stepY = 1;
            sideDistY = (mapY + 1.0 - camera.yPos) * deltaDistY;
        }
        //Loop to find where the ray hits a wall
        int counter = 0; // testing only block away for 3 times
        while (!hit && counter++ < 2) {
            //Jump to next square
            if (sideDistX < sideDistY) {
                sideDistX += deltaDistX;
                mapX += stepX;
            } else {
                sideDistY += deltaDistY;
                mapY += stepY;
            }
            //Check if ray has hit a wall
            if (!hit && map[mapX][mapY] > 0) {
                hit = true;
            } else //Check if ray has hit a sprite
            {
                for (Sprite s : sprites) {
                    if (((int) s.y) == (int) mapX && ((int) s.x) == (int) mapY) {
                        hit = true;
                    }
                }
            }
        }
        if (hit) {
            listener.OnAction((int) mapY, (int) mapX);
        }
    }

    public ArrayList<Texture> getTextures() {
        return textures;
    }

    public int[][] getMap() {
        return map;
    }

    public ArrayList<Sprite> getSprites() {
        return sprites;
    }

    public Camera getCamera() {
        return camera;
    }

    public synchronized void start() {
        running = true;
        thread.start();
    }

    public synchronized void stop() {
        running = false;
        for (Texture t : textures) {
            t.stop();
        }
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void addMessage(String msg) {
        userMessages.add(msg);
        if (userMessages.size() > 5) {
            userMessages.remove(0);
        }
    }

    private long lastFPSCount = System.currentTimeMillis();
    private long imageCountFPS = 0;
    private long fps = 0;

    @Override
    public void run() {
        startTime = System.currentTimeMillis();
        long nextPTR = System.currentTimeMillis() + 30;
        BufferedImage image = new BufferedImage(Texture.SIZE * 4 / 3, Texture.SIZE, BufferedImage.TYPE_INT_ARGB);
        pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        WritableImage wimg = new WritableImage(image.getWidth(), image.getHeight());
        GraphicsContext g = getGraphicsContext2D();
        while (running) {

            long wait = nextPTR - System.currentTimeMillis();
            if (wait > 0) {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException ex) {
                }
            }
            nextPTR = System.currentTimeMillis() + 15;
            try {
                try {
                    screen.update(camera, pixels);
                } catch (Exception ex) {
                    System.err.println("ERROR: " + ex.getMessage());
                }
                camera.update(map);
                //draw Pixels

                SwingFXUtils.toFXImage(image, wimg);
                g.fillRect(0, 0, widthProperty().doubleValue(), heightProperty().doubleValue());
                g.drawImage(wimg, 0, 0);

            } catch (Exception ex) {
                System.err.println("ERROR: " + ex.getMessage());
            }
            imageCountFPS++;
            if (System.currentTimeMillis() - lastFPSCount >= 1000) {
                fps = imageCountFPS;
                imageCountFPS = 0;
                lastFPSCount = System.currentTimeMillis();
                //adjust camera speed
                camera.MOVE_SPEED = Math.PI / fps / 2D;
                camera.ROTATION_SPEED = Math.PI / fps / 3.5D;
                //System.out.println("FPS: " + fps);
            }
        }
    }

}
