package webroom.engine;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Camera implements KeyListener {

    public double xPos, yPos, xDir, yDir, xPlane, yPlane;
    public boolean left, right, forward, back, strafe;
    public double MOVE_SPEED = 0.09;
    public double ROTATION_SPEED = 0.08;
    private double runFactor = 1D;
    private final Message listener;
    private int lastX = 0;
    private int lastY = 0;

    public Camera(double x, double y, double xd, double yd, double xp, double yp, Message listener) {
        this.listener = listener;
        xPos = x;
        yPos = y;
        xDir = xd;
        yDir = yd;
        xPlane = xp;
        yPlane = yp;
    }

    @Override
    public void keyPressed(KeyEvent key) {
        if ((key.getKeyCode() == KeyEvent.VK_LEFT) || key.getKeyCode() == KeyEvent.VK_A) {
            left = true;
        }
        if ((key.getKeyCode() == KeyEvent.VK_RIGHT) || key.getKeyCode() == KeyEvent.VK_D) {
            right = true;
        }
        if ((key.getKeyCode() == KeyEvent.VK_UP) || key.getKeyCode() == KeyEvent.VK_W) {
            forward = true;
        }
        if ((key.getKeyCode() == KeyEvent.VK_DOWN) || key.getKeyCode() == KeyEvent.VK_S) {
            back = true;
        }
        if (key.getKeyCode() == KeyEvent.VK_SHIFT) {
            runFactor = 1.8;
        }
        if (key.getKeyCode() == KeyEvent.VK_CONTROL) {
            strafe = true;
        }
        // TODO Auto-generated method stub
        if (key.getKeyCode() == KeyEvent.VK_SPACE) {
            // coordinates are reversed...
            listener.OnAction((int) this.yPos, (int) this.xPos);
        }
    }

    @Override
    public void keyReleased(KeyEvent key) {
        if ((key.getKeyCode() == KeyEvent.VK_LEFT) || key.getKeyCode() == KeyEvent.VK_A) {
            left = false;
        }
        if ((key.getKeyCode() == KeyEvent.VK_RIGHT) || key.getKeyCode() == KeyEvent.VK_D) {
            right = false;
        }
        if ((key.getKeyCode() == KeyEvent.VK_UP) || key.getKeyCode() == KeyEvent.VK_W) {
            forward = false;
        }
        if ((key.getKeyCode() == KeyEvent.VK_DOWN) || key.getKeyCode() == KeyEvent.VK_S) {
            back = false;
        }
        if (key.getKeyCode() == KeyEvent.VK_SHIFT) {
            runFactor = 1;
        }
        if (key.getKeyCode() == KeyEvent.VK_CONTROL) {
            strafe = false;
        }
    }

    public void update(int[][] map) {
        if (forward) {
            if (map[(int) (xPos + xDir * MOVE_SPEED * runFactor)][(int) yPos] == 0) {
                xPos += xDir * MOVE_SPEED * runFactor;
            }
            if (map[(int) xPos][(int) (yPos + yDir * MOVE_SPEED * runFactor)] == 0) {
                yPos += yDir * MOVE_SPEED * runFactor;
            }
        }
        if (back) {
            if (map[(int) (xPos - xDir * MOVE_SPEED * runFactor)][(int) yPos] == 0) {
                xPos -= xDir * MOVE_SPEED * runFactor;
            }
            if (map[(int) xPos][(int) (yPos - yDir * MOVE_SPEED * runFactor)] == 0) {
                yPos -= yDir * MOVE_SPEED * runFactor;
            }
        }
        if (right) {
            if (strafe) {
                double newXDir = xDir * Math.cos(Math.PI/2) - yDir * Math.sin(Math.PI/2);
                if (map[(int) (xPos - newXDir * MOVE_SPEED)][(int) yPos] == 0) {
                    xPos -= newXDir * MOVE_SPEED;
                }
                double newYDir = xDir * Math.sin(Math.PI/2) + yDir * Math.cos(Math.PI/2);;
                if (map[(int) xPos][(int) (yPos - newYDir * MOVE_SPEED)] == 0) {
                    yPos -= newYDir * MOVE_SPEED;
                }
            } else {
                double oldxDir = xDir;
                xDir = xDir * Math.cos(-ROTATION_SPEED) - yDir * Math.sin(-ROTATION_SPEED);
                yDir = oldxDir * Math.sin(-ROTATION_SPEED) + yDir * Math.cos(-ROTATION_SPEED);
                double oldxPlane = xPlane;
                xPlane = xPlane * Math.cos(-ROTATION_SPEED) - yPlane * Math.sin(-ROTATION_SPEED);
                yPlane = oldxPlane * Math.sin(-ROTATION_SPEED) + yPlane * Math.cos(-ROTATION_SPEED);
            }
        }
        if (left) {
            if (strafe) {
                double newXDir = xDir * Math.cos(Math.PI/2) - yDir * Math.sin(Math.PI/2);;
                if (map[(int) (xPos + newXDir * MOVE_SPEED)][(int) yPos] == 0) {
                    xPos += newXDir * MOVE_SPEED;
                }
                double newYDir = xDir * Math.sin(Math.PI/2) + yDir * Math.cos(Math.PI/2);
                if (map[(int) xPos][(int) (yPos + newYDir * MOVE_SPEED)] == 0) {
                    yPos += newYDir * MOVE_SPEED;
                }
            } else {
                double oldxDir = xDir;
                xDir = xDir * Math.cos(ROTATION_SPEED) - yDir * Math.sin(ROTATION_SPEED);
                yDir = oldxDir * Math.sin(ROTATION_SPEED) + yDir * Math.cos(ROTATION_SPEED);
                double oldxPlane = xPlane;
                xPlane = xPlane * Math.cos(ROTATION_SPEED) - yPlane * Math.sin(ROTATION_SPEED);
                yPlane = oldxPlane * Math.sin(ROTATION_SPEED) + yPlane * Math.cos(ROTATION_SPEED);
            }
        }
        if (lastX != (int) xPos || lastY != (int) yPos) {
            lastX = (int) xPos;
            lastY = (int) yPos;
            //coordinate are reversed...
            listener.OnTrigger(lastY, lastX);
        }
    }

    public void keyTyped(KeyEvent key) {

    }

}
