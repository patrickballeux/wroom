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

    /**
     * Creates new form Renderer
     *
     */
    public Renderer(RoomFile file, Message listener,ArrayList<Sprite>userSprites) {
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
        sprites = new ArrayList<>();
        for (String key : teleports.keySet()) {
            Sprite s = new Sprite();
            double x = Integer.parseInt(key.split(",")[0]);
            double y = Integer.parseInt(key.split(",")[1]);
            x += 0.5;
            y += 0.5;
            s.texture = new Texture(teleporticon, "<div style='text-align:center;font-size:8px;color:white;background-color:#111111;'>" + teleports.get(key).base.toString() + "</div>", 0);
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
            String door = file.getDoors().get(key);
            int x = Integer.parseInt(door.trim().split("x")[0]);
            int y = Integer.parseInt(door.trim().split("x")[1]);
            Texture texture = textures.get(map[y][x] - 1);
            textures.add(new Texture(texture, "<center>Door</center>", 0));
            map[y][x] = textures.size();
        }
        camera = new Camera(startX, startY, 1, 0, 0, -.66, listener, getBounds());
        screen = new Screen(map, textures, getWidth() - STEPHEIGHT, getHeight() - STEPHEIGHT, floor, ceiling, sprites,userSprites);
        thread = new Thread(this);
        addKeyListener(camera);
        this.setOpaque(true);
        repaint();
        setVisible(true);
        //setDoubleBuffered(true);
        running = true;
    }

    public ArrayList<Texture> getTextures() {
        return textures;
    }

    public int[][] getMap() {
        return map;
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

    public void paint(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        try {
            screen.update(camera, pixels);
            camera.update(map);
            if (System.currentTimeMillis() - startTime < 1000) {
                ((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ((System.currentTimeMillis() - startTime) / 1000.0f)));
            }
        } catch (Exception ex) {
            System.err.println("ERROR: " + ex.getMessage());
        }
        if (camera.back || camera.forward) {
            deltaSteps += deltaStepsDir;
            if (Math.abs(deltaSteps) > STEPHEIGHT) {
                deltaStepsDir = deltaStepsDir * -1;
            }
        } else {
            deltaSteps = 0;
            deltaStepsDir = 1;
        }
        g.drawImage(image, 0, 0 + deltaSteps, this);
        g.setFont(new Font("Monospaced", Font.BOLD, 16));
        int y = 20;
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
        addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                formFocusGained(evt);
            }
        });
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                formMouseClicked(evt);
            }
        });
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
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

    private void formMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseClicked
        this.requestFocus();
        if (evt.getClickCount() == 2) {
            String loc = "#ACTION=" + (int) camera.yPos + "," + (int) camera.xPos;
            listener.status(loc);
        }
    }//GEN-LAST:event_formMouseClicked

    private void formFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_formFocusGained
        if (listener != null) {
            listener.status("#CLOSEMEDIA");
        }
    }//GEN-LAST:event_formFocusGained

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        if (getWidth() > 0 && (image == null || image.getWidth() != getWidth())) {
            //we need to keep a 4/3 ratio
            int w = getWidth() / 2 * 2;
            int h = getHeight() / 2 * 2;
            image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
            screen = new Screen(map, textures, image.getWidth(), image.getHeight(), floor, ceiling, sprites,userSprites);
            camera.setSize(getBounds());
        }

    }//GEN-LAST:event_formComponentResized

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
            nextPTR = System.currentTimeMillis() + 30;
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
