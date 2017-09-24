/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webroom.gui;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import webroom.engine.Message;

/**
 *
 * @author patri
 */
public class MyWebView extends BorderPane {

    private javafx.scene.web.WebView viewer;
    private Pane instance;

    public MyWebView(Message listener, boolean showOpen) {
        ToolBar bar = new ToolBar();
        viewer = new javafx.scene.web.WebView();
        instance = this;
        Button close = new Button("Close");
        bar.getItems().add(close);
        close.setOnAction((event) -> {
            listener.onCloseView(instance);
        });
        if (showOpen) {
            Button openBrowser = new Button("Open...");
            openBrowser.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    if (!viewer.getEngine().getHistory().getEntries().isEmpty()) {
                        if (Desktop.isDesktopSupported()) {
                            try {
                                Desktop.getDesktop().browse(new URI(viewer.getEngine().getHistory().getEntries().get(0).getUrl()));
                            } catch (URISyntaxException ex) {
                                Logger.getLogger(MyWebView.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (IOException ex) {
                                Logger.getLogger(MyWebView.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                    listener.onCloseView(instance);
                }
            });
            bar.getItems().add(openBrowser);
        }
        setTop(bar);
        setCenter(viewer);
        viewer.setVisible(true);
    }

    public WebEngine getEngine() {
        return viewer.getEngine();

    }

    public WebView getViewer() {
        return viewer;
    }
}
