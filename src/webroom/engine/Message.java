/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webroom.engine;

import javax.swing.JComponent;

/**
 *
 * @author patri
 */
public interface Message {
    public void OnAction(int x, int y);
    public void OnTrigger(int x, int y);
    public void OnNotification(int x, int y, String msg);
    public void OnError(int x, int y, String error);
    public void onCloseView(JComponent comp);
    public void onChatRequest();
}
