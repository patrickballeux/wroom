/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webroom.engine;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 *
 * @author patri
 */
public class Renderer extends javax.swing.JPanel implements Runnable {

    private boolean running;
    private BufferedImage image;
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
    private static final BasicStroke lineWidth = new BasicStroke(15);
    private boolean enableHighQuality = true;

    /**
     * Creates new form Renderer
     *
     */
    public Renderer(RoomFile file, Message listener, ArrayList<Sprite> userSprites) {
        initComponents();
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
                    System.out.println("Old texture: " + map[y][x]);
                    map[y][x] = textures.size();
                    System.out.println("New texture: " + map[y][x]);
                } else {
                    Texture t = new Texture(file.getTexts().get(key), 0);
                    textures.add(t);
                    System.out.println("Old texture: " + map[y][x]);
                    map[y][x] = textures.size();
                    System.out.println("New texture: " + map[y][x]);
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
        image = new BufferedImage(Texture.SIZE * 4 / 3, Texture.SIZE, BufferedImage.TYPE_INT_ARGB);
        pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        screen = new Screen(map, textures, image.getWidth(), image.getHeight(), floor, ceiling, sprites, userSprites);
        thread = new Thread(this);
        addKeyListener(camera);
        this.setOpaque(true);
        repaint();
        setVisible(true);
        //setDoubleBuffered(true);
        running = true;
    }

    public void setHighQuality(boolean value) {
        enableHighQuality = value;
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
        System.out.println("| " + msg);
        if (userMessages.size() > 5) {
            userMessages.remove(0);
        }
    }

    private long lastFPSCount = System.currentTimeMillis();
    private long imageCountFPS = 0;
    private long fps = 0;

    @Override
    public void paintComponent(Graphics gg) {
        Graphics2D g = (Graphics2D) gg;
        g.setColor(Color.BLACK);
        g.clearRect(0, 0, getWidth(), getHeight());
        g.fillRect(0, 0, getWidth(), getHeight());
        try {
            screen.update(camera, pixels);
            if (System.currentTimeMillis() - startTime < 1000) {
                ((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ((System.currentTimeMillis() - startTime) / 1000.0f)));
            }
        } catch (Exception ex) {
            System.err.println("ERROR: " + ex.getMessage());
        }
        camera.update(map);
        if (camera.back || camera.forward) {
            deltaSteps += deltaStepsDir;
            if (Math.abs(deltaSteps) > STEPHEIGHT) {
                deltaStepsDir = deltaStepsDir * -1;
            }
        } else {
            deltaSteps = 0;
            deltaStepsDir = 1;
        }
        //g.drawImage(image, 0, 0 + deltaSteps, this);
        if (enableHighQuality) {
            g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        }
        int w = getWidth();
        int h = w * image.getHeight() / image.getWidth();
        int x = (getWidth() - w) / 2;
        int y = ((getHeight() - h) / 2) + deltaSteps;
        //g.drawImage(image,0,0,null);
        g.drawImage(image, x, y, w + x, h + y, 0, 0, image.getWidth(), image.getHeight(), null);
        //g.drawImage(image.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH), x, y, null);
        g.setFont(new Font("Monospaced", Font.BOLD, 16));
        y = 20;
        for (int i = 0; i < userMessages.size(); i++) {
            String m = userMessages.get(i);
            g.setColor(Color.BLACK);
            g.drawString(m, 12, y + 2);
            g.setColor(Color.GREEN);
            g.drawString(m, 10, y);
            y += 20;
        }
        g.setColor(Color.BLACK);
        ((Graphics2D) g).setStroke(lineWidth);
        g.drawRect(0, 0, getWidth(), getHeight());
        imageCountFPS++;
        if (System.currentTimeMillis() - lastFPSCount >= 1000) {
            fps = imageCountFPS;
            imageCountFPS = 0;
            lastFPSCount = System.currentTimeMillis();
            //adjust camera speed
            camera.MOVE_SPEED = Math.PI / fps / 2D;
            camera.ROTATION_SPEED = Math.PI / fps / 2D;
        }
        g.setColor(Color.red);
        g.drawString("FPS: " + fps, 2, getHeight() - 5);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorMoved(javax.swing.event.AncestorEvent evt) {
            }
            public void ancestorAdded(javax.swing.event.AncestorEvent evt) {
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent evt) {
                formAncestorRemoved(evt);
            }
        });
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                formMouseClicked(evt);
            }
        });
        addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                formKeyPressed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void formAncestorRemoved(javax.swing.event.AncestorEvent evt) {//GEN-FIRST:event_formAncestorRemoved

    }//GEN-LAST:event_formAncestorRemoved

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


    private void formMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseClicked
        this.requestFocus();
        if (evt.getClickCount() == 2) {
            // Find if object was clicked...
            double cameraX = (evt.getX() * 2D / getWidth()) - 1D;
            sendOnAction(cameraX);

        }
    }//GEN-LAST:event_formMouseClicked

    private void formKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_formKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_SPACE) {
            sendOnAction(0);
        }
    }//GEN-LAST:event_formKeyPressed

    public void run() {
        requestFocus();
        startTime = System.currentTimeMillis();
        long nextPTR = System.currentTimeMillis() + 30;
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
                repaint();
            } catch (Exception ex) {
                System.err.println("ERROR: " + ex.getMessage());
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
