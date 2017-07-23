package webroom.engine;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JLabel;

public class Texture implements Runnable {

    public int[] pixels;
    public static final int SIZE = 256;
    public BufferedImage image;
    public Texture[] animated = new Texture[0];
    private long reloadTime = 0;
    private Texture original = null;
    private String text = "";
    private boolean mStopMe = false;
    private URL url = null;
    JLabel label = null;

    @Override
    public void run() {
        animate();
    }

    public Texture(URL[] urls) {
        animated = new Texture[urls.length];
        for (int i = 0; i < urls.length; i++) {
            animated[i] = new Texture(urls[i]);
        }
        new Thread(this).start();
    }

    public Texture(URL file) {
        this.url = file;
        new Thread(this).start();
    }

    public Texture(String text, long reloadTime) {
        this(null, text, reloadTime);
    }

    public Texture(Texture t, String text, long reloadTime) {
        original = t;
        this.text = text;
        this.reloadTime = reloadTime;
        new Thread(this).start();
    }

    public void stop() {
        mStopMe = true;
    }

    private void initLabel(Graphics2D g) {
        String html = "<html><body width=" + SIZE + " height=" + SIZE + ">" + text + "</body></html>";
        if (text.trim().length() > 0) {
            label = new JLabel(html);
            label.setSize(SIZE, SIZE);
            label.setLocation(0, 0);
            label.setForeground(Color.BLACK.brighter());
            label.setFont(new Font("Monospaced", Font.BOLD, 24));
            label.setPreferredSize(label.getSize());
            label.setOpaque(false);
            label.paint(g);
            image.getRGB(0, 0, SIZE, SIZE, pixels, 0, SIZE);
        }
    }

    private void animate() {
        pixels = new int[SIZE * SIZE];
        BufferedImage originalImage = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        if (original != null) {
            if (original.animated.length > 0) {
                animated = new Texture[original.animated.length];
                for (int i = 0; i < animated.length; i++) {
                    animated[i] = new Texture(original.animated[i], text, reloadTime);
                }
                originalImage = animated[0].image;
            } else {
                while (original.image == null) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Texture.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                originalImage = original.image;
            }

        } else if (url != null) {
            try {
                BufferedImage temp = ImageIO.read(url);
                Graphics2D gtemp = originalImage.createGraphics();
                gtemp.drawImage(temp.getScaledInstance(SIZE, SIZE, Image.SCALE_SMOOTH), 0, 0, null);
                gtemp.dispose();
            } catch (IOException ex) {
                Logger.getLogger(Texture.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (animated.length > 0) {
            while (animated[0].image == null) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Texture.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            originalImage = animated[0].image;
        } else {
            Graphics2D gtemp = originalImage.createGraphics();
            gtemp.setColor(Color.BLACK);
            gtemp.fillRect(0, 0, SIZE, SIZE);
            gtemp.dispose();
        }
        image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.drawImage(originalImage, 0, 0, null);
        pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        initLabel(g);
        if (animated.length > 0) {
            int index = 0;
            while (!mStopMe) {
                while (animated[index].image == null) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Texture.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                pixels = ((DataBufferInt) animated[index++].image.getRaster().getDataBuffer()).getData();
                if (index >= animated.length) {
                    index = 0;
                }
                try {
                    Thread.sleep(1000 / animated.length);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Texture.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        g.dispose();
    }

}
