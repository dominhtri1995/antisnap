package com.financialwhirlpool.antisnap.Class;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by an vo on 7/18/2016.
 */
public class ImageMessage extends RealmObject {
    private String imageMessage;
    @PrimaryKey
    private String keyMessage;
    private String sender;
    private String orientation;

    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }

    public String getKeyMessage() {
        return keyMessage;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setKeyMessage(String keyMessage) {
        this.keyMessage = keyMessage;
    }

    public ImageMessage() {
    }

    public ImageMessage(String imageMessage) {

        this.imageMessage = imageMessage;
    }

    public String getImageMessage() {
        return imageMessage;
    }

    public void setImageMessage(String imageMessage) {
        this.imageMessage = imageMessage;
    }
}
