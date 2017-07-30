/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webroom.engine;

import java.net.URL;

/**
 *
 * @author patri
 */
public class MapEntry {

    /**
     * @return the texture
     */
    public URL getTexture() {
        return texture;
    }

    /**
     * @param texture the texture to set
     */
    public void setTexture(URL texture) {
        this.texture = texture;
    }

    /**
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * @param text the text to set
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * @return the isDoor
     */
    public boolean isIsDoor() {
        return isDoor;
    }

    /**
     * @param isDoor the isDoor to set
     */
    public void setIsDoor(boolean isDoor) {
        this.isDoor = isDoor;
    }

    /**
     * @return the sound
     */
    public URL getSound() {
        return sound;
    }

    /**
     * @param sound the sound to set
     */
    public void setSound(URL sound) {
        this.sound = sound;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return the download
     */
    public URL getDownload() {
        return download;
    }

    /**
     * @param download the download to set
     */
    public void setDownload(URL download) {
        this.download = download;
    }

    /**
     * @return the teleport
     */
    public URL getTeleport() {
        return teleport;
    }

    /**
     * @param teleport the teleport to set
     */
    public void setTeleport(URL teleport) {
        this.teleport = teleport;
    }

    /**
     * @return the media
     */
    public URL getMedia() {
        return media;
    }

    /**
     * @param media the media to set
     */
    public void setMedia(URL media) {
        this.media = media;
    }

    /**
     * @return the browse
     */
    public URL getBrowse() {
        return browse;
    }

    /**
     * @param browse the browse to set
     */
    public void setBrowse(URL browse) {
        this.browse = browse;
    }

    /**
     * @return the embededText
     */
    public String getEmbededText() {
        return embededText;
    }

    /**
     * @param embededText the embededText to set
     */
    public void setEmbededText(String embededText) {
        this.embededText = embededText;
    }

    /**
     * @return the embededURL
     */
    public URL getEmbededURL() {
        return embededURL;
    }

    /**
     * @param embededURL the embededURL to set
     */
    public void setEmbededURL(URL embededURL) {
        this.embededURL = embededURL;
    }

    /**
     * @return the x
     */
    public double getX() {
        return x;
    }

    /**
     * @param x the x to set
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * @return the y
     */
    public double getY() {
        return y;
    }

    /**
     * @param y the y to set
     */
    public void setY(double y) {
        this.y = y;
    }

    public String toString() {
        if (texture == null) {
            return "0";
        } else {
            return "1";
        }
    }

    private URL texture = null;
    private String text = null;
    private boolean isDoor = false;
    private URL sound = null;
    private String message = null;
    private URL download = null;
    private URL teleport = null;
    private URL media = null;
    private URL browse = null;
    private String embededText = null;
    private URL embededURL = null;
    private double x = 0;
    private double y = 0;

}
