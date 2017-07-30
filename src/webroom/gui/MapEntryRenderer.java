/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webroom.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import webroom.engine.MapEntry;

/**
 *
 * @author patri
 */
public class MapEntryRenderer implements TableCellRenderer {

    private TreeMap<String, ImageIcon> cache = new TreeMap<>();

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = new JLabel(value.toString());
        label.setOpaque(true);
        MapEntry entry = (MapEntry) value;
        if (entry.getTexture() != null) {
            if (!cache.containsKey(entry.getTexture().toString())) {
                try {
                    ImageIcon icon = new ImageIcon(javax.imageio.ImageIO.read(entry.getTexture()).getScaledInstance(32, 32, Image.SCALE_FAST));
                    if (icon != null) {
                        cache.put(entry.getTexture().toString(), icon);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(MapEntryRenderer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            label.setIcon(cache.get(entry.getTexture().toString()));
        }

        String info = "";
        if (entry.isIsDoor()) {
            info += "D";
        }
        if (entry.getTeleport() != null) {
            info += "T";
        }
        if (entry.getSound() != null) {
            info += "S";
        }
        if (entry.getBrowse() != null) {
            info += "B";
        }
        if (entry.getText() != null) {
            info += "$";
        }
        if (entry.getMedia() != null) {
            info += "M";
        }
        if (entry.getMessage() != null) {
            info += "m";
        }
        label.setText(info);
        if (isSelected) {
            label.setBackground(Color.lightGray);

        } else {
            label.setBackground(Color.white);
        }
        return label;
    }

}
