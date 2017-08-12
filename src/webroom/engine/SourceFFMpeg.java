/*
 * Copyright (C) 2016 Patrick Balleux (Twitter: @patrickballeux)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package webroom.engine;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 *
 * @author patrick
 */
public class SourceFFMpeg {

    private Process mProcess;
    private final String mInput;
    private int mFPS = 15;
    private boolean mStopMe = false;
    private final Texture mTexture;
    private ServerSocket videoStream;
    private ServerSocket audioStream;
    private ArrayList<int[]> videoBuffer = new ArrayList<>();
    private static final int MAXBUFFERFRAMES = 100;

    private void renderVideo() throws IOException, InterruptedException {
        Socket s = videoStream.accept();
        byte[] dataBuffer = new byte[Texture.SIZE * Texture.SIZE * 4];
        DataInputStream din = new DataInputStream(s.getInputStream());
        while (!mStopMe) {
            if (videoBuffer.size() < MAXBUFFERFRAMES) {
                din.readFully(dataBuffer);
                IntBuffer intBuf = ByteBuffer.wrap(dataBuffer).asIntBuffer();
                int[] temp = new int[dataBuffer.length / 4];
                intBuf.get(temp);
                videoBuffer.add(temp);
            } else {
                Thread.sleep(50);
            }
        }
    }

    private void drawVideo() throws InterruptedException {
        long audioFrameSize = ((44100) / mFPS);
        long nextFrameIndex = 0;
        while (!mStopMe) {
            if (videoBuffer.size() > 0 && source != null && source.isRunning()) {
                if (nextFrameIndex <= source.getLongFramePosition()) {
                    nextFrameIndex += audioFrameSize;
                    mTexture.pixels = videoBuffer.remove(0);
                } else {
                    Thread.sleep(33);
                }
            } else {
                Thread.sleep(50);
            }
        }
    }
    private SourceDataLine source;

    private void renderAudio() throws IOException, LineUnavailableException, UnsupportedAudioFileException, InterruptedException {
        Socket s = audioStream.accept();
        DataInputStream din = new DataInputStream(s.getInputStream());
        AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
        source = AudioSystem.getSourceDataLine(format);
        byte[] buffer = new byte[(44100 * 4)];
        source.open(format, buffer.length * 2);
        source.start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    drawVideo();
                } catch (InterruptedException ex) {
                    Logger.getLogger(SourceFFMpeg.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }).start();
        while (!mStopMe) {
            din.readFully(buffer);
            source.write(buffer, 0, buffer.length);
        }
        source.stop();
        source.close();
    }

    public SourceFFMpeg(Texture t, String input) {
        mInput = input;
        mTexture = t;
    }

    protected void initStream() throws IOException {
        mStopMe = false;
        videoStream = new ServerSocket(0);
        audioStream = new ServerSocket(0);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    renderVideo();
                } catch (IOException ex) {
                    //Logger.getLogger(SourceFFMpeg.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    //Logger.getLogger(SourceFFMpeg.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    renderAudio();
                } catch (IOException ex) {
                    //Logger.getLogger(SourceFFMpeg.class.getName()).log(Level.SEVERE, null, ex);
                } catch (LineUnavailableException ex) {
                    Logger.getLogger(SourceFFMpeg.class.getName()).log(Level.SEVERE, null, ex);
                } catch (UnsupportedAudioFileException ex) {
                    Logger.getLogger(SourceFFMpeg.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SourceFFMpeg.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }).start();
        String command = "libs/ffmpeg.exe  -v 0 -i " + mInput + " " + "-s " + Texture.SIZE + "x" + Texture.SIZE + " -r " + mFPS + " -f rawvideo -pix_fmt argb tcp://127.0.0.1:" + videoStream.getLocalPort() + " -f s16le -ac 2 tcp://127.0.0.1:" + audioStream.getLocalPort();
        if (!isWindows()) {
            if (isOSX()) {
                command = "libs/ffmpegosx  -v 0 -i " + mInput + " " + "-s " + Texture.SIZE + "x" + Texture.SIZE + " -r " + mFPS + " -f rawvideo -pix_fmt argb tcp://127.0.0.1:" + videoStream.getLocalPort() + " -f s16le -ac 2 tcp://127.0.0.1:" + audioStream.getLocalPort();
            } else {
                command = "libs/ffmpeg  -v 0 -i " + mInput + " " + "-s " + Texture.SIZE + "x" + Texture.SIZE + " -r " + mFPS + " -f rawvideo -pix_fmt argb tcp://127.0.0.1:" + videoStream.getLocalPort() + " -f s16le -ac 2 tcp://127.0.0.1:" + audioStream.getLocalPort();
            }
        }
        System.out.println(command);
        mProcess = Runtime.getRuntime().exec(command);
        new Thread(new Runnable() {
            @Override
            public void run() {
                DataInputStream in = new DataInputStream(mProcess.getErrorStream());
                while (!mStopMe) {
                    try {
                        String s = in.readUTF();
                        System.err.println(s);
                        Thread.sleep(200);
                    } catch (IOException ex) {
                        //Logger.getLogger(SourceFFMpeg.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(SourceFFMpeg.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                try {
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(SourceFFMpeg.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }).start();
    }

    protected void disposeStream() throws IOException {
        mStopMe = true;
        mProcess.getOutputStream().write("q\n".getBytes());
        try {
            mProcess.getOutputStream().flush();
            mProcess.getOutputStream().close();
        } catch (IOException ex) {
            //just in case the stream is already closed...
        }
        mProcess.destroy();
        mProcess.destroyForcibly();
        mProcess = null;
    }

    public static boolean isOSX() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.startsWith("mac os x");
    }

    public static boolean isWindows() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.startsWith("windows");
    }

}
