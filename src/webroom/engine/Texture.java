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
    private boolean mIsReady = false;
    private SourceFFMpeg ffmpeg = null;

    @Override
    public void run() {
        animate();
    }

    public boolean isReady() {
        return mIsReady;
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
        if (ffmpeg != null) {
            try {
                ffmpeg.disposeStream();
            } catch (IOException ex) {
                Logger.getLogger(Texture.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
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
            while (!original.isReady()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Texture.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (original.animated.length > 0) {
                animated = new Texture[original.animated.length];
                for (int i = 0; i < animated.length; i++) {
                    animated[i] = new Texture(original.animated[i], text, reloadTime);
                }
                originalImage = animated[0].image;
            } else {
                originalImage = original.image;
            }

        } else if (url != null) {
            String endsWidthVideo = ".mp4.m3u8.avi.mpg.mov.ogg.ogv.m4v";
            String ext = url.toString().toLowerCase();
            ext = ext.substring(ext.length() - 3);
            if (endsWidthVideo.indexOf(ext) >= 0) {
                //this is a video...
                ffmpeg = new SourceFFMpeg(this, url.toString());
            } else {
                //This is an image
                try {
                    BufferedImage temp = ImageIO.read(url);
                    Graphics2D gtemp = originalImage.createGraphics();
                    gtemp.drawImage(temp.getScaledInstance(SIZE, SIZE, Image.SCALE_SMOOTH), 0, 0, null);
                    gtemp.dispose();
                } catch (IOException ex) {
                    Logger.getLogger(Texture.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else if (animated.length > 0) {
            while (!animated[0].isReady()) {
                try {
                    Thread.sleep(50);
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
        if (ffmpeg != null) {
//            try {
//                ffmpeg.initStream();
//            } catch (IOException ex) {
//                Logger.getLogger(Texture.class.getName()).log(Level.SEVERE, null, ex);
//            }
        }
        mIsReady = true;
        if (animated.length > 0) {
            int index = 0;
            while (!mStopMe) {
                while (!animated[index].isReady()) {
                    try {
                        Thread.sleep(50);
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

    private boolean isPlaying = false;

    public boolean isMediaPlaying() {
        return isPlaying;
    }

    public boolean hasMedia() {
        return ffmpeg != null;
    }

    public void playMedia() {
        if (ffmpeg != null && !isPlaying) {
            try {
                ffmpeg.initStream();
                isPlaying = true;
            } catch (IOException ex) {
                Logger.getLogger(Texture.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void stopMedia() {
        if (ffmpeg != null && isPlaying) {
            try {
                isPlaying = false;
                ffmpeg.disposeStream();
            } catch (IOException ex) {
                Logger.getLogger(Texture.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
