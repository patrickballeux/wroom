/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webroom.engine;

/**
 *
 * @author patri
 */
public interface Message {
    public enum Type{
        ACTION,
        CLOSEMEDIA,
        TRIGGER,
        NOTIFICATION   
    }
    public void status(Type t,String msg);
}
