package com.financialwhirlpool.antisnap.Class;

import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.List;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by an vo on 7/15/2016.
 */
public class Friend extends RealmObject {
    String profile;
    String name;
    String gender;
    String email;
    @PrimaryKey
    String uid;
    boolean hasMessage = false;
    RealmList<ImageMessage> imageMessageList;
    //List<ImageMessage> imageMessageList = new ArrayList<ImageMessage>();


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public RealmList<ImageMessage> getImageMessageList() {
        return imageMessageList;
    }

    public void setImageMessageList(RealmList<ImageMessage> imageMessageList) {
        this.imageMessageList = imageMessageList;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getProfile() {
        return profile;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public boolean isHasMessage() {
        return hasMessage;
    }

    public void setHasMessage(boolean hasMessage) {
        this.hasMessage = hasMessage;
    }


    public String getUid() {

        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    // Constructor
    public Friend() {
    }
}
